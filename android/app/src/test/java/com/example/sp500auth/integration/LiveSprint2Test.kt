package com.example.sp500auth.integration

import com.example.sp500auth.data.api.models.AnalysisRequest
import com.example.sp500auth.data.api.models.SimulationRequest
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Pruebas optativas por HTTP real: el ejecutor suministra una sesión aislada. */
class LiveSprint2Test {
    private lateinit var retrofit: Retrofit
    @Before fun setup() {
        val url=System.getenv("SPRINT_API_URL");val token=System.getenv("SPRINT_TEST_ACCESS")
        assumeTrue("Requiere servidor y sesión de prueba",!url.isNullOrBlank() && !token.isNullOrBlank())
        val client=OkHttpClient.Builder().readTimeout(160,TimeUnit.SECONDS).callTimeout(180,TimeUnit.SECONDS)
            .addInterceptor{ chain -> chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $token").build()) }.build()
        retrofit=Retrofit.Builder().baseUrl(url!!).client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }
    @Test fun realScenariosUseSharedPercentilesAndRestoreDefaults()=runBlocking {
        val repository=SimulationRepository(retrofit)
        val d=(repository.defaults("MSFT") as Resource.Success).successData
        val request=SimulationRequest(d.annual_return!!,d.annual_volatility!!,d.seed!!,d.paths!!)
        val original=(repository.simulate("MSFT",request) as Resource.Success).successData
        val flat=(repository.simulate("MSFT",request.copy(annual_return=0.0,annual_volatility=0.0)) as Resource.Success).successData
        val restored=(repository.simulate("MSFT",request) as Resource.Success).successData
        assertEquals(13,original.points!!.size)
        assertEquals(original.points,restored.points)
        assertEquals(1,flat.points!!.map{it.p50}.toSet().size)
        assertTrue(original.points!!.all{it.p10!!<=it.p50!! && it.p50!!<=it.p90!!})
    }
    @Test fun bothAnalysisLevelsAreValidatedAgainstRealFinancialContext()=runBlocking {
        val repository=AnalysisRepository(retrofit)
        val simple=(repository.load("MSFT",AnalysisRequest(2024,"simple")) as Resource.Success).successData
        val technical=(repository.load("MSFT",AnalysisRequest(2024,"technical")) as Resource.Success).successData
        assertEquals("passed",simple.validation!!.status)
        assertEquals(5,simple.pillars!!.size)
        assertEquals(simple.metrics_version,technical.metrics_version)
        assertEquals(simple.period,technical.period)
        assertEquals(simple.sources,technical.sources)
        assertNotEquals(simple.pillars!!.map{it.text},technical.pillars!!.map{it.text})
    }
    @Test fun missingCompanyDataDoesNotBecomeSimulatedOrAiContent()=runBlocking {
        assertTrue(SimulationRepository(retrofit).defaults("AAPL") is Resource.Error)
        assertTrue(AnalysisRepository(retrofit).load("AAPL",AnalysisRequest(2024,"simple")) is Resource.Error)
    }
}
