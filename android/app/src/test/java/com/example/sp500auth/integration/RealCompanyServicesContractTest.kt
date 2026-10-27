package com.example.sp500auth.integration

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.analysis.validAnalysis
import com.example.sp500auth.ui.scenarios.validSimulation
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class RealCompanyServicesContractTest {
    private fun <T> fixture(name: String, type: Class<T>): T = javaClass.classLoader!!.getResourceAsStream(name)!!.reader().use { Gson().fromJson(it,type) }
    @Test fun androidAcceptsRealLocalAiResponseForChosenCompany() {
        val report=fixture("analysis-aapl-real.json",AnalysisResponse::class.java)
        assertEquals("ai",report.generation_mode)
        assertTrue(validAnalysis(report,"AAPL",AnalysisRequest(2024,"simple")))
        assertFalse(validAnalysis(report,"MSFT",AnalysisRequest(2024,"simple")))
    }
    @Test fun androidAcceptsRealServerSimulationAndRejectsAnotherCompany() {
        val response=fixture("simulation-aapl-real.json",SimulationResponse::class.java)
        assertTrue(validSimulation(response,"AAPL",response.parameters!!))
        assertFalse(validSimulation(response,"MSFT",response.parameters!!))
    }
}
