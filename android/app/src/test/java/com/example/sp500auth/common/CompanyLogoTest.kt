package com.example.sp500auth.common

import com.example.sp500auth.ui.common.companyLogoUrl
import com.example.sp500auth.ui.common.tickerInitials
import org.junit.Assert.*
import org.junit.Test

class CompanyLogoTest {
    @Test fun serverLogoWinsAndTickerFallbackIsSafe() {
        assertEquals("https://cdn.example.test/a.png", companyLogoUrl("AAPL", "https://cdn.example.test/a.png"))
        assertEquals("https://financialmodelingprep.com/image-stock/AAPL.png", companyLogoUrl("aapl", ""))
        assertEquals("https://financialmodelingprep.com/image-stock/BRK-B.png", companyLogoUrl("BRK.B"))
        // Plain-http or odd server values are ignored; invalid tickers never build a URL.
        assertEquals("https://financialmodelingprep.com/image-stock/MSFT.png", companyLogoUrl("MSFT", "http://insecure.test/x.png"))
        assertNull(companyLogoUrl("A/../B"))
        assertEquals("BRKB", tickerInitials("BRK.B"))
        assertEquals("?", tickerInitials(""))
    }
}
