package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.time.LocalDate
import javax.inject.Inject

interface HistoryComparisonSource { suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> }
data class SharedHistoryPoint(val date: String, val a: Double, val b: Double, val price_a: Double, val price_b: Double)
data class SharedHistory(val schema_version: String?, val owner_id: String?, val a: String?, val b: String?, val range: String?,
    val status: String?, val reason: String?, val base_date: String?, val points: List<SharedHistoryPoint>?,
    val unit: String?, val sources: List<String>?, val is_demo: Boolean?)
interface ComparisonHistoryApi {
    @GET("compare/history/")
    suspend fun history(@Query("a") a: String, @Query("b") b: String, @Query("range") range: String,
        @Query("fiscal_year") year: Int, @Header("Authorization") bearer: String, @Tag account: AccountBinding): Response<SharedHistory>
}
class HistoryComparisonRepository internal constructor(retrofit: Retrofit, token: () -> String?) : HistoryComparisonSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    private val remote = ScopedRemote(retrofit, token)
    private val api = retrofit.create(ComparisonHistoryApi::class.java)
    override suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> {
        if (!validTicker(a) || !validTicker(b) || a == b || range !in historyRanges) return Resource.Error("Revisa las dos empresas y el rango histórico.")
        val result = remote.call({ data: SharedHistory, owner ->
            data.schema_version == "comparison-history-v1" && data.owner_id == owner && data.a == a && data.b == b && data.range == range
        }) { token, account -> api.history(a, b, range, LocalDate.now().year, token, account) }
        if (result !is Resource.Success) return Resource.Error(result.message ?: "No se pudo consultar el histórico.")
        val data = result.successData
        if (data.status != "available") return Resource.Error(data.reason ?: "No hay suficientes cierres comunes.")
        return try {
            require(data.unit == "percent" && data.is_demo == false && !data.sources.isNullOrEmpty())
            val points = data.points.orEmpty().map {
                require(it.a.isFinite() && it.b.isFinite() && it.price_a.isFinite() && it.price_b.isFinite() && it.price_a > 0 && it.price_b > 0)
                ReturnPoint(it.date, LocalDate.parse(it.date).toEpochDay(), it.a, it.b, it.price_a, it.price_b)
            }
            require(points.size >= 2 && points.zipWithNext().all { (l,r) -> l.epochDay < r.epochDay })
            require(points.first().date == data.base_date && points.first().a == 0.0 && points.first().b == 0.0)
            Resource.Success(AlignedHistory(a,b,range,points,data.sources.orEmpty(),data.sources.orEmpty(),emptyList(),emptyList(),
                listOf("Fechas comunes y variación calculadas por el servidor desde el primer cierre compartido. Sin interpolación ni conversión de divisas.")))
        } catch (_: Exception) { Resource.Error("El histórico recibido está incompleto o contiene cierres no válidos.") }
    }
}
