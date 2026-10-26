package com.example.sp500auth.ui.finance
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PriceLine(values: List<Double>) {
    val color=MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(160.dp).semantics{contentDescription="Gráfico del histórico de precios, ${values.size} cierres."}) {
        if(values.isNotEmpty()) {
            val low=values.minOrNull()!!;val span=(values.maxOrNull()!!-low).takeIf{it>0} ?: 1.0
            val path=Path()
            values.forEachIndexed{i,v ->
                val x=i*size.width/(values.size-1).coerceAtLeast(1);val y=size.height-((v-low)/span*size.height).toFloat()
                if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
            }
            drawPath(path,color,style=Stroke(2.dp.toPx()))
            if(values.size==1)drawCircle(color,3.dp.toPx(),Offset(0f,size.height))
        }
    }
}
