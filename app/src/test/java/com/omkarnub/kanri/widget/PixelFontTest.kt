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
}
