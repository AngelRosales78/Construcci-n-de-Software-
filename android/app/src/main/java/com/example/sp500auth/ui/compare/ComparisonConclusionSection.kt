package com.example.sp500auth.ui.compare

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sp500auth.data.api.models.ComparisonResponse
import com.example.sp500auth.ui.sprint3.RemoteContent
import com.example.sp500auth.util.Resource

@Composable
fun ComparisonConclusionSection(
    a: String,
    b: String,
    year: String,
    comparison: ComparisonResponse?,
    onLoadComparison: () -> Unit,
    vm: ConclusionViewModel = hiltViewModel()
) {
    LaunchedEffect(a,b,year) { vm.selection(a,b,year) }
    val state = vm.state(a,b,year)
    val rows = remember(comparison) {
        comparison?.takeIf { it.status != "comparison_service_unavailable" }?.let(::comparisonRows).orEmpty()
    }
    if (state is Resource.Empty || state is Resource.Error) Button(onClick = {
        onLoadComparison()
        vm.load(a, b, year)
    }, enabled = validTicker(a) && validTicker(b) && a != b, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Consultar conclusión comparativa")
    }
    if (state !is Resource.Empty) {
        RemoteContent(state, { vm.load(a, b, year) }) { ComparisonConclusionCard(it, rows) }
    }
}
