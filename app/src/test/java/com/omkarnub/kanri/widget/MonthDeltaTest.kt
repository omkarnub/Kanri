package com.omkarnub.kanri.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class MonthDeltaTest {

    @Test
    fun testSpendingIncrease() {
        val currentMonthToDate = 12000.0
        val previousMonthToDate = 10000.0

        val diff = currentMonthToDate - previousMonthToDate
        val isSpendingMore = diff > 0
        val deltaPercent = (abs(diff) / max(previousMonthToDate, 1.0) * 100.0).toFloat()

        assertTrue(isSpendingMore)
        assertEquals(20.0f, deltaPercent, 0.001f)
    }

    @Test
    fun testSpendingDecrease() {
        val currentMonthToDate = 8000.0
        val previousMonthToDate = 10000.0

        val diff = currentMonthToDate - previousMonthToDate
        val isSpendingMore = diff > 0
        val deltaPercent = (abs(diff) / max(previousMonthToDate, 1.0) * 100.0).toFloat()

        assertFalse(isSpendingMore)
        assertEquals(20.0f, deltaPercent, 0.001f)
    }

    @Test
    fun testDay31ClampedForFebruary() {
        val currentDay = 31
        val maxDaysFeb = 28
        val compareDay = currentDay.coerceAtMost(maxDaysFeb)

        assertEquals(28, compareDay)
    }
}

