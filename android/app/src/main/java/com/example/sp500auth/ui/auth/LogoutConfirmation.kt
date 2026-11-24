package com.example.sp500auth.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** HU01_04 / HU02_05: closing the session always asks first and offers "Cancelar". */
@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Se revocará la sesión y se borrarán de este dispositivo los tokens y el acceso por huella. Para volver a entrar necesitarás tu contraseña.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
