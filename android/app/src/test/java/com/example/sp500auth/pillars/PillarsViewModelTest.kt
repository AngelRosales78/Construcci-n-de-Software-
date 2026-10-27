package com.example.sp500auth.pillars

import com.example.sp500auth.data.api.models.PillarsResponse
import com.example.sp500auth.data.repository.PillarsDataSource
import com.example.sp500auth.data.repository.PillarsResult
import com.example.sp500auth.ui.pillars.PillarsState
import com.example.sp500auth.ui.pillars.PillarsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.util.ArrayDeque

@OptIn(ExperimentalCoroutinesApi::class)
class PillarsViewModelTest {
    private class FakeSource(vararg results: PillarsResult) : PillarsDataSource {
        val queue = ArrayDeque(results.toList())
        var calls = 0
        override suspend fun load(ticker: String, fiscalYear: Int): PillarsResult {
            calls++
            return queue.removeFirst()
        }
    }
    private fun success() = PillarsResult.Success(PillarsResponse(ticker = "MSFT", fiscal_year = 2024))
    @Test fun loadingThenReady() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = PillarsViewModel(FakeSource(success()))
            vm.load("MSFT", "2024")
            assertTrue(vm.state is PillarsState.Loading)
            runCurrent()
            assertTrue(vm.state is PillarsState.Ready)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun errorCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = PillarsViewModel(FakeSource(PillarsResult.Failure("Sin conexión"), success()))
            vm.load("MSFT", "2024")
            runCurrent()
            assertEquals("Sin conexión", (vm.state as PillarsState.Error).message)
            vm.load("MSFT", "2024")
            assertTrue(vm.state is PillarsState.Loading)
            runCurrent()
            assertTrue(vm.state is PillarsState.Ready)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun invalidSelectionDoesNotCallServer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeSource()
            val vm = PillarsViewModel(repository)
            for ((ticker, year) in listOf("" to "2024", "MSFT" to "abc", "MSFT" to "0", "../MSFT" to "2024")) {
                vm.load(ticker, year)
                assertTrue(vm.state is PillarsState.Error)
            }
            assertEquals(0, repository.calls)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun newSelectionCancelsOldResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = object : PillarsDataSource {
                override suspend fun load(ticker: String, fiscalYear: Int): PillarsResult {
                    if (fiscalYear == 2023) delay(1000)
                    return PillarsResult.Success(PillarsResponse(ticker = ticker, fiscal_year = fiscalYear))
                }
            }
            val vm = PillarsViewModel(repository)
            vm.load("MSFT", "2023")
            runCurrent()
            vm.load("MSFT", "2024")
            advanceUntilIdle()
            assertEquals(2024, (vm.state as PillarsState.Ready).data.fiscal_year)
        } finally { Dispatchers.resetMain() }
    }
}
