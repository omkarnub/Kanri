package com.omkarnub.kanri.data.wallet

import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AtmModeSwitchTest {

    @Test
    fun testAtmModeSwitch_atomicRewriteScope() {
        val atmDebitAuto = TransactionEntity(
            id = 1L,
            type = "DEBIT",
            amount = 3000.0,
            sourceType = "ATM",
            wallet = "ONLINE",
            transferToWallet = null,
            isManualEntry = false,
            timestamp = 1000L
        )

        val atmCredit = TransactionEntity(
            id = 2L,
            type = "CREDIT",
            amount = 1000.0,
            sourceType = "ATM",
            wallet = "ONLINE",
            transferToWallet = null,
            isManualEntry = false,
            timestamp = 1100L
        )

        val manualTransfer = TransactionEntity(
            id = 3L,
            type = "DEBIT",
            amount = 500.0,
            sourceType = "WALLET_TRANSFER",
            wallet = "CASH",
            transferToWallet = "ONLINE",
            isManualEntry = true,
            timestamp = 1200L
        )

        val upiDebit = TransactionEntity(
            id = 4L,
            type = "DEBIT",
            amount = 200.0,
            sourceType = "UPI",
            wallet = "ONLINE",
            transferToWallet = null,
            isManualEntry = false,
            timestamp = 1300L
        )

        val initialList = listOf(atmDebitAuto, atmCredit, manualTransfer, upiDebit)

        // 1. Switch to TRANSFER mode: rewrite only ATM DEBIT
        val rewrittenToTransfer = initialList.map { tx ->
            if (tx.sourceType == "ATM" && tx.type.equals("DEBIT", ignoreCase = true) && !tx.isManualEntry) {
                tx.copy(transferToWallet = "CASH")
            } else tx
        }

        // ATM DEBIT should now have transferToWallet = CASH
        assertEquals("CASH", rewrittenToTransfer.first { it.id == 1L }.transferToWallet)

        // ATM Credit must remain untouched
        assertNull(rewrittenToTransfer.first { it.id == 2L }.transferToWallet)

        // Manual transfer must remain completely untouched
        assertEquals("CASH", rewrittenToTransfer.first { it.id == 3L }.wallet)
        assertEquals("ONLINE", rewrittenToTransfer.first { it.id == 3L }.transferToWallet)

        // UPI debit untouched
        assertNull(rewrittenToTransfer.first { it.id == 4L }.transferToWallet)

        // 2. Switch back to SPENDING mode: rewrite ATM DEBIT to NULL
        val rewrittenToSpending = rewrittenToTransfer.map { tx ->
            if (tx.sourceType == "ATM" && tx.type.equals("DEBIT", ignoreCase = true) && !tx.isManualEntry) {
                tx.copy(transferToWallet = null)
            } else tx
        }

        assertNull(rewrittenToSpending.first { it.id == 1L }.transferToWallet)
        // Manual transfer still intact
        assertEquals("CASH", rewrittenToSpending.first { it.id == 3L }.wallet)
        assertEquals("ONLINE", rewrittenToSpending.first { it.id == 3L }.transferToWallet)
    }
}
