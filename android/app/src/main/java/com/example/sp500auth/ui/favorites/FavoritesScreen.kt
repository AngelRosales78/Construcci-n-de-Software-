package com.example.sp500auth.ui.favorites

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource

@Composable
fun RefreshFavoritesOnResume(vm: FavoritesViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refresh() }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.refresh()
        onDispose { lifecycle.removeObserver(observer) }
    }
    RemovalConfirmation(vm)
}

@Composable
fun FavoritesStatus(vm: FavoritesViewModel) {
    when (val state = vm.visibleState) {
        is Resource.Error -> { Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error); TextButton(onClick = vm::refresh) { Text("Reintentar favoritos") } }
        is Resource.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        else -> Unit
    }
    vm.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
}

@Composable
fun FavoriteAction(ticker: String, vm: FavoritesViewModel) {
    val available = vm.visibleState is Resource.Success
    val marked = vm.item(ticker) != null
    OutlinedButton(onClick = { if (marked) vm.requestRemoval(ticker) else vm.add(ticker) }, enabled = available && !vm.busy) {
        Text(when { !available -> "Favoritos no disponibles"; vm.busy -> "Actualizando favoritos…"; marked -> "★ Quitar de favoritos"; else -> "☆ Guardar en favoritos" })
    }
}

@Composable
fun FavoritesScreen(nav: NavController, vm: FavoritesViewModel) {
    RefreshFavoritesOnResume(vm)
    Scaffold { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = { nav.popBackStack() }) { Text("Volver") }
                Text("Mis favoritos", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Tu lista personal guardada en el servidor.")
                FavoritesStatus(vm)
                TextButton(onClick = vm::refresh, enabled = !vm.busy) { Text("Actualizar lista") }
            }
            val state = vm.visibleState
            if (state is Resource.Success) {
                item { Text("${state.successData.size} empresas favoritas") }
                if (state.successData.isEmpty()) item { Text("Aún no tienes favoritos. Puedes añadir empresas desde Explorar o su ficha.") }
                items(state.successData, key = { it.id ?: it.ticker.orEmpty() }) { favorite ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(favorite.ticker.orEmpty(), fontWeight = FontWeight.Bold)
                            Text(favorite.name ?: favorite.ticker.orEmpty())
                            TextButton(onClick = { nav.navigate("company/${Uri.encode(favorite.ticker.orEmpty())}") }) { Text("Ver ficha") }
                            TextButton(onClick = { vm.requestRemoval(favorite.ticker.orEmpty()) }, enabled = !vm.busy) { Text("Quitar ${favorite.ticker} de favoritos") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemovalConfirmation(vm: FavoritesViewModel) {
    val selected = vm.removalCandidate ?: return
    AlertDialog(onDismissRequest = vm::cancelRemoval,
        title = { Text("Quitar de favoritos") },
        text = { Column { Text("¿Quieres quitar ${selected.name ?: selected.ticker} (${selected.ticker}) de tu lista?"); vm.message?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(onClick = vm::confirmRemoval, enabled = !vm.busy) { Text(if (vm.busy) "Quitando…" else "Quitar de favoritos") } },
        dismissButton = { TextButton(onClick = vm::cancelRemoval, enabled = !vm.busy) { Text("Cancelar") } })
}
