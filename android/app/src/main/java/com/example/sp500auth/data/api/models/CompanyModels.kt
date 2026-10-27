package com.example.sp500auth.data.api.models

data class CompanyPage(val count: Int = 0, val next: String? = null, val previous: String? = null, val results: List<CompanyItem>? = null)
data class CompanyItem(val ticker: String? = null, val name: String? = null, val sector: String? = null,
    val sector_display: String? = null, val market_cap: String? = null)
data class CompanySummary(val ticker: String? = null, val name: String? = null, val sector_display: String? = null,
    val as_of: String? = null, val source: String? = null, val is_demo: Boolean? = null,
    val market_cap: String? = null, val per: String? = null, val roe: String? = null,
    val altman_z: AltmanData? = null, val quote: QuoteData? = null,
    val beta: String? = null, val dividend_yield: String? = null,
    val field_sources: Map<String, FieldOrigin>? = null, val reporting_note: String? = null)
data class AltmanData(val score: String? = null, val zone: String? = null)
data class QuoteData(val date: String? = null, val adjusted_close: String? = null,
    val currency: String? = null, val source: String? = null, val is_demo: Boolean? = null)
data class PriceSeries(val ticker: String? = null, val range: String? = null,
    val history: List<PricePoint>? = null, val currencies: List<String>? = null,
    val sources: List<String>? = null, val is_demo: Boolean? = null)
data class PricePoint(val date: String? = null, val price: Double? = null)

data class FieldOrigin(val as_of: String? = null, val source: String? = null, val method: String? = null, val status: String? = null, val reason: String? = null)
