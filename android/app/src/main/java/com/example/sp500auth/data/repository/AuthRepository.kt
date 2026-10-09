package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Resource<Unit> {
        return try {
            val response = authApiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al iniciar sesión")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        passwordConfirm: String,
        firstName: String = "",
        lastName: String = ""
    ): Resource<Unit> {
        return try {
            val request = RegisterRequest(
                username = username,
                email = email,
                password = password,
                password_confirm = passwordConfirm,
                first_name = firstName,
                last_name = lastName,
                user_type = "retail"
            )
            val response = authApiService.register(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al registrar usuario")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                authApiService.logout(RefreshRequest(refreshToken))
            }
            tokenManager.clearTokens()
            Resource.Success(Unit)
        } catch (e: Exception) {
            tokenManager.clearTokens()
            Resource.Success(Unit)
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.isUserLoggedIn()

    fun getAccessToken(): String? = tokenManager.getAccessToken()

    suspend fun verifyCredentialsWithBiometrics(username: String, password: String): Resource<Unit> {
        return try {
            val response = authApiService.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                Resource.Success(Unit)
            } else {
                Resource.Error("Credenciales biométricas inválidas")
            }
        } catch (e: Exception) {
            Resource.Error("Error al verificar credenciales biométricas: ${e.message}")
        }
    }
}
