package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable fun AnalysisOptions(include: Boolean, level: String, enabled: Boolean, available: Boolean, origin: String?, onInclude: (Boolean)->Unit, onLevel: (String)->Unit, onCheck: ()->Unit) {
    Text("Análisis del reporte", style = MaterialTheme.typography.titleMedium)
    Row { listOf("simple" to "Sencillo","technical" to "Técnico").forEach { (value,label) -> FilterChip(level==value,{onLevel(value)},enabled=enabled,label={Text(label)}) } }
    TextButton(onClick=onCheck,enabled=enabled) { Text("Comprobar análisis disponible") }
    Text(if (available) "Origen comprobado: $origin" else "Todavía no se confirmó un análisis para esta selección.")
    Row { Checkbox(include,onInclude,enabled=enabled && available); Text("Incluir análisis y sus fuentes") }
}
