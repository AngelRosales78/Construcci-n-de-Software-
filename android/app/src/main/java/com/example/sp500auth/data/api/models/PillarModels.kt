package com.example.sp500auth.data.api.models

/** Matches Diego's proposed pillars-v1 contract. Decimal values stay as strings. */
data class PillarsResponse(
    val schema_version: String? = null,
    val methodology_version: String? = null,
    val ticker: String? = null,
    val fiscal_year: Int? = null,
    val period: FiscalPeriod? = null,
    val pillars: List<FinancialPillar>? = null,
    val currency: String? = null, val available_years: List<Int>? = null,
    val methodology_status: String? = null, val overall_score: PillarScore? = null,
    val scoring_policy: ScoringPolicy? = null
)
data class FiscalPeriod(val start: String? = null, val end: String? = null)
data class FinancialPillar(
    val id: String? = null, val label: String? = null, val status: String? = null,
    val reason: String? = null, val metrics: List<PillarMetric>? = null,
    val score: PillarScore? = null, val sector_reference: SectorReference? = null,
    val history: List<PillarHistory>? = null
)
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
    val formula_display: String? = null,
    val explanation: String? = null,
    val thresholds: MetricThresholds? = null,
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
    val published_at: String? = null,
    val accession: String? = null, val filed: String? = null,
    val currency: String? = null, val snapshot_sha256: String? = null,
    val is_demo: Boolean? = null
)

/** Optional Android adapter proposal; final field names must be confirmed with the server. */
data class SectorReference(
    val status: String? = null,
    val value: String? = null,
    val display_value: String? = null,
    val unit: String? = null,
    val reason: String? = null,
    val sample_size: Int? = null,
    val explanation: String? = null,
    val universe: String? = null,
    val as_of: String? = null,
    val period: FiscalPeriod? = null,
    val source: ComponentSource? = null
)

data class MetricThresholds(val lower: String? = null, val upper: String? = null,
    val direction: String? = null, val version: String? = null, val band: String? = null,
    val explanation: String? = null)
data class PillarHistory(val fiscal_year: Int? = null, val period: FiscalPeriod? = null,
    val metrics: List<PillarMetric>? = null)
data class PillarScore(val status: String? = null, val value: String? = null,
    val display_value: String? = null, val version: String? = null, val reason: String? = null,
    val period: FiscalPeriod? = null, val aggregation: String? = null,
    val missing_metrics: List<String>? = null, val excluded_metrics: List<String>? = null,
    val missing_pillars: List<String>? = null, val parts: List<ScorePart>? = null,
    val contributions: List<ScoreContribution>? = null)
data class ScorePart(val id: String? = null, val label: String? = null,
    val input_value: String? = null, val value: String? = null, val display_value: String? = null,
    val anchors: List<ScoreAnchor>? = null)
data class ScoreAnchor(val input: String? = null, val score: String? = null)
data class ScoreContribution(val id: String? = null, val label: String? = null,
    val weight: String? = null, val weight_display: String? = null,
    val value: String? = null, val display_value: String? = null)
data class ScoringPolicy(val version: String? = null, val status: String? = null,
    val explanation: String? = null)
