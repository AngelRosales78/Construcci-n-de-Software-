package com.example.sp500auth.pillars
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.*
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

fun serverFixture(): PillarsResponse = Gson().fromJson(
    ServerContractTest::class.java.getResource("/pillars-msft-2024.json")!!.readText(), PillarsResponse::class.java)
class ServerContractTest {
    @Test fun liquidityAndProfitabilityMatchImplementedServer() {
        val data=serverFixture()
        assertEquals("annual-ratios-v2",data.methodology_version)
        assertEquals("1,27 veces",metricValue(data.pillars!![0].metrics!![0]))
        assertEquals("37,13 %",metricValue(data.pillars!![1].metrics!![0]))
        assertEquals("insufficient_sector_sample",data.pillars!![1].sector_reference!!.reason)
        assertFalse(hasComparableSector(data.pillars!![1].sector_reference))
    }
    @Test fun actualSecSourceNamesAndThresholdsArePreserved() {
        val metric=serverFixture().pillars!![0].metrics!![0]
        val source=metric.components!![0].source!!
        assertEquals("0000950170-24-087843",source.accession)
        assertEquals("2024-07-30",source.filed)
        assertEquals("USD",source.currency)
        assertEquals(false,source.is_demo)
        assertEquals(64,source.snapshot_sha256!!.length)
        assertEquals("academic-thresholds-v1",metric.thresholds!!.version)
        assertNotNull(metric.formula_display)
    }
}
