package com.example.sp500auth.data.repository

import com.example.sp500auth.data.local.TokenManager
import com.google.gson.JsonParser
import retrofit2.Retrofit
import java.util.Base64
import javax.inject.Inject

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

class FavoritesRepository internal constructor(private val retrofit: Retrofit, private val token: () -> String?) : FavoritesDataSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    override fun owner(): String? = favoritesOwner(token())
    override suspend fun list(): FavoriteResult<List<FavoriteItem>> = FavoriteResult.Failure("El servicio de favoritos todavía no está conectado.")
    override suspend fun add(ticker: String): FavoriteResult<FavoriteItem> = FavoriteResult.Failure("El servicio de favoritos todavía no está conectado. No se guardó el cambio.")
}
