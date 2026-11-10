package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.SprintApiService
import com.example.sp500auth.data.api.ComparisonApiService
import com.example.sp500auth.data.api.PillarsApiService
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.data.api.models.ComparisonResponse
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.ui.compare.validTicker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.io.IOException
import com.example.sp500auth.util.Resource
import retrofit2.Retrofit
import javax.inject.Inject

interface ComparisonDataSource {
    suspend fun search(query: String): Resource<List<CompanyItem>>
    suspend fun compare(a: String, b: String, fiscalYear: Int): Resource<ComparisonResponse>
}

class ComparisonRepository internal constructor(retrofit: Retrofit, private val accessToken: () -> String?) : ComparisonDataSource {
    @Inject constructor(retrofit: Retrofit, tokens: TokenManager) : this(retrofit, { tokens.getAccessToken() })
    private val catalog = retrofit.create(SprintApiService::class.java)
    private val comparison = retrofit.create(ComparisonApiService::class.java)
    private val pillars = retrofit.create(PillarsApiService::class.java)

    override suspend fun search(query: String): Resource<List<CompanyItem>> =
        when (val result = remoteCall { catalog.search(query.take(200), 1) }) {
            is Resource.Success -> Resource.Success(result.successData.results.orEmpty().take(5))
            is Resource.Error -> Resource.Error(result.message.orEmpty())
            else -> Resource.Error("No se pudo buscar empresas.")
        }

    override suspend fun compare(a: String, b: String, fiscalYear: Int): Resource<ComparisonResponse> {
        if (!validTicker(a) || !validTicker(b) || a == b || fiscalYear !in 2000..2100)
            return Resource.Error("Elige dos empresas distintas y un ejercicio válido.")
        val token = accessToken() ?: return Resource.Error("Inicia sesión para comparar empresas.")
        return try {
            val response = comparison.compare(a, b, fiscalYear, "Bearer $token")
            if (response.code() in setOf(404, 501)) {
                response.errorBody()?.close()
                return independentPillars(a, b, fiscalYear, token)
            }
            when (val result = remoteCall { response }) {
            is Resource.Success -> {
                val body = result.successData
                if (body.schema_version != "comparison-v1" || body.fiscal_year != fiscalYear ||
                    body.a?.schema_version != "pillars-v1" || body.b?.schema_version != "pillars-v1" ||
                    body.a?.ticker != a || body.b?.ticker != b ||
                    body.a?.fiscal_year != fiscalYear || body.b?.fiscal_year != fiscalYear)
                    Resource.Error("La respuesta no corresponde a las dos empresas y al ejercicio solicitados.")
                else result
            }
            else -> result
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: IOException) {
            Resource.Error("No se pudo conectar con el servidor. Conservamos tu selección; puedes reintentar.")
        } catch (_: Exception) {
            Resource.Error("La respuesta del comparador no se pudo interpretar.")
        }
    }

    /** Existing server facts remain usable, without impersonating a comparison decision. */
    private suspend fun independentPillars(a: String, b: String, year: Int, token: String): Resource<ComparisonResponse> = coroutineScope {
        val left = async { remoteCall { pillars.getPillars(a, year, "Bearer $token") } }
        val right = async { remoteCall { pillars.getPillars(b, year, "Bearer $token") } }
        val aResult = left.await(); val bResult = right.await()
        if (aResult !is Resource.Success) return@coroutineScope Resource.Error("$a: ${aResult.message ?: "No se pudieron consultar sus pilares."}")
        if (bResult !is Resource.Success) return@coroutineScope Resource.Error("$b: ${bResult.message ?: "No se pudieron consultar sus pilares."}")
        val aData = aResult.successData; val bData = bResult.successData
        if (aData.schema_version != "pillars-v1" || bData.schema_version != "pillars-v1" ||
            aData.ticker != a || bData.ticker != b || aData.fiscal_year != year || bData.fiscal_year != year)
            return@coroutineScope Resource.Error("El servidor devolvió pilares de otra empresa, ejercicio o formato.")
        Resource.Success(ComparisonResponse("independent-pillars-v1", year, aData, bData, emptyList(),
            "comparison_service_unavailable", "El servicio comparativo todavía no está disponible. Se muestran dos consultas reales de pilares; no se asignan ganadores ni diferencias."))
    }
}
