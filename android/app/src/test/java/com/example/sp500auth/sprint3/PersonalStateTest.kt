package com.example.sp500auth.sprint3

import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.favorites.*
import com.example.sp500auth.ui.sprint3.*
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalStateTest {
    private fun exercise(block:suspend TestScope.()->Unit)=runTest {Dispatchers.setMain(StandardTestDispatcher(testScheduler));try{block()}finally{Dispatchers.resetMain()}}
    @Test fun syncRetainsClearlyStaleCopyAndDenialClearsIt()=exercise {
        var failure:FavoriteResult.Failure?=null;var rows=listOf(FavoriteItem(7,"AAPL"));var changes=0
        val source=object:FavoritesDataSource {
            override fun owner()="1"
            override suspend fun list():FavoriteResult<List<FavoriteItem>> = failure ?: FavoriteResult.Success(rows)
            override suspend fun add(ticker:String):FavoriteResult<FavoriteItem>{changes++;return FavoriteResult.Success(FavoriteItem(8,ticker))}
            override suspend fun remove(item:FavoriteItem):FavoriteResult<Unit> = error("Not used")
        }
        val vm=FavoritesViewModel(source);vm.refresh();advanceUntilIdle();assertNotNull(vm.lastSynced)
        failure=FavoriteResult.Failure("Sin red");vm.refresh();advanceUntilIdle();assertTrue(vm.isStale);assertNotNull(vm.item("AAPL"))
        vm.add("MSFT");advanceUntilIdle();assertEquals(0,changes)
        failure=null;rows=listOf(FavoriteItem(8,"MSFT"));vm.refresh();advanceUntilIdle();assertNull(vm.item("AAPL"));assertFalse(vm.isStale)
        failure=FavoriteResult.Failure("Sin permiso",true);vm.refresh();advanceUntilIdle();assertTrue(vm.visibleState is Resource.Error);assertNull(vm.item("MSFT"))
    }
    @Test fun visitEventIsRecordedOnceAndAccountChangeHidesHistory()=exercise {
        var calls=0;var user="1"
        val source=object:RecentSource {
            override fun owner()=user
            override suspend fun list():Resource<RecentPage> = Resource.Success(RecentPage("recent-companies-v1",user,emptyList()))
            override suspend fun visit(ticker:String,event:String):Resource<RecentPage>{calls++;return Resource.Success(RecentPage("recent-companies-v1",user,listOf(RecentCompany(ticker,ticker,"2026-11-02T00:00:00Z"))))}
        }
        val vm=RecentViewModel(source,SavedStateHandle());vm.visit("AAPL","event-1");vm.visit("AAPL","event-1");advanceUntilIdle();vm.visit("AAPL","event-1");advanceUntilIdle();assertEquals(1,calls)
        user="2";assertTrue(vm.state is Resource.Empty);vm.load();advanceUntilIdle();assertTrue((vm.state as Resource.Success).successData.results!!.isEmpty())
    }
    @Test fun duplicateTagNamesAreBlockedAndConflictsRequireReload()=exercise {
        var mutations=0;var failed=true
        val snapshot=TagsSnapshot("tags-v1","1","v1",listOf(TagItem(1,"Tecnología","#36F5A0")),emptyList())
        val source=object:TagsSource {
            override fun owner()="1"
            override suspend fun load():Resource<TagsSnapshot> = Resource.Success(snapshot)
            override suspend fun edit(id:Long?,name:String,color:String,version:String):Resource<TagsSnapshot>{mutations++;return if(failed)Resource.Error("Conflicto de versión")else Resource.Success(snapshot)}
            override suspend fun delete(id:Long,version:String):Resource<TagsSnapshot> = error("Not used")
            override suspend fun assign(id:Long,tags:List<Long>,version:String):Resource<TagsSnapshot> = error("Not used")
        }
        val vm=TagsViewModel(source);vm.load();advanceUntilIdle();assertEquals(0,vm.savedVersion)
        vm.edit(null," TECNOLOGÍA ","#36F5A0");advanceUntilIdle();assertEquals(0,mutations)
        vm.edit(1,"Nuevo","#36F5A0");advanceUntilIdle();assertTrue(vm.state is Resource.Error)
        vm.load();advanceUntilIdle();failed=false;vm.edit(1,"Nuevo","#36F5A0");advanceUntilIdle();assertEquals(1,vm.savedVersion)
    }
    @Test fun lateHistoryReadCannotOverwriteAConfirmedVisit()=exercise {
        val read=CompletableDeferred<Resource<RecentPage>>();val visit=CompletableDeferred<Resource<RecentPage>>()
        val source=object:RecentSource {
            override fun owner()="1"
            override suspend fun list()=read.await()
            override suspend fun visit(ticker:String,event:String)=visit.await()
        }
        val vm=RecentViewModel(source,SavedStateHandle());vm.visit("AAPL","event-1");runCurrent();vm.load();runCurrent()
        visit.complete(Resource.Success(RecentPage("recent-companies-v1","1",listOf(RecentCompany("AAPL","Apple","2026-11-02T00:00:00Z")))));runCurrent()
        read.complete(Resource.Success(RecentPage("recent-companies-v1","1",emptyList())));advanceUntilIdle()
        assertEquals("AAPL",(vm.state as Resource.Success).successData.results!!.single()!!.ticker)
    }
}
