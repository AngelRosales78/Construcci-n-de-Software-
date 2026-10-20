package com.example.sp500auth.ui.pillars

import com.example.sp500auth.data.api.models.PillarMetric
import com.example.sp500auth.data.api.models.SectorReference

/** Display server rounding verbatim: the Android client never calculates financial ratios. */
fun metricValue(metric: PillarMetric?): String {
    if (metric == null) return "No disponible"
    if (metric.status == "not_applicable") return "No aplica"
    if (metric.status != "available" || metric.value?.toBigDecimalOrNull() == null ||
        metric.display_value.isNullOrBlank()) return "No disponible"
    val suffix = when (metric.unit) {
        "times" -> " veces"
        "percent" -> " %"
        else -> metric.unit?.let { " $it" } ?: ""
    }
    return metric.display_value + suffix
}

fun reasonLabel(reason: String?): String = when (reason) {
    "missing_components" -> "Faltan componentes del balance."
    "zero_denominator" -> "El denominador es cero; el indicador no se puede calcular."
    "nonpositive_equity" -> "El patrimonio es cero o negativo; el ROE no es interpretable."
    "insufficient_history" -> "No hay suficientes ejercicios comparables."
    "insufficient_sample" -> "La muestra sectorial es insuficiente."
    null, "" -> "El servidor no proporcionó un motivo."
    else -> reason
}

fun hasComparableSector(reference: SectorReference?): Boolean = reference != null &&
    reference.status == "available" && reference.value?.toBigDecimalOrNull() != null &&
    !reference.display_value.isNullOrBlank() && reference.unit == "percent" &&
    (reference.sample_size ?: 0) > 0 && !reference.universe.isNullOrBlank() &&
    !reference.as_of.isNullOrBlank() && !reference.period?.start.isNullOrBlank() &&
    !reference.period?.end.isNullOrBlank() && !reference.source?.url.isNullOrBlank()
