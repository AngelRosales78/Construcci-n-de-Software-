package com.example.sp500auth.ui.operations

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import java.util.UUID

@Composable
fun OperationShell(nav:NavController,title:String,vm:OperationsViewModel,content:LazyListScope.()->Unit) {
    Scaffold { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { TextButton(onClick={nav.popBackStack()}){Text("Volver")};Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold) }
        if(vm.loading)item{LinearProgressIndicator(Modifier.fillMaxWidth());Text("Consultando…")}
        if(vm.error.isNotBlank())item{Text(vm.error,color=MaterialTheme.colorScheme.error);TextButton(onClick=vm::reload){Text("Volver a consultar")}}
        if(vm.message.isNotBlank())item{Text(vm.message,color=MaterialTheme.colorScheme.primary)}
        content()
    } }
}

@Composable
fun OperationCard(content:@Composable ColumnScope.()->Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content) }
}

fun nextPath(value:String):String {
    val uri=Uri.parse(value)
    return uri.path.orEmpty().removePrefix("/api/v1/")+(uri.encodedQuery?.let{"?$it"} ?: "")
}

@Composable
fun PageActions(vm:OperationsViewModel) {
    val data=vm.data ?: return
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        TextButton(enabled=data.text("previous").isNotBlank() && !vm.loading,onClick={vm.load(nextPath(data.text("previous")))}){Text("Anterior")}
        TextButton(enabled=data.text("next").isNotBlank() && !vm.loading,onClick={vm.load(nextPath(data.text("next")))}){Text("Siguiente")}
    }
}

@Composable
fun AccountHome(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    LaunchedEffect(Unit){vm.load("auth/profile/")}
    OperationShell(nav,"Tu espacio",vm) {
        item { OperationCard {
            vm.avatar?.let{bytes->remember(bytes){android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)}?.let{Image(it.asImageBitmap(),contentDescription="Tu foto de perfil",modifier=Modifier.size(80.dp))}}
            Text(vm.data?.text("username").orEmpty(),style=MaterialTheme.typography.titleLarge)
            TextButton(onClick={nav.navigate("account/edit")}){Text("Editar perfil y foto")}
            TextButton(onClick={nav.navigate("subscription")}){Text("Mi suscripción")}
            TextButton(onClick={nav.navigate("notifications")}){Text("Mis avisos")}
            TextButton(onClick={nav.navigate("notification-preferences")}){Text("Preferencias de avisos")}
        } }
        if(vm.data?.text("role")=="admin") item { OperationCard {
            Text("Administración",style=MaterialTheme.typography.titleLarge)
            TextButton(onClick={nav.navigate("admin/users")}){Text("Usuarios y acceso")}
            TextButton(onClick={nav.navigate("admin/companies")}){Text("Empresas y datos")}
            TextButton(onClick={nav.navigate("admin/monitoring")}){Text("Uso y operación")}
        } }
    }
}

