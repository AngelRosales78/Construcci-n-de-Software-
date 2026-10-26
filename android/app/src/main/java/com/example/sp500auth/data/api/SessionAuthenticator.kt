package com.example.sp500auth.data.api

import com.example.sp500auth.data.api.models.RefreshRequest
import com.example.sp500auth.data.api.models.TokenResponse
import com.example.sp500auth.data.local.SessionTokenStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

interface SessionRefreshApi {
    @POST("auth/refresh/") fun refresh(@Body request: RefreshRequest): Call<TokenResponse>
}

/** A single synchronized refresh; the refresh client has no authenticator of its own. */
class SessionAuthenticator(private val store: SessionTokenStore, private val api: SessionRefreshApi) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null || response.request.header("Authorization") == null) return null
        return synchronized(store) {
            val current = store.getAccessToken() ?: return@synchronized null
            if (response.request.header("Authorization") != "Bearer $current")
                return@synchronized response.request.newBuilder().header("Authorization", "Bearer $current").build()
            val refresh = store.getRefreshToken() ?: return@synchronized null
            try {
                val renewed = api.refresh(RefreshRequest(refresh)).execute()
                val body = renewed.body()
                if (!renewed.isSuccessful || body?.accessToken.isNullOrBlank() || body?.refreshToken.isNullOrBlank()) {
                    if (renewed.code() == 401 || renewed.code() == 403) store.clearTokens()
                    return@synchronized null
                }
                store.saveTokens(body!!.accessToken, body.refreshToken)
                response.request.newBuilder().header("Authorization", "Bearer ${body.accessToken}").build()
            } catch (_: IOException) { null }
        }
    }
}
