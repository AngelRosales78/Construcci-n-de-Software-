package com.example.sp500auth.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavController
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.sprint3.RemoteContent
import com.example.sp500auth.util.Resource

@Composable fun TagsObserver(vm:TagsViewModel){val lifecycle=LocalLifecycleOwner.current.lifecycle;DisposableEffect(lifecycle,vm){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)vm.load()};lifecycle.addObserver(observer);onDispose{lifecycle.removeObserver(observer)}}}
@Composable fun FavoritesTagFilter(vm:TagsViewModel,selected:Long?,select:(Long?)->Unit){
    TagsObserver(vm);val snapshot=(vm.state as? Resource.Success)?.successData
    LaunchedEffect(snapshot){if(snapshot?.tags.orEmpty().none{it?.id==selected})select(null)}
    if(snapshot!=null)LazyRow{item{FilterChip(selected==null,{select(null)},label={Text("Todas las listas")})};items(snapshot.tags.orEmpty().filterNotNull()){tag->FilterChip(selected==tag.id,{select(tag.id)},label={Text(tag.name.orEmpty())})}}
    else if(vm.state is Resource.Error)Text("No se pudieron cargar las etiquetas. Se muestran los favoritos sin filtro.")
}
@Composable fun TagsScreen(nav:NavController,favoriteId:Long?,favorites:FavoritesViewModel,vm:TagsViewModel=hiltViewModel()){
    TagsObserver(vm);RefreshFavoritesOnResume(favorites)
    var creating by remember(vm.accountKey){mutableStateOf(false)};var editing by remember(vm.accountKey){mutableStateOf<TagItem?>(null)};var deleting by remember(vm.accountKey){mutableStateOf<TagItem?>(null)}
    var selected by remember(vm.accountKey,favoriteId){mutableStateOf<Set<Long>>(emptySet())}
    val snapshot=(vm.state as? Resource.Success)?.successData
    LaunchedEffect(snapshot,favoriteId){selected=snapshot?.assignments?.singleOrNull{it?.favorite_id==favoriteId}?.tag_ids.orEmpty().filterNotNull().toSet()}
    LaunchedEffect(vm.savedVersion){creating=false;editing=null;deleting=null}
    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        TextButton(onClick={nav.popBackStack()}){Text("Volver")};Text("Mis etiquetas",style=MaterialTheme.typography.headlineMedium)
        RemoteContent(vm.state,vm::load){data->
            Button(onClick={creating=true},enabled=!vm.busy){Text("Nueva etiqueta")}
            data.tags.orEmpty().filterNotNull().forEach{tag->Card{Column(Modifier.padding(12.dp)){
                Text("${tag.name} · ${tag.color}")
                TextButton(onClick={editing=tag},enabled=!vm.busy){Text("Editar ${tag.name}")};TextButton(onClick={deleting=tag},enabled=!vm.busy){Text("Eliminar ${tag.name}")}
                if(favoriteId!=null)Row{Checkbox(tag.id in selected,{checked->selected=if(checked)selected+tag.id!! else selected-tag.id!!},enabled=!vm.busy);Text("Asignar ${tag.name}")}
            }}}
            if(favoriteId!=null){
                val validFavorite=(favorites.visibleState as? Resource.Success)?.successData?.any{it.id==favoriteId}==true
                Button(onClick={vm.assign(favoriteId,selected.toList())},enabled=validFavorite && !vm.busy){Text("Guardar etiquetas del favorito")}
                if(!validFavorite)Text("Actualiza tus favoritos para comprobar esta empresa.")
            }
        }
        vm.message?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }
    if(creating || editing!=null)TagEditor(if(creating)"Nueva etiqueta" else "Editar etiqueta",editing?.name.orEmpty(),editing?.color ?: "#36F5A0",vm.busy,vm.message,{name,color->vm.edit(editing?.id,name,color)},{creating=false;editing=null})
    deleting?.let{tag->AlertDialog(onDismissRequest={if(!vm.busy)deleting=null},title={Text("Eliminar etiqueta")},text={Text("Eliminar ${tag.name} quita sus asignaciones, pero conserva las empresas favoritas.")},confirmButton={TextButton(onClick={vm.delete(tag.id!!)},enabled=!vm.busy){Text("Eliminar")}},dismissButton={TextButton(onClick={deleting=null},enabled=!vm.busy){Text("Cancelar")}})}
}
