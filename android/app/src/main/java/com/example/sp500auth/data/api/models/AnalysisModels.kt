package com.example.sp500auth.data.api.models

/** Proposed Android contract. Only server-validated explanations can be displayed. */
data class AnalysisRequest(val fiscal_year: Int,val level: String)
data class AnalysisValidation(val status: String? = null,val checked_at: String? = null)
data class AnalysisSection(val pillar_id: String? = null,val text: String? = null,val metric_ids: List<String>? = null)
data class AnalysisPoint(val text: String? = null,val metric_ids: List<String>? = null)
data class AnalysisSource(val label: String? = null,val url: String? = null)
data class AnalysisResponse(val schema_version: String? = null,val ticker: String? = null,
    val fiscal_year: Int? = null,val level: String? = null,val metrics_version: String? = null,
    val data_as_of: String? = null,val generated_at: String? = null,val period: FiscalPeriod? = null,
    val validation: AnalysisValidation? = null,val summary: List<String>? = null,
    val pillars: List<AnalysisSection>? = null,val strengths: List<AnalysisPoint>? = null,
    val risks: List<AnalysisPoint>? = null,val sources: List<AnalysisSource>? = null)
