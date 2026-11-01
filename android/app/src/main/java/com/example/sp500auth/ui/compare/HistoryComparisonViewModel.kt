package com.example.sp500auth.ui.compare

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.HistoryComparisonSource
import com.example.sp500auth.data.repository.HistoryComparisonRepository
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryComparisonViewModel internal constructor(private val source: HistoryComparisonSource, private val saved: SavedStateHandle) : ViewModel() {
    @Inject constructor(repository: HistoryComparisonRepository, saved: SavedStateHandle) : this(repository as HistoryComparisonSource, saved)
    var range by mutableStateOf(saved.get<String>("history_range")?.takeIf { it in historyRanges } ?: "1Y"); private set
    var state by mutableStateOf<Resource<AlignedHistory>>(Resource.Empty()); private set
    var a by mutableStateOf(""); private set
    var b by mutableStateOf(""); private set
    private var job: Job? = null
    fun load(left: String, right: String, period: String = range) {
        job?.cancel(); a = left; b = right; state = Resource.Empty()
        if (!validTicker(left) || !validTicker(right) || left == right) return
        if (period !in historyRanges) { state = Resource.Error("Rango no válido."); return }
        range = period; saved["history_range"] = period; state = Resource.Loading()
        job = viewModelScope.launch {
            val response = source.load(left, right, period)
            coroutineContext.ensureActive()
            if (a == left && b == right && range == period) state = response
        }
    }
}
