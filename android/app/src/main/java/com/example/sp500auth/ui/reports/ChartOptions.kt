package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.sp500auth.ui.compare.historyRanges

@Composable fun ChartOptions(include: Boolean, range: String, enabled: Boolean, onInclude: (Boolean)->Unit, onRange: (String)->Unit) {
    Row { Checkbox(include,onInclude,enabled=enabled); Text("Incluir gráfico de precios y radar") }
    if (include) {
        Text("Rango histórico a exportar: $range")
        Row { historyRanges.forEach { period -> FilterChip(range==period,{onRange(period)},enabled=enabled,label={Text(period)}) } }
        Text("Se solicitarán gráficos vectoriales generados con los datos del servidor; no capturas de la pantalla.")
    }
}
