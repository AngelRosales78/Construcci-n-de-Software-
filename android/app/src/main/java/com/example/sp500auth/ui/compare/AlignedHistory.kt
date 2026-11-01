package com.example.sp500auth.ui.compare

import com.example.sp500auth.data.api.models.PriceSeries
import java.time.LocalDate

val historyRanges = listOf("1M", "6M", "1Y", "5Y")
data class ReturnPoint(val date: String, val epochDay: Long, val a: Double, val b: Double, val aPrice: Double, val bPrice: Double)
data class AlignedHistory(val a: String, val b: String, val range: String, val points: List<ReturnPoint>,
    val sourcesA: List<String>, val sourcesB: List<String>, val currenciesA: List<String>, val currenciesB: List<String>, val warnings: List<String>)

/** Common observed dates only. No interpolation, currency conversion or invented closes. */
fun alignHistory(a: PriceSeries, b: PriceSeries): AlignedHistory {
    require(validTicker(a.ticker.orEmpty()) && validTicker(b.ticker.orEmpty()) && a.ticker != b.ticker) { "Las empresas no son válidas o están repetidas." }
    require(a.range in historyRanges && a.range == b.range) { "Los rangos históricos no coinciden." }
    require(a.is_demo != true && b.is_demo != true) { "No se comparan históricos de demostración como datos reales." }
    fun prices(series: PriceSeries): Map<LocalDate, Double> {
        val map = sortedMapOf<LocalDate, Double>()
        for (point in series.history.orEmpty()) {
            requireNotNull(point) { "El histórico contiene un cierre vacío." }
            val date = try { LocalDate.parse(point.date) } catch (_: Exception) { throw IllegalArgumentException("El histórico contiene una fecha inválida.") }
            val price = point.price
            require(price != null && price.isFinite() && price > 0) { "El histórico contiene un precio ausente, cero o negativo." }
            require(map.put(date, price) == null) { "El histórico contiene fechas duplicadas." }
        }
        return map
    }
    val left = prices(a); val right = prices(b)
    val dates = left.keys.intersect(right.keys).sorted()
    require(dates.size >= 2) { "Se necesitan al menos dos fechas comunes con precios válidos." }
    val baseA = left.getValue(dates.first()); val baseB = right.getValue(dates.first())
    val points = dates.map { date ->
        val pa = left.getValue(date); val pb = right.getValue(date)
        val ra = (pa / baseA - 1) * 100; val rb = (pb / baseB - 1) * 100
        require(ra.isFinite() && rb.isFinite()) { "No se puede normalizar este histórico." }
        ReturnPoint(date.toString(), date.toEpochDay(), ra, rb, pa, pb)
    }
    val warnings = mutableListOf<String>()
    if (dates.size < left.size || dates.size < right.size) warnings += "Se usan solo ${dates.size} fechas comunes. No se rellenan huecos."
    if (dates.size < 20) warnings += "El intervalo común contiene pocos cierres (${dates.size})."
    if (a.is_demo == null || b.is_demo == null) warnings += "El servidor no informa si toda la serie está verificada."
    if (a.sources.isNullOrEmpty() || b.sources.isNullOrEmpty()) warnings += "Falta la fuente de alguna serie."
    warnings += "Variación en la moneda de cada activo, sin conversión de divisas. No incluye una reinversión de dividendos calculada por la app."
    return AlignedHistory(a.ticker!!, b.ticker!!, a.range!!, points, a.sources.orEmpty(), b.sources.orEmpty(), a.currencies.orEmpty(), b.currencies.orEmpty(), warnings)
}

fun nearestHistoryIndex(points: List<ReturnPoint>, fraction: Float): Int {
    if (points.size < 2) return 0
    val day = points.first().epochDay + (points.last().epochDay - points.first().epochDay) * fraction.coerceIn(0f, 1f)
    var low = 0; var high = points.lastIndex
    while (low < high) { val mid = (low + high) / 2; if (points[mid].epochDay < day) low = mid + 1 else high = mid }
    return if (low > 0 && day - points[low - 1].epochDay <= points[low].epochDay - day) low - 1 else low
}
