package com.example.sp500auth.ui.compare

import com.example.sp500auth.data.api.models.*
import com.example.sp500auth.ui.pillars.pillarLabels
import com.example.sp500auth.ui.pillars.scoreNumber

data class ComparisonRow(
    val id: String, val label: String, val aValue: String, val bValue: String,
    val result: String, val delta: String?, val reason: String?, val higher: String?
)

/** Render server decisions; never select a winner from Android ratios or thresholds. */
fun comparisonRows(data: ComparisonResponse): List<ComparisonRow> = pillarLabels.map { (id, label) ->
    val a = data.a?.pillars.orEmpty().singleOrNull { it.id == id }?.score
    val b = data.b?.pillars.orEmpty().singleOrNull { it.id == id }?.score
    val decision = data.comparisons.orEmpty().singleOrNull { it.id == id }
    val result = decision?.result.takeIf { decision?.status == "available" && scoreNumber(a) != null && scoreNumber(b) != null }
    ComparisonRow(id, label, scoreText(a), scoreText(b), when (result) {
        "a" -> "✓ Mayor nota: ${data.a?.ticker} (A)"
        "b" -> "✓ Mayor nota: ${data.b?.ticker} (B)"
        "tie" -> "= Empate"
        else -> "— No comparable"
    }, decision?.delta?.takeIf { result in setOf("a", "b", "tie") && decision.unit == "points" && it.toBigDecimalOrNull() != null },
        if (result in setOf("a", "b", "tie")) null else decision?.reason ?: data.reason ?: "El servidor no proporcionó una comparación válida para este pilar.",
        result?.takeIf { it in setOf("a", "b") })
}

private fun scoreText(score: PillarScore?) = if (scoreNumber(score) != null) "${score?.display_value} / 100" else "No disponible"

fun periodText(data: PillarsResponse?): String = data?.period?.let { period ->
    if (period.start.isNullOrBlank() || period.end.isNullOrBlank()) "Periodo no informado" else "${period.start} a ${period.end}"
} ?: "Periodo no informado"
