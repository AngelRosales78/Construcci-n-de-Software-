package com.example.sp500auth.sprint3

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

class RemainingHttpTest {
    private val requests=mutableListOf<Request>();private var user="1";private var plan="pro";private var brokenOwner=false;private var rejectSubmit=false
    private var requested=ReportRequest(a="AAPL",fiscal_year=2024)
    private val bytes="%PDF-1.4\nfixture\n%%EOF".toByteArray()
    private val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private val gson=Gson()
    private fun token()="e30."+Base64.getUrlEncoder().withoutPadding().encodeToString("""{"user_id":$user}""".toByteArray())+".fixture"
    private val remote=ScopedRemote(Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(OkHttpClient.Builder().addInterceptor {chain->
        val request=chain.request();requests+=request
        assertEquals(user,request.tag(AccountBinding::class.java)?.owner)
        assertEquals("Bearer ${token()}",request.header("Authorization"))
        val path=request.url.encodedPath
        var type="application/json";var code=200
        val body:ByteArray=when {
            path.endsWith("subscription/")->gson.toJson(AccessInfo("subscription-v1",if(brokenOwner)"2" else user,plan,true,listOf("compare"),Instant.now().plusSeconds(100).toString())).toByteArray()
            path.endsWith("reports/jobs/")->{val buffer=Buffer();request.body!!.writeTo(buffer);requested=gson.fromJson(buffer.readUtf8(),ReportRequest::class.java);code=if(rejectSubmit)400 else 202;if(rejectSubmit)"{}".toByteArray()else gson.toJson(ReportJob("report-job-v1",user,"job1","queued",requested)).toByteArray()}
            path.endsWith("file/")->{type="application/pdf";bytes}
            path.endsWith("job1/")->gson.toJson(ReportJob("report-job-v1",user,"job1","ready",requested,"snapshot",hash,bytes.size.toLong())).toByteArray()
            path.endsWith("tags/")->gson.toJson(TagsSnapshot("tags-v1",if(brokenOwner)"2" else user,"v1",emptyList(),emptyList())).toByteArray()
            else->{code=404;"{}".toByteArray()}
        }
        Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Fixture").body(body.toResponseBody(type.toMediaType())).build()
    }.build()).addConverterFactory(GsonConverterFactory.create()).build()){token()}
    @Test fun reportUsesConfirmedMetadataAndProtectedFixedFileRoute()=runBlocking {
        val source=ReportRepository(remote,AccessRepository(remote));val request=ReportRequest(a="AAPL",fiscal_year=2024)
        val created=source.submit(request) as Resource.Success
        val ready=source.status(created.successData.id!!,request) as Resource.Success
        val pdf=source.download(ready.successData) as Resource.Success
        assertArrayEquals(bytes,pdf.successData.bytes);assertEquals(request,pdf.successData.request)
        assertTrue(requests.any{it.method=="POST" && it.url.encodedPath=="/api/v1/reports/jobs/"})
        assertEquals("/api/v1/reports/jobs/job1/file/",requests.last().url.encodedPath)
    }
    @Test fun freeAccountCannotSubmitComparativeJob()=runBlocking {
        plan="free";val source=ReportRepository(remote,AccessRepository(remote))
        assertTrue(source.submit(ReportRequest(a="AAPL",b="MSFT",kind="comparison",fiscal_year=2024)) is Resource.Error)
        assertTrue(requests.none{it.method=="POST"})
    }
    @Test fun anotherOwnerAndMissingServicesAreErrorsNotExamples()=runBlocking {
        brokenOwner=true;assertTrue(AccessRepository(remote).load() is Resource.Error);assertTrue(TagsRepository(remote).load() is Resource.Error)
        brokenOwner=false;assertTrue(ConclusionRepository(remote).load("AAPL","MSFT",2024) is Resource.Error)
    }
    @Test fun anUnchangedTagSnapshotCannotPretendThatCreateSucceeded()=runBlocking {
        assertTrue(TagsRepository(remote).edit(null,"Tecnología","#36F5A0","v1") is Resource.Error)
        val request=requests.single();assertEquals("POST",request.method)
        val buffer=Buffer();request.body!!.writeTo(buffer);assertTrue(buffer.readUtf8().contains("\"version\":\"v1\""))
    }
    @Test fun knownServerRejectionIsDistinguishedFromUncertainNetworkFailure()=runBlocking {
        rejectSubmit=true
        val result=ReportRepository(remote,AccessRepository(remote)).submit(ReportRequest(a="AAPL",fiscal_year=2024)) as Resource.Error
        assertEquals("not_submitted",result.errorData?.status)
    }
}
