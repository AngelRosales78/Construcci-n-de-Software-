package com.example.sp500auth.data.repository

import com.example.sp500auth.data.api.AccountBinding
import com.example.sp500auth.ui.sprint3.RecentCompany
import com.example.sp500auth.ui.compare.validTicker
import com.example.sp500auth.util.Resource
import retrofit2.Response
import retrofit2.http.*
import java.time.Instant
import javax.inject.Inject

data class RecentPage(val schema_version:String?=null,val owner_id:String?=null,val results:List<RecentCompany?>?=null)
data class VisitEvent(val event_id:String)
fun validRecent(page:RecentPage,user:String):Boolean=runCatching {
    val rows=page.results ?: return false
    page.schema_version=="recent-companies-v1" && page.owner_id==user && rows.size<=10 && rows.all {it!=null && validTicker(it.ticker.orEmpty()) && !it.visited_at.isNullOrBlank()} &&
        rows.map{it?.ticker}.distinct().size==rows.size && rows.map {Instant.parse(it!!.visited_at)}.zipWithNext().all{(a,b)->a>=b}
}.getOrDefault(false)
interface RecentApi {
    @GET("companies/recent/") suspend fun list(@Header("Authorization")token:String,@Tag account:AccountBinding):Response<RecentPage>
    @POST("companies/{ticker}/visits/") suspend fun visit(@Path("ticker") ticker:String,@Body event:VisitEvent,@Header("Authorization")token:String,@Tag account:AccountBinding):Response<RecentPage>
}
interface RecentSource {fun owner():String?;suspend fun list():Resource<RecentPage>;suspend fun visit(ticker:String,event:String):Resource<RecentPage>}
class RecentRepository @Inject constructor(private val remote:ScopedRemote):RecentSource {
    private val api=remote.retrofit.create(RecentApi::class.java)
    override fun owner()=remote.owner()
    override suspend fun list()=remote.call({page:RecentPage,user->validRecent(page,user)}){token,account->api.list(token,account)}
    override suspend fun visit(ticker:String,event:String):Resource<RecentPage>{
        if(!validTicker(ticker) || !event.matches(Regex("[a-zA-Z0-9-]{1,80}")))return Resource.Error("Visita no válida.")
        return remote.call({page:RecentPage,user->validRecent(page,user) && page.results?.firstOrNull()?.ticker==ticker}){token,account->api.visit(ticker,VisitEvent(event),token,account)}
    }
}
