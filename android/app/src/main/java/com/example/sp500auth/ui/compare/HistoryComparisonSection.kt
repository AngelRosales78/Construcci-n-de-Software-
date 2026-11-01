package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sp500auth.util.Resource

@Composable
fun HistoryComparisonSection(a: String, b: String, vm: HistoryComparisonViewModel = hiltViewModel()) {
    LaunchedEffect(a, b) { vm.load(a, b) }
    if (!validTicker(a) || !validTicker(b) || a == b) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Comparar evolución del precio", style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(historyRanges) { period -> FilterChip(selected = vm.range == period, onClick = { vm.load(a, b, period) }, label = { Text(period) }) }
        }
        when (val state = if (vm.a == a && vm.b == b) vm.state else Resource.Empty()) {
            is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is Resource.Error -> { Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error); TextButton(onClick = { vm.load(a, b) }) { Text("Reintentar histórico comparado") } }
            is Resource.Success -> if (state.successData.a == a && state.successData.b == b) HistoryComparisonChart(state.successData)
            else -> Unit
        }
    }
}
