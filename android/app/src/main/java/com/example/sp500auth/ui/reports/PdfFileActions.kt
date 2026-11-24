package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.ui.common.AppCard
import com.example.sp500auth.ui.common.AppColors

/** Final card once the PDF is downloaded and verified: save, then open or share the saved copy. */
@Composable fun PdfFileActions(ready: Boolean, saved: Boolean, busy: Boolean, fileName: String? = null,
                               onSave: () -> Unit, onOpen: () -> Unit, onShare: () -> Unit) {
    if (!ready && !saved) return
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = Color(0xFFE5484D).copy(alpha = .16f), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFFF6B6B), modifier = Modifier.padding(10.dp).size(28.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(fileName ?: "Reporte PDF", fontWeight = FontWeight.SemiBold)
                Text(if (saved) "Guardado en tu teléfono" else "Descargado y verificado. Falta guardarlo.", style = MaterialTheme.typography.bodySmall,
                    color = if (saved) AppColors.Green else AppColors.Muted)
            }
        }
        Button(onClick = onSave, enabled = ready && !busy, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Icon(Icons.Default.SaveAlt, null); Spacer(Modifier.width(8.dp)); Text(if (busy) "Guardando…" else if (saved) "Guardar otra copia" else "Guardar PDF")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpen, enabled = saved && !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Abrir PDF")
            }
            OutlinedButton(onClick = onShare, enabled = saved && !busy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Share, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Compartir PDF")
            }
        }
        Text("Elige dónde guardar el archivo. Puedes cancelar el selector sin guardar nada.", style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
    }
}
