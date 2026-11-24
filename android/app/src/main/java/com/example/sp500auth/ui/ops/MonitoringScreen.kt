package com.example.sp500auth.ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.*
import com.example.sp500auth.ui.common.*
import java.text.NumberFormat
import java.util.Locale

private val tabs = listOf("IA", "Planes", "Cuotas", "Estimación", "Errores")
private val levels = listOf("all" to "Todos", "error" to "Error", "warning" to "Advertencia")
private val components = listOf("all" to "Todo", "api" to "Servidor", "local_ai" to "IA local", "yahoo_chart" to "Precios", "email" to "Correo", "push" to "Push")
private fun whole(value: Number?) = value?.let { NumberFormat.getIntegerInstance(Locale.forLanguageTag("es")).format(it) } ?: "No informado"
private fun statusColor(status: String?) = when (status) { "normal" -> AppColors.Green; "warning" -> AppColors.Amber; "exhausted" -> AppColors.Red; else -> AppColors.Muted }

/** Épica 15. */
@Composable
fun MonitoringScreen(nav: NavController, vm: MonitoringViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.reload() }
    OpsScaffold("Monitoreo", tabs[vm.tab], { nav.popBackStack() }, actions = { IconButton(onClick = { vm.reload() }) { Icon(Icons.Default.Refresh, "Actualizar") } }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            ScrollableTabRow(vm.tab, containerColor = AppColors.Background, edgePadding = 8.dp) {
                tabs.forEachIndexed { index, label -> Tab(vm.tab == index, { vm.open(index) }, text = { Text(label) }) }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (vm.tab) {
                    0 -> item { AiTab(vm) }
                    1 -> item { LoadContent(vm.plans, vm::reload) { PlansTab(it) } }
                    2 -> item { LoadContent(vm.budgets, vm::reload) { BudgetsTab(it, vm) } }
                    3 -> item { MonthlyTab(vm) }
                    else -> {
                        item { ChoiceChips(levels, vm.level, onSelect = vm::chooseLevel) }
                        item { ChoiceChips(components, vm.component, onSelect = vm::chooseComponent) }
                        item {
                            LoadContent(vm.logs, vm::reload) { page ->
                                Column {
                                    Text("${page.count ?: 0} registros · se conservan ${page.retention_days ?: 30} días · ${displayDateTime(page.generated_at)}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                                    if (page.results.isNullOrEmpty()) EmptyBox(Icons.Default.CheckCircle, "Sin registros", "No hay errores con estos filtros.")
                                }
                            }
                        }
                        items(vm.logs.value?.results.orEmpty(), key = { it.id ?: 0 }) { LogCard(it) { vm.openLog(it.id!!) } }
                        vm.logs.value?.let { page -> item { Pager(vm.page, 15, page.count, page.previous != null, page.next != null, !vm.logs.loading) { vm.goLogs(it) } } }
                    }
                }
            }
        }
    }
    vm.editing?.let { BudgetDialog(it, vm) }
    vm.detail?.let { LogDialog(it, vm) }
}

/** HU15_01. */
@Composable
private fun AiTab(vm: MonitoringViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceChips(AiPeriod.entries.map { it.name to it.label }, vm.period.name) { vm.choosePeriod(AiPeriod.valueOf(it)) }
        if (vm.models.isNotEmpty()) ChoiceChips(listOf("" to "Todos los modelos") + vm.models.map { it to it }, vm.model.orEmpty()) { vm.chooseModel(it.ifEmpty { null }) }
        LoadContent(vm.ai, vm::reload) { data ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard {
                    Text("${whole(data.calls)} llamadas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    FieldLine("Periodo", "${displayDay(data.start)} – ${displayDay(data.end)}")
                    FieldLine("Costo estimado", when {
                        !data.estimated_costs.isNullOrEmpty() -> data.estimated_costs.joinToString { "${it.amount} ${it.currency}" }
                        else -> "No calculado"
                    })
                    if ((data.unpriced_calls ?: 0) > 0) FieldLine("Sin tarifa", "${whole(data.unpriced_calls)} llamadas no calculadas", AppColors.Amber)
                    FieldLine("Actualizado", displayDateTime(data.generated_at))
                }
                if (data.breakdown.isNullOrEmpty()) EmptyBox(Icons.Default.AutoAwesome, "Sin llamadas", "No hubo llamadas a la IA en este periodo.")
                data.breakdown.orEmpty().forEach { row ->
                    AppCard {
                        Text(row.model ?: "Modelo no informado", fontWeight = FontWeight.Bold)
                        FieldLine("Proveedor", providerLabel(row.provider))
                        FieldLine("Llamadas", "${whole(row.calls)} · ${whole(row.errors)} con error")
                        FieldLine("Tokens de entrada", whole(row.input_tokens)); FieldLine("Tokens de salida", whole(row.output_tokens))
                        if ((row.unreported_tokens ?: 0) > 0) Text("${row.unreported_tokens} llamadas sin tokens informados por el motor.", color = AppColors.Amber, style = MaterialTheme.typography.labelSmall)
                    }
                }
                data.explanation?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted) }
            }
        }
    }
}

