package com.example.sp500auth.ui.finance
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PriceLine(values: List<Double>) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(170.dp).semantics { contentDescription = "Gráfico del histórico de precios, ${values.size} cierres." }) {
        val inset = 8.dp.toPx()
        val width = (size.width - inset * 2).coerceAtLeast(1f)
        val height = (size.height - inset * 2).coerceAtLeast(1f)
        repeat(3) { index ->
            val y = inset + height * index / 2
            drawLine(color.copy(alpha = .08f), Offset(inset, y), Offset(size.width - inset, y))
        }
        if (values.isNotEmpty()) {
            val low = values.minOrNull()!!
            val span = (values.maxOrNull()!! - low).takeIf { it > 0 } ?: 1.0
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = inset + i * width / (values.size - 1).coerceAtLeast(1)
                val y = inset + height - ((v - low) / span * height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            if (values.size > 1) {
                val fill = Path().apply { addPath(path); lineTo(inset + width, inset + height); lineTo(inset, inset + height); close() }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = .25f), color.copy(alpha = 0f))))
            }
            drawPath(path, color, style = Stroke(2.dp.toPx()))
            if (values.size == 1) drawCircle(color, 3.dp.toPx(), Offset(inset, inset + height))
        }
    }
}
