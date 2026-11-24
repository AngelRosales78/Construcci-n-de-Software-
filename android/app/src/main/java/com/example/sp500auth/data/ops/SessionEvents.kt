package com.example.sp500auth.data.ops

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** HU13_03: the server revoked the session (blocked account, role change or another closed session). */
@Singleton
class SessionEvents @Inject constructor() {
    private val current = MutableStateFlow<String?>(null)
    val revoked: StateFlow<String?> = current
    fun notifyRevoked() { current.value = REVOKED }
    fun consumed() { current.value = null }

    companion object {
        const val REVOKED = "El servidor cerró tu sesión. Puede deberse a un bloqueo de la cuenta, a un cambio de permisos o a que venció. Inicia sesión nuevamente."
    }
}