@Composable
fun EditAccount(nav:NavController,onDeleted:()->Unit,vm:OperationsViewModel=hiltViewModel()) {
    LaunchedEffect(Unit){vm.load("auth/profile/")}
    var first by rememberSaveable{mutableStateOf("")};var last by rememberSaveable{mutableStateOf("")};var phone by rememberSaveable{mutableStateOf("")}
    var password by remember{mutableStateOf("")};var deleting by remember{mutableStateOf(false)}
    var photoError by remember{mutableStateOf("")};var preview by remember{mutableStateOf<android.graphics.Bitmap?>(null)}
    val context=LocalContext.current
    LaunchedEffect(vm.data){vm.data?.let{first=it.text("first_name");last=it.text("last_name");phone=it.text("phone_number")}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) try {
            val bytes=context.contentResolver.openInputStream(uri)?.use{com.example.sp500auth.data.repository.boundedBytes(it,2*1024*1024)} ?: byteArrayOf()
            val options=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
            android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
            if(bytes.size>=2*1024*1024 || options.outWidth<=0 || options.outWidth.toLong()*options.outHeight>10_000_000) throw IllegalArgumentException()
            preview=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,android.graphics.BitmapFactory.Options().apply{inSampleSize=4})
            photoError="";vm.avatar(MultipartBody.Part.createFormData("avatar","avatar.jpg",bytes.toRequestBody("image/jpeg".toMediaType())))
        } catch(_:Exception){photoError="Selecciona una imagen válida de menos de 2 MB y hasta 10 megapíxeles."}
    }
    OperationShell(nav,"Editar perfil",vm) {
        item { OperationCard {
            preview?.let{Image(it.asImageBitmap(),contentDescription="Vista previa de tu foto",modifier=Modifier.size(100.dp))}
            TextButton(enabled=!vm.busy,onClick={picker.launch("image/*")}){Text("Cambiar foto")}
            if(photoError.isNotBlank())Text(photoError,color=MaterialTheme.colorScheme.error)
            OutlinedTextField(first,{first=it.take(150)},label={Text("Nombres")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(last,{last=it.take(150)},label={Text("Apellidos")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(phone,{phone=it.take(20)},label={Text("Teléfono")},modifier=Modifier.fillMaxWidth())
            Button(enabled=!vm.busy && vm.data!=null,onClick={vm.mutate("auth/profile/","PATCH",body("first_name" to first,"last_name" to last,"phone_number" to phone))}){Text(if(vm.busy)"Guardando…" else "Guardar perfil")}
        } }
        item { OperationCard { Text("Eliminar cuenta",style=MaterialTheme.typography.titleMedium);Text("Esta acción elimina tus favoritos, preferencias y datos personales. Requiere tu contraseña.");TextButton(onClick={deleting=true}){Text("Eliminar mi cuenta")} } }
    }
    if(deleting)AlertDialog(onDismissRequest={if(!vm.busy){deleting=false;password=""}},title={Text("¿Eliminar tu cuenta?")},text={Column{Text("La acción es permanente.");OutlinedTextField(password,{password=it},label={Text("Contraseña actual")},visualTransformation=PasswordVisualTransformation());if(vm.error.isNotBlank())Text(vm.error)}},
        confirmButton={TextButton(enabled=password.isNotBlank()&&!vm.busy,onClick={vm.mutate("auth/account/","DELETE",body("password" to password)){password="";deleting=false;onDeleted()}}){Text("Eliminar definitivamente")}},
        dismissButton={TextButton(enabled=!vm.busy,onClick={deleting=false;password=""}){Text("Cancelar")}})
}

@Composable
fun SubscriptionScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    LaunchedEffect(Unit){vm.load("user/subscription/history/")}
    var confirmation by remember{mutableStateOf("")}
    var requestId by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())}
    val subscription=vm.data?.obj("subscription")
    OperationShell(nav,"Mi suscripción",vm) {
        item{OperationCard {
            Text(if(subscription?.text("effective_plan")=="pro")"Plan Pro" else "Plan Free",style=MaterialTheme.typography.headlineSmall)
            Text("Vigencia: "+(subscription?.text("expires_at")?.takeIf{it.isNotBlank()} ?: "Sin fecha de vencimiento"))
            Text("Free incluye fichas, favoritos, análisis individual y PDF. Pro añade el comparador y los reportes de dos empresas.")
            if(subscription?.text("effective_plan")!="pro")Button(enabled=!vm.busy&&vm.plans!=null,onClick={confirmation="activate"}){Text("Elegir Pro")}
            else if(subscription.text("cancelled_at").isBlank())TextButton(enabled=!vm.busy,onClick={confirmation="cancel"}){Text("Cancelar renovación")}
            else Text("Renovación cancelada. Conservas el acceso durante la vigencia indicada.")
        }}
        item{Text("Historial de suscripciones",style=MaterialTheme.typography.titleLarge)}
        if(vm.data?.rows("receipts").isNullOrEmpty())item{Text("Aún no tienes operaciones registradas.")}
        items(vm.data?.rows("receipts").orEmpty()){row->OperationCard {
            Text(if(row.text("outcome")=="approved")"Activación registrada" else "Operación no aprobada",fontWeight=FontWeight.Bold)
            Text(row.text("created_at"));Text(row.text("currency")+" "+row.text("amount"));Text("Vigencia: "+row.text("starts_at")+" — "+row.text("ends_at"))
            Text("Referencia: "+row.text("id"));Text("Correo: "+when(row.text("email_status")){"sent"->"Enviado";"pending"->"Pendiente";else->"Registrado"})
        }}
    }
    if(confirmation.isNotBlank())AlertDialog(onDismissRequest={if(!vm.busy)confirmation=""},title={Text(if(confirmation=="activate")"Activar acceso Pro" else "Cancelar renovación")},text={Text(if(confirmation=="activate")"Activa Pro por ${vm.plans?.text("duration_days")} días. Valor de referencia: ${vm.plans?.text("currency")} ${vm.plans?.text("amount")}. En esta versión no se realiza un cargo a una tarjeta." else "Conservarás Pro hasta el final de su vigencia.")},
        confirmButton={TextButton(enabled=!vm.busy,onClick={if(confirmation=="activate")vm.mutate("user/subscription/checkout/","POST",body("request_id" to requestId,"outcome" to "approved")){confirmation="";requestId=UUID.randomUUID().toString()}
            else vm.mutate("user/subscription/cancel/","POST"){confirmation=""}}){Text("Confirmar")}},dismissButton={TextButton(enabled=!vm.busy,onClick={confirmation=""}){Text("Volver")}})
}
