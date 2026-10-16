package com.example.sp500auth.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.UserProfile
import com.example.sp500auth.data.repository.AuthRepository
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    var loginState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    var registerState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    var forgotPasswordState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    var logoutState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    var userProfileState by mutableStateOf<Resource<UserProfile>>(Resource.Empty())
        private set

    var biometricEnabled by mutableStateOf(repository.isBiometricEnabled())
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            logoutState = Resource.Empty()
            loginState = Resource.Loading()
            loginState = repository.login(username, password)
            if (loginState is Resource.Success) biometricEnabled = false
        }
    }
    fun register(username: String, email: String, password: String, passwordConfirm: String, firstName: String, lastName: String) {
        if (password != passwordConfirm) { registerState = Resource.Error("Las contraseñas no coinciden"); return }
        viewModelScope.launch {
            logoutState = Resource.Empty()
            registerState = Resource.Loading()
            registerState = repository.register(username, email, password, passwordConfirm, firstName, lastName)
            if (registerState is Resource.Success) biometricEnabled = false
        }
    }
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            forgotPasswordState = Resource.Loading()
            forgotPasswordState = repository.forgotPassword(email)
        }
    }

    fun loginWithBiometrics() {
        if (!repository.isBiometricEnabled()) { loginState = Resource.Error("Activa la huella después de iniciar sesión."); return }
        viewModelScope.launch {
            loginState = Resource.Loading()
            val profile = repository.getUserProfile()
            userProfileState = profile
            loginState = if (profile is Resource.Success) Resource.Success(Unit)
                else Resource.Error(profile.message ?: "La sesión venció. Usa tu contraseña.")
        }
    }
    fun updateBiometricPreference(enabled: Boolean) {
        repository.setBiometricEnabled(enabled)
        biometricEnabled = repository.isBiometricEnabled()
    }
    fun biometricError(message: String) { loginState = Resource.Error(message) }
    fun logout() {
        viewModelScope.launch {
            logoutState = Resource.Loading()
            logoutState = repository.logout()
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            userProfileState = Resource.Loading()
            userProfileState = repository.getUserProfile()
        }
    }

    fun isLoggedIn(): Boolean = repository.isLoggedIn()
}
