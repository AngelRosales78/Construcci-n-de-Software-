package com.example.sp500auth.ui.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.util.Resource
import java.time.LocalDate

private val CompareBackground = Color(0xFF101416)
internal val ComparePanel = Color(0xFF1C2024)
internal val CompareGreen = Color(0xFF36F5A0)
internal val CompareBlue = Color(0xFF60CDF9)
internal val CompareMuted = Color(0xFF9BAAA8)
internal fun slotColor(slot: CompanySlot) = if (slot == CompanySlot.A) CompareGreen else CompareBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(nav: NavController, initialTicker: String = "", vm: ComparisonViewModel = hiltViewModel()) {
    LaunchedEffect(initialTicker) { vm.initialize(initialTicker) }
    val selection = vm.selection
    // The search box serves one slot at a time; it moves to B once A is chosen.
    var active by rememberSaveable { mutableStateOf(if (selection.a.isNotBlank() && selection.b.isBlank()) CompanySlot.B else CompanySlot.A) }
    Scaffold(containerColor = CompareBackground, topBar = {
        TopAppBar(title = {
            Column {
                Text("Comparar empresas", fontWeight = FontWeight.Bold)
                Text("Dos empresas, cinco pilares", style = MaterialTheme.typography.labelMedium, color = CompareMuted)
            }
        }, navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        }, colors = TopAppBarDefaults.topAppBarColors(containerColor = CompareBackground))
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SelectionHeader(selection, active, onActivate = { active = it }, onRemove = { slot -> vm.remove(slot); active = slot },
                    onSwap = vm::swap)
            }
            item { YearChips(vm) }
            item {
                val query = if (active == CompanySlot.A) vm.queryA else vm.queryB
                val suggestions = if (active == CompanySlot.A) vm.suggestionsA else vm.suggestionsB
                val anchorSlot = if (vm.anchor.isNotBlank() && vm.anchor == selection.a) CompanySlot.A else if (vm.anchor.isNotBlank()) CompanySlot.B else null
                val compat: (String) -> Int? = { ticker ->
                    (vm.candidates as? Resource.Success)?.successData?.takeIf { anchorSlot != null && anchorSlot != active }
                        ?.let { data -> data.results.orEmpty().firstOrNull { it.ticker == ticker }?.comparable_pillars ?: 0 }
                }
                CompanySearch(active, query, suggestions, otherTicker = if (active == CompanySlot.A) selection.b else selection.a, compat = compat,
                    onQuery = { vm.query(active, it) },
                    onSelect = { company ->
                        vm.select(active, company)
                        val other = if (active == CompanySlot.A) CompanySlot.B else CompanySlot.A
                        if ((if (other == CompanySlot.A) vm.selection.a else vm.selection.b).isBlank()) active = other
                    })
                vm.selectionError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 6.dp)) }
            }
            item {
                CandidatesPanel(vm, onPick = { candidate ->
                    vm.selectCandidate(candidate)
                    active = if (vm.anchor == vm.selection.a) CompanySlot.B else CompanySlot.A
                })
            }
            item { CompareButton(vm) }
            when (val result = vm.result) {
                is Resource.Loading -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = CompareGreen)
                        Text("Consultando ambas empresas…", color = CompareMuted)
                    }
                }
                is Resource.Error -> item {
                    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(result.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = vm::compare) { Text("Reintentar comparación") }
                        }
                    }
                }
                is Resource.Success -> item { ComparisonTable(result.successData) }
                else -> Unit
            }
            if (selection.canCompare) {
                item { HistoryComparisonSection(selection.a, selection.b) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("Conclusión y reporte")
                        ComparisonConclusionSection(selection.a, selection.b, vm.year,
                            comparison = (vm.result as? Resource.Success)?.successData,
                            onLoadComparison = { if (vm.result !is Resource.Success && vm.result !is Resource.Loading) vm.compare() })
                        OutlinedButton(onClick = { nav.navigate("report?a=${selection.a}&b=${selection.b}&year=${vm.year}") },
                            enabled = vm.year.toIntOrNull() in 2000..2100, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Exportar comparación en PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CompareMuted) }
    }
}

