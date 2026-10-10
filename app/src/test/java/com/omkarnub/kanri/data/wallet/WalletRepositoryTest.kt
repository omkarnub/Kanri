package com.omkarnub.kanri.data.wallet

import com.omkarnub.kanri.data.db.CategorySpendAggregate
import com.omkarnub.kanri.data.db.DailySpendAggregate
import com.omkarnub.kanri.data.db.PayeeSpendAggregate
import com.omkarnub.kanri.data.db.SourceTypeAggregate
import com.omkarnub.kanri.data.db.TransactionDao
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.data.db.WalletBalanceEntity
import com.omkarnub.kanri.data.db.WalletDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRepositoryTest {

    private class FakeWalletDao : WalletDao {
        val balances = mutableMapOf<String, WalletBalanceEntity>()
        val flow = MutableStateFlow<List<WalletBalanceEntity>>(emptyList())

        private fun updateFlow() {
            flow.value = balances.values.toList()
        }

        override fun observeBalances(): Flow<List<WalletBalanceEntity>> = flow

        override suspend fun getBalancesSync(): List<WalletBalanceEntity> = balances.values.toList()

        override suspend fun getBalance(walletId: String): WalletBalanceEntity? = balances[walletId.uppercase()]

        override suspend fun insertAll(balances: List<WalletBalanceEntity>) {
            balances.forEach { this.balances[it.walletId.uppercase()] = it }
            updateFlow()
        }

        override suspend fun insert(balance: WalletBalanceEntity) {
            balances[balance.walletId.uppercase()] = balance
            updateFlow()
        }

        override suspend fun updateOpeningAmount(walletId: String, amount: Double) {
            balances[walletId.uppercase()]?.let {
                balances[walletId.uppercase()] = it.copy(openingAmount = amount)
            }
            updateFlow()
        }

        override suspend fun deleteAll() {
            balances.clear()
            updateFlow()
        }

        override suspend fun getCount(): Int = balances.size
    }

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L
        val walletFlow = MutableStateFlow<List<WalletTxnRow>>(emptyList())

        private fun updateFlow() {
            walletFlow.value = transactions.map {
                WalletTxnRow(it.id, it.type, it.amount, it.wallet, it.transferToWallet, it.timestamp, it.isDuplicate)
            }
        }

        override suspend fun insert(transaction: TransactionEntity): Long {
            val assigned = transaction.copy(id = if (transaction.id == 0L) nextId++ else transaction.id)
            transactions.removeAll { it.id == assigned.id }
            transactions.add(assigned)
            updateFlow()
            return assigned.id
        }

        override fun observeWalletTransactions(): Flow<List<WalletTxnRow>> = walletFlow

        override suspend fun getWalletTransactionsSync(): List<WalletTxnRow> = transactions.map {
            WalletTxnRow(it.id, it.type, it.amount, it.wallet, it.transferToWallet, it.timestamp, it.isDuplicate)
        }

        override suspend fun rewriteAtmTransfers(targetTransferWallet: String?): Int {
            var count = 0
            val updated = transactions.map {
                if (it.sourceType == "ATM" && it.type.equals("DEBIT", ignoreCase = true) && !it.isManualEntry) {
                    count++
                    it.copy(transferToWallet = targetTransferWallet)
                } else it
            }
            transactions.clear()
            transactions.addAll(updated)
            updateFlow()
            return count
        }

        override suspend fun convertTransactionToTransfer(id: Long, wallet: String, transferToWallet: String): Int {
            val idx = transactions.indexOfFirst { it.id == id }
            return if (idx != -1) {
                transactions[idx] = transactions[idx].copy(
                    wallet = wallet,
                    transferToWallet = transferToWallet,
                    categoryId = null,
                    needsReview = false,
                    reviewReason = null
                )
                updateFlow()
                1
            } else 0
        }

        override suspend fun updateWallet(id: Long, wallet: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                transactions[idx] = transactions[idx].copy(wallet = wallet)
                updateFlow()
            }
        }

        override suspend fun updateWalletAndTransfer(id: Long, wallet: String, transferToWallet: String?) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                transactions[idx] = transactions[idx].copy(wallet = wallet, transferToWallet = transferToWallet)
                updateFlow()
            }
        }

        override suspend fun enrichBankIfGeneric(id: Long, bank: String) {}
        override suspend fun enrichRawSmsIfFromNotification(id: Long, rawSms: String) {}

        // Stubs for remaining TransactionDao methods
        override fun getAllTransactions(): Flow<List<TransactionEntity>> = emptyFlow()
        override fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> = emptyFlow()
        override suspend fun getTransactionById(id: Long): TransactionEntity? = transactions.firstOrNull { it.id == id }
        override fun observeTransactionWithCategory(id: Long): Flow<TransactionWithCategory?> = emptyFlow()
        override suspend fun getTransactionWithCategoryById(id: Long): TransactionWithCategory? = null
        override suspend fun deleteById(id: Long) {}
        override suspend fun deleteByRefNo(refNo: String) {}
        override suspend fun deleteByRefNoPattern(pattern: String) {}
        override suspend fun getTransactionsByRefNoPattern(pattern: String): List<TransactionEntity> = emptyList()
        override suspend fun updateCounterpartyForLending(oldName: String, newName: String) {}
        override fun getTransactionsWithCategory(): Flow<List<TransactionWithCategory>> = emptyFlow()
        override suspend fun getTransactionsWithCategoryBetweenSync(startTime: Long, endTime: Long): List<TransactionWithCategory> = emptyList()
        override suspend fun getAllTransactionsWithCategorySync(): List<TransactionWithCategory> = emptyList()
        override fun observeReviewCount(): Flow<Int> = emptyFlow()
        override fun observeReviewQueue(): Flow<List<TransactionEntity>> = emptyFlow()
        override suspend fun clearReviewFlag(transactionId: Long) {}
        override suspend fun clearReviewFlags(transactionIds: List<Long>) {}
        override suspend fun clearAllReviewFlags() {}
        override suspend fun updateCategoryId(transactionId: Long, categoryId: Long) {}
        override suspend fun updateCategoryAndNotes(transactionId: Long, categoryId: Long, notes: String?) {}
        override suspend fun updateNotes(transactionId: Long, notes: String?) {}
        override suspend fun updateCategoryForCounterparty(counterparty: String, categoryId: Long) {}
        override suspend fun applyCategoryToMatchingTransactions(keyword: String, categoryId: Long): Int = 0
        override suspend fun countTransactionsMatchingKeyword(keyword: String): Int = 0
        override suspend fun reassignTransactionsCategory(deletedCategoryId: Long, fallbackCategoryId: Long?) {}
        override suspend fun getCount(): Int = transactions.size
        override suspend fun getAllTransactionsSync(): List<TransactionEntity> = transactions
        override suspend fun getRecentTransactionsSync(limit: Int): List<TransactionEntity> = transactions.take(limit)
        override suspend fun insertAll(transactions: List<TransactionEntity>) {}
        override suspend fun deleteAllTransactions() { transactions.clear() }
        override suspend fun delete(transaction: TransactionEntity) { transactions.remove(transaction) }
        override suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsByTypeBetweenSync(type: String, startTime: Long, endTime: Long): List<TransactionEntity> = emptyList()
        override suspend fun getTransactionsWithCategoryByCounterpartySync(counterparty: String): List<TransactionWithCategory> = emptyList()
        override suspend fun getTransactionsWithCategoryByCounterpartyBetweenSync(counterparty: String, startTime: Long, endTime: Long): List<TransactionWithCategory> = emptyList()
        override suspend fun getDailySpendAggregates(type: String, startTime: Long, endTime: Long): List<DailySpendAggregate> = emptyList()
        override suspend fun getTopPayeeAggregates(type: String, startTime: Long, endTime: Long, limit: Int): List<PayeeSpendAggregate> = emptyList()
        override suspend fun getSourceTypeAggregates(type: String, startTime: Long, endTime: Long): List<SourceTypeAggregate> = emptyList()
        override suspend fun getCategorySpendAggregates(type: String, startTime: Long, endTime: Long): List<CategorySpendAggregate> = emptyList()
        override suspend fun getBiggestTransactionsWithCategory(type: String, startTime: Long, endTime: Long, limit: Int): List<TransactionWithCategory> = emptyList()
        override suspend fun getBiggestSingleExpenseSync(): TransactionEntity? = null
        override suspend fun findByRefNo(refNo: String): TransactionEntity? = transactions.firstOrNull { it.refNo == refNo }
        override suspend fun findRecentMatching(type: String, amount: Double, minTimestamp: Long, maxTimestamp: Long): TransactionEntity? = null
        override suspend fun enrichCounterpartyIfEmpty(id: Long, counterparty: String, displayName: String) {}
        override suspend fun enrichRefNoIfEmpty(id: Long, refNo: String) {}
    }

    @Test
    fun testSetup_configuresCashAndOnline() = runBlocking {
        val walletDao = FakeWalletDao()
        val txDao = FakeTransactionDao()
        val repo = WalletRepository(
            context = null,
            db = null,
            walletDao = walletDao,
            transactionDao = txDao,
            walletPrefs = null
        )

        assertFalse(repo.isConfiguredSync())

        repo.setup(cashAmount = 2500.0, onlineAmount = 15000.0)

        assertTrue(repo.isConfiguredSync())
        val openings = repo.getOpeningBalancesSync()
        assertEquals(2500.0, openings["CASH"]?.openingAmount ?: 0.0, 0.001)
        assertEquals(15000.0, openings["ONLINE"]?.openingAmount ?: 0.0, 0.001)

        val liveBalances = repo.observeBalances().first()
        assertTrue(liveBalances.isConfigured)
        assertEquals(2500.0, liveBalances.cash, 0.001)
        assertEquals(15000.0, liveBalances.online, 0.001)
        assertEquals(17500.0, liveBalances.total, 0.001)
    }

    @Test
    fun testMoveMoney_createsTransferRowAndUpdatesDerivedBalances() = runBlocking {
        val walletDao = FakeWalletDao()
        val txDao = FakeTransactionDao()
        val repo = WalletRepository(
            context = null,
            db = null,
            walletDao = walletDao,
            transactionDao = txDao,
            walletPrefs = null
        )

        repo.setup(cashAmount = 1000.0, onlineAmount = 10000.0)

        // Move ₹2,000 from ONLINE to CASH (Withdraw cash from bank)
        val transferId = repo.moveMoney(fromWallet = "ONLINE", toWallet = "CASH", amount = 2000.0, note = "ATM withdraw")
        assertTrue(transferId > 0)

        val tx = txDao.transactions.first { it.id == transferId }
        assertEquals("WALLET_TRANSFER", tx.sourceType)
        assertEquals("ONLINE", tx.wallet)
        assertEquals("CASH", tx.transferToWallet)
        assertTrue(tx.isTransfer)

        val liveBalances = repo.observeBalances().first()
        // Online: 10000 - 2000 = 8000
        // Cash: 1000 + 2000 = 3000
        assertEquals(8000.0, liveBalances.online, 0.001)
        assertEquals(3000.0, liveBalances.cash, 0.001)
        assertEquals(11000.0, liveBalances.total, 0.001)
    }

    @Test
    fun testCorrect_updatesOpeningAmountPreservingOpeningTimestamp() = runBlocking {
        val walletDao = FakeWalletDao()
        val txDao = FakeTransactionDao()
        val repo = WalletRepository(
            context = null,
            db = null,
            walletDao = walletDao,
            transactionDao = txDao,
            walletPrefs = null
        )

        repo.setup(cashAmount = 1000.0, onlineAmount = 10000.0)
        val initialOpeningTs = walletDao.getBalance("CASH")!!.openingTimestamp

        // Correct Cash to ₹1,500
        repo.correct(wallet = "CASH", desiredBalance = 1500.0)

        val updatedCashEntity = walletDao.getBalance("CASH")!!
        assertEquals(1500.0, updatedCashEntity.openingAmount, 0.001)
        // Opening timestamp must be preserved
        assertEquals(initialOpeningTs, updatedCashEntity.openingTimestamp)

        val liveBalances = repo.observeBalances().first()
        assertEquals(1500.0, liveBalances.cash, 0.001)
    }

    @Test
    fun testReset_clearsBalances() = runBlocking {
        val walletDao = FakeWalletDao()
        val txDao = FakeTransactionDao()
        val repo = WalletRepository(
            context = null,
            db = null,
            walletDao = walletDao,
            transactionDao = txDao,
            walletPrefs = null
        )

        repo.setup(cashAmount = 1000.0, onlineAmount = 10000.0)
        assertTrue(repo.isConfiguredSync())

        repo.reset()

        assertFalse(repo.isConfiguredSync())
        assertEquals(0, walletDao.getCount())
        val liveBalances = repo.observeBalances().first()
        assertFalse(liveBalances.isConfigured)
    }
}
