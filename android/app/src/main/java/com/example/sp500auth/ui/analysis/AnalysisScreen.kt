package com.example.sp500auth.ui.analysis
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sp500auth.ui.common.SourceLink
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource
import com.example.sp500auth.ui.pillars.pillarLabels

@Composable
fun AnalysisScreen(nav: NavController,ticker: String,year: Int,vm: AnalysisViewModel=hiltViewModel()) {
    LaunchedEffect(ticker,year){vm.load(ticker,year)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        TextButton(onClick={nav.popBackStack()}){Text("Volver")}
        Text("$ticker · Explicación de IA FY$year",style=MaterialTheme.typography.headlineSmall)
        Row{listOf("simple" to "Sencillo","technical" to "Técnico").forEach{(level,label)->
            TextButton(onClick={vm.load(ticker,year,level)},enabled=vm.state !is Resource.Loading){Text(if(vm.requestedLevel==level)"✓ $label" else label)}
        }}
        if(vm.state is Resource.Loading){CircularProgressIndicator();Text("Solicitando análisis validado…")}
        if(vm.state is Resource.Error){Text(vm.state.message.orEmpty(),color=MaterialTheme.colorScheme.error);Button(onClick={vm.load(ticker,year)}){Text("Reintentar")}}
        val data=vm.lastValid
        if(data!=null) {
            if(data.level!=vm.requestedLevel) Text("Se conserva el último análisis válido (${if(data.level=="simple")"sencillo" else "técnico"}).")
            Text("Versión de datos: ${data.metrics_version} · Datos a ${data.data_as_of}")
            Text("Periodo: ${data.period?.start} — ${data.period?.end}")
            Text("Validación: ${data.validation?.status} · ${data.validation?.checked_at} · Generado ${data.generated_at}")
            Text("Resumen",style=MaterialTheme.typography.titleLarge)
            data.summary.orEmpty().forEach{Text(it)}
            data.pillars.orEmpty().forEach{section ->
                Text(pillarLabels[section.pillar_id] ?: "Pilar",style=MaterialTheme.typography.titleLarge)
                Text(section.text.orEmpty());Text("Métricas: ${section.metric_ids?.joinToString()}")
            }
            Text("Fortalezas",style=MaterialTheme.typography.titleLarge)
            if(data.strengths.isNullOrEmpty())Text("No hay evidencia suficiente para enumerar fortalezas.")
            data.strengths.orEmpty().forEach{Text("• ${it.text}");Text("Métricas: ${it.metric_ids?.joinToString()}")}
            Text("Riesgos",style=MaterialTheme.typography.titleLarge)
            if(data.risks.isNullOrEmpty())Text("No hay riesgos adicionales sustentados en este análisis.")
            data.risks.orEmpty().forEach{Text("• ${it.text}");Text("Métricas: ${it.metric_ids?.joinToString()}")}
            Text("Fuentes",style=MaterialTheme.typography.titleLarge)
            data.sources.orEmpty().forEach{SourceLink(it.url,it.label ?: it.url)}
        } else if(vm.state !is Resource.Loading) Text("Análisis no disponible. La app no genera una respuesta de ejemplo.")
    }
}
