package com.example.sp500auth.pillars
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.metricValue
import org.junit.Assert.*
import org.junit.Test
class EfficiencyTest {
    @Test fun readsOnlyExistingAnnualRows() {
        val pillar=serverFixture().pillars!!.first{it.id=="efficiency"}
        assertEquals(listOf(2024,2023,2022),pillar.history!!.map{it.fiscal_year})
        assertEquals("0,53 veces",metricValue(pillar.metrics!![0]))
        assertEquals("9,22 días",metricValue(pillar.metrics!![1]))
        assertTrue(pillar.history!!.all{it.period?.start!=null && it.period?.end!=null})
    }
    @Test fun nonApplicableInventoryIsDistinctFromAbsentAndZero() {
        assertEquals("No aplica",metricValue(PillarMetric(status="not_applicable",reason="inventory_not_applicable")))
        assertEquals("No disponible",metricValue(PillarMetric(status="unavailable")))
        assertEquals("0,00 días",metricValue(PillarMetric(status="available",value="0",display_value="0,00",unit="days")))
    }
}
