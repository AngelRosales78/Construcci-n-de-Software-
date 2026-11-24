package com.example.sp500auth.ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.Load
import com.example.sp500auth.data.ops.Outcome
import com.example.sp500auth.ui.common.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es")
private val dateTime = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", spanish)
private val day = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)

/** Accepts the offsets the server publishes; never invents a date. */
fun displayDateTime(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String {
    val instant = runCatching { OffsetDateTime.parse(iso).toInstant() }.recoverCatching { Instant.parse(iso) }.getOrNull() ?: return "Sin fecha"
    return dateTime.format(instant.atZone(zone))
}
fun displayDay(iso: String?): String = runCatching { day.format(LocalDate.parse(iso)) }.getOrDefault("Sin fecha")

@Composable
fun OpsScaffold(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(containerColor = AppColors.Background, topBar = { AppTopBar(title, subtitle, onBack, actions) }, content = content)
}

@Composable
fun ErrorBox(error: Outcome.Fail, onRetry: (() -> Unit)?) {
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (error.forbidden) Icons.Default.Lock else Icons.Default.CloudOff, null, tint = AppColors.Red)
            Text(error.message, color = AppColors.Red, modifier = Modifier.weight(1f))
        }
        if (onRetry != null) OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}

/** Loading, error and empty states around the last valid data. */
@Composable
fun <T> LoadContent(load: Load<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (load.loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green)
        load.error?.let { ErrorBox(it, onRetry) }
        load.value?.let { content(it) }
    }
}

@Composable
fun EmptyBox(icon: ImageVector, title: String, text: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = AppColors.Muted, modifier = Modifier.size(44.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(text, color = AppColors.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChoiceChips(options: List<Pair<String, String>>, selected: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options, key = { it.first }) { (value, label) ->
            FilterChip(selected = selected == value, onClick = { if (selected != value) onSelect(value) }, enabled = enabled, label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Green.copy(alpha = .18f), selectedLabelColor = AppColors.Green))
        }
    }
}

@Composable
fun Pager(page: Int, pageSize: Int, count: Int?, hasPrevious: Boolean, hasNext: Boolean, enabled: Boolean, onPage: (Int) -> Unit) {
    val total = count?.let { maxOf(1, (it + pageSize - 1) / pageSize) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        OutlinedButton(onClick = { onPage(page - 1) }, enabled = enabled && hasPrevious) { Text("Anterior") }
        Text("Página $page${total?.let { " de $it" }.orEmpty()}", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        OutlinedButton(onClick = { onPage(page + 1) }, enabled = enabled && hasNext) { Text("Siguiente") }
    }
}

@Composable
fun NoticeBar(text: String?) {
    if (text == null) return
    Surface(color = AppColors.Green.copy(alpha = .12f), shape = MaterialTheme.shapes.medium) {
        Text(text, color = AppColors.Green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(12.dp))
    }
}

@Composable
fun FieldLine(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = AppColors.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(.42f))
        Text(value, color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(.58f))
    }
}

/** Administration hub. Hiding it is convenience; the server rejects anyone who is not an administrator. */
@Composable
fun AdminHubScreen(nav: NavController, account: AccountViewModel) {
    LaunchedEffect(Unit) { account.load() }
    OpsScaffold("Administración", "Solo para administradores", { nav.popBackStack() }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadContent(account.profile, account::load) {
                if (!account.isAdmin) ErrorBox(Outcome.Fail("Tu cuenta no tiene permisos de administración.", 403), null)
                else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HubCard(Icons.Default.Group, "Usuarios", "Buscar cuentas, cambiar rol o plan y bloquear") { nav.navigate("admin/users") }
                    HubCard(Icons.Default.Business, "Empresas", "Catálogo, piloto, precios, datos y altas") { nav.navigate("admin/catalog") }
                    HubCard(Icons.Default.Insights, "Monitoreo", "Consumo de IA, planes, cuotas, estimación y errores") { nav.navigate("admin/monitoring") }
                }
            }
        }
    }
}

@Composable
private fun HubCard(icon: ImageVector, title: String, text: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = AppColors.Panel), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = AppColors.Green.copy(alpha = .14f), shape = MaterialTheme.shapes.medium) {
                Icon(icon, null, tint = AppColors.Green, modifier = Modifier.padding(10.dp).size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AppColors.Muted)
        }
    }
}
