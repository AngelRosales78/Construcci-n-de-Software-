package com.example.sp500auth.ui.pillars

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.*

private val FinancePanel = Color(0xFF1C2024)
private val FinanceMuted = Color(0xFF9BAAA8)

@Composable
private fun AnalysisCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinancePanel)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun PillarScreen(navController: NavController, pillarId: String, viewModel: PillarsViewModel = hiltViewModel(), initialTicker: String = "MSFT") {
    var year by rememberSaveable(initialTicker) { mutableStateOf("2024") }
    var selected by rememberSaveable(initialTicker, pillarId) { mutableStateOf(pillarId) }
    LaunchedEffect(initialTicker) { viewModel.load(initialTicker, year) }
    Scaffold(containerColor = Color(0xFF101416)) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a la ficha") }
                Column { Text("ANÁLISIS FINANCIERO", color = FinanceMuted, style = MaterialTheme.typography.labelSmall); Text(initialTicker, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Ejercicio fiscal") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                Button(onClick = { viewModel.load(initialTicker, year) }, shape = RoundedCornerShape(12.dp)) { Text("Consultar") }
            }
            when (val state = viewModel.state) {
                PillarsState.Loading -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Consultando indicadores…", color = FinanceMuted) }
                is PillarsState.Error -> AnalysisCard { Text(state.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.load(initialTicker, year) }) { Text("Reintentar") } }
                is PillarsState.Ready -> {
                    val data = state.data
                    data.reporting_note?.let { AnalysisCard { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) } }
                    AnalysisCard {
                        Text("${data.ticker} · FY${data.fiscal_year}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${data.period?.start ?: "Sin fecha"} — ${data.period?.end ?: "Sin fecha"} · ${data.currency ?: "Sin moneda"}", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        if (data.available_years.isNullOrEmpty()) Text("Esta empresa aún no tiene balances anuales cargados.", color = MaterialTheme.colorScheme.error)
                        else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            data.available_years.orEmpty().forEach { available ->
                                FilterChip(selected = data.fiscal_year == available, onClick = { year = available.toString(); viewModel.load(initialTicker, year) }, label = { Text("FY$available") })
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf("all" to "Resumen") + pillarLabels.toList()).forEach { (id, label) -> FilterChip(selected = selected == id, onClick = { selected = id }, label = { Text(label) }) }
                    }
                    if (selected == "all") {
                        AnalysisCard {
                            Text("PUNTAJE GENERAL", color = FinanceMuted, style = MaterialTheme.typography.labelMedium)
                            Text(overallDisplay(data), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            val overall = scoreNumber(data.overall_score)
                            if (overall != null) LinearProgressIndicator(progress = { overall / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                            Text("Cinco pilares · Ponderación informada por el servidor", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                            data.overall_score?.contributions.orEmpty().forEach { part ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(part.label ?: part.id.orEmpty(), modifier = Modifier.weight(1f))
                                    Text("${part.display_value ?: "—"} · ${part.weight_display ?: "—"}", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (!data.overall_score?.missing_pillars.isNullOrEmpty()) Text("Sin nota completa: ${data.overall_score?.missing_pillars?.map { pillarLabels[it] ?: it }?.joinToString()}", color = MaterialTheme.colorScheme.error)
                            Text(data.scoring_policy?.explanation ?: "Consulta la metodología del proyecto.", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        AnalysisCard {
                            Text("Equilibrio financiero", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Selecciona un pilar para consultar sus indicadores.", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                            PillarRadar(data.pillars.orEmpty()) { selected = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(onClick = { navController.navigate("scenarios/${Uri.encode(data.ticker)}") }, modifier = Modifier.weight(1f)) { Text("Escenarios") }
                            FilledTonalButton(onClick = { navController.navigate("analysis/${Uri.encode(data.ticker)}/${data.fiscal_year}") }, modifier = Modifier.weight(1f)) { Text("Informe") }
                        }
                    } else {
                        val pillar = data.pillars?.firstOrNull { it.id == selected }
                        AnalysisCard {
                            Text(pillarLabels[selected] ?: selected, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(if (scoreNumber(pillar?.score) != null) "${pillar?.score?.display_value} / 100" else "Nota no disponible", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineMedium)
                            var showScoring by rememberSaveable(selected) { mutableStateOf(false) }
                            TextButton(onClick = { showScoring = !showScoring }) { Text(if (showScoring) "Ocultar cálculo de la nota" else "Ver cálculo de la nota") }
                            if (showScoring) pillar?.score?.parts.orEmpty().forEach { part ->
                                Text("${part.label ?: part.id}: ${part.display_value} · Origen ${part.input_value}")
                                Text("Anclajes: ${part.anchors.orEmpty().joinToString { "${it.input} → ${it.score}" }}", color = FinanceMuted)
                            }
                        }
                        if (pillar?.metrics.isNullOrEmpty()) Text("No hay indicadores disponibles para este pilar.")
                        pillar?.metrics.orEmpty().forEach { MetricDetails(it.label ?: it.id ?: "Indicador", it) }
                        if (selected == "profitability") AnalysisCard { SectorDetails(pillar?.sector_reference) }
                        if (selected == "solvency") Text("Estos indicadores son distintos del Altman Z de la ficha.", color = FinanceMuted)
                        if (selected == "efficiency") AnalysisCard {
                            Text("Evolución anual", style = MaterialTheme.typography.titleLarge)
                            val history = pillar?.history.orEmpty().filter { (it.fiscal_year ?: Int.MAX_VALUE) <= (data.fiscal_year ?: 0) }.sortedByDescending { it.fiscal_year }.take(3)
                            if (history.isEmpty()) Text("No hay ejercicios comparables.")
                            history.forEach { row ->
                                Text("FY${row.fiscal_year} · ${row.period?.start} — ${row.period?.end}", color = MaterialTheme.colorScheme.primary)
                                row.metrics.orEmpty().forEach { Text("${it.label ?: it.id}: ${metricValue(it)}") }
                            }
                        }
                    }
                    AnalysisCard {
                        Text("Metodología y procedencia", fontWeight = FontWeight.Bold)
                        Text("${data.methodology_version ?: "Sin versión"} · ${data.overall_score?.version ?: "Sin versión de notas"}", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        Text(if (data.methodology_status == "implementation_pending_team_review") "Metodología implementada, pendiente de revisión del equipo." else data.methodology_status.orEmpty(), color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                        Text("Las fuentes y componentes se muestran dentro de cada indicador.", color = FinanceMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun SectorDetails(reference: SectorReference?) {
    Text("Referencia sectorial",style=MaterialTheme.typography.titleMedium)
    if(hasComparableSector(reference)) {
        Text("${reference?.display_value} % · Muestra ${reference?.sample_size} · ${reference?.universe}")
        Text("${reference?.as_of} · ${reference?.period?.start} — ${reference?.period?.end}")
        Text(reference?.source?.url.orEmpty())
    } else Text(reference?.explanation ?: reasonLabel(reference?.reason))
}

@Composable
fun MetricDetails(label: String, metric: PillarMetric?, showSector: Boolean = false) {
    var expanded by rememberSaveable(metric?.id){mutableStateOf(false)}
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinancePanel)) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(label,style=MaterialTheme.typography.titleMedium)
            Text(metricValue(metric),style=MaterialTheme.typography.headlineSmall)
            if(metric==null) Text("No hay datos para este indicador.")
            else {
                if(metric.status!="available") Text(metric.explanation ?: reasonLabel(metric.reason))
                if(!metric.missing_fields.isNullOrEmpty()) Text("Faltan: ${metric.missing_fields.joinToString()}")
                Text("Fórmula: ${metric.formula_display ?: metric.formula ?: "No informada"}")
                Text("Periodo: ${metric.period?.start ?: "—"} — ${metric.period?.end ?: "—"}")
                metric.thresholds?.let {
                    Text("Tramo: ${it.band ?: "No disponible"} · Límites ${it.lower ?: "—"} / ${it.upper ?: "—"} · ${it.version.orEmpty()}")
                    Text(it.explanation.orEmpty())
                }
                TextButton(onClick={expanded=!expanded}){Text(if(expanded) "Ocultar componentes y fuentes" else "Ver componentes y fuentes")}
                if(expanded) {
                    if(metric.components.isNullOrEmpty()) Text("Sin componentes informados.")
                    metric.components.orEmpty().forEach { c ->
                        Text("${c.label ?: c.id}: ${c.value ?: "No disponible"} ${c.unit ?: c.source?.currency.orEmpty()}")
                        Text("${c.period?.start ?: "—"} — ${c.period?.end ?: "—"}")
                        val src=c.source
                        com.example.sp500auth.ui.common.SourceLink(src?.url)
                        Text("Etiqueta: ${src?.tag ?: "—"} · Informe ${src?.accession ?: src?.filing ?: "—"}")
                        Text("Publicación: ${src?.filed ?: src?.published_at ?: "—"}")
                        src?.snapshot_sha256?.let{Text("Snapshot SHA-256: $it")}
                        if(src?.is_demo==true) Text("Dato de demostración")
                    }
                }
            }
            if(showSector) SectorDetails(metric?.sector_reference)
        }
    }
}
