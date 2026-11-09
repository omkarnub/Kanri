package com.omkarnub.kanri.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class HeatmapQuantizationTest {

    @Test
    fun testFiveLevelPercentileBucketing() {
        val dailySpends = listOf(0.0, 100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 50000.0) // 50k outlier
        val nonZeroSpends = dailySpends.filter { it > 0.0 }.sorted()

        val p25 = nonZeroSpends[(nonZeroSpends.size * 0.25).toInt()]
        val p50 = nonZeroSpends[(nonZeroSpends.size * 0.50).toInt()]
        val p75 = nonZeroSpends[(nonZeroSpends.size * 0.75).toInt()]

        fun quantize(amt: Double): Float = when {
            amt <= 0.0 -> 0f
            amt < p25 -> 0.25f
            amt < p50 -> 0.50f
            amt < p75 -> 0.75f
            else -> 1.0f
        }

        assertEquals(0f, quantize(0.0), 0.001f)
        assertEquals(0.25f, quantize(100.0), 0.001f)
        assertEquals(0.50f, quantize(350.0), 0.001f)
        assertEquals(0.75f, quantize(550.0), 0.001f)
        assertEquals(1.0f, quantize(50000.0), 0.001f)
    }
}

