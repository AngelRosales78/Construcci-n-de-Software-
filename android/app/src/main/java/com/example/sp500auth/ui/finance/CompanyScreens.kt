package com.example.sp500auth.ui.finance

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.data.api.models.CompanySummary
import com.example.sp500auth.ui.auth.AuthViewModel
import com.example.sp500auth.util.Resource
import com.example.sp500auth.ui.favorites.*
import com.example.sp500auth.ui.sprint3.*
import com.example.sp500auth.ui.common.CompanyLogo
import androidx.compose.foundation.clickable
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val Background = Color(0xFF101416)
private val Panel = Color(0xFF1C2024)
private val Inner = Color(0xFF252B30)
private val Green = Color(0xFF36F5A0)
private val Muted = Color(0xFF9BAAA8)
private val sectors = listOf("" to "Todos", "technology" to "Tecnología", "healthcare" to "Salud",
    "financials" to "Finanzas", "consumer_discretionary" to "Consumo discrecional",
    "consumer_staples" to "Consumo básico", "industrials" to "Industria", "energy" to "Energía",
    "materials" to "Materiales", "utilities" to "Servicios públicos", "real_estate" to "Inmuebles",
    "communication_services" to "Comunicaciones")

private fun money(value: String?): String = value?.toBigDecimalOrNull()?.let {
    NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(it)
} ?: "No disponible"

private fun capital(value: String?): String {
    val amount = value?.toBigDecimalOrNull() ?: return "No disponible"
    val unit = listOf(BigDecimal("1000000000000") to " T", BigDecimal("1000000000") to " mil M", BigDecimal("1000000") to " M")
        .firstOrNull { amount.abs() >= it.first }
    return if (unit == null) "$${money(value)}" else "$${amount.divide(unit.first, 2, java.math.RoundingMode.HALF_UP).toPlainString()}${unit.second}"
}

@Composable
private fun FinanceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = Muted) }
    }
}

@Composable
private fun Retry(message: String, action: () -> Unit) {
    FinanceCard { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = action) { Text("Reintentar") } }
}

