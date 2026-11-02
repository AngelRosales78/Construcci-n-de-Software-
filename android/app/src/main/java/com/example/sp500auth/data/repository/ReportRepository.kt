package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

data class ReportRequest(val request_id: String = UUID.randomUUID().toString(), val kind: String = "company", val a: String,
    val b: String? = null, val fiscal_year: Int, val range: String = "1Y", val include_charts: Boolean = false,
    val include_analysis: Boolean = false, val analysis_level: String = "simple", val analysis_version:String?=null, val analysis_origin:String?=null)
data class ReportJob(val schema_version: String? = null, val owner_id: String? = null, val id: String? = null,
    val status: String? = null, val request: ReportRequest? = null, val data_version: String? = null,
    val content_sha256: String? = null, val size_bytes: Long? = null, val message: String? = null, val progress: Int? = null,
    val chart_format: String? = null, val chart_range: String? = null, val analysis_version:String?=null, val analysis_origin:String?=null,
    val page_count:Int?=null,val orientation:String?=null)
data class PdfPayload(val bytes: ByteArray, val sha256: String, val request: ReportRequest)
fun validReportRequest(r: ReportRequest): Boolean = validTicker(r.a) && r.fiscal_year in 2000..2100 && r.range in historyRanges &&
    r.analysis_level in setOf("simple","technical") && r.request_id.matches(Regex("[a-zA-Z0-9-]{1,80}")) &&
    ((r.kind == "company" && r.b == null) || (r.kind == "comparison" && validTicker(r.b.orEmpty()) && r.a != r.b)) &&
    (!r.include_analysis || (!r.analysis_version.isNullOrBlank() && r.analysis_origin in setOf("ai","rules","mixed")))
fun validReportJob(job: ReportJob, user: String, request: ReportRequest, expectedId: String? = null): Boolean =
    job.schema_version == "report-job-v1" && job.owner_id == user && job.request == request &&
    job.id?.matches(Regex("[A-Za-z0-9_-]{1,128}")) == true && (expectedId == null || job.id == expectedId) &&
    job.status in setOf("queued","running","ready","error","cancelled") && (job.progress == null || job.progress in 0..100) &&
    (job.status != "ready" || (!job.data_version.isNullOrBlank() && job.content_sha256?.matches(Regex("[a-f0-9]{64}")) == true && (job.size_bytes ?: 0L) in 1L..20_000_000L &&
        (!request.include_charts || (job.chart_format == "vector" && job.chart_range == request.range)) &&
        (!request.include_analysis || (job.analysis_version==request.analysis_version && job.analysis_origin==request.analysis_origin)) &&
        (request.kind!="comparison" || (job.page_count in 1..2 && job.orientation=="landscape"))))
fun verifyPdf(bytes: ByteArray, expectedHash: String, expectedSize: Long): Boolean {
    if (bytes.size.toLong() != expectedSize || bytes.size !in 12..20_000_000 || !bytes.copyOfRange(0,5).contentEquals("%PDF-".toByteArray())) return false
    if (!String(bytes.takeLast(1024).toByteArray(), Charsets.ISO_8859_1).contains("%%EOF")) return false
    return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) } == expectedHash
}
interface ReportApi {
    @POST("reports/jobs/") suspend fun submit(@Body request: ReportRequest, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ReportJob>
    @GET("reports/jobs/{id}/") suspend fun status(@Path("id") id: String, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ReportJob>
    @Streaming @GET("reports/jobs/{id}/file/") suspend fun file(@Path("id") id: String, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ResponseBody>
}
interface ReportSource {
    fun owner(): String?
    suspend fun submit(request: ReportRequest): Resource<ReportJob>
    suspend fun status(id: String, request: ReportRequest): Resource<ReportJob>
    suspend fun download(job: ReportJob): Resource<PdfPayload>
}
class ReportRepository @Inject constructor(private val remote: ScopedRemote, private val access:AccessRepository) : ReportSource {
    private val api = remote.retrofit.create(ReportApi::class.java)
    override fun owner() = remote.owner()
    override suspend fun submit(request: ReportRequest): Resource<ReportJob> {
        if (!validReportRequest(request)) return Resource.Error("Revisa empresa, ejercicio y opciones de exportación.")
        if(request.kind=="comparison") {
            val plan=access.load()
            if(plan !is Resource.Success || !canCompare(plan.successData)) return Resource.Error(plan.message ?: "El reporte comparativo requiere un plan Pro activo.")
        }
        return remote.call({ job, user -> validReportJob(job,user,request) }) { token, account -> api.submit(request,token,account) }
    }
    override suspend fun status(id: String, request: ReportRequest): Resource<ReportJob> {
        if (!id.matches(Regex("[A-Za-z0-9_-]{1,128}"))) return Resource.Error("Identificador de trabajo no válido.")
        return remote.call({ job, user -> validReportJob(job,user,request,id) }) { token, account -> api.status(id,token,account) }
    }
    override suspend fun download(job: ReportJob): Resource<PdfPayload> = withContext(Dispatchers.IO) {
        val user = owner(); val request = job.request
        if (user == null || request == null || job.status != "ready" || !validReportJob(job,user,request)) return@withContext Resource.Error("El reporte todavía no está listo o no pertenece a esta cuenta.")
        if(request.kind=="comparison") {
            val plan=access.load()
            if(plan !is Resource.Success || !canCompare(plan.successData)) return@withContext Resource.Error(plan.message ?: "Tu plan no permite descargar esta comparación.")
        }
        val result = remote.call({ body: ResponseBody, _ -> body.contentType()?.let { it.type == "application" && it.subtype == "pdf" } == true }) { token, account -> api.file(job.id!!,token,account) }
        if (result !is Resource.Success) return@withContext Resource.Error(result.message ?: "No se pudo descargar el PDF.")
        try {
            val bytes = result.successData.use { body ->
                val out = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                body.byteStream().use { input ->
                    while (true) { coroutineContext.ensureActive(); val n = input.read(buffer); if (n < 0) break; if (out.size() + n > 20_000_000) throw IllegalArgumentException("El archivo supera el límite de descarga."); out.write(buffer,0,n) }
                }; out.toByteArray()
            }
            if (owner() != user || !verifyPdf(bytes,job.content_sha256!!,job.size_bytes!!)) Resource.Error("El PDF no coincide con el archivo completo anunciado por el servidor.")
            else Resource.Success(PdfPayload(bytes,job.content_sha256,request))
        } catch (e: CancellationException) { throw e } catch (_: Exception) { Resource.Error("La descarga no se completó. Puedes volver a intentarlo.") }
    }
}
