package com.example.sp500auth.ui.favorites

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.sp500auth.util.Resource
import com.example.sp500auth.ui.common.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.ui.Alignment

@Composable
fun RefreshFavoritesOnResume(vm: FavoritesViewModel) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, vm) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) vm.refresh() }
        lifecycle.addObserver(observer)
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
    FavoritesSyncStamp(vm.lastSynced,vm.isStale,if(vm.visibleState is Resource.Error) null else vm.syncError)
}

@Composable
fun FavoriteAction(ticker: String, vm: FavoritesViewModel) {
    val available = vm.visibleState is Resource.Success
    val marked = vm.item(ticker) != null
    val gold = androidx.compose.ui.graphics.Color(0xFFFFC857)
    OutlinedButton(onClick = { if (marked) vm.requestRemoval(ticker) else vm.add(ticker) }, enabled = available && !vm.busy && !vm.isStale,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (marked) gold else MaterialTheme.colorScheme.onSurface)) {
        Icon(if (marked) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = if (marked) gold else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(when { !available -> "Favoritos no disponibles"; vm.busy -> "Actualizando…"; marked -> "Quitar de favoritos"; else -> "Guardar en favoritos" })
    }
}

@Composable
fun FavoritesScreen(nav: NavController, vm: FavoritesViewModel,tags:TagsViewModel?=null) {
    val tagModel=tags
    var selectedTag by remember(tagModel?.accountKey){mutableStateOf<Long?>(null)}
    RefreshFavoritesOnResume(vm)
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle,vm){lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED){while(isActive){vm.refresh();delay(2000)}}}
    val listState=rememberLazyListState()
    Scaffold(containerColor = AppColors.Background, topBar = {
        AppTopBar("Mis favoritos", "Tu lista personal guardada en el servidor", onBack = { nav.popBackStack() }, actions = {
            IconButton(onClick = vm::refresh, enabled = !vm.busy) { Icon(Icons.Default.Refresh, contentDescription = "Actualizar lista") }
            IconButton(onClick = { nav.navigate("tags") }) { Icon(Icons.Default.Label, contentDescription = "Organizar etiquetas") }
        })
    }) { padding ->
        PullFavoritesRefresh(listState.firstVisibleItemIndex==0 && listState.firstVisibleItemScrollOffset==0,!vm.busy,vm::refresh) {
        LazyColumn(Modifier.fillMaxSize().padding(padding),state=listState,contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FavoritesStatus(vm)
                if(tagModel!=null)FavoritesTagFilter(tagModel,selectedTag){selectedTag=it}
            }
            val state = vm.visibleState
            if (state is Resource.Success) {
                item { Text("${state.successData.size} ${if (state.successData.size == 1) "empresa favorita" else "empresas favoritas"}",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (state.successData.isEmpty()) item {
                    AppCard {
                        Icon(Icons.Default.StarBorder, null, tint = AppColors.Muted, modifier = Modifier.size(36.dp))
                        Text("Aún no tienes favoritos. Puedes añadir empresas desde Explorar o su ficha.", color = AppColors.Muted)
                    }
                }
                val snapshot=(tagModel?.state as? Resource.Success)?.successData
                val filtered=state.successData.filter {favorite->selectedTag==null || snapshot==null || snapshot.assignments?.singleOrNull{it?.favorite_id==favorite.id}?.tag_ids.orEmpty().contains(selectedTag)}
                if(selectedTag!=null)item{Text("Mostrando ${filtered.size} de ${state.successData.size}")}
                items(filtered, key = { it.id ?: it.ticker.orEmpty() }) { favorite ->
                    val open = { nav.navigate("company/${Uri.encode(favorite.ticker.orEmpty())}") }
                    Card(Modifier.fillMaxWidth().clickable { open() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = AppColors.Panel)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CompanyLogo(favorite.ticker.orEmpty(), size = 48.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(favorite.name ?: favorite.ticker.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(favorite.ticker.orEmpty(), color = AppColors.Green, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                }
                                Icon(Icons.Default.Star, null, tint = androidx.compose.ui.graphics.Color(0xFFFFC857))
                            }
                            val tagNames = (tagModel?.state as? Resource.Success)?.successData?.let { snap ->
                                val ids = snap.assignments?.singleOrNull { it?.favorite_id == favorite.id }?.tag_ids.orEmpty()
                                snap.tags.orEmpty().filterNotNull().filter { it.id in ids }
                            }.orEmpty()
                            if (tagNames.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                tagNames.take(3).forEach { tag ->
                                    StatusPill(tag.name.orEmpty(), runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(tag.color)) }.getOrDefault(AppColors.Blue))
                                }
                            }
                            HorizontalDivider(color = AppColors.Line)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { open() }) { Text("Ver ficha") }
                                TextButton(onClick={nav.navigate("tags?favorite=${favorite.id}")}){ Icon(Icons.Default.Label, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Etiquetas de ${favorite.ticker}") }
                                Spacer(Modifier.weight(1f))
                            }
                            TextButton(onClick = { vm.requestRemoval(favorite.ticker.orEmpty()) }, enabled = !vm.busy && !vm.isStale,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Quitar ${favorite.ticker} de favoritos") }
                        }
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
