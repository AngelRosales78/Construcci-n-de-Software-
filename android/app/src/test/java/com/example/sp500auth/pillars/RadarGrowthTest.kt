package com.example.sp500auth.pillars
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.*
import org.junit.Assert.*
import org.junit.Test
class RadarGrowthTest {
    @Test fun fiveAxesAndBothGrowthIndicators() {
        val data=serverFixture()
        assertEquals(pillarLabels.keys,data.pillars!!.map{it.id}.toSet())
        val growth=data.pillars!!.first{it.id=="growth"}
        assertEquals(listOf("revenue_growth","revenue_cagr_3y"),growth.metrics!!.map{it.id})
        assertEquals("15,67 %",metricValue(growth.metrics!![0]))
        assertEquals("13,40 %",metricValue(growth.metrics!![1]))
    }
    @Test fun missingScoresAreNotZeroAndInvalidBoundsAreRejected() {
        assertNull(scoreNumber(null))
        assertNull(scoreNumber(PillarScore(status="available",value="101",display_value="101")))
        assertNull(scoreNumber(PillarScore(status="available",value="-1",display_value="-1")))
        assertEquals(0f,scoreNumber(PillarScore(status="available",value="0",display_value="0,00")))
        assertEquals("No disponible",metricValue(PillarMetric(status="unavailable",reason="insufficient_history")))
    }
}
