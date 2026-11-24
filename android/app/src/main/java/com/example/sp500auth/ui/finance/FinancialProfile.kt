package com.example.sp500auth.ui.finance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.api.models.CompanySummary

/** HU04_01: one level of the financial profile scale, from "Muy sólido" to "Muy débil". */
data class ProfileLevel(val category: String, val label: String, val color: Color, val meaning: String)

internal val profileScale = listOf(
    ProfileLevel("strong", "Muy sólido", Color(0xFF36F5A0), "Altman-Z mayor que 2,99: zona segura."),
    ProfileLevel("moderate", "Moderado", Color(0xFFE0C34D), "Altman-Z entre 1,81 y 2,99: zona gris."),
    ProfileLevel("weak", "Muy débil", Color(0xFFFF8F8F), "Altman-Z menor que 1,81: zona de riesgo."),
)
private val unknownLevel = ProfileLevel("unknown", "Sin datos suficientes", Color(0xFF9BAAA8),
    "El balance no tiene componentes suficientes para clasificar.")

/** Uses the server label when present; falls back to the Altman zone. Never invents a category. */
internal fun profileLevel(company: CompanySummary): ProfileLevel {
    val byCategory = profileScale.firstOrNull { it.category == company.profile?.category }
    val byZone = when (company.altman_z?.zone) { "safe" -> profileScale[0]; "grey" -> profileScale[1]; "distress" -> profileScale[2]; else -> null }
    val level = byCategory ?: byZone ?: unknownLevel
    return company.profile?.label?.takeIf { it.isNotBlank() && byCategory != null }?.let { level.copy(label = it) } ?: level
}

@Composable
internal fun FinancialProfileCard(company: CompanySummary) {
    val level = profileLevel(company)
    var legend by remember { mutableStateOf(false) }
    Surface(color = Color(0xFF1C2024), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Perfil financiero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { legend = !legend }, modifier = Modifier.semantics { contentDescription = "Qué significa cada categoría" }) {
                    Icon(Icons.Outlined.Info, null, tint = Color(0xFF9BAAA8))
                }
            }
            Surface(color = level.color.copy(alpha = .16f), shape = RoundedCornerShape(50)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(10.dp).background(level.color, CircleShape))
                    Text(level.label, color = level.color, fontWeight = FontWeight.Bold)
                }
            }
            Text(level.meaning, color = Color(0xFF9BAAA8), style = MaterialTheme.typography.bodySmall)
            if (legend) profileScale.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(8.dp).background(item.color, CircleShape))
                    Text("${item.label}: ${item.meaning}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
