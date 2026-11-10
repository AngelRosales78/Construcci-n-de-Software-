package com.example.sp500auth.ui.compare

import java.util.Locale

enum class CompanySlot { A, B }

data class CompanySelection(
    val a: String = "", val aName: String = "",
    val b: String = "", val bName: String = ""
) {
    val canCompare: Boolean get() = validTicker(a) && validTicker(b) && !a.equals(b, true)

    fun select(slot: CompanySlot, ticker: String, name: String): CompanySelection? {
        val normalized = ticker.trim().uppercase(Locale.ROOT)
        val other = if (slot == CompanySlot.A) b else a
        if (!validTicker(normalized) || normalized.equals(other, true)) return null
        return if (slot == CompanySlot.A) copy(a = normalized, aName = name)
        else copy(b = normalized, bName = name)
    }
    fun remove(slot: CompanySlot) = if (slot == CompanySlot.A) copy(a = "", aName = "") else copy(b = "", bName = "")
    fun swap() = CompanySelection(b, bName, a, aName)
}

fun validTicker(ticker: String) = ticker.matches(Regex("[A-Z0-9][A-Z0-9.\\-]{0,14}"))
