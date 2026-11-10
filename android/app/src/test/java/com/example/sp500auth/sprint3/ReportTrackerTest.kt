package com.example.sp500auth.sprint3

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.example.sp500auth.data.repository.*
import com.example.sp500auth.ui.reports.ReportsViewModel
import com.example.sp500auth.util.Resource
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ReportTrackerTest {
    private class Source:ReportSource {
        var user="1";var fail=false;var rejected=false;val submitted=mutableListOf<ReportRequest>();var reads=0
        override fun owner()=user
        override suspend fun submit(request:ReportRequest):Resource<ReportJob>{submitted+=request;return if(fail)Resource.Error("Sin red",if(rejected)ReportJob(status="not_submitted",request=request)else null)else Resource.Success(ReportJob("report-job-v1",user,"job1","queued",request))}
        override suspend fun status(id:String,request:ReportRequest):Resource<ReportJob>{reads++;return Resource.Success(ReportJob("report-job-v1",user,id,"ready",request))}
        override suspend fun download(job:ReportJob):Resource<PdfPayload> = error("Not used")
        override suspend fun cancel(job:ReportJob):Resource<ReportJob> = Resource.Success(job.copy(status="cancelled"))
    }
    private fun exercise(block:suspend TestScope.()->Unit)=runTest {Dispatchers.setMain(StandardTestDispatcher(testScheduler));try{block()}finally{Dispatchers.resetMain()}}
    @Test fun repeatedTapsDoNotCreateTwoJobsAndRetryKeepsIdempotencyKey()=exercise {
        val source=Source().apply{fail=true};val vm=ReportsViewModel(source,mock(Context::class.java),SavedStateHandle());val req=ReportRequest(a="AAPL",fiscal_year=2024)
        vm.generate(req);vm.generate(req.copy());advanceUntilIdle();assertEquals(1,source.submitted.size)
        assertFalse(vm.canPrepareNew)
        source.fail=false;vm.refresh();advanceUntilIdle();assertEquals(2,source.submitted.size);assertEquals(source.submitted[0].request_id,source.submitted[1].request_id)
    }
    @Test fun recreationResumesKnownJobWithGetInsteadOfPostingAgain()=exercise {
        val source=Source();val saved=SavedStateHandle();val context=mock(Context::class.java)
        val first=ReportsViewModel(source,context,saved);first.generate(ReportRequest(a="AAPL",fiscal_year=2024));advanceUntilIdle()
        val restored=ReportsViewModel(source,context,saved);restored.resume();advanceUntilIdle();restored.pause()
        assertEquals(1,source.submitted.size);assertEquals(1,source.reads);assertEquals("ready",(restored.state as Resource.Success).successData.status)
    }
    @Test fun cancellationMustBeConfirmedAndAnotherAccountCannotSeeOldJob()=exercise {
        val source=Source();val vm=ReportsViewModel(source,mock(Context::class.java),SavedStateHandle())
        vm.generate(ReportRequest(a="AAPL",fiscal_year=2024));advanceUntilIdle();vm.cancel();advanceUntilIdle()
        assertEquals("cancelled",(vm.state as Resource.Success).successData.status)
        source.user="2";assertTrue(vm.state is Resource.Empty);assertNull(vm.request);assertNull(vm.message)
    }
    @Test fun definitiveRejectionAllowsEditingWithoutAbandoningAnUncertainJob()=exercise {
        val source=Source().apply{fail=true;rejected=true};val vm=ReportsViewModel(source,mock(Context::class.java),SavedStateHandle())
        vm.generate(ReportRequest(a="AAPL",fiscal_year=2024));advanceUntilIdle();assertTrue(vm.canPrepareNew)
        vm.prepareNew();assertNull(vm.request);assertTrue(vm.state is Resource.Empty)
    }
}
