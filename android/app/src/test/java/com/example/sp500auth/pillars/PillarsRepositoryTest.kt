package com.example.sp500auth.pillars

import com.example.sp500auth.data.repository.PillarsRepository
import com.example.sp500auth.data.repository.PillarsResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.util.ArrayDeque
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PillarsRepositoryTest {
    private val responses = ArrayDeque<Pair<Int, String>>()
    private val requests = mutableListOf<Request>()
    private var failNetwork = false
    private fun enqueue(body: String = "", code: Int = 200) { responses.add(code to body) }
    private var accessToken: String? = "test-access"
    private lateinit var repository: PillarsRepository
    @Before fun setup() {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            if (failNetwork) throw IOException("offline fixture")
            val (code, body) = responses.removeFirst()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("Test fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        repository = PillarsRepository(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()) { accessToken }
    }
    @Test fun sendsCompanyYearAndBearerAndReadsComponents() = runBlocking {
        enqueue("""{"schema_version":"pillars-v1","ticker":"MSFT","fiscal_year":2024,"period":{"start":"2023-07-01","end":"2024-06-30"},"pillars":[{"id":"liquidity","metrics":[{"id":"current_ratio","status":"available","value":"2","display_value":"2,00","unit":"times","components":[{"id":"current_assets","value":"200","source":{"url":"https://www.sec.gov/example","tag":"AssetsCurrent"}}]}]}]}""")
        val result = repository.load("MSFT", 2024) as PillarsResult.Success
        val request = requests.single()
        assertEquals("/api/v1/companies/MSFT/pillars/?fiscal_year=2024", request.url.encodedPath + "?" + request.url.encodedQuery)
        assertEquals("Bearer test-access", request.header("Authorization"))
        assertEquals("AssetsCurrent", result.data.pillars!![0].metrics!![0].components!![0].source!!.tag)
        assertEquals("2024-06-30", result.data.period!!.end)
    }
    @Test fun missingYearRemainsUnavailableRatherThanChangingYear() = runBlocking {
        enqueue("""{"schema_version":"pillars-v1","ticker":"MSFT","fiscal_year":2020,"pillars":[]}""")
        val result = repository.load("MSFT", 2020) as PillarsResult.Success
        assertEquals(2020, result.data.fiscal_year)
        assertTrue(result.data.pillars!!.isEmpty())
    }
    @Test fun rejectsAnotherYearOrUnsupportedSchema() = runBlocking {
        enqueue("""{"schema_version":"pillars-v1","ticker":"MSFT","fiscal_year":2023}""")
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
        enqueue("""{"schema_version":"pillars-v2","ticker":"MSFT","fiscal_year":2024}""")
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
    }
    @Test fun handlesUnauthorizedMissingServiceAndProviderFailure() = runBlocking {
        for (code in listOf(400, 401, 403, 404, 503, 500)) {
            enqueue(code = code)
            assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
        }
    }
    @Test fun malformedJsonAndEmptyBodyAreErrors() = runBlocking {
        enqueue("{broken")
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
        enqueue(code = 204)
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
    }
    @Test fun networkFailureIsRecoverable() = runBlocking {
        failNetwork = true
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
        failNetwork = false
        enqueue("""{"schema_version":"pillars-v1","ticker":"MSFT","fiscal_year":2024,"pillars":[]}""")
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Success)
    }
    @Test fun missingSessionMakesNoRequest() = runBlocking {
        accessToken = null
        assertTrue(repository.load("MSFT", 2024) is PillarsResult.Failure)
        assertEquals(0, requests.size)
    }
}
