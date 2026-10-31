package com.example.sp500auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertCountEquals
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.compare.ComparisonTable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class ComparisonTableUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missingScoresAndUnavailableComparisonHaveExplicitLabels() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ComparisonTable(ComparisonResponse(fiscal_year = 2024,
                        a = PillarsResponse(ticker = "AAPL", fiscal_year = 2024),
                        b = PillarsResponse(ticker = "MSFT", fiscal_year = 2024),
                        status = "comparison_service_unavailable", reason = "Servicio pendiente"))
                }
            }
        }
        compose.onNodeWithText("Cinco pilares lado a lado").assertExists()
        compose.onNodeWithText("A · AAPL").assertExists()
        compose.onNodeWithText("B · MSFT").assertExists()
        compose.onAllNodesWithText("— No comparable").assertCountEquals(5)
        compose.onAllNodesWithText("No disponible").assertCountEquals(10)
    }
}
