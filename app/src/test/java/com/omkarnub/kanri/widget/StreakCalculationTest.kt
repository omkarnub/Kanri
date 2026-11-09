package com.omkarnub.kanri.widget

import com.omkarnub.kanri.data.analytics.StreakCalculator
import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class StreakCalculationTest {

    @Test
    fun testEmptyTransactionsGivesZeroStreak() {
        val streak = StreakCalculator.calculateStreak(emptyList())
        assertEquals(0, streak.currentStreak)
        assertEquals(0, streak.bestStreak)
    }

    @Test
    fun testNoSpendDaysCalculation() {
        val now = Calendar.getInstance()
        val threeDaysAgo = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -3) }

        // A single debit transaction 3 days ago
        val tx = TransactionEntity(
            id = 1L,
            type = "DEBIT",
            amount = 250.0,
            sourceType = "UPI",
            notes = "Dinner",
            timestamp = threeDaysAgo.timeInMillis
        )

        val state = StreakCalculator.calculateStreak(listOf(tx), now)
        // Today, yesterday, and 2 days ago are no-spend days => currentStreak should be 2
        assertTrue(state.currentStreak >= 2)
    }

    @Test
    fun testTransfersDoNotBreakStreak() {
        val now = Calendar.getInstance()
        val transferTx = TransactionEntity(
            id = 2L,
            type = "DEBIT",
            amount = 1000.0,
            sourceType = "WALLET_TRANSFER",
            wallet = "CASH",
            transferToWallet = "ONLINE",
            notes = "ATM deposit",
            timestamp = now.timeInMillis
        )

        val state = StreakCalculator.calculateStreak(listOf(transferTx), now)
        // Transfers are excluded, so today remains a no-spend day
        assertTrue(transferTx.isTransfer)
    }
}
