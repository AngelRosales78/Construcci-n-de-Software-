package com.example.sp500auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sp500auth.ui.sprint3.PlanPreview
import com.example.sp500auth.ui.reports.ChartOptions
import com.example.sp500auth.ui.compare.ComparisonConclusionCard
import com.example.sp500auth.data.api.models.ComparisonConclusion
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class Sprint3RemainingUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun previewClearlyLabelsIllustrationAndDoesNotActivatePro(){
        var requests=0
        compose.setContent{MaterialTheme{PlanPreview({requests++},{})}}
        compose.onNodeWithText("Ver plan Pro").performClick();compose.runOnIdle{assertEquals(1,requests)}
        compose.onNodeWithText("Vista previa ilustrativa. No contiene datos privados ni resultados de empresas.").assertExists()
    }
    @Test fun graphRangeAndRulesLabelRemainExplicit(){
        var selected="1Y"
        compose.setContent{MaterialTheme{Column(Modifier.verticalScroll(rememberScrollState())){
            ChartOptions(true,selected,true,{},{selected=it})
            ComparisonConclusionCard(ComparisonConclusion(a="AAPL",b="MSFT",fiscal_year=2024,generation_mode="rules"))
        }}}
        compose.onNodeWithText("6M").performClick();compose.runOnIdle{assertEquals("6M",selected)}
        compose.onNodeWithText("Informe elaborado por reglas").assertExists()
    }
}
