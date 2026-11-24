package com.example.sp500auth.ui.ops

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.ops.*
import com.example.sp500auth.ui.common.*
import com.example.sp500auth.ui.common.SourceLink

private val stateOptions = listOf("all" to "Todas", "active" to "Activas", "inactive" to "Inactivas")
private val scopeOptions = listOf("all" to "Catálogo general", "pilot" to "Piloto")
private val coverageOptions = listOf("all" to "Toda cobertura", "loaded" to "Con datos", "missing" to "Sin datos")
private fun levelColor(level: CoverageLevel) = when (level) { CoverageLevel.COMPLETE -> AppColors.Green; CoverageLevel.PARTIAL -> AppColors.Amber; CoverageLevel.MISSING -> AppColors.Red }

/** HU14_01 to HU14_04. */
@Composable
fun CatalogScreen(nav: NavController, vm: CatalogViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val pilot = vm.scope == "pilot"
    OpsScaffold(if (pilot) "Empresas piloto" else "Catálogo de empresas", vm.catalog.value?.count?.let { if (it == 1) "1 empresa" else "$it empresas" }, { nav.popBackStack() }, actions = {
        IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar lista") }
        IconButton(onClick = { nav.navigate("admin/catalog/add") }) { Icon(Icons.Default.Add, "Añadir empresa") }
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ChoiceChips(scopeOptions, vm.scope, onSelect = vm::filterScope) }
            item {
                OutlinedTextField(vm.query, vm::search, label = { Text("Buscar ticker o nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (vm.query.isNotEmpty()) IconButton(onClick = { vm.search("") }) { Icon(Icons.Default.Close, "Borrar búsqueda") } })
            }
            item { ChoiceChips(stateOptions, vm.state, onSelect = vm::filterState) }
            item { ChoiceChips(coverageOptions, vm.coverage, onSelect = vm::filterCoverage) }
            if (pilot) vm.catalog.value?.pilot?.let { info ->
                item {
                    AppCard {
                        Text("Piloto: ${info.selected ?: 0} de ${info.target ?: 15} empresas", fontWeight = FontWeight.Bold)
                        Text((if (info.selection_status == "proposal") "La selección es una propuesta pendiente de validar. " else "") + "Estar en el piloto no significa tener los cinco pilares completos.",
                            style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                    }
                }
            }
            vm.provider?.let { usage -> item { ProviderLine(usage) } }
            item { NoticeBar(vm.notice) }
            item {
                LoadContent(vm.catalog, { vm.load() }) { page ->
                    if (page.results.isNullOrEmpty()) EmptyBox(Icons.Default.Business, "Sin empresas", "Ninguna empresa coincide con la búsqueda y los filtros.")
                }
            }
            items(vm.catalog.value?.results.orEmpty(), key = { it.id ?: 0 }) { CompanyCard(it, vm, nav) }
            vm.catalog.value?.let { page -> item { Pager(vm.page, 20, page.count, page.previous != null, page.next != null, !vm.catalog.loading) { vm.go(it) } } }
        }
    }
    vm.toggling?.let { ToggleDialog(it, vm) }
    vm.editing?.let { EditDialog(it, vm) }
}

@Composable
private fun ProviderLine(usage: ProviderUsage) {
    Text("${usage.provider ?: "Proveedor"}: ${usage.calls ?: 0} de ${usage.limit ?: 0} consultas de hoy (límite interno). Se reinicia ${displayDateTime(usage.reset_at)}.",
        style = MaterialTheme.typography.bodySmall, color = if ((usage.calls ?: 0) >= (usage.limit ?: 1)) AppColors.Red else AppColors.Muted)
}

@Composable
private fun CompanyCard(company: AdminCompany, vm: CatalogViewModel, nav: NavController) {
    val active = company.is_active == true
    val level = coverageLevel(company.coverage)
    val refresh = vm.refreshOf(company.id)
    var sources by remember(company.id) { mutableStateOf(false) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompanyLogo(company.ticker.orEmpty(), size = 44.dp)
            Column(Modifier.weight(1f)) {
                Text(company.name.orEmpty(), fontWeight = FontWeight.Bold)
                Text("${company.ticker} · ${company.sector_display.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
            }
            Switch(checked = active, onCheckedChange = { vm.proposeToggle(company) }, enabled = !vm.saving,
                modifier = Modifier.semantics { contentDescription = "${company.ticker}: ${if (active) "activa" else "inactiva"}" })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusPill(if (active) "Activa" else "Inactiva", if (active) AppColors.Green else AppColors.Red)
            StatusPill("Cobertura ${level.label.lowercase()}", levelColor(level))
            if (company.is_pilot == true) StatusPill("Piloto", AppColors.Blue)
        }
        val coverage = company.coverage
        FieldLine("Indicadores", "${coverage?.summary_fields ?: 0} de ${coverage?.summary_total ?: 5} · ${displayDay(coverage?.summary_as_of)}")
        FieldLine("Balances", coverage?.fiscal_years.orEmpty().take(5).joinToString().ifBlank { "Sin balances" })
        FieldLine("Último precio", displayDay(coverage?.price_as_of))
        coverageGaps(coverage).forEach { Text("• $it", style = MaterialTheme.typography.labelSmall, color = AppColors.Amber) }
        if (!coverage?.sources.isNullOrEmpty()) {
            TextButton(onClick = { sources = !sources }) { Text(if (sources) "Ocultar fuentes" else "Ver fuentes (${coverage!!.sources!!.size})") }
            if (sources) coverage!!.sources!!.forEach { source -> if (source.startsWith("https://")) SourceLink(source, source.removePrefix("https://").take(60)) else Text(source, style = MaterialTheme.typography.labelSmall) }
        }
        RefreshBlock(refresh, company.last_update)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { vm.refresh(company) }, enabled = refresh?.running != true, modifier = Modifier.weight(1f)) {
                if (refresh?.running == true) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Default.Sync, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp)); Text(if (refresh?.running == true) "Actualizando" else "Precios")
            }
            OutlinedButton(onClick = { vm.startEdit(company) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Editar") }
        }
        TextButton(onClick = { nav.navigate("company/${Uri.encode(company.ticker.orEmpty())}") }) { Text("Abrir ficha") }
    }
}

/** HU14_03: the previous data stays visible; only the result of the consultation changes here. */
@Composable
private fun RefreshBlock(state: RefreshState?, last: MarketOperation?) {
    val operation = state?.operation ?: last
    if (state?.running == true) { LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green); Text("Consultando al proveedor. Los datos guardados siguen disponibles.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted) }
    state?.error?.let { Text("$it Los datos anteriores se conservan.", color = AppColors.Red, style = MaterialTheme.typography.bodySmall) }
    if (operation != null && state?.running != true && operation.status == "succeeded") {
        Text("${operation.message.orEmpty()} ${displayDateTime(operation.finished_at)}", color = AppColors.Green, style = MaterialTheme.typography.bodySmall)
        operation.text("price_as_of")?.let { FieldLine("Cierre más reciente", "${displayDay(it)} · ${operation.text("adjusted_close") ?: "—"} ${operation.text("currency").orEmpty()}") }
        operation.text("new_dates")?.let { FieldLine("Fechas nuevas", it) }
    } else if (operation != null && state == null && operation.status == "failed")
        Text("Último intento (${displayDateTime(operation.finished_at)}): ${operation.message.orEmpty()}", color = AppColors.Amber, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ToggleDialog(company: AdminCompany, vm: CatalogViewModel) {
    val active = company.is_active == true
    AlertDialog(onDismissRequest = vm::dismissToggle, title = { Text(if (active) "¿Desactivar ${company.ticker}?" else "¿Activar ${company.ticker}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (active) "Dejará de aparecer en las búsquedas de la web y del teléfono. Su ficha, precios y balances se conservan." else "Volverá a aparecer en las búsquedas de la web y del teléfono.")
                Text("El estado no indica si la empresa tiene indicadores disponibles.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                vm.errors["form"]?.let { Text(it, color = AppColors.Red) }
            }
        },
        confirmButton = { TextButton(onClick = vm::confirmToggle, enabled = !vm.saving) { Text(if (vm.saving) "Guardando…" else if (active) "Desactivar" else "Activar") } },
        dismissButton = { TextButton(onClick = vm::dismissToggle, enabled = !vm.saving) { Text("Cancelar") } })
}

@Composable
private fun EditDialog(company: AdminCompany, vm: CatalogViewModel) {
    AlertDialog(onDismissRequest = vm::cancelEdit, title = { Text("Editar ${company.ticker}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(vm.description, vm::writeDescription, label = { Text("Descripción") }, minLines = 3, maxLines = 6, enabled = !vm.saving, isError = "description" in vm.errors,
                    supportingText = { Text(vm.errors["description"] ?: "${vm.description.length}/3000") }, modifier = Modifier.fillMaxWidth())
                SectorPicker(vm.sector, !vm.saving, vm.errors["sector"], vm::writeSector)
                OutlinedTextField(vm.website, vm::writeWebsite, label = { Text("Sitio web (https)") }, singleLine = true, enabled = !vm.saving, isError = "website_url" in vm.errors,
                    supportingText = { Text(vm.errors["website_url"] ?: "Opcional") }, modifier = Modifier.fillMaxWidth())
                Text("No se modifican precios ni indicadores financieros.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                vm.errors["form"]?.let { Text(it, color = AppColors.Red) }
            }
        },
        confirmButton = { TextButton(onClick = vm::saveEdit, enabled = !vm.saving) { Text(if (vm.saving) "Guardando…" else "Guardar") } },
        dismissButton = { TextButton(onClick = vm::cancelEdit, enabled = !vm.saving) { Text("Cancelar") } })
}

@Composable
private fun SectorPicker(selected: String, enabled: Boolean, error: String?, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Box {
            OutlinedButton(onClick = { open = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Sector: ${catalogSectors[selected] ?: "elegir"}", modifier = Modifier.weight(1f)); Icon(Icons.Default.ArrowDropDown, null) }
            DropdownMenu(open, { open = false }) { catalogSectors.forEach { (value, label) -> DropdownMenuItem({ Text(label) }, { onSelect(value); open = false }) } }
        }
        error?.let { Text(it, color = AppColors.Red, style = MaterialTheme.typography.bodySmall) }
    }
}

/** HU14_05. */
@Composable
fun AddCompanyScreen(nav: NavController, vm: AddCompanyViewModel = hiltViewModel()) {
    OpsScaffold("Añadir empresa", "Verificación antes del alta", { nav.popBackStack() }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AppCard {
                AppSectionTitle("1. Identificar", "Se consulta el ticker en la fuente de precios")
                OutlinedTextField(vm.ticker, vm::writeTicker, label = { Text("Ticker") }, singleLine = true, enabled = !vm.busy, isError = "ticker" in vm.errors, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = KeyboardCapitalization.Characters), supportingText = { Text(vm.errors["ticker"] ?: "Por ejemplo SHOP") })
                SectorPicker(vm.sector, !vm.busy, vm.errors["sector"], vm::writeSector)
                Button(onClick = vm::verify, enabled = !vm.busy && vm.created == null, modifier = Modifier.fillMaxWidth()) { Text(if (vm.busy && vm.verified == null) "Verificando…" else "Verificar ticker") }
                if (vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green)
                vm.errors["form"]?.let { Text(it, color = AppColors.Red) }
            }
            vm.verified?.let { operation ->
                AppCard {
                    AppSectionTitle("2. Revisar y confirmar", operation.message)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CompanyLogo(operation.ticker.orEmpty(), size = 48.dp)
                        Column { Text(operation.text("name") ?: operation.ticker.orEmpty(), fontWeight = FontWeight.Bold); Text("${operation.ticker} · ${operation.text("exchange") ?: "Bolsa no informada"}", color = AppColors.Muted) }
                    }
                    FieldLine("Precios", "${operation.text("price_count") ?: "0"} sesiones desde ${displayDay(operation.text("first_price_date"))}")
                    FieldLine("Último cierre", "${operation.text("adjusted_close") ?: "—"} ${operation.text("currency").orEmpty()} · ${displayDay(operation.text("price_as_of"))}")
                    operation.text("source_url")?.let { SourceLink(it, "Fuente consultada") }
                    Text("La verificación vence ${displayDateTime(operation.expires_at)}.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                    Button(onClick = vm::confirm, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) { Text(if (vm.busy) "Añadiendo…" else "Confirmar alta") }
                }
            }
            vm.created?.let { operation ->
                AppCard {
                    Text("${operation.ticker} está en el catálogo", color = AppColors.Green, fontWeight = FontWeight.Bold)
                    Text(operation.message.orEmpty())
                    Text("Pendiente: balances e indicadores. No se asigna ninguna nota hasta tener datos suficientes.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
                    Button(onClick = { nav.navigate("company/${Uri.encode(operation.ticker.orEmpty())}") }, modifier = Modifier.fillMaxWidth()) { Text("Abrir ficha") }
                    OutlinedButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Volver al catálogo") }
                }
            }
        }
    }
}
