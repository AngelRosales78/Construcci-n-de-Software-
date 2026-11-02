package com.example.sp500auth.ui.sprint3

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.sp500auth.util.Resource

@Composable fun <T> RemoteContent(state: Resource<T>, retry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        is Resource.Loading -> CircularProgressIndicator()
        is Resource.Error -> { Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error); TextButton(onClick = retry) { Text("Reintentar") } }
        is Resource.Success -> content(state.successData)
        else -> Text("Consulta pendiente.")
    }
}
