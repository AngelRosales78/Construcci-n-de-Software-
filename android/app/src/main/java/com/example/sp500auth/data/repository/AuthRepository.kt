package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AuthApiService,
    private val tokens: TokenManager
) {
    suspend fun login(username: String, password: String): Resource<Unit> = try {
        val response = api.login(LoginRequest(username, password))
        val body = response.body()
        if (response.isSuccessful && body != null && body.accessToken.isNotBlank() && body.refreshToken.isNotBlank()) {
            tokens.clearTokens()
            tokens.saveTokens(body.accessToken, body.refreshToken)
            Resource.Success(Unit)
        } else Resource.Error("Usuario, correo o contraseña incorrectos.")
    } catch (e: CancellationException) { throw e
    } catch (_: Exception) { Resource.Error("No se pudo conectar. Inténtalo de nuevo.") }

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
            val response = api.register(request)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al registrar usuario")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun forgotPassword(email: String): Resource<Unit> {
        return try {
            val response = api.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al enviar el enlace de recuperación")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun getUserProfile(): Resource<UserProfile> {
        return try {
            val response = api.getUserProfile()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error("Error al cargar el perfil")
            }
        } catch (e: Exception) {
            Resource.Error("Error de conexión: ${e.message}")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            val refreshToken = tokens.getRefreshToken()
            if (refreshToken != null) {
                api.logout(RefreshRequest(refreshToken))
            }
            tokens.clearTokens()
            Resource.Success(Unit)
        } catch (e: Exception) {
            tokens.clearTokens()
            Resource.Success(Unit)
        }
    }

    fun isLoggedIn(): Boolean = tokens.isUserLoggedIn()

    fun getAccessToken(): String? = tokens.getAccessToken()

    suspend fun verifyCredentialsWithBiometrics(username: String, password: String): Resource<Unit> {
        return try {
            val response = api.login(LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                val tokenResponse = response.body()!!
                tokens.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
                Resource.Success(Unit)
            } else {
                Resource.Error("Credenciales biométricas inválidas")
            }
        } catch (e: Exception) {
            Resource.Error("Error al verificar credenciales biométricas: ${e.message}")
        }
    }
}
