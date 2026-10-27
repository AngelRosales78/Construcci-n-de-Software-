package com.example.sp500auth.integration

import com.example.sp500auth.data.repository.SprintRepository
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CompanyFiltersTest {
    @Test fun filtersAndSortAreSentToServer() = runTest {
        var received: HttpUrl? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            received = chain.request().url
            Response.Builder().request(chain.request()).code(200).message("fixture").protocol(Protocol.HTTP_1_1)
                .body("""{"count":0,"results":[]}""".toResponseBody("application/json".toMediaType())).build()
        }.build()
        val retrofit = Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        assertTrue(SprintRepository(retrofit).search("micro", 2, "technology", "-market_cap") is Resource.Success)
        assertEquals("micro", received!!.queryParameter("q"))
        assertEquals("2", received!!.queryParameter("page"))
        assertEquals("technology", received!!.queryParameter("sector"))
        assertEquals("-market_cap", received!!.queryParameter("ordering"))
    }
}
