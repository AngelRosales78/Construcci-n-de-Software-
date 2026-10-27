package com.example.sp500auth.integration
import com.example.sp500auth.analysis.analysisFixture
import com.example.sp500auth.scenarios.*
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PendingServicesTest {
    private fun retrofit(code:Int,body:String,request:(Request)->Unit = {}):Retrofit {
        val client=OkHttpClient.Builder().addInterceptor{chain ->
            request(chain.request())
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }
    @Test fun absentServicesNeverProduceDemoResults()=runBlocking {
        val simulation=SimulationRepository(retrofit(404,""))
        assertTrue(simulation.defaults("MSFT") is Resource.Error)
        assertTrue(simulation.simulate("MSFT",requestFixture()) is Resource.Error)
        assertTrue(AnalysisRepository(retrofit(404,"")).load("MSFT",AnalysisRequest(2024,"simple")) is Resource.Error)
    }
    @Test fun simulationsUseOnePostWithEffectiveParameters()=runBlocking {
        var path="";var requestBody=""
        val repository=SimulationRepository(retrofit(200,Gson().toJson(simulationFixture())){request ->
            path=request.url.encodedPath
            val buffer=okio.Buffer();request.body!!.writeTo(buffer);requestBody=buffer.readUtf8()
        })
        assertTrue(repository.simulate("MSFT",requestFixture()) is Resource.Success)
        assertEquals("/api/v1/companies/MSFT/simulations/",path)
        assertEquals(requestFixture(),Gson().fromJson(requestBody,SimulationRequest::class.java))
    }
    @Test fun invalidServerPercentilesAreRejectedByRepository()=runBlocking {
        val data=simulationFixture().copy(points=listOf(ScenarioPoint("2024-01-31",200.0,100.0,150.0)))
        assertTrue(SimulationRepository(retrofit(200,Gson().toJson(data))).simulate("MSFT",requestFixture()) is Resource.Error)
    }
    @Test fun analysisLanguageIsSentAndUnvalidatedResponseIsNotDisplayed()=runBlocking {
        var sent=""
        val repository=AnalysisRepository(retrofit(200,Gson().toJson(analysisFixture("technical"))){request ->
            val buffer=okio.Buffer();request.body!!.writeTo(buffer);sent=buffer.readUtf8()
        })
        assertTrue(repository.load("MSFT",AnalysisRequest(2024,"technical")) is Resource.Success)
        assertEquals("technical",Gson().fromJson(sent,AnalysisRequest::class.java).level)
        val failed=analysisFixture().copy(validation=AnalysisValidation("failed","2025-01-01"))
        assertTrue(AnalysisRepository(retrofit(200,Gson().toJson(failed))).load("MSFT",AnalysisRequest(2024,"simple")) is Resource.Error)
    }
}
