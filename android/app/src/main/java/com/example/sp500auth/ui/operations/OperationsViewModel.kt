package com.example.sp500auth.ui.operations

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.repository.OperationsRepository
import com.google.gson.JsonObject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

@HiltViewModel
class OperationsViewModel @Inject constructor(private val repository:OperationsRepository):ViewModel() {
    var data by mutableStateOf<JsonObject?>(null); private set
    var loading by mutableStateOf(false); private set
    var busy by mutableStateOf(false); private set
    var error by mutableStateOf(""); private set
    var message by mutableStateOf(""); private set
    var extra by mutableStateOf<JsonObject?>(null); private set
    var plans by mutableStateOf<JsonObject?>(null); private set
    var avatar by mutableStateOf<ByteArray?>(null); private set
    private var current=""
    private var sequence=0
    fun load(path:String) {
        current=path
        val id=++sequence; val owner=repository.owner()
        data=null; loading=true; error=""
        viewModelScope.launch {
            try { val result=repository.request(path); if(id==sequence && repository.owner()==owner) {
                data=result
                if(path=="user/subscription/history/"){val value=repository.request("plans/");if(id==sequence&&repository.owner()==owner)plans=value}
                if(path=="auth/profile/" && result.text("avatar_version").isNotBlank()){
                    val value=repository.picture();if(id==sequence&&repository.owner()==owner)avatar=value
                }
            } }
            catch(e:CancellationException){throw e}
            catch(e:Exception){if(id==sequence && repository.owner()==owner)error=e.message ?: "No se pudo consultar."}
            finally{if(id==sequence)loading=false}
        }
    }
    fun reload() { if(current.isNotBlank())load(current) }
    fun mutate(path:String,method:String,body:JsonObject=JsonObject(),onSuccess:(JsonObject)->Unit={}) {
        if(busy)return
        val owner=repository.owner(); busy=true;error="";message=""
        viewModelScope.launch {
            try {
                val result=repository.request(path,method,body)
                if(repository.owner()!=owner)return@launch
                message=if(method=="GET")"Información consultada." else "Cambios guardados.";extra=result;onSuccess(result)
                reload()
            } catch(e:CancellationException){throw e}
            catch(e:Exception){if(repository.owner()==owner)error=e.message ?: "No se pudo confirmar el cambio."}
            finally {if(repository.owner()==owner)busy=false}
        }
    }
    fun avatar(part:MultipartBody.Part) {
        if(busy)return
        val owner=repository.owner();busy=true;error=""
        viewModelScope.launch {
            try {repository.uploadAvatar(part);if(repository.owner()==owner){message="Foto actualizada.";reload()}}
            catch(e:CancellationException){throw e}
            catch(_:Exception){if(repository.owner()==owner)error="No se pudo guardar la foto. Revisa el formato y el tamaño."}
            finally{if(repository.owner()==owner)busy=false}
        }
    }
}

fun JsonObject.text(key:String)=get(key)?.takeUnless{it.isJsonNull}?.let{if(it.isJsonPrimitive)it.asString else ""}.orEmpty()
fun JsonObject.flag(key:String)=get(key)?.takeUnless{it.isJsonNull}?.runCatching{asBoolean}?.getOrDefault(false) ?: false
fun JsonObject.obj(key:String)=get(key)?.takeIf{it.isJsonObject}?.asJsonObject ?: JsonObject()
fun JsonObject.rows(key:String)=get(key)?.takeIf{it.isJsonArray}?.asJsonArray?.filter{it.isJsonObject}?.map{it.asJsonObject}.orEmpty()
fun body(vararg values:Pair<String,Any?>)=JsonObject().apply {
    values.forEach{(key,value)->when(value){is Boolean->addProperty(key,value);is Number->addProperty(key,value);is com.google.gson.JsonElement->add(key,value);null->add(key,null);else->addProperty(key,value.toString())}}
}
