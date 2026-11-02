package com.example.sp500auth.ui.reports

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject
import com.google.gson.Gson

@HiltViewModel class ReportsViewModel internal constructor(private val source: ReportSource, private val context: Context,private val saved:SavedStateHandle) : ViewModel() {
    @Inject constructor(repository:ReportRepository,@ApplicationContext context:Context,saved:SavedStateHandle):this(repository as ReportSource,context,saved)
    private var owner: String? = saved["report_owner"]
    private var task: Job? = null
    private var polling:Job?=null;private var active=false;private var started=System.nanoTime()
    private var current by mutableStateOf<ReportRequest?>(runCatching {Gson().fromJson(saved.get<String>("report_request"),ReportRequest::class.java)}.getOrNull()?.takeIf {runCatching {validReportRequest(it)}.getOrDefault(false)})
    private var last:ReportJob?=null
    var acceptedMillis by mutableStateOf<Long?>(null);private set
    var readyMillis by mutableStateOf<Long?>(null);private set
    var downloadMillis by mutableStateOf<Long?>(null);private set
    var downloading by mutableStateOf(false);private set
    private var data by mutableStateOf<Resource<ReportJob>>(Resource.Empty())
    private var pdf by mutableStateOf<PdfPayload?>(null)
    private var document by mutableStateOf<Uri?>(null)
    private var notice by mutableStateOf<String?>(null)
    var message:String? get()=notice.takeIf {owner==source.owner()}; private set(value){notice=value}
    var saving by mutableStateOf(false); private set
    val state: Resource<ReportJob> get() = if (owner == source.owner()) data else Resource.Empty()
    val payload get() = pdf.takeIf { owner != null && owner == source.owner() }
    val savedUri get() = document.takeIf { owner != null && owner == source.owner() }
    val request get()=current.takeIf {owner!=null && owner==source.owner()}
    fun reset() { task?.cancel();polling?.cancel(); owner = source.owner(); data = Resource.Empty(); pdf = null; document = null; message = null; saving = false;downloading=false;current=null;last=null;acceptedMillis=null;readyMillis=null;downloadMillis=null;saved["report_request"]=null;saved["report_id"]=null;saved["report_owner"]=owner }
    fun prepareNew(){if(last?.status in setOf("ready","error","cancelled")) reset() else message="Recupera o cancela el trabajo actual antes de crear otro."}
    fun generate(request: ReportRequest) {
        if(owner!=source.owner()) reset()
        if(current!=null) {message="Ya existe una solicitud. Continúa su seguimiento para evitar duplicados.";return}
        if(!validReportRequest(request)) {owner=source.owner();data=Resource.Error("Revisa los datos de exportación.");return}
        owner=source.owner();current=request;started=System.nanoTime();saved["report_request"]=Gson().toJson(request);saved["report_owner"]=owner
        submit()
    }
    private fun submit() {
        val request=request ?: return
        if(task?.isActive==true) return
        val account = owner; data = Resource.Loading()
        task = viewModelScope.launch {
            val result = source.submit(request); coroutineContext.ensureActive()
            if (account == source.owner()) {acceptedMillis=(System.nanoTime()-started)/1_000_000;accept(result);monitor()}
        }
    }
    private fun accept(value:Resource<ReportJob>){data=value;if(value is Resource.Success){last=value.successData;saved["report_id"]=last?.id;if(last?.status=="ready" && readyMillis==null)readyMillis=(System.nanoTime()-started)/1_000_000}}
    fun resume(){active=true;if(owner!=source.owner()){reset();return};if(current!=null) refresh()}
    fun pause(){active=false;polling?.cancel()}
    private fun monitor(){
        polling?.cancel();if(!active || last?.status !in setOf("queued","running"))return
        val account=owner
        polling=viewModelScope.launch {while(active && account==source.owner() && last?.status in setOf("queued","running")){
            delay(1500);val job=last ?: break;val request=request ?: break;val value=source.status(job.id!!,request);coroutineContext.ensureActive()
            if(account!=source.owner())break;accept(value);if(value is Resource.Error)break
        }}
    }
    fun refresh() {
        if(owner!=source.owner()){reset();return}
        if(task?.isActive==true)return
        val request=request ?: return;val id=last?.id ?: saved.get<String>("report_id")
        if(id==null){submit();return};polling?.cancel();val account=owner
        task=viewModelScope.launch {val result=source.status(id,request);coroutineContext.ensureActive();if(source.owner()==account){accept(result);monitor()}}
    }
    fun cancel(){
        val job=last ?: return;if(task?.isActive==true || owner!=source.owner())return
        polling?.cancel();val account=owner
        task=viewModelScope.launch {val result=source.cancel(job);coroutineContext.ensureActive();if(account==source.owner()){accept(result);message=if((result as? Resource.Success)?.successData?.status=="cancelled")"Cancelación confirmada por el servidor." else "El servidor no confirmó una cancelación; consulta el estado."}}
    }
    fun download() {
        val job = (state as? Resource.Success)?.successData ?: return
        if(task?.isActive==true || job.status!="ready")return
        val account = owner; message = "Descargando PDF…";downloading=true;val began=System.nanoTime()
        task = viewModelScope.launch {
            try {
            val result = source.download(job); coroutineContext.ensureActive()
            if (account != source.owner()) return@launch
            if (result is Resource.Success) {
                val valid = withContext(Dispatchers.IO) {
                    val file = File.createTempFile("report-check-", ".pdf", context.cacheDir)
                    try {
                        file.writeBytes(result.successData.bytes)
                        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                            if(job.request?.kind=="comparison") renderer.pageCount in 1..2 && renderer.pageCount==job.page_count &&
                                (0 until renderer.pageCount).all { index -> renderer.openPage(index).use {it.width>it.height} }
                            else renderer.pageCount in 1..200
                        }
                    } catch (_: Exception) { false } finally { file.delete() }
                }
                coroutineContext.ensureActive()
                if (account == source.owner() && valid) { pdf = result.successData;downloadMillis=(System.nanoTime()-began)/1_000_000; message = "PDF completo verificado. Elige dónde guardarlo." }
                else message = "El archivo no se puede abrir como PDF válido."
            } else message = result.message
            } finally {if(account==owner)downloading=false}
        }
    }
    fun cancelDownload(){task?.cancel();downloading=false;message="Descarga detenida. El trabajo del servidor se conserva."}
    fun save(uri: Uri?, expectedHash: String?) {
        if (uri == null) { message = "Guardado cancelado."; return }
        val value = payload
        if (value == null || expectedHash != value.sha256) { discard(uri); message = "La selección cambió; no se guardó el archivo."; return }
        val account = owner; saving = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri,"w")?.use { it.write(value.bytes); it.flush() } ?: error("Sin destino") }.isSuccess }
            saving = false
            if (!ok || account != source.owner()) { discard(uri); message = "No se pudo completar el guardado." }
            else { document = uri; message = "PDF guardado correctamente." }
        }
    }
    private fun discard(uri: Uri) { runCatching { DocumentsContract.deleteDocument(context.contentResolver,uri) } }
}
