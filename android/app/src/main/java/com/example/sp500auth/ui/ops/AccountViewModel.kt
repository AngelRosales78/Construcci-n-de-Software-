package com.example.sp500auth.ui.ops

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.api.models.UserProfile
import com.example.sp500auth.data.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

const val DELETE_PHRASE = "ELIMINAR"

/** HU13_04 and HU13_05. Lives with the activity so the header shows the confirmed name and photo. */
@HiltViewModel
class AccountViewModel internal constructor(private val source: AccountSource) : ViewModel() {
    @Inject constructor(repository: AccountRepository) : this(repository as AccountSource)

    private var owner: String? = null
    private var profileState by mutableStateOf(Load<UserProfile>())
    private var photoState by mutableStateOf<ByteArray?>(null)
    private var photoVersion: String? = null
    private var job: Job? = null

    /** Data of another account is never shown, even before it is cleared. */
    val profile: Load<UserProfile> get() = if (owner != null && owner == source.owner()) profileState else Load()
    val photo: ByteArray? get() = photoState.takeIf { owner != null && owner == source.owner() }
    val isAdmin get() = profile.value?.role == "admin"

    var first by mutableStateOf(""); private set
    var last by mutableStateOf(""); private set
    var picked by mutableStateOf<AvatarFile?>(null); private set
    var saving by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var errors by mutableStateOf<Map<String, String>>(emptyMap()); private set
    var deleting by mutableStateOf(false); private set
    var deleteError by mutableStateOf<String?>(null); private set
    var deleted by mutableStateOf(false); private set

    fun clear() {
        job?.cancel(); owner = null; profileState = Load(); photoState = null; photoVersion = null
        first = ""; last = ""; picked = null; saving = false; message = null; errors = emptyMap()
        deleting = false; deleteError = null; deleted = false
    }

    fun load() {
        val account = source.owner()
        if (account == null) { clear(); return }
        if (account != owner) { clear(); owner = account }
        job?.cancel()
        profileState = profileState.started()
        job = viewModelScope.launch {
            val answer = source.profile()
            ensureActive()
            if (source.owner() != account) return@launch
            profileState = profileState.with(answer)
            if (answer is Outcome.Ok) loadPhoto(answer.value)
        }
    }

    private suspend fun loadPhoto(profile: UserProfile) {
        val version = profile.avatar_version
        if (version == null) { photoState = null; photoVersion = null; return }
        if (version == photoVersion && photoState != null) return
        val answer = source.avatar()
        if (answer is Outcome.Ok) { photoState = answer.value; photoVersion = version }
    }

    fun startEditing() {
        val current = profile.value
        first = current?.first_name.orEmpty(); last = current?.last_name.orEmpty()
        picked = null; message = null; errors = emptyMap()
    }
    fun writeFirst(text: String) { first = text.take(150); message = null }
    fun writeLast(text: String) { last = text.take(150); message = null }

    /** `null` type or bytes mean the file could not be read. */
    fun pick(type: String?, bytes: ByteArray?) {
        val problem = avatarProblem(type, bytes?.size ?: 0)
        message = null
        if (problem != null || type == null || bytes == null) errors = errors + ("avatar" to (problem ?: "No se pudo leer la imagen."))
        else { picked = AvatarFile(type, bytes); errors = errors - "avatar" }
    }
    fun discardPick() { picked = null; errors = errors - "avatar" }

    fun save() = submit("Perfil actualizado.") { source.save(first, last, picked) }
    fun removePhoto() = submit("Se quitó tu foto.") { source.removeAvatar() }

    private fun submit(success: String, block: suspend () -> Outcome<UserProfile>) {
        if (saving) return
        val account = owner ?: return
        saving = true; message = null; errors = emptyMap()
        viewModelScope.launch {
            val answer = block()
            if (source.owner() == account) when (answer) {
                is Outcome.Ok -> { profileState = Load(answer.value); picked = null; loadPhoto(answer.value); message = success }
                // A rejected image changes nothing: the typed names stay in the form.
                is Outcome.Fail -> errors = answer.fields.ifEmpty { mapOf("form" to answer.message) }
            }
            saving = false
        }
    }

    fun deleteAccount(password: String, phrase: String) {
        if (deleting) return
        deleteError = when {
            phrase.trim() != DELETE_PHRASE -> "Escribe $DELETE_PHRASE para confirmar."
            password.isEmpty() -> "Escribe tu contraseña actual."
            else -> null
        }
        if (deleteError != null) return
        deleting = true
        viewModelScope.launch {
            when (val answer = source.delete(password)) {
                is Outcome.Ok -> deleted = true
                is Outcome.Fail -> deleteError = if (answer.uncertain)
                    "No hubo respuesta del servidor y no se sabe si la cuenta se eliminó. Tu sesión sigue abierta: comprueba tu perfil antes de repetir." else answer.message
            }
            deleting = false
        }
    }
    fun forgetDeleteError() { if (!deleting) deleteError = null }
}
