package com.example.sp500auth.ui.operations

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.gson.JsonArray
import kotlinx.coroutines.delay

@Composable
fun NotificationsScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit){vm.load("notifications/")}
    LaunchedEffect(lifecycle){lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED){while(true){delay(30_000);vm.reload()}}}
    OperationShell(nav,"Mis avisos",vm) {
        item{Row{TextButton(onClick={nav.navigate("notification-preferences")}){Text("Preferencias")};TextButton(onClick=vm::reload){Text("Actualizar")}}
            Text("Sin leer: "+(vm.data?.text("unread_count") ?: "—"))
            TextButton(enabled=!vm.busy && vm.data!=null,onClick={val data=vm.data ?: return@TextButton;vm.mutate("notifications/mark-read/","POST",body("all" to true,"through_id" to data.text("read_through").toLong()))}){Text("Marcar todos como leídos")}}
        if(vm.data?.rows("results").isNullOrEmpty()&&!vm.loading)item{OperationCard{Text("Estás al día",style=MaterialTheme.typography.titleLarge);Text("Aquí aparecerán las novedades de tus empresas favoritas.")}}
        items(vm.data?.rows("results").orEmpty(),key={it.text("id")}){row->OperationCard {
            Text(if(row.text("read_at").isBlank())"Sin leer" else "Leído",color=MaterialTheme.colorScheme.primary)
            Text(row.text("title"),fontWeight=FontWeight.Bold);Text(row.text("message"));Text(row.text("created_at"))
            Row{TextButton(onClick={nav.navigate("company/${android.net.Uri.encode(row.text("ticker"))}")}){Text("Ver empresa")}
                if(row.text("read_at").isBlank())TextButton(enabled=!vm.busy,onClick={val ids=JsonArray().apply{add(row.text("id").toLong())};vm.mutate("notifications/mark-read/","POST",body("ids" to ids))}){Text("Marcar leído")}}
        }}
        item{PageActions(vm)}
    }
}

@Composable
fun NotificationPreferencesScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    LaunchedEffect(Unit){vm.load("user/notification-preferences/")}
    var rating by remember{mutableStateOf(true)};var web by remember{mutableStateOf(true)};var email by remember{mutableStateOf(false)};var push by remember{mutableStateOf(false)}
    val context=LocalContext.current
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed->if(allowed)push=true;com.example.sp500auth.data.push.PushRegistration.sync(context)}
    LaunchedEffect(vm.data){vm.data?.let{rating=it.flag("rating_changes");web=it.obj("channels").flag("web");email=it.obj("channels").flag("email");push=it.obj("channels").flag("push")}}
    OperationShell(nav,"Preferencias de avisos",vm) {
        item{OperationCard {
            Text("Elige cómo recibir novedades de tus empresas favoritas.")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Cambios de perfil financiero",modifier=Modifier.weight(1f));Switch(checked=rating,onCheckedChange={rating=it},enabled=!vm.busy&&vm.data!=null)}
            val available=vm.data?.obj("available_channels")
            listOf("web" to "En la web","email" to "Por correo","push" to "En este teléfono").forEach{(id,label)->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,modifier=Modifier.weight(1f));Switch(checked=when(id){"web"->web;"email"->email;else->push},enabled=!vm.busy&&available?.flag(id)==true&&(id!="push"||com.example.sp500auth.data.push.PushRegistration.configured()),onCheckedChange={value->when(id){"web"->web=value;"email"->email=value;else->{if(value&&Build.VERSION.SDK_INT>=33)permission.launch(Manifest.permission.POST_NOTIFICATIONS) else push=value}}})}
                if(id=="push"&&!com.example.sp500auth.data.push.PushRegistration.configured())Text("Los avisos en este teléfono requieren completar la configuración del servicio.")
                else if(available?.flag(id)!=true)Text(vm.data?.obj("channel_messages")?.text(id).orEmpty())
            }
            Button(enabled=!vm.busy&&vm.data!=null,onClick={val version=vm.data?.text("version")?.toLongOrNull() ?: return@Button;vm.mutate("user/notification-preferences/","PUT",body("version" to version,"rating_changes" to rating,"channels" to body("web" to web,"email" to email,"push" to push))){com.example.sp500auth.data.push.PushRegistration.sync(context)}}){Text(if(vm.busy)"Guardando…" else "Guardar preferencias")}
        }}
    }
}
