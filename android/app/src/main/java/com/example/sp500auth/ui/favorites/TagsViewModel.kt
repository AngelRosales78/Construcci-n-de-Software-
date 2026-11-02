package com.example.sp500auth.ui.favorites

import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel class TagsViewModel internal constructor(private val source:TagsSource):ViewModel(){
    @Inject constructor(repository:TagsRepository):this(repository as TagsSource)
    private var data by mutableStateOf<Resource<TagsSnapshot>>(Resource.Empty());private var owner:String?=null;private var job:Job?=null
    private var notice by mutableStateOf<String?>(null);private var working by mutableStateOf(false)
    var savedVersion by mutableStateOf(0);private set
    val accountKey get()=source.owner()
    val message get()=notice.takeIf{owner==source.owner()}
    val busy get()=working && owner==source.owner()
    val state:Resource<TagsSnapshot> get()=if(owner==source.owner())data else Resource.Empty()
    private fun execute(operation:suspend ()->Resource<TagsSnapshot>){
        if(owner!=source.owner()){job?.cancel();data=Resource.Empty();working=false;notice=null;owner=source.owner()}
        if(working)return
        val account=owner;working=true;notice=null
        job=viewModelScope.launch{try{val result=operation();coroutineContext.ensureActive();if(account==source.owner()){data=result;if(result is Resource.Success)savedVersion++ else notice=result.message}}finally{if(account==owner)working=false}}
    }
    fun load(){execute{source.load()}}
    fun edit(id:Long?,name:String,color:String){val snapshot=(state as? Resource.Success)?.successData ?: return
        if(!validTagName(name) || snapshot.tags.orEmpty().any{it?.id!=id && normalizedTag(it?.name.orEmpty())==normalizedTag(name)}){notice="Usa un nombre no vacío y diferente de las otras etiquetas.";return}
        execute{source.edit(id,name,color,snapshot.version!!)}
    }
    fun delete(id:Long){val snapshot=(state as? Resource.Success)?.successData ?: return;execute{source.delete(id,snapshot.version!!)}}
    fun assign(id:Long,ids:List<Long>){val snapshot=(state as? Resource.Success)?.successData ?: return;execute{source.assign(id,ids,snapshot.version!!)}}
}
