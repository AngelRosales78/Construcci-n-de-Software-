package com.example.sp500auth.billing

import com.example.sp500auth.data.api.models.AltmanData
import com.example.sp500auth.data.api.models.CompanySummary
import com.example.sp500auth.data.api.models.FinancialProfile
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.auth.registrationPhoneError
import com.example.sp500auth.ui.billing.*
import com.example.sp500auth.ui.finance.profileLevel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BillingTest {
    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun luhnBrandAndFormatting() {
        assertTrue(luhnValid("4242424242424242"))
        assertTrue(luhnValid("5555555555554444"))
        assertTrue(luhnValid("378282246310005"))
        assertFalse(luhnValid("4242424242424241"))
        assertFalse(luhnValid("4242"))
        assertEquals("Visa", cardBrand("4242"))
        assertEquals("Mastercard", cardBrand("2221000000000009"))
        assertEquals("American Express", cardBrand("378282246310005"))
        assertEquals("4242 4242 4242 4242", formatCardNumber("4242424242424242"))
        assertEquals("3782 822463 10005", formatCardNumber("378282246310005"))
        assertEquals("4242424242424242", cardDigits("4242-4242 4242 4242"))
    }

    @Test fun separatorsNeverMoveTheCursorBackwards() {
        val card = CardNumberTransformation().filter(androidx.compose.ui.text.AnnotatedString("424242"))
        assertEquals("4242 42", card.text.text)
        // Cursor at the end of the raw text stays at the end of the shown text.
        assertEquals(7, card.offsetMapping.originalToTransformed(6))
        assertEquals(4, card.offsetMapping.originalToTransformed(4))
        assertEquals(6, card.offsetMapping.originalToTransformed(5))
        assertEquals(4, card.offsetMapping.transformedToOriginal(5))
        assertEquals(6, card.offsetMapping.transformedToOriginal(7))
        val full = CardNumberTransformation().filter(androidx.compose.ui.text.AnnotatedString("4242424242424242"))
        assertEquals("4242 4242 4242 4242", full.text.text)
        assertEquals(19, full.offsetMapping.originalToTransformed(16))
        val expiry = ExpiryTransformation().filter(androidx.compose.ui.text.AnnotatedString("1228"))
        assertEquals("12/28", expiry.text.text)
        assertEquals(5, expiry.offsetMapping.originalToTransformed(4))
        assertEquals(2, expiry.offsetMapping.originalToTransformed(2))
        assertEquals("12", ExpiryTransformation().filter(androidx.compose.ui.text.AnnotatedString("12")).text.text)
        assertEquals("1228", expiryDigits("12/28x9"))
        assertEquals(YearMonth.of(2028, 12), parseExpiry(expiryText("1228")))
    }

    @Test fun cardErrorsCoverExpiryHolderAndCvc() {
        val today = YearMonth.of(2026, 11)
        assertTrue(cardErrors("4242424242424242", "Ana Prueba", "11/26", "123", today).valid)
        assertNotNull(cardErrors("4242424242424242", "Ana Prueba", "10/26", "123", today).expiry)
        assertNotNull(cardErrors("4242424242424242", "Ana Prueba", "13/27", "123", today).expiry)
        assertNotNull(cardErrors("4242424242424242", "A", "12/27", "123", today).holder)
        assertNotNull(cardErrors("378282246310005", "Ana Prueba", "12/27", "123", today).cvc)
        assertTrue(cardErrors("378282246310005", "Ana Prueba", "12/2027", "1234", today).valid)
        assertEquals(YearMonth.of(2030, 1), parseExpiry("01/30"))
    }

    @Test fun periodProgressReportsRemainingDays() {
        val (share, days) = periodProgress("2026-11-01T00:00:00Z", "2026-12-01T00:00:00Z", Instant.parse("2026-11-21T00:00:00Z"))!!
        assertEquals(10L, days)
        assertEquals(1f / 3f, share, 0.01f)
        assertNull(periodProgress(null, "2026-12-01T00:00:00Z"))
    }

    @Test fun phoneIsOptionalAndValidated() {
        assertNull(registrationPhoneError(""))
        assertNull(registrationPhoneError("+51 987-654-321"))
        assertNotNull(registrationPhoneError("12ab"))
        assertNotNull(registrationPhoneError("+1234"))
    }

    @Test fun profileUsesServerCategoryThenAltmanZone() {
        assertEquals("Muy sólido", profileLevel(CompanySummary(profile = FinancialProfile("strong", "Muy sólido", "green"))).label)
        assertEquals("Moderado", profileLevel(CompanySummary(altman_z = AltmanData("2.1", "grey"))).label)
        assertEquals("Muy débil", profileLevel(CompanySummary(profile = FinancialProfile("weak"))).label)
        assertEquals("unknown", profileLevel(CompanySummary(profile = FinancialProfile("unknown", "Sin datos suficientes"))).category)
    }

    private val proInfo = AccessInfo("subscription-v1", "1", "pro", true, listOf("compare"), "2026-12-01T00:00:00Z")
    private fun invoice(number: String) = InvoiceInfo(number, "TXN-1", "purchase", "pro", "9.99", "USD", "Visa", "4242",
        "2026-11-01T00:00:00Z", "2026-12-01T00:00:00Z", "2026-11-01T00:00:00Z", true, true)

    private class FakeBilling(var user: String = "1") : BillingSource {
        val requests = mutableListOf<PaymentRequest>()
        var answer: Resource<PaymentResult> = Resource.Error("Sin red")
        var cancels = 0
        override fun owner() = user
        override suspend fun plans() = Resource.Success(PlansInfo("plans-v1"))
        override suspend fun pay(request: PaymentRequest): Resource<PaymentResult> { requests += request; return answer }
        override suspend fun invoices() = Resource.Success(InvoiceList("invoices-v1", user, emptyList()))
        override suspend fun invoice(number: String): Resource<InvoiceInfo> = Resource.Error("No usado")
        override suspend fun cancel(reason: String, comment: String): Resource<AccessInfo> { cancels++; return Resource.Error("No usado") }
    }

    @Test fun retryAfterLostAnswerReusesRequestIdAndNeverSendsInvalidCards() = exercise {
        val source = FakeBilling(); var ids = 0
        val vm = BillingViewModel(source) { "req-${++ids}" }
        vm.pay("4242424242424241", "Ana Prueba", "12/40", "123"); advanceUntilIdle()
        assertTrue(source.requests.isEmpty())
        vm.pay("4242 4242 4242 4242", "Ana Prueba", "12/40", "123"); advanceUntilIdle()
        vm.pay("4242424242424242", "Ana Prueba", "12/40", "123"); advanceUntilIdle()
        assertEquals(listOf("req-1", "req-1"), source.requests.map { it.request_id })
        assertEquals("4242424242424242", source.requests.first().card_number)
        assertEquals(2040, source.requests.first().exp_year)
        vm.cardEdited()
        source.answer = Resource.Success(PaymentResult("payment-v1", "1", invoice("INV-1"), proInfo))
        vm.pay("5555555555554444", "Ana Prueba", "12/40", "321"); advanceUntilIdle()
        assertEquals("req-2", source.requests.last().request_id)
        assertEquals("INV-1", (vm.payment as Resource.Success).successData.invoice?.number)
    }

    @Test fun accountChangeHidesPaymentAndCancelNeedsReason() = exercise {
        val source = FakeBilling()
        source.answer = Resource.Success(PaymentResult("payment-v1", "1", invoice("INV-1"), proInfo))
        val vm = BillingViewModel(source) { "req-x" }
        vm.pay("4242424242424242", "Ana Prueba", "12/40", "123"); advanceUntilIdle()
        assertTrue(vm.payment is Resource.Success)
        source.user = "2"
        assertTrue(vm.payment is Resource.Empty)
        vm.cancel("", ""); vm.cancel("unknown", ""); advanceUntilIdle()
        assertEquals(0, source.cancels)
        vm.cancel("price", "caro"); advanceUntilIdle()
        assertEquals(1, source.cancels)
    }

    @Test fun contractsRejectForeignOrIncompleteData() {
        assertTrue(validSubscription(proInfo, "1"))
        assertFalse(validSubscription(proInfo, "2"))
        assertTrue(validInvoice(invoice("INV-1")))
        assertFalse(validInvoice(invoice("INV-1").copy(amount = "abc")))
    }
}
