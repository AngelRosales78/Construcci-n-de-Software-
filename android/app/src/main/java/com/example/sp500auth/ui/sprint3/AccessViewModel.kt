package com.example.sp500auth.ui.sprint3

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel class AccessViewModel internal constructor(private val source: AccessSource) : ViewModel() {
    @Inject constructor(repository: AccessRepository) : this(repository as AccessSource)
    private var result by mutableStateOf<Resource<AccessInfo>>(Resource.Empty())
    private var owner by mutableStateOf<String?>(null); private var job: Job? = null
    val state: Resource<AccessInfo> get() = if (owner == source.owner()) result else Resource.Empty()
    fun load() {
        job?.cancel(); owner = source.owner(); val requested = owner; result = Resource.Loading()
        job = viewModelScope.launch { val value = source.load(); coroutineContext.ensureActive(); result = if (requested == source.owner()) value else Resource.Empty() }
    }
}
