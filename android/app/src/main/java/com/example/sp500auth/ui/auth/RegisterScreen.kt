package com.example.sp500auth.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.ui.common.AppColors
import com.example.sp500auth.ui.navigation.NavRoutes
import com.example.sp500auth.util.Resource

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val registerState = viewModel.registerState

    LaunchedEffect(registerState) {
        if (registerState is Resource.Success) {
            navController.navigate(NavRoutes.Login.route) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            viewModel.registrationCompleted()
        }
    }

    Box(Modifier.fillMaxSize().background(AppColors.Background), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .statusBarsPadding().imePadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(color = AppColors.Green.copy(alpha = .14f), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.TrendingUp, null, tint = AppColors.Green, modifier = Modifier.padding(12.dp).size(32.dp))
            }
            Text("Crea tu cuenta", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Analiza las 503 empresas del S&P 500 con cinco pilares, escenarios e IA.", color = AppColors.Muted,
                style = MaterialTheme.typography.bodyMedium)

            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Panel)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cuenta", style = MaterialTheme.typography.labelLarge, color = AppColors.Muted)
                    Field(username, { username = it }, "Usuario", Icons.Default.Person)
                    Field(email, { email = it }, "Email", Icons.Default.Email, KeyboardType.Email)

                    Text("Datos personales", style = MaterialTheme.typography.labelLarge, color = AppColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Field(firstName, { firstName = it }, "Nombre", null, modifier = Modifier.weight(1f))
                        Field(lastName, { lastName = it }, "Apellido", null, modifier = Modifier.weight(1f))
                    }
                    val phoneError = registrationPhoneError(phone)
                    Field(phone, { phone = it }, "Teléfono (opcional)", Icons.Default.Phone, KeyboardType.Phone,
                        error = phoneError, help = PHONE_HELP)

                    Text("Seguridad", style = MaterialTheme.typography.labelLarge, color = AppColors.Muted, modifier = Modifier.padding(top = 6.dp))
                    Field(password, { password = it }, "Contraseña", Icons.Default.Lock, KeyboardType.Password, help = PASSWORD_HELP,
                        visual = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña")
                            }
                        })
                    Field(passwordConfirm, { passwordConfirm = it }, "Confirmar Contraseña", Icons.Default.LockReset, KeyboardType.Password,
                        visual = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation())
                }
            }

            Button(
                onClick = { viewModel.register(username, email, password, passwordConfirm, firstName, lastName, phone) },
                enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank() &&
                    registrationPhoneError(phone) == null && registerState !is Resource.Loading,
                modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppColors.Green, contentColor = Color.Black)
            ) {
                if (registerState is Resource.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                else Text("Registrarse", fontWeight = FontWeight.Bold)
            }

            if (registerState is Resource.Error) {
                Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f), shape = RoundedCornerShape(12.dp)) {
                    Text(registerState.message ?: "Error desconocido", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }

            TextButton(onClick = { navController.navigate(NavRoutes.Login.route) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("¿Ya tienes cuenta? Inicia Sesión")
            }
        }
    }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: String, icon: ImageVector?, keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(), error: String? = null, help: String? = null,
    visual: VisualTransformation = VisualTransformation.None, trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = modifier,
        leadingIcon = icon?.let { { Icon(it, contentDescription = null, tint = AppColors.Green) } },
        trailingIcon = trailing, isError = error != null, visualTransformation = visual,
        supportingText = (error ?: help)?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppColors.Green, focusedLabelColor = AppColors.Green, cursorColor = AppColors.Green)
    )
}
