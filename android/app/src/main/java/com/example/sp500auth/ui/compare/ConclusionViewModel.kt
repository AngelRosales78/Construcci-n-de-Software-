package com.example.sp500auth.ui.compare

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.api.models.ComparisonConclusion
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel class ConclusionViewModel internal constructor(private val source: ConclusionSource) : ViewModel() {
    @Inject constructor(repository: ConclusionRepository) : this(repository as ConclusionSource)
    private var state by mutableStateOf<Resource<ComparisonConclusion>>(Resource.Empty())
    private var key by mutableStateOf(""); private var owner by mutableStateOf<String?>(null); private var job: Job? = null
    fun state(a: String, b: String, year: String): Resource<ComparisonConclusion> = if (key == "$a/$b/$year" && owner == source.owner()) state else Resource.Empty()
    fun selection(a: String, b: String, year: String) {
        if (key != "$a/$b/$year" || owner != source.owner()) { job?.cancel(); state = Resource.Empty(); key = "$a/$b/$year"; owner = source.owner() }
    }
    fun load(a: String, b: String, year: String) {
        selection(a,b,year); job?.cancel()
        val fiscal = year.toIntOrNull()
        if (!validTicker(a) || !validTicker(b) || a == b || fiscal == null || fiscal !in 2000..2100) { state = Resource.Error("Selecciona dos empresas y un ejercicio válidos."); return }
        val account = source.owner(); state = Resource.Loading()
        job = viewModelScope.launch {
            val result = source.load(a,b,fiscal); coroutineContext.ensureActive()
            state = if (source.owner() == account) result else Resource.Empty()
        }
    }
}
