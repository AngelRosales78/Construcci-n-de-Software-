package com.example.sp500auth.sprint3

import com.example.sp500auth.data.api.models.ComparisonConclusion
import com.example.sp500auth.data.repository.ConclusionSource
import com.example.sp500auth.ui.compare.ConclusionViewModel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConclusionStateTest {
    @Test fun lateConclusionNeverAppearsForAnotherPairOrAccount()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var owner="1"
            val source=object:ConclusionSource {
                override fun owner()=owner
                override suspend fun load(a:String,b:String,year:Int):Resource<ComparisonConclusion>{if(a=="AAPL")withContext(NonCancellable){delay(1000)};return Resource.Success(ComparisonConclusion(a=a,b=b,fiscal_year=year))}
            }
            val vm=ConclusionViewModel(source);vm.load("AAPL","MSFT","2024");runCurrent();vm.selection("MSFT","NVDA","2024");vm.load("MSFT","NVDA","2024");advanceUntilIdle()
            assertEquals("MSFT",(vm.state("MSFT","NVDA","2024") as Resource.Success).successData.a)
            owner="2";assertTrue(vm.state("MSFT","NVDA","2024") is Resource.Empty)
        }finally{Dispatchers.resetMain()}
    }
}
