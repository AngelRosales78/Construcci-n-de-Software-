package com.example.sp500auth.data.api
import com.example.sp500auth.data.api.models.*
import retrofit2.Response
import retrofit2.http.*
interface SimulationApiService {
    @GET("companies/{ticker}/simulations/defaults/") suspend fun defaults(@Path("ticker") ticker: String): Response<SimulationDefaults>
    @POST("companies/{ticker}/simulations/") suspend fun simulate(@Path("ticker") ticker: String,@Body request: SimulationRequest): Response<SimulationResponse>
}
