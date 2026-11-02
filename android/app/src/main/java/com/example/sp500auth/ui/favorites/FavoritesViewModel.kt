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
    private var candidate by mutableStateOf<FavoriteItem?>(null)
    val removalCandidate get() = candidate.takeIf { account == source.owner() }
    private var accountMessage by mutableStateOf<String?>(null)
    private var synced by mutableStateOf<Long?>(null)
    private var stale by mutableStateOf(false)
    private var syncProblem by mutableStateOf<String?>(null)
    val lastSynced get()=synced.takeIf {account==source.owner()}
    val isStale get()=stale && account==source.owner()
    val syncError get()=syncProblem.takeIf {account==source.owner()}
    val message get() = accountMessage.takeIf { account == source.owner() }
    val visibleState: Resource<List<FavoriteItem>> get() = when {
        source.owner() == null -> Resource.Error("Inicia sesión para consultar tus favoritos.")
        source.owner() != account -> Resource.Empty()
        else -> state
    }
    val busy get() = working && account == source.owner()
    fun item(ticker: String): FavoriteItem? = (visibleState as? Resource.Success)?.successData?.singleOrNull { it.ticker == ticker }
    fun clear() {
        job?.cancel(); epoch++; account = null; state = Resource.Empty(); working = false; accountMessage = null; candidate = null;synced=null;stale=false;syncProblem=null
    }
    private fun synchronizeAccount(): String? {
        val current = source.owner()
        if (account != current) { clear(); account = current }
        return current
    }
    fun refresh() {
        val owner = synchronizeAccount() ?: return
        if (working || candidate!=null) return
        val generation = epoch
        val previous=(state as? Resource.Success)?.successData
        if(previous==null)state = Resource.Loading()
        working = true;syncProblem=null
        job = viewModelScope.launch {
            try {
                val result = source.list(); coroutineContext.ensureActive()
                if (source.owner() != owner || epoch != generation) return@launch
                state = when (result) {
                    is FavoriteResult.Success -> {synced=System.currentTimeMillis();stale=false;Resource.Success(result.value)}
                    is FavoriteResult.Failure -> {
                        syncProblem=result.message
                        if(previous!=null && !result.clearPrivate){stale=true;Resource.Success(previous)} else {stale=false;synced=null;Resource.Error(result.message)}
                    }
                }
            } finally { if (account == owner && epoch == generation) working = false }
        }
    }
    fun add(ticker: String) {
        val owner = synchronizeAccount() ?: return
        val items = (visibleState as? Resource.Success)?.successData
        if (working || stale || items == null || !validTicker(ticker)) return
        if (items.any { it.ticker == ticker }) { accountMessage = "$ticker ya está en favoritos."; return }
        val generation = epoch
        working = true; accountMessage = null
        job = viewModelScope.launch {
            try {
                val result = source.add(ticker); coroutineContext.ensureActive()
                if (source.owner() != owner || epoch != generation) return@launch
                when (result) {
                    is FavoriteResult.Success -> {
                        if (result.value.ticker != ticker || result.value.id == null || items.any { it.id == result.value.id && it.ticker != ticker }) {
                            accountMessage = "El servidor devolvió un favorito incoherente. Actualiza la lista."
                        } else {
                            state = Resource.Success(items.filterNot { it.ticker == ticker } + result.value)
                            accountMessage = "$ticker guardado en tus favoritos."
                        }
                    }
                    is FavoriteResult.Failure -> { if (result.clearPrivate) state = Resource.Error(result.message); accountMessage = result.message }
                }
            } finally { if (account == owner && epoch == generation) working = false }
        }
    }

    fun requestRemoval(ticker: String) {
        synchronizeAccount()
        if (!working && !stale) { candidate = item(ticker); accountMessage = null }
    }
    fun cancelRemoval() { if (!working) { candidate = null; accountMessage = null } }
    fun confirmRemoval() {
        val owner = synchronizeAccount() ?: return
        val selected = removalCandidate ?: return
        val items = (visibleState as? Resource.Success)?.successData ?: return
        if (working || selected.id == null || item(selected.ticker.orEmpty())?.id != selected.id) return
        val generation = epoch
        working = true; accountMessage = null
        job = viewModelScope.launch {
            try {
                val result = source.remove(selected); coroutineContext.ensureActive()
                if (source.owner() != owner || epoch != generation) return@launch
                when (result) {
                    is FavoriteResult.Success -> {
                        state = Resource.Success(items.filterNot { it.id == selected.id && it.ticker == selected.ticker })
                        candidate = null; accountMessage = "${selected.ticker} quitado de favoritos."
                    }
                    is FavoriteResult.Failure -> {
                        if (result.clearPrivate) { state = Resource.Error(result.message); candidate = null }
                        accountMessage = result.message
                    }
                }
            } finally { if (account == owner && epoch == generation) working = false }
        }
    }
}
