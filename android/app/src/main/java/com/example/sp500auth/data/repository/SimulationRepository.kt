package com.example.sp500auth.data.repository
import com.example.sp500auth.data.api.SimulationApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.scenarios.*
import com.example.sp500auth.util.Resource
import retrofit2.Retrofit
import javax.inject.Inject

interface SimulationSource {
    suspend fun defaults(ticker: String): Resource<SimulationDefaults>
    suspend fun simulate(ticker: String,request: SimulationRequest): Resource<SimulationResponse>
}
class SimulationRepository @Inject constructor(retrofit: Retrofit): SimulationSource {
    private val api=retrofit.create(SimulationApiService::class.java)
    override suspend fun defaults(ticker: String): Resource<SimulationDefaults> {
        val result=remoteCall{api.defaults(ticker)}
        return if(result is Resource.Success && !validDefaults(result.successData,ticker)) Resource.Error("El servidor no entregó límites y supuestos válidos.") else result
    }
    override suspend fun simulate(ticker: String,request: SimulationRequest): Resource<SimulationResponse> {
        val result=remoteCall{api.simulate(ticker,request)}
        return if(result is Resource.Success && !validSimulation(result.successData,ticker,request)) Resource.Error("Se rechazó una simulación con fechas, percentiles o parámetros incoherentes.") else result
    }
}
