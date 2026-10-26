package com.example.sp500auth.session

import com.example.sp500auth.data.api.*
import com.example.sp500auth.data.local.SessionTokenStore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SessionAuthenticatorTest {
    private class Store : SessionTokenStore {
        var access: String? = "old"; var refresh: String? = "refresh"
        override fun getAccessToken() = access
        override fun getRefreshToken() = refresh
        override fun saveTokens(accessToken: String, refreshToken: String) { access = accessToken; refresh = refreshToken }
        override fun clearTokens() { access = null; refresh = null }
    }
    private fun response(token: String? = "old", prior: Response? = null): Response {
        val request = Request.Builder().url("https://fixture.invalid/api/v1/companies/MSFT/pillars/")
        if (token != null) request.header("Authorization", "Bearer $token")
        return Response.Builder().request(request.build()).code(401).message("Unauthorized")
            .protocol(Protocol.HTTP_1_1).priorResponse(prior).build()
    }
    private fun authenticator(store: Store, code: Int, body: String): SessionAuthenticator {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).code(code).message("fixture").protocol(Protocol.HTTP_1_1)
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://fixture.invalid/api/v1/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(SessionRefreshApi::class.java)
        return SessionAuthenticator(store, api)
    }
    @Test fun refreshRotatesBothTokensAndRetries() {
        val store = Store()
        val result = authenticator(store, 200, """{"access":"new","refresh":"rotated"}""").authenticate(null, response())
        assertEquals("Bearer new", result!!.header("Authorization"))
        assertEquals("rotated", store.refresh)
    }
    @Test fun concurrentRefreshReusesNewToken() {
        val store = Store().apply { access = "already-renewed" }
        val result = authenticator(store, 500, "").authenticate(null, response())
        assertEquals("Bearer already-renewed", result!!.header("Authorization"))
    }
    @Test fun rejectedRefreshClearsSession() {
        val store = Store()
        assertNull(authenticator(store, 401, "").authenticate(null, response()))
        assertNull(store.access)
    }
    @Test fun providerFailureKeepsSessionForRetry() {
        val store = Store()
        assertNull(authenticator(store, 503, "").authenticate(null, response()))
        assertEquals("old", store.access)
    }
    @Test fun doesNotLoopOrRefreshPublicRequests() {
        val store = Store(); val auth = authenticator(store, 200, "")
        assertNull(auth.authenticate(null, response(null)))
        assertNull(auth.authenticate(null, response(prior = response())))
    }
}
