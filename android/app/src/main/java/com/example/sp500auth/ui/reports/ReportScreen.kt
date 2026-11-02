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

@Composable fun ReportScreen(nav: NavController, a: String, b: String?, initialYear: String, initialRange: String, vm: ReportsViewModel) {
    val context = LocalContext.current
    var year by rememberSaveable(a,b) { mutableStateOf(initialYear.ifBlank { (LocalDate.now().year-1).toString() }) }
    var pendingHash by rememberSaveable { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { vm.save(it,pendingHash) }
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
        Text("Exportar ficha financiera", style = MaterialTheme.typography.headlineMedium)
        Text("Empresa: $a${b?.let { " y $it" }.orEmpty()}")
        OutlinedTextField(year, { year=it.take(4) }, label = { Text("Ejercicio fiscal") })
        Button(onClick = { year.toIntOrNull()?.let { vm.generate(ReportRequest(a=a,b=b,kind=if(b==null) "company" else "comparison", fiscal_year=it, range=initialRange)) } }, enabled = vm.state !is Resource.Loading) { Text("Generar PDF") }
        if (vm.state !is Resource.Empty) RemoteContent(vm.state, vm::refresh) { job ->
            Text("Reporte actual: ${job.request?.a} · FY${job.request?.fiscal_year} · ${job.status}")
            if (job.status in setOf("queued","running")) TextButton(onClick = vm::refresh) { Text("Consultar estado") }
            if (job.status == "ready") Button(onClick = vm::download) { Text("Descargar PDF") }
            job.message?.let { Text(it) }
        }
        vm.message?.let { Text(it) }
        PdfFileActions(vm.payload != null,vm.savedUri != null,vm.saving,
            onSave = { vm.payload?.let { pendingHash=it.sha256; launcher.launch("${it.request.a}_${it.request.fiscal_year}.pdf") } },
            onOpen = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) } } },
            onShare = { vm.savedUri?.let { uri -> runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Compartir reporte")) } } })
    }
}
