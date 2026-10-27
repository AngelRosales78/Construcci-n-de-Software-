package com.example.sp500auth.analysis
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.analysis.*
import com.example.sp500auth.ui.common.safeSourceUrl
import com.example.sp500auth.ui.pillars.pillarLabels
import com.example.sp500auth.data.repository.AnalysisSource as ExplanationSource
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

fun analysisFixture(level:String="simple")=AnalysisResponse(schema_version="analysis-v1",ticker="MSFT",fiscal_year=2024,
    level=level,metrics_version="annual-ratios-v2:snapshot-1",data_as_of="2024-06-30",generated_at="2025-01-01T12:00:00Z",
    period=FiscalPeriod("2023-07-01","2024-06-30"),validation=AnalysisValidation("passed","2025-01-01T12:00:00Z"),
    summary=listOf("Resumen de prueba sustentado por los datos."),
    pillars=pillarLabels.keys.map{AnalysisSection(it,"Explicación de prueba.",listOf(pillarMetricIds[it]!!.first()))},
    strengths=listOf(AnalysisPoint("Punto respaldado.",listOf("roe"))),risks=emptyList(),
    sources=listOf(AnalysisSource("SEC","https://www.sec.gov/example")))
class AnalysisValidationTest {
    private val request=AnalysisRequest(2024,"simple")
    @Test fun acceptsFiveValidatedSectionsAndShortEvidenceLists() {
        assertTrue(validAnalysis(analysisFixture(),"MSFT",request))
        assertEquals(5,analysisFixture().pillars!!.size)
    }
    @Test fun rejectsUnvalidatedFalseFigureAndWrongContext() {
        val data=analysisFixture()
        for(invalid in listOf(data.copy(validation=AnalysisValidation("failed","now"),summary=listOf("Cifra falsa: 999999.")),
            data.copy(ticker="AAPL"),data.copy(fiscal_year=2023),data.copy(level="technical"),data.copy(metrics_version=null),
            data.copy(summary=List(4){"Párrafo"}),data.copy(pillars=data.pillars!!.dropLast(1)),
            data.copy(strengths=List(6){AnalysisPoint("Punto",listOf("roe"))})))
            assertFalse(validAnalysis(invalid,"MSFT",request))
    }
    @Test fun rejectsUnknownMetricReferencesAndUnsafeSources() {
        val data=analysisFixture()
        assertFalse(validAnalysis(data.copy(risks=listOf(AnalysisPoint("Sin respaldo",listOf("invented_metric")))),"MSFT",request))
        assertFalse(validAnalysis(data.copy(sources=listOf(AnalysisSource(url="javascript:alert(1)"))),"MSFT",request))
        assertFalse(safeSourceUrl("https://user:pass@example.org/"))
        assertTrue(safeSourceUrl("https://www.sec.gov/"))
    }
}
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    private class Fake: ExplanationSource {
        var fail=false;val requests=mutableListOf<AnalysisRequest>()
        override suspend fun load(ticker:String,request:AnalysisRequest):Resource<AnalysisResponse> {
            requests.add(request)
            return if(fail) Resource.Error("Proveedor no disponible") else Resource.Success(analysisFixture(request.level).copy(ticker=ticker,fiscal_year=request.fiscal_year))
        }
    }
    @Test fun failedLanguageChangeKeepsLastValidWithoutCrossCompanyReuse()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source=Fake();val vm=AnalysisViewModel(source)
            vm.load("MSFT",2024);runCurrent();val old=vm.lastValid
            source.fail=true;vm.load("MSFT",2024,"technical");assertTrue(vm.state is Resource.Loading);runCurrent()
            assertSame(old,vm.lastValid);assertTrue(vm.state is Resource.Error)
            vm.load("AAPL",2024,"simple");assertNull(vm.lastValid);runCurrent();assertNull(vm.lastValid)
        }finally{Dispatchers.resetMain()}
    }
    @Test fun bothLevelsAndInvalidLevel()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source=Fake();val vm=AnalysisViewModel(source)
            for(level in listOf("simple","technical")){vm.load("MSFT",2024,level);runCurrent();assertEquals(level,vm.lastValid!!.level)}
            vm.load("MSFT",2024,"unsupported");runCurrent();assertEquals(2,source.requests.size);assertTrue(vm.state is Resource.Error)
        }finally{Dispatchers.resetMain()}
    }
}
