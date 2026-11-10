package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.analysis.validAnalysis
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

data class ReportAnalysis(val a: String, val b: String?, val year: Int, val level: String, val origin: String, val version: String)
interface ReportAnalysisApi {
    @POST("companies/{ticker}/analysis/") suspend fun get(@Path("ticker") ticker: String, @Body request: AnalysisRequest, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<AnalysisResponse>
}
class ReportAnalysisRepository @Inject constructor(remote: ScopedRemote, private val comparison: ConclusionRepository) {
    private val scoped=remote.longRunning(); private val api=scoped.retrofit.create(ReportAnalysisApi::class.java)
    fun owner()=scoped.owner()
    suspend fun load(a:String,b:String?,year:Int,level:String):Resource<ReportAnalysis> {
        if (b != null) return when(val result=comparison.loadForReport(a,b,year,level)) {
            is Resource.Success -> Resource.Success(ReportAnalysis(a,b,year,level,result.successData.generation_mode!!,"${result.successData.data_version}/${result.successData.generated_at}"))
            else -> Resource.Error(result.message ?: "No hay conclusión comparativa disponible.")
        }
        val request=AnalysisRequest(year,level)
        return when(val result=scoped.call({ data:AnalysisResponse,_ -> validAnalysis(data,a,request) && !data.generated_at.isNullOrBlank() && !data.metrics_version.isNullOrBlank() && !data.data_as_of.isNullOrBlank() }) { token,account -> api.get(a,request,token,account) }) {
            is Resource.Success -> Resource.Success(ReportAnalysis(a,null,year,level,
                if(result.successData.summary_origin=="ai") "ai" else if(result.successData.generation_mode=="ai") "mixed" else "rules",
                "${result.successData.metrics_version}/${result.successData.data_as_of}/${result.successData.generated_at}"))
            else -> Resource.Error(result.message ?: "No hay análisis disponible.")
        }
    }
}
