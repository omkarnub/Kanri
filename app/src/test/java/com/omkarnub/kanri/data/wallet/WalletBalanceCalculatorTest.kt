package com.omkarnub.kanri.data.wallet

import com.omkarnub.kanri.data.db.WalletBalanceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletBalanceCalculatorTest {

    @Test
    fun testRoundingToTwoDecimals_neverNegativeZero() {
        assertEquals(0.0, WalletBalanceCalculator.roundTo2Decimals(-0.000001), 0.00001)
        assertEquals(0.0, WalletBalanceCalculator.roundTo2Decimals(0.000001), 0.00001)
        assertEquals(1234.57, WalletBalanceCalculator.roundTo2Decimals(1234.567), 0.00001)
        assertEquals(10.50, WalletBalanceCalculator.roundTo2Decimals(10.50), 0.00001)
        assertEquals(0.0, WalletBalanceCalculator.roundTo2Decimals(-0.0), 0.00001)
    }

    @Test
    fun testIsBelowZero() {
        assertTrue(WalletBalanceCalculator.isBelowZero(-0.01))
        assertTrue(WalletBalanceCalculator.isBelowZero(-500.0))
        assertFalse(WalletBalanceCalculator.isBelowZero(0.0))
        assertFalse(WalletBalanceCalculator.isBelowZero(-0.0001))
        assertFalse(WalletBalanceCalculator.isBelowZero(10.0))
    }

    @Test
    fun testFormula_balanceCalculatedCorrectly() {
        val openingTime = 1000L
        val txs = listOf(
            WalletTxnRow(id = 1, type = "CREDIT", amount = 5000.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1100L, isDuplicate = false),
            WalletTxnRow(id = 2, type = "DEBIT", amount = 1200.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = false),
            WalletTxnRow(id = 3, type = "DEBIT", amount = 300.0, wallet = "CASH", transferToWallet = null, timestamp = 1300L, isDuplicate = false)
        )

        // ONLINE: 10000 + 5000 - 1200 = 13800
        val onlineBal = WalletBalanceCalculator.balance("ONLINE", 10000.0, openingTime, txs)
        assertEquals(13800.0, onlineBal, 0.001)

        // CASH: 2000 - 300 = 1700
        val cashBal = WalletBalanceCalculator.balance("CASH", 2000.0, openingTime, txs)
        assertEquals(1700.0, cashBal, 0.001)
    }

    @Test
    fun testOpeningTimestampCutoff_preCutoffTransactionsIgnored() {
        val openingTime = 2000L
        val txs = listOf(
            // Pre-cutoff: must not affect balance
            WalletTxnRow(id = 1, type = "DEBIT", amount = 5000.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1500L, isDuplicate = false),
            WalletTxnRow(id = 2, type = "CREDIT", amount = 10000.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1999L, isDuplicate = false),
            // At cutoff (inclusive): must affect balance
            WalletTxnRow(id = 3, type = "DEBIT", amount = 400.0, wallet = "ONLINE", transferToWallet = null, timestamp = 2000L, isDuplicate = false),
            // Post cutoff: must affect balance
            WalletTxnRow(id = 4, type = "CREDIT", amount = 100.0, wallet = "ONLINE", transferToWallet = null, timestamp = 2500L, isDuplicate = false)
        )

        // 10000 - 400 + 100 = 9700
        val balance = WalletBalanceCalculator.balance("ONLINE", 10000.0, openingTime, txs)
        assertEquals(9700.0, balance, 0.001)
    }

    @Test
    fun testTransfers_inAndOut() {
        val openingTime = 1000L
        val txs = listOf(
            // Transfer ₹2,000 from ONLINE to CASH
            WalletTxnRow(id = 1, type = "DEBIT", amount = 2000.0, wallet = "ONLINE", transferToWallet = "CASH", timestamp = 1200L, isDuplicate = false),
            // Transfer ₹500 from CASH to ONLINE
            WalletTxnRow(id = 2, type = "DEBIT", amount = 500.0, wallet = "CASH", transferToWallet = "ONLINE", timestamp = 1400L, isDuplicate = false)
        )

        // ONLINE: 15000 - 2000 + 500 = 13500
        val onlineBal = WalletBalanceCalculator.balance("ONLINE", 15000.0, openingTime, txs)
        assertEquals(13500.0, onlineBal, 0.001)

        // CASH: 2000 + 2000 - 500 = 3500
        val cashBal = WalletBalanceCalculator.balance("CASH", 2000.0, openingTime, txs)
        assertEquals(3500.0, cashBal, 0.001)
    }

    @Test
    fun testDuplicateTransactions_strictlyIgnored() {
        val openingTime = 1000L
        val txs = listOf(
            WalletTxnRow(id = 1, type = "DEBIT", amount = 500.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = false),
            // Duplicate row
            WalletTxnRow(id = 2, type = "DEBIT", amount = 500.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = true)
        )

        val balance = WalletBalanceCalculator.balance("ONLINE", 5000.0, openingTime, txs)
        assertEquals(4500.0, balance, 0.001)
    }

    @Test
    fun testNegativeBalance_allowedAndTracked() {
        val openingTime = 1000L
        val txs = listOf(
            WalletTxnRow(id = 1, type = "DEBIT", amount = 3000.0, wallet = "CASH", transferToWallet = null, timestamp = 1100L, isDuplicate = false)
        )

        // Cash was ₹500, spent ₹3000 -> balance is -₹2500
        val balance = WalletBalanceCalculator.balance("CASH", 500.0, openingTime, txs)
        assertEquals(-2500.0, balance, 0.001)
        assertTrue(WalletBalanceCalculator.isBelowZero(balance))
    }

    @Test
    fun testBalanceAfter_inclusiveAndIdTieBreaking() {
        val openingTime = 1000L
        val tx1 = WalletTxnRow(id = 10, type = "DEBIT", amount = 100.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = false)
        val tx2 = WalletTxnRow(id = 20, type = "DEBIT", amount = 200.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = false)
        val tx3 = WalletTxnRow(id = 15, type = "DEBIT", amount = 300.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1200L, isDuplicate = false)
        val tx4 = WalletTxnRow(id = 30, type = "DEBIT", amount = 400.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1400L, isDuplicate = false)

        val allTxs = listOf(tx1, tx2, tx3, tx4)

        // For tx1 (timestamp 1200, id 10):
        // Only tx1 should be included (tie breaking by id on same timestamp: id 10 <= 10)
        val balAfterTx1 = WalletBalanceCalculator.balanceAfter("ONLINE", tx1, 1000.0, openingTime, allTxs)
        assertEquals(900.0, balAfterTx1, 0.001)

        // For tx3 (timestamp 1200, id 15):
        // tx1 (id 10) and tx3 (id 15) included: 1000 - 100 - 300 = 600
        val balAfterTx3 = WalletBalanceCalculator.balanceAfter("ONLINE", tx3, 1000.0, openingTime, allTxs)
        assertEquals(600.0, balAfterTx3, 0.001)

        // For tx2 (timestamp 1200, id 20):
        // tx1 (10), tx3 (15), tx2 (20) included: 1000 - 100 - 300 - 200 = 400
        val balAfterTx2 = WalletBalanceCalculator.balanceAfter("ONLINE", tx2, 1000.0, openingTime, allTxs)
        assertEquals(400.0, balAfterTx2, 0.001)

        // For tx4 (timestamp 1400, id 30):
        // All four included: 1000 - 100 - 200 - 300 - 400 = 0
        val balAfterTx4 = WalletBalanceCalculator.balanceAfter("ONLINE", tx4, 1000.0, openingTime, allTxs)
        assertEquals(0.0, balAfterTx4, 0.001)
    }

    @Test
    fun testCorrectionToOpening_producesExactDesiredBalance() {
        val openingTime = 1000L
        val txs = listOf(
            WalletTxnRow(id = 1, type = "DEBIT", amount = 300.0, wallet = "ONLINE", transferToWallet = null, timestamp = 1100L, isDuplicate = false)
        )
        val currentOpening = 5000.0
        val currentBalance = WalletBalanceCalculator.balance("ONLINE", currentOpening, openingTime, txs) // 4700.0

        // User says actual balance is ₹6,000.00
        val desiredBalance = 6000.0
        val newOpening = WalletBalanceCalculator.correctionToOpening(
            currentOpeningAmount = currentOpening,
            currentBalance = currentBalance,
            desiredBalance = desiredBalance
        )

        // New opening should be 5000 + (6000 - 4700) = 6300.0
        assertEquals(6300.0, newOpening, 0.001)

        // When recomputed with newOpening, balance should exactly match desiredBalance
        val recomputedBalance = WalletBalanceCalculator.balance("ONLINE", newOpening, openingTime, txs)
        assertEquals(desiredBalance, recomputedBalance, 0.001)
    }

    @Test
    fun testBalances_unconfigured() {
        val emptyOpenings = emptyMap<String, WalletBalanceEntity>()
        val result = WalletBalanceCalculator.balances(emptyOpenings, emptyList())
        assertFalse(result.isConfigured)
        assertEquals(0.0, result.total, 0.001)
        assertEquals(0.0, result.cash, 0.001)
        assertEquals(0.0, result.online, 0.001)
        assertEquals(0.0, result.cashTodayNet, 0.001)
        assertEquals(0.0, result.onlineTodayNet, 0.001)
    }

    @Test
    fun testTodayNet_ignoresYesterdayAndDuplicates() {
        val openingTime = 1000L
        val startOfToday = 5000L

        val txs = listOf(
            // Yesterday transactions (timestamp < 5000L): must be ignored by todayNet
            WalletTxnRow(id = 1, type = "CREDIT", amount = 10000.0, wallet = "ONLINE", transferToWallet = null, timestamp = 2000L, isDuplicate = false),
            WalletTxnRow(id = 2, type = "DEBIT", amount = 3000.0, wallet = "CASH", transferToWallet = null, timestamp = 3000L, isDuplicate = false),

            // Today transactions (timestamp >= 5000L)
            // 1. Credit to ONLINE: +500
            WalletTxnRow(id = 3, type = "CREDIT", amount = 500.0, wallet = "ONLINE", transferToWallet = null, timestamp = 5100L, isDuplicate = false),
            // 2. Duplicate credit to ONLINE: must be ignored
            WalletTxnRow(id = 4, type = "CREDIT", amount = 500.0, wallet = "ONLINE", transferToWallet = null, timestamp = 5100L, isDuplicate = true),
            // 3. Debit from ONLINE: -200
            WalletTxnRow(id = 5, type = "DEBIT", amount = 200.0, wallet = "ONLINE", transferToWallet = null, timestamp = 5200L, isDuplicate = false),
            // 4. ATM Transfer from ONLINE to CASH: ₹1,000 (ONLINE -1000, CASH +1000)
            WalletTxnRow(id = 6, type = "DEBIT", amount = 1000.0, wallet = "ONLINE", transferToWallet = "CASH", timestamp = 5300L, isDuplicate = false),
            // 5. Cash spend: -150
            WalletTxnRow(id = 7, type = "DEBIT", amount = 150.0, wallet = "CASH", transferToWallet = null, timestamp = 5400L, isDuplicate = false)
        )

        // ONLINE today net: +500 - 200 - 1000 = -700
        val onlineNet = WalletBalanceCalculator.todayNet("ONLINE", openingTime, startOfToday, txs)
        assertEquals(-700.0, onlineNet, 0.001)

        // CASH today net: +1000 (transfer from ONLINE) - 150 = +850
        val cashNet = WalletBalanceCalculator.todayNet("CASH", openingTime, startOfToday, txs)
        assertEquals(850.0, cashNet, 0.001)
    }

    @Test
    fun testBalances_computesFullStateWithTodayNet() {
        val openingTime = 1000L
        val startOfToday = 5000L

        val openings = mapOf(
            "CASH" to WalletBalanceEntity(walletId = "CASH", openingAmount = 2000.0, openingTimestamp = openingTime),
            "ONLINE" to WalletBalanceEntity(walletId = "ONLINE", openingAmount = 10000.0, openingTimestamp = openingTime)
        )

        val txs = listOf(
            // Yesterday spend
            WalletTxnRow(id = 1, type = "DEBIT", amount = 1000.0, wallet = "ONLINE", transferToWallet = null, timestamp = 2000L, isDuplicate = false),
            // Today spend
            WalletTxnRow(id = 2, type = "DEBIT", amount = 500.0, wallet = "ONLINE", transferToWallet = null, timestamp = 5200L, isDuplicate = false),
            // Today cash addition
            WalletTxnRow(id = 3, type = "CREDIT", amount = 300.0, wallet = "CASH", transferToWallet = null, timestamp = 5300L, isDuplicate = false)
        )

        val balances = WalletBalanceCalculator.balances(openings, txs, startOfToday)

        assertTrue(balances.isConfigured)
        // Balances total
        assertEquals(2300.0, balances.cash, 0.001)      // 2000 + 300
        assertEquals(8500.0, balances.online, 0.001)    // 10000 - 1000 - 500
        assertEquals(10800.0, balances.total, 0.001)
        // Today net flows
        assertEquals(300.0, balances.cashTodayNet, 0.001)    // +300 today
        assertEquals(-500.0, balances.onlineTodayNet, 0.001) // -500 today
    }
}
