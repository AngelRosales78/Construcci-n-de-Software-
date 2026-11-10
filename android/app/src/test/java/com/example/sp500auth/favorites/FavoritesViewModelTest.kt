package com.example.sp500auth.favorites

import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.favorites.FavoritesViewModel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private class Source : FavoritesDataSource {
        var user: String? = "1"; var calls = 0; var fail = false; var slow = false
        override fun owner() = user
        override suspend fun remove(item: FavoriteItem): FavoriteResult<Unit> = error("Unused")
        override suspend fun list(): FavoriteResult<List<FavoriteItem>> = FavoriteResult.Success(emptyList())
        override suspend fun add(ticker: String): FavoriteResult<FavoriteItem> {
            calls++; if (slow) withContext(NonCancellable) { delay(1000) }
            return if (fail) FavoriteResult.Failure("Sin red") else FavoriteResult.Success(FavoriteItem(1, ticker, ticker))
        }
    }
    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
    @Test fun blocksRepeatedTapsAndMarksOnlyAfterConfirmation() = exercise {
        val source = Source().apply { slow = true }; val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.add("AAPL"); vm.add("AAPL"); runCurrent()
        assertNull(vm.item("AAPL")); assertTrue(vm.busy)
        advanceUntilIdle(); assertEquals(1, source.calls); assertNotNull(vm.item("AAPL"))
        vm.add("AAPL"); advanceUntilIdle(); assertEquals(1, source.calls)
    }
    @Test fun failedAddDoesNotMarkCompanyAndCanRetry() = exercise {
        val source = Source().apply { fail = true }; val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.add("AAPL"); advanceUntilIdle()
        assertNull(vm.item("AAPL")); assertEquals("Sin red", vm.message)
        source.fail = false; vm.add("AAPL"); advanceUntilIdle(); assertNotNull(vm.item("AAPL"))
    }
    @Test fun changingAccountHidesPrivateStateAndIgnoresLateReply() = exercise {
        val source = Source().apply { slow = true }; val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.add("AAPL"); runCurrent()
        source.user = "2"; assertTrue(vm.visibleState is Resource.Empty)
        vm.refresh(); advanceUntilIdle(); assertNull(vm.item("AAPL")); assertNull(vm.message)
        vm.clear(); assertNull(vm.item("AAPL"))
    }
}
