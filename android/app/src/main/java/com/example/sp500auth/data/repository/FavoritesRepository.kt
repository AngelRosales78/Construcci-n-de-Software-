package com.example.sp500auth.data.repository

import com.example.sp500auth.data.local.TokenManager
import com.google.gson.JsonParser
import retrofit2.Retrofit
import java.util.Base64
import javax.inject.Inject
import com.example.sp500auth.data.api.*
import com.example.sp500auth.ui.compare.validTicker
import kotlinx.coroutines.CancellationException
import okhttp3.Request
import java.io.IOException

data class FavoriteItem(val id: Long? = null, val ticker: String? = null, val name: String? = null)
sealed class FavoriteResult<out T> {
    data class Success<T>(val value: T) : FavoriteResult<T>()
    data class Failure(val message: String, val clearPrivate: Boolean = false) : FavoriteResult<Nothing>()
}
interface FavoritesDataSource {
    fun owner(): String?
    suspend fun list(): FavoriteResult<List<FavoriteItem>>
    suspend fun add(ticker: String): FavoriteResult<FavoriteItem>
}

/** Only an account cache key, never an authorization decision. The server validates the JWT. */
fun favoritesOwner(token: String?): String? = try {
    val parts = token.orEmpty().split('.')
    if (parts.size != 3) null else JsonParser.parseString(String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8))
        .asJsonObject.get("user_id")?.asString?.takeIf { it.matches(Regex("[1-9][0-9]{0,17}")) }
} catch (_: Exception) { null }

fun enforceFavoriteAccount(request: Request, currentToken: String?) {
    val expected = request.tag(AccountBinding::class.java)?.owner ?: return
    val requestToken = request.header("Authorization")?.removePrefix("Bearer ")
    if (favoritesOwner(currentToken) != expected || favoritesOwner(requestToken) != expected)
        throw IOException("La cuenta cambió durante la operación de favoritos.")
}

class FavoritesRepository internal constructor(private val retrofit: Retrofit, private val token: () -> String?) : FavoritesDataSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    override fun owner(): String? = favoritesOwner(token())
    private val api = retrofit.create(FavoritesApiService::class.java)
    override suspend fun list(): FavoriteResult<List<FavoriteItem>> = safely {
        val access = token(); val user = favoritesOwner(access) ?: return@safely FavoriteResult.Failure("Inicia sesión para consultar favoritos.", true)
        val response = api.list("Bearer $access", AccountBinding(user))
        if (owner() != user) return@safely changedAccount()
        if (!response.isSuccessful) return@safely httpFailure(response.code())
        val body = response.body(); val results = body?.results
        if (body?.schema_version != "favorites-v1" || body.owner_id != user || results == null || results.size > 10000 ||
            results.any { !valid(it) } || results.map { it?.id }.distinct().size != results.size || results.map { it?.ticker }.distinct().size != results.size)
            return@safely FavoriteResult.Failure("La lista de favoritos no tiene un formato o propietario válido.", true)
        FavoriteResult.Success(results.filterNotNull())
    }
    override suspend fun add(ticker: String): FavoriteResult<FavoriteItem> = safely {
        if (!validTicker(ticker)) return@safely FavoriteResult.Failure("El símbolo no es válido.")
        val access = token(); val user = favoritesOwner(access) ?: return@safely FavoriteResult.Failure("Inicia sesión para guardar favoritos.", true)
        val response = api.add(AddFavoriteRequest(ticker), "Bearer $access", AccountBinding(user))
        if (owner() != user) return@safely changedAccount()
        if (response.code() == 409) {
            response.errorBody()?.close()
            val list = list()
            if (list is FavoriteResult.Success) {
                val existing = list.value.singleOrNull { it.ticker == ticker }
                return@safely if (existing != null) FavoriteResult.Success(existing) else FavoriteResult.Failure("El servidor rechazó un duplicado, pero no confirmó el favorito. Actualiza la lista.")
            }
            return@safely list as FavoriteResult.Failure
        }
        if (!response.isSuccessful) return@safely httpFailure(response.code())
        val body = response.body(); val item = body?.favorite
        if (body?.schema_version != "favorites-v1" || body.owner_id != user || !valid(item) || item?.ticker != ticker)
            return@safely FavoriteResult.Failure("El servidor no confirmó el favorito solicitado.")
        FavoriteResult.Success(item!!)
    }

    private fun valid(item: FavoriteItem?) = item != null && (item.id ?: 0) > 0 && validTicker(item.ticker.orEmpty())
    private fun changedAccount() = FavoriteResult.Failure("La cuenta cambió. Vuelve a cargar tus favoritos.", true)
    private fun httpFailure(code: Int) = FavoriteResult.Failure(when (code) {
        401, 403 -> "La sesión no permite esta operación. Vuelve a iniciar sesión."
        404, 501 -> "El servicio de favoritos todavía no está disponible en el servidor. No se confirmó ningún cambio."
        400 -> "El servidor rechazó los datos. Actualiza la lista y revisa la empresa."
        else -> "No se confirmó la operación de favoritos (HTTP $code). Actualiza la lista antes de repetirla."
    }, code in setOf(401, 403))
    private suspend fun <T> safely(block: suspend () -> FavoriteResult<T>): FavoriteResult<T> = try { block() }
    catch (e: CancellationException) { throw e }
    catch (_: IOException) { FavoriteResult.Failure("No se pudo confirmar la operación. Comprueba la conexión y actualiza la lista antes de repetirla.") }
    catch (_: Exception) { FavoriteResult.Failure("La respuesta de favoritos no se pudo interpretar. Actualiza la lista.") }
}
