package com.example.sp500auth.session

import com.example.sp500auth.data.repository.AuthRepository
import com.example.sp500auth.ui.auth.AuthViewModel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricSessionTest {
    @Test fun localLockRequiresOptInAndKeepsTheSession() {
        val repository = mock(AuthRepository::class.java)
        `when`(repository.isBiometricEnabled()).thenReturn(false)
        val vm = AuthViewModel(repository)
        assertFalse(vm.lockSession())
        `when`(repository.isBiometricEnabled()).thenReturn(true)
        assertTrue(vm.lockSession())
        assertTrue(vm.loginState is Resource.Empty)
        assertTrue(vm.sessionState is Resource.Empty)
        verify(repository, times(3)).isBiometricEnabled()
        verifyNoMoreInteractions(repository)
    }

    @Test fun biometricCallbackStillRequiresServerValidation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mock(AuthRepository::class.java)
            `when`(repository.isBiometricEnabled()).thenReturn(true)
            `when`(repository.getUserProfile()).thenReturn(Resource.Error("Sesión vencida"))
            val vm = AuthViewModel(repository)
            vm.loginWithBiometrics()
            advanceUntilIdle()
            assertTrue(vm.loginState is Resource.Error)
            verify(repository).getUserProfile()
        } finally { Dispatchers.resetMain() }
    }

    @Test fun failedOrCancelledBiometricsCannotUnlock() {
        val repository = mock(AuthRepository::class.java)
        `when`(repository.isBiometricEnabled()).thenReturn(true)
        val vm = AuthViewModel(repository)
        vm.biometricError("Cancelado")
        assertTrue(vm.loginState is Resource.Error)
        verify(repository).isBiometricEnabled()
        verifyNoMoreInteractions(repository)
    }
}
