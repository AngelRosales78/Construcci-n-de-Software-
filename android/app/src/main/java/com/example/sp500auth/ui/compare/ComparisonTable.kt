package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.common.SourceLink
import java.math.RoundingMode

@Composable
fun ComparisonTable(data: ComparisonResponse) {
    val rows = remember(data) { comparisonRows(data) }
    val left = data.a
    val right = data.b
    val a = left?.ticker ?: "Empresa A"
    val b = right?.ticker ?: "Empresa B"
    val leftPeriod = periodText(left)
    val rightPeriod = periodText(right)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Cinco pilares lado a lado", style = MaterialTheme.typography.titleLarge)
        Text("FY${data.fiscal_year ?: "—"} · Notas sobre 100", style = MaterialTheme.typography.labelMedium)
        if (leftPeriod == rightPeriod) {
            Text(leftPeriod, style = MaterialTheme.typography.bodySmall)
        } else {
            Text("$a · $leftPeriod", style = MaterialTheme.typography.bodySmall)
            Text("$b · $rightPeriod", style = MaterialTheme.typography.bodySmall)
        }
        data.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
            Column {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pilar", Modifier.weight(1.5f), style = MaterialTheme.typography.labelLarge)
                        Text("A · $a", Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge)
                        Text("B · $b", Modifier.weight(1f), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                rows.forEach { row ->
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1.5f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(row.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(pillarOutcome(row, a, b), style = MaterialTheme.typography.bodySmall,
                                color = if (row.higher != null) CompareGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        PillarCell(row.aValue, row.higher == "a", Modifier.weight(1f))
                        PillarCell(row.bValue, row.higher == "b", Modifier.weight(1f))
                    }
                }
            }
        }

        key(a, b, data.fiscal_year) {
            ComparisonDetails("Ver metodología y fuentes", "Ocultar metodología y fuentes") {
                rows.filter { it.reason != null }.groupBy { it.reason }.forEach { (reason, affected) ->
                    if (reason != data.reason) {
                        Text(affected.joinToString { it.label }, style = MaterialTheme.typography.labelLarge)
                        Text(reason.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
                val commonMethod = left?.methodology_version == right?.methodology_version &&
                    left?.scoring_policy == right?.scoring_policy &&
                    left?.methodology_status == right?.methodology_status
                if (commonMethod) MethodologyDetails(left ?: right) else {
                    Text(a, fontWeight = FontWeight.SemiBold)
                    MethodologyDetails(left)
                    Text(b, fontWeight = FontWeight.SemiBold)
                    MethodologyDetails(right)
                }
                CompanySources(left)
                CompanySources(right)
            }
        }
    }
}

private fun pillarOutcome(row: ComparisonRow, a: String, b: String): String {
    val winner = when (row.higher) {
        "a" -> a
        "b" -> b
        else -> return row.result.removePrefix("— ").removePrefix("= ")
    }
    val lead = row.delta?.toBigDecimalOrNull()?.abs()?.setScale(2, RoundingMode.HALF_UP)
    return if (lead == null || lead.signum() == 0) "Lidera $winner"
    else "$winner +${lead.toPlainString().replace('.', ',')} pts"
}

@Composable
private fun PillarCell(value: String, leading: Boolean, modifier: Modifier) {
    Text(value.removeSuffix(" / 100"), modifier, textAlign = TextAlign.Center,
        style = if (value == "No disponible") MaterialTheme.typography.bodySmall else MaterialTheme.typography.titleSmall,
        color = if (leading) CompareGreen else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (leading) FontWeight.Bold else FontWeight.Normal)
}

@Composable
internal fun ComparisonDetails(showLabel: String, hideLabel: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) hideLabel else showLabel)
            Spacer(Modifier.width(6.dp))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
        }
        if (expanded) content()
    }
}

@Composable
private fun MethodologyDetails(data: PillarsResponse?) {
    Text("Metodología: ${data?.methodology_version ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
    Text("Normalización: ${data?.scoring_policy?.version ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
    data?.methodology_status?.let { Text("Estado metodológico: $it", style = MaterialTheme.typography.bodySmall) }
    data?.scoring_policy?.explanation?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun CompanySources(data: PillarsResponse?) {
    val sources = data?.pillars.orEmpty().filterNotNull().flatMap { it.metrics.orEmpty().filterNotNull() }
        .flatMap { it.components.orEmpty().filterNotNull() }.mapNotNull { it.source }
        .filter { !it.url.isNullOrBlank() }.distinctBy { it.url }
    Text("Fuentes de ${data?.ticker.orEmpty()}", fontWeight = FontWeight.SemiBold)
    Text("Moneda: ${data?.currency ?: "No informada"}", style = MaterialTheme.typography.bodySmall)
    data?.reporting_note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (sources.isEmpty()) Text("Fuentes no informadas por el servidor.", style = MaterialTheme.typography.bodySmall)
    sources.forEach { source ->
        SourceLink(source.url, source.label ?: "Fuente ${data?.ticker}")
        if (source.is_demo == true) Text("Fuente de demostración", color = MaterialTheme.colorScheme.error)
    }
}
