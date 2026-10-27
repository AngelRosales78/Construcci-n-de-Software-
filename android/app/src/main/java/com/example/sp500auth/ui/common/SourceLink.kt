package com.example.sp500auth.ui.common
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import java.net.URI

fun safeSourceUrl(value: String?): Boolean = try {
    val uri=URI(value ?: "")
    uri.scheme in setOf("http","https") && !uri.host.isNullOrBlank() && uri.userInfo==null
} catch(_:Exception){false}

@Composable
fun SourceLink(url: String?,label: String?=null) {
    val handler=LocalUriHandler.current
    if(safeSourceUrl(url)) TextButton(onClick={runCatching{handler.openUri(url!!)}}){Text(label ?: url.orEmpty())}
    else Text("Fuente no disponible")
}
