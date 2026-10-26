package com.example.sp500auth.integration
import com.example.sp500auth.data.api.models.CompanyPage
import com.example.sp500auth.data.api.models.CompanySummary
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.pillars.overallDisplay
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Runs against an isolated live Django server started by tools/verify_android_server.py. */
class LiveServerTest {
    private lateinit var retrofit: Retrofit
    private lateinit var token: String
    @Before fun setup() {
        val url=System.getenv("SPRINT_API_URL");val access=System.getenv("SPRINT_TEST_ACCESS")
        assumeTrue("Use tools/verify_android_server.py for live integration",!url.isNullOrBlank() && !access.isNullOrBlank())
        token=access!!
        val client=OkHttpClient.Builder().callTimeout(30,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).addInterceptor{chain -> chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $token").build())}.build()
        retrofit=Retrofit.Builder().baseUrl(url!!).client(client).addConverterFactory(GsonConverterFactory.create()).build()
    }
    @Test fun readsFivePillarsAndScoreFromRealServer()=runBlocking {
        val repository=PillarsRepository(retrofit){token}
        val result=repository.load("MSFT",2024) as PillarsResult.Success
        assertEquals(5,result.data.pillars!!.size)
        assertEquals(11,result.data.pillars!!.sumOf{it.metrics.orEmpty().size})
        assertEquals("85,25 / 100",overallDisplay(result.data))
        assertEquals("AssetsCurrent",result.data.pillars!![0].metrics!![0].components!![0].source!!.tag)
    }
    @Test fun requestedMissingYearIsNotSilentlyReplaced()=runBlocking {
        val result=PillarsRepository(retrofit){token}.load("MSFT",2018) as PillarsResult.Success
        assertEquals(2018,result.data.fiscal_year)
        assertEquals("No disponible",overallDisplay(result.data))
    }
    @Test fun realCatalogAndSummaryAreAccessibleWithSession()=runBlocking {
        val repository=SprintRepository(retrofit)
        val page=repository.search("Microsoft",1) as Resource.Success<CompanyPage>
        assertEquals("MSFT",page.successData.results!!.first().ticker)
        val company=repository.summary("MSFT") as Resource.Success<CompanySummary>
        assertEquals("Microsoft",company.successData.name!!.substringBefore(" "))
        assertEquals("2024-06-30",company.successData.as_of)
    }
}
