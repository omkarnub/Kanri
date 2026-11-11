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

    @Test
    fun testCycleStartDay25_whenPastCycleStart() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 28)
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val startCal = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getStartOfCycleMillis(25, cal)
        }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCycleStartDay25_whenBeforeCycleStart() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.NOVEMBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 10)
        }
        val startCal = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getStartOfCycleMillis(25, cal)
        }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(25, startCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDefaultMonthlyBudgetForCurrencies() {
        assertEquals(25000.0, BudgetCalculator.getDefaultMonthlyBudget(com.omkarnub.kanri.util.AppCurrency.INR), 0.001)
        assertEquals(2500.0, BudgetCalculator.getDefaultMonthlyBudget(com.omkarnub.kanri.util.AppCurrency.USD), 0.001)
        assertEquals(300000.0, BudgetCalculator.getDefaultMonthlyBudget(com.omkarnub.kanri.util.AppCurrency.JPY), 0.001)
    }

    @Test
    fun testCycleStartDay31_inFebruaryLeapAndNonLeapYear() {
        // Leap year: 2024-02-29 with cycleStartDay 31 -> clamps start to Feb 29, ends Mar 30 23:59:59.999
        val leapCal = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 29, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val leapStart = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getStartOfCycleMillis(31, leapCal)
        }
        val leapEnd = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getEndOfCycleMillis(31, leapCal)
        }
        assertEquals(2024, leapStart.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, leapStart.get(Calendar.MONTH))
        assertEquals(29, leapStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(2024, leapEnd.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, leapEnd.get(Calendar.MONTH))
        assertEquals(30, leapEnd.get(Calendar.DAY_OF_MONTH))

        // Non-leap year: 2026-02-15 with cycleStartDay 31 -> started Jan 31, ends Feb 27 23:59:59.999
        val nonLeapCal = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nonLeapStart = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getStartOfCycleMillis(31, nonLeapCal)
        }
        val nonLeapEnd = Calendar.getInstance().apply {
            timeInMillis = BudgetCalculator.getEndOfCycleMillis(31, nonLeapCal)
        }
        assertEquals(Calendar.JANUARY, nonLeapStart.get(Calendar.MONTH))
        assertEquals(31, nonLeapStart.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FEBRUARY, nonLeapEnd.get(Calendar.MONTH))
        assertEquals(27, nonLeapEnd.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDaysRemainingInCycle_dstTransitionsReturnExactCalendarDays() {
        val nyTz = java.util.TimeZone.getTimeZone("America/New_York")

        // Spring-forward DST is Sunday March 8, 2026 (23-hour day).
        // On Mar 7 23:30 with cycleStartDay 10 (ends Mar 9 23:59:59.999): Mar 7, Mar 8, Mar 9 = 3 days.
        val springCal = Calendar.getInstance(nyTz).apply {
            set(2026, Calendar.MARCH, 7, 23, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(3, BudgetCalculator.getDaysRemainingInCycle(10, springCal))

        // Fall-back DST is Sunday November 1, 2026 (25-hour day).
        // On Oct 31 00:30 with cycleStartDay 5 (ends Nov 4 23:59:59.999): Oct 31, Nov 1, Nov 2, Nov 3, Nov 4 = 5 days.
        val fallCal = Calendar.getInstance(nyTz).apply {
            set(2026, Calendar.OCTOBER, 31, 0, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(5, BudgetCalculator.getDaysRemainingInCycle(5, fallCal))
    }
}
