package com.omkarnub.kanri.data.budget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BudgetCalculatorTest {

    @Test
    fun testCalculateRemaining() {
        val remaining = BudgetCalculator.calculateRemaining(20000.0, 4500.0)
        assertEquals(15500.0, remaining, 0.001)
    }

    @Test
    fun testCalculateRemaining_OverBudget() {
        val remaining = BudgetCalculator.calculateRemaining(20000.0, 23500.0)
        assertEquals(-3500.0, remaining, 0.001)
    }

    @Test
    fun testCalculateSafeToSpendPerDay() {
        // ₹15,000 remaining with 15 days left = ₹1,000/day
        val safe = BudgetCalculator.calculateSafeToSpendPerDay(20000.0, 5000.0, 15)
        assertEquals(1000.0, safe, 0.001)
    }

    @Test
    fun testCalculateSafeToSpendPerDay_OverBudget() {
        // When over budget, safe to spend per day is 0.0
        val safe = BudgetCalculator.calculateSafeToSpendPerDay(20000.0, 25000.0, 10)
        assertEquals(0.0, safe, 0.001)
    }

    @Test
    fun testCalculatePercentRemaining() {
        val percent = BudgetCalculator.calculatePercentRemaining(20000.0, 5000.0)
        assertEquals(0.75f, percent, 0.001f)
    }

    @Test
    fun testCalculatePercentUsed() {
        val percent = BudgetCalculator.calculatePercentUsed(20000.0, 5000.0)
        assertEquals(0.25f, percent, 0.001f)
    }

    @Test
    fun testCurrentMonthKey() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 17)
        }
        val key = BudgetCalculator.getCurrentMonthKey(cal)
        assertEquals("2026-09", key)
    }

    @Test
    fun testDaysRemainingInMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER) // September has 30 days
            set(Calendar.DAY_OF_MONTH, 17)
        }
        val days = BudgetCalculator.getDaysRemainingInMonth(cal)
        // 30 - 17 + 1 = 14 days remaining
        assertEquals(14, days)
    }
}
