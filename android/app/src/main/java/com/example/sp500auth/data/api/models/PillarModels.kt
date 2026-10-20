package com.example.sp500auth.data.api.models

/** Matches Diego's proposed pillars-v1 contract. Decimal values stay as strings. */
data class PillarsResponse(
    val schema_version: String? = null,
    val methodology_version: String? = null,
    val ticker: String? = null,
    val fiscal_year: Int? = null,
    val period: FiscalPeriod? = null,
    val pillars: List<FinancialPillar>? = null
)
data class FiscalPeriod(val start: String? = null, val end: String? = null)
data class FinancialPillar(val id: String? = null, val metrics: List<PillarMetric>? = null)
data class PillarMetric(
    val id: String? = null,
    val label: String? = null,
    val status: String? = null,
    val value: String? = null,
    val display_value: String? = null,
    val unit: String? = null,
    val reason: String? = null,
    val missing_fields: List<String>? = null,
    val formula: String? = null,
    val period: FiscalPeriod? = null,
    val components: List<MetricComponent>? = null,
    val sector_reference: SectorReference? = null
)
data class MetricComponent(
    val id: String? = null,
    val label: String? = null,
    val value: String? = null,
    val unit: String? = null,
    val period: FiscalPeriod? = null,
    val source: ComponentSource? = null
)
data class ComponentSource(
    val url: String? = null,
    val label: String? = null,
    val tag: String? = null,
    val unit: String? = null,
    val filing: String? = null,
    val published_at: String? = null
)

/** Optional Android adapter proposal; final field names must be confirmed with the server. */
data class SectorReference(
    val status: String? = null,
    val value: String? = null,
    val display_value: String? = null,
    val unit: String? = null,
    val reason: String? = null,
    val sample_size: Int? = null,
    val universe: String? = null,
    val as_of: String? = null,
    val period: FiscalPeriod? = null,
    val source: ComponentSource? = null
)
