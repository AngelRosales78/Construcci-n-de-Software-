package com.example.sp500auth.data.push

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.*
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.BuildConfig
import com.example.sp500auth.data.ops.*
import com.example.sp500auth.ui.compare.validTicker
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class PushStatus(val text: String) {
    NOT_CONFIGURED("Esta compilación no tiene configuración de Firebase, por lo que este teléfono no puede recibir Push."),
    DENIED("El permiso de notificaciones está denegado en Android. Aunque el canal esté activo, este teléfono no las mostrará."),
    UNLINKED("Este teléfono todavía no está vinculado a tu cuenta para recibir Push."),
    READY("Este teléfono está vinculado a tu cuenta para recibir Push.")
}

/** What a Push message may carry: a ticker, its owner and the notice id. Nothing financial or private. */
data class PushTarget(val ticker: String, val owner: String, val noticeId: Long?)

fun pushTarget(ticker: String?, owner: String?, notice: String?): PushTarget? {
    if (ticker == null || !validTicker(ticker) || owner == null || !owner.matches(Regex("[1-9][0-9]{0,17}"))) return null
    return PushTarget(ticker, owner, notice?.toLongOrNull()?.takeIf { it > 0 })
}

/** Local memory of the link between this installation and an account. */
interface PushMemory {
    val installationId: String
    var linkedOwner: String?
    /** User choice; without it the phone never asks Firebase for a token. */
    var wanted: Boolean
}

/** HU12_02 rules without Firebase, so they can be tested. */
class PushLink(private val source: PushInstallationsSource, private val memory: PushMemory) {
    fun linked() = memory.linkedOwner != null && memory.linkedOwner == source.owner()

    /** Links or renews the token for the account of the current session. */
    suspend fun sync(token: String, granted: Boolean): Outcome<Unit> {
        val account = source.owner() ?: return Outcome.Fail("Inicia sesión para vincular este teléfono.", 401)
        return when (val answer = source.register(memory.installationId, token, granted)) {
            is Outcome.Ok -> {
                memory.linkedOwner = account
                if (answer.value.configured == true) Outcome.Ok(Unit)
                else Outcome.Fail("El servidor guardó este teléfono, pero todavía no tiene configurado el envío de Push.", 200)
            }
            is Outcome.Fail -> answer
        }
    }

    /** Before closing the session: the server stops sending to this phone for that account. */
    suspend fun detach() {
        if (memory.linkedOwner == null) return
        source.unregister(memory.installationId)
        memory.linkedOwner = null
    }

    fun forget() { memory.linkedOwner = null }
}

private class StoredPushMemory(context: Context) : PushMemory {
    private val prefs = context.getSharedPreferences("push_installation", Context.MODE_PRIVATE)
    override val installationId: String get() = prefs.getString("id", null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("id", it).apply() }
    override var linkedOwner: String?
        get() = prefs.getString("owner", null)
        set(value) = prefs.edit().putString("owner", value).apply()
    override var wanted: Boolean
        get() = prefs.getBoolean("wanted", false)
        set(value) = prefs.edit().putBoolean("wanted", value).apply()
}

@Singleton
class PushRegistrar @Inject constructor(@ApplicationContext private val context: Context, private val repository: NotificationsRepository) {
    private val memory = StoredPushMemory(context)
    private val link = PushLink(repository, memory)
    val configured = BuildConfig.FIREBASE_APP_ID.isNotBlank()

    /** Firebase is created from local build values, so no configuration file is needed inside Git. */
    fun start() {
        if (!configured || FirebaseApp.getApps(context).isNotEmpty()) return
        FirebaseApp.initializeApp(context, FirebaseOptions.Builder().setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID).build())
    }

    fun permissionGranted() = (Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun status() = when {
        !configured -> PushStatus.NOT_CONFIGURED
        !link.linked() -> PushStatus.UNLINKED
        !permissionGranted() -> PushStatus.DENIED
        else -> PushStatus.READY
    }

    private suspend fun token(): String? = withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine { waiting ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task -> if (waiting.isActive) waiting.resume(if (task.isSuccessful) task.result else null) }
        }
    }

    suspend fun enable(): Outcome<Unit> {
        if (!configured) return Outcome.Fail(PushStatus.NOT_CONFIGURED.text, 0)
        memory.wanted = true
        val value = runCatching { start(); token() }.getOrNull() ?: return Outcome.Fail("Firebase no entregó el identificador de este teléfono. Revisa la conexión y reintenta.")
        return link.sync(value, permissionGranted())
    }

    /** After login or a permission change: renews the link only if the person had asked for Push. */
    suspend fun resync() { if (configured && memory.wanted && repository.owner() != null) enable() }
    suspend fun renewed(token: String) { if (memory.wanted && repository.owner() != null) link.sync(token, permissionGranted()) }
    suspend fun detach() { runCatching { withTimeoutOrNull(8_000) { link.detach() } }; link.forget() }
    fun owner() = repository.owner()
}

@HiltViewModel
class PushViewModel @Inject constructor(private val registrar: PushRegistrar) : ViewModel() {
    var status by mutableStateOf(registrar.status()); private set
    var problem by mutableStateOf<String?>(null); private set
    var working by mutableStateOf(false); private set
    fun permissionGranted() = registrar.permissionGranted()
    fun refresh() { status = registrar.status() }
    fun enable() {
        if (working) return
        working = true; problem = null
        viewModelScope.launch {
            (registrar.enable() as? Outcome.Fail)?.let { problem = it.message }
            status = registrar.status(); working = false
        }
    }
}
