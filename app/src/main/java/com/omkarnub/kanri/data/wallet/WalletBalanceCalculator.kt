package com.omkarnub.kanri.data.wallet

import com.omkarnub.kanri.data.db.WalletBalanceEntity
import kotlin.math.abs
import kotlin.math.round

object WalletBalanceCalculator {

    /**
     * Rounds amount to 2 decimal places (paise) and ensures -0.00 is never returned.
     */
    fun roundTo2Decimals(amount: Double): Double {
        if (amount.isNaN() || amount.isInfinite()) return 0.0
        val rounded = round(amount * 100.0) / 100.0
        return if (abs(rounded) < 0.0001) 0.0 else rounded
    }

    /**
     * Checks if a balance is strictly below zero.
     */
    fun isBelowZero(amount: Double): Boolean {
        return roundTo2Decimals(amount) < -0.005
    }

    /**
     * Computes the balance for a specific wallet ("CASH" or "ONLINE") derived from:
     * opening_amount + credits - debits + transfers_in - transfers_out
     *
     * Only non-duplicate transactions at or after openingTimestamp are counted.
     */
    fun balance(
        wallet: String,
        openingAmount: Double,
        openingTimestamp: Long,
        transactions: List<WalletTxnRow>
    ): Double {
        var delta = 0.0
        val targetWallet = wallet.uppercase().trim()

        for (tx in transactions) {
            if (tx.isDuplicate) continue
            if (tx.timestamp < openingTimestamp) continue

            val txWallet = tx.wallet.uppercase().trim()
            val transferTo = tx.transferToWallet?.uppercase()?.trim()

            if (transferTo != null) {
                // Transfer row
                if (txWallet == targetWallet) {
                    delta -= tx.amount // Outflow from source wallet
                }
                if (transferTo == targetWallet) {
                    delta += tx.amount // Inflow to destination wallet
                }
            } else {
                // Standard income or expense
                if (txWallet == targetWallet) {
                    if (tx.type.equals("CREDIT", ignoreCase = true)) {
                        delta += tx.amount
                    } else if (tx.type.equals("DEBIT", ignoreCase = true)) {
                        delta -= tx.amount
                    }
                }
            }
        }

        return roundTo2Decimals(openingAmount + delta)
    }

    /**
     * Computes Cash, Online, and Total balances given opening balances and transactions.
     */
    fun balances(
        openings: Map<String, WalletBalanceEntity>,
        transactions: List<WalletTxnRow>
    ): WalletBalances {
        val cashOpening = openings["CASH"]
        val onlineOpening = openings["ONLINE"]

        if (cashOpening == null && onlineOpening == null) {
            return WalletBalances(cash = 0.0, online = 0.0, total = 0.0, isConfigured = false)
        }

        val cashBalance = if (cashOpening != null) {
            balance("CASH", cashOpening.openingAmount, cashOpening.openingTimestamp, transactions)
        } else {
            0.0
        }

        val onlineBalance = if (onlineOpening != null) {
            balance("ONLINE", onlineOpening.openingAmount, onlineOpening.openingTimestamp, transactions)
        } else {
            0.0
        }

        val total = roundTo2Decimals(cashBalance + onlineBalance)
        return WalletBalances(
            cash = cashBalance,
            online = onlineBalance,
            total = total,
            isConfigured = true,
            cashOpeningTimestamp = cashOpening?.openingTimestamp,
            onlineOpeningTimestamp = onlineOpening?.openingTimestamp
        )
    }

    /**
     * Computes balance after a target transaction (inclusive), ordered by timestamp ASC, id ASC.
     */
    fun balanceAfter(
        wallet: String,
        targetTransaction: WalletTxnRow,
        openingAmount: Double,
        openingTimestamp: Long,
        allTransactions: List<WalletTxnRow>
    ): Double {
        if (targetTransaction.timestamp < openingTimestamp) {
            return roundTo2Decimals(openingAmount)
        }

        val filtered = allTransactions.filter {
            !it.isDuplicate &&
                    (it.timestamp < targetTransaction.timestamp ||
                            (it.timestamp == targetTransaction.timestamp && it.id <= targetTransaction.id))
        }

        return balance(wallet, openingAmount, openingTimestamp, filtered)
    }

    /**
     * Calculates the new opening_amount required so that the current balance becomes desiredBalance.
     * Keeps opening_timestamp untouched.
     */
    fun correctionToOpening(
        desiredBalance: Double,
        currentBalance: Double,
        currentOpeningAmount: Double
    ): Double {
        val adjustment = desiredBalance - currentBalance
        return roundTo2Decimals(currentOpeningAmount + adjustment)
    }
}
