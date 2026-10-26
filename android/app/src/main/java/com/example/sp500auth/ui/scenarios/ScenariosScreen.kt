package com.example.sp500auth.ui.scenarios
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.SimulationResponse
import com.example.sp500auth.util.Resource
import java.util.Locale

@Composable
fun ScenariosScreen(nav: NavController,ticker: String,vm: ScenariosViewModel=hiltViewModel()) {
    LaunchedEffect(ticker){vm.load(ticker)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        TextButton(onClick={nav.popBackStack()}){Text("Volver")}
        Text("$ticker · Escenarios a doce meses",style=MaterialTheme.typography.headlineSmall)
        Text("Simulación del servidor, no ganancias garantizadas. P10, P50 y P90 pertenecen a una misma ejecución.")
        when(val state=vm.defaults) {
            is Resource.Loading -> CircularProgressIndicator()
            is Resource.Error -> {Text(state.message.orEmpty());Button(onClick={vm.load(ticker)}){Text("Reintentar servicio de escenarios")}}
            is Resource.Success -> {
                val d=state.successData
                Text("Retorno anual permitido: ${d.min_return!!*100} % a ${d.max_return!!*100} %")
                OutlinedTextField(vm.returnText,{vm.returnText=it},label={Text("Retorno anual supuesto (%)")},singleLine=true)
                if(d.min_return<d.max_return) Slider(value=(vm.returnText.toFloatOrNull() ?: (d.annual_return!!*100).toFloat()).coerceIn((d.min_return*100).toFloat(),(d.max_return*100).toFloat()),
                    onValueChange={vm.returnText=String.format(Locale.US,"%.2f",it)},valueRange=(d.min_return*100).toFloat()..(d.max_return*100).toFloat())
                Text("Volatilidad anual permitida: ${d.min_volatility!!*100} % a ${d.max_volatility!!*100} %")
                OutlinedTextField(vm.volatilityText,{vm.volatilityText=it},label={Text("Volatilidad anual (%)")},singleLine=true)
                if(d.min_volatility<d.max_volatility) Slider(value=(vm.volatilityText.toFloatOrNull() ?: (d.annual_volatility!!*100).toFloat()).coerceIn((d.min_volatility*100).toFloat(),(d.max_volatility*100).toFloat()),
                    onValueChange={vm.volatilityText=String.format(Locale.US,"%.2f",it)},valueRange=(d.min_volatility*100).toFloat()..(d.max_volatility*100).toFloat())
                OutlinedTextField(vm.seedText,{vm.seedText=it},label={Text("Semilla reproducible")},singleLine=true)
                Row{Button(onClick={vm.calculate(ticker)},enabled=vm.result !is Resource.Loading){Text("Recalcular")};TextButton(onClick={vm.reset(ticker)}){Text("Restablecer")}}
            }
            else -> Unit
        }
        when(val state=vm.result) {
            is Resource.Loading -> {CircularProgressIndicator();Text("Calculando los tres escenarios…")}
            is Resource.Error -> Text(state.message.orEmpty(),color=MaterialTheme.colorScheme.error)
            is Resource.Success -> {
                val data=state.successData
                Text("Ejecución: ${data.simulation_id} · Modelo ${data.model_version}")
                Text("${data.start_date} — ${data.end_date} · ${data.currency} · ${data.generated_at}")
                Text("Retorno ${data.parameters?.annual_return} · Volatilidad ${data.parameters?.annual_volatility} · Semilla ${data.parameters?.seed} · Trayectorias ${data.parameters?.paths}")
                Text("Fuentes: ${data.sources?.joinToString()}")
                if(data.history.isNullOrEmpty()) Text("El servidor no entregó histórico para esta ejecución.")
                else {Text("Histórico previo a la proyección");com.example.sp500auth.ui.finance.PriceLine(data.history.orEmpty().mapNotNull{it.price})}
                Row{listOf("p10" to "Pesimista P10","p50" to "Base P50","p90" to "Optimista P90").forEach{(id,label)->
                    Column(Modifier.weight(1f)){Checkbox(checked=id in vm.visible,onCheckedChange={vm.toggle(id)});Text(label)}
                }}
                ScenarioChart(data,vm.visible)
                val last=data.points.orEmpty().lastOrNull()
                Text("Precio final (${data.currency})",style=MaterialTheme.typography.titleMedium)
                Text("Pesimista P10: ${last?.p10} · Base P50: ${last?.p50} · Optimista P90: ${last?.p90}")
                Text("Ocultar una curva no recalcula ni cambia esta tabla.")
            }
            else -> Unit
        }
    }
}

@Composable
fun ScenarioChart(data: SimulationResponse,visible: Set<String>) {
    val points=data.points.orEmpty()
    val all=points.flatMap{listOfNotNull(it.p10,it.p50,it.p90)}
    val colors=mapOf("p10" to Color(0xFFFF7575),"p50" to Color(0xFF4EBBFF),"p90" to Color(0xFF39D98A))
    Canvas(Modifier.fillMaxWidth().height(220.dp).semantics{contentDescription="Proyección P10, P50 y P90 de la misma ejecución, con escala común."}) {
        if(all.isNotEmpty()) {
            val low=all.minOrNull()!!;val span=(all.maxOrNull()!!-low).takeIf{it>0} ?: 1.0
            for(id in listOf("p10","p50","p90")) if(id in visible) {
                val path=Path()
                points.forEachIndexed{i,p -> val value=when(id){"p10"->p.p10;"p50"->p.p50;else->p.p90}!!
                    val x=i*size.width/(points.size-1).coerceAtLeast(1);val y=size.height-((value-low)/span*size.height).toFloat()
                    if(i==0)path.moveTo(x,y) else path.lineTo(x,y)
                }
                drawPath(path,colors[id]!!,style=Stroke(2.dp.toPx()))
            }
        }
    }
}
