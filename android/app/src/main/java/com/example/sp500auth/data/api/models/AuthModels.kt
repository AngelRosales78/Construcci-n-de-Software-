package com.example.sp500auth.data.api.models

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val password_confirm: String,
    val first_name: String = "",
    val last_name: String = "",
    val user_type: String = "retail"
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class TokenResponse(
    @SerializedName("access") val accessToken: String,
    @SerializedName("refresh") val refreshToken: String
)

data class RefreshRequest(
    @SerializedName("refresh") val refreshToken: String
)

data class ResetPasswordRequest(
    val uid: String,
    val token: String,
    val password: String,
    @SerializedName("password_confirm") val passwordConfirm: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class UserProfile(
    val id: Int,
    val username: String,
    val email: String,
    val first_name: String,
    val last_name: String,
    val user_type: String,
    val date_joined: String
)
