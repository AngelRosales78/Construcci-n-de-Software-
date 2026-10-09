package com.example.sp500auth.data.api

import com.example.sp500auth.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("api/register/")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("api/login/")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("api/token/refresh/")
    suspend fun refreshAccessToken(@Body request: RefreshRequest): Response<TokenResponse>

    @POST("api/logout/")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @POST("api/password-reset/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("api/password-reset/confirm/")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    @GET("api/profile/")
    suspend fun getUserProfile(): Response<UserProfile>
}
