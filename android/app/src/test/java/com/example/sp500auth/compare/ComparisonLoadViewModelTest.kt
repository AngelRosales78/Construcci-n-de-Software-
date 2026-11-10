package com.example.sp500auth.compare

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.ComparisonDataSource
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonLoadViewModelTest {
    private class Source : ComparisonDataSource {
        var calls = 0
        var fail = false
        var slowFirst = false
        override suspend fun search(query: String): Resource<List<CompanyItem>> = Resource.Success(emptyList())
        override suspend fun compare(a: String, b: String, fiscalYear: Int): Resource<ComparisonResponse> {
            calls++
            if (slowFirst && calls == 1) withContext(NonCancellable) { delay(1000) }
            return if (fail) Resource.Error("Sin conexión")
            else Resource.Success(ComparisonResponse(fiscal_year = fiscalYear, a = PillarsResponse(ticker = a), b = PillarsResponse(ticker = b)))
        }
    }
    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
    private fun ready(source: Source): ComparisonViewModel = ComparisonViewModel(source, SavedStateHandle(mapOf("year" to "2024"))).apply {
        select(CompanySlot.A, CompanyItem("AAPL", "Apple")); select(CompanySlot.B, CompanyItem("MSFT", "Microsoft"))
    }
    @Test fun failureRetainsSelectionAndRetryCanSucceed() = exercise {
        val source = Source().apply { fail = true }; val vm = ready(source)
        vm.compare(); assertTrue(vm.result is Resource.Loading)
        advanceUntilIdle(); assertTrue(vm.result is Resource.Error)
        assertEquals("AAPL", vm.selection.a); assertEquals("2024", vm.year)
        source.fail = false; vm.compare(); advanceUntilIdle()
        assertTrue(vm.result is Resource.Success); assertEquals(2, source.calls)
    }
    @Test fun invalidYearOrIncompletePairNeverCallsServer() = exercise {
        val source = Source(); val vm = ready(source)
        vm.changeYear("abc"); vm.compare(); advanceUntilIdle()
        assertTrue(vm.result is Resource.Error); assertEquals(0, source.calls)
        vm.changeYear("2024"); vm.remove(CompanySlot.B); vm.compare(); advanceUntilIdle()
        assertEquals(0, source.calls)
    }
    @Test fun lateResponseCannotReplaceNewPairEvenIfSourceIgnoresCancellation() = exercise {
        val source = Source().apply { slowFirst = true }; val vm = ready(source)
        vm.compare(); runCurrent()
        vm.swap(); vm.compare(); advanceUntilIdle()
        val result = (vm.result as Resource.Success).successData
        assertEquals("MSFT", result.a?.ticker); assertEquals("AAPL", result.b?.ticker)
    }
    @Test fun yearAndPairChangesClearOldResultsAndYearIsSaved() = exercise {
        val source = Source(); val saved = SavedStateHandle(mapOf("a" to "AAPL", "b" to "MSFT", "year" to "2024"))
        val vm = ComparisonViewModel(source, saved)
        vm.compare(); advanceUntilIdle(); assertTrue(vm.result is Resource.Success)
        vm.changeYear("2023"); assertTrue(vm.result is Resource.Empty)
        assertEquals("2023", ComparisonViewModel(source, saved).year)
        vm.compare(); advanceUntilIdle(); vm.remove(CompanySlot.A)
        assertTrue(vm.result is Resource.Empty)
    }
}
