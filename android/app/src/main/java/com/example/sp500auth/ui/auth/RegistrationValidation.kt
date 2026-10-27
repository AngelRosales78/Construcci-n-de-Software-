package com.example.sp500auth.ui.auth

internal const val PASSWORD_HELP = "Usa al menos 8 caracteres, mayúscula, minúscula, número y símbolo."

internal fun registrationPasswordError(password: String, confirmation: String): String? = when {
    password.length < 8 || !password.any { it in 'A'..'Z' } ||
        !password.any { it in 'a'..'z' } || !password.any { it.isDigit() } ||
        !password.any { !it.isLetterOrDigit() && it != '_' && !it.isWhitespace() } -> PASSWORD_HELP
    password != confirmation -> "Las contraseñas no coinciden."
    else -> null
}
