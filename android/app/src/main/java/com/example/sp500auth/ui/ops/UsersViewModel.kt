package com.example.sp500auth.ui.ops

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.ops.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

/** A change the administrator must confirm before it is sent. */
sealed class UserDraft(val user: AdminUser) {
    class Role(user: AdminUser, val role: String) : UserDraft(user)
    class Plan(user: AdminUser, val plan: String) : UserDraft(user)
    class Block(user: AdminUser) : UserDraft(user)
    class Unblock(user: AdminUser) : UserDraft(user)
}

/** HU13_01 to HU13_03. The server authorises every operation; this only presents and confirms. */
@HiltViewModel
class UsersViewModel internal constructor(private val source: UsersSource, private val saved: SavedStateHandle) : ViewModel() {
    @Inject constructor(repository: UsersRepository, saved: SavedStateHandle) : this(repository as UsersSource, saved)

    var query by mutableStateOf(saved.get<String>("q") ?: ""); private set
    var plan by mutableStateOf(saved.get<String>("plan") ?: "all"); private set
    var state by mutableStateOf(saved.get<String>("state") ?: "all"); private set
    var page by mutableStateOf(saved.get<Int>("page") ?: 1); private set
    var users by mutableStateOf(Load<Page<AdminUser>>()); private set
    var draft by mutableStateOf<UserDraft?>(null); private set
    var reason by mutableStateOf(""); private set
    var saving by mutableStateOf(false); private set
    var draftError by mutableStateOf<String?>(null); private set
    var notice by mutableStateOf<String?>(null); private set
    private var job: Job? = null

    fun search(text: String) { query = text.take(120); saved["q"] = query; go(1, 350) }
    fun filterPlan(value: String) { plan = value; saved["plan"] = value; go(1) }
    fun filterState(value: String) { state = value; saved["state"] = value; go(1) }
    fun go(to: Int, wait: Long = 0) { page = maxOf(1, to); saved["page"] = page; load(wait) }

    fun load(wait: Long = 0) {
        job?.cancel()
        users = users.started()
        job = viewModelScope.launch {
            if (wait > 0) delay(wait)
            val answer = source.list(query, plan, state, page)
            ensureActive()
            // The page can disappear after a change of filters or accounts: return to the first one once.
            if (answer is Outcome.Fail && answer.code == 404 && answer.message != SERVICE_MISSING && page > 1) { go(1); return@launch }
            users = users.with(answer)
        }
    }

    fun propose(value: UserDraft) { if (!saving) { draft = value; reason = ""; draftError = null; notice = null } }
    fun writeReason(text: String) { reason = text.take(240); draftError = null }
    fun dismiss() { if (!saving) { draft = null; draftError = null } }

    fun confirm() {
        val current = draft ?: return
        if (saving) return
        val id = current.user.id ?: return
        val change: Map<String, Any> = when (current) {
            is UserDraft.Role -> mapOf("role" to current.role)
            is UserDraft.Plan -> mapOf("plan" to current.plan)
            is UserDraft.Block -> {
                if (reason.isBlank()) { draftError = "Indica un motivo para bloquear la cuenta."; return }
                mapOf("is_active" to false, "reason" to reason.trim())
            }
            is UserDraft.Unblock -> mapOf("is_active" to true)
        }
        saving = true; draftError = null
        viewModelScope.launch {
            when (val answer = source.update(id, change)) {
                is Outcome.Ok -> {
                    val confirmed = answer.value
                    // Only what the server confirmed reaches the list.
                    users = users.copy(value = users.value?.let { page -> page.copy(results = page.results?.map { if (it.id == id) confirmed else it }) })
                    notice = when (current) {
                        is UserDraft.Block -> "${confirmed.username} quedó bloqueado. Sus sesiones abiertas dejan de ser válidas."
                        is UserDraft.Unblock -> "${confirmed.username} puede volver a entrar iniciando sesión de nuevo."
                        is UserDraft.Role -> "${confirmed.username} ahora es ${if (confirmed.role == "admin") "administrador" else "usuario"}. Debe iniciar sesión otra vez."
                        is UserDraft.Plan -> "${confirmed.username} ahora tiene el plan ${if (confirmed.plan == "pro") "Pro" else "Free"}."
                    }
                    draft = null
                }
                is Outcome.Fail -> {
                    draftError = answer.fields["reason"] ?: answer.message
                    if (answer.code in setOf(401, 403)) users = users.with(answer)
                }
            }
            saving = false
        }
    }
}
