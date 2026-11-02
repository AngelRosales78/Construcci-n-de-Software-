package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.sp500auth.data.repository.ReportJob

@Composable fun ReportProgress(job:ReportJob, accepted:Long?, ready:Long?, download:Long?, canCancel:Boolean,onCancel:()->Unit) {
    Column {
        Text(when(job.status) {"queued"->"Solicitud recibida";"running"->"Generando reporte";"ready"->"PDF disponible";"cancelled"->"Trabajo cancelado por el servidor";else->"Error de generación"})
        job.progress?.let { Text("Avance informado por el servidor: $it %") }
        if(job.status in setOf("queued","running")) { LinearProgressIndicator(); TextButton(onClick=onCancel,enabled=canCancel) {Text("Cancelar generación")} }
        accepted?.let { Text("Aceptación de solicitud: $it ms") }
        ready?.let { Text("Disponibilidad observada: $it ms · ${if(it<=3000) "dentro del objetivo de 3 s" else "supera el objetivo de 3 s"}") }
        download?.let { Text("Descarga completa: $it ms") }
        job.size_bytes?.let {Text("Tamaño anunciado: $it bytes${if(it>=2_000_000) " · supera el objetivo de 2 MB" else ""}")}
        Text("La aceptación de la solicitud no equivale a tener el PDF generado.")
    }
}
