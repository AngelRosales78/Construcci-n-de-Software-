package com.example.sp500auth.scenarios
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.SimulationSource
import com.example.sp500auth.ui.scenarios.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

fun defaultsFixture()=SimulationDefaults("simulation-defaults-v1","MSFT",.05,.2,-.5,.5,0.0,1.0,42L,1000)
fun requestFixture()=SimulationRequest(.05,.2,42,1000)
fun simulationFixture()=SimulationResponse(schema_version="simulation-v1",ticker="MSFT",simulation_id="run-1",currency="USD",
    model_version="monte-carlo-v1",generated_at="2025-01-31T12:00:00Z",start_date="2024-01-31",end_date="2025-01-31",
    horizon_months=12,parameters=requestFixture(),sources=listOf("https://example.org/history"),
    points=listOf(ScenarioPoint("2024-01-31",100.0,100.0,100.0),ScenarioPoint("2025-01-31",90.0,110.0,130.0)))
class ScenarioValidationTest {
    @Test fun baseAndBothPercentilesShareOneRun() {
        assertTrue(validSimulation(simulationFixture(),"MSFT",requestFixture()))
        assertEquals(listOf(90.0,110.0,130.0),simulationFixture().points!!.last().let{listOf(it.p10,it.p50,it.p90)})
    }
    @Test fun rejectsWrongDatesInvertedPercentilesOrDifferentSeed() {
        val data=simulationFixture()
        val invalid=listOf(data.copy(end_date="2025-02-01"),data.copy(ticker="AAPL"),data.copy(simulation_id=null),
            data.copy(parameters=requestFixture().copy(seed=99)),data.copy(sources=emptyList()),
            data.copy(points=listOf(ScenarioPoint("2024-01-31",110.0,100.0,130.0),data.points!!.last())),
            data.copy(points=listOf(data.points!!.last(),data.points!!.first())))
        invalid.forEach{assertFalse(validSimulation(it,"MSFT",requestFixture()))}
    }
    @Test fun parametersRespectServerLimitsAndDoNotAcceptNan() {
        val defaults=defaultsFixture()
        assertEquals(requestFixture(),simulationRequest("5","20","42",defaults))
        for((r,v,s) in listOf(Triple("NaN","20","42"),Triple("60","20","42"),Triple("5","-1","42"),Triple("5","20","x")))
            assertNull(simulationRequest(r,v,s,defaults))
        assertNull(simulationRequest("5","20","42",null))
        assertFalse(validDefaults(defaults.copy(min_volatility=-.1),"MSFT"))
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
class ScenariosViewModelTest {
    private class Fake: SimulationSource {
        var calls=0;val requests=mutableListOf<SimulationRequest>()
        override suspend fun defaults(ticker:String):Resource<SimulationDefaults> = Resource.Success(defaultsFixture())
        override suspend fun simulate(ticker:String,request:SimulationRequest):Resource<SimulationResponse>{calls++;requests.add(request);return Resource.Success(simulationFixture().copy(parameters=request))}
    }
    @Test fun hideAndShowDoesNotRecalculateAndResetUsesInitialValues()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source=Fake();val vm=ScenariosViewModel(source)
            vm.load("MSFT");runCurrent();vm.calculate("MSFT");runCurrent()
            val before=vm.result.data
            vm.toggle("p90");vm.toggle("p10");assertEquals(1,source.calls);assertSame(before,vm.result.data)
            vm.returnText="10";vm.calculate("MSFT");runCurrent();assertEquals(.1,source.requests.last().annual_return,1e-9)
            vm.reset("MSFT");runCurrent();assertEquals(requestFixture(),source.requests.last());assertEquals(3,vm.visible.size)
        }finally{Dispatchers.resetMain()}
    }
    @Test fun invalidInputNeverCallsSimulation()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source=Fake();val vm=ScenariosViewModel(source);vm.load("MSFT");runCurrent()
            vm.returnText="999";vm.calculate("MSFT");runCurrent()
            assertEquals(0,source.calls);assertTrue(vm.result is Resource.Error)
        }finally{Dispatchers.resetMain()}
    }
}
