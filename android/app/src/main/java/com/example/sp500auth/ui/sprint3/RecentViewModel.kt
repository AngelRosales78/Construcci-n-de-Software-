package com.example.sp500auth.ui.sprint3

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel class RecentViewModel internal constructor(private val source:RecentSource,private val saved:SavedStateHandle):ViewModel() {
    @Inject constructor(repository:RecentRepository,saved:SavedStateHandle):this(repository as RecentSource,saved)
    private var owner by mutableStateOf<String?>(saved["recent_owner"]);private var revision=0;private val mutex=Mutex();private val pending=mutableSetOf<String>()
    private var result by mutableStateOf<Resource<RecentPage>>(Resource.Empty())
    val state:Resource<RecentPage> get()=if(owner==source.owner())result else Resource.Empty()
    fun clear(){revision++;owner=source.owner();result=Resource.Empty();pending.clear();saved["recent_events"]=arrayListOf<String>();saved["recent_owner"]=owner}
    private fun scope():String? {if(owner!=source.owner())clear();return owner}
    fun load(){val account=scope() ?: return;val generation=revision;result=Resource.Loading();viewModelScope.launch {val data=source.list();coroutineContext.ensureActive();if(owner==account && source.owner()==account && generation==revision)result=data}}
    fun visit(ticker:String,event:String){
        val account=scope() ?: return
        if(event in saved.get<ArrayList<String>>("recent_events").orEmpty() || !pending.add(event))return
        revision++;val generation=revision
        viewModelScope.launch {try {mutex.withLock {
            if(owner!=account || source.owner()!=account)return@withLock
            val data=source.visit(ticker,event);coroutineContext.ensureActive()
            if(owner==account && source.owner()==account){
                if(data is Resource.Success)saved["recent_events"]=ArrayList((saved.get<ArrayList<String>>("recent_events").orEmpty()+event).takeLast(30))
                if(generation==revision){result=data;revision++}
            }
        }} finally {pending.remove(event)}}
    }
}
