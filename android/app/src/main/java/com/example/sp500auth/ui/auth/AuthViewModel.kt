package com.example.sp500auth.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.repository.AuthRepository
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var loginState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    var registerState by mutableStateOf<Resource<Unit>>(Resource.Empty())
        private set

    fun login(username: String, password: String) {
        viewModelScope.launch {
            loginState = Resource.Loading()
            loginState = authRepository.login(username, password)
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        passwordConfirm: String,
        firstName: String,
        lastName: String
    ) {
        if (password != passwordConfirm) {
            registerState = Resource.Error("Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            registerState = Resource.Loading()
            registerState = authRepository.register(username, email, password, passwordConfirm, firstName, lastName)
        }
    }

    fun loginWithBiometrics() {
        val savedUsername = authRepository.getAccessToken()
        if (savedUsername != null) {
            login("biometric_user", "biometric_token")
        } else {
            loginState = Resource.Error("No se encontraron credenciales biométricas")
        }
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}
