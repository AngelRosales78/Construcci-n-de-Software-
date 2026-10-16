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