@Composable
fun CompanySearchScreen(nav: NavController, auth: AuthViewModel, vm: CompanyViewModel = hiltViewModel(), favorites: FavoritesViewModel = hiltViewModel(),recent:RecentViewModel=hiltViewModel(),
                        notices: com.example.sp500auth.ui.ops.NotificationsViewModel? = null, account: com.example.sp500auth.ui.ops.AccountViewModel? = null) {
    RefreshFavoritesOnResume(favorites)
    var query by rememberSaveable { mutableStateOf("") }
    var page by rememberSaveable { mutableStateOf(1) }
    var sector by rememberSaveable { mutableStateOf("") }
    var descending by rememberSaveable { mutableStateOf(true) }
    var readyOnly by rememberSaveable { mutableStateOf(true) }
    fun search() = vm.search(query, page, sector, if (descending) "-market_cap" else "market_cap", readyOnly)
    LaunchedEffect(Unit) { search() }
    LaunchedEffect(Unit){recent.load()}
    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) com.example.sp500auth.ui.auth.LogoutConfirmDialog({ confirmLogout = false; auth.logout() }, { confirmLogout = false })
    Scaffold(containerColor = Background, bottomBar = {
        NavigationBar(containerColor = Panel) {
            NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Explore, null) }, label = { Text("Explorar") })
            NavigationBarItem(selected = false, onClick = { nav.navigate("compare") }, icon = { Icon(Icons.Default.CompareArrows, null) }, label = { Text("Comparar") })
            NavigationBarItem(selected = false, onClick = { nav.navigate("favorites") }, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Favoritos") })
            NavigationBarItem(selected = false, onClick = { nav.navigate("profile") }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Mi cuenta") })
            NavigationBarItem(selected = false, onClick = { confirmLogout = true }, icon = { Icon(Icons.Default.Logout, null) }, label = { Text("Salir") })
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("MERCADOS", color = Muted, style = MaterialTheme.typography.labelSmall); Text("Explorar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                    if (notices != null) { com.example.sp500auth.ui.ops.NotificationsBell(notices) { nav.navigate("notifications") }; Spacer(Modifier.width(4.dp)) }
                    IconButton(onClick = { nav.navigate("profile") }) { com.example.sp500auth.ui.ops.AvatarCircle(account?.photo, 38.dp, "Mi perfil") }
                }
            }
            item {
                OutlinedTextField(query, { query = it.take(200) }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Buscar empresa o símbolo") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { IconButton(onClick = { page = 1; search() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Buscar") } },
                    shape = RoundedCornerShape(14.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(sectors) { (value, label) -> FilterChip(selected = sector == value, onClick = { sector = value; page = 1; search() }, label = { Text(label) }) }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = readyOnly, onCheckedChange = { readyOnly = it; page = 1; search() })
                    Spacer(Modifier.width(10.dp))
                    Text("Solo empresas con datos para analizar", style = MaterialTheme.typography.bodySmall)
                }
                if (!readyOnly) Text("El catálogo completo también incluye empresas sin balances o histórico cargados.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            item {
                FinanceCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = Green.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.TrendingUp, null, tint = Green, modifier = Modifier.padding(10.dp).size(28.dp))
                        }
                        Column(Modifier.weight(1f)) { Text("Universo S&P 500", fontWeight = FontWeight.Bold); Text("503 empresas · cinco pilares · IA", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(-8.dp)) {
                        listOf("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL").forEach { CompanyLogo(it, size = 34.dp) }
                    }
                    Text("Consulta datos, fuentes y cinco pilares de cada empresa.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SectionTitle("Empresas", "Orden por capitalización")
                FavoritesStatus(favorites)
                TextButton(onClick = { descending = !descending; page = 1; search() }) { Icon(Icons.Default.SwapVert, null); Text(if (descending) "Mayor a menor" else "Menor a mayor") }
            }
            item {RemoteContent(recent.state,recent::load){RecentCompanies(it.results.orEmpty().filterNotNull()){ticker->nav.navigate("company/${Uri.encode(ticker)}")}}}
            when (val state = vm.companies) {
                is Resource.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green) }
                is Resource.Error -> item { Retry(state.message.orEmpty()) { search() } }
                is Resource.Success -> {
                    val data = state.successData
                    item { Text("${data.count} resultados · Página $page", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    if (data.results.isNullOrEmpty()) item { FinanceCard { Text("No encontramos empresas con esta búsqueda.") } }
                    items(data.results.orEmpty()) { company -> CompanyRow(company, favorites) { company.ticker?.let { nav.navigate("company/${Uri.encode(it)}") } } }
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            OutlinedButton(enabled = data.previous != null, onClick = { page--; search() }) { Text("Anterior") }
                            OutlinedButton(enabled = data.next != null, onClick = { page++; search() }) { Text("Siguiente") }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CompanyRow(company: CompanyItem, favorites: FavoritesViewModel, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CompanyLogo(company.ticker.orEmpty(), size = 48.dp)
                Column(Modifier.weight(1f)) {
                    Text(company.name.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(company.ticker.orEmpty(), color = Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text("· ${company.sector_display ?: company.sector.orEmpty()}", color = Muted, style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(capital(company.market_cap), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Capitalización", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { FavoriteAction(company.ticker.orEmpty(), favorites) }
                TextButton(onClick = onOpen) { Text("Ver ficha"); Spacer(Modifier.width(4.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun CompanyDetailScreen(nav: NavController, ticker: String, vm: CompanyViewModel = hiltViewModel(), favorites: FavoritesViewModel = hiltViewModel(),recent:RecentViewModel=hiltViewModel()) {
    RefreshFavoritesOnResume(favorites)
    var range by rememberSaveable(ticker) { mutableStateOf("1Y") }
    val visitEvent=rememberSaveable(ticker){java.util.UUID.randomUUID().toString()}
    LaunchedEffect(ticker,vm.summary){if((vm.summary as? Resource.Success)?.successData?.ticker==ticker)recent.visit(ticker,visitEvent)}
    LaunchedEffect(ticker) { vm.load(ticker); if (range != "1Y") vm.history(ticker, range) }
    val encoded = Uri.encode(ticker)
    Scaffold(containerColor = Background, bottomBar = {
        Surface(color = Panel) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Insights, null, tint = Green)
                Spacer(Modifier.width(12.dp))
                Button(onClick = { nav.navigate("finance/$encoded/all") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Ver cinco pilares y puntaje") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a empresas") }
                    Text("Detalle del activo", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { nav.navigate("profile") }) { Icon(Icons.Default.AccountCircle, "Mi perfil", tint = Green) }
                }
                val logoUrl = (vm.summary as? Resource.Success<CompanySummary>)?.successData?.logo_url
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    CompanyLogo(ticker, size = 64.dp, logoUrl = logoUrl)
                    Column(Modifier.weight(1f)) {
                        val loaded = (vm.summary as? Resource.Success<CompanySummary>)?.successData
                        Text(loaded?.name ?: ticker, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            com.example.sp500auth.ui.common.StatusPill(ticker, Green)
                            loaded?.sector_display?.let { com.example.sp500auth.ui.common.StatusPill(it, com.example.sp500auth.ui.common.AppColors.Blue) }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { nav.navigate("compare?ticker=$encoded") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CompareArrows, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Comparar")
                    }
                    FilledTonalButton(onClick = { nav.navigate("report?a=$encoded&range=$range") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PictureAsPdf, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Exportar PDF")
                    }
                }
                FavoritesStatus(favorites)
                FavoriteAction(ticker, favorites)
                if(recent.state is Resource.Error){Text("No se pudo registrar la visita en recientes.");TextButton(onClick={recent.visit(ticker,visitEvent)}){Text("Reintentar registro de visita")}}
            }
            when (val state = vm.summary) {
                is Resource.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green) }
                is Resource.Error -> item { Retry(state.message.orEmpty()) { vm.load(ticker); if (range != "1Y") vm.history(ticker, range) } }
                is Resource.Success -> item {
                    val company = state.successData
                    Text("Último cierre", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(company.quote?.adjusted_close?.let { "$${money(it)}" } ?: "Precio no disponible", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("${company.quote?.currency ?: "Moneda no informada"} · Cierre ajustado ${company.quote?.date ?: "sin fecha"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                    company.reporting_note?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                    if (company.is_demo == true || company.quote?.is_demo == true) Text("Datos de demostración o heredados sin verificar", color = MaterialTheme.colorScheme.error)
                }
                else -> Unit
            }
            item {
                FinanceCard {
                    SectionTitle("Comportamiento del precio", "Histórico")
                    when (val state = vm.prices) {
                        is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)
                        is Resource.Error -> { Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.history(ticker, range) }) { Text("Reintentar histórico") } }
                        is Resource.Success -> {
                            val data = state.successData
                            val values = data.history.orEmpty().mapNotNull { it.price?.takeIf { v -> v.isFinite() } }
                            if (values.isEmpty()) Text("No hay cierres disponibles para este período.", color = Muted)
                            else { PriceLine(values); Text("${data.history?.firstOrNull()?.date} — ${data.history?.lastOrNull()?.date}", color = Muted, style = MaterialTheme.typography.labelSmall) }
                            Text("Moneda: ${data.currencies?.joinToString() ?: "No informada"}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            Text("Fuente: ${data.sources?.joinToString() ?: "No informada"}", color = Muted, style = MaterialTheme.typography.labelSmall)
                            if (data.is_demo == true) Text("Serie de demostración", color = MaterialTheme.colorScheme.error)
                        }
                        else -> Unit
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("1M", "6M", "1Y", "5Y").forEach { period ->
                            FilterChip(selected = range == period, onClick = { range = period; vm.history(ticker, period) }, label = { Text(period) })
                        }
                    }
                }
            }
            val company = (vm.summary as? Resource.Success<CompanySummary>)?.successData
            if (company != null) {
                item { SectionTitle("Métricas fundamentales", company.as_of ?: "Sin fecha") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("P/E Ratio", company.per ?: "No disponible", "Dato al ${company.field_sources?.get("per")?.as_of ?: company.as_of ?: "sin fecha"}", Modifier.weight(1f))
                        MetricTile("Retorno (ROE)", company.roe?.let { "$it %" } ?: if (company.field_sources?.get("roe")?.status == "not_applicable") "No interpretable" else "No disponible", company.field_sources?.get("roe")?.reason ?: "Balance al ${company.field_sources?.get("roe")?.as_of ?: company.as_of ?: "sin fecha"}", Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricTile("Capitalización", capital(company.market_cap), "Dato al ${company.field_sources?.get("market_cap")?.as_of ?: company.as_of ?: "sin fecha"}", Modifier.weight(1f))
                        MetricTile("Beta · S&P 500", company.beta ?: "No disponible", company.field_sources?.get("beta")?.reason ?: "Dato al ${company.field_sources?.get("beta")?.as_of ?: "sin fecha"}", Modifier.weight(1f))
                    }
                }
                item {
                    FinanceCard {
                        SectionTitle("Rendimiento por dividendos")
                        Text(company.dividend_yield?.let { "$it %" } ?: "No disponible", color = Green, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Dividendos registrados en los últimos doce meses · ${company.field_sources?.get("dividend_yield")?.as_of ?: "Sin fecha"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        if (company.dividend_yield == null) Text(company.field_sources?.get("dividend_yield")?.reason ?: "La fuente no proporcionó datos suficientes.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (company.is_active == false || !company.description.isNullOrBlank() || !company.website_url.isNullOrBlank()) item {
                    FinanceCard {
                        SectionTitle("Acerca de la empresa")
                        if (company.is_active == false) Text("Empresa inactiva en el catálogo: no aparece en las búsquedas. Sus datos se conservan.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        company.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        company.website_url?.takeIf { it.startsWith("https://") }?.let { com.example.sp500auth.ui.common.SourceLink(it, "Sitio web oficial") }
                    }
                }
                item { FinancialProfileCard(company) }
                item { SolvencyCard(company) }
                item {
                    FinanceCard {
                        SectionTitle("Análisis financiero")
                        Text("Liquidez · Rentabilidad · Solvencia · Eficiencia · Crecimiento", color = Muted, style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { nav.navigate("finance/$encoded/all") }, modifier = Modifier.fillMaxWidth()) { Text("Pilares, radar y puntaje") }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { nav.navigate("scenarios/$encoded") }, modifier = Modifier.weight(1f)) { Text("Escenarios") }
                            FilledTonalButton(onClick = { nav.navigate("analysis/$encoded/2024") }, modifier = Modifier.weight(1f)) { Text("Informe") }
                        }
                    }
                }
                item {
                    FinanceCard {
                        SectionTitle("Perfil de la compañía")
                        Text(company.name.orEmpty(), fontWeight = FontWeight.Bold)
                        Text(company.sector_display.orEmpty(), color = Green)
                        Text("Fecha financiera: ${company.as_of ?: "No disponible"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("Fuente financiera: ${company.source ?: "No disponible"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        Text("Fuente del precio: ${company.quote?.source ?: "No disponible"}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        company.field_sources.orEmpty().forEach { (key, origin) ->
                            val label = mapOf("market_cap" to "Capitalización", "per" to "P/E", "roe" to "ROE", "beta" to "Beta", "dividend_yield" to "Dividendos", "equity_value" to "Valor de mercado al cierre")[key]
                            if (label != null && origin.source != null) com.example.sp500auth.ui.common.SourceLink(origin.source, "$label · ${origin.as_of ?: "Fecha no disponible"}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, note: String, modifier: Modifier) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp).heightIn(min = 104.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Green)
            Text(note, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SolvencyCard(company: CompanySummary) {
    FinanceCard {
        SectionTitle("Solvencia (Altman Z-Score)")
        val score = company.altman_z?.score?.toDoubleOrNull()?.takeIf { it.isFinite() }
        if (score == null) Text("Sin componentes suficientes para calcular el Z-Score.", color = Muted)
        else {
            Box(Modifier.fillMaxWidth().height(155.dp), contentAlignment = Alignment.BottomCenter) {
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val diameter = minOf(size.width * .8f, size.height * 1.65f)
                    val topLeft = Offset((size.width - diameter) / 2, size.height - diameter / 2 - 8.dp.toPx())
                    listOf(Color(0xFFFF8F8F), Color(0xFFE0C34D), Green).forEachIndexed { index, color ->
                        drawArc(color, 180f + index * 60f, 56f, false, topLeft, androidx.compose.ui.geometry.Size(diameter, diameter), style = androidx.compose.ui.graphics.drawscope.Stroke(12.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text("Z-Score ${company.altman_z?.score}", color = Green, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(when (company.altman_z?.zone) { "safe" -> "Zona segura"; "grey" -> "Zona gris"; "distress" -> "Zona de riesgo"; else -> "Zona no informada" }, color = Muted)
                }
            }
            Text("< 1,81 Riesgo   ·   1,81–2,99 Gris   ·   > 2,99 Segura", style = MaterialTheme.typography.labelSmall, color = Muted)
            Text("Indicador calculado por el servidor con el balance disponible.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}
