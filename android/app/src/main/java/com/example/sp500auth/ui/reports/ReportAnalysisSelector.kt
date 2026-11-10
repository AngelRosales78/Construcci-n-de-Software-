package com.example.sp500auth.ui.reports

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel

@HiltViewModel class ReportAnalysisViewModel @Inject constructor(private val source:ReportAnalysisRepository):ViewModel() {
    private var value by mutableStateOf<Resource<ReportAnalysis>>(Resource.Empty()); private var key by mutableStateOf(""); private var owner by mutableStateOf<String?>(null); private var job:Job?=null
    fun state(k:String):Resource<ReportAnalysis> = if(key==k && owner==source.owner()) value else Resource.Empty()
    fun clear(k:String) { if(key!=k || owner!=source.owner()) {job?.cancel();key=k;owner=source.owner();value=Resource.Empty()} }
    fun load(a:String,b:String?,year:String,level:String) {
        clear("$a/$b/$year/$level"); job?.cancel(); val account=owner; val fiscal=year.toIntOrNull()
        if(fiscal==null || fiscal !in 2000..2100) {value=Resource.Error("Ejercicio no válido.");return}
        value=Resource.Loading(); job=viewModelScope.launch { val result=source.load(a,b,fiscal,level);coroutineContext.ensureActive();value=if(source.owner()==account) result else Resource.Empty() }
    }
}
@Composable fun ReportAnalysisSelector(a:String,b:String?,year:String,level:String,include:Boolean,enabled:Boolean,onInclude:(Boolean)->Unit,onLevel:(String)->Unit,onSelection:(ReportAnalysis?)->Unit,vm:ReportAnalysisViewModel=hiltViewModel()) {
    val key="$a/$b/$year/$level"; val state=vm.state(key)
    LaunchedEffect(key) {vm.clear(key);onInclude(false);onSelection(null)}
    val data=(state as? Resource.Success)?.successData
    LaunchedEffect(data) {onSelection(data)}
    AnalysisOptions(include,level,enabled && state !is Resource.Loading,data!=null,data?.origin,onInclude,onLevel,{vm.load(a,b,year,level)})
    if(state is Resource.Loading) Text("Comprobando análisis…")
    if(state is Resource.Error) Text(state.message.orEmpty())
    if(data?.origin=="mixed") Text("Incluye explicaciones de IA y resumen por reglas; se conservará esa distinción en el PDF.")
}
