package com.example.sp500auth.data.api
import com.example.sp500auth.data.api.models.*
import retrofit2.Response
import retrofit2.http.*
interface AnalysisApiService {
    @POST("companies/{ticker}/analysis/") suspend fun analysis(@Path("ticker") ticker: String,@Body request: AnalysisRequest): Response<AnalysisResponse>
}
