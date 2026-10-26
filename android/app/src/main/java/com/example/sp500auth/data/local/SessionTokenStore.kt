package com.example.sp500auth.data.local
interface SessionTokenStore {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    fun saveTokens(accessToken: String, refreshToken: String)
    fun clearTokens()
}
