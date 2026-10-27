package com.example.sp500auth
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Device-only UI checks. Compile them locally; execute when an emulator is connected. */
@RunWith(AndroidJUnit4::class)
class PillarRadarUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun eachPillarHasAnAccessibleDetailButton() {
        var selected=""
        val data=pillarLabels.keys.map{FinancialPillar(id=it,score=PillarScore(status="available",value="80",display_value="80,00"))}
        compose.setContent{MaterialTheme{Column{PillarRadar(data){selected=it}}}}
        compose.onNodeWithText("Liquidez: 80,00 / 100").performClick()
        compose.runOnIdle{assertEquals("liquidity",selected)}
    }
    @Test fun absentScoreIsNotShownAsZero() {
        compose.setContent{MaterialTheme{Column{PillarRadar(emptyList()){}}}}
        compose.onNodeWithText("Liquidez: No disponible").assertExists()
        compose.onNodeWithText("Liquidez: 0,00 / 100").assertDoesNotExist()
    }
}
