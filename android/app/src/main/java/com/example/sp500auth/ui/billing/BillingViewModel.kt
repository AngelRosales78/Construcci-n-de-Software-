package com.example.sp500auth.ui.billing

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BillingViewModel internal constructor(private val source: BillingSource, private val newRequestId: () -> String) : ViewModel() {
    @Inject constructor(repository: BillingRepository) : this(repository as BillingSource, { UUID.randomUUID().toString() })

    private var owner: String? = null
    private fun <T> scoped(value: Resource<T>): Resource<T> = if (owner == source.owner()) value else Resource.Empty()

    private var plansResult by mutableStateOf<Resource<PlansInfo>>(Resource.Empty())
    private var invoicesResult by mutableStateOf<Resource<InvoiceList>>(Resource.Empty())
    private var receiptResult by mutableStateOf<Resource<InvoiceInfo>>(Resource.Empty())
    private var paymentResult by mutableStateOf<Resource<PaymentResult>>(Resource.Empty())
    private var cancelResult by mutableStateOf<Resource<AccessInfo>>(Resource.Empty())
    val plans get() = scoped(plansResult)
    val invoices get() = scoped(invoicesResult)
    val receipt get() = scoped(receiptResult)
    val payment get() = scoped(paymentResult)
    val cancellation get() = scoped(cancelResult)

    /** One id per checkout attempt: a retry after a lost answer cannot charge twice. */
    private var requestId: String? = null
    private var paymentJob: Job? = null

    private fun bind() { if (owner != source.owner()) { owner = source.owner(); requestId = null } }

    fun loadPlans() { bind(); val requested = owner; plansResult = Resource.Loading()
        viewModelScope.launch { val value = source.plans(); if (requested == source.owner()) plansResult = value } }

    fun loadInvoices() { bind(); val requested = owner; invoicesResult = Resource.Loading()
        viewModelScope.launch { val value = source.invoices(); if (requested == source.owner()) invoicesResult = value } }

    fun loadReceipt(number: String) {
        bind()
        val paid = (paymentResult as? Resource.Success)?.successData?.invoice
        if (paid?.number == number) { receiptResult = Resource.Success(paid); return }
        val requested = owner; receiptResult = Resource.Loading()
        viewModelScope.launch { val value = source.invoice(number); if (requested == source.owner()) receiptResult = value }
    }

    /** Called when the card data changes: a different card is a different payment attempt. */
    fun cardEdited() { if (paymentResult !is Resource.Loading) { requestId = null; if (paymentResult is Resource.Error) paymentResult = Resource.Empty() } }

    fun pay(number: String, holder: String, expiry: String, cvc: String) {
        if (paymentResult is Resource.Loading) return
        val errors = cardErrors(number, holder, expiry, cvc)
        if (!errors.valid) { paymentResult = Resource.Error("Revisa los datos marcados de la tarjeta."); return }
        bind(); val requested = owner; val month = parseExpiry(expiry)!!
        val id = requestId ?: newRequestId().also { requestId = it }
        paymentResult = Resource.Loading()
        paymentJob = viewModelScope.launch {
            val value = source.pay(PaymentRequest(id, cardDigits(number), holder.trim(), month.monthValue, month.year, cvc))
            if (requested != source.owner()) return@launch
            paymentResult = value
            if (value is Resource.Success) requestId = null
        }
    }

    fun paymentShown() { if (paymentResult is Resource.Success) paymentResult = Resource.Empty() }

    fun cancel(reason: String, comment: String) {
        if (cancelResult is Resource.Loading || reason !in cancelReasons) return
        bind(); val requested = owner; cancelResult = Resource.Loading()
        viewModelScope.launch { val value = source.cancel(reason, comment.trim().take(150)); if (requested == source.owner()) cancelResult = value }
    }

    fun cancellationShown() { if (cancelResult !is Resource.Loading) cancelResult = Resource.Empty() }
}
