package com.example.sp500auth.data.ops

import com.example.sp500auth.data.api.AccountBinding
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

/** Épica 15. Every figure is computed by the server; the phone only presents it. */
data class AiRow(val provider: String? = null, val model: String? = null, val calls: Int? = null, val errors: Int? = null,
    val input_tokens: Long? = null, val output_tokens: Long? = null, val unreported_tokens: Int? = null)
data class Amount(val currency: String? = null, val amount: String? = null)
data class AiUsage(
    val schema_version: String? = null, val start: String? = null, val end: String? = null, val timezone: String? = null, val generated_at: String? = null,
    val calls: Int? = null, val breakdown: List<AiRow>? = null, val estimated_costs: List<Amount>? = null, val unpriced_calls: Int? = null, val explanation: String? = null
)
data class PlanShare(val plan: String? = null, val count: Int? = null, val percentage: String? = null)
data class PlanDistribution(val schema_version: String? = null, val generated_at: String? = null, val total: Int? = null, val blocked: Int? = null,
    val rule: String? = null, val results: List<PlanShare>? = null)
data class Budget(
    val provider: String? = null, val limit: Int? = null, val used: Int? = null, val version: Int? = null, val percentage: String? = null,
    val window_start: String? = null, val resets_at: String? = null, val timezone: String? = null, val status: String? = null,
    val warned_at: String? = null, val kind: String? = null
)
data class Budgets(val schema_version: String? = null, val results: List<Budget>? = null, val explanation: String? = null)
data class BudgetChange(val version: Int, val daily_limit: Int, val reset_timezone: String)
data class MonthlyValue(
    val schema_version: String? = null, val month: String? = null, val generated_at: String? = null, val simulated: Boolean? = null,
    val plans_included: Int? = null, val transactions: Int? = null, val results: List<Amount>? = null, val explanation: String? = null
)
data class ErrorLog(val id: Long? = null, val created_at: String? = null, val level: String? = null, val component: String? = null,
    val code: String? = null, val summary: String? = null, val context: Map<String, Any?>? = null)
data class ErrorPage(val count: Int? = null, val next: String? = null, val previous: String? = null, val results: List<ErrorLog>? = null,
    val retention_days: Int? = null, val generated_at: String? = null)

/** Context keys the server is allowed to publish; anything else is never shown on the phone. */
private val safeKeys = linkedMapOf("http_status" to "Estado HTTP", "attempt" to "Intento", "retryable" to "Admite reintento")
fun safeContext(log: ErrorLog?): Map<String, String> = safeKeys.mapNotNull { (key, label) ->
    when (val value = log?.context?.get(key)) {
        is Boolean -> label to if (value) "Sí" else "No"
        is Number -> label to value.toLong().toString()
        else -> null
    }
}.toMap()

interface MonitoringApi {
    @GET("admin/monitoring/ai/")
    suspend fun ai(@Query("start") start: String?, @Query("end") end: String?, @Query("provider") provider: String?, @Query("model") model: String?,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<AiUsage>

    @GET("admin/monitoring/plans/")
    suspend fun plans(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<PlanDistribution>

    @GET("admin/monitoring/budgets/")
    suspend fun budgets(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<Budgets>

    @PATCH("admin/monitoring/budgets/{provider}/")
    suspend fun saveBudget(@Path("provider") provider: String, @Body change: BudgetChange,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<Budget>

    @GET("admin/monitoring/subscriptions/")
    suspend fun monthly(@Query("month") month: String, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<MonthlyValue>

    @GET("admin/monitoring/errors/")
    suspend fun errors(@Query("level") level: String, @Query("component") component: String, @Query("page") page: Int,
        @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ErrorPage>

    @GET("admin/monitoring/errors/{id}/")
    suspend fun error(@Path("id") id: Long, @Header("Authorization") token: String, @Tag account: AccountBinding): Response<ErrorLog>
}

interface MonitoringSource {
    /** Without dates the server answers with its own current month, which also tells its date. */
    suspend fun ai(start: String?, end: String?, provider: String?, model: String?): Outcome<AiUsage>
    suspend fun plans(): Outcome<PlanDistribution>
    suspend fun budgets(): Outcome<Budgets>
    suspend fun saveBudget(provider: String, change: BudgetChange): Outcome<Budget>
    suspend fun monthly(month: String): Outcome<MonthlyValue>
    suspend fun errors(level: String, component: String, page: Int): Outcome<ErrorPage>
    suspend fun error(id: Long): Outcome<ErrorLog>
}

fun validBudget(row: Budget?) = !row?.provider.isNullOrBlank() && (row?.limit ?: 0) > 0 && (row?.used ?: -1) >= 0 &&
    row?.version != null && row.status in setOf("normal", "warning", "exhausted")
private fun validLog(row: ErrorLog?) = row?.id != null && !row.created_at.isNullOrBlank() && row.level in setOf("warning", "error")

class MonitoringRepository @Inject constructor(private val remote: OpsRemote) : MonitoringSource {
    private val api = remote.retrofit.create(MonitoringApi::class.java)

    override suspend fun ai(start: String?, end: String?, provider: String?, model: String?) = remote.call({ data: AiUsage, _ ->
        data.schema_version == "ai-usage-v1" && data.end != null && (start == null || data.start == start) && (end == null || data.end == end) && (data.calls ?: -1) >= 0 && data.breakdown != null
    }) { token, account -> api.ai(start, end, provider, model, token, account) }

    override suspend fun plans() = remote.call({ data: PlanDistribution, _ ->
        data.schema_version == "plan-distribution-v1" && (data.total ?: -1) >= 0 && data.results.orEmpty().sumOf { it.count ?: -1 } == data.total
    }) { token, account -> api.plans(token, account) }

    override suspend fun budgets() = remote.call({ data: Budgets, _ ->
        data.schema_version == "provider-budgets-v1" && data.results != null && data.results.all(::validBudget)
    }) { token, account -> api.budgets(token, account) }

    override suspend fun saveBudget(provider: String, change: BudgetChange) = remote.call({ data: Budget, _ ->
        validBudget(data) && data.provider == provider
    }) { token, account -> api.saveBudget(provider, change, token, account) }

    override suspend fun monthly(month: String) = remote.call({ data: MonthlyValue, _ ->
        data.schema_version == "monthly-subscription-value-v1" && data.month == month && data.simulated == true && data.results != null
    }) { token, account -> api.monthly(month, token, account) }

    override suspend fun errors(level: String, component: String, page: Int) = remote.call({ data: ErrorPage, _ ->
        data.results != null && data.results.size <= 15 && data.results.all(::validLog)
    }) { token, account -> api.errors(level, component, page, token, account) }

    override suspend fun error(id: Long) = remote.call({ data: ErrorLog, _ -> validLog(data) && data.id == id }) { token, account ->
        api.error(id, token, account)
    }
}
