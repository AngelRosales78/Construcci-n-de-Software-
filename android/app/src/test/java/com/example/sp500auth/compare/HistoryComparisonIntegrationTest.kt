package com.example.sp500auth.compare

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.compare.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryComparisonIntegrationTest {
    @Test fun existingHistoryRoutesUseSameRangeAndBearerAndRejectWrongTicker() = runBlocking {
        var wrong = false
        val requests = Collections.synchronizedList(mutableListOf<Request>())
        val api = Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request(); requests.add(req)
            val ticker = if (wrong) "NVDA" else if ("AAPL" in req.url.encodedPath) "AAPL" else "MSFT"
            Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
                .body("""{"ticker":"$ticker","range":"6M","is_demo":false,"history":[{"date":"2024-01-01","price":100},{"date":"2024-01-02","price":110}]}""".toResponseBody("application/json".toMediaType())).build()
        }.build()).addConverterFactory(GsonConverterFactory.create()).build()
        val source = HistoryComparisonRepository(api) { "fixture-token" }
        assertTrue(source.load("AAPL", "MSFT", "6M") is Resource.Success)
        assertEquals(2, requests.size)
        requests.forEach { assertEquals("Bearer fixture-token", it.header("Authorization")); assertEquals("6M", it.url.queryParameter("range")) }
        wrong = true; assertTrue(source.load("AAPL", "MSFT", "6M") is Resource.Error)
    }
    @Test fun staleRangeCannotReplaceNewRangeAndSavedRangeSurvivesRecreation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val source = object : HistoryComparisonSource {
                override suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> {
                    if (range == "1Y") withContext(NonCancellable) { delay(1000) }
                    return Resource.Success(AlignedHistory(a, b, range, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
                }
            }
            val saved = SavedStateHandle(); val vm = HistoryComparisonViewModel(source, saved)
            vm.load("AAPL", "MSFT", "1Y"); runCurrent()
            vm.load("AAPL", "MSFT", "6M"); advanceUntilIdle()
            assertEquals("6M", (vm.state as Resource.Success).successData.range)
            assertEquals("6M", HistoryComparisonViewModel(source, saved).range)
            vm.load("", "MSFT"); assertTrue(vm.state is Resource.Empty)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun errorRetainsPairAndCanBeRetried() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var failed = true
            val source = object : HistoryComparisonSource {
                override suspend fun load(a: String, b: String, range: String): Resource<AlignedHistory> = if (failed) Resource.Error("Sin red") else Resource.Success(AlignedHistory(a, b, range, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()))
            }
            val vm = HistoryComparisonViewModel(source, SavedStateHandle())
            vm.load("AAPL", "MSFT"); advanceUntilIdle(); assertTrue(vm.state is Resource.Error)
            assertEquals("AAPL", vm.a); failed = false; vm.load(vm.a, vm.b); advanceUntilIdle(); assertTrue(vm.state is Resource.Success)
        } finally { Dispatchers.resetMain() }
    }
}
