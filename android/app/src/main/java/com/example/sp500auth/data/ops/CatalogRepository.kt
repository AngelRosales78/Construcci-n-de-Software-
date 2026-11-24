package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

/** Épica 14. Being in the catalogue or in the pilot never implies that the five pillars are available. */
data class Coverage(
    val summary_fields: Int? = null, val summary_total: Int? = null, val summary_as_of: String? = null,
    val fiscal_years: List<Int>? = null, val price_as_of: String? = null, val sources: List<String>? = null, val has_data: Boolean? = null
)

enum class CoverageLevel(val label: String) { COMPLETE("Completa"), PARTIAL("Parcial"), MISSING("Ausente") }

fun coverageLevel(coverage: Coverage?): CoverageLevel = when {
    coverage?.has_data != true -> CoverageLevel.MISSING
    coverageGaps(coverage).isEmpty() -> CoverageLevel.COMPLETE
    else -> CoverageLevel.PARTIAL
}

/** What the server reports as missing; nothing is inferred beyond the published counters. */
fun coverageGaps(coverage: Coverage?): List<String> = if (coverage == null) listOf("El servidor no informó la cobertura") else buildList {
    val fields = coverage.summary_fields ?: 0
    val total = coverage.summary_total ?: 5
    if (fields < total) add("Faltan ${total - fields} de $total indicadores de la ficha")
    if (coverage.fiscal_years.isNullOrEmpty()) add("Sin balances anuales cargados")
    if (coverage.price_as_of == null) add("Sin precios históricos")
}

data class MarketOperation(
    val id: String? = null, val status: String? = null, val kind: String? = null, val ticker: String? = null, val company_id: Long? = null,
    val started_at: String? = null, val finished_at: String? = null, val expires_at: String? = null, val message: String? = null,
    val error_code: String? = null, val result: JsonObject? = null
) {
    fun text(key: String): String? = runCatching { result?.get(key)?.takeIf { it.isJsonPrimitive }?.asString }.getOrNull()
}

data class ProviderUsage(val provider: String? = null, val calls: Int? = null, val limit: Int? = null, val reset_at: String? = null,
    val budget_kind: String? = null, val window_timezone: String? = null)
data class OperationAnswer(val operation: MarketOperation? = null, val provider: ProviderUsage? = null, val detail: String? = null)

data class AdminCompany(
    val id: Long? = null, val ticker: String? = null, val name: String? = null, val sector: String? = null, val sector_display: String? = null,
    val is_active: Boolean? = null, val is_pilot: Boolean? = null, val description: String? = null, val website_url: String? = null,
    val is_index_member: Boolean? = null, val last_update: MarketOperation? = null, val updated_at: String? = null, val coverage: Coverage? = null
)
data class PilotInfo(val selected: Int? = null, val target: Int? = null, val selection_status: String? = null, val tickers: List<String>? = null)
data class CatalogPage(val count: Int? = null, val next: String? = null, val previous: String? = null, val results: List<AdminCompany>? = null,
    val pilot: PilotInfo? = null, val market_provider: ProviderUsage? = null)

data class RefreshBody(val request_id: String)
data class VerifyBody(val request_id: String, val ticker: String, val sector: String)
data class CreateBody(val verification_id: String)

val catalogSectors = linkedMapOf(
    "technology" to "Tecnología", "healthcare" to "Salud", "financials" to "Finanzas", "consumer_discretionary" to "Consumo discrecional",
    "consumer_staples" to "Consumo básico", "industrials" to "Industria", "energy" to "Energía", "materials" to "Materiales",
    "utilities" to "Servicios públicos", "real_estate" to "Inmuebles", "communication_services" to "Comunicaciones")

val ADMIN_TICKER = Regex("[A-Z][A-Z0-9.-]{0,9}")

fun validAdminCompany(company: AdminCompany?) =
    company?.id != null && !company.ticker.isNullOrBlank() && !company.name.isNullOrBlank() && company.is_active != null
fun validOperation(operation: MarketOperation?) =
    !operation?.id.isNullOrBlank() && operation?.status in setOf("running", "ready", "succeeded", "failed")

/** The server answers failed consultations with an HTTP error that still carries the operation. */
fun failedOperation(fail: Outcome.Fail): OperationAnswer? = runCatching {
    fail.body?.takeIf { it.has("operation") }?.let { Gson().fromJson(it, OperationAnswer::class.java) }
}.getOrNull()?.takeIf { validOperation(it.operation) }

interface CatalogApi {
    @GET("admin/companies/")
    suspend fun list(@Query("q") q: String, @Query("state") state: String, @Query("scope") scope: String, @Query("coverage") coverage: String,
        @Query("page") page: Int, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<CatalogPage>

    @PATCH("admin/companies/{id}/")
    suspend fun update(@Path("id") id: Long, @Body change: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<AdminCompany>

    @POST("admin/companies/{id}/refresh/")
    suspend fun refresh(@Path("id") id: Long, @Body body: RefreshBody, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<OperationAnswer>

    @GET("admin/market-operations/{id}/")
    suspend fun operation(@Path("id") id: String, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<OperationAnswer>

    @POST("admin/companies/verify/")
    suspend fun verify(@Body body: VerifyBody, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<OperationAnswer>

    @POST("admin/companies/")
    suspend fun create(@Body body: CreateBody, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<OperationAnswer>
}

interface CatalogSource {
    suspend fun list(q: String, state: String, scope: String, coverage: String, page: Int): Outcome<CatalogPage>
    suspend fun update(id: Long, change: Map<String, Any>): Outcome<AdminCompany>
    suspend fun refresh(id: Long, requestId: String): Outcome<OperationAnswer>
    suspend fun operation(id: String): Outcome<OperationAnswer>
    suspend fun verify(requestId: String, ticker: String, sector: String): Outcome<OperationAnswer>
    suspend fun create(verificationId: String): Outcome<OperationAnswer>
}

class CatalogRepository @Inject constructor(private val remote: OpsRemote) : CatalogSource {
    private val api = remote.retrofit.create(CatalogApi::class.java)
    private val slow = remote.longRunning()
    private val slowApi = slow.retrofit.create(CatalogApi::class.java)

    private fun validPage(data: CatalogPage, user: String): Boolean {
        val rows = data.results ?: return false
        return user.isNotBlank() && rows.size <= 20 && rows.all(::validAdminCompany) && rows.map { it.id }.distinct().size == rows.size
    }
    private fun validAnswer(answer: OperationAnswer, user: String) = user.isNotBlank() && validOperation(answer.operation)

    override suspend fun list(q: String, state: String, scope: String, coverage: String, page: Int) =
        remote.call(::validPage) { token, account -> api.list(q.trim().take(120), state, scope, coverage, page, token, account) }
    override suspend fun update(id: Long, change: Map<String, Any>) =
        remote.call({ data: AdminCompany, _ -> validAdminCompany(data) && data.id == id }) { token, account -> api.update(id, change, token, account) }
    override suspend fun refresh(id: Long, requestId: String) =
        slow.call(::validAnswer) { token, account -> slowApi.refresh(id, RefreshBody(requestId), token, account) }
    override suspend fun operation(id: String) =
        remote.call(::validAnswer) { token, account -> api.operation(id, token, account) }
    override suspend fun verify(requestId: String, ticker: String, sector: String) =
        slow.call(::validAnswer) { token, account -> slowApi.verify(VerifyBody(requestId, ticker, sector), token, account) }
    override suspend fun create(verificationId: String) =
        slow.call(::validAnswer) { token, account -> slowApi.create(CreateBody(verificationId), token, account) }
}
