package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.SprintApiService
import com.example.sp500auth.data.api.ComparisonApiService
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.data.api.models.ComparisonResponse
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.ui.compare.validTicker
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
        return when (val result = remoteCall { comparison.compare(a, b, fiscalYear, "Bearer $token") }) {
            is Resource.Success -> {
                val body = result.successData
                if (body.schema_version != "comparison-v1" || body.fiscal_year != fiscalYear ||
                    body.a?.ticker != a || body.b?.ticker != b ||
                    body.a?.fiscal_year != fiscalYear || body.b?.fiscal_year != fiscalYear)
                    Resource.Error("La respuesta no corresponde a las dos empresas y al ejercicio solicitados.")
                else result
            }
            else -> result
        }
    }
}
