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

@HiltViewModel class ReportsViewModel @Inject constructor(private val source: ReportRepository, @ApplicationContext private val context: Context) : ViewModel() {
    private var owner: String? = null
    private var task: Job? = null
    private var data by mutableStateOf<Resource<ReportJob>>(Resource.Empty())
    private var pdf by mutableStateOf<PdfPayload?>(null)
    private var document by mutableStateOf<Uri?>(null)
    var message by mutableStateOf<String?>(null); private set
    var saving by mutableStateOf(false); private set
    val state: Resource<ReportJob> get() = if (owner == source.owner()) data else Resource.Empty()
    val payload get() = pdf.takeIf { owner != null && owner == source.owner() }
    val savedUri get() = document.takeIf { owner != null && owner == source.owner() }
    fun reset() { task?.cancel(); owner = source.owner(); data = Resource.Empty(); pdf = null; document = null; message = null; saving = false }
    fun generate(request: ReportRequest) {
        reset(); val account = owner; data = Resource.Loading()
        task = viewModelScope.launch {
            val result = source.submit(request); coroutineContext.ensureActive()
            if (account == source.owner()) data = result
        }
    }
    fun refresh() {
        val job = (state as? Resource.Success)?.successData ?: return
        val request = job.request ?: return; task?.cancel(); val account = owner
        task = viewModelScope.launch { val result = source.status(job.id!!,request); coroutineContext.ensureActive(); if (source.owner() == account) data = result }
    }
    fun download() {
        val job = (state as? Resource.Success)?.successData ?: return
        task?.cancel(); val account = owner; message = "Descargando PDF…"
        task = viewModelScope.launch {
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
                if (account == source.owner() && valid) { pdf = result.successData; message = "PDF completo verificado. Elige dónde guardarlo." }
                else message = "El archivo no se puede abrir como PDF válido."
            } else message = result.message
        }
    }
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
