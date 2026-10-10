package com.omkarnub.kanri.data.analytics

import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.export.CsvExporter
import com.omkarnub.kanri.ui.search.SearchFilterUtils
import com.omkarnub.kanri.ui.search.TransactionTypeFilter
import com.omkarnub.kanri.ui.search.WalletFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TransferExclusionTest {

    private fun createTx(
        id: Long = 1L,
        amount: Double,
        type: String,
        sourceType: String = "UPI",
        wallet: String = "ONLINE",
        transferToWallet: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type,
            amount = amount,
            sourceType = sourceType,
            counterparty = "Test",
            wallet = wallet,
            transferToWallet = transferToWallet,
            timestamp = timestamp,
            rawSms = "raw"
        )
    }

    @Test
    fun testIsTransferPredicate() {
        val normalDebit = createTx(amount = 500.0, type = "DEBIT")
        assertFalse(normalDebit.isTransfer)

        val transferTx = createTx(amount = 2000.0, type = "DEBIT", wallet = "ONLINE", transferToWallet = "CASH")
        assertTrue(transferTx.isTransfer)
    }

    @Test
    fun testStreakCalculator_ignoresTransfers() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }
        val cal19 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 19, 12, 0, 0)
        }
        val cal10 = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 12, 0, 0)
        }

        val baseTx = createTx(amount = 100.0, type = "DEBIT", timestamp = cal10.timeInMillis)
        // Huge ₹50,000 transfer yesterday
        val transferTx = createTx(
            amount = 50000.0,
            type = "DEBIT",
            wallet = "ONLINE",
            transferToWallet = "CASH",
            timestamp = cal19.timeInMillis
        )

        val streak = StreakCalculator.calculateStreak(listOf(baseTx, transferTx), nowCal)
        // Streak must NOT be broken by the transfer
        assertTrue("Yesterday must remain a no-spend day", streak.last7Days[5].isNoSpend)
        assertEquals(10, streak.currentStreak)
    }

    @Test
    fun testDeltaCalculator_ignoresTransfers() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }
        val calSep = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 5, 12, 0, 0) }
        val calAug = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 5, 12, 0, 0) }

        val txSep = createTx(amount = 1000.0, type = "DEBIT", timestamp = calSep.timeInMillis)
        val transferSep = createTx(
            amount = 25000.0,
            type = "DEBIT",
            wallet = "ONLINE",
            transferToWallet = "CASH",
            timestamp = calSep.timeInMillis
        )
        val txAug = createTx(amount = 2000.0, type = "DEBIT", timestamp = calAug.timeInMillis)

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 9,
            transactions = listOf(txSep, transferSep, txAug),
            nowCalendar = nowCal
        )

        assertTrue(delta is Delta.Down)
        assertEquals(50, (delta as Delta.Down).percent)
    }

    @Test
    fun testCsvExporter_formatsTransferColumns() {
        val normalTx = createTx(id = 1L, amount = 150.0, type = "DEBIT", wallet = "CASH")
        val transferTx = createTx(id = 2L, amount = 3000.0, type = "DEBIT", wallet = "ONLINE", transferToWallet = "CASH")

        val csv = CsvExporter.generateCsv(
            transactions = listOf(
                com.omkarnub.kanri.data.db.TransactionWithCategory(normalTx, null),
                com.omkarnub.kanri.data.db.TransactionWithCategory(transferTx, null)
            ),
            periodTitle = "All Time"
        )

        assertTrue(csv.contains("Wallet,Transfer To Wallet"))
        assertTrue(csv.contains("CASH,\n") || csv.contains("CASH,"))
        assertTrue(csv.contains("ONLINE,CASH"))
    }

    @Test
    fun testSearchFilter_transfersFilterIsolatesTransfersOnly() {
        val normalTx = com.omkarnub.kanri.data.db.TransactionWithCategory(
            createTx(amount = 100.0, type = "DEBIT"),
            null
        )
        val transferTx = com.omkarnub.kanri.data.db.TransactionWithCategory(
            createTx(amount = 1000.0, type = "DEBIT", wallet = "ONLINE", transferToWallet = "CASH"),
            null
        )

        // When TRANSFERS filter is selected: normal transactions must be rejected, transfers must be accepted
        assertFalse(
            SearchFilterUtils.matchesFilter(
                item = normalTx,
                query = "",
                typeFilter = TransactionTypeFilter.ALL,
                startTime = null,
                endTime = null,
                categoryIds = emptySet(),
                sourceTypes = emptySet(),
                minAmount = null,
                maxAmount = null,
                walletFilter = WalletFilter.TRANSFERS
            )
        )

        assertTrue(
            SearchFilterUtils.matchesFilter(
                item = transferTx,
                query = "",
                typeFilter = TransactionTypeFilter.ALL,
                startTime = null,
                endTime = null,
                categoryIds = emptySet(),
                sourceTypes = emptySet(),
                minAmount = null,
                maxAmount = null,
                walletFilter = WalletFilter.TRANSFERS
            )
        )
    }
}
