package com.example.sp500auth.pillars

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ProfitabilityTest {
    @Test fun knownPercentagesUseServerPresentation() {
        // Fixtures for net income 20, average equity 100, average assets 200, revenue 160.
        val metrics = listOf(
            PillarMetric(id = "roe", status = "available", value = "20", display_value = "20,00", unit = "percent"),
            PillarMetric(id = "roa", status = "available", value = "10", display_value = "10,00", unit = "percent"),
            PillarMetric(id = "net_margin", status = "available", value = "12.5", display_value = "12,50", unit = "percent")
        )
        assertEquals(listOf("20,00 %", "10,00 %", "12,50 %"), metrics.map(::metricValue))
    }
    @Test fun roundingIsNotRepeatedOnAndroid() {
        val metric = PillarMetric(status = "available", value = "12.345", display_value = "12,35", unit = "percent")
        assertEquals("12,35 %", metricValue(metric))
        assertEquals("12.345", metric.value)
    }
    @Test fun negativeEquityShowsServerReason() {
        val metric = PillarMetric(id = "roe", status = "unavailable", reason = "nonpositive_equity")
        assertEquals("No disponible", metricValue(metric))
        assertTrue(reasonLabel(metric.reason).contains("negativo"))
    }
    @Test fun validLossAndZeroAreDisplayed() {
        val loss = PillarMetric(status = "available", value = "-5", display_value = "-5,00", unit = "percent")
        assertEquals("-5,00 %", metricValue(loss))
        assertEquals("0,00 %", metricValue(loss.copy(value = "0", display_value = "0,00")))
    }
    private fun comparable() = SectorReference(status = "available", value = "15", display_value = "15,00",
        unit = "percent", sample_size = 30, universe = "Empresas tecnológicas con cobertura anual",
        as_of = "2025-01-31", period = FiscalPeriod("2024-01-01", "2024-12-31"),
        source = ComponentSource(url = "https://example.org/sector", label = "Fuente de prueba"))
    @Test fun insufficientSampleDoesNotShowBenchmark() {
        assertFalse(hasComparableSector(comparable().copy(status = "unavailable", reason = "insufficient_sample")))
        assertFalse(hasComparableSector(null))
        assertTrue(reasonLabel("insufficient_sample").contains("insuficiente"))
    }
    @Test fun sectorRequiresProvenancePeriodAndUniverse() {
        assertTrue(hasComparableSector(comparable()))
        for (reference in listOf(comparable().copy(sample_size = 0), comparable().copy(universe = null),
            comparable().copy(as_of = null), comparable().copy(source = null), comparable().copy(period = null),
            comparable().copy(value = "NaN"), comparable().copy(display_value = null), comparable().copy(unit = "times"))) {
            assertFalse(hasComparableSector(reference))
        }
    }
    @Test fun parsesProfitabilityAndKeepsAllSourceFields() {
        val response = Gson().fromJson("""{"schema_version":"pillars-v1","ticker":"MSFT","fiscal_year":2024,"pillars":[{"id":"profitability","metrics":[{"id":"roe","status":"unavailable","reason":"nonpositive_equity","components":[{"id":"equity","value":"-10","unit":"USD","source":{"label":"SEC","url":"https://www.sec.gov/example","tag":"StockholdersEquity","unit":"USD","filing":"10-K","published_at":"2024-08-01"}}]}]}]}""", PillarsResponse::class.java)
        val metric = response.pillars!![0].metrics!![0]
        assertEquals("No disponible", metricValue(metric))
        val component = metric.components!![0]
        assertEquals("-10", component.value)
        assertEquals("10-K", component.source!!.filing)
        assertEquals("2024-08-01", component.source!!.published_at)
        assertEquals("USD", component.source!!.unit)
    }
}
