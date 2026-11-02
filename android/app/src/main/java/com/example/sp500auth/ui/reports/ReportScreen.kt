package com.example.sp500auth.ui.reports

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.sprint3.RemoteContent
import com.example.sp500auth.util.Resource
import java.time.LocalDate
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.*

@Composable fun ReportScreen(nav: NavController, a: String, b: String?, initialYear: String, initialRange: String, vm: ReportsViewModel) {
    val context = LocalContext.current
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(vm,lifecycle){val observer=LifecycleEventObserver {_,event->when(event){Lifecycle.Event.ON_RESUME->vm.resume();Lifecycle.Event.ON_PAUSE->vm.pause();else->Unit}};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer);vm.pause()}}
    var year by rememberSaveable(a,b) { mutableStateOf(initialYear.ifBlank { (LocalDate.now().year-1).toString() }) }
    var pendingHash by rememberSaveable { mutableStateOf<String?>(null) }
    var charts by rememberSaveable(a,b) { mutableStateOf(false) }
    var range by rememberSaveable(a,b) { mutableStateOf(initialRange.takeIf { it in com.example.sp500auth.ui.compare.historyRanges } ?: "1Y") }
    var includeAnalysis by rememberSaveable(a,b) { mutableStateOf(false) }
    var level by rememberSaveable(a,b) { mutableStateOf("simple") }
    var analysis by remember { mutableStateOf<ReportAnalysis?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { vm.save(it,pendingHash) }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
        Text(if(b==null) "Exportar ficha financiera" else "Exportar comparación Pro", style = MaterialTheme.typography.headlineMedium)
        Text("Empresa: $a${b?.let { " y $it" }.orEmpty()}")
        vm.request?.let{Text("Seguimiento actual: ${it.a}${it.b?.let {b->" y $b"}.orEmpty()} · FY${it.fiscal_year}. Se conserva hasta terminar o cancelar.")}
        OutlinedTextField(year, { year=it.take(4) }, label = { Text("Ejercicio fiscal") },enabled=vm.request==null)
        ChartOptions(charts,range,vm.request==null,{charts=it},{range=it})
        ReportAnalysisSelector(a,b,year,level,includeAnalysis,vm.request==null,{includeAnalysis=it},{level=it},{analysis=it})
        Button(onClick = { year.toIntOrNull()?.let { vm.generate(ReportRequest(a=a,b=b,kind=if(b==null) "company" else "comparison", fiscal_year=it, range=range,include_charts=charts,include_analysis=includeAnalysis,analysis_level=level,analysis_version=analysis?.version,analysis_origin=analysis?.origin)) } }, enabled = vm.request==null && year.toIntOrNull() in 2000..2100) { Text("Generar PDF") }
        if (vm.state !is Resource.Empty) RemoteContent(vm.state, vm::refresh) { job ->
            Text("Reporte actual: ${job.request?.a} · FY${job.request?.fiscal_year} · ${job.status}")
            ReportProgress(job,vm.acceptedMillis,vm.readyMillis,vm.downloadMillis,true,vm::cancel)
            if (job.request?.include_charts == true) Text("Gráficos solicitados: precios ${job.request.range} y radar · formato anunciado: ${job.chart_format ?: "pendiente"}")
            if (job.request?.include_analysis==true) Text("Análisis incluido: ${job.analysis_origin ?: "pendiente"} · ${job.analysis_version ?: "pendiente"}")
            if (job.status in setOf("queued","running")) TextButton(onClick = vm::refresh) { Text("Consultar estado") }
            if (job.status == "ready") Button(onClick = vm::download,enabled=!vm.downloading) { Text("Descargar PDF") }
            if(job.status in setOf("ready","error","cancelled")) TextButton(onClick=vm::prepareNew){Text("Nuevo reporte")}
            job.message?.let { Text(it) }
        }
        vm.message?.let { Text(it) }
        if(vm.downloading)TextButton(onClick=vm::cancelDownload){Text("Detener descarga")}
        PdfFileActions(vm.payload != null,vm.savedUri != null,vm.saving,
            onSave = { vm.payload?.let { pendingHash=it.sha256; launcher.launch("${it.request.a}${it.request.b?.let {b -> "_$b"}.orEmpty()}_${it.request.fiscal_year}.pdf") } },
            onOpen = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } } },
            onShare = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Compartir reporte")) } } })
    }
}
