package com.example.sp500auth.ui.scenarios
import com.example.sp500auth.data.api.models.*
import java.time.LocalDate
import kotlin.math.abs

fun validDefaults(data: SimulationDefaults, ticker: String): Boolean {
    val numbers=listOf(data.annual_return,data.annual_volatility,data.min_return,data.max_return,data.min_volatility,data.max_volatility)
    if(data.schema_version!="simulation-defaults-v1" || data.ticker!=ticker || numbers.any{it==null || !it.isFinite()}) return false
    return data.min_return!! <= data.annual_return!! && data.annual_return <= data.max_return!! &&
        data.min_volatility!!>=0 && data.min_volatility<=data.annual_volatility!! && data.annual_volatility<=data.max_volatility!! &&
        data.seed!=null && (data.paths ?: 0)>0
}
fun simulationRequest(returnText: String,volatilityText: String,seedText: String,defaults: SimulationDefaults?): SimulationRequest? {
    if(defaults==null || !validDefaults(defaults,defaults.ticker.orEmpty())) return null
    val r=returnText.toDoubleOrNull()?.div(100) ?: return null
    val v=volatilityText.toDoubleOrNull()?.div(100) ?: return null
    val seed=seedText.toLongOrNull() ?: return null
    if(!r.isFinite() || !v.isFinite() || r<defaults.min_return!! || r>defaults.max_return!! || v<defaults.min_volatility!! || v>defaults.max_volatility!!) return null
    return SimulationRequest(r,v,seed,defaults.paths!!)
}
fun validSimulation(data: SimulationResponse,ticker: String,request: SimulationRequest): Boolean = try {
    val points=data.points.orEmpty()
    val start=LocalDate.parse(data.start_date);val end=LocalDate.parse(data.end_date)
    val actual=data.parameters
    val dates=points.map{LocalDate.parse(it.date)}
    data.schema_version=="simulation-v1" && data.ticker==ticker && !data.simulation_id.isNullOrBlank() &&
        !data.currency.isNullOrBlank() && !data.model_version.isNullOrBlank() && !data.generated_at.isNullOrBlank() &&
        !data.sources.isNullOrEmpty() && data.horizon_months==12 && start.plusMonths(12)==end &&
        actual!=null && actual.horizon_months==12 && actual.seed==request.seed && actual.paths==request.paths &&
        abs(actual.annual_return-request.annual_return)<1e-10 && abs(actual.annual_volatility-request.annual_volatility)<1e-10 &&
        points.size>=2 && dates.first()==start && dates.last()==end && dates.zipWithNext().all{it.first<it.second} &&
        points.all{p -> listOf(p.p10,p.p50,p.p90).all{it!=null && it.isFinite() && it>=0} && p.p10!!<=p.p50!! && p.p50<=p.p90!!}
} catch(_: Exception) { false }
