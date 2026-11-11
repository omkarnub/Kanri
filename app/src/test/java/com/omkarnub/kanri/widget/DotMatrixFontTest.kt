package com.omkarnub.kanri.widget

import com.omkarnub.kanri.widget.font.DotMatrixFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DotMatrixFontTest {

    @Test
    fun testIndianCompactNumberFormatting() {
        assertEquals("₹850", DotMatrixFont.formatIndianCompact(850.0))
        assertEquals("₹1.2k", DotMatrixFont.formatIndianCompact(1200.0))
        assertEquals("₹12.4k", DotMatrixFont.formatIndianCompact(12400.0))
        assertEquals("₹1.2L", DotMatrixFont.formatIndianCompact(120000.0))
        assertEquals("₹15L", DotMatrixFont.formatIndianCompact(1500000.0))
        assertEquals("₹1.5Cr", DotMatrixFont.formatIndianCompact(15000000.0))
    }

    @Test
    fun testNegativeNumberFormatting() {
        assertEquals("-₹500", DotMatrixFont.formatIndianCompact(-500.0))
        assertEquals("-₹3.5k", DotMatrixFont.formatIndianCompact(-3500.0))
    }

    @Test
    fun testTabularMonospaceMeasurement() {
        // Tabular digits: "1234" and "8888" should have identical widths
        val size1 = DotMatrixFont.measure("1234", dotPitch = 10f)
        val size2 = DotMatrixFont.measure("8888", dotPitch = 10f)
        assertEquals(size1.width, size2.width, 0.001f)
        assertEquals(size1.height, size2.height, 0.001f)
    }

    @Test
    fun testFittingPitch() {
        val pitch = DotMatrixFont.findFittingPitch("₹12.4k", availableWidth = 200f, maxPitch = 14f, minPitch = 7f)
        assertTrue(pitch in 7f..14f)
    }

    @Test
    fun testNonInrCompactFormatting() {
        assertEquals("$850", DotMatrixFont.formatIndianCompact(850.0, "$"))
        assertEquals("$1.2k", DotMatrixFont.formatIndianCompact(1200.0, "$"))
        assertEquals("$120k", DotMatrixFont.formatIndianCompact(120000.0, "$"))
        assertEquals("€1.5M", DotMatrixFont.formatIndianCompact(1500000.0, "€"))
        assertEquals("£1.5B", DotMatrixFont.formatIndianCompact(1500000000.0, "£"))
        assertEquals("-CA$4.5k", DotMatrixFont.formatIndianCompact(-4500.0, "CA$"))
    }

    @Test
    fun testTinyNegativeAndNaNGuards() {
        assertEquals("₹0", DotMatrixFont.formatIndianCompact(-0.001, "₹"))
        assertEquals("$0", DotMatrixFont.formatIndianCompact(-0.2, "$"))
        assertEquals("$0", DotMatrixFont.formatIndianCompact(Double.NaN, "$"))
        assertEquals("₹0", DotMatrixFont.formatIndianCompact(Double.POSITIVE_INFINITY, "₹"))
    }

    @Test
    fun testDefaultPrefixTracksActiveCurrency() {
        try {
            com.omkarnub.kanri.util.CurrencyUtils.setCurrency(com.omkarnub.kanri.util.AppCurrency.USD)
            assertEquals("$1.5M", DotMatrixFont.formatIndianCompact(1_500_000.0))
        } finally {
            com.omkarnub.kanri.util.CurrencyUtils.setCurrency(com.omkarnub.kanri.util.AppCurrency.INR)
        }
    }
}

