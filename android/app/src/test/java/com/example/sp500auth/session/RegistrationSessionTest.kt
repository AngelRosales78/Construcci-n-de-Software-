package com.example.sp500auth.session

import com.example.sp500auth.data.api.AuthApiService
import com.example.sp500auth.data.api.models.RegisterRequest
import com.example.sp500auth.data.api.models.TokenResponse
import com.example.sp500auth.data.local.TokenManager
import com.example.sp500auth.data.repository.AuthRepository
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import retrofit2.Response

class RegistrationSessionTest {
    @Test fun registeringDoesNotPersistAnAuthenticatedSession() = runTest {
        val api = mock(AuthApiService::class.java)
        val tokens = mock(TokenManager::class.java)
        val request = RegisterRequest("fixture", "fixture@example.invalid", "Ficticia8!", "Ficticia8!", "", "")
        `when`(api.register(request)).thenReturn(Response.success(TokenResponse("access", "refresh")))
        val result = AuthRepository(api, tokens).register("fixture", "fixture@example.invalid", "Ficticia8!", "Ficticia8!")
        assertTrue(result is Resource.Success)
        verify(tokens).clearTokens()
        verify(tokens, never()).saveTokens(anyString(), anyString())
    }
    @Test fun malformedRegistrationDoesNotCreateSession() = runTest {
        val api = mock(AuthApiService::class.java)
        val tokens = mock(TokenManager::class.java)
        val request = RegisterRequest("fixture", "fixture@example.invalid", "Ficticia8!", "Ficticia8!", "", "")
        `when`(api.register(request)).thenReturn(Response.success(TokenResponse("", "")))
        assertTrue(AuthRepository(api, tokens).register("fixture", "fixture@example.invalid", "Ficticia8!", "Ficticia8!") is Resource.Error)
        verify(tokens, never()).saveTokens(anyString(), anyString())
    }
}
