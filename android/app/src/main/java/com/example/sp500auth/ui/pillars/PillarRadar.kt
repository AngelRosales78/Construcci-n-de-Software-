package com.example.sp500auth.ui.pillars

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.FinancialPillar
import kotlin.math.*

@Composable
fun PillarRadar(pillars: List<FinancialPillar>, onSelect: (String) -> Unit) {
    val ids = pillarLabels.keys.toList()
    val scores = ids.map { id -> scoreNumber(pillars.firstOrNull { it.id == id }?.score) }
    val accent = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(240.dp).semantics {
        contentDescription = "Radar de cinco pilares. Los datos ausentes no se representan como cero. Usa los botones para ver el detalle."
    }.pointerInput(pillars) {
        detectTapGestures { position ->
            val angle = atan2((position.y - size.height / 2f).toDouble(), (position.x - size.width / 2f).toDouble()) + PI / 2
            val index = ((angle / (2 * PI) * 5).roundToInt() % 5 + 5) % 5
            onSelect(ids[index])
        }
    }) {
        val center = Offset(size.width/2, size.height/2)
        val radius = min(size.width, size.height) * .42f
        fun point(i: Int, fraction: Float): Offset {
            val a = -PI / 2 + i * 2 * PI / 5
            return center + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius * fraction
        }
        for (level in 1..4) {
            val path = Path()
            ids.indices.forEach { i -> val q = point(i, level / 4f); if (i == 0) path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y) }
            path.close(); drawPath(path, Color.Gray.copy(alpha=.4f), style=Stroke(1.dp.toPx()))
        }
        ids.indices.forEach { drawLine(Color.Gray, center, point(it,1f),1.dp.toPx()) }
        if (scores.all { it != null }) {
            val path = Path()
            scores.forEachIndexed { i, value -> val q=point(i,value!!/100); if(i==0) path.moveTo(q.x,q.y) else path.lineTo(q.x,q.y) }
            path.close(); drawPath(path,accent.copy(alpha=.2f)); drawPath(path,accent,style=Stroke(2.dp.toPx()))
        }
        scores.forEachIndexed { i, value -> if(value!=null) drawCircle(accent,4.dp.toPx(),point(i,value/100)) }
    }
    ids.forEach { id ->
        val score = pillars.firstOrNull { it.id == id }?.score
        OutlinedButton(onClick={onSelect(id)}, modifier=Modifier.fillMaxWidth()) {
            Text("${pillarLabels[id]}: ${if(scoreNumber(score)!=null) score?.display_value + " / 100" else "No disponible"}")
        }
    }
}
