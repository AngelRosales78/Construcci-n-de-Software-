package com.example.sp500auth.ui.reports

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.common.*
import com.example.sp500auth.util.Resource
import java.time.LocalDate

@Composable fun ReportScreen(nav: NavController, a: String, b: String?, initialYear: String, initialRange: String, vm: ReportsViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> when (event) { Lifecycle.Event.ON_RESUME -> vm.resume(); Lifecycle.Event.ON_PAUSE -> vm.pause(); else -> Unit } }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer); vm.pause() }
    }
    var year by rememberSaveable(a, b) { mutableStateOf(initialYear.ifBlank { (LocalDate.now().year - 1).toString() }) }
    var pendingHash by rememberSaveable { mutableStateOf<String?>(null) }
    var charts by rememberSaveable(a, b) { mutableStateOf(false) }
    var range by rememberSaveable(a, b) { mutableStateOf(initialRange.takeIf { it in com.example.sp500auth.ui.compare.historyRanges } ?: "1Y") }
    var includeAnalysis by rememberSaveable(a, b) { mutableStateOf(false) }
    var level by rememberSaveable(a, b) { mutableStateOf("simple") }
    var analysis by remember { mutableStateOf<ReportAnalysis?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { vm.save(it, pendingHash) }
    val editable = vm.request == null
    val comparison = b != null
    Scaffold(containerColor = AppColors.Background, topBar = {
        AppTopBar(if (comparison) "Exportar comparación Pro" else "Exportar ficha financiera", "Reporte PDF", onBack = { nav.popBackStack() })
    }) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ReportHeader(a, b)
            vm.request?.let {
                Surface(color = AppColors.Blue.copy(alpha = .12f), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Sync, null, tint = AppColors.Blue)
                        Text("Seguimiento actual: ${it.a}${it.b?.let { b -> " y $b" }.orEmpty()} · FY${it.fiscal_year}. Se conserva hasta terminar o cancelar.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            StepCard(1, "Ejercicio fiscal", "Año de los estados financieros del reporte") {
                val current = LocalDate.now().year
                val years = (current - 1 downTo current - 5).map(Int::toString)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(if (year in years || year.isBlank()) years else listOf(year) + years) { option ->
                        FilterChip(selected = year == option, onClick = { year = option }, enabled = editable, label = { Text("FY$option") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppColors.Green.copy(alpha = .18f), selectedLabelColor = AppColors.Green))
                    }
                }
            }

            StepCard(2, "Contenido del PDF", "Elige qué secciones incluir además de la ficha") {
                ChartOptions(charts, range, editable, { charts = it }, { range = it })
                HorizontalDivider(color = AppColors.Line)
                ReportAnalysisSelector(a, b, year, level, includeAnalysis, editable, { includeAnalysis = it }, { level = it }, { analysis = it })
            }

            StepCard(3, "Generar y descargar", "El servidor arma el PDF; luego lo guardas en tu teléfono") {
                Summary(year, charts, range, includeAnalysis, level, comparison)
                Button(onClick = {
                    year.toIntOrNull()?.let {
                        vm.generate(ReportRequest(a = a, b = b, kind = if (comparison) "comparison" else "company", fiscal_year = it, range = range,
                            include_charts = charts, include_analysis = includeAnalysis, analysis_level = level,
                            analysis_version = analysis?.version, analysis_origin = analysis?.origin))
                    }
                }, enabled = editable && year.toIntOrNull() in 2000..2100, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Green, contentColor = Color.Black)) {
                    Icon(Icons.Default.PictureAsPdf, null); Spacer(Modifier.width(8.dp)); Text("Generar PDF", fontWeight = FontWeight.Bold)
                }
            }

            if (vm.state !is Resource.Empty) {
                when (val state = vm.state) {
                    is Resource.Loading -> AppCard { LinearProgressIndicator(Modifier.fillMaxWidth(), color = AppColors.Green); Text("Enviando la solicitud…", color = AppColors.Muted) }
                    is Resource.Error -> AppCard {
                        Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = vm::refresh) { Text("Reintentar") }
                    }
                    is Resource.Success -> {
                        val job = state.successData
                        AppCard {
                            Text("Reporte actual: ${job.request?.a}${job.request?.b?.let { " y $it" }.orEmpty()} · FY${job.request?.fiscal_year}",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            ReportProgress(job, vm.acceptedMillis, vm.readyMillis, vm.downloadMillis, true, vm::cancel)
                            if (job.request?.include_charts == true) DetailLine(Icons.Default.ShowChart, "Gráficos: precios ${job.request.range} y radar · ${job.chart_format ?: "formato pendiente"}")
                            if (job.request?.include_analysis == true) DetailLine(Icons.Default.AutoAwesome, "Análisis: ${job.analysis_origin ?: "pendiente"} · ${job.analysis_version ?: "pendiente"}")
                            job.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (job.status in setOf("queued", "running")) OutlinedButton(onClick = vm::refresh, modifier = Modifier.weight(1f)) { Text("Consultar estado") }
                                if (job.status in setOf("ready", "error", "cancelled")) OutlinedButton(onClick = vm::prepareNew, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Nuevo reporte")
                                }
                            }
                            if (job.status == "ready") Button(onClick = vm::download, enabled = !vm.downloading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                if (vm.downloading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else { Icon(Icons.Default.Download, null); Spacer(Modifier.width(8.dp)); Text("Descargar PDF") }
                            }
                        }
                    }
                    else -> Unit
                }
            }
            vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (vm.state is Resource.Error && vm.canPrepareNew) TextButton(onClick = vm::prepareNew) { Text("Cambiar datos de la solicitud rechazada") }
            if (vm.downloading) TextButton(onClick = vm::cancelDownload) { Text("Detener descarga") }
            PdfFileActions(vm.payload != null, vm.savedUri != null, vm.saving,
                fileName = vm.payload?.let { "${it.request.a}${it.request.b?.let { b -> "_$b" }.orEmpty()}_${it.request.fiscal_year}.pdf" },
                onSave = { vm.payload?.let { pendingHash = it.sha256; launcher.launch("${it.request.a}${it.request.b?.let { b -> "_$b" }.orEmpty()}_${it.request.fiscal_year}.pdf") } },
                onOpen = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }.onFailure { vm.openFailure() } } },
                onShare = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Compartir reporte")) }.onFailure { vm.openFailure() } } })
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ReportHeader(a: String, b: String?) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompanyLogo(a, size = 52.dp)
            if (b != null) { Icon(Icons.Default.CompareArrows, null, tint = AppColors.Muted); CompanyLogo(b, size = 52.dp, accent = AppColors.Blue) }
            Column(Modifier.weight(1f)) {
                Text(if (b == null) a else "$a vs $b", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(if (b == null) "Ficha técnica, métricas y pilares" else "Comparación horizontal de hasta dos páginas",
                    style = MaterialTheme.typography.bodySmall, color = AppColors.Muted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("PDF vectorial", AppColors.Green)
            StatusPill(if (b == null) "Ficha" else "Pro", if (b == null) AppColors.Blue else AppColors.Amber)
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(Modifier.size(28.dp), shape = CircleShape, color = AppColors.Green.copy(alpha = .16f)) {
                Box(contentAlignment = Alignment.Center) { Text("$number", color = AppColors.Green, fontWeight = FontWeight.Bold) }
            }
            AppSectionTitle(title, subtitle)
        }
        content()
    }
}

@Composable
private fun Summary(year: String, charts: Boolean, range: String, analysis: Boolean, level: String, comparison: Boolean) {
    Surface(color = AppColors.Inner, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DetailLine(Icons.Default.CalendarMonth, "Ejercicio FY$year")
            DetailLine(Icons.Default.Description, if (comparison) "Tabla comparativa de cinco pilares" else "Ficha, métricas fundamentales y pilares")
            DetailLine(Icons.Default.ShowChart, if (charts) "Gráfico de precios $range y radar" else "Sin gráficos", charts)
            DetailLine(Icons.Default.AutoAwesome, if (analysis) "Análisis ${if (level == "technical") "técnico" else "sencillo"} con fuentes" else "Sin análisis", analysis)
        }
    }
}

@Composable
internal fun DetailLine(icon: ImageVector, text: String, active: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = if (active) AppColors.Green else AppColors.Muted, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = if (active) MaterialTheme.colorScheme.onSurface else AppColors.Muted)
    }
}
