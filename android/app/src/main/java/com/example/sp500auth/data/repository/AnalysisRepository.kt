package com.example.sp500auth.data.repository
import com.example.sp500auth.data.api.AnalysisApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.analysis.validAnalysis
import com.example.sp500auth.util.Resource
import retrofit2.Retrofit
import javax.inject.Inject
interface AnalysisSource {
    suspend fun load(ticker: String,request: AnalysisRequest): Resource<AnalysisResponse>
}
class AnalysisRepository @Inject constructor(retrofit: Retrofit): AnalysisSource {
    private val api=retrofit.create(AnalysisApiService::class.java)
    override suspend fun load(ticker: String,request: AnalysisRequest): Resource<AnalysisResponse> {
        if(request.level !in setOf("simple","technical")) return Resource.Error("Nivel de lenguaje no admitido.")
        val result=remoteCall{api.analysis(ticker,request)}
        return if(result is Resource.Success && !validAnalysis(result.successData,ticker,request))
            Resource.Error("El análisis no tiene validación, fuentes o estructura suficientes y no se mostrará.") else result
    }
}
