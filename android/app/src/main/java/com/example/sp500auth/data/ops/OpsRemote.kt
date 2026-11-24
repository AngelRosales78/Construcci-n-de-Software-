package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.data.repository.favoritesOwner
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Result of a Sprint 4 call. `code` is null when the server never answered, so the outcome is uncertain. */
sealed class Outcome<out T> {
    data class Ok<T>(val value: T) : Outcome<T>()
    data class Fail(
        val message: String, val code: Int? = null,
        val fields: Map<String, String> = emptyMap(), val body: JsonObject? = null
    ) : Outcome<Nothing>() {
        val forbidden get() = code == 403
        val uncertain get() = code == null
    }
}

/** Server page as published by the shared API. */
data class Page<T>(val count: Int? = null, val next: String? = null, val previous: String? = null, val results: List<T>? = null)

/** Screen state that keeps the last valid data while it refreshes or after an error. */
data class Load<T>(val value: T? = null, val loading: Boolean = false, val error: Outcome.Fail? = null) {
    fun started() = copy(loading = true, error = null)
    fun with(outcome: Outcome<T>): Load<T> = when (outcome) {
        is Outcome.Ok -> Load(outcome.value)
        // Losing permission removes private data; any other failure keeps it visible.
        is Outcome.Fail -> Load(if (outcome.code in setOf(401, 403)) null else value, false, outcome)
    }
}

internal const val SERVICE_MISSING = "Este servidor todavía no ofrece esta función del Sprint 4."
private val notFields = setOf("detail", "code", "non_field_errors", "operation", "provider", "messages")

private fun JsonElement.firstText(): String? = when {
    isJsonPrimitive -> asString
    isJsonArray -> asJsonArray.firstOrNull()?.firstText()
    isJsonObject -> asJsonObject.entrySet().firstOrNull()?.value?.firstText()
    else -> null
}

/** Turns an HTTP error into a message for people and, when the server says so, one message per field. */
fun httpFailure(code: Int, raw: String?): Outcome.Fail {
    val json = runCatching { JsonParser.parseString(raw.orEmpty()) }.getOrNull()?.takeIf { it.isJsonObject || it.isJsonArray }
    val obj = json?.takeIf { it.isJsonObject }?.asJsonObject
    val fields = obj?.entrySet().orEmpty().filter { it.key !in notFields }
        .mapNotNull { (key, value) -> value.firstText()?.let { key to it.take(300) } }.toMap()
    val detail = (obj?.get("detail")?.firstText() ?: obj?.get("non_field_errors")?.firstText()
        ?: json?.takeIf { it.isJsonArray }?.firstText())?.take(400)?.takeIf { it.isNotBlank() }
    val generic = detail == null || detail.startsWith("You do not have") || detail == "Not found." ||
        (detail.startsWith("No ") && detail.contains("matches the given query"))
    val message = when {
        code == 401 -> "Tu sesión terminó. Inicia sesión nuevamente."
        code == 403 && generic -> "Tu cuenta no tiene permisos para esta acción."
        code == 404 && json == null -> SERVICE_MISSING
        code == 404 && generic -> "El elemento ya no existe o no pertenece a tu cuenta."
        detail != null -> detail
        fields.isNotEmpty() -> "Revisa los campos marcados."
        code == 409 -> "Los datos cambiaron. Actualiza antes de repetir la operación."
        code == 413 -> "El archivo es demasiado grande."
        code == 429 -> "Se alcanzó el límite de llamadas. Los datos guardados siguen disponibles."
        code >= 500 -> "El servidor no pudo completar la operación. Inténtalo más tarde."
        else -> "No se pudo completar la operación (HTTP $code)."
    }
    return Outcome.Fail(message, code, fields, obj)
}

/** Every call is bound to the account that started it, including authenticated retries. */
class OpsRemote internal constructor(val retrofit: Retrofit, private val token: () -> String?) {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })

    fun owner(): String? = favoritesOwner(token())

    suspend fun <T : Any> call(
        valid: (T, String) -> Boolean = { _, _ -> true },
        block: suspend (String, AccountBinding) -> Response<T>
    ): Outcome<T> = execute(block) { response, user ->
        val body = response.body()
        if (body != null && runCatching { valid(body, user) }.getOrDefault(false)) Outcome.Ok(body)
        else Outcome.Fail("El servidor devolvió datos incompletos o de otra cuenta.", response.code())
    }

    /** For answers without a body, such as 204. */
    suspend fun command(block: suspend (String, AccountBinding) -> Response<Unit>): Outcome<Unit> =
        execute(block) { _, _ -> Outcome.Ok(Unit) }

    private suspend fun <T : Any, R> execute(
        block: suspend (String, AccountBinding) -> Response<T>, success: (Response<T>, String) -> Outcome<R>
    ): Outcome<R> {
        val access = token()
        val user = favoritesOwner(access) ?: return Outcome.Fail("Inicia sesión para continuar.", 401)
        return try {
            val response = block("Bearer $access", AccountBinding(user))
            when {
                response.code() == 401 -> httpFailure(401, null)
                owner() != user -> Outcome.Fail("La cuenta cambió durante la operación. Vuelve a consultar.", 409)
                response.isSuccessful -> success(response, user)
                else -> httpFailure(response.code(), response.errorBody()?.string())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: SocketTimeoutException) {
            Outcome.Fail("El servidor tardó demasiado en responder. No se sabe si terminó la operación.")
        } catch (_: IOException) {
            Outcome.Fail("No hay conexión con el servidor. Puedes reintentar.")
        } catch (_: Exception) {
            Outcome.Fail("No se pudo interpretar la respuesta del servidor.", 0)
        }
    }

    /** Market consultations wait for an external provider. */
    fun longRunning(): OpsRemote {
        val client = retrofit.callFactory() as? okhttp3.OkHttpClient ?: return this
        val slow = client.newBuilder().readTimeout(100, TimeUnit.SECONDS).callTimeout(110, TimeUnit.SECONDS).build()
        return OpsRemote(retrofit.newBuilder().client(slow).build(), token)
    }
}
