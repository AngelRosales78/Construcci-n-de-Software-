package com.example.sp500auth.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    val winsA = rows.count { it.higher == "a" }
    val winsB = rows.count { it.higher == "b" }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Cinco pilares lado a lado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("FY${data.fiscal_year ?: "—"} · Notas sobre 100", style = MaterialTheme.typography.labelMedium, color = CompareMuted)
            if (leftPeriod == rightPeriod) Text(leftPeriod, style = MaterialTheme.typography.bodySmall, color = CompareMuted)
            else {
                Text("$a · $leftPeriod", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
                Text("$b · $rightPeriod", style = MaterialTheme.typography.bodySmall, color = CompareMuted)
            }
        }
        data.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }

        Scoreboard(a, b, winsA, winsB, rows.size)

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ComparePanel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(CompareGreen, "A · $a"); LegendDot(CompareBlue, "B · $b")
                }
                rows.forEachIndexed { index, row ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
                    PillarBars(row, a, b)
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

internal fun scoreValue(text: String): Float? =
    text.removeSuffix(" / 100").trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(0f, 100f)

@Composable
private fun Scoreboard(a: String, b: String, winsA: Int, winsB: Int, total: Int) {
    val headline = when {
        winsA > winsB -> "$a lidera $winsA de $total pilares"
        winsB > winsA -> "$b lidera $winsB de $total pilares"
        winsA == 0 -> "Sin un líder claro en los pilares"
        else -> "Empate: $winsA pilares para cada una"
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ScoreSide(a, winsA, CompareGreen, winsA > winsB, Modifier.weight(1f))
                Text("vs", color = CompareMuted, style = MaterialTheme.typography.titleMedium)
                ScoreSide(b, winsB, CompareBlue, winsB > winsA, Modifier.weight(1f))
            }
            if (winsA + winsB > 0) Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))) {
                if (winsA > 0) Box(Modifier.weight(winsA.toFloat()).fillMaxHeight().background(CompareGreen))
                if (winsB > 0) Box(Modifier.weight(winsB.toFloat()).fillMaxHeight().background(CompareBlue))
            }
            Text(headline, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ScoreSide(ticker: String, wins: Int, color: Color, leading: Boolean, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        com.example.sp500auth.ui.common.CompanyLogo(ticker, size = 40.dp, accent = color)
        Text(ticker, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$wins", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold,
            color = if (leading) color else MaterialTheme.colorScheme.onSurface)
        Text(if (wins == 1) "pilar ganado" else "pilares ganados", style = MaterialTheme.typography.labelSmall, color = CompareMuted)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape)); Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun PillarBars(row: ComparisonRow, a: String, b: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(row.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(pillarOutcome(row, a, b), style = MaterialTheme.typography.labelMedium,
                color = when (row.higher) { "a" -> CompareGreen; "b" -> CompareBlue; else -> CompareMuted })
        }
        ScoreBar(row.aValue, CompareGreen, row.higher == "a")
        ScoreBar(row.bValue, CompareBlue, row.higher == "b")
    }
}

@Composable
private fun ScoreBar(text: String, color: Color, leading: Boolean) {
    val value = scoreValue(text)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFF2A3036))) {
            if (value != null) Box(Modifier.fillMaxWidth(value / 100f).fillMaxHeight().clip(RoundedCornerShape(50))
                .background(if (leading) color else color.copy(alpha = .55f)))
        }
        Text(if (value == null) "Sin dato" else text.removeSuffix(" / 100"), modifier = Modifier.widthIn(min = 52.dp), textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelLarge, fontWeight = if (leading) FontWeight.Bold else FontWeight.Normal,
            color = if (value == null) CompareMuted else if (leading) color else MaterialTheme.colorScheme.onSurface)
    }
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
