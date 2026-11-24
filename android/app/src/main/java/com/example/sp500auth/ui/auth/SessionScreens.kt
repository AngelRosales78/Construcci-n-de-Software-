package com.example.sp500auth.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.UserProfile
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: AuthViewModel, account: com.example.sp500auth.ui.ops.AccountViewModel? = null) {
    val context = LocalContext.current
    var biometricError by remember { mutableStateOf("") }
    val manager = remember { BiometricAuthManager() }
    LaunchedEffect(Unit) { viewModel.fetchUserProfile(); account?.load() }
    val signingOut = viewModel.logoutState is Resource.Loading
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) LogoutConfirmDialog({ confirmLogout = false; viewModel.logout() }, { confirmLogout = false })
    val biometricEnabled = viewModel.biometricEnabled
    val colors = MaterialTheme.colorScheme
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Mi cuenta", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
        )
    }, containerColor = colors.background) { insets ->
        Box(Modifier.fillMaxSize().padding(insets), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth().fillMaxHeight()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                when (val profile = viewModel.userProfileState) {
                    is Resource.Success -> ProfileIdentityCard(account?.profile?.value ?: profile.successData, account?.photo)
                    is Resource.Error -> Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(profile.message ?: "No se pudo cargar el perfil.", color = colors.error)
                            TextButton(onClick = viewModel::fetchUserProfile) { Text("Reintentar") }
                        }
                    }
                    else -> LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Perfil y avisos", style = MaterialTheme.typography.titleMedium)
                    AccountLink(Icons.Default.Edit, "Editar perfil", "Nombre, apellido y foto") { navController.navigate("profile/edit") }
                    AccountLink(Icons.Default.Notifications, "Avisos", "Cambios de calificación de tus favoritas") { navController.navigate("notifications") }
                    AccountLink(Icons.Default.Tune, "Preferencias de avisos", "Tipos de aviso y canales") { navController.navigate("notifications/preferences") }
                    if (account?.isAdmin == true) AccountLink(Icons.Default.AdminPanelSettings, "Administración", "Usuarios, empresas y monitoreo") { navController.navigate("admin") }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suscripción", style = MaterialTheme.typography.titleMedium)
                    Card(onClick = { navController.navigate("plans") }, colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = colors.primary)
                            Column(Modifier.weight(1f)) {
                                Text("Plan y suscripción", style = MaterialTheme.typography.titleSmall)
                                Text("Estado, vencimiento, comprobantes y cambio de plan", style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seguridad", style = MaterialTheme.typography.titleMedium)
                    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = colors.primary)
                                Column(Modifier.weight(1f)) {
                                    Text("Acceso por huella", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        if (biometricEnabled) "Activado en este dispositivo" else "Desactivado",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    enabled = !signingOut,
                                    modifier = Modifier.semantics { contentDescription = "Acceso por huella" },
                                    onCheckedChange = { enabled ->
                                        biometricError = ""
                                        if (!enabled) viewModel.updateBiometricPreference(false)
                                        else if (!manager.isBiometricAvailable(context)) {
                                            biometricError = manager.unavailableReason(context).orEmpty()
                                        } else manager.authenticate(
                                            context,
                                            onSuccess = { viewModel.updateBiometricPreference(true) },
                                            onError = { biometricError = it },
                                            onFailed = { biometricError = "Huella no reconocida. Inténtalo de nuevo." }
                                        )
                                    }
                                )
                            }
                            Text(
                                if (biometricEnabled) "Bloquea la app para volver a entrar con tu huella."
                                else "Usa la huella registrada en Android para volver a entrar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            if (biometricError.isNotBlank()) {
                                Text(biometricError, style = MaterialTheme.typography.bodySmall, color = colors.error)
                            }
                            if (biometricEnabled) {
                                OutlinedButton(
                                    onClick = {
                                        if (viewModel.lockSession()) {
                                            navController.navigate(NavRoutes.Login.route) { popUpTo(0) { inclusive = true } }
                                        }
                                    },
                                    enabled = !signingOut,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bloquear y usar huella")
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmLogout = true },
                    enabled = !signingOut,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (signingOut) "Cerrando sesión…" else "Cerrar sesión")
                }
                TextButton(onClick = { navController.navigate("profile/delete") }, enabled = !signingOut, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.error)) { Text("Eliminar mi cuenta") }
            }
        }
    }
}

@Composable
private fun AccountLink(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(profile: UserProfile, photo: ByteArray? = null) {
    val colors = MaterialTheme.colorScheme
    val username = profile.username.orEmpty()
    val name = listOf(profile.first_name.orEmpty(), profile.last_name.orEmpty())
        .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
    val title = name.ifBlank { username.ifBlank { "Perfil" } }
    Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                com.example.sp500auth.ui.ops.AvatarCircle(photo, 56.dp, "Foto de perfil")
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    if (name.isNotBlank() && username.isNotBlank() && name != username) {
                        Text("@$username", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Email, contentDescription = null, tint = colors.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Correo electrónico", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(profile.email.orEmpty().ifBlank { "Sin registrar" }, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
