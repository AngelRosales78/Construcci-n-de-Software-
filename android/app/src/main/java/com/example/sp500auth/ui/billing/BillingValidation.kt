package com.example.sp500auth.ui.billing

import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** HU11_01: client-side checks that mirror the server. The server validates again and decides. */
fun cardDigits(input: String): String = input.filter { it.isDigit() }.take(19)

fun luhnValid(digits: String): Boolean {
    if (digits.length !in 13..19 || !digits.all { it.isDigit() }) return false
    val sum = digits.reversed().mapIndexed { index, char ->
        val digit = char.digitToInt()
        if (index % 2 == 1) (digit * 2).let { if (it > 9) it - 9 else it } else digit
    }.sum()
    return sum % 10 == 0
}

fun cardBrand(digits: String): String = when {
    digits.startsWith("4") -> "Visa"
    digits.take(2) in setOf("34", "37") -> "American Express"
    digits.length >= 2 && digits.take(2).toInt() in 51..55 -> "Mastercard"
    digits.length >= 4 && digits.take(4).toInt() in 2221..2720 -> "Mastercard"
    else -> "Tarjeta"
}

/** Groups of four (4-6-5 for American Express) so the number is easy to read back. */
fun formatCardNumber(digits: String): String {
    val groups = if (digits.take(2) in setOf("34", "37")) listOf(4, 6, 5) else List(5) { 4 }
    val parts = mutableListOf<String>(); var index = 0
    for (size in groups) { if (index >= digits.length) break; parts += digits.substring(index, minOf(digits.length, index + size)); index += size }
    if (index < digits.length) parts += digits.substring(index)
    return parts.joinToString(" ")
}

/** Accepts MM/AA or MM/AAAA. Returns null when the format is not a real month. */
fun parseExpiry(input: String): YearMonth? {
    val match = Regex("""^\s*(\d{1,2})\s*/\s*(\d{2}|\d{4})\s*$""").find(input) ?: return null
    val month = match.groupValues[1].toInt(); val raw = match.groupValues[2].toInt()
    if (month !in 1..12) return null
    return YearMonth.of(if (raw < 100) 2000 + raw else raw, month)
}

fun cvcLength(digits: String) = if (digits.take(2) in setOf("34", "37")) 4 else 3

data class CardErrors(val number: String? = null, val holder: String? = null, val expiry: String? = null, val cvc: String? = null) {
    val valid get() = number == null && holder == null && expiry == null && cvc == null
}

fun cardErrors(number: String, holder: String, expiry: String, cvc: String, today: YearMonth = YearMonth.now()): CardErrors {
    val digits = cardDigits(number); val month = parseExpiry(expiry)
    return CardErrors(
        number = if (!luhnValid(digits)) "Número de tarjeta no válido." else null,
        holder = if (holder.trim().length !in 3..80) "Escribe el nombre como figura en la tarjeta." else null,
        expiry = when { month == null -> "Usa el formato MM/AA."; month < today -> "La tarjeta está vencida."
            month > today.plusYears(20) -> "Fecha no válida."; else -> null },
        cvc = if (!Regex("""\d{${cvcLength(digits)}}""").matches(cvc)) "Código de ${cvcLength(digits)} dígitos." else null)
}

private val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es"))
fun displayDate(iso: String?): String = runCatching { dateFormat.format(Instant.parse(iso).atZone(ZoneId.systemDefault())) }.getOrDefault("Fecha no disponible")

/** HU11_04: remaining share of the current billing period, 0..1, and whole days left. */
fun periodProgress(start: String?, end: String?, now: Instant = Instant.now()): Pair<Float, Long>? = runCatching {
    val from = Instant.parse(start); val to = Instant.parse(end)
    val total = Duration.between(from, to).toMillis().toFloat()
    val left = Duration.between(now, to)
    if (total <= 0f) null else ((left.toMillis() / total).coerceIn(0f, 1f) to maxOf(0L, left.toDays()))
}.getOrNull()

val cancelReasons = linkedMapOf(
    "price" to "El precio es alto", "not_using" to "No lo uso lo suficiente",
    "missing_features" to "Faltan funciones que necesito", "temporary" to "Es una pausa temporal", "other" to "Otro motivo")

/**
 * Shows the raw digits in groups without changing the stored value, so the cursor never jumps
 * when a separator appears (typing "424242" shows "4242 42", not "4242 24").
 */
class CardNumberTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text
        val formatted = formatCardNumber(digits)
        // Positions in the raw text after which a space is shown.
        val breaks = formatted.mapIndexedNotNull { index, char -> if (char == ' ') index - formatted.take(index).count { it == ' ' } else null }
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = offset + breaks.count { it < offset }
            override fun transformedToOriginal(offset: Int) =
                (offset - formatted.take(offset).count { it == ' ' }).coerceIn(0, digits.length)
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(formatted), mapping)
    }
}

/** Expiry is typed as digits only (MMAA); the "/" is drawn automatically after the month. */
fun expiryDigits(input: String): String = input.filter { it.isDigit() }.take(4)
fun expiryText(digits: String): String = if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits

class ExpiryTransformation : androidx.compose.ui.text.input.VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val digits = text.text
        val slash = digits.length > 2
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (slash && offset > 2) offset + 1 else offset
            override fun transformedToOriginal(offset: Int) = (if (slash && offset > 2) offset - 1 else offset).coerceIn(0, digits.length)
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(expiryText(digits)), mapping)
    }
}
