package com.example.sp500auth.ui.sprint3

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun PlanPreview(onPlans: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TextButton(onClick = onBack) { Text("Volver") }
        Text("Comparador Pro", style = MaterialTheme.typography.headlineMedium)
        Text("Vista previa ilustrativa. No contiene datos privados ni resultados de empresas.")
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
            Text("Empresa A     Empresa B")
            Text("Cinco pilares, evolución y conclusión comparativa")
            Text("Contenido reservado al plan Pro")
        } }
        Button(onClick = onPlans) { Text("Ver plan Pro") }
    }
}
