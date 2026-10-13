package com.example.sp500auth.data.api

import com.example.sp500auth.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {

    @POST("auth/register/")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/token/")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh/")
    suspend fun refreshAccessToken(@Body request: RefreshRequest): Response<TokenResponse>

    @POST("auth/logout/")
    suspend fun logout(@Body request: RefreshRequest): Response<Unit>

    @POST("auth/password/reset/")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<Unit>

    @POST("auth/password/reset/confirm/")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Unit>

    @PATCH("auth/change-password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Unit>

    @GET("auth/profile/")
    suspend fun getUserProfile(): Response<UserProfile>
}
