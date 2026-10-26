package com.example.sp500auth.ui.finance
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.ui.auth.AuthViewModel
import com.example.sp500auth.util.Resource
import java.util.Locale

@Composable
fun CompanySearchScreen(nav: NavController, auth: AuthViewModel, vm: CompanyViewModel=hiltViewModel()) {
    var query by rememberSaveable{mutableStateOf("")};var page by rememberSaveable{mutableStateOf(1)}
    LaunchedEffect(Unit){vm.search(query,page)}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item {
            Text("S&P 500 · Empresas",style=MaterialTheme.typography.headlineSmall)
            Row { TextButton(onClick={nav.navigate("profile")}){Text("Mi perfil")};TextButton(onClick={auth.logout()}){Text("Cerrar sesión")} }
            OutlinedTextField(query,{query=it.take(200)},label={Text("Empresa o símbolo")},modifier=Modifier.fillMaxWidth())
            Button(onClick={page=1;vm.search(query,page)}){Text("Buscar en servidor")}
            TextButton(onClick={
                val ticker=query.trim().uppercase(Locale.ROOT)
                if(ticker.matches(Regex("[A-Z0-9][A-Z0-9.-]{0,14}"))) nav.navigate("company/${Uri.encode(ticker)}")
            }){Text("Abrir símbolo escrito")}
        }
        when(val state=vm.companies) {
            is Resource.Loading -> item{CircularProgressIndicator()}
            is Resource.Error -> item{Text(state.message.orEmpty());Button(onClick={vm.search(query,page)}){Text("Reintentar")}}
            is Resource.Success -> {
                val data=state.successData
                item{Text("${data.count} empresas · Página $page")}
                if(data.results.isNullOrEmpty()) item{Text("No se encontraron empresas.")}
                items(data.results.orEmpty()){company ->
                    OutlinedButton(onClick={company.ticker?.let{nav.navigate("company/${Uri.encode(it)}")}},modifier=Modifier.fillMaxWidth()) {
                        Column{Text("${company.ticker.orEmpty()} · ${company.name.orEmpty()}");Text(company.sector_display ?: company.sector.orEmpty())}
                    }
                }
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(enabled=data.previous!=null,onClick={page--;vm.search(query,page)}){Text("Anterior")}
                    Button(enabled=data.next!=null,onClick={page++;vm.search(query,page)}){Text("Siguiente")}
                }}
            }
            else -> Unit
        }
    }
}

@Composable
fun CompanyDetailScreen(nav: NavController,ticker: String,vm: CompanyViewModel=hiltViewModel()) {
    var range by rememberSaveable{mutableStateOf("1Y")}
    LaunchedEffect(ticker){vm.load(ticker)}
    LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item{TextButton(onClick={nav.popBackStack()}){Text("Volver a empresas")};Text(ticker,style=MaterialTheme.typography.headlineLarge)}
        when(val state=vm.summary) {
            is Resource.Loading -> item{CircularProgressIndicator()}
            is Resource.Error -> item{Text(state.message.orEmpty());Button(onClick={vm.load(ticker)}){Text("Reintentar")}}
            is Resource.Success -> item {
                val company=state.successData
                Text(company.name.orEmpty(),style=MaterialTheme.typography.titleLarge)
                Text(company.sector_display.orEmpty())
                Text("Datos financieros: ${company.as_of ?: "Fecha no disponible"} · ${company.source ?: "Sin fuente"}")
                if(company.is_demo==true) Text("Ficha con datos de demostración o heredados sin verificar.")
                Text("Capitalización: ${company.market_cap ?: "No disponible"} · PER: ${company.per ?: "No disponible"}")
                Text("Altman Z: ${company.altman_z?.score ?: "No disponible"} · ${company.altman_z?.zone ?: "Sin zona"}")
                company.quote?.let{Text("Cierre ajustado: ${it.adjusted_close} ${it.currency} · ${it.date} · ${it.source}")}
                Button(onClick={nav.navigate("finance/${Uri.encode(ticker)}/all")}){Text("Cinco pilares y puntaje")}
                TextButton(onClick={nav.navigate("scenarios/${Uri.encode(ticker)}")}){Text("Escenarios")}
                TextButton(onClick={nav.navigate("analysis/${Uri.encode(ticker)}/2024")}){Text("Explicación de IA")}
            }
            else -> Unit
        }
        item{Text("Histórico de cierres ajustados",style=MaterialTheme.typography.titleMedium)
            Row{listOf("1M","6M","1Y","5Y").forEach{period ->TextButton(onClick={range=period;vm.history(ticker,period)}){Text(period)}}}
        }
        when(val state=vm.prices) {
            is Resource.Loading -> item{CircularProgressIndicator()}
            is Resource.Error -> item{Text(state.message.orEmpty());TextButton(onClick={vm.history(ticker,range)}){Text("Reintentar histórico")}}
            is Resource.Success -> item{
                val data=state.successData
                Text("Moneda: ${data.currencies?.joinToString() ?: "No informada"} · Fuentes: ${data.sources?.joinToString() ?: "No informadas"}")
                if(data.is_demo==true) Text("Serie de demostración")
                val values=data.history.orEmpty().mapNotNull{it.price?.takeIf{v->v.isFinite()}}
                if(values.isEmpty()) Text("No hay precios para este periodo.")
                else { PriceLine(values);Text("${data.history?.firstOrNull()?.date} — ${data.history?.lastOrNull()?.date} · ${values.size} cierres") }
            }
            else -> Unit
        }
    }
}
