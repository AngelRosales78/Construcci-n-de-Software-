package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.ComparisonConclusion
import com.example.sp500auth.ui.common.SourceLink

@Composable fun ComparisonConclusionCard(data: ComparisonConclusion) {
    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Conclusión comparativa", style = MaterialTheme.typography.titleLarge)
            Text("${data.a} frente a ${data.b} · FY${data.fiscal_year}", fontWeight = FontWeight.Bold)
            Text(if (data.generation_mode == "ai") "Generado por IA" else "Informe elaborado por reglas")
            Text("Generado: ${data.generated_at} · Datos: ${data.data_version}", style = MaterialTheme.typography.labelSmall)
            data.paragraphs.orEmpty().filterNotNull().forEach { Text(it) }
            Text("Fortalezas relativas de ${data.a}", fontWeight = FontWeight.Bold)
            data.strengths_a.orEmpty().filterNotNull().forEach { Text("• $it") }
            Text("Fortalezas relativas de ${data.b}", fontWeight = FontWeight.Bold)
            data.strengths_b.orEmpty().filterNotNull().forEach { Text("• $it") }
            data.sources.orEmpty().filterNotNull().forEach { SourceLink(it.url, it.label) }
            Text("La comparación no garantiza resultados ni constituye una recomendación de inversión.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