@Composable
private fun SelectionHeader(selection: CompanySelection, active: CompanySlot, onActivate: (CompanySlot) -> Unit,
                            onRemove: (CompanySlot) -> Unit, onSwap: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            SlotCard(CompanySlot.A, selection.a, selection.aName, active == CompanySlot.A, onActivate, onRemove, Modifier.weight(1f).fillMaxHeight())
            SlotCard(CompanySlot.B, selection.b, selection.bName, active == CompanySlot.B, onActivate, onRemove, Modifier.weight(1f).fillMaxHeight())
        }
        FilledIconButton(onClick = onSwap, enabled = selection.a.isNotBlank() || selection.b.isNotBlank(),
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2A3036), contentColor = Color.White),
            modifier = Modifier.size(40.dp).semantics { contentDescription = "Intercambiar A y B" }) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
        }
    }
}

@Composable
private fun SlotCard(slot: CompanySlot, ticker: String, name: String, active: Boolean, onActivate: (CompanySlot) -> Unit,
                     onRemove: (CompanySlot) -> Unit, modifier: Modifier) {
    val color = slotColor(slot)
    Card(modifier.clickable { onActivate(slot) }, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ComparePanel),
        border = BorderStroke(if (active) 2.dp else 1.dp, if (active) color else Color(0xFF2A3036))) {
        Column(Modifier.padding(14.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(color, CircleShape)); Spacer(Modifier.width(6.dp))
                Text("Empresa ${slot.name}", style = MaterialTheme.typography.labelMedium, color = CompareMuted, modifier = Modifier.weight(1f))
                if (ticker.isNotBlank()) Icon(Icons.Default.Close, contentDescription = "Quitar empresa ${slot.name}", tint = CompareMuted,
                    modifier = Modifier.size(20.dp).clickable { onRemove(slot) })
            }
            if (ticker.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.sp500auth.ui.common.CompanyLogo(ticker, size = 36.dp, accent = color)
                    Text(ticker, color = color, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Text(name.ifBlank { ticker }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            } else {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = if (active) color else CompareMuted, modifier = Modifier.size(28.dp))
                Text(if (active) "Busca abajo" else "Toca para elegir", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
            }
        }
    }
}

