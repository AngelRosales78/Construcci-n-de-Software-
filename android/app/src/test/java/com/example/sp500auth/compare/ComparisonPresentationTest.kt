package com.example.sp500auth.compare

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.compare.*
import org.junit.Assert.*
import org.junit.Test

class ComparisonPresentationTest {
    private fun company(ticker: String, value: String) = PillarsResponse(ticker = ticker, pillars = listOf(
        FinancialPillar(id = "liquidity", score = PillarScore(status = "available", value = value, display_value = value))))
    @Test fun presentsFiveAxesAndUsesServerDecisionRatherThanInferringWinner() {
        val data = ComparisonResponse(a = company("AAPL", "90"), b = company("MSFT", "80"), comparisons = listOf(
            PillarComparison("liquidity", "not_comparable", "not_comparable", reason = "Periodos distintos")))
        val rows = comparisonRows(data)
        assertEquals(5, rows.size)
        assertEquals("90 / 100", rows.first().aValue)
        assertEquals("— No comparable", rows.first().result)
        assertNull(rows.first().higher)
        assertEquals("Periodos distintos", rows.first().reason)
        assertEquals("Crecimiento", rows.last().label)
    }
    @Test fun missingScoresAreNotZerosAndTiesAreExplicit() {
        val tie = ComparisonResponse(a = company("AAPL", "80"), b = company("MSFT", "80"), comparisons = listOf(
            PillarComparison("liquidity", "available", "tie", "0", "points")))
        assertEquals("= Empate", comparisonRows(tie).first().result)
        assertNull(comparisonRows(tie).first().higher)
        assertEquals("No disponible", comparisonRows(tie)[1].aValue)
    }
}
