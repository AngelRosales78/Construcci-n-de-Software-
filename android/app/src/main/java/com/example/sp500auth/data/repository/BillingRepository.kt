package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import javax.inject.Inject

/** Epic 11 contracts: plans-v1, payment-v1 and invoices-v1. Payments are simulated; no real charge exists. */
data class PlanFeature(val id: String? = null, val label: String? = null, val included: Boolean? = null)
data class PlanInfo(val id: String? = null, val name: String? = null, val price: String? = null, val popular: Boolean? = null,
    val description: String? = null, val features: List<PlanFeature>? = null)
data class PlansInfo(val schema_version: String? = null, val currency: String? = null, val period: String? = null, val plans: List<PlanInfo>? = null)
data class InvoiceInfo(val number: String? = null, val transaction_id: String? = null, val kind: String? = null, val plan: String? = null,
    val amount: String? = null, val currency: String? = null, val card_brand: String? = null, val card_last4: String? = null,
    val period_start: String? = null, val period_end: String? = null, val created_at: String? = null,
    val emailed: Boolean? = null, val simulated: Boolean? = null)
data class InvoiceList(val schema_version: String? = null, val owner_id: String? = null, val results: List<InvoiceInfo>? = null)
data class PaymentRequest(val request_id: String, val card_number: String, val holder: String,
    val exp_month: Int, val exp_year: Int, val cvc: String)
data class PaymentResult(val schema_version: String? = null, val owner_id: String? = null,
    val invoice: InvoiceInfo? = null, val subscription: AccessInfo? = null)
data class CancelRequest(val reason: String, val comment: String = "")

interface BillingApi {
    @GET("payments/plans/") suspend fun plans(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<PlansInfo>
    @POST("payments/simulate/") suspend fun pay(@Header("Authorization") token: String, @Body body: PaymentRequest, @Tag account: AccountBinding): Response<PaymentResult>
    @GET("payments/invoices/") suspend fun invoices(@Header("Authorization") token: String, @Tag account: AccountBinding): Response<InvoiceList>
    @GET("payments/invoices/{number}/") suspend fun invoice(@Header("Authorization") token: String, @Path("number") number: String, @Tag account: AccountBinding): Response<InvoiceInfo>
    @POST("subscription/cancel/") suspend fun cancel(@Header("Authorization") token: String, @Body body: CancelRequest, @Tag account: AccountBinding): Response<AccessInfo>
}

interface BillingSource {
    fun owner(): String?
    suspend fun plans(): Resource<PlansInfo>
    suspend fun pay(request: PaymentRequest): Resource<PaymentResult>
    suspend fun invoices(): Resource<InvoiceList>
    suspend fun invoice(number: String): Resource<InvoiceInfo>
    suspend fun cancel(reason: String, comment: String): Resource<AccessInfo>
}

internal fun validInvoice(invoice: InvoiceInfo?) = invoice != null && !invoice.number.isNullOrBlank() &&
    !invoice.transaction_id.isNullOrBlank() && invoice.amount?.toBigDecimalOrNull() != null && invoice.period_end != null
internal fun validSubscription(info: AccessInfo?, owner: String) = info != null && info.schema_version == "subscription-v1" &&
    info.owner_id == owner && info.plan in setOf("free", "pro") && info.active != null && info.features != null

class BillingRepository @Inject constructor(private val remote: ScopedRemote) : BillingSource {
    private val api = remote.retrofit.create(BillingApi::class.java)
    override fun owner() = remote.owner()
    override suspend fun plans() = remote.call({ data: PlansInfo, _ -> data.schema_version == "plans-v1" &&
        data.plans.orEmpty().map { it.id }.containsAll(listOf("free", "pro")) }) { t, a -> api.plans(t, a) }
    override suspend fun pay(request: PaymentRequest) = remote.call({ data: PaymentResult, owner ->
        data.schema_version == "payment-v1" && data.owner_id == owner && validInvoice(data.invoice) && validSubscription(data.subscription, owner) }) { t, a -> api.pay(t, request, a) }
    override suspend fun invoices() = remote.call({ data: InvoiceList, owner ->
        data.schema_version == "invoices-v1" && data.owner_id == owner && data.results.orEmpty().all(::validInvoice) }) { t, a -> api.invoices(t, a) }
    override suspend fun invoice(number: String) = remote.call({ data: InvoiceInfo, _ -> validInvoice(data) && data.number == number }) { t, a -> api.invoice(t, number, a) }
    override suspend fun cancel(reason: String, comment: String) = remote.call({ data: AccessInfo, owner -> validSubscription(data, owner) }) { t, a -> api.cancel(t, CancelRequest(reason, comment), a) }
}
