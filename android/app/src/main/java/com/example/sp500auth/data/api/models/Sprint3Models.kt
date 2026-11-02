package com.example.sp500auth.data.api.models

data class ComparisonConclusion(val schema_version: String? = null, val owner_id: String? = null,
    val a: String? = null, val b: String? = null, val fiscal_year: Int? = null,
    val generation_mode: String? = null, val generated_at: String? = null,
    val data_version: String? = null, val paragraphs: List<String?>? = null,
    val strengths_a: List<String?>? = null, val strengths_b: List<String?>? = null,
    val sources: List<AnalysisSource?>? = null)
