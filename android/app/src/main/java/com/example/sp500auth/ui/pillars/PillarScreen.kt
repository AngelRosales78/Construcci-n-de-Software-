package com.example.sp500auth.ui.pillars

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.*
import java.util.Locale

@Composable
fun PillarScreen(navController: NavController, pillarId: String, viewModel: PillarsViewModel = hiltViewModel(), initialTicker: String = "MSFT") {
    var ticker by rememberSaveable { mutableStateOf(initialTicker) }
    var year by rememberSaveable { mutableStateOf("2024") }
    var selected by rememberSaveable { mutableStateOf(pillarId) }
    LaunchedEffect(Unit) { viewModel.load(ticker, year) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        TextButton(onClick={navController.popBackStack()}) { Text("Volver") }
        Text("Análisis financiero",style=MaterialTheme.typography.headlineSmall)
        OutlinedTextField(ticker,{ticker=it.trim().uppercase(Locale.ROOT)},label={Text("Empresa")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(year,{year=it},label={Text("Ejercicio fiscal")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Button(onClick={viewModel.load(ticker,year)}) { Text("Consultar") }
        when(val state=viewModel.state) {
            PillarsState.Loading -> { CircularProgressIndicator(); Text("Consultando el servidor…") }
            is PillarsState.Error -> { Text(state.message,color=MaterialTheme.colorScheme.error); Button(onClick={viewModel.load(ticker,year)}){Text("Reintentar")} }
            is PillarsState.Ready -> {
                val data=state.data
                Text("${data.ticker} · FY${data.fiscal_year} · ${data.currency.orEmpty()}",style=MaterialTheme.typography.titleLarge)
                Text("Periodo: ${data.period?.start ?: "Sin fecha"} — ${data.period?.end ?: "Sin fecha"}")
                Text("Ejercicios disponibles: ${data.available_years?.joinToString() ?: "No informados"}")
                Text("Metodología: ${data.methodology_version ?: "No informada"} · ${data.methodology_status.orEmpty()}")
                if(selected=="all") {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            Text("Puntaje general",style=MaterialTheme.typography.titleLarge)
                            Text(overallDisplay(data),style=MaterialTheme.typography.headlineLarge)
                            Text("Versión: ${data.overall_score?.version ?: "No informada"}")
                            Text(data.overall_score?.aggregation ?: "Se requieren cinco notas válidas del mismo periodo y versión.")
                            if(!data.overall_score?.missing_pillars.isNullOrEmpty()) Text("Faltan: ${data.overall_score?.missing_pillars?.joinToString()}")
                            data.overall_score?.contributions.orEmpty().forEach { Text("${it.label ?: it.id}: ${it.display_value ?: "No disponible"} · Peso ${it.weight_display ?: "No informado"}") }
                            Text(data.scoring_policy?.explanation ?: "Sin política informada. El puntaje es distinto de Altman Z.")
                        }
                    }
                    PillarRadar(data.pillars.orEmpty()){selected=it}
                    OutlinedButton(onClick={navController.navigate("scenarios/${data.ticker}")}){Text("Escenarios a doce meses")}
                    OutlinedButton(onClick={navController.navigate("analysis/${data.ticker}/${data.fiscal_year}")}){Text("Explicación de IA")}
                } else {
                    TextButton(onClick={selected="all"}){Text("Ver radar y puntaje")}
                    val pillar=data.pillars?.firstOrNull{it.id==selected}
                    Text(pillarLabels[selected] ?: selected,style=MaterialTheme.typography.headlineSmall)
                    Text("Nota: ${if(scoreNumber(pillar?.score)!=null) pillar?.score?.display_value + " / 100" else "No disponible"}")
                    pillar?.score?.parts.orEmpty().forEach { part ->
                        Text("${part.label ?: part.id}: ${part.display_value} · Valor de origen ${part.input_value}")
                        Text("Anclajes: ${part.anchors.orEmpty().joinToString { "${it.input} → ${it.score}" }}")
                    }
                    if(pillar?.metrics.isNullOrEmpty()) Text("El servidor no incluyó indicadores de este pilar.")
                    pillar?.metrics.orEmpty().forEach { MetricDetails(it.label ?: it.id ?: "Indicador",it) }
                    if(selected=="profitability") SectorDetails(pillar?.sector_reference)
                    if(selected=="solvency") Text("Pasivos sobre patrimonio y cobertura de intereses no equivalen al Altman Z.")
                    if(selected=="efficiency") {
                        Text("Serie anual disponible",style=MaterialTheme.typography.titleLarge)
                        val history=pillar?.history.orEmpty().filter{(it.fiscal_year ?: Int.MAX_VALUE) <= (data.fiscal_year ?: 0)}.sortedByDescending{it.fiscal_year}.take(3)
                        if(history.isEmpty()) Text("No hay ejercicios comparables disponibles.")
                        history.forEach { row ->
                            Text("FY${row.fiscal_year} · ${row.period?.start} — ${row.period?.end}")
                            row.metrics.orEmpty().forEach{ Text("${it.label ?: it.id}: ${metricValue(it)}") }
                        }
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
    Card(Modifier.fillMaxWidth()) {
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
                        Text("Fuente: ${src?.url ?: "No informada"}")
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
