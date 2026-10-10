package com.omkarnub.kanri.data.wallet

import android.content.Context
import androidx.room.withTransaction
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionDao
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.WalletBalanceEntity
import com.omkarnub.kanri.data.db.WalletDao
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WalletRepository(
    private val context: Context? = null,
    private val db: KanriDatabase? = context?.let { KanriDatabase.getDatabase(it) },
    private val walletDao: WalletDao = db?.walletDao() ?: throw IllegalArgumentException("walletDao must be provided if db is null"),
    private val transactionDao: TransactionDao = db?.transactionDao() ?: throw IllegalArgumentException("transactionDao must be provided if db is null"),
    private val walletPrefs: WalletPreferences? = context?.let { WalletPreferences.getInstance(it) }
) {
    private suspend inline fun <T> runInTransaction(crossinline block: suspend () -> T): T {
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    /**
     * Observes live balances: Cash, Online, Total.
     */
    fun observeBalances(): Flow<WalletBalances> {
        return combine(
            walletDao.observeBalances(),
            transactionDao.observeWalletTransactions()
        ) { balanceEntities, txRows ->
            if (balanceEntities.isEmpty()) {
                WalletBalances(cash = 0.0, online = 0.0, total = 0.0, isConfigured = false)
            } else {
                val map = balanceEntities.associateBy { it.walletId.uppercase() }
                WalletBalanceCalculator.balances(map, txRows)
            }
        }
    }

    /**
     * Checks if wallets have been set up.
     */
    fun isConfigured(): Flow<Boolean> {
        return walletDao.observeBalances().map { it.isNotEmpty() }
    }

    suspend fun isConfiguredSync(): Boolean = withContext(Dispatchers.IO) {
        walletDao.getCount() >= 2
    }

    suspend fun getOpeningBalancesSync(): Map<String, WalletBalanceEntity> = withContext(Dispatchers.IO) {
        walletDao.getBalancesSync().associateBy { it.walletId.uppercase() }
    }

    /**
     * First-time setup or full re-initialization of Cash and Online opening balances.
     */
    suspend fun setup(
        cashAmount: Double,
        onlineAmount: Double,
        atmMode: AtmWithdrawalMode = AtmWithdrawalMode.TRANSFER
    ): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cashOpening = WalletBalanceEntity(
            walletId = "CASH",
            openingAmount = WalletBalanceCalculator.roundTo2Decimals(cashAmount),
            openingTimestamp = now
        )
        val onlineOpening = WalletBalanceEntity(
            walletId = "ONLINE",
            openingAmount = WalletBalanceCalculator.roundTo2Decimals(onlineAmount),
            openingTimestamp = now
        )

        runInTransaction {
            walletDao.insertAll(listOf(cashOpening, onlineOpening))
            walletPrefs?.atmWithdrawalMode = atmMode
            walletPrefs?.isWalletSetupCompleted = true

            if (atmMode == AtmWithdrawalMode.TRANSFER) {
                transactionDao.rewriteAtmTransfers("CASH")
            }
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Edits opening balances and timestamps directly.
     */
    suspend fun editOpeningBalances(
        cashAmount: Double,
        onlineAmount: Double
    ): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cashOpening = WalletBalanceEntity(
            walletId = "CASH",
            openingAmount = WalletBalanceCalculator.roundTo2Decimals(cashAmount),
            openingTimestamp = now
        )
        val onlineOpening = WalletBalanceEntity(
            walletId = "ONLINE",
            openingAmount = WalletBalanceCalculator.roundTo2Decimals(onlineAmount),
            openingTimestamp = now
        )

        runInTransaction {
            walletDao.insertAll(listOf(cashOpening, onlineOpening))
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Corrects a wallet's current balance to desiredBalance by adjusting opening_amount
     * and preserving opening_timestamp.
     */
    suspend fun correct(wallet: String, desiredBalance: Double): Double = withContext(Dispatchers.IO) {
        val targetWallet = wallet.uppercase().trim()
        val roundedDesired = WalletBalanceCalculator.roundTo2Decimals(desiredBalance)

        val newOpening = runInTransaction {
            val entity = walletDao.getBalance(targetWallet)
                ?: throw IllegalStateException("Wallet $targetWallet is not set up.")

            val allTxs = transactionDao.getWalletTransactionsSync()
            val currentBal = WalletBalanceCalculator.balance(
                targetWallet,
                entity.openingAmount,
                entity.openingTimestamp,
                allTxs
            )

            val updatedOpening = WalletBalanceCalculator.correctionToOpening(
                desiredBalance = roundedDesired,
                currentBalance = currentBal,
                currentOpeningAmount = entity.openingAmount
            )

            walletDao.updateOpeningAmount(targetWallet, updatedOpening)
            updatedOpening
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }

        newOpening
    }

    /**
     * Moves money from one wallet to another (creates a WALLET_TRANSFER row).
     */
    suspend fun moveMoney(
        fromWallet: String,
        toWallet: String,
        amount: Double,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null
    ): Long = withContext(Dispatchers.IO) {
        val cleanAmount = WalletBalanceCalculator.roundTo2Decimals(amount)
        require(cleanAmount > 0.0) { "Transfer amount must be greater than zero." }

        val from = fromWallet.uppercase().trim()
        val to = toWallet.uppercase().trim()

        val transferTx = TransactionEntity(
            type = "DEBIT",
            amount = cleanAmount,
            sourceType = "WALLET_TRANSFER",
            counterparty = "Transfer to $to",
            displayName = "Transfer ($from → $to)",
            bank = null,
            refNo = "TRANSFER_${System.currentTimeMillis()}",
            timestamp = timestamp,
            categoryId = null,
            rawSms = "Moved ₹$cleanAmount from $from to $to",
            isDuplicate = false,
            isManualEntry = true,
            needsReview = false,
            reviewReason = null,
            notes = note,
            wallet = from,
            transferToWallet = to
        )

        val insertedId = runInTransaction {
            transactionDao.insert(transferTx)
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }

        insertedId
    }

    /**
     * Atomically switches ATM withdrawal mode in preference and updates ATM debit rows.
     */
    suspend fun setAtmWithdrawalMode(mode: AtmWithdrawalMode): Unit = withContext(Dispatchers.IO) {
        runInTransaction {
            walletPrefs?.atmWithdrawalMode = mode
            val targetTransferWallet = if (mode == AtmWithdrawalMode.TRANSFER) "CASH" else null
            transactionDao.rewriteAtmTransfers(targetTransferWallet)
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Computes the balance of a wallet after a specific transaction.
     */
    suspend fun getBalanceAfter(
        wallet: String,
        transaction: TransactionEntity
    ): Double? = withContext(Dispatchers.IO) {
        val targetWallet = wallet.uppercase().trim()
        if (targetWallet == "NONE") return@withContext null

        val entity = walletDao.getBalance(targetWallet) ?: return@withContext null
        val allTxs = transactionDao.getWalletTransactionsSync()

        val row = WalletTxnRow(
            id = transaction.id,
            type = transaction.type,
            amount = transaction.amount,
            wallet = transaction.wallet,
            transferToWallet = transaction.transferToWallet,
            timestamp = transaction.timestamp,
            isDuplicate = transaction.isDuplicate
        )

        WalletBalanceCalculator.balanceAfter(
            wallet = targetWallet,
            targetTransaction = row,
            openingAmount = entity.openingAmount,
            openingTimestamp = entity.openingTimestamp,
            allTransactions = allTxs
        )
    }

    /**
     * Converts a specific transaction into a cash deposit or cash withdrawal transfer.
     */
    suspend fun convertToTransfer(
        transactionId: Long,
        wallet: String,
        transferToWallet: String
    ): Unit = withContext(Dispatchers.IO) {
        runInTransaction {
            transactionDao.convertTransactionToTransfer(
                id = transactionId,
                wallet = wallet.uppercase().trim(),
                transferToWallet = transferToWallet.uppercase().trim()
            )
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Resets wallet balances and settings without deleting transactions.
     */
    suspend fun reset(): Unit = withContext(Dispatchers.IO) {
        runInTransaction {
            walletDao.deleteAll()
            walletPrefs?.reset()
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }
}
