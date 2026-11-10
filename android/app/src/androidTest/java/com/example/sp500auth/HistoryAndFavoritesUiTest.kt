package com.example.sp500auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.ui.favorites.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryAndFavoritesUiTest {
    @get:Rule val compose = createComposeRule()
    @Test fun historicalDateCanBeSelectedWithoutTouchingCanvas() {
        val a = PriceSeries("AAPL", "1Y", listOf(PricePoint("2024-01-01", 100.0), PricePoint("2024-01-02", 110.0)), is_demo = false)
        val data = alignHistory(a, a.copy(ticker = "MSFT"))
        compose.setContent { MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HistoryComparisonChart(data) } } }
        compose.onNodeWithText("Fecha consultada: 2024-01-02").assertExists()
        compose.onNodeWithText("Anterior").performScrollTo().performClick()
        compose.onNodeWithText("Fecha consultada: 2024-01-01").assertExists()
    }
    @Test fun cancelRemovalDoesNotCallService() {
        var deletes = 0
        val source = object : FavoritesDataSource {
            override fun owner() = "1"
            override suspend fun list(): FavoriteResult<List<FavoriteItem>> = FavoriteResult.Success(listOf(FavoriteItem(7, "AAPL", "Apple")))
            override suspend fun add(ticker: String): FavoriteResult<FavoriteItem> = error("Unused")
            override suspend fun remove(item: FavoriteItem): FavoriteResult<Unit> { deletes++; return FavoriteResult.Success(Unit) }
        }
        val vm = FavoritesViewModel(source)
        compose.setContent { MaterialTheme { FavoritesScreen(rememberNavController(), vm) } }
        compose.onNodeWithText("Quitar AAPL de favoritos").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("1 empresa favorita").assertExists()
    }
}
