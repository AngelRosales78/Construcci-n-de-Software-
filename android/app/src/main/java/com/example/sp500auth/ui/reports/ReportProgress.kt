package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sp500auth.data.repository.ReportJob
import com.example.sp500auth.ui.common.AppColors

/** Three-step tracker: request received → generating → PDF ready, with the measured times. */
@Composable fun ReportProgress(job: ReportJob, accepted: Long?, ready: Long?, download: Long?, canCancel: Boolean, onCancel: () -> Unit) {
    val failed = job.status in setOf("error", "cancelled")
    val step = when (job.status) { "queued" -> 1; "running" -> 2; "ready" -> 3; else -> 0 }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(when (job.status) { "queued" -> "Solicitud recibida"; "running" -> "Generando reporte"; "ready" -> "PDF disponible"
            "cancelled" -> "Trabajo cancelado por el servidor"; else -> "Error de generación" },
            fontWeight = FontWeight.Bold, color = if (failed) MaterialTheme.colorScheme.error else AppColors.Green)
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf("Recibido", "Generando", "Listo").forEachIndexed { index, label ->
                val done = step > index || step == 3
                val current = step == index + 1 && step != 3
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Surface(Modifier.size(28.dp), shape = CircleShape, color = when {
                        failed -> MaterialTheme.colorScheme.error.copy(alpha = .2f); done -> AppColors.Green; current -> AppColors.Green.copy(alpha = .25f)
                        else -> AppColors.Inner }) {
                        Box(contentAlignment = Alignment.Center) {
                            when {
                                failed -> Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                done -> Icon(Icons.Default.Check, null, tint = androidx.compose.ui.graphics.Color.Black, modifier = Modifier.size(16.dp))
                                current -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = AppColors.Green)
                                else -> Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
                            }
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (done || current) MaterialTheme.colorScheme.onSurface else AppColors.Muted)
                }
            }
        }
        job.progress?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth(), color = AppColors.Green); Text("Avance informado por el servidor: $it %", style = MaterialTheme.typography.labelSmall) }
        if (job.status in setOf("queued", "running")) {
            if (job.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green)
            TextButton(onClick = onCancel, enabled = canCancel) { Text("Cancelar generación") }
        }
        val metrics = listOfNotNull(
            accepted?.let { "Aceptación de solicitud: $it ms" },
            ready?.let { "Disponibilidad observada: $it ms · ${if (it <= 3000) "dentro del objetivo de 3 s" else "supera el objetivo de 3 s"}" },
            download?.let { "Descarga completa: $it ms" },
            job.size_bytes?.let { "Tamaño anunciado: ${"%.1f".format(it / 1024.0)} KB${if (it >= 2_000_000) " · supera el objetivo de 2 MB" else ""}" })
        if (metrics.isNotEmpty()) Surface(color = AppColors.Inner, shape = MaterialTheme.shapes.medium) {
            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Speed, null, tint = AppColors.Muted, modifier = Modifier.size(16.dp))
                    Text("Medidas", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                }
                metrics.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (step in 1..2) Text("La aceptación de la solicitud no equivale a tener el PDF generado.", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
    }
}
