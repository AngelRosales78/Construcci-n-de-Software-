package com.example.sp500auth.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    val forgotPasswordState = viewModel.forgotPasswordState
    val context = LocalContext.current

    LaunchedEffect(forgotPasswordState) {
        if (forgotPasswordState is Resource.Success) {
            Toast.makeText(context, "Enlace de recuperación enviado a tu correo", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar Contraseña") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "¿Olvidaste tu contraseña?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.forgotPassword(email) },
                enabled = email.isNotBlank() && forgotPasswordState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (forgotPasswordState is Resource.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Enviar enlace de recuperación")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (forgotPasswordState is Resource.Error) {
                Text(
                    text = forgotPasswordState.errorMessage ?: "Error desconocido",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
