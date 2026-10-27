package com.example.sp500auth.ui.finance
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.data.repository.SprintRepository
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(private val repository: SprintRepository): ViewModel() {
    var companies by mutableStateOf<Resource<CompanyPage>>(Resource.Empty()); private set
    var summary by mutableStateOf<Resource<CompanySummary>>(Resource.Empty()); private set
    var prices by mutableStateOf<Resource<PriceSeries>>(Resource.Empty()); private set
    private var searchJob: Job?=null
    private var summaryJob: Job?=null
    private var historyJob: Job?=null
    fun search(query: String,page: Int=1) {
        searchJob?.cancel();companies=Resource.Loading()
        searchJob=viewModelScope.launch{companies=repository.search(query.take(200),page)}
    }
    fun load(ticker: String) {
        summaryJob?.cancel();summary=Resource.Loading()
        summaryJob=viewModelScope.launch{
            val result=repository.summary(ticker)
            summary=if(result is Resource.Success && result.data?.ticker!=ticker) Resource.Error("El servidor respondió con otra empresa.") else result
        }
        history(ticker,"1Y")
    }
    fun history(ticker: String,range: String) {
        historyJob?.cancel();prices=Resource.Loading()
        historyJob=viewModelScope.launch{
            val result=repository.history(ticker,range)
            prices=if(result is Resource.Success && (result.data?.ticker!=ticker || result.data?.range!=range)) Resource.Error("El histórico no corresponde a la selección.") else result
        }
    }
}
