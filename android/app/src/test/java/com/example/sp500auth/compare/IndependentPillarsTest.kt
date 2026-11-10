package com.example.sp500auth.compare

import com.example.sp500auth.data.repository.ComparisonRepository
import com.example.sp500auth.ui.compare.comparisonRows
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections

class IndependentPillarsTest {
    private val paths = Collections.synchronizedList(mutableListOf<String>())
    private var comparisonStatus = 404
    private var wrongCompany = false
    private val repository = ComparisonRepository(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/")
        .client(OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request(); val path = request.url.encodedPath; paths.add(path)
            assertEquals("Bearer token", request.header("Authorization"))
            val ticker = if ("/AAPL/" in path) "AAPL" else "MSFT"
            val code = if (path.endsWith("compare/")) comparisonStatus else 200
            val body = if (path.endsWith("compare/")) "{}" else """{"schema_version":"pillars-v1","ticker":"${if (wrongCompany) "NVDA" else ticker}","fiscal_year":2024,"pillars":[{"id":"liquidity","score":{"status":"available","value":"80","display_value":"80,00"}}]}"""
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()).addConverterFactory(GsonConverterFactory.create()).build()) { "token" }

    @Test fun absentComparisonUsesTwoRealResponsesWithoutWinnerOrDelta() = runBlocking {
        val result = repository.compare("AAPL", "MSFT", 2024) as Resource.Success
        assertEquals("independent-pillars-v1", result.successData.schema_version)
        assertEquals("AAPL", result.successData.a?.ticker); assertEquals("MSFT", result.successData.b?.ticker)
        assertEquals(3, paths.size)
        assertTrue(paths.contains("/api/v1/companies/AAPL/pillars/"))
        assertTrue(paths.contains("/api/v1/companies/MSFT/pillars/"))
        val row = comparisonRows(result.successData).first()
        assertEquals("80,00 / 100", row.aValue); assertNull(row.higher); assertNull(row.delta)
    }
    @Test fun forbiddenAndServerErrorsNeverBypassComparisonWithOtherRoutes() = runBlocking {
        for (code in listOf(401, 403, 500, 503)) {
            paths.clear(); comparisonStatus = code
            assertTrue(repository.compare("AAPL", "MSFT", 2024) is Resource.Error)
            assertEquals(listOf("/api/v1/compare/"), paths.toList())
        }
    }
    @Test fun fallbackRejectsPillarsOfAnotherCompany() = runBlocking {
        wrongCompany = true
        assertTrue(repository.compare("AAPL", "MSFT", 2024) is Resource.Error)
    }
}
