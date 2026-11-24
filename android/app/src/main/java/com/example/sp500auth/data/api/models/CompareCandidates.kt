package com.example.sp500auth.data.api.models

/** compare-candidates-v1: companies with the same fiscal dates as A, ranked by comparable pillars. */
data class CompareCandidates(val schema_version: String? = null, val a: String? = null, val fiscal_year: Int? = null,
    val period: FiscalPeriod? = null, val a_pillars: List<String>? = null, val total_same_period: Int? = null,
    val results: List<CompareCandidate>? = null, val reason: String? = null, val available_years: List<Int>? = null)

data class CompareCandidate(val ticker: String? = null, val name: String? = null, val sector: String? = null,
    val sector_display: String? = null, val market_cap: String? = null, val comparable_pillars: Int? = null,
    val pillars: List<String>? = null, val same_sector: Boolean? = null)
