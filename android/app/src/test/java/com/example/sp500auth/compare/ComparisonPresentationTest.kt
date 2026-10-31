package com.example.sp500auth.compare

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.compare.*
import org.junit.Assert.*
import org.junit.Test

class ComparisonPresentationTest {
    private val period = FiscalPeriod("2023-01-01", "2023-12-31")
    private fun company(ticker: String, value: String) = PillarsResponse(ticker = ticker, fiscal_year = 2023,
        period = period, methodology_version = "annual-ratios-v2", scoring_policy = ScoringPolicy(version = "score-v1"), pillars = listOf(
        FinancialPillar(id = "liquidity", score = PillarScore(status = "available", value = value, display_value = value, version = "score-v1", period = period))))
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
        val tie = ComparisonResponse(fiscal_year = 2023, a = company("AAPL", "80"), b = company("MSFT", "80"), comparisons = listOf(
            PillarComparison("liquidity", "available", "tie", "0", "points")))
        assertEquals("= Empate", comparisonRows(tie).first().result)
        assertNull(comparisonRows(tie).first().higher)
        assertEquals("No disponible", comparisonRows(tie)[1].aValue)
    }
    private fun comparable(): ComparisonResponse = ComparisonResponse(fiscal_year = 2023, a = company("AAPL", "90"), b = company("MSFT", "80"),
        comparisons = listOf(PillarComparison("liquidity", "available", "a", "10", "points")))
    @Test fun showsServerResultAndDeltaOnlyWithComparableMetadata() {
        val row = comparisonRows(comparable()).first()
        assertEquals("a", row.higher); assertEquals("10", row.delta)
        assertTrue(row.result.contains("AAPL"))
    }
    @Test fun incompatiblePeriodsMethodologyOrNormalizationNeverShowWinner() {
        val data = comparable()
        val original = data.b!!
        for (right in listOf(original.copy(period = FiscalPeriod("2023-07-01", "2024-06-30")),
            original.copy(methodology_version = "different"), original.copy(scoring_policy = ScoringPolicy(version = "different")),
            original.copy(period = FiscalPeriod("bad", "bad")))) {
            val row = comparisonRows(data.copy(b = right)).first()
            assertNull(row.higher); assertNull(row.delta); assertEquals("— No comparable", row.result)
        }
    }
    @Test fun absentOrDuplicateScoresAndUnknownDecisionsAreSafe() {
        val data = comparable()
        val original = data.b!!
        for (right in listOf(original.copy(pillars = emptyList()), original.copy(pillars = original.pillars.orEmpty() + original.pillars.orEmpty()))) {
            val row = comparisonRows(data.copy(b = right)).first()
            assertEquals("No disponible", row.bValue); assertNull(row.higher)
        }
        for (decision in listOf(PillarComparison("liquidity", "available", "x", "10", "points"),
            PillarComparison("liquidity", "available", "a", "-10", "points"), PillarComparison("liquidity", "available", "a", "NaN", "points"))) {
            assertNull(comparisonRows(data.copy(comparisons = listOf(decision))).first().higher)
        }
    }
    @Test fun independentRealPillarsDoNotImpersonateServerComparison() {
        val data = comparable().copy(status = "comparison_service_unavailable", reason = "Servicio pendiente", comparisons = emptyList())
        val row = comparisonRows(data).first()
        assertEquals("90 / 100", row.aValue); assertEquals("80 / 100", row.bValue)
        assertNull(row.higher); assertNull(row.delta); assertEquals("Servicio pendiente", row.reason)
    }
}
