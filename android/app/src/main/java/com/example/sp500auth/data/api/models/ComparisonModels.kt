package com.example.sp500auth.data.api.models

/** Android proposal for the shared comparison service; see docs/android/CONTRATO_COMPARADOR.md. */
data class ComparisonResponse(
    val schema_version: String? = null, val fiscal_year: Int? = null,
    val a: PillarsResponse? = null, val b: PillarsResponse? = null,
    val comparisons: List<PillarComparison>? = null,
    val status: String? = null, val reason: String? = null
)

data class PillarComparison(
    val id: String? = null, val status: String? = null,
    val result: String? = null, val delta: String? = null,
    val unit: String? = null, val reason: String? = null
)