/** HU15_02: the table carries the meaning; the bar only supports it. */
@Composable
private fun PlansTab(data: PlanDistribution) {
    AppCard {
        if ((data.total ?: 0) == 0) EmptyBox(Icons.Default.Group, "Sin datos", "Todavía no hay cuentas registradas.")
        else {
            Text(if (data.total == 1) "1 cuenta" else "${whole(data.total)} cuentas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(50)).background(AppColors.Inner)) {
                data.results.orEmpty().filter { (it.count ?: 0) > 0 }.forEach { Box(Modifier.weight(it.count!!.toFloat()).fillMaxHeight().background(if (it.plan == "pro") AppColors.Green else AppColors.Blue)) }
            }
            data.results.orEmpty().forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(50)).background(if (row.plan == "pro") AppColors.Green else AppColors.Blue))
                    Text(if (row.plan == "pro") "Pro" else "Free", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Text(whole(row.count)); Text("${row.percentage?.replace('.', ',') ?: "—"} %", color = AppColors.Muted)
                }
            }
            FieldLine("Bloqueadas", whole(data.blocked))
        }
        FieldLine("Fecha del corte", displayDateTime(data.generated_at))
        data.rule?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted) }
    }
}

/** HU15_03. */
@Composable
private fun BudgetsTab(data: Budgets, vm: MonitoringViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        data.results.orEmpty().forEach { row ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(providerLabel(row.provider), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    StatusPill(budgetLabel(row.status), statusColor(row.status))
                }
                val share = (row.percentage?.toFloatOrNull() ?: 0f) / 100f
                LinearProgressIndicator(progress = { share.coerceIn(0f, 1f) }, color = statusColor(row.status), trackColor = AppColors.Inner, modifier = Modifier.fillMaxWidth().height(10.dp))
                FieldLine("Consumo", "${whole(row.used)} de ${whole(row.limit)} llamadas (${row.percentage?.replace('.', ',') ?: "—"} %)")
                FieldLine("Se reinicia", "${displayDateTime(row.resets_at)} · zona ${row.timezone ?: "no informada"}")
                row.warned_at?.let { FieldLine("Aviso del 85 %", displayDateTime(it), AppColors.Amber) }
                if (row.status == "exhausted") Text("Las llamadas nuevas están detenidas hasta el reinicio. Los datos guardados siguen disponibles.", color = AppColors.Red, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { vm.edit(row) }) { Text("Cambiar límite interno") }
            }
        }
        data.explanation?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = AppColors.Muted) }
    }
}

@Composable
private fun BudgetDialog(budget: Budget, vm: MonitoringViewModel) {
    AlertDialog(onDismissRequest = vm::cancelEdit, title = { Text("Límite diario") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(providerLabel(budget.provider), fontWeight = FontWeight.SemiBold)
            OutlinedTextField(vm.limitText, vm::writeLimit, label = { Text("Llamadas por día") }, singleLine = true, enabled = !vm.saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Text("Es un presupuesto interno, distinto de la cuota oficial del proveedor.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
            vm.budgetError?.let { Text(it, color = AppColors.Red) }
        }
    }, confirmButton = { TextButton(onClick = vm::saveLimit, enabled = !vm.saving) { Text(if (vm.saving) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(onClick = vm::cancelEdit, enabled = !vm.saving) { Text("Cancelar") } })
}

/** HU15_04. */
@Composable
private fun MonthlyTab(vm: MonitoringViewModel) {
    var detail by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { vm.moveMonth(-1) }) { Text("Mes anterior") }
            Text(vm.month, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = { vm.moveMonth(1) }, enabled = vm.canGoNext) { Text("Siguiente") }
        }
        LoadContent(vm.monthly, vm::reload) { data ->
            AppCard {
                StatusPill("Simulado · no es dinero cobrado", AppColors.Amber)
                if (data.results.isNullOrEmpty()) Text("Sin planes con importe en este mes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                data.results.orEmpty().forEach { Text("${it.amount} ${it.currency}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = AppColors.Green) }
                FieldLine("Periodo", data.month.orEmpty())
                FieldLine("Planes incluidos", whole(data.plans_included)); FieldLine("Suscripciones", whole(data.transactions))
                FieldLine("Actualizado", displayDateTime(data.generated_at))
                TextButton(onClick = { detail = !detail }) { Text(if (detail) "Ocultar cálculo" else "Ver cómo se calcula") }
                if (detail) Text(data.explanation ?: "El servidor no informó los supuestos.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
            }
        }
    }
}

/** HU15_05. */
@Composable
private fun LogCard(log: ErrorLog, onOpen: () -> Unit) {
    val error = log.level == "error"
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppColors.Panel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(providerLabel(log.component), Modifier.weight(1f), fontWeight = FontWeight.Bold)
                StatusPill(if (error) "Error" else "Advertencia", if (error) AppColors.Red else AppColors.Amber)
            }
            Text(log.summary.orEmpty(), style = MaterialTheme.typography.bodySmall)
            Text("${displayDateTime(log.created_at)} · ${log.code.orEmpty()}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
        }
    }
}

@Composable
private fun LogDialog(load: Load<ErrorLog>, vm: MonitoringViewModel) {
    AlertDialog(onDismissRequest = vm::closeLog, title = { Text("Detalle del registro") }, text = {
        LoadContent(load, vm::retryLog) { log ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldLine("Nivel", if (log.level == "error") "Error" else "Advertencia"); FieldLine("Componente", providerLabel(log.component))
                FieldLine("Fecha", displayDateTime(log.created_at)); FieldLine("Código", log.code.orEmpty()); FieldLine("Resumen", log.summary.orEmpty())
                safeContext(log).forEach { (key, value) -> FieldLine(key, value) }
                Text("El registro no guarda contraseñas, claves ni datos personales.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
            }
        }
    }, confirmButton = { TextButton(onClick = vm::closeLog) { Text("Cerrar") } })
}
