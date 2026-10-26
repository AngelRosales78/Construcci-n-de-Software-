package com.example.sp500auth.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource
import com.example.sp500auth.util.biometric.BiometricAuthManager

@Composable
fun ForgotPasswordScreen(navController: NavController, viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    val state = viewModel.forgotPasswordState
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Recuperar acceso", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(email, { email = it }, label = { Text("Correo registrado") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.forgotPassword(email.trim()) }, enabled = email.contains('@') && state !is Resource.Loading) { Text("Solicitar enlace") }
        if (state is Resource.Loading) CircularProgressIndicator()
        if (state is Resource.Success) Text("Si el correo está registrado, recibirás un enlace para restablecer tu contraseña.")
        if (state is Resource.Error) Text(state.message ?: "No se pudo enviar", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { navController.popBackStack() }) { Text("Volver al acceso") }
    }
}

@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    var biometricError by remember { mutableStateOf("") }
    val manager = remember { BiometricAuthManager() }
    LaunchedEffect(Unit) { viewModel.fetchUserProfile() }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
        val profile = viewModel.userProfileState
        if (profile is Resource.Success) { Text(profile.successData.username); Text(profile.successData.email) }
        if (profile is Resource.Error) Text(profile.message ?: "No se pudo cargar")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Acceso por huella")
            Switch(checked = viewModel.biometricEnabled, onCheckedChange = { enabled ->
                biometricError = ""
                if (!enabled) viewModel.updateBiometricPreference(false)
                else if (!manager.isBiometricAvailable(context)) biometricError = "No hay una huella registrada disponible."
                else manager.authenticate(context,
                    onSuccess = { viewModel.updateBiometricPreference(true) },
                    onError = { biometricError = it },
                    onFailed = { biometricError = "Huella no reconocida. Inténtalo de nuevo." })
            })
        }
        if (biometricError.isNotBlank()) Text(biometricError, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
        Button(onClick = { viewModel.logout() }, enabled = viewModel.logoutState !is Resource.Loading) { Text("Cerrar sesión") }
    }
}
