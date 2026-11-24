package com.example.sp500auth.ui.ops

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.Channels
import com.example.sp500auth.data.ops.Notice
import com.example.sp500auth.data.push.PushStatus
import com.example.sp500auth.data.push.PushViewModel
import com.example.sp500auth.ui.common.*

private fun categoryColor(category: String?) = when (category) { "strong" -> AppColors.Green; "moderate" -> AppColors.Amber; "weak" -> AppColors.Red; else -> AppColors.Muted }

/** Bell with the unread count confirmed by the server. */
@Composable
fun NotificationsBell(vm: NotificationsViewModel, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics {
        contentDescription = if (vm.unread == 0) "Avisos" else "Avisos, ${vm.unread} sin leer"
    }) {
        BadgedBox(badge = { if (vm.unread > 0) Badge { Text(if (vm.unread > 99) "99+" else "${vm.unread}") } }) { Icon(Icons.Default.Notifications, null, tint = AppColors.Green) }
    }
}

/** HU12_01, HU12_03 and HU12_05. */
@Composable
fun NotificationsScreen(nav: NavController, vm: NotificationsViewModel) {
    val list = rememberLazyListState()
    // Fresh every time it is opened; a rotation or coming back from a company keeps the loaded pages.
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!opened || !vm.loaded) { opened = true; vm.refresh() } }
    // A new notice enters above the saved position: show it instead of leaving it hidden.
    val newest = vm.items.firstOrNull()?.id
    var shown by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(newest) { if (newest != null && newest != shown) { if (shown != null) list.scrollToItem(0); shown = newest } }
    OpsScaffold("Avisos", if (vm.unread == 0) "Sin avisos pendientes" else "${vm.unread} sin leer", { nav.popBackStack() }, actions = {
        IconButton(onClick = vm::refresh, enabled = !vm.loading) { Icon(Icons.Default.Refresh, "Actualizar avisos") }
        IconButton(onClick = { nav.navigate("notifications/preferences") }) { Icon(Icons.Default.Settings, "Preferencias de avisos") }
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), state = list, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (vm.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green) }
            vm.error?.let { item { ErrorBox(it) { if (vm.items.isEmpty()) vm.refresh() else if (vm.hasMore) vm.loadMore() else vm.refresh() } } }
            vm.message?.let { item { Text(it, color = AppColors.Red) } }
            if (vm.unread > 0) item {
                OutlinedButton(onClick = vm::markAll, enabled = !vm.markingAll, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DoneAll, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(if (vm.markingAll) "Marcando…" else "Marcar todas como leídas")
                }
            }
            if (vm.loaded && vm.items.isEmpty() && vm.error == null) item {
                EmptyBox(Icons.Default.NotificationsNone, "No tienes avisos", "Cuando cambie el perfil financiero de una empresa favorita, aparecerá aquí.")
            }
            items(vm.items, key = { it.id ?: 0 }) { notice ->
                NoticeCard(notice, notice.id in vm.marking, { notice.id?.let(vm::markRead) }) {
                    notice.id?.let(vm::markRead); nav.navigate("company/${android.net.Uri.encode(notice.ticker.orEmpty())}")
                }
            }
            if (vm.hasMore) item {
                OutlinedButton(onClick = vm::loadMore, enabled = !vm.loadingMore, modifier = Modifier.fillMaxWidth()) { Text(if (vm.loadingMore) "Cargando…" else "Cargar más avisos") }
            }
        }
    }
}

