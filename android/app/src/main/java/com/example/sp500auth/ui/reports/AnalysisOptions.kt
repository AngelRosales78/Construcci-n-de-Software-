package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sp500auth.ui.common.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun AnalysisOptions(include: Boolean, level: String, enabled: Boolean, available: Boolean, origin: String?,
                                onInclude: (Boolean) -> Unit, onLevel: (String) -> Unit, onCheck: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionRow(Icons.Default.AutoAwesome, "Análisis del reporte",
            if (available) "Resumen y explicaciones con sus fuentes" else "Primero comprueba que haya un análisis disponible",
            include, enabled && available, onInclude)
        val levels = listOf("simple" to "Sencillo", "technical" to "Técnico")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            levels.forEachIndexed { index, (value, label) ->
                SegmentedButton(selected = level == value, onClick = { onLevel(value) }, enabled = enabled,
                    shape = SegmentedButtonDefaults.itemShape(index, levels.size)) { Text(label) }
            }
        }
        if (available) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.CheckCircle, null, tint = AppColors.Green, modifier = Modifier.size(18.dp))
            Text("Origen comprobado: $origin", style = MaterialTheme.typography.bodySmall)
        } else OutlinedButton(onClick = onCheck, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Comprobar análisis disponible") }
    }
}
