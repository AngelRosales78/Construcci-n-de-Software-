package com.example.sp500auth.ui.compare

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.ui.common.SourceLink
import java.util.Locale

private fun number(value: Double) = String.format(Locale.getDefault(), "%.2f", value)

@Composable
fun HistoryComparisonChart(data: AlignedHistory) {
    if (data.points.size < 2) { Text("No hay suficientes fechas comunes."); return }
    var index by rememberSaveable(data.a, data.b, data.range, data.points.first().date, data.points.last().date, data.points.size) { mutableStateOf(data.points.lastIndex) }
    val selected = data.points[index.coerceIn(0, data.points.lastIndex)]
    val colorA = CompareGreen; val colorB = Color(0xFF60CDF9)
    val low = minOf(0.0, data.points.minOf { minOf(it.a, it.b) })
    val high = maxOf(0.0, data.points.maxOf { maxOf(it.a, it.b) }).let { if (it == low) it + 1 else it }
    Card(colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Rendimiento histórico comparado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("A · ${data.a}: línea continua", color = colorA)
            Text("B · ${data.b}: línea discontinua", color = colorB)
            Text("${data.points.first().date} a ${data.points.last().date} · ${data.points.size} fechas comunes")
            Text("Ambas series parten de 0 %. Variación = (precio / precio inicial − 1) × 100.", style = MaterialTheme.typography.bodySmall)
            Text("Escala: ${number(low)} % a ${number(high)} %", style = MaterialTheme.typography.labelSmall)
            Canvas(Modifier.fillMaxWidth().height(220.dp)
                .semantics { contentDescription = "Rendimiento de ${data.a} y ${data.b}, rango ${data.range}. Toca o arrastra para consultar una fecha; también puedes usar Anterior y Siguiente." }
                .pointerInput(data) { detectTapGestures { position -> index = nearestHistoryIndex(data.points, (position.x - 12.dp.toPx()) / (size.width - 24.dp.toPx()).coerceAtLeast(1f)) } }
                .pointerInput(data, "drag") { detectDragGestures { change, _ -> change.consume(); index = nearestHistoryIndex(data.points, (change.position.x - 12.dp.toPx()) / (size.width - 24.dp.toPx()).coerceAtLeast(1f)) } }) {
                val inset = 12.dp.toPx(); val width = (size.width - 2 * inset).coerceAtLeast(1f); val height = size.height - 2 * inset
                fun x(point: ReturnPoint) = inset + ((point.epochDay - data.points.first().epochDay).toDouble() / (data.points.last().epochDay - data.points.first().epochDay) * width).toFloat()
                fun y(value: Double) = inset + height - ((value - low) / (high - low) * height).toFloat()
                repeat(3) { grid -> drawLine(Color.Gray.copy(alpha = .25f), Offset(inset, inset + height * grid / 2), Offset(inset + width, inset + height * grid / 2)) }
                drawLine(Color.Gray, Offset(inset, y(0.0)), Offset(inset + width, y(0.0)))
                fun line(a: Boolean): Path = Path().apply { data.points.forEachIndexed { i, point -> if (i == 0) moveTo(x(point), y(if (a) point.a else point.b)) else lineTo(x(point), y(if (a) point.a else point.b)) } }
                drawPath(line(true), colorA, style = Stroke(2.dp.toPx()))
                drawPath(line(false), colorB, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 5.dp.toPx()))))
                drawLine(Color.White.copy(alpha = .45f), Offset(x(selected), inset), Offset(x(selected), inset + height))
                drawCircle(colorA, 4.dp.toPx(), Offset(x(selected), y(selected.a)))
                drawCircle(colorB, 4.dp.toPx(), Offset(x(selected), y(selected.b)))
            }
            Text("Fecha consultada: ${selected.date}", fontWeight = FontWeight.Bold)
            Text("${data.a}: ${number(selected.a)} % · Cierre ajustado ${number(selected.aPrice)} ${data.currenciesA.joinToString().ifBlank { "Moneda no informada" }}")
            Text("${data.b}: ${number(selected.b)} % · Cierre ajustado ${number(selected.bPrice)} ${data.currenciesB.joinToString().ifBlank { "Moneda no informada" }}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { index-- }, enabled = index > 0) { Text("Anterior") }
                OutlinedButton(onClick = { index++ }, enabled = index < data.points.lastIndex) { Text("Siguiente") }
            }
            data.warnings.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Text("Normalización de visualización realizada en Android con cierres del servidor.", style = MaterialTheme.typography.labelSmall)
            data.sourcesA.forEach { SourceLink(it, "Fuente ${data.a}") }
            data.sourcesB.forEach { SourceLink(it, "Fuente ${data.b}") }
        }
    }
}
