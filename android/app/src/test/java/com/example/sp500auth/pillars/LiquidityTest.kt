package com.example.sp500auth.pillars

import com.example.sp500auth.data.api.models.PillarMetric
import com.example.sp500auth.ui.pillars.metricValue
import com.example.sp500auth.ui.pillars.reasonLabel
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class LiquidityTest {
    private val gson = Gson()
    @Test fun knownRatiosPreserveServerRounding() {
        // Server fixtures: 200 / 100 = 2; (50 + 20 + 40) / 100 = 1.10.
        val current = gson.fromJson("""{"id":"current_ratio","status":"available","value":"2","display_value":"2,00","unit":"times"}""", PillarMetric::class.java)
        val acid = current.copy(id = "quick_ratio", value = "1.1", display_value = "1,10")
        assertEquals("2,00 veces", metricValue(current))
        assertEquals("1,10 veces", metricValue(acid))
    }
    @Test fun zeroDenominatorIsUnavailable() {
        val metric = PillarMetric(status = "unavailable", reason = "zero_denominator")
        assertEquals("No disponible", metricValue(metric))
        assertTrue(reasonLabel(metric.reason).contains("cero"))
    }
    @Test fun absentComponentsAreNotZero() {
        val metric = gson.fromJson("""{"status":"unavailable","value":null,"reason":"missing_components","missing_fields":["current_assets"],"components":[]}""", PillarMetric::class.java)
        assertEquals("No disponible", metricValue(metric))
        assertEquals(listOf("current_assets"), metric.missing_fields)
        assertEquals("No disponible", metricValue(null))
    }
    @Test fun zeroIsAValidServerResult() {
        assertEquals("0,00 veces", metricValue(PillarMetric(status = "available", value = "0", display_value = "0,00", unit = "times")))
    }
    @Test fun malformedAvailableValuesAreNotDisplayed() {
        assertEquals("No disponible", metricValue(PillarMetric(status = "available", value = "NaN", display_value = "NaN")))
        assertEquals("No disponible", metricValue(PillarMetric(status = "available", value = "2")))
        assertEquals("No aplica", metricValue(PillarMetric(status = "not_applicable")))
    }
}
