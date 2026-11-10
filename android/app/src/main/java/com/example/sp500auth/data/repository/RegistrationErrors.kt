package com.example.sp500auth.data.repository

import com.google.gson.JsonParser
import java.io.IOException
import java.net.SocketTimeoutException

internal fun registrationHttpError(code: Int, body: String?): String {
    if (code >= 500) return "El servidor no pudo completar el registro. Inténtalo más tarde."
    if (code == 429) return "Demasiados intentos. Espera unos minutos antes de registrarte."
    if (code == 404) return "No se encontró el servicio de registro. Revisa la dirección del servidor."
    val labels = linkedMapOf("username" to "Usuario", "email" to "Correo", "password" to "Contraseña",
        "password_confirm" to "Confirmación", "non_field_errors" to "Registro", "detail" to "Registro")
    val messages = runCatching {
        val json = JsonParser.parseString(body ?: "").asJsonObject
        labels.flatMap { (field, label) ->
            val value = json.get(field)
            val values = when {
                value == null -> emptyList()
                value.isJsonArray -> value.asJsonArray.toList()
                else -> listOf(value)
            }
            values.filter { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                .map { "$label: ${it.asString.take(400)}" }
        }.take(6).joinToString("\n")
    }.getOrDefault("")
    return messages.ifBlank { "No se pudo registrar la cuenta (HTTP $code). Revisa los datos e inténtalo de nuevo." }
}

internal fun registrationExceptionMessage(error: Exception): String = when (error) {
    is SocketTimeoutException -> "El servidor tardó demasiado en responder. Comprueba que esté iniciado e inténtalo de nuevo."
    is IOException -> "No se pudo conectar con el servidor. Comprueba que esté iniciado y que la dirección de la API sea correcta."
    else -> "No se pudo procesar la respuesta del registro. Intenta iniciar sesión antes de volver a registrarte."
}
