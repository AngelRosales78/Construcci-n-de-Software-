package com.example.sp500auth.data.api
import com.example.sp500auth.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface SprintApiService {
    @GET("companies/search/") suspend fun search(@Query("q") query: String, @Query("page") page: Int): Response<CompanyPage>
    @GET("companies/{ticker}/summary/") suspend fun summary(@Path("ticker") ticker: String): Response<CompanySummary>
    @GET("companies/{ticker}/history/") suspend fun history(@Path("ticker") ticker: String, @Query("range") range: String): Response<PriceSeries>
}
