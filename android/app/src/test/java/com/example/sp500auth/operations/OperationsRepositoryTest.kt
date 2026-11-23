package com.example.sp500auth.operations

import com.example.sp500auth.data.repository.OperationsRepository
import com.example.sp500auth.data.repository.OperationsFailure
import com.google.gson.JsonObject
import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class OperationsRepositoryTest {
    private fun token(id:String)="e30."+Base64.getUrlEncoder().withoutPadding().encodeToString("{\"user_id\":\"$id\"}".toByteArray())+".signature"
    private fun repo(server:MockWebServer,access:()->String?)=OperationsRepository(Retrofit.Builder().baseUrl(server.url("/api/v1/")).addConverterFactory(GsonConverterFactory.create()).build(),access)

    @Test fun adminReadsServerTotalsAndPermissionFailureIsNotEmptySuccess()=runBlocking {
        MockWebServer().use { server->
            val repository=repo(server){token("1")}
            server.enqueue(MockResponse().setBody("{\"total\":4,\"results\":[{\"plan\":\"pro\",\"count\":1}]}"))
            assertEquals(4,repository.request("admin/monitoring/plans/").get("total").asInt)
            assertEquals("Bearer "+token("1"),server.takeRequest().getHeader("Authorization"))
            server.enqueue(MockResponse().setResponseCode(403))
            try {repository.request("admin/monitoring/plans/");fail("403 must not become empty totals")}
            catch(expected:OperationsFailure){assertTrue(expected.message!!.contains("permiso"))}
        }
    }

    @Test fun noSessionCannotMakePersonalRequest()=runBlocking {
        MockWebServer().use { server->
            try {repo(server){null}.request("notifications/");fail("Session required")}
            catch(expected:OperationsFailure){assertTrue(expected.message!!.contains("sesión"))}
            assertEquals(0,server.requestCount)
        }
    }

    @Test fun mutationUsesServerVersionAndDoesNotPermitExternalDestinations()=runBlocking {
        MockWebServer().use { server->
            val repository=repo(server){token("1")}
            server.enqueue(MockResponse().setBody("{\"version\":3}"))
            val payload=JsonObject().apply{addProperty("version",2);addProperty("rating_changes",false)}
            assertEquals(3,repository.request("user/notification-preferences/","PUT",payload).get("version").asInt)
            val request=server.takeRequest();assertEquals("PUT",request.method);assertTrue(request.body.readUtf8().contains("\"version\":2"))
            try {repository.request("https://example.com/private");fail("External route rejected")}
            catch(expected:IllegalArgumentException){}
        }
    }
}
