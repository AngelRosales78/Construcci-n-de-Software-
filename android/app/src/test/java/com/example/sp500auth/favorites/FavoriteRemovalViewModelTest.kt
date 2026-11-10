package com.example.sp500auth.favorites

import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.favorites.FavoritesViewModel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteRemovalViewModelTest {
    private class Source : FavoritesDataSource {
        var user = "1"; var calls = 0; var failed = false; var slow = false
        override fun owner() = user
        override suspend fun list(): FavoriteResult<List<FavoriteItem>> = FavoriteResult.Success(listOf(FavoriteItem(7, "AAPL", "Apple")))
        override suspend fun add(ticker: String): FavoriteResult<FavoriteItem> = error("Unused")
        override suspend fun remove(item: FavoriteItem): FavoriteResult<Unit> {
            calls++; if (slow) withContext(NonCancellable) { delay(1000) }
            return if (failed) FavoriteResult.Failure("Sin red") else FavoriteResult.Success(Unit)
        }
    }
    private fun exercise(block: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try { block() } finally { Dispatchers.resetMain() }
    }
    @Test fun cancelDoesNotDeleteOrChangeCount() = exercise {
        val source = Source(); val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.requestRemoval("AAPL"); assertNotNull(vm.removalCandidate)
        vm.cancelRemoval(); vm.confirmRemoval(); advanceUntilIdle()
        assertEquals(0, source.calls); assertNotNull(vm.item("AAPL")); assertNull(vm.removalCandidate)
    }
    @Test fun confirmsOnlyOnceAndShowsEmptyStateAfterLastConfirmedRemoval() = exercise {
        val source = Source().apply { slow = true }; val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.requestRemoval("AAPL"); vm.confirmRemoval(); vm.confirmRemoval(); runCurrent()
        assertNotNull(vm.item("AAPL")); assertTrue(vm.busy)
        advanceUntilIdle(); assertEquals(1, source.calls); assertNull(vm.item("AAPL"))
        assertTrue((vm.visibleState as Resource.Success).successData.isEmpty())
    }
    @Test fun failureKeepsFavoriteAndRetryCanConfirmRemoval() = exercise {
        val source = Source().apply { failed = true }; val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.requestRemoval("AAPL"); vm.confirmRemoval(); advanceUntilIdle()
        assertNotNull(vm.item("AAPL")); assertNotNull(vm.removalCandidate); assertEquals("Sin red", vm.message)
        source.failed = false; vm.confirmRemoval(); advanceUntilIdle(); assertNull(vm.item("AAPL"))
    }
    @Test fun switchingAccountCancelsConfirmationAndHidesOldMessages() = exercise {
        val source = Source(); val vm = FavoritesViewModel(source)
        vm.refresh(); advanceUntilIdle(); vm.requestRemoval("AAPL")
        source.user = "2"; assertNull(vm.removalCandidate); assertNull(vm.message)
        vm.confirmRemoval(); advanceUntilIdle(); assertEquals(0, source.calls)
    }
}
