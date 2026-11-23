package com.example.sp500auth.ui.operations

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.gson.JsonObject
import java.util.UUID

private fun sectorLabel(id:String)=mapOf("technology" to "Tecnología","healthcare" to "Salud","financials" to "Finanzas","consumer_discretionary" to "Consumo discrecional","consumer_staples" to "Consumo básico","industrials" to "Industria","energy" to "Energía","materials" to "Materiales","utilities" to "Servicios públicos","real_estate" to "Inmobiliario","communication_services" to "Comunicaciones")[id] ?: id

@Composable
fun AdminUsersScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    var query by rememberSaveable{mutableStateOf("")};var plan by rememberSaveable{mutableStateOf("all")};var state by rememberSaveable{mutableStateOf("all")}
    var pending by remember{mutableStateOf<JsonObject?>(null)};var changes by remember{mutableStateOf(JsonObject())};var reason by remember{mutableStateOf("")}
    fun search(){vm.load("admin/users/?q=${Uri.encode(query)}&plan=$plan&state=$state")}
    LaunchedEffect(Unit){search()}
    OperationShell(nav,"Usuarios y acceso",vm) {
        item{OutlinedTextField(query,{query=it.take(120)},label={Text("Buscar usuario")},modifier=Modifier.fillMaxWidth());TextButton(onClick={search()}){Text("Buscar")}
            Row{listOf("all" to "Todos","free" to "Free","pro" to "Pro").forEach{(id,label)->FilterChip(selected=plan==id,onClick={plan=id;search()},label={Text(label)})}}
            Row{listOf("all" to "Todos","active" to "Activos","blocked" to "Bloqueados").forEach{(id,label)->FilterChip(selected=state==id,onClick={state=id;search()},label={Text(label)})}}
        }
        if(vm.data?.rows("results").isNullOrEmpty()&&!vm.loading)item{Text("No hay usuarios para estos filtros.")}
        items(vm.data?.rows("results").orEmpty()){row->OperationCard {
            Text(row.text("username"),style=MaterialTheme.typography.titleLarge);Text(row.text("email"))
            Text("Rol: "+(if(row.text("role")=="admin")"Administrador" else "Usuario")+" · Plan "+row.text("plan").uppercase())
            Text(if(row.flag("is_active"))"Cuenta activa" else "Cuenta bloqueada")
            Row{TextButton(enabled=!vm.busy,onClick={pending=row;changes=body("role" to if(row.text("role")=="admin")"user" else "admin")}){Text("Cambiar rol")}
                TextButton(enabled=!vm.busy,onClick={pending=row;changes=body("plan" to if(row.text("plan")=="pro")"free" else "pro")}){Text("Cambiar plan")}}
            TextButton(enabled=!vm.busy,onClick={pending=row;reason="";changes=body("is_active" to !row.flag("is_active"))}){Text(if(row.flag("is_active"))"Bloquear cuenta" else "Desbloquear cuenta")}
        }}
        item{PageActions(vm)}
    }
    pending?.let{row->AlertDialog(onDismissRequest={if(!vm.busy)pending=null},title={Text("Confirmar cambio de acceso")},text={Column{Text("Cuenta: "+row.text("username"));Text("El cambio se registra y se aplica en el servidor.")
        if(changes.has("is_active")&&!changes.flag("is_active"))OutlinedTextField(reason,{reason=it.take(240)},label={Text("Motivo del bloqueo")})
        if(vm.error.isNotBlank())Text(vm.error,color=MaterialTheme.colorScheme.error)}},confirmButton={TextButton(enabled=!vm.busy && (!changes.has("is_active")||changes.flag("is_active")||reason.isNotBlank()),onClick={
            val payload=changes.deepCopy();if(payload.has("is_active")&&!payload.flag("is_active"))payload.addProperty("reason",reason)
            vm.mutate("admin/users/${row.text("id")}/","PATCH",payload){pending=null}
        }){Text("Confirmar")}},dismissButton={TextButton(enabled=!vm.busy,onClick={pending=null}){Text("Cancelar")}})}
}

