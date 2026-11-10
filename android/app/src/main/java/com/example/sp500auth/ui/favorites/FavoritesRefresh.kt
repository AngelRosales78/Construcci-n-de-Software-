package com.example.sp500auth.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable fun PullFavoritesRefresh(atTop:Boolean,enabled:Boolean,refresh:()->Unit,content:@Composable ()->Unit) {
    var distance by remember {mutableStateOf(0f)}
    val currentTop by rememberUpdatedState(atTop);val currentEnabled by rememberUpdatedState(enabled);val action by rememberUpdatedState(refresh)
    val threshold=with(LocalDensity.current){80.dp.toPx()}
    val connection=remember {object:NestedScrollConnection {
        override fun onPostScroll(consumed:Offset,available:Offset,source:NestedScrollSource):Offset {
            if(source==NestedScrollSource.Drag && currentTop && currentEnabled)distance=(distance+available.y).coerceIn(0f,threshold*2)
            return Offset.Zero
        }
        override suspend fun onPostFling(consumed:Velocity,available:Velocity):Velocity {if(distance>=threshold && currentEnabled)action();distance=0f;return Velocity.Zero}
    }}
    Column {if(distance>=threshold) Text("Suelta para actualizar favoritos");Box(Modifier.fillMaxSize().nestedScroll(connection)){content()}}
}
@Composable fun FavoritesSyncStamp(time:Long?,stale:Boolean,problem:String?) {
    time?.let {Text("Última sincronización: ${DateFormat.getDateTimeInstance().format(Date(it))}")}
    if(stale)Text("Copia anterior del servidor. No se confirmó una sincronización nueva.")
    problem?.let {Text(it)}
}
