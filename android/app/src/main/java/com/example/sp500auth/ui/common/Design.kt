package com.example.sp500auth.ui.common

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Shared palette of the finance screens. */
object AppColors {
    val Background = Color(0xFF101416)
    val Panel = Color(0xFF1C2024)
    val Inner = Color(0xFF252B30)
    val Line = Color(0xFF2A3036)
    val Green = Color(0xFF36F5A0)
    val Blue = Color(0xFF60CDF9)
    val Amber = Color(0xFFE0C34D)
    val Red = Color(0xFFFF8F8F)
    val Muted = Color(0xFF9BAAA8)
}

/**
 * Logo of a company: the server's logo_url when present, otherwise the public logo published by ticker.
 * Only the ticker leaves the device. Dots become hyphens (BRK.B → BRK-B) as the provider expects.
 */
fun companyLogoUrl(ticker: String, serverUrl: String? = null): String? {
    serverUrl?.trim()?.takeIf { it.startsWith("https://") }?.let { return it }
    val symbol = ticker.trim().uppercase(Locale.ROOT).replace('.', '-')
    if (!Regex("[A-Z0-9-]{1,15}").matches(symbol)) return null
    return "https://financialmodelingprep.com/image-stock/$symbol.png"
}

fun tickerInitials(ticker: String): String = ticker.filter { it.isLetterOrDigit() }.take(4).uppercase(Locale.ROOT).ifBlank { "?" }

/** Small logo cache: memory first, then a 20 MB disk cache kept for a week. */
object LogoLoader {
    private val memory = LruCache<String, ImageBitmap>(250)
    private val missing = ConcurrentHashMap.newKeySet<String>()
    @Volatile private var client: OkHttpClient? = null

    private fun client(context: Context): OkHttpClient = client ?: synchronized(this) {
        client ?: OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "company-logos"), 20L * 1024 * 1024))
            .connectTimeout(6, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor { chain ->
                chain.proceed(chain.request()).newBuilder().header("Cache-Control", "public, max-age=604800").removeHeader("Pragma").build()
            }.build().also { client = it }
    }

    fun cached(url: String?): ImageBitmap? = url?.let { memory.get(it) }

    suspend fun load(context: Context, url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        memory.get(url)?.let { return@withContext it }
        if (url in missing) return@withContext null
        try {
            val request = Request.Builder().url(url).cacheControl(CacheControl.Builder().maxStale(7, TimeUnit.DAYS).build()).build()
            client(context.applicationContext).newCall(request).execute().use { response ->
                val bytes = if (response.isSuccessful) response.body?.bytes() else null
                val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                if (bitmap == null || bitmap.width < 4) { missing += url; null }
                else bitmap.asImageBitmap().also { memory.put(url, it) }
            }
        } catch (_: IOException) { null } // offline: retry on the next screen, never mark as missing
    }
}

@Composable
fun CompanyLogo(ticker: String, modifier: Modifier = Modifier, size: Dp = 44.dp, logoUrl: String? = null, accent: Color = AppColors.Green) {
    val url = remember(ticker, logoUrl) { companyLogoUrl(ticker, logoUrl) }
    val context = LocalContext.current
    val image by produceState(LogoLoader.cached(url), url) {
        if (value == null && url != null) value = LogoLoader.load(context, url)
    }
    val shape = RoundedCornerShape(size * 0.28f)
    Surface(modifier.size(size), shape = shape, color = if (image != null) Color.White else AppColors.Inner) {
        Box(contentAlignment = Alignment.Center) {
            val bitmap = image
            if (bitmap != null) Image(bitmap, contentDescription = "Logo de $ticker", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(size * 0.14f))
            else Text(tickerInitials(ticker), color = accent, fontWeight = FontWeight.Bold,
                fontSize = (size.value * if (ticker.length > 3) 0.24f else 0.3f).sp, maxLines = 1)
        }
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Panel)) {
        Column(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun AppSectionTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted) }
    }
}

/** Top bar with a back arrow, used by the secondary screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String, subtitle: String? = null, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = {
        Column {
            Text(title, fontWeight = FontWeight.Bold, maxLines = 1)
            subtitle?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, maxLines = 1) }
        }
    }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
    }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background))
}

/** Rounded pill with a coloured label, for statuses and tags. */
@Composable
fun StatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = color.copy(alpha = .15f), shape = RoundedCornerShape(50)) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
    }
}

/** In-content header for screens without a Scaffold: back arrow, optional company logo, title and subtitle. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: () -> Unit, ticker: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        if (ticker != null) CompanyLogo(ticker, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted) }
        }
    }
}
