package com.example.sp500auth.ui.analysis
import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.pillarLabels
import com.example.sp500auth.ui.common.safeSourceUrl
import java.time.LocalDate

val pillarMetricIds=mapOf("liquidity" to setOf("current_ratio","quick_ratio"),
    "profitability" to setOf("roe","roa","net_margin"),"solvency" to setOf("liabilities_to_equity","interest_coverage"),
    "efficiency" to setOf("asset_turnover","inventory_days"),"growth" to setOf("revenue_growth","revenue_cagr_3y"))
val allowedMetricIds=setOf("current_ratio","quick_ratio","roe","roa","net_margin","liabilities_to_equity",
    "interest_coverage","asset_turnover","inventory_days","revenue_growth","revenue_cagr_3y")
fun validAnalysis(data: AnalysisResponse,ticker: String,request: AnalysisRequest): Boolean {
    val sections=data.pillars.orEmpty();val paragraphs=data.summary.orEmpty()
    val validPeriod=try {
        val start=LocalDate.parse(data.period?.start);val end=LocalDate.parse(data.period?.end)
        start<end && end.year==request.fiscal_year && data.data_as_of==end.toString()
    } catch(_:Exception){false}
    fun validRefs(refs: List<String>?) = !refs.isNullOrEmpty() && refs.all{it in allowedMetricIds}
    return data.schema_version=="analysis-v1" && data.ticker==ticker && data.fiscal_year==request.fiscal_year &&
        validPeriod && data.level==request.level && request.level in setOf("simple","technical") &&
        !data.metrics_version.isNullOrBlank() && !data.data_as_of.isNullOrBlank() && !data.generated_at.isNullOrBlank() &&
        !data.period?.start.isNullOrBlank() && !data.period?.end.isNullOrBlank() &&
        data.validation?.status=="passed" && !data.validation.checked_at.isNullOrBlank() &&
        paragraphs.size in 1..3 && paragraphs.all{it.isNotBlank() && it.length<=6000} &&
        sections.size==5 && sections.map{it.pillar_id}.toSet()==pillarLabels.keys &&
        sections.all{!it.text.isNullOrBlank() && it.text.length<=6000 && validRefs(it.metric_ids) && it.metric_ids.orEmpty().all { id -> id in pillarMetricIds[it.pillar_id].orEmpty() }} &&
        data.strengths.orEmpty().size<=5 && data.risks.orEmpty().size<=5 &&
        (data.strengths.orEmpty()+data.risks.orEmpty()).all{!it.text.isNullOrBlank() && it.text.length<=3000 && validRefs(it.metric_ids)} &&
        !data.sources.isNullOrEmpty() && data.sources.all{safeSourceUrl(it.url)}
}
