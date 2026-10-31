package com.example.sp500auth.ui.compare

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.pillarLabels
import com.example.sp500auth.ui.pillars.scoreNumber
import java.math.BigDecimal
import java.time.LocalDate

data class ComparisonRow(
    val id: String, val label: String, val aValue: String, val bValue: String,
    val result: String, val delta: String?, val reason: String?, val higher: String?
)

/** Render server decisions; never select a winner from Android ratios or thresholds. */
fun comparisonRows(data: ComparisonResponse): List<ComparisonRow> = pillarLabels.map { (id, label) ->
    val a = data.a?.pillars.orEmpty().filterNotNull().singleOrNull { it.id == id }?.score
    val b = data.b?.pillars.orEmpty().filterNotNull().singleOrNull { it.id == id }?.score
    val decision = data.comparisons.orEmpty().filterNotNull().singleOrNull { it.id == id }
    val invalid = comparisonProblem(data, decision, a, b)
    val result = decision?.result.takeIf { invalid == null }
    ComparisonRow(id, label, scoreText(a), scoreText(b), when (result) {
        "a" -> "✓ Mayor nota: ${data.a?.ticker} (A)"
        "b" -> "✓ Mayor nota: ${data.b?.ticker} (B)"
        "tie" -> "= Empate"
        else -> "— No comparable"
    }, decision?.delta?.takeIf { result in setOf("a", "b", "tie") && decision.unit == "points" && it.toBigDecimalOrNull() != null },
        invalid,
        result?.takeIf { it in setOf("a", "b") })
}

private fun comparisonProblem(data: ComparisonResponse, decision: PillarComparison?, a: PillarScore?, b: PillarScore?): String? {
    if (data.status == "comparison_service_unavailable") return data.reason ?: "Servicio comparativo pendiente."
    if (decision?.status != "available") return decision?.reason ?: data.reason ?: "El servidor no proporcionó una comparación válida para este pilar."
    if (scoreNumber(a) == null || scoreNumber(b) == null) return a?.reason ?: b?.reason ?: "Falta una nota válida; no se sustituye por cero."
    val left = data.a ?: return "Falta la empresa A."
    val right = data.b ?: return "Falta la empresa B."
    if (left.fiscal_year == null || left.fiscal_year != right.fiscal_year || left.fiscal_year != data.fiscal_year)
        return "Los ejercicios no son equivalentes."
    if (!validPeriod(left.period) || !validPeriod(right.period) || left.period != right.period)
        return "Los periodos no son equivalentes; no se presenta un ganador."
    if (left.methodology_version.isNullOrBlank() || left.methodology_version != right.methodology_version)
        return "Las metodologías no son equivalentes."
    val version = left.scoring_policy?.version
    if (version.isNullOrBlank() || version != right.scoring_policy?.version || a?.version != version || b?.version != version ||
        a?.period != left.period || b?.period != right.period)
        return "Las notas no comparten versión de normalización o periodo."
    val delta = decision.delta?.toBigDecimalOrNull()
    if (decision.unit != "points" || delta == null || when (decision.result) {
            "a" -> delta <= BigDecimal.ZERO
            "b" -> delta >= BigDecimal.ZERO
            "tie" -> delta.compareTo(BigDecimal.ZERO) != 0
            else -> true
        }) return "La decisión comparativa del servidor está incompleta o es incoherente."
    return null
}

private fun validPeriod(period: FiscalPeriod?): Boolean = try {
    period?.start != null && period.end != null && LocalDate.parse(period.start) < LocalDate.parse(period.end)
} catch (_: Exception) { false }

private fun scoreText(score: PillarScore?) = if (scoreNumber(score) != null) "${score?.display_value} / 100" else "No disponible"

fun periodText(data: PillarsResponse?): String = data?.period?.let { period ->
    if (period.start.isNullOrBlank() || period.end.isNullOrBlank()) "Periodo no informado" else "${period.start} a ${period.end}"
} ?: "Periodo no informado"
