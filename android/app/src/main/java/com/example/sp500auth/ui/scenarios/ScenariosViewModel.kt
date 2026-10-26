package com.example.sp500auth.ui.scenarios
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class ScenariosViewModel internal constructor(private val source: SimulationSource): ViewModel() {
    @Inject constructor(repository: SimulationRepository):this(repository as SimulationSource)
    var defaults by mutableStateOf<Resource<SimulationDefaults>>(Resource.Empty());private set
    var result by mutableStateOf<Resource<SimulationResponse>>(Resource.Empty());private set
    var returnText by mutableStateOf("")
    var volatilityText by mutableStateOf("")
    var seedText by mutableStateOf("")
    var visible by mutableStateOf(setOf("p10","p50","p90"));private set
    private var job: Job?=null
    private var defaultsJob: Job?=null
    fun load(ticker: String) {
        job?.cancel();defaultsJob?.cancel();defaults=Resource.Loading();result=Resource.Empty()
        defaultsJob=viewModelScope.launch{defaults=source.defaults(ticker);defaults.data?.let{fill(it)}}
    }
    private fun fill(data: SimulationDefaults) {
        fun percent(value: Double) = BigDecimal.valueOf(value).multiply(BigDecimal("100")).stripTrailingZeros().toPlainString()
        returnText=percent(data.annual_return!!)
        volatilityText=percent(data.annual_volatility!!)
        seedText=data.seed?.toString().orEmpty()
    }
    fun calculate(ticker: String) {
        job?.cancel()
        val request=simulationRequest(returnText,volatilityText,seedText,defaults.data)
        if(request==null){result=Resource.Error("Revisa retorno, volatilidad y semilla usando los límites del servidor.");return}
        result=Resource.Loading()
        job=viewModelScope.launch{result=source.simulate(ticker,request)}
    }
    fun reset(ticker: String) { defaults.data?.let{fill(it);visible=setOf("p10","p50","p90");calculate(ticker)} }
    fun toggle(id: String) { visible=if(id in visible)visible-id else visible+id }
}
