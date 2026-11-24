package com.example.sp500auth.ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.AdminUser
import com.example.sp500auth.ui.common.*

private val plans = listOf("all" to "Todos los planes", "free" to "Free", "pro" to "Pro")
private val states = listOf("all" to "Todos los estados", "active" to "Activas", "blocked" to "Bloqueadas")

/** HU13_01, HU13_02 and HU13_03. */
@Composable
fun UsersScreen(nav: NavController, vm: UsersViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    OpsScaffold("Usuarios", vm.users.value?.count?.let { if (it == 1) "1 cuenta" else "$it cuentas" }, { nav.popBackStack() }, actions = {
        IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar usuarios") }
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OutlinedTextField(vm.query, vm::search, label = { Text("Buscar por nombre o correo") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(14.dp),
                    trailingIcon = { if (vm.query.isNotEmpty()) IconButton(onClick = { vm.search("") }) { Icon(Icons.Default.Close, "Borrar búsqueda") } })
            }
            item { ChoiceChips(plans, vm.plan, onSelect = vm::filterPlan) }
            item { ChoiceChips(states, vm.state, onSelect = vm::filterState) }
            item { NoticeBar(vm.notice) }
            item {
                LoadContent(vm.users, { vm.load() }) { page ->
                    if (page.results.isNullOrEmpty()) EmptyBox(Icons.Default.PersonSearch, "Sin cuentas", "Ninguna cuenta coincide con la búsqueda y los filtros.")
                }
            }
            items(vm.users.value?.results.orEmpty(), key = { it.id ?: 0 }) { UserCard(it, vm) }
            vm.users.value?.let { page ->
                item { Pager(vm.page, 20, page.count, page.previous != null, page.next != null, !vm.users.loading) { vm.go(it) } }
            }
        }
    }
    vm.draft?.let { DraftDialog(it, vm) }
}

@Composable
private fun UserCard(user: AdminUser, vm: UsersViewModel) {
    val active = user.is_active == true
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text(user.fullName.ifBlank { user.username.orEmpty() }, fontWeight = FontWeight.Bold)
                Text("@${user.username} · ID ${user.id}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
            }
            StatusPill(if (active) "Activa" else "Bloqueada", if (active) AppColors.Green else AppColors.Red)
        }
        FieldLine("Correo", user.email?.ifBlank { null } ?: "Sin correo")
        FieldLine("Rol", if (user.role == "admin") "Administrador" else "Usuario")
        FieldLine("Plan", if (user.plan == "pro") "Pro" else "Free")
        if (!active && !user.blocked_reason.isNullOrBlank()) FieldLine("Motivo del bloqueo", user.blocked_reason, AppColors.Red)
        HorizontalDivider(color = AppColors.Line)
        Text("Rol", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        ChoiceChips(listOf("user" to "Usuario", "admin" to "Administrador"), user.role.orEmpty(), !vm.saving) { vm.propose(UserDraft.Role(user, it)) }
        Text("Plan", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        ChoiceChips(listOf("free" to "Free", "pro" to "Pro"), user.plan.orEmpty(), !vm.saving) { vm.propose(UserDraft.Plan(user, it)) }
        OutlinedButton(onClick = { vm.propose(if (active) UserDraft.Block(user) else UserDraft.Unblock(user)) }, enabled = !vm.saving,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (active) AppColors.Red else AppColors.Green)) {
            Icon(if (active) Icons.Default.Lock else Icons.Default.LockOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
            Text(if (active) "Bloquear cuenta" else "Desbloquear cuenta")
        }
    }
}

@Composable
private fun DraftDialog(draft: UserDraft, vm: UsersViewModel) {
    val user = draft.user
    val role = { value: String? -> if (value == "admin") "Administrador" else "Usuario" }
    val plan = { value: String? -> if (value == "pro") "Pro" else "Free" }
    val (title, text, action) = when (draft) {
        is UserDraft.Role -> Triple("¿Cambiar el rol?", "${role(user.role)} → ${role(draft.role)}. El plan ${plan(user.plan)} no cambia. La cuenta deberá iniciar sesión otra vez.", "Cambiar rol")
        is UserDraft.Plan -> Triple("¿Cambiar el plan?", "${plan(user.plan)} → ${plan(draft.plan)}. El rol ${role(user.role)} no cambia. Es una asignación administrativa, no una compra.", "Cambiar plan")
        is UserDraft.Block -> Triple("¿Bloquear la cuenta?", "No podrá entrar y sus sesiones abiertas dejarán de ser válidas.", "Bloquear")
        is UserDraft.Unblock -> Triple("¿Desbloquear la cuenta?", "Podrá entrar de nuevo con su contraseña. Las sesiones anteriores no se recuperan.", "Desbloquear")
    }
    AlertDialog(onDismissRequest = vm::dismiss, title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${user.fullName.ifBlank { user.username.orEmpty() }} · ${user.email.orEmpty()}", fontWeight = FontWeight.SemiBold)
                Text(text)
                if (draft is UserDraft.Block) OutlinedTextField(vm.reason, vm::writeReason, label = { Text("Motivo (obligatorio)") }, enabled = !vm.saving,
                    supportingText = { Text("${vm.reason.length}/240") }, modifier = Modifier.fillMaxWidth())
                vm.draftError?.let { Text(it, color = AppColors.Red) }
            }
        },
        confirmButton = { TextButton(onClick = vm::confirm, enabled = !vm.saving) { Text(if (vm.saving) "Guardando…" else action, color = if (draft is UserDraft.Block) AppColors.Red else AppColors.Green) } },
        dismissButton = { TextButton(onClick = vm::dismiss, enabled = !vm.saving) { Text("Cancelar") } })
}
