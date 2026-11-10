package com.example.sp500auth.ui.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

@Composable fun TagEditor(title:String,initialName:String,initialColor:String,busy:Boolean,message:String?,save:(String,String)->Unit,cancel:()->Unit,retry:(()->Unit)?=null) {
    var name by remember(initialName){mutableStateOf(initialName)};var color by remember(initialColor){mutableStateOf(initialColor)}
    AlertDialog(onDismissRequest={if(!busy)cancel()},title={Text(title)},text={Column {
        OutlinedTextField(name,{name=it.take(40)},label={Text("Nombre de etiqueta")},enabled=!busy)
        listOf("#36F5A0" to "Verde","#60CDF9" to "Azul","#FFD166" to "Amarillo","#B399FF" to "Violeta").forEach {(value,label)->
            FilterChip(selected=color==value,onClick={color=value},enabled=!busy,label={Text(label)})
        }
        message?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        if(message!=null && retry!=null)TextButton(onClick=retry,enabled=!busy){Text("Reconectar antes de guardar")}
        Text("Al quitar una etiqueta se conservan las empresas favoritas.")
    }},confirmButton={TextButton(onClick={save(name,color)},enabled=!busy && name.isNotBlank()){Text("Guardar")}},dismissButton={TextButton(onClick=cancel,enabled=!busy){Text("Cancelar")}})
}
