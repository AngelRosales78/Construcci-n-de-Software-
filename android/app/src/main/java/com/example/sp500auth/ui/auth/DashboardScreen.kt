package com.example.sp500auth.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.util.biometric.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val context = LocalContext.current
    val biometricManager = remember { BiometricAuthManager() }
    var biometricAvailable by remember { mutableStateOf(false) }
    var showBiometricButton by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricAvailable = biometricManager.isBiometricAvailable(context)
        showBiometricButton = biometricAvailable
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bienvenido",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Plataforma de Análisis Financiero S&P 500")

        if (showBiometricButton) {
            Spacer(modifier = Modifier.height(24.dp))

            FilledButton(
                onClick = {
                    biometricManager.authenticate(
                        context = context,
                        onSuccess = { },
                        onError = { },
                        onFailed = { }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acceso Rápido con Huella")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                navController.navigate(NavRoutes.Login.route) {
                    popUpTo(NavRoutes.Dashboard.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión")
        }
    }
}
