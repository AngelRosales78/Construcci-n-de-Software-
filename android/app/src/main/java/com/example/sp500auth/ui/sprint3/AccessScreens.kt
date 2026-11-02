package com.example.sp500auth.ui.sprint3

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.example.sp500auth.data.repository.*
import kotlinx.coroutines.delay

@Composable fun AccessObserver(vm: AccessViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle,vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.load() }
        lifecycle.addObserver(observer); onDispose { lifecycle.removeObserver(observer) }
    }
}
@Composable fun ComparisonAccessGate(nav: NavController, vm: AccessViewModel = hiltViewModel(), content: @Composable () -> Unit) {
    AccessObserver(vm)
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(vm.state) { while (true) { delay(1000); tick++ } }
    @Suppress("UNUSED_VARIABLE") val clock = tick
    RemoteContent(vm.state, vm::load) { info ->
        if (canCompare(info)) content()
        else if (accessFresh(info)) PlanPreview({ nav.navigate("plans") }, { nav.popBackStack() })
        else { Text("El permiso venció. Vuelve a verificar tu plan."); TextButton(onClick = vm::load) { Text("Verificar acceso") } }
    }
    if (vm.state is com.example.sp500auth.util.Resource.Error) TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
}
@Composable fun PlansScreen(nav: NavController, vm: AccessViewModel = hiltViewModel()) {
    AccessObserver(vm)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }; Text("Mi plan", style = MaterialTheme.typography.headlineMedium)
        RemoteContent(vm.state, vm::load) { info ->
            Text("Plan: ${info.plan} · ${if (info.active == true) "Activo" else "Inactivo"}")
            Text("Funciones habilitadas: ${info.features.orEmpty().filterNotNull().joinToString()}")
        }
        Text("Pro habilita la comparación avanzada cuando el servidor confirma el acceso. La contratación o cambio de plan corresponde al módulo de suscripciones; todavía no está disponible aquí.")
    }
}
