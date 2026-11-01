package com.example.sp500auth.compare

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.compare.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AlignedHistoryTest {
    private fun series(ticker: String, vararg prices: Double) = PriceSeries(ticker, "1Y", prices.mapIndexed { i, value -> PricePoint(LocalDate.of(2024, 1, 1).plusDays(i.toLong()).toString(), value) }, listOf("USD"), listOf("https://example.org/source"), false)
    @Test fun normalizesCommonBaseAndKeepsOriginalPrices() {
        val data = alignHistory(series("AAPL", 100.0, 110.0), series("MSFT", 200.0, 180.0))
        assertEquals(0.0, data.points.first().a, 0.0); assertEquals(0.0, data.points.first().b, 0.0)
        assertEquals(10.0, data.points.last().a, .000001); assertEquals(-10.0, data.points.last().b, .000001)
        assertEquals(180.0, data.points.last().bPrice, 0.0)
    }
    @Test fun alignsDatesWithoutFillingGapsAndSortsThem() {
        val a = series("AAPL", 100.0, 110.0, 120.0)
        val b = series("MSFT", 200.0, 220.0, 240.0).copy(history = listOf(PricePoint("2024-01-03", 240.0), PricePoint("2024-01-01", 200.0)))
        val data = alignHistory(a, b)
        assertEquals(listOf("2024-01-01", "2024-01-03"), data.points.map { it.date })
        assertTrue(data.warnings.any { "huecos" in it })
    }
    @Test fun rejectsInvalidPricesDatesDuplicatesAndDemo() {
        val a = series("AAPL", 100.0, 110.0); val b = series("MSFT", 200.0, 220.0)
        for (bad in listOf(b.copy(history = listOf(PricePoint("bad", 10.0))), b.copy(history = listOf(PricePoint("2024-01-01", 0.0))),
            b.copy(history = listOf(PricePoint("2024-01-01", Double.NaN))), b.copy(history = listOf(PricePoint("2024-01-01", 1.0), PricePoint("2024-01-01", 2.0))), b.copy(is_demo = true), b.copy(range = "6M"))) {
            assertThrows(IllegalArgumentException::class.java) { alignHistory(a, bad) }
        }
    }
    @Test fun handlesLongSeriesAndNearestDateByCalendarDistance() {
        val prices = DoubleArray(1500) { 100.0 + it }
        val data = alignHistory(series("AAPL", *prices), series("MSFT", *prices))
        assertEquals(1500, data.points.size)
        assertEquals(0, nearestHistoryIndex(data.points, -1f)); assertEquals(1499, nearestHistoryIndex(data.points, 2f))
        assertEquals(750, nearestHistoryIndex(data.points, .5f))
    }
    @Test fun refusesInsufficientCommonHistoryOrRepeatedCompanies() {
        assertThrows(IllegalArgumentException::class.java) { alignHistory(series("AAPL", 1.0), series("MSFT", 2.0)) }
        assertThrows(IllegalArgumentException::class.java) { alignHistory(series("AAPL", 1.0, 2.0), series("AAPL", 2.0, 3.0)) }
    }
}
