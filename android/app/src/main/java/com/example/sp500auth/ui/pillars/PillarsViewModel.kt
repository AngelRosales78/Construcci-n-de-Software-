package com.example.sp500auth.ui.pillars

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.PillarsResponse
import com.example.sp500auth.data.repository.PillarsDataSource
import com.example.sp500auth.data.repository.PillarsRepository
import com.example.sp500auth.data.repository.PillarsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PillarsState {
    object Loading : PillarsState()
    data class Ready(val data: PillarsResponse) : PillarsState()
    data class Error(val message: String) : PillarsState()
}

@HiltViewModel
class PillarsViewModel internal constructor(private val repository: PillarsDataSource) : ViewModel() {
    @Inject constructor(repository: PillarsRepository) : this(repository as PillarsDataSource)
    var state by mutableStateOf<PillarsState>(PillarsState.Loading)
        private set
    private var request: Job? = null

    fun load(ticker: String, yearText: String) {
        request?.cancel()
        val year = yearText.toIntOrNull()
        if (!ticker.matches(Regex("[A-Z0-9][A-Z0-9.\\-]{0,14}")) || year == null || year !in 1900..2100) {
            state = PillarsState.Error("Ingresa un símbolo válido y un ejercicio fiscal entre 1900 y 2100.")
            return
        }
        state = PillarsState.Loading
        request = viewModelScope.launch {
            state = when (val result = repository.load(ticker, year)) {
                is PillarsResult.Success -> PillarsState.Ready(result.data)
                is PillarsResult.Failure -> PillarsState.Error(result.message)
            }
        }
    }
}
