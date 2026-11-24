package com.example.sp500auth.data.api

import com.example.sp500auth.data.api.models.ComparisonResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface ComparisonApiService {
    @GET("compare/")
    suspend fun compare(@Query("a") a: String, @Query("b") b: String,
        @Query("fiscal_year") fiscalYear: Int, @Header("Authorization") authorization: String): Response<ComparisonResponse>

    @GET("compare/candidates/")
    suspend fun candidates(@Query("a") a: String, @Query("fiscal_year") fiscalYear: Int,
        @Header("Authorization") authorization: String): Response<com.example.sp500auth.data.api.models.CompareCandidates>
}
