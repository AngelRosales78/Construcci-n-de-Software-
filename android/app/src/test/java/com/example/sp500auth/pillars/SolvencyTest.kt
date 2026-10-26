package com.example.sp500auth.pillars
import com.example.sp500auth.data.api.models.PillarMetric
import com.example.sp500auth.ui.pillars.metricValue
import org.junit.Assert.*
import org.junit.Test
class SolvencyTest {
    @Test fun preservesServerSolvencyAndSource() {
        val pillar=serverFixture().pillars!!.first{it.id=="solvency"}
        assertEquals(listOf("liabilities_to_equity","interest_coverage"),pillar.metrics!!.map{it.id})
        assertEquals("0,91 veces",metricValue(pillar.metrics!![0]))
        assertEquals("37,72 veces",metricValue(pillar.metrics!![1]))
        assertTrue(pillar.metrics!!.all{!it.components.isNullOrEmpty()})
    }
    @Test fun negativeEquityAndZeroInterestStayUnavailable() {
        for(reason in listOf("nonpositive_equity","zero_denominator"))
            assertEquals("No disponible",metricValue(PillarMetric(status="unavailable",reason=reason)))
    }
    @Test fun negativeEbitIsNotConvertedToZero() {
        assertEquals("-2,50 veces",metricValue(PillarMetric(status="available",value="-2.5",display_value="-2,50",unit="times")))
    }
}
