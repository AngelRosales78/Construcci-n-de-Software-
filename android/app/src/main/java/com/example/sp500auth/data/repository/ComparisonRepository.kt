package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.SprintApiService
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.util.Resource
import retrofit2.Retrofit
import javax.inject.Inject

interface ComparisonDataSource {
    suspend fun search(query: String): Resource<List<CompanyItem>>
}

class ComparisonRepository @Inject constructor(retrofit: Retrofit) : ComparisonDataSource {
    private val catalog = retrofit.create(SprintApiService::class.java)

    override suspend fun search(query: String): Resource<List<CompanyItem>> =
        when (val result = remoteCall { catalog.search(query.take(200), 1) }) {
            is Resource.Success -> Resource.Success(result.successData.results.orEmpty().take(5))
            is Resource.Error -> Resource.Error(result.message.orEmpty())
            else -> Resource.Error("No se pudo buscar empresas.")
        }
}
