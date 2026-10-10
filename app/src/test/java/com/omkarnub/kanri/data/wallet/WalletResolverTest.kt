package com.omkarnub.kanri.data.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletResolverTest {

    @Test
    fun testUpiDebitAndCredit_resolvesToOnline() {
        val debitResult = WalletResolver.resolve(sourceType = "UPI", type = "DEBIT")
        assertEquals("ONLINE", debitResult.wallet)
        assertNull(debitResult.transferToWallet)

        val creditResult = WalletResolver.resolve(sourceType = "UPI", type = "CREDIT")
        assertEquals("ONLINE", creditResult.wallet)
        assertNull(creditResult.transferToWallet)
    }

    @Test
    fun testBankTransferAndCard_resolvesToOnline() {
        val bankResult = WalletResolver.resolve(sourceType = "BANK_TRANSFER", type = "DEBIT")
        assertEquals("ONLINE", bankResult.wallet)
        assertNull(bankResult.transferToWallet)

        val cardResult = WalletResolver.resolve(sourceType = "CARD", type = "DEBIT")
        assertEquals("ONLINE", cardResult.wallet)
        assertNull(cardResult.transferToWallet)
    }

    @Test
    fun testCashSource_resolvesToCash() {
        val cashDebit = WalletResolver.resolve(sourceType = "CASH", type = "DEBIT")
        assertEquals("CASH", cashDebit.wallet)
        assertNull(cashDebit.transferToWallet)

        val cashCredit = WalletResolver.resolve(sourceType = "CASH", type = "CREDIT")
        assertEquals("CASH", cashCredit.wallet)
        assertNull(cashCredit.transferToWallet)
    }

    @Test
    fun testRefund_resolvesToOnline() {
        val refund = WalletResolver.resolve(sourceType = "UPI", type = "CREDIT", isRefund = true)
        assertEquals("ONLINE", refund.wallet)
        assertNull(refund.transferToWallet)
    }

    @Test
    fun testUserChoice_overridesDefault() {
        // Source is UPI, but user explicitly selected CASH
        val resultCash = WalletResolver.resolve(sourceType = "UPI", type = "DEBIT", userChoice = "CASH")
        assertEquals("CASH", resultCash.wallet)
        assertNull(resultCash.transferToWallet)

        // User picked NONE ("Not from my balances")
        val resultNone = WalletResolver.resolve(sourceType = "CARD", type = "DEBIT", userChoice = "NONE")
        assertEquals("NONE", resultNone.wallet)
        assertNull(resultNone.transferToWallet)

        // User picked ONLINE
        val resultOnline = WalletResolver.resolve(sourceType = "CASH", type = "DEBIT", userChoice = "ONLINE")
        assertEquals("ONLINE", resultOnline.wallet)
        assertNull(resultOnline.transferToWallet)
    }

    @Test
    fun testAtmWithdrawal_spendingMode_resolvesToOnlineNoTransfer() {
        val result = WalletResolver.resolve(
            sourceType = "ATM",
            type = "DEBIT",
            atmMode = AtmWithdrawalMode.SPENDING
        )
        assertEquals("ONLINE", result.wallet)
        assertNull(result.transferToWallet)
    }

    @Test
    fun testAtmWithdrawal_transferMode_resolvesToOnlineTransferToCash() {
        val result = WalletResolver.resolve(
            sourceType = "ATM",
            type = "DEBIT",
            atmMode = AtmWithdrawalMode.TRANSFER
        )
        assertEquals("ONLINE", result.wallet)
        assertEquals("CASH", result.transferToWallet)
    }
}
