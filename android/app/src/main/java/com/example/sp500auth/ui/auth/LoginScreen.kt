package com.example.sp500auth.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.util.Resource
import com.example.sp500auth.util.biometric.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val loginState = viewModel.loginState
    val context = LocalContext.current
    val biometricManager = remember { BiometricAuthManager() }
    var biometricAvailable by remember { mutableStateOf(false) }
    var biometricAuthInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricAvailable = biometricManager.isBiometricAvailable(context)
    }

    LaunchedEffect(loginState) {
        if (loginState is Resource.Success) {
            navController.navigate(NavRoutes.Dashboard.route) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SP500Auth",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(username, password) },
            enabled = username.isNotBlank() && password.isNotBlank() && loginState !is Resource.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loginState is Resource.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Iniciar Sesión")
            }
        }

        if (biometricAvailable) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    biometricAuthInProgress = true
                    biometricManager.authenticate(
                        context = context,
                        onSuccess = {
                            biometricAuthInProgress = false
                            viewModel.loginWithBiometrics()
                        },
                        onError = { error ->
                            biometricAuthInProgress = false
                        },
                        onFailed = {
                            biometricAuthInProgress = false
                        }
                    )
                },
                enabled = loginState !is Resource.Loading && !biometricAuthInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Iniciar con Huella Dactilar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loginState is Resource.Error) {
            Text(
                text = loginState.message ?: "Error desconocido",
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { navController.navigate(NavRoutes.Register.route) }
        ) {
            Text("¿No tienes cuenta? Regístrate")
        }
    }
}
