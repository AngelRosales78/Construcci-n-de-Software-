package com.example.sp500auth.ui.analysis
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel internal constructor(private val source: com.example.sp500auth.data.repository.AnalysisSource): ViewModel() {
    @Inject constructor(repository: AnalysisRepository):this(repository as com.example.sp500auth.data.repository.AnalysisSource)
    var state by mutableStateOf<Resource<AnalysisResponse>>(Resource.Empty());private set
    var lastValid by mutableStateOf<AnalysisResponse?>(null);private set
    var requestedLevel by mutableStateOf("simple");private set
    private var selection: Pair<String,Int>?=null
    private var job: Job?=null
    fun load(ticker: String,year: Int,level: String=requestedLevel) {
        job?.cancel()
        if(level !in setOf("simple","technical") || year !in 1900..java.time.Year.now().value) {
            state=Resource.Error("Empresa, ejercicio o nivel de lenguaje inválido.");return
        }
        if(selection != (ticker to year)){lastValid=null;selection=ticker to year}
        requestedLevel=level;state=Resource.Loading()
        job=viewModelScope.launch {
            state=source.load(ticker,AnalysisRequest(year,level))
            if(state is Resource.Success)lastValid=state.data
        }
    }
}
