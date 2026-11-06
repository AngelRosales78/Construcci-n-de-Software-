package com.example.sp500auth.util.biometric

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthManager @Inject constructor() {

    fun isBiometricAvailable(context: Context): Boolean {
        return unavailableReason(context) == null
    }

    fun unavailableReason(context: Context): String? = when (
        BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    ) {
        BiometricManager.BIOMETRIC_SUCCESS -> null
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Registra una huella en Ajustes del teléfono y vuelve a intentarlo."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Este teléfono no tiene un sensor biométrico compatible."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "El sensor está ocupado o no está disponible. Inténtalo de nuevo."
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Actualiza la seguridad del teléfono para usar el acceso biométrico."
        else -> "No se puede usar la huella en este teléfono. Puedes entrar con tu contraseña."
    }

    private tailrec fun activity(context: Context): FragmentActivity? = when (context) {
        is FragmentActivity -> context
        is ContextWrapper -> if (context.baseContext !== context) activity(context.baseContext) else null
        else -> null
    }

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onFailed: () -> Unit
    ) {
        val host = activity(context)
        if (host == null) {
            onError("Contexto no válido para autenticación biométrica")
            return
        }
        unavailableReason(context)?.let { onError(it); return }

        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(host, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación Biométrica")
            .setSubtitle("Usa tu huella dactilar para iniciar sesión")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
