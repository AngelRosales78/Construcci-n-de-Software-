package com.example.sp500auth.compare

import com.example.sp500auth.data.repository.ComparisonRepository
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class ComparisonRepositoryTest {
    private val requests = mutableListOf<Request>()
    private var code = 200
    private var body = response()
    private var offline = false
    private var token: String? = "fixture-token"
    private val repo = ComparisonRepository(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            requests.add(chain.request())
            if (offline) throw IOException("test offline")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()).addConverterFactory(GsonConverterFactory.create()).build()) { token }

    @Test fun sendsBothTickersYearAndBearerAndKeepsTheirOrder() = runBlocking {
        assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Success)
        val request = requests.single()
        assertEquals("/api/v1/compare/", request.url.encodedPath)
        assertEquals("a=AAPL&b=MSFT&fiscal_year=2024", request.url.encodedQuery)
        assertEquals("Bearer fixture-token", request.header("Authorization"))
        body = response("MSFT", "AAPL")
        assertTrue(repo.compare("MSFT", "AAPL", 2024) is Resource.Success)
    }
    @Test fun validatesBeforeRequestAndRequiresSession() = runBlocking {
        assertTrue(repo.compare("AAPL", "AAPL", 2024) is Resource.Error)
        assertTrue(repo.compare("../AAPL", "MSFT", 2024) is Resource.Error)
        assertTrue(repo.compare("AAPL", "MSFT", 0) is Resource.Error)
        token = null
        assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Error)
        assertTrue(requests.isEmpty())
    }
    @Test fun rejectsMismatchedCompanyYearAndUnsupportedSchema() = runBlocking {
        for (json in listOf(response("NVDA"), response().replace("2024", "2023"), response().replace("comparison-v1", "comparison-v2"), response().replace("pillars-v1", "pillars-v2"))) {
            body = json
            assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Error)
        }
    }
    @Test fun handlesHttpErrorsMalformedJsonAndNetworkRecovery() = runBlocking {
        for (status in listOf(400, 401, 403, 404, 500, 501, 503)) {
            code = status; body = "{}"
            assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Error)
        }
        code = 200; body = "{broken"
        assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Error)
        offline = true
        assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Error)
        offline = false; body = response()
        assertTrue(repo.compare("AAPL", "MSFT", 2024) is Resource.Success)
    }
    companion object {
        fun response(a: String = "AAPL", b: String = "MSFT") = """{"schema_version":"comparison-v1","fiscal_year":2024,"a":{"schema_version":"pillars-v1","ticker":"$a","fiscal_year":2024},"b":{"schema_version":"pillars-v1","ticker":"$b","fiscal_year":2024}}"""
    }
}
