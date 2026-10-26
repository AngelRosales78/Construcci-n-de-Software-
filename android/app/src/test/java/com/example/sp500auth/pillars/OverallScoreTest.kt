package com.example.sp500auth.pillars
import com.example.sp500auth.ui.pillars.overallDisplay
import org.junit.Assert.*
import org.junit.Test
class OverallScoreTest {
    @Test fun preservesServerScoreVersionAndContributions() {
        val data=serverFixture()
        assertEquals("85,25 / 100",overallDisplay(data))
        assertEquals("academic-pillar-scores-v1",data.overall_score!!.version)
        assertEquals(5,data.overall_score!!.contributions!!.size)
        assertTrue(data.overall_score!!.contributions!!.all{it.weight=="0.20"})
    }
    @Test fun missingPillarVersionOrPeriodNeverProducesPartialAverage() {
        val data=serverFixture()
        assertEquals("No disponible",overallDisplay(data.copy(pillars=data.pillars!!.dropLast(1))))
        assertEquals("No disponible",overallDisplay(data.copy(overall_score=data.overall_score!!.copy(version="other"))))
        assertEquals("No disponible",overallDisplay(data.copy(period=null)))
        assertEquals("No disponible",overallDisplay(data.copy(overall_score=data.overall_score!!.copy(value="100.01"))))
    }
}
