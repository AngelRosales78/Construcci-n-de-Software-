package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.models.PriceSeries
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject

interface HistoryComparisonSource { suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> }
interface ComparisonHistoryApi {
    @GET("companies/{ticker}/history/")
    suspend fun history(@Path("ticker") ticker: String, @Query("range") range: String, @Header("Authorization") bearer: String): Response<PriceSeries>
}
class HistoryComparisonRepository internal constructor(retrofit: Retrofit, private val token: () -> String?) : HistoryComparisonSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    private val api = retrofit.create(ComparisonHistoryApi::class.java)
    override suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> = coroutineScope {
        if (!validTicker(a) || !validTicker(b) || a == b || range !in historyRanges) return@coroutineScope Resource.Error("Revisa las dos empresas y el rango histórico.")
        val access = token() ?: return@coroutineScope Resource.Error("Inicia sesión para consultar el histórico.")
        val left = async { remoteCall { api.history(a, range, "Bearer $access") } }
        val right = async { remoteCall { api.history(b, range, "Bearer $access") } }
        val l = left.await(); val r = right.await()
        if (l !is Resource.Success) return@coroutineScope Resource.Error("$a: ${l.message}")
        if (r !is Resource.Success) return@coroutineScope Resource.Error("$b: ${r.message}")
        if (l.successData.ticker != a || r.successData.ticker != b || l.successData.range != range || r.successData.range != range)
            return@coroutineScope Resource.Error("El histórico recibido corresponde a otra selección.")
        try { Resource.Success(alignHistory(l.successData, r.successData)) }
        catch (e: IllegalArgumentException) { Resource.Error(e.message ?: "No se pueden comparar estos históricos.") }
    }
}
