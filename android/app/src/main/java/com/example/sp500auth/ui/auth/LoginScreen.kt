package com.example.sp500auth.ui.auth

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource
import com.example.sp500auth.util.biometric.BiometricAuthManager
import com.example.sp500auth.ui.navigation.NavRoutes

private val DarkBg = Color(0xFF0D1117)
private val CardBg = Color(0xFF161B22)
private val InputBg = Color(0xFF0F141C)
private val PrimaryGreen = Color(0xFF00FF87)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF8B949E)
private val BorderDark = Color(0xFF21262D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController? = null,
    viewModel: AuthViewModel? = null,
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
    onBiometricAuth: () -> Unit = {}
) {
    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val loginState = viewModel?.loginState
    LaunchedEffect(loginState) {
        if (loginState is Resource.Success) {
            onLoginSuccess()
            navController?.navigate(NavRoutes.Dashboard.route) {
                popUpTo(NavRoutes.Login.route) { inclusive = true }
            }
        }
    }
    fun launchBiometricPrompt() {
        if (viewModel?.biometricEnabled != true) {
            viewModel?.biometricError("Inicia sesión y activa la huella desde tu perfil.")
            return
        }
        BiometricAuthManager().authenticate(context,
            onSuccess = { viewModel.loginWithBiometrics() },
            onError = { viewModel.biometricError(it) },
            onFailed = { viewModel.biometricError("Huella no reconocida. Inténtalo de nuevo.") })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S&P\n500",
                    color = TextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                        .align(Alignment.BottomEnd)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "S&P 500 Finance",
                color = TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Acceso a Mercados e Inteligencia Financiera",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InputBg)
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CardBg,
                        contentColor = PrimaryGreen
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = {
                        onNavigateToRegister()
                        navController?.navigate(NavRoutes.Register.route)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Crear Cuenta",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "USUARIO O CORREO ELECTRÓNICO",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "REQUERIDO",
                    color = PrimaryGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Usuario",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedBorderColor = BorderDark,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONTRASEÑA",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "¿Olvidaste?",
                    color = PrimaryGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        onForgotPassword()
                        navController?.navigate(NavRoutes.ForgotPassword.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Contraseña",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Ver Contraseña",
                            tint = TextMuted
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = InputBg,
                    unfocusedContainerColor = InputBg,
                    focusedBorderColor = BorderDark,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (loginState is Resource.Error) Text(loginState.message ?: "No se pudo acceder", color = MaterialTheme.colorScheme.error)
            if (viewModel?.logoutState is Resource.Error) Text(viewModel.logoutState.message ?: "Cierre local realizado")
            // Biometric login never navigates on cancellation or failure.
            OutlinedButton(
                onClick = {
                    onBiometricAuth()
                    launchBiometricPrompt()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = InputBg,
                    contentColor = TextWhite
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Huella",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Acceso con Huella Dactilar",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    viewModel?.login(usernameOrEmail.trim(), password)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (loginState is Resource.Loading) "Validando…" else "Iniciar sesión",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Ingresar",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "¿No tienes cuenta? ",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Text(
                    text = "Registrate",
                    color = PrimaryGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        onNavigateToRegister()
                        navController?.navigate(NavRoutes.Register.route)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(InputBg)
                    .border(0.5.dp, BorderDark, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Tu sesión se valida con el servidor",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}