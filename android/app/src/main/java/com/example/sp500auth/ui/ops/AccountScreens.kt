package com.example.sp500auth.ui.ops

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.AVATAR_LIMIT
import com.example.sp500auth.ui.common.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Decodes at a small size: the photo is only shown in circles. */
private fun decodePhoto(bytes: ByteArray): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 512) sample *= 2
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

/** Reads at most the allowed size, so a huge file is rejected without loading it. */
private suspend fun readImage(context: Context, uri: Uri): Pair<String?, ByteArray?> = withContext(Dispatchers.IO) {
    runCatching {
        val type = context.contentResolver.getType(uri)
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val buffer = ByteArray(AVATAR_LIMIT); var size = 0
            while (size < buffer.size) { val read = input.read(buffer, size, buffer.size - size); if (read < 0) break; size += read }
            buffer.copyOf(size)
        }
        type to bytes?.takeIf { it.size >= AVATAR_LIMIT || decodePhoto(it) != null }
    }.getOrDefault(null to null)
}

@Composable
fun AvatarCircle(bytes: ByteArray?, size: Dp, description: String? = null) {
    val bitmap = remember(bytes) { bytes?.let(::decodePhoto)?.asImageBitmap() }
    Surface(Modifier.size(size), shape = CircleShape, color = AppColors.Green.copy(alpha = .14f)) {
        if (bitmap != null) Image(bitmap, description, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, description, tint = AppColors.Green, modifier = Modifier.size(size * .55f)) }
    }
}

/** HU13_04: names and photo, with a circular preview before sending. */
@Composable
fun ProfileEditScreen(nav: NavController, account: AccountViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (account.profile.value == null) account.load() }
    LaunchedEffect(account.profile.value) { if (!started && account.profile.value != null) { account.startEditing(); started = true } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { readImage(context, uri).let { (type, bytes) -> account.pick(type, bytes) } }
    }
    OpsScaffold("Editar perfil", "Nombre y foto", { nav.popBackStack() }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LoadContent(account.profile, account::load) { profile ->
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppCard {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AvatarCircle(account.picked?.bytes ?: account.photo, 120.dp, if (account.picked != null) "Vista previa de la foto nueva" else "Foto de perfil")
                            Text(if (account.picked != null) "Vista previa: todavía no se guardó" else "JPEG, PNG o WebP de menos de 2 MB", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                            account.errors["avatar"]?.let { Text(it, color = AppColors.Red) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !account.saving) { Text("Elegir foto") }
                                if (account.picked != null) TextButton(onClick = account::discardPick, enabled = !account.saving) { Text("Descartar") }
                                else if (profile.avatar_version != null) TextButton(onClick = account::removePhoto, enabled = !account.saving) { Text("Quitar foto", color = AppColors.Red) }
                            }
                        }
                    }
                    AppCard {
                        OutlinedTextField(account.first, account::writeFirst, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            isError = "first_name" in account.errors, supportingText = account.errors["first_name"]?.let { { Text(it) } })
                        OutlinedTextField(account.last, account::writeLast, label = { Text("Apellido") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                            isError = "last_name" in account.errors, supportingText = account.errors["last_name"]?.let { { Text(it) } })
                        FieldLine("Usuario", profile.username); FieldLine("Correo", profile.email)
                    }
                    account.errors["form"]?.let { Text(it, color = AppColors.Red) }
                    NoticeBar(account.message)
                    Button(onClick = account::save, enabled = !account.saving, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(if (account.saving) "Guardando…" else "Guardar cambios") }
                }
            }
        }
    }
}

/** HU13_05. The local session only ends after the server confirms the deletion. */
@Composable
fun DeleteAccountScreen(nav: NavController, account: AccountViewModel) {
    // Never saved: the password only lives while this screen is open.
    var password by remember { mutableStateOf("") }
    var phrase by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { account.forgetDeleteError(); if (account.profile.value == null) account.load() }
    OpsScaffold("Eliminar cuenta", "Acción definitiva", { nav.popBackStack() }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = AppColors.Red)
                    Text("Esto no se puede deshacer", color = AppColors.Red, fontWeight = FontWeight.Bold)
                }
                account.profile.value?.let { Text("Cuenta: ${it.username} · ${it.email}") }
                Text("Se eliminarán:", fontWeight = FontWeight.SemiBold)
                listOf("Tu cuenta, tu nombre, tu correo y tu foto", "Favoritos, etiquetas, empresas recientes y búsquedas", "Avisos, preferencias y teléfonos vinculados",
                    "Reportes y análisis guardados", "Tus sesiones en todos los dispositivos").forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                Text("Los comprobantes y los registros de administración se conservan sin identificarte.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
            }
            AppCard {
                OutlinedTextField(password, { password = it; account.forgetDeleteError() }, label = { Text("Contraseña actual") }, singleLine = true, enabled = !account.deleting,
                    visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phrase, { phrase = it.take(20); account.forgetDeleteError() }, label = { Text("Escribe $DELETE_PHRASE") }, singleLine = true, enabled = !account.deleting, modifier = Modifier.fillMaxWidth())
                account.deleteError?.let { Text(it, color = AppColors.Red) }
                Button(onClick = { account.deleteAccount(password, phrase) }, enabled = !account.deleting && password.isNotEmpty() && phrase.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                    Text(if (account.deleting) "Eliminando…" else "Eliminar mi cuenta definitivamente")
                }
                OutlinedButton(onClick = { nav.popBackStack() }, enabled = !account.deleting, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            }
        }
    }
}
