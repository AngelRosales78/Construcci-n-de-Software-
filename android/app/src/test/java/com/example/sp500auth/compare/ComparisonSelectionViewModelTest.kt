package com.example.sp500auth.compare

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.data.repository.ComparisonDataSource
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComparisonSelectionViewModelTest {
    private class Catalog : ComparisonDataSource {
        override suspend fun compare(a: String, b: String, fiscalYear: Int): Resource<com.example.sp500auth.data.api.models.ComparisonResponse> = error("Not used by selection tests")
        val queries = mutableListOf<String>()
        override suspend fun search(query: String): Resource<List<CompanyItem>> {
            queries.add(query)
            return Resource.Success(listOf(CompanyItem(query.uppercase(), query)))
        }
    }
    @Test fun debouncesBothInputsIndependentlyAndClearingCancelsSearch() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val catalog = Catalog()
            val vm = ComparisonViewModel(catalog, SavedStateHandle())
            vm.query(CompanySlot.A, "ap"); vm.query(CompanySlot.A, "aapl")
            vm.query(CompanySlot.B, "msft")
            advanceTimeBy(299); runCurrent(); assertTrue(catalog.queries.isEmpty())
            advanceUntilIdle(); assertEquals(listOf("aapl", "msft"), catalog.queries)
            vm.query(CompanySlot.A, "nvda"); vm.query(CompanySlot.A, "")
            advanceUntilIdle(); assertEquals(2, catalog.queries.size)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun savedSelectionAndQueriesSurviveViewModelRecreation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val saved = SavedStateHandle()
            val vm = ComparisonViewModel(Catalog(), saved)
            vm.select(CompanySlot.A, CompanyItem("AAPL", "Apple"))
            vm.select(CompanySlot.B, CompanyItem("MSFT", "Microsoft"))
            vm.query(CompanySlot.B, "nvidia")
            val restored = ComparisonViewModel(Catalog(), saved)
            assertEquals(vm.selection, restored.selection)
            assertEquals("nvidia", restored.queryB)
            assertTrue(restored.selection.canCompare)
            advanceUntilIdle()
        } finally { Dispatchers.resetMain() }
    }
    @Test fun initialCompanyIsAppliedOnlyOnceAndDuplicateDoesNotOverwriteSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val saved = SavedStateHandle()
            val vm = ComparisonViewModel(Catalog(), saved)
            vm.initialize("MSFT")
            vm.select(CompanySlot.B, CompanyItem("msft", "Microsoft"))
            assertNotNull(vm.selectionError); assertEquals("", vm.selection.b)
            vm.remove(CompanySlot.A)
            val restored = ComparisonViewModel(Catalog(), saved)
            restored.initialize("MSFT")
            assertEquals("", restored.selection.a)
        } finally { Dispatchers.resetMain() }
    }
}
