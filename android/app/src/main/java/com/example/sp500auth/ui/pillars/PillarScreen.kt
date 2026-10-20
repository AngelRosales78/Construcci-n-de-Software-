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
import com.example.sp500auth.data.api.models.PillarMetric
import java.util.Locale

@Composable
fun PillarScreen(navController: NavController, pillarId: String, viewModel: PillarsViewModel = hiltViewModel()) {
    var ticker by rememberSaveable { mutableStateOf("MSFT") }
    var year by rememberSaveable { mutableStateOf("2024") }
    LaunchedEffect(Unit) { viewModel.load(ticker, year) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { navController.popBackStack() }) { Text("Volver al detalle") }
        Text("Pilar de Liquidez", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = ticker, onValueChange = { ticker = it.trim().uppercase(Locale.ROOT) },
            label = { Text("Símbolo de la empresa") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Ejercicio fiscal") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.load(ticker, year) }) { Text("Consultar") }
        when (val state = viewModel.state) {
            PillarsState.Loading -> { CircularProgressIndicator(); Text("Cargando indicadores del servidor…") }
            is PillarsState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.load(ticker, year) }) { Text("Reintentar") }
            }
            is PillarsState.Ready -> {
                val data = state.data
                Text("${data.ticker} · Ejercicio ${data.fiscal_year}", style = MaterialTheme.typography.titleMedium)
                Text("Periodo: ${data.period?.start ?: "Sin fecha"} — ${data.period?.end ?: "Sin fecha"}")
                Text("Metodología: ${data.methodology_version ?: "No informada"}")
                val pillar = data.pillars?.firstOrNull { it.id == pillarId }
                val metrics = listOf("current_ratio" to "Liquidez corriente", "quick_ratio" to "Prueba ácida")
                metrics.forEach { (id, label) ->
                    MetricDetails(label, pillar?.metrics?.firstOrNull { it.id == id })
                }
            }
        }
    }
}

@Composable
fun MetricDetails(label: String, metric: PillarMetric?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(metricValue(metric), style = MaterialTheme.typography.headlineSmall)
            if (metric == null) Text("El servidor no incluyó este indicador.")
            else {
                if (metric.status != "available" || metricValue(metric) == "No disponible")
                    Text(reasonLabel(metric.reason))
                if (!metric.missing_fields.isNullOrEmpty()) Text("Faltan: ${metric.missing_fields.joinToString()}")
                Text("Fórmula: ${metric.formula ?: "No informada"}")
                metric.period?.let { Text("Periodo del indicador: ${it.start ?: "—"} — ${it.end ?: "—"}") }
                Text("Componentes", style = MaterialTheme.typography.titleSmall)
                if (metric.components.isNullOrEmpty()) Text("Sin componentes informados.")
                metric.components.orEmpty().forEach { component ->
                    Text("${component.label ?: component.id ?: "Componente"}: ${component.value ?: "No disponible"} ${component.unit.orEmpty()}")
                    component.period?.let { Text("Periodo: ${it.start ?: "—"} — ${it.end ?: "—"}") }
                    val source = component.source
                    Text("Fuente: ${source?.label ?: "No informada"}")
                    source?.let {
                        it.url?.let { url -> Text(url) }
                        it.tag?.let { tag -> Text("Etiqueta SEC: $tag") }
                        it.unit?.let { unit -> Text("Unidad de origen: $unit") }
                        it.filing?.let { filing -> Text("Informe: $filing") }
                        it.published_at?.let { date -> Text("Publicación: $date") }
                    }
                }
            }
        }
    }
}
