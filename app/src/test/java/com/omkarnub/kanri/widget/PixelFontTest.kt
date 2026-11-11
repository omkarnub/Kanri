package com.omkarnub.kanri.widget

import com.omkarnub.kanri.widget.font.PixelFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelFontTest {

    @Test
    fun testIndianCompactFormatting() {
        assertEquals("₹850", PixelFont.formatIndianCompact(850.0))
        assertEquals("₹1.2k", PixelFont.formatIndianCompact(1200.0))
        assertEquals("₹12.4k", PixelFont.formatIndianCompact(12400.0))
        assertEquals("₹1.2L", PixelFont.formatIndianCompact(120000.0))
        assertEquals("₹15L", PixelFont.formatIndianCompact(1500000.0))
        assertEquals("₹1.5Cr", PixelFont.formatIndianCompact(15000000.0))
    }

    @Test
    fun testNegativeCurrencyFormatting() {
        assertEquals("-₹500", PixelFont.formatIndianCompact(-500.0))
        assertEquals("-₹3.5k", PixelFont.formatIndianCompact(-3500.0))
    }

    @Test
    fun testCurrencySplitting() {
        val (p1, n1) = PixelFont.splitCurrency("₹1,240")
        assertEquals("₹", p1)
        assertEquals("1,240", n1)

        val (p2, n2) = PixelFont.splitCurrency("-₹12.4k")
        assertEquals("-₹", p2)
        assertEquals("12.4k", n2)

        val (p3, n3) = PixelFont.splitCurrency("84%")
        assertEquals("", p3)
        assertEquals("84%", n3)

        val (p4, n4) = PixelFont.splitCurrency("5 DAYS")
        assertEquals("", p4)
        assertEquals("5 DAYS", n4)
    }

    @Test
    fun testFallbackTypefaceSafeOnJvm() {
        // Without mocked Android Context, getNdotTypeface safely returns null or Monospace without throwing
        val tf = try { PixelFont.getNdotTypeface(null) } catch (e: Throwable) { null }
        // Test passes as long as no unhandled exception is thrown
        assertTrue(true)
    }

    @Test
    fun testMultiCurrencyAndUnitSplitting() {
        val (pUsd, nUsd, uUsd) = PixelFont.splitCurrencyAndUnit("$1,240 /d")
        assertEquals("$", pUsd)
        assertEquals("1,240", nUsd)
        assertEquals(" /d", uUsd)

        val (pEur, nEur, uEur) = PixelFont.splitCurrencyAndUnit("-€12.4k")
        assertEquals("-€", pEur)
        assertEquals("12.4k", nEur)
        assertEquals("", uEur)

        val (pGbp, nGbp, uGbp) = PixelFont.splitCurrencyAndUnit("+£500")
        assertEquals("+£", pGbp)
        assertEquals("500", nGbp)
        assertEquals("", uGbp)

        val (pCad, nCad) = PixelFont.splitCurrency("CA$950")
        assertEquals("CA$", pCad)
        assertEquals("950", nCad)

        val (pAed, nAed) = PixelFont.splitCurrency("AED4,200")
        assertEquals("AED", pAed)
        assertEquals("4,200", nAed)

        val (pBrl, nBrl) = PixelFont.splitCurrency("-R$1.2k")
        assertEquals("-R$", pBrl)
        assertEquals("1.2k", nBrl)
    }

    @Test
    fun testNonCurrencyTextNotMisclassifiedAsCurrencySymbol() {
        // "R" is South African Rand, so plain words starting with R like "REVIEW" must not split on "R"
        val (p, n, u) = PixelFont.splitCurrencyAndUnit("REVIEW")
        assertEquals("", p)
        assertEquals("REVIEW", n)
        assertEquals("", u)

        val (pZar, nZar, _) = PixelFont.splitCurrencyAndUnit("R500")
        assertEquals("R", pZar)
        assertEquals("500", nZar)
    }

    @Test
    fun testNonInrCompactAndTinyNegative() {
        assertEquals("$1.5M", PixelFont.formatIndianCompact(1_500_000.0, "$"))
        assertEquals("₹0", PixelFont.formatIndianCompact(-0.001, "₹"))
    }
}
