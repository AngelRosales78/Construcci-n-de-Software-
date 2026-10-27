package com.example.sp500auth.data.api

import com.example.sp500auth.data.api.models.PillarsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PillarsApiService {
    @GET("companies/{ticker}/pillars/")
    suspend fun getPillars(
        @Path("ticker") ticker: String,
        @Query("fiscal_year") fiscalYear: Int,
        @Header("Authorization") authorization: String
    ): Response<PillarsResponse>
}
