package com.example.sp500auth.ui.compare

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.common.SourceLink

@Composable
fun ComparisonTable(data: ComparisonResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cinco pilares lado a lado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Notas sobre 100 del servidor. Mayor nota no significa una recomendación de inversión.", style = MaterialTheme.typography.bodySmall)
        data.reason?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
        PeriodCard("A", data.a)
        PeriodCard("B", data.b)
        Text("Desliza la tabla horizontalmente para ver ambas empresas.", style = MaterialTheme.typography.bodySmall)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(660.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Pilar", Modifier.width(160.dp), fontWeight = FontWeight.Bold)
                        Text("A · ${data.a?.ticker}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("B · ${data.b?.ticker}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    }
                }
                comparisonRows(data).forEach { row ->
                    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(row.label, Modifier.width(160.dp), fontWeight = FontWeight.SemiBold)
                                Text(row.aValue, Modifier.weight(1f), color = if (row.higher == "a") CompareGreen else Color.Unspecified)
                                Text(row.bValue, Modifier.weight(1f), color = if (row.higher == "b") CompareGreen else Color.Unspecified)
                            }
                            Text(row.result, fontWeight = FontWeight.Bold, color = if (row.higher != null) CompareGreen else Color.Unspecified)
                            row.delta?.let { Text("Diferencia A menos B: $it puntos", style = MaterialTheme.typography.bodySmall) }
                            row.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
        }
        Text("Consulta las fuentes de cada empresa", fontWeight = FontWeight.Bold)
        CompanySources(data.a)
        CompanySources(data.b)
    }
}

@Composable
private fun PeriodCard(slot: String, data: PillarsResponse?) {
    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$slot · ${data?.ticker ?: "Empresa no informada"} · FY${data?.fiscal_year ?: "—"}", fontWeight = FontWeight.Bold)
            Text(periodText(data))
            Text("Moneda: ${data?.currency ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
            Text("Metodología: ${data?.methodology_version ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
            Text("Normalización: ${data?.scoring_policy?.version ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
            data?.methodology_status?.let { Text("Estado metodológico: $it", style = MaterialTheme.typography.bodySmall) }
            data?.scoring_policy?.explanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            data?.reporting_note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun CompanySources(data: PillarsResponse?) {
    val sources = data?.pillars.orEmpty().flatMap { it.metrics.orEmpty() }
        .flatMap { it.components.orEmpty() }.mapNotNull { it.source }
        .filter { !it.url.isNullOrBlank() }.distinctBy { it.url }
    Text(data?.ticker.orEmpty(), fontWeight = FontWeight.SemiBold)
    if (sources.isEmpty()) Text("Fuentes no informadas por el servidor.", style = MaterialTheme.typography.bodySmall)
    sources.forEach { source ->
        SourceLink(source.url, source.label ?: "Fuente ${data?.ticker}")
        if (source.is_demo == true) Text("Fuente de demostración", color = MaterialTheme.colorScheme.error)
    }
}