@Composable
private fun CompanySearch(slot: CompanySlot, query: String, suggestions: Resource<List<CompanyItem>>, otherTicker: String,
                          compat: (String) -> Int? = { null }, onQuery: (String) -> Unit, onSelect: (CompanyItem) -> Unit) {
    val color = slotColor(slot)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = query, onValueChange = onQuery, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar empresa ${slot.name}") }, placeholder = { Text("Nombre o ticker, p. ej. AAPL") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = color) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda") } },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, focusedLabelColor = color, cursorColor = color))
        when (suggestions) {
            is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = color)
            is Resource.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(suggestions.message.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = { onQuery(query) }) { Text("Reintentar") }
            }
            is Resource.Success -> Card(colors = CardDefaults.cardColors(containerColor = ComparePanel), shape = RoundedCornerShape(14.dp)) {
                if (suggestions.successData.isEmpty()) Text("No encontramos empresas con ese nombre o ticker.", color = CompareMuted, modifier = Modifier.padding(16.dp))
                suggestions.successData.forEachIndexed { index, company ->
                    if (index > 0) HorizontalDivider(color = Color(0xFF2A3036))
                    val taken = company.ticker.equals(otherTicker, ignoreCase = true)
                    Row(Modifier.fillMaxWidth().clickable(enabled = !taken) { onSelect(company) }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        com.example.sp500auth.ui.common.CompanyLogo(company.ticker.orEmpty(), size = 40.dp, accent = color)
                        Column(Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(company.ticker.orEmpty(), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(company.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(if (taken) "Ya elegida como la otra empresa" else company.sector_display ?: "", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
                        }
                        if (!taken) compat(company.ticker.orEmpty())?.let { PillarsBadge(it) }
                    }
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun YearChips(vm: ComparisonViewModel) {
    val current = LocalDate.now().year
    val years = remember(current) { (current - 1 downTo current - 5).map(Int::toString) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ejercicio fiscal", style = MaterialTheme.typography.labelLarge, color = CompareMuted)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((if (vm.year in years || vm.year.isBlank()) years else listOf(vm.year) + years)) { year ->
                FilterChip(selected = vm.year == year, onClick = { vm.changeYear(year) }, label = { Text("FY$year") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CompareGreen.copy(alpha = .18f), selectedLabelColor = CompareGreen))
            }
        }
    }
}

@Composable
private fun CompareButton(vm: ComparisonViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = vm::compare, enabled = vm.selection.canCompare && vm.result !is Resource.Loading,
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CompareGreen, contentColor = Color.Black)) {
            Icon(Icons.Default.CompareArrows, contentDescription = null); Spacer(Modifier.width(8.dp))
            Text("Comparar cinco pilares", fontWeight = FontWeight.Bold)
        }
        if (!vm.selection.canCompare) Text("Elige una empresa distinta en A y en B para comparar.", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
    }
}

/** Green for the five pillars, amber for 3-4, red for 1-2, grey when nothing can be compared. */
internal fun pillarsColor(count: Int) = when { count >= 5 -> CompareGreen; count >= 3 -> Color(0xFFE0C34D); count >= 1 -> Color(0xFFFF8F8F); else -> CompareMuted }

@Composable
private fun PillarsBadge(count: Int) {
    val color = pillarsColor(count)
    Surface(color = color.copy(alpha = .15f), shape = RoundedCornerShape(50)) {
        Text(if (count == 0) "No comparable" else "$count/5 pilares", color = color, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

/** HU08_01: suggests companies that share A's fiscal dates, so the five pillars can actually be compared. */
@Composable
private fun CandidatesPanel(vm: ComparisonViewModel, onPick: (com.example.sp500auth.data.api.models.CompareCandidate) -> Unit) {
    val anchor = vm.anchor
    if (anchor.isBlank()) return
    val other = if (anchor == vm.selection.a) vm.selection.b else vm.selection.a
    var expanded by rememberSaveable(anchor, vm.year) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFE0C34D))
                Column(Modifier.weight(1f)) {
                    Text(if (other.isBlank()) "Compatibles con $anchor" else "Compatibilidad del par", fontWeight = FontWeight.Bold)
                    Text("Mismas fechas de ejercicio FY${vm.year} y notas de pilares en ambas", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
                }
            }
            when (val state = vm.candidates) {
                is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = CompareGreen)
                is Resource.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(state.message.orEmpty(), color = CompareMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.loadCandidates(force = true) }) { Text("Reintentar") }
                }
                is Resource.Success -> {
                    val data = state.successData
                    val rows = data.results.orEmpty().filter { it.ticker != null && it.ticker != other }
                    data.period?.let { Text("Ejercicio de $anchor: ${it.start} a ${it.end} · tiene ${data.a_pillars.orEmpty().size}/5 pilares con nota",
                        style = MaterialTheme.typography.labelSmall, color = CompareMuted) }
                    if (other.isNotBlank()) {
                        val match = data.results.orEmpty().firstOrNull { it.ticker == other }
                        val count = match?.comparable_pillars ?: 0
                        Surface(color = pillarsColor(count).copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (count > 0) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null, tint = pillarsColor(count))
                                Text(if (count > 0) "$anchor y $other se pueden comparar en $count de 5 pilares."
                                    else "$anchor y $other no comparten fechas de ejercicio o pilares con nota en FY${vm.year}. Elige una de las sugeridas.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    data.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CompareMuted) }
                    val years = data.available_years.orEmpty().map(Int::toString).filter { it != vm.year }
                    if (years.isNotEmpty()) {
                        Text("Ejercicios disponibles para $anchor:", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(years) { year -> AssistChip(onClick = { vm.changeYear(year) }, label = { Text("FY$year") }) }
                        }
                    }
                    if (rows.isNotEmpty()) {
                        Text(if (other.isBlank()) "Toca una para ponerla como empresa ${if (anchor == vm.selection.a) "B" else "A"}" else "Otras opciones para $anchor",
                            style = MaterialTheme.typography.labelMedium, color = CompareMuted)
                        rows.take(if (expanded) 30 else 5).forEach { candidate ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onPick(candidate) }.padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                com.example.sp500auth.ui.common.CompanyLogo(candidate.ticker.orEmpty(), size = 38.dp)
                                Column(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(candidate.ticker.orEmpty(), fontWeight = FontWeight.Bold)
                                        Text(candidate.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(if (candidate.same_sector == true) "Mismo sector · ${candidate.sector_display.orEmpty()}" else candidate.sector_display.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall, color = if (candidate.same_sector == true) CompareBlue else CompareMuted)
                                }
                                PillarsBadge(candidate.comparable_pillars ?: 0)
                            }
                        }
                        if (rows.size > 5) TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "Ver menos" else "Ver más (${minOf(rows.size, 30)} de ${rows.size})")
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
