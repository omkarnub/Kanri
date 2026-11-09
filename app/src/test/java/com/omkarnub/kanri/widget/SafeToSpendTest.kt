package com.omkarnub.kanri.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

class SafeToSpendTest {

    @Test
    fun testAllowanceFormula() {
        val remainingBudget = 15000.0
        val daysInMonth = 30
        val currentDay = 16 // 15 days remaining incl. today
        val remainingDays = max(1, daysInMonth - currentDay + 1)

        val dailyAllowance = remainingBudget / remainingDays
        assertEquals(1000.0, dailyAllowance, 0.001)
    }

    @Test
    fun testAllowanceThresholdCrossed() {
        val dailyAllowance = 1000.0
        val todaySpentNormal = 750.0
        val todaySpentOver = 1250.0

        val normalRatio = (todaySpentNormal / dailyAllowance).toFloat()
        val overRatio = (todaySpentOver / dailyAllowance).toFloat()

        assertTrue(normalRatio < 1.0f)
        assertTrue(overRatio > 1.0f)
    }

    @Test
    fun testAllowanceOnLastDayOfMonth() {
        val remainingBudget = 500.0
        val daysInMonth = 31
        val currentDay = 31
        val remainingDays = max(1, daysInMonth - currentDay + 1)

        assertEquals(1, remainingDays)
        val dailyAllowance = remainingBudget / remainingDays
        assertEquals(500.0, dailyAllowance, 0.001)
    }
}