@Composable
fun AdminCompaniesScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    var query by rememberSaveable{mutableStateOf("")};var scope by rememberSaveable{mutableStateOf("all")}
    var editing by remember{mutableStateOf<JsonObject?>(null)};var description by remember{mutableStateOf("")};var website by remember{mutableStateOf("")};var sector by remember{mutableStateOf("technology")}
    var ticker by rememberSaveable{mutableStateOf("")};var adding by rememberSaveable{mutableStateOf(false)}
    var verification by remember{mutableStateOf<JsonObject?>(null)}
    var activating by remember{mutableStateOf<JsonObject?>(null)}
    var requestId by rememberSaveable{mutableStateOf(UUID.randomUUID().toString())}
    val sectors=listOf("technology","healthcare","financials","consumer_discretionary","consumer_staples","industrials","energy","materials","utilities","real_estate","communication_services")
    fun search(){vm.load("admin/companies/?q=${Uri.encode(query)}&scope=$scope")}
    LaunchedEffect(Unit){search()}
    OperationShell(nav,"Empresas y datos",vm) {
        item{OutlinedTextField(query,{query=it.take(120)},label={Text("Buscar empresa")},modifier=Modifier.fillMaxWidth());Row{TextButton(onClick={search()}){Text("Buscar")};FilterChip(selected=scope=="pilot",onClick={scope=if(scope=="pilot")"all" else "pilot";search()},label={Text("Selección inicial")})};TextButton(onClick={adding=!adding;verification=null}){Text("Añadir empresa")}}
        if(adding)item{OperationCard {
            Text("Verificar una empresa",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(ticker,{ticker=it.uppercase().take(10);verification=null;requestId=UUID.randomUUID().toString()},label={Text("Símbolo de mercado")})
            Text("Sector: ${sectorLabel(sector)}");TextButton(onClick={sector=sectors[(sectors.indexOf(sector)+1)%sectors.size]}){Text("Cambiar sector")}
            Button(enabled=!vm.busy&&ticker.isNotBlank(),onClick={vm.mutate("admin/companies/verify/","POST",body("ticker" to ticker,"sector" to sector,"request_id" to requestId)){verification=it.obj("operation")}}){Text("Consultar fuente")}
            verification?.let{operation->Text(operation.obj("result").text("name"));Text("Estado: "+operation.text("status"));Text("Precios disponibles: "+operation.obj("result").text("price_count"))
                if(operation.text("status")=="running")TextButton(enabled=!vm.busy,onClick={vm.mutate("admin/market-operations/${operation.text("id")}/","GET"){verification=it.obj("operation")}}){Text("Consultar resultado")}
                if(operation.text("status")=="ready")Button(enabled=!vm.busy,onClick={vm.mutate("admin/companies/","POST",body("verification_id" to operation.text("id"))){adding=false;verification=null;ticker=""}}){Text("Confirmar alta")}}
        }}
        if(vm.data?.rows("results").isNullOrEmpty()&&!vm.loading)item{Text("No hay empresas para este filtro.")}
        items(vm.data?.rows("results").orEmpty()){row->OperationCard {
            Text(row.text("ticker")+" · "+row.text("name"),style=MaterialTheme.typography.titleMedium)
            Text(if(row.flag("is_active"))"Visible en el catálogo" else "Oculta en las búsquedas")
            val coverage=row.obj("coverage")
            Text("Indicadores de resumen: "+coverage.text("summary_fields")+" de "+coverage.text("summary_total"))
            var sourcesOpen by remember(row.text("id")){mutableStateOf(false)}
            TextButton(onClick={sourcesOpen=!sourcesOpen}){Text(if(sourcesOpen)"Ocultar fuentes" else "Consultar cobertura y fuentes")}
            if(sourcesOpen) {
                Text("La selección inicial no garantiza que todos los indicadores estén disponibles.")
                val sources=coverage.getAsJsonArray("sources")
                if(sources==null||sources.size()==0)Text("Sin fuentes importadas.")
                sources?.forEach{source->Text(source.asString)}
                coverage.obj("field_sources").entrySet().forEach{entry->
                    val source=entry.value.asJsonObject
                    Text(entry.key+" · "+source.text("as_of")+" · "+source.text("method"))
                    Text(source.text("source"))
                }
            }
            val operation=row.obj("last_update")
            if(operation.text("id").isNotBlank()) {
                Text("Última actualización: "+when(operation.text("status")){"running"->"En proceso";"succeeded"->"Completada";"failed"->"No completada";else->"Consultada"})
                Text(operation.text("message"))
                Text(operation.text("finished_at").ifBlank{operation.text("started_at")})
                if(operation.text("status")=="running") {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    TextButton(onClick=vm::reload){Text("Consultar actualización")}
                }
            }
            Text("Balance: "+coverage.text("summary_as_of").ifBlank{"Sin balance disponible"});Text("Precios: "+coverage.text("price_as_of").ifBlank{"Sin precios disponibles"})
            Row{TextButton(enabled=!vm.busy,onClick={activating=row}){Text(if(row.flag("is_active"))"Desactivar" else "Activar")}
                TextButton(enabled=!vm.busy,onClick={editing=row;description=row.text("description");website=row.text("website_url");sector=row.text("sector")}){Text("Editar ficha")}}
            TextButton(enabled=!vm.busy&&operation.text("status")!="running",onClick={vm.mutate("admin/companies/${row.text("id")}/refresh/","POST",body("request_id" to UUID.randomUUID().toString()))}){Text("Actualizar precios")}
            TextButton(onClick={nav.navigate("company/${Uri.encode(row.text("ticker"))}")}){Text("Ver ficha")}
        }}
        item{PageActions(vm)}
    }
    activating?.let{row->AlertDialog(onDismissRequest={if(!vm.busy)activating=null},title={Text(if(row.flag("is_active"))"¿Ocultar esta empresa?" else "¿Activar esta empresa?")},text={Column{Text(row.text("ticker")+" · "+row.text("name"));Text("Los datos guardados se conservan. El cambio se aplica a las búsquedas de todos los clientes.");if(vm.error.isNotBlank())Text(vm.error)}},confirmButton={TextButton(enabled=!vm.busy,onClick={vm.mutate("admin/companies/${row.text("id")}/","PATCH",body("is_active" to !row.flag("is_active"))){activating=null}}){Text("Confirmar")}},dismissButton={TextButton(enabled=!vm.busy,onClick={activating=null}){Text("Cancelar")}})}
    editing?.let{row->AlertDialog(onDismissRequest={if(!vm.busy)editing=null},title={Text("Editar "+row.text("ticker"))},text={Column {
        OutlinedTextField(description,{description=it.take(3000)},label={Text("Descripción")},maxLines=4)
        OutlinedTextField(website,{website=it.take(500)},label={Text("Sitio web HTTPS")})
        Text("Sector: ${sectorLabel(sector)}");TextButton(onClick={sector=sectors[(sectors.indexOf(sector)+1)%sectors.size]}){Text("Cambiar sector")}
        if(vm.error.isNotBlank())Text(vm.error,color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton(enabled=!vm.busy,onClick={vm.mutate("admin/companies/${row.text("id")}/","PATCH",body("description" to description,"website_url" to website,"sector" to sector)){editing=null}}){Text("Guardar ficha")}},dismissButton={TextButton(enabled=!vm.busy,onClick={editing=null}){Text("Cancelar")}})}
}

@Composable
fun MonitoringScreen(nav:NavController,vm:OperationsViewModel=hiltViewModel()) {
    var section by rememberSaveable{mutableStateOf("ai")}
    var month by rememberSaveable{mutableStateOf(java.time.LocalDate.now().toString().take(7))}
    var start by rememberSaveable{mutableStateOf(java.time.LocalDate.now().withDayOfMonth(1).toString())}
    var end by rememberSaveable{mutableStateOf(java.time.LocalDate.now().toString())}
    var level by rememberSaveable{mutableStateOf("all")}
    var detail by remember{mutableStateOf<JsonObject?>(null)}
    var budget by remember{mutableStateOf<JsonObject?>(null)};var limit by remember{mutableStateOf("")}
    fun load(){val query=when(section){"ai"->"?start=$start&end=$end";"subscriptions"->"?month=$month";"errors"->"?level=$level";else->""};vm.load("admin/monitoring/$section/$query")}
    LaunchedEffect(section){load()}
    OperationShell(nav,"Uso y operación",vm) {
        item { Column { listOf("ai" to "Inteligencia artificial","plans" to "Planes","budgets" to "Presupuestos","subscriptions" to "Suscripciones","errors" to "Registros").forEach{(id,label)->TextButton(onClick={section=id}){Text(if(section==id)"• $label" else label)}} } }
        if(section=="ai")item { OutlinedTextField(start,{start=it},label={Text("Desde AAAA-MM-DD")});OutlinedTextField(end,{end=it},label={Text("Hasta AAAA-MM-DD")});TextButton(onClick={load()}){Text("Aplicar período")} }
        if(section=="subscriptions")item { OutlinedTextField(month,{month=it},label={Text("Mes AAAA-MM")});TextButton(onClick={load()}){Text("Consultar mes")} }
        if(section=="errors")item { Row{listOf("all" to "Todos","warning" to "Advertencias","error" to "Errores").forEach{(id,label)->FilterChip(selected=level==id,onClick={level=id;load()},label={Text(label)})}} }
        vm.data?.let{data->
            if(data.text("explanation").isNotBlank())item{Text(data.text("explanation"))}
            when(section) {
                "ai"->{item{OperationCard {Text("Llamadas observadas: "+data.text("calls"),fontWeight=FontWeight.Bold);Text("Sin tarifa calculable: "+data.text("unpriced_calls"));if(data.rows("estimated_costs").isEmpty())Text("Coste estimado: No calculado") else data.rows("estimated_costs").forEach{cost->Text("Coste estimado: "+cost.text("currency")+" "+cost.text("amount"))}}}
                    items(data.rows("breakdown")){row->OperationCard{Text(row.text("model"));Text("Llamadas: "+row.text("calls"));Text("Tokens de entrada: "+row.text("input_tokens").ifBlank{"No informado"});Text("Tokens de salida: "+row.text("output_tokens").ifBlank{"No informado"});Text("Sin medición completa: "+row.text("unreported_tokens"))}}}
                "plans"->{item{Text("Cuentas: "+data.text("total"));Text(data.text("rule"))};items(data.rows("results")){row->OperationCard{Text(row.text("plan").uppercase()+": "+row.text("count")+" cuentas · "+row.text("percentage")+" %")}}}
                "budgets"->items(data.rows("results")){row->OperationCard{Text(if(row.text("provider")=="local_ai")"Motor de análisis local" else "Datos de mercado",fontWeight=FontWeight.Bold);Text(row.text("used")+" de "+row.text("limit")+" llamadas · "+row.text("percentage")+" %");LinearProgressIndicator(progress=(row.text("percentage").toFloatOrNull() ?: 0f).coerceIn(0f,100f)/100,modifier=Modifier.fillMaxWidth());Text(when(row.text("status")){"warning"->"Consumo alto";"exhausted"->"Límite alcanzado";else->"Disponible"});Text("Reinicio: "+row.text("resets_at"));TextButton(onClick={budget=row;limit=row.text("limit")}){Text("Ajustar límite")}}}
                "subscriptions"->{item{Text("Planes incluidos: "+data.text("plans_included"))};if(data.rows("results").isEmpty())item{Text("Importe estimado: 0,00")};items(data.rows("results")){row->OperationCard{Text("Valor del mes: "+row.text("currency")+" "+row.text("amount"))}}}
                "errors"->{if(data.rows("results").isEmpty())item{Text("Sin registros para este filtro.")};items(data.rows("results")){row->OperationCard{Text(if(row.text("level")=="warning")"Advertencia" else "Error",fontWeight=FontWeight.Bold);Text(row.text("created_at"));Text(row.text("summary"));Text(row.text("component"));TextButton(enabled=!vm.busy,onClick={vm.mutate("admin/monitoring/errors/${row.text("id")}/","GET"){detail=it}}){Text("Ver detalle")}}};item{PageActions(vm)}}
            }
        }
    }
    detail?.let{row->AlertDialog(onDismissRequest={detail=null},title={Text("Detalle del registro")},text={Column{Text(row.text("created_at"));Text(row.text("summary"));Text("Componente: "+row.text("component"));Text("Código: "+row.text("code"));row.obj("context").entrySet().forEach{entry->Text(entry.key+": "+entry.value.asString)}}},confirmButton={TextButton(onClick={detail=null}){Text("Cerrar")}})}
    budget?.let{row->AlertDialog(onDismissRequest={if(!vm.busy)budget=null},title={Text("Ajustar presupuesto")},text={Column{OutlinedTextField(limit,{limit=it.filter(Char::isDigit)},label={Text("Llamadas por día")});Text("Zona: "+row.text("timezone"));if(vm.error.isNotBlank())Text(vm.error)}},confirmButton={TextButton(enabled=!vm.busy && (limit.toIntOrNull() ?: 0)>0,onClick={vm.mutate("admin/monitoring/budgets/${row.text("provider")}/","PATCH",body("version" to row.text("version").toInt(),"daily_limit" to limit.toInt(),"reset_timezone" to row.text("timezone"))){budget=null}}){Text("Guardar")}},dismissButton={TextButton(onClick={budget=null}){Text("Cancelar")}})}
}
