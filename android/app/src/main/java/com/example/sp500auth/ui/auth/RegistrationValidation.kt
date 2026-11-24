package com.example.sp500auth.ui.auth

internal const val PASSWORD_HELP = "Usa al menos 8 caracteres, mayúscula, minúscula, número y símbolo."

internal fun registrationPasswordError(password: String, confirmation: String): String? = when {
    password.length < 8 || !password.any { it in 'A'..'Z' } ||
        !password.any { it in 'a'..'z' } || !password.any { it.isDigit() } ||
        !password.any { !it.isLetterOrDigit() && it != '_' && !it.isWhitespace() } -> PASSWORD_HELP
    password != confirmation -> "Las contraseñas no coinciden."
    else -> null
}

internal const val PHONE_HELP = "Opcional. Incluye el código de país, por ejemplo +51 987 654 321."

/** Optional phone: + and 7 to 15 digits once spaces, hyphens and parentheses are removed. */
internal fun registrationPhoneError(phone: String): String? {
    val normalized = com.example.sp500auth.data.repository.normalizedPhone(phone)
    return if (normalized.isEmpty() || Regex("""\+?\d{7,15}""").matches(normalized)) null
    else "Teléfono no válido. $PHONE_HELP"
}
