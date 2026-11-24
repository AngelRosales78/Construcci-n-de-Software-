package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.ui.common.AppColors
import com.example.sp500auth.ui.compare.historyRanges

@Composable fun ChartOptions(include: Boolean, range: String, enabled: Boolean, onInclude: (Boolean) -> Unit, onRange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OptionRow(Icons.Default.ShowChart, "Gráficos", "Precio histórico y radar de pilares, en vector", include, enabled, onInclude)
        if (include) {
            Text("Rango histórico: $range", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                historyRanges.forEach { period ->
                    FilterChip(range == period, { onRange(period) }, enabled = enabled, label = { Text(period) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Green.copy(alpha = .18f), selectedLabelColor = AppColors.Green))
                }
            }
        }
    }
}

/** A labelled switch with an icon, used for each optional section of the PDF. */
@Composable internal fun OptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String,
                                   checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = if (checked) AppColors.Green else AppColors.Muted)
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
        }
        Switch(checked, onChange, enabled = enabled)
    }
}
