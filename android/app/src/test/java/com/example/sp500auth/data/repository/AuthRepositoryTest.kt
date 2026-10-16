package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

class AuthRepositoryTest {
    private lateinit var tokens: TokenManager
    private lateinit var api: FakeApi
    private lateinit var repository: AuthRepository
    private var access: String? = "old-access"
    private var refresh: String? = "old-refresh"
    private var biometric = false

    @Before fun setup() {
        access = "old-access"; refresh = "old-refresh"; biometric = false
        tokens = mock(TokenManager::class.java)
        `when`(tokens.isUserLoggedIn()).thenAnswer { access != null && refresh != null }
        `when`(tokens.getAccessToken()).thenAnswer { access }
        `when`(tokens.getRefreshToken()).thenAnswer { refresh }
        `when`(tokens.isBiometricEnabled()).thenAnswer { biometric }
        doAnswer { access = it.getArgument(0); refresh = it.getArgument(1); null }
            .`when`(tokens).saveTokens(anyString(), anyString())
        doAnswer { access = null; refresh = null; biometric = false; null }.`when`(tokens).clearTokens()
        doAnswer { biometric = it.getArgument(0); null }.`when`(tokens).setBiometricEnabled(anyBoolean())
        api = FakeApi(); repository = AuthRepository(api, tokens)
    }

    @Test fun validLoginStoresRealTokensAndResetsBiometricPreference() = runBlocking {
        biometric = true
        assertTrue(repository.login("user", "password") is Resource.Success)
        assertEquals("new-access", access); assertEquals("new-refresh", refresh)
        assertFalse(repository.isBiometricEnabled())
    }
    @Test fun rejectedLoginDoesNotGrantNewSession() = runBlocking {
        access = null; refresh = null
        api.loginResponse = Response.error(401, "{}".toResponseBody())
        assertTrue(repository.login("user", "wrong") is Resource.Error)
        assertFalse(repository.isLoggedIn())
    }
    @Test fun registrationStoresTokensBeforeOpeningDashboard() = runBlocking {
        access = null; refresh = null
        assertTrue(repository.register("user", "u@example.test", "Password!1", "Password!1") is Resource.Success)
        assertEquals("new-access", access); assertTrue(repository.isLoggedIn())
    }
    @Test fun expiredAccessRotatesRefreshAndRechecksProfile() = runBlocking {
        api.profiles.add(Response.error(401, "{}".toResponseBody()))
        api.profiles.add(Response.success(api.user))
        assertTrue(repository.getUserProfile() is Resource.Success)
        assertEquals("old-refresh", api.refreshRequest?.refreshToken)
        assertEquals("new-refresh", refresh)
        assertEquals(2, api.profileCalls)
    }
    @Test fun revokedRefreshClearsLocalSession() = runBlocking {
        api.profiles.add(Response.error(401, "{}".toResponseBody()))
        api.refreshResponse = Response.error(401, "{}".toResponseBody())
        assertTrue(repository.getUserProfile() is Resource.Error)
        assertFalse(repository.isLoggedIn())
    }
    @Test fun offlineRestoreDoesNotPretendToBeAuthenticated() = runBlocking {
        api.offline = true
        assertTrue(repository.getUserProfile() is Resource.Error)
        assertEquals("old-refresh", refresh) // Keep a retry possible, but grant no navigation.
    }
    @Test fun logoutUsesSharedContractAndAlwaysClearsLocalTokens() = runBlocking {
        biometric = true
        assertTrue(repository.logout() is Resource.Success)
        assertEquals("old-refresh", api.logoutRequest?.refreshToken)
        assertFalse(repository.isLoggedIn()); assertFalse(biometric)
    }
    @Test fun offlineLogoutClearsTokensButReportsUnconfirmedRemoteLogout() = runBlocking {
        api.offline = true
        assertTrue(repository.logout() is Resource.Error)
        assertFalse(repository.isLoggedIn())
    }
    @Test fun biometricCannotBeEnabledWithoutAnAuthenticatedSession() {
        access = null; refresh = null
        repository.setBiometricEnabled(true)
        assertFalse(repository.isBiometricEnabled())
    }

    private class FakeApi : AuthApiService {
        val user = UserProfile(1, "user", "u@example.test", "", "", "retail")
        val profiles = java.util.ArrayDeque<Response<UserProfile>>()
        var profileCalls = 0
        var offline = false
        var loginResponse = Response.success(TokenResponse("new-access", "new-refresh"))
        var refreshResponse = Response.success(TokenResponse("new-access", "new-refresh"))
        var refreshRequest: RefreshRequest? = null
        var logoutRequest: LogoutRequest? = null
        override suspend fun register(request: RegisterRequest) = loginResponse
        override suspend fun login(request: LoginRequest) = loginResponse
        override suspend fun refreshAccessToken(request: RefreshRequest): Response<TokenResponse> {
            refreshRequest = request
            return refreshResponse
        }
        override suspend fun logout(request: LogoutRequest): Response<Unit> {
            if (offline) throw IOException("offline")
            logoutRequest = request
            return Response.success(Unit)
        }
        override suspend fun getUserProfile(): Response<UserProfile> {
            profileCalls++
            if (offline) throw IOException("offline")
            return if (profiles.isEmpty()) Response.success(user) else profiles.removeFirst()
        }
        override suspend fun forgotPassword(request: ForgotPasswordRequest) = Response.success(Unit)
        override suspend fun resetPassword(request: ResetPasswordRequest) = Response.success(Unit)
        override suspend fun changePassword(request: ChangePasswordRequest) = Response.success(Unit)
    }
}
