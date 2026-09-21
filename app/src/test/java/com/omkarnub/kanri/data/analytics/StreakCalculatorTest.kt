package com.omkarnub.kanri.data.analytics

import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakCalculatorTest {

    private fun createTx(amount: Double, type: String, year: Int, month: Int, day: Int): TransactionEntity {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TransactionEntity(
            id = 0,
            type = type,
            amount = amount,
            sourceType = "UPI",
            counterparty = "Merchant",
            bank = "HDFC",
            refNo = "REF${cal.timeInMillis}",
            timestamp = cal.timeInMillis,
            rawSms = "SMS"
        )
    }

    @Test
    fun calculateStreak_emptyTransactions_returnsZeroStateFirstUse() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val state = StreakCalculator.calculateStreak(
            transactions = emptyList(),
            nowCalendar = nowCal
        )

        assertEquals(0, state.currentStreak)
        assertEquals(0, state.lastStreak)
        assertEquals(0, state.bestStreak)
        assertEquals(0, state.noSpendDaysThisMonth)
        assertTrue(state.isFirstUse)
        assertFalse(state.isStreakActive)
        assertEquals(7, state.last7Days.size)
        // All days in 7-day strip should not be marked as no-spend because user hasn't started yet
        assertTrue(state.last7Days.none { it.isNoSpend })
    }

    @Test
    fun calculateStreak_noSpendToday_countsTodayInCurrentStreak() {
        // Today is Sep 20, 2026.
        // First transaction on Sep 10 (DEBIT).
        // Spend on Sep 17.
        // Sep 18, 19, 20: No spend -> streak = 3.
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(500.0, "DEBIT", 2026, 9, 10),
            createTx(200.0, "DEBIT", 2026, 9, 17)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(3, state.currentStreak)
        assertEquals(3, state.lastStreak)
        assertTrue(state.isStreakActive)
        assertFalse(state.isFirstUse)
    }

    @Test
    fun calculateStreak_spendToday_currentStreakIsZero_lastStreakPreserved() {
        // Today is Sep 20, 2026.
        // First transaction on Sep 10 (DEBIT).
        // Spend on Sep 15.
        // Sep 16, 17, 18, 19: No spend (streak of 4).
        // Spend on Sep 20 (today): current streak becomes 0, last streak = 4.
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(500.0, "DEBIT", 2026, 9, 10),
            createTx(300.0, "DEBIT", 2026, 9, 15),
            createTx(150.0, "DEBIT", 2026, 9, 20)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(0, state.currentStreak)
        assertEquals(4, state.lastStreak)
        assertFalse(state.isStreakActive)
        assertFalse(state.isFirstUse)
    }

    @Test
    fun calculateStreak_incomeToday_doesNotBreakStreak() {
        // Today is Sep 20, 2026.
        // First transaction on Sep 10.
        // Spend on Sep 18.
        // Sep 19: No spend.
        // Sep 20: Income of 10,000 (CREDIT) -> Streak should include today: 2 days (Sep 19, Sep 20).
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(500.0, "DEBIT", 2026, 9, 10),
            createTx(100.0, "DEBIT", 2026, 9, 18),
            createTx(10000.0, "CREDIT", 2026, 9, 20)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(2, state.currentStreak)
        assertTrue(state.isStreakActive)
    }

    @Test
    fun calculateStreak_firstUse_daysBeforeFirstTransactionNotCounted() {
        // First transaction was 2 days ago (Sep 18).
        // Sep 18 had a spend.
        // Sep 19 had no spend.
        // Sep 20 had no spend.
        // Streak = 2 (Sep 19, 20).
        // Days before Sep 18 must NOT be counted as no-spend.
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(500.0, "DEBIT", 2026, 9, 18)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(2, state.currentStreak)
        // Best streak is also 2
        assertEquals(2, state.bestStreak)
        // In the 7-day strip, days before Sep 18 should be isNoSpend = false
        val day17 = state.last7Days.find {
            val cal = Calendar.getInstance().apply { timeInMillis = it.dateEpochDay * 24 * 60 * 60 * 1000L }
            cal.get(Calendar.DAY_OF_MONTH) == 17
        }
        // Day 17 is before first transaction on Day 18
        assertEquals(false, day17?.isNoSpend)
    }

    @Test
    fun calculateStreak_bestStreak_computedAcrossAllTime() {
        // Historical:
        // First tx: Sep 1.
        // Spend on Sep 1.
        // No spend: Sep 2 to Sep 8 (7 days streak).
        // Spend on Sep 9.
        // No spend: Sep 10 to Sep 13 (4 days streak).
        // Spend on Sep 14.
        // No spend: Sep 15 to Sep 17 (3 days streak).
        // Spend on Sep 18.
        // No spend: Sep 19, 20 (2 days current streak).
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(100.0, "DEBIT", 2026, 9, 1),
            createTx(100.0, "DEBIT", 2026, 9, 9),
            createTx(100.0, "DEBIT", 2026, 9, 14),
            createTx(100.0, "DEBIT", 2026, 9, 18)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(2, state.currentStreak)
        assertEquals(7, state.bestStreak) // Sep 2..8 = 7 days
    }

    @Test
    fun calculateStreak_noSpendDaysThisMonth_countsOnlyCurrentMonth() {
        // Aug 25 - first tx (DEBIT)
        // Sep 5 - DEBIT
        // Sep 15 - DEBIT
        // Sep 20 - today (no spend)
        // In September up to Sep 20: total 20 days.
        // Spend on 2 days (Sep 5, 15).
        // No-spend days in September: 20 - 2 = 18 days.
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(100.0, "DEBIT", 2026, 8, 25),
            createTx(100.0, "DEBIT", 2026, 9, 5),
            createTx(100.0, "DEBIT", 2026, 9, 15)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(18, state.noSpendDaysThisMonth)
        assertEquals("September", state.currentMonthName)
    }

    @Test
    fun calculateStreak_last7Days_hasCorrectTodayFlagAndLength() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            createTx(100.0, "DEBIT", 2026, 9, 10),
            createTx(100.0, "DEBIT", 2026, 9, 19)
        )

        val state = StreakCalculator.calculateStreak(transactions, nowCal)

        assertEquals(7, state.last7Days.size)
        // The last item in the list is today
        val todayItem = state.last7Days.last()
        assertTrue(todayItem.isToday)
        assertTrue(todayItem.isNoSpend) // Sep 20 has no spend

        // Second to last is yesterday (Sep 19)
        val yesterdayItem = state.last7Days[5]
        assertFalse(yesterdayItem.isToday)
        assertFalse(yesterdayItem.isNoSpend) // Sep 19 had spend
    }
}
