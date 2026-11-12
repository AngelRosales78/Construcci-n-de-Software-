package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.ComparisonConclusion
import com.example.sp500auth.ui.common.SourceLink

@Composable
fun ComparisonConclusionCard(data: ComparisonConclusion, rows: List<ComparisonRow> = emptyList()) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Conclusión comparativa", style = MaterialTheme.typography.titleMedium)
            Text("${data.a} y ${data.b} · FY${data.fiscal_year}", style = MaterialTheme.typography.bodyMedium)
            if (rows.isNotEmpty()) {
                val ties = rows.count { it.result == "= Empate" }
                val unavailable = rows.count { it.higher == null && it.result != "= Empate" }
                if (unavailable == rows.size) {
                    Text("No hay pilares comparables", style = MaterialTheme.typography.titleSmall)
                } else {
                    Text("Pilares con mayor nota", style = MaterialTheme.typography.labelMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompanyHighlight(data.a.orEmpty(), rows.filter { it.higher == "a" }, Modifier.weight(1f))
                        CompanyHighlight(data.b.orEmpty(), rows.filter { it.higher == "b" }, Modifier.weight(1f))
                    }
                    val remaining = listOfNotNull(
                        if (ties == 1) "1 empate" else if (ties > 1) "$ties empates" else null,
                        if (unavailable > 0) "$unavailable sin comparar" else null
                    )
                    if (remaining.isNotEmpty()) {
                        Text(remaining.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Text(if (data.generation_mode == "ai") "Generado por IA" else "Informe elaborado por reglas",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            key(data.a, data.b, data.fiscal_year, data.generated_at) {
                ComparisonDetails("Ver informe completo", "Ocultar informe completo") {
                    data.paragraphs.orEmpty().filterNotNull().forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (!data.strengths_a.isNullOrEmpty()) {
                        Text("Fortalezas relativas de ${data.a}", fontWeight = FontWeight.SemiBold)
                        data.strengths_a.filterNotNull().forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    if (!data.strengths_b.isNullOrEmpty()) {
                        Text("Fortalezas relativas de ${data.b}", fontWeight = FontWeight.SemiBold)
                        data.strengths_b.filterNotNull().forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    data.sources.orEmpty().filterNotNull().forEach { SourceLink(it.url, it.label) }
                    Text("Generado: ${data.generated_at}", style = MaterialTheme.typography.labelSmall)
                    Text("Versión de datos: ${data.data_version}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun CompanyHighlight(ticker: String, rows: List<ComparisonRow>, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("$ticker · ${rows.size}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (rows.isNotEmpty()) Text(rows.joinToString { it.label }, style = MaterialTheme.typography.bodySmall)
        }
    }
}