@Composable
private fun NoticeCard(notice: Notice, marking: Boolean, onRead: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (notice.unread) AppColors.Inner else AppColors.Panel),
        border = if (notice.unread) BorderStroke(1.dp, AppColors.Green.copy(alpha = .5f)) else null) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompanyLogo(notice.ticker.orEmpty(), size = 42.dp)
                Column(Modifier.weight(1f)) {
                    Text(notice.title ?: "Cambio de perfil", fontWeight = if (notice.unread) FontWeight.Bold else FontWeight.Normal)
                    Text(notice.company_name ?: notice.ticker.orEmpty(), style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                }
                StatusPill(if (notice.unread) "No leído" else "Leído", if (notice.unread) AppColors.Green else AppColors.Muted)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Antes: ${notice.before?.label ?: "sin dato"}", categoryColor(notice.before?.category))
                StatusPill("Ahora: ${notice.after?.label ?: "sin dato"}", categoryColor(notice.after?.category))
            }
            Text("${displayDateTime(notice.created_at)} · Balance al ${displayDay(notice.after?.as_of)}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
            if (notice.is_demo == true) Text("Aviso de ejemplo: no representa un cambio real.", color = AppColors.Amber, style = MaterialTheme.typography.labelSmall)
            Row {
                TextButton(onClick = onOpen) { Text("Abrir ${notice.ticker}") }
                if (notice.unread) TextButton(onClick = onRead, enabled = !marking) { Text(if (marking) "Marcando…" else "Marcar como leído") }
            }
        }
    }
}

/** HU12_04 and the device part of HU12_02. */
@Composable
fun PreferencesScreen(nav: NavController, vm: PreferencesViewModel = hiltViewModel(), push: PushViewModel = hiltViewModel()) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.load(); push.refresh() }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { push.enable() }
    OpsScaffold("Preferencias de avisos", "Se guardan al cambiar cada opción", { nav.popBackStack() }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (vm.save) {
                SaveState.SAVING -> Text("Guardando…", color = AppColors.Muted)
                SaveState.SAVED -> NoticeBar("Preferencias guardadas.")
                SaveState.FAILED -> AppCard {
                    Text("No se guardó: ${vm.problem.orEmpty()}", color = AppColors.Red)
                    Row { TextButton(onClick = vm::retry) { Text("Reintentar") }; TextButton(onClick = vm::load) { Text("Recargar") } }
                }
                SaveState.IDLE -> Unit
            }
            LoadContent(vm.preferences, vm::load) { data ->
                val available = data.available_channels ?: Channels()
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppCard {
                        AppSectionTitle("Qué avisos recibir")
                        Option("Cambios de calificación", "Cuando cambia el perfil financiero de una empresa favorita", vm.rating, true, vm::changeRating)
                    }
                    AppCard {
                        AppSectionTitle("Por dónde recibirlos")
                        Option("Campana y centro de avisos", "En la web y en esta aplicación", vm.channels.web, available.web) { vm.changeChannels(vm.channels.copy(web = it)) }
                        Option("Correo", if (available.email) "Al correo de tu cuenta" else data.channel_messages?.get("email") ?: "Canal sin configurar en el servidor", vm.channels.email && available.email, available.email) { vm.changeChannels(vm.channels.copy(email = it)) }
                        Option("Push en el teléfono", if (available.push) "Notificación en tus teléfonos vinculados" else data.channel_messages?.get("push") ?: "Canal sin configurar en el servidor",
                            vm.channels.push && available.push, available.push) { enabled ->
                            vm.changeChannels(vm.channels.copy(push = enabled))
                            if (enabled) if (Build.VERSION.SDK_INT >= 33 && !push.permissionGranted()) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else push.enable()
                        }
                    }
                    AppCard {
                        AppSectionTitle("Este teléfono")
                        Text(push.status.text, color = if (push.status == PushStatus.READY) AppColors.Green else AppColors.Muted, style = MaterialTheme.typography.bodySmall)
                        push.problem?.let { Text(it, color = AppColors.Red, style = MaterialTheme.typography.bodySmall) }
                        if (push.status == PushStatus.DENIED) OutlinedButton(onClick = {
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                        }) { Text("Abrir ajustes de notificaciones") }
                        if (push.status == PushStatus.UNLINKED) OutlinedButton(onClick = {
                            if (Build.VERSION.SDK_INT >= 33 && !push.permissionGranted()) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else push.enable()
                        }, enabled = !push.working) { Text(if (push.working) "Vinculando…" else "Vincular este teléfono") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Option(title: String, text: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodySmall, color = if (enabled) AppColors.Muted else AppColors.Amber)
        }
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = title })
    }
}
