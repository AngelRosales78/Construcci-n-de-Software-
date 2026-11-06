package com.example.sp500auth.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource
import com.example.sp500auth.util.biometric.BiometricAuthManager
import com.example.sp500auth.ui.navigation.NavRoutes

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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
        val profile = viewModel.userProfileState
        if (profile is Resource.Success) { Text(profile.successData.username); Text(profile.successData.email) }
        if (profile is Resource.Error) Text(profile.message ?: "No se pudo cargar")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Acceso por huella")
            Switch(checked = viewModel.biometricEnabled, onCheckedChange = { enabled ->
                biometricError = ""
                if (!enabled) viewModel.updateBiometricPreference(false)
                else if (!manager.isBiometricAvailable(context)) biometricError = manager.unavailableReason(context).orEmpty()
                else manager.authenticate(context,
                    onSuccess = { viewModel.updateBiometricPreference(true) },
                    onError = { biometricError = it },
                    onFailed = { biometricError = "Huella no reconocida. Inténtalo de nuevo." })
            })
        }
        Text(if (viewModel.biometricEnabled) "Huella activada para esta cuenta en este teléfono." else "Activa la huella después de iniciar sesión con tu contraseña.")
        Text("Android comprueba tu identidad. La app no recibe ni guarda tu huella.")
        if (biometricError.isNotBlank()) Text(biometricError, color = MaterialTheme.colorScheme.error)
        if (viewModel.biometricEnabled) {
            Button(onClick = {
                if (viewModel.lockSession()) {
                    navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
                }
            }, enabled = viewModel.logoutState !is Resource.Loading) { Text("Bloquear y usar huella") }
            Text("Puedes volver a entrar con huella al abrir la app. Si cierras sesión, deberás usar tu contraseña y activarla otra vez.")
        }
        TextButton(onClick = { navController.popBackStack() }) { Text("Volver") }
        Button(onClick = { viewModel.logout() }, enabled = viewModel.logoutState !is Resource.Loading) { Text("Cerrar sesión") }
    }
}
