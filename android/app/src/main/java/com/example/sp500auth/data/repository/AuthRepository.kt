package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(private val api: AuthApiService, private val tokens: TokenManager) {
    private val refreshMutex = Mutex()

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

    suspend fun register(username: String, email: String, password: String, passwordConfirm: String,
                         firstName: String = "", lastName: String = ""): Resource<Unit> = try {
        val response = api.register(RegisterRequest(username, email, password, passwordConfirm, firstName, lastName))
        val body = response.body()
        if (response.isSuccessful && body != null && body.accessToken.isNotBlank() && body.refreshToken.isNotBlank()) {
            tokens.clearTokens()
            tokens.saveTokens(body.accessToken, body.refreshToken)
            Resource.Success(Unit)
        } else Resource.Error("Revisa los datos, la contraseña y si la cuenta ya existe.")
    } catch (e: CancellationException) { throw e
    } catch (_: Exception) { Resource.Error("No se pudo completar el registro. Inténtalo de nuevo.") }

    suspend fun getUserProfile(): Resource<UserProfile> = refreshMutex.withLock {
        if (!isLoggedIn()) return@withLock Resource.Empty()
        try {
            var response = api.getUserProfile()
            if (response.code() == 401) {
                val refresh = tokens.getRefreshToken() ?: return@withLock Resource.Empty()
                val renewed = api.refreshAccessToken(RefreshRequest(refresh))
                val body = renewed.body()
                if (!renewed.isSuccessful || body == null || body.accessToken.isBlank() || body.refreshToken.isBlank()) {
                    if (renewed.code() == 401 || renewed.code() == 403) tokens.clearTokens()
                    return@withLock Resource.Error("La sesión no pudo renovarse. Vuelve a iniciar sesión.")
                }
                tokens.saveTokens(body.accessToken, body.refreshToken)
                response = api.getUserProfile()
            }
            if (response.isSuccessful && response.body() != null) Resource.Success(response.body()!!)
            else {
                if (response.code() == 401 || response.code() == 403) tokens.clearTokens()
                Resource.Error("No se pudo verificar la sesión. Inicia sesión o vuelve a intentarlo.")
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { Resource.Error("No hay conexión para verificar la sesión. Vuelve a intentarlo.") }
    }

    suspend fun forgotPassword(email: String): Resource<Unit> = try {
        if (api.forgotPassword(ForgotPasswordRequest(email)).isSuccessful) Resource.Success(Unit)
        else Resource.Error("No se pudo enviar la solicitud. Revisa el correo.")
    } catch (e: CancellationException) { throw e
    } catch (_: Exception) { Resource.Error("No se pudo conectar para solicitar el enlace.") }

    suspend fun logout(): Resource<Unit> {
        var confirmed = false
        try {
            if (tokens.getRefreshToken() != null) {
                getUserProfile()
                val refresh = tokens.getRefreshToken()
                if (refresh != null) confirmed = api.logout(LogoutRequest(refresh)).isSuccessful
                else confirmed = true
            } else confirmed = true
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { confirmed = false
        } finally { tokens.clearTokens() }
        return if (confirmed) Resource.Success(Unit)
        else Resource.Error("Sesión cerrada aquí. No se confirmó el cierre remoto por falta de conexión.")
    }

    fun isLoggedIn(): Boolean = tokens.isUserLoggedIn()
    fun isBiometricEnabled(): Boolean = tokens.isBiometricEnabled() && isLoggedIn()
    fun setBiometricEnabled(enabled: Boolean) { tokens.setBiometricEnabled(enabled && isLoggedIn()) }
}
