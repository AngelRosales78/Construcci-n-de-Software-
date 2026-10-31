package com.example.sp500auth.ui.compare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.api.models.CompanyItem
import com.example.sp500auth.data.api.models.ComparisonResponse
import com.example.sp500auth.data.repository.ComparisonDataSource
import com.example.sp500auth.data.repository.ComparisonRepository
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ComparisonViewModel internal constructor(
    private val source: ComparisonDataSource, private val saved: SavedStateHandle
) : ViewModel() {
    @Inject constructor(repository: ComparisonRepository, saved: SavedStateHandle) : this(repository as ComparisonDataSource, saved)

    var selection by mutableStateOf(CompanySelection(saved["a"] ?: "", saved["aName"] ?: "", saved["b"] ?: "", saved["bName"] ?: "")); private set
    var queryA by mutableStateOf(saved.get<String>("queryA") ?: ""); private set
    var queryB by mutableStateOf(saved.get<String>("queryB") ?: ""); private set
    var suggestionsA by mutableStateOf<Resource<List<CompanyItem>>>(Resource.Empty()); private set
    var suggestionsB by mutableStateOf<Resource<List<CompanyItem>>>(Resource.Empty()); private set
    var selectionError by mutableStateOf<String?>(null); private set
    var year by mutableStateOf(saved.get<String>("year") ?: (LocalDate.now().year - 1).toString()); private set
    var result by mutableStateOf<Resource<ComparisonResponse>>(Resource.Empty()); private set
    private var comparisonJob: Job? = null
    private val jobs = mutableMapOf<CompanySlot, Job>()

    fun initialize(ticker: String) {
        if (saved.get<Boolean>("initial_applied") == true) return
        saved["initial_applied"] = true
        if (selection.a.isBlank() && selection.b.isBlank() && validTicker(ticker)) {
            select(CompanySlot.A, CompanyItem(ticker = ticker, name = ticker))
        }
    }

    fun query(slot: CompanySlot, text: String) {
        val value = text.take(200)
        if (slot == CompanySlot.A) { queryA = value; saved["queryA"] = value }
        else { queryB = value; saved["queryB"] = value }
        jobs[slot]?.cancel()
        suggestions(slot, if (value.isBlank()) Resource.Empty() else Resource.Loading())
        if (value.isBlank()) return
        jobs[slot] = viewModelScope.launch {
            delay(300)
            val result = source.search(value)
            if (value == if (slot == CompanySlot.A) queryA else queryB) suggestions(slot, result)
        }
    }

    private fun suggestions(slot: CompanySlot, value: Resource<List<CompanyItem>>) {
        if (slot == CompanySlot.A) suggestionsA = value else suggestionsB = value
    }

    fun select(slot: CompanySlot, company: CompanyItem) {
        val next = selection.select(slot, company.ticker.orEmpty(), company.name.orEmpty())
        if (next == null) { selectionError = "Elige dos empresas distintas con un símbolo válido."; return }
        update(next)
        query(slot, "")
    }
    fun remove(slot: CompanySlot) { update(selection.remove(slot)); query(slot, "") }
    fun swap() { update(selection.swap()); query(CompanySlot.A, ""); query(CompanySlot.B, "") }

    private fun update(value: CompanySelection) {
        clearResult()
        selection = value; selectionError = null
        saved["a"] = value.a; saved["aName"] = value.aName
        saved["b"] = value.b; saved["bName"] = value.bName
    }

    fun changeYear(value: String) {
        clearResult()
        year = value.take(4); saved["year"] = year
    }

    private fun clearResult() {
        comparisonJob?.cancel(); result = Resource.Empty()
    }

    fun compare() {
        clearResult()
        val fiscalYear = year.toIntOrNull()
        if (!selection.canCompare || fiscalYear == null || fiscalYear !in 2000..2100) {
            result = Resource.Error("Selecciona dos empresas distintas y un ejercicio de cuatro cifras entre 2000 y 2100.")
            return
        }
        val pair = selection
        result = Resource.Loading()
        comparisonJob = viewModelScope.launch {
            val response = source.compare(pair.a, pair.b, fiscalYear)
            coroutineContext.ensureActive()
            if (selection == pair && year.toIntOrNull() == fiscalYear) result = response
        }
    }
}
