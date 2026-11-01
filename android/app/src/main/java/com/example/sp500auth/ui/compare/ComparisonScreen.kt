package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.util.Resource

private val CompareBackground = Color(0xFF101416)
internal val ComparePanel = Color(0xFF1C2024)
internal val CompareGreen = Color(0xFF36F5A0)

@Composable
fun ComparisonScreen(nav: NavController, initialTicker: String = "", vm: ComparisonViewModel = hiltViewModel()) {
    LaunchedEffect(initialTicker) { vm.initialize(initialTicker) }
    Scaffold(containerColor = CompareBackground) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
                Text("Comparar empresas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Selecciona dos empresas del catálogo", style = MaterialTheme.typography.bodyMedium)
            }
            item { CompanyPicker(CompanySlot.A, vm.selection.a, vm.selection.aName, vm.queryA, vm.suggestionsA, vm::query, vm::select, vm::remove) }
            item { OutlinedButton(onClick = vm::swap, enabled = vm.selection.a.isNotBlank() || vm.selection.b.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Intercambiar A y B") } }
            item { CompanyPicker(CompanySlot.B, vm.selection.b, vm.selection.bName, vm.queryB, vm.suggestionsB, vm::query, vm::select, vm::remove) }
            vm.selectionError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item { HistoryComparisonSection(vm.selection.a, vm.selection.b) }
            item {
                OutlinedTextField(vm.year, vm::changeYear, label = { Text("Ejercicio fiscal") }, singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Button(onClick = vm::compare, enabled = vm.selection.canCompare && vm.result !is Resource.Loading, modifier = Modifier.fillMaxWidth()) { Text("Comparar cinco pilares") }
                if (!vm.selection.canCompare) Text("Selecciona una empresa distinta en cada buscador.")
            }
            when (val result = vm.result) {
                is Resource.Loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Consultando ambas empresas…") }
                is Resource.Error -> item {
                    Text(result.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = vm::compare) { Text("Reintentar comparación") }
                }
                is Resource.Success -> item {
                    Text("${result.successData.a?.ticker} y ${result.successData.b?.ticker} · FY${result.successData.fiscal_year}", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    ComparisonTable(result.successData)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun CompanyPicker(
    slot: CompanySlot, ticker: String, name: String, query: String,
    suggestions: Resource<List<CompanyItem>>, onQuery: (CompanySlot, String) -> Unit,
    onSelect: (CompanySlot, CompanyItem) -> Unit, onRemove: (CompanySlot) -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = ComparePanel)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Empresa ${slot.name}", fontWeight = FontWeight.Bold)
            if (ticker.isNotBlank()) {
                Text(ticker, color = CompareGreen, style = MaterialTheme.typography.titleLarge)
                Text(name)
                TextButton(onClick = { onRemove(slot) }) { Text("Quitar empresa ${slot.name}") }
            }
            OutlinedTextField(value = query, onValueChange = { onQuery(slot, it) }, singleLine = true,
                label = { Text("Buscar para empresa ${slot.name}") }, placeholder = { Text("Nombre o ticker") }, modifier = Modifier.fillMaxWidth())
            when (suggestions) {
                is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                is Resource.Error -> {
                    Text(suggestions.message.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onQuery(slot, query) }) { Text("Reintentar búsqueda ${slot.name}") }
                }
                is Resource.Success -> {
                    if (suggestions.successData.isEmpty()) Text("No encontramos empresas.")
                    suggestions.successData.forEach { company ->
                        TextButton(onClick = { onSelect(slot, company) }, modifier = Modifier.fillMaxWidth()) {
                            Text("${company.ticker.orEmpty()} · ${company.name.orEmpty()}", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}
