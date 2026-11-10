package com.example.sp500auth.integration

import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Optativas: nunca reemplazan la evidencia de UI en dispositivo. */
class LiveSprint3Test {
    private lateinit var api: Retrofit
    private lateinit var remote: ScopedRemote
    private lateinit var token: String
    @Before fun setup() {
        val url = System.getenv("SPRINT_API_URL")
        val access = System.getenv("SPRINT_TEST_ACCESS")
        assumeTrue(!url.isNullOrBlank() && !access.isNullOrBlank())
        token = access!!
        api = Retrofit.Builder().baseUrl(url!!).client(OkHttpClient.Builder().readTimeout(170,TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Authorization","Bearer $token").build()) }.build())
            .addConverterFactory(GsonConverterFactory.create()).build()
        remote = ScopedRemote(api) { token }
    }
    @Test fun realComparisonHistoryAndConclusionUseSharedServer() = runBlocking {
        val pair = ComparisonRepository(api) { token }.compare("GOOGL","META",2024)
        assertTrue(pair.message, pair is Resource.Success)
        val history = HistoryComparisonRepository(api) { token }.load("GOOGL","META","1Y")
        assertTrue(history.message, history is Resource.Success)
        val values = (history as Resource.Success).successData
        assertTrue(values.points.size > 200)
        assertEquals(0.0, values.points.first().a,0.0)
        val conclusion = ConclusionRepository(remote).load("GOOGL","META",2024)
        assertTrue(conclusion.message, conclusion is Resource.Success)
        assertEquals("ai",(conclusion as Resource.Success).successData.generation_mode)
    }
    @Test fun personalTagsAndVisitsRoundTripWithoutLocalExamples() = runBlocking {
        val favorites = FavoritesRepository(api) { token }
        val added = favorites.add("META")
        assertTrue(added is FavoriteResult.Success)
        val favorite = (added as FavoriteResult.Success).value
        val tags = TagsRepository(remote)
        val original = tags.load() as Resource.Success
        val created = tags.edit(null,"QA " + UUID.randomUUID().toString().take(8),"#158569",original.successData.version!!)
        assertTrue(created.message,created is Resource.Success)
        val snapshot = (created as Resource.Success).successData
        val tag = snapshot.tags!!.last()!!
        val assigned = tags.assign(favorite.id!!,listOf(tag.id!!),snapshot.version!!)
        assertTrue(assigned.message,assigned is Resource.Success)
        val removed = tags.delete(tag.id!!,(assigned as Resource.Success).successData.version!!)
        assertTrue(removed.message,removed is Resource.Success)
        assertTrue(favorites.remove(favorite) is FavoriteResult.Success)
        val visits = RecentRepository(remote).visit("GOOGL",UUID.randomUUID().toString())
        assertTrue(visits.message, visits is Resource.Success)
        assertTrue(RecentRepository(remote).list() is Resource.Success)
    }
    @Test fun realPdfJobsPreserveAnalysisVersionAndIntegrity() = runBlocking {
        val repository = ReportRepository(remote, AccessRepository(remote))
        val analysis = ReportAnalysisRepository(remote,ConclusionRepository(remote)).load("GOOGL","META",2024,"simple")
        assertTrue(analysis.message, analysis is Resource.Success)
        val prepared = (analysis as Resource.Success).successData
        val request = ReportRequest(kind="comparison",a="GOOGL",b="META",fiscal_year=2024,include_charts=true,
            include_analysis=true,analysis_version=prepared.version,analysis_origin=prepared.origin)
        val submitted = repository.submit(request)
        assertTrue(submitted.message,submitted is Resource.Success)
        var job = (submitted as Resource.Success).successData
        withTimeout(30_000) {
            while(job.status in setOf("queued","running")) {
                delay(100)
                val status = repository.status(job.id!!,request)
                assertTrue(status.message,status is Resource.Success)
                job = (status as Resource.Success).successData
            }
        }
        assertEquals("ready",job.status)
        assertEquals("vector",job.chart_format)
        assertTrue(repository.download(job) is Resource.Success)
    }
    @Test fun freePlanCannotObtainProtectedComparisonOrPdf() = runBlocking {
        val free = System.getenv("SPRINT_FREE_ACCESS")
        assumeTrue(!free.isNullOrBlank())
        token = free!!
        assertTrue(ComparisonRepository(api) { token }.compare("GOOGL","META",2024) is Resource.Error)
        assertTrue(HistoryComparisonRepository(api) { token }.load("GOOGL","META","1Y") is Resource.Error)
        assertTrue(ReportRepository(remote,AccessRepository(remote)).submit(ReportRequest(kind="comparison",a="GOOGL",b="META",fiscal_year=2024)) is Resource.Error)
    }
}
