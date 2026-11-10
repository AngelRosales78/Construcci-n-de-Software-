package com.example.sp500auth.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable fun PdfFileActions(ready: Boolean, saved: Boolean, busy: Boolean, onSave: () -> Unit, onOpen: () -> Unit, onShare: () -> Unit) {
    Column {
        Button(onClick = onSave, enabled = ready && !busy) { Text(if (busy) "Guardando…" else "Guardar PDF") }
        Row {
            TextButton(onClick = onOpen, enabled = saved && !busy) { Text("Abrir PDF") }
            TextButton(onClick = onShare, enabled = saved && !busy) { Text("Compartir PDF") }
        }
        Text("Elige dónde guardar el archivo. Puedes cancelar el selector sin guardar nada.", style = MaterialTheme.typography.bodySmall)
    }
}
