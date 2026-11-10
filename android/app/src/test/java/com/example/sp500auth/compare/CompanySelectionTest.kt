package com.example.sp500auth.compare

import com.example.sp500auth.ui.compare.*
import org.junit.Assert.*
import org.junit.Test

class CompanySelectionTest {
    @Test fun requiresTwoDistinctCompanies() {
        val a = CompanySelection().select(CompanySlot.A, "aapl", "Apple")!!
        assertFalse(a.canCompare)
        assertNull(a.select(CompanySlot.B, " AAPL ", "Apple"))
        assertTrue(a.select(CompanySlot.B, "MSFT", "Microsoft")!!.canCompare)
    }
    @Test fun swappingKeepsNamesAttachedAndRemovalClearsReadiness() {
        val pair = CompanySelection("AAPL", "Apple", "MSFT", "Microsoft")
        assertEquals(CompanySelection("MSFT", "Microsoft", "AAPL", "Apple"), pair.swap())
        assertFalse(pair.remove(CompanySlot.A).canCompare)
        assertEquals("", pair.remove(CompanySlot.B).bName)
    }
    @Test fun rejectsInvalidIdentifiersButAllowsDottedTicker() {
        for (ticker in listOf("", "../AAPL", "<MSFT>", "A A")) assertNull(CompanySelection().select(CompanySlot.A, ticker, ticker))
        assertNotNull(CompanySelection().select(CompanySlot.A, "BRK.B", "Berkshire"))
    }
}
