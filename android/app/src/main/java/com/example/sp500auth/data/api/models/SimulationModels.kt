package com.example.sp500auth.data.api.models

/** Android proposal for the shared simulation API; backend implementation is pending. */
data class SimulationDefaults(val schema_version: String? = null, val ticker: String? = null,
    val annual_return: Double? = null, val annual_volatility: Double? = null,
    val min_return: Double? = null, val max_return: Double? = null,
    val min_volatility: Double? = null, val max_volatility: Double? = null,
    val seed: Long? = null, val paths: Int? = null)
data class SimulationRequest(val annual_return: Double, val annual_volatility: Double,
    val seed: Long, val paths: Int, val horizon_months: Int = 12)
data class ScenarioPoint(val date: String? = null, val p10: Double? = null,
    val p50: Double? = null, val p90: Double? = null)
data class SimulationResponse(val schema_version: String? = null, val ticker: String? = null,
    val simulation_id: String? = null, val currency: String? = null,
    val model_version: String? = null, val generated_at: String? = null,
    val start_date: String? = null, val end_date: String? = null, val horizon_months: Int? = null,
    val parameters: SimulationRequest? = null, val points: List<ScenarioPoint>? = null,
    val history: List<PricePoint>? = null, val sources: List<String>? = null)
