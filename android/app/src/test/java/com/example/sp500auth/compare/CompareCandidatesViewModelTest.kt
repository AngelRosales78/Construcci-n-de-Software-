package com.example.sp500auth.compare

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.ComparisonDataSource
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompareCandidatesViewModelTest {
    private class Source : ComparisonDataSource {
        val requests = mutableListOf<String>()
        override suspend fun search(query: String): Resource<List<CompanyItem>> = Resource.Success(emptyList())
        override suspend fun compare(a: String, b: String, fiscalYear: Int): Resource<ComparisonResponse> = error("Not used")
        override suspend fun candidates(a: String, fiscalYear: Int): Resource<CompareCandidates> {
            requests += "$a/$fiscalYear"
            return Resource.Success(CompareCandidates("compare-candidates-v1", a, fiscalYear,
                results = listOf(CompareCandidate("INTC", "Intel", comparable_pillars = 3, same_sector = true))))
        }
    }

    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); try { block() } finally { Dispatchers.resetMain() }
    }

    @Test fun suggestionsFollowTheChosenCompanyAndYear() = exercise {
        val source = Source()
        val vm = ComparisonViewModel(source, SavedStateHandle(mapOf("year" to "2024")))
        advanceUntilIdle()
        assertTrue(source.requests.isEmpty())
        assertTrue(vm.candidates is Resource.Empty)
        vm.select(CompanySlot.A, CompanyItem("AMD", "AMD")); advanceUntilIdle()
        assertEquals(listOf("AMD/2024"), source.requests)
        vm.changeYear("2023"); advanceUntilIdle()
        assertEquals("AMD/2023", source.requests.last())
        vm.loadCandidates(); advanceUntilIdle()
        assertEquals(2, source.requests.size) // same key is not requested again
        val suggestion = (vm.candidates as Resource.Success).successData.results!!.first()
        vm.selectCandidate(suggestion)
        assertEquals("INTC", vm.selection.b)
        assertEquals("AMD", vm.anchor)
    }

    @Test fun onlyBChosenMakesBTheAnchorAndFillsA() = exercise {
        val source = Source()
        val vm = ComparisonViewModel(source, SavedStateHandle(mapOf("year" to "2024")))
        vm.select(CompanySlot.B, CompanyItem("AMD", "AMD")); advanceUntilIdle()
        assertEquals("AMD", vm.anchor)
        vm.selectCandidate(CompareCandidate("INTC", "Intel", comparable_pillars = 3))
        assertEquals("INTC", vm.selection.a)
        vm.remove(CompanySlot.A); vm.remove(CompanySlot.B); advanceUntilIdle()
        assertTrue(vm.candidates is Resource.Empty)
    }
}
