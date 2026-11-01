package com.example.sp500auth.ui.favorites

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.compare.validTicker
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel internal constructor(private val source: FavoritesDataSource) : ViewModel() {
    @Inject constructor(repository: FavoritesRepository) : this(repository as FavoritesDataSource)
    private var state by mutableStateOf<Resource<List<FavoriteItem>>>(Resource.Empty())
    private var account: String? = null
    private var job: Job? = null
    private var epoch = 0
    private var working by mutableStateOf(false)
    var message by mutableStateOf<String?>(null); private set
    val visibleState: Resource<List<FavoriteItem>> get() = when {
        source.owner() == null -> Resource.Error("Inicia sesión para consultar tus favoritos.")
        source.owner() != account -> Resource.Empty()
        else -> state
    }
    val busy get() = working && account == source.owner()
    fun item(ticker: String): FavoriteItem? = (visibleState as? Resource.Success)?.successData?.singleOrNull { it.ticker == ticker }
    fun clear() {
        job?.cancel(); epoch++; account = null; state = Resource.Empty(); working = false; message = null
    }
    private fun synchronizeAccount(): String? {
        val current = source.owner()
        if (account != current) { clear(); account = current }
        return current
    }
    fun refresh() {
        val owner = synchronizeAccount() ?: return
        if (working) return
        val generation = epoch
        state = Resource.Loading(); working = true; message = null
        job = viewModelScope.launch {
            try {
                val result = source.list(); coroutineContext.ensureActive()
                if (source.owner() != owner || epoch != generation) return@launch
                state = when (result) {
                    is FavoriteResult.Success -> Resource.Success(result.value)
                    is FavoriteResult.Failure -> Resource.Error(result.message)
                }
            } finally { if (account == owner && epoch == generation) working = false }
        }
    }
    fun add(ticker: String) {
        val owner = synchronizeAccount() ?: return
        val items = (visibleState as? Resource.Success)?.successData
        if (working || items == null || !validTicker(ticker)) return
        if (items.any { it.ticker == ticker }) { message = "$ticker ya está en favoritos."; return }
        val generation = epoch
        working = true; message = null
        job = viewModelScope.launch {
            try {
                val result = source.add(ticker); coroutineContext.ensureActive()
                if (source.owner() != owner || epoch != generation) return@launch
                when (result) {
                    is FavoriteResult.Success -> {
                        state = Resource.Success(items.filterNot { it.ticker == ticker } + result.value)
                        message = "$ticker guardado en tus favoritos."
                    }
                    is FavoriteResult.Failure -> { if (result.clearPrivate) state = Resource.Error(result.message); message = result.message }
                }
            } finally { if (account == owner && epoch == generation) working = false }
        }
    }
}
