package com.omkarnub.kanri.data.parser

import com.omkarnub.kanri.data.db.CategoryDao
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CategorySpendAggregate
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.DailySpendAggregate
import com.omkarnub.kanri.data.db.PayeeSpendAggregate
import com.omkarnub.kanri.data.db.SourceTypeAggregate
import com.omkarnub.kanri.data.db.TransactionDao
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DeduplicationLogicTest {

    private class FakeTransactionDao : TransactionDao {
        val transactions = mutableListOf<TransactionEntity>()
        private var nextId = 1L

        override suspend fun insert(transaction: TransactionEntity): Long {
            val assigned = transaction.copy(id = if (transaction.id == 0L) nextId++ else transaction.id)
            transactions.removeAll { it.id == assigned.id }
            transactions.add(assigned)
            return assigned.id
        }

        override suspend fun findByRefNo(refNo: String): TransactionEntity? {
            return transactions.firstOrNull { it.refNo == refNo }
        }

        override suspend fun findRecentMatching(
            type: String,
            amount: Double,
            minTimestamp: Long,
            maxTimestamp: Long
        ): TransactionEntity? {
            return transactions.firstOrNull {
                it.type.equals(type, ignoreCase = true) &&
                        abs(it.amount - amount) < 0.01 &&
                        it.timestamp in minTimestamp..maxTimestamp
            }
        }

        override suspend fun enrichCounterpartyIfEmpty(id: Long, counterparty: String, displayName: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                val current = transactions[idx]
                transactions[idx] = current.copy(counterparty = counterparty, displayName = displayName)
            }
        }

        override suspend fun enrichRefNoIfEmpty(id: Long, refNo: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                val current = transactions[idx]
                transactions[idx] = current.copy(refNo = refNo)
            }
        }

        override suspend fun enrichBankIfGeneric(id: Long, bank: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                val current = transactions[idx]
                transactions[idx] = current.copy(bank = bank)
            }
        }

        override suspend fun enrichRawSmsIfFromNotification(id: Long, rawSms: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                val current = transactions[idx]
                transactions[idx] = current.copy(rawSms = rawSms)
            }
        }

        // Unused stubs
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
        override fun observeWalletTransactions(): Flow<List<com.omkarnub.kanri.data.wallet.WalletTxnRow>> = emptyFlow()
        override suspend fun getWalletTransactionsSync(): List<com.omkarnub.kanri.data.wallet.WalletTxnRow> = transactions.map {
            com.omkarnub.kanri.data.wallet.WalletTxnRow(it.id, it.type, it.amount, it.wallet, it.transferToWallet, it.timestamp, it.isDuplicate)
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
            return count
        }
        override suspend fun convertTransactionToTransfer(id: Long, wallet: String, transferToWallet: String): Int {
            val idx = transactions.indexOfFirst { it.id == id }
            return if (idx != -1) {
                transactions[idx] = transactions[idx].copy(wallet = wallet, transferToWallet = transferToWallet, categoryId = null, needsReview = false, reviewReason = null)
                1
            } else 0
        }
        override suspend fun updateWallet(id: Long, wallet: String) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                transactions[idx] = transactions[idx].copy(wallet = wallet)
            }
        }
        override suspend fun updateWalletAndTransfer(id: Long, wallet: String, transferToWallet: String?) {
            val idx = transactions.indexOfFirst { it.id == id }
            if (idx != -1) {
                transactions[idx] = transactions[idx].copy(wallet = wallet, transferToWallet = transferToWallet)
            }
        }
    }

    private class FakeCategoryDao : CategoryDao {
        override fun getAllCategories(): Flow<List<CategoryEntity>> = emptyFlow()
        override suspend fun getAllCategoriesSync(): List<CategoryEntity> = emptyList()
        override suspend fun getCategoryByName(name: String): CategoryEntity? = null
        override suspend fun getCategoryById(id: Long): CategoryEntity? = null
        override suspend fun insertCategories(categories: List<CategoryEntity>) {}
        override suspend fun insertCategory(category: CategoryEntity): Long = 1L
        override suspend fun getCategoryCount(): Int = 0
        override suspend fun getMappingForCounterparty(counterparty: String): CounterpartyCategoryMapEntity? = null
        override suspend fun setMapping(mapping: CounterpartyCategoryMapEntity) {}
        override suspend fun getAllMappings(): List<CounterpartyCategoryMapEntity> = emptyList()
        override fun getAllMappingsFlow(): Flow<List<CounterpartyCategoryMapEntity>> = emptyFlow()
        override suspend fun findSmartRuleForCounterparty(counterparty: String): CounterpartyCategoryMapEntity? = null
        override suspend fun deleteMapping(counterparty: String) {}
        override suspend fun insertMappings(mappings: List<CounterpartyCategoryMapEntity>) {}
        override suspend fun deleteAllMappings() {}
        override suspend fun deleteAllCategories() {}
        override suspend fun updateCategory(category: CategoryEntity) {}
        override suspend fun deleteCategoryById(id: Long) {}
        override suspend fun deleteMappingsForCategory(categoryId: Long) {}
    }

    @Test
    fun testSmsFirst_ThenNotificationWithin5Min_MergesAndEnrichesCounterparty() = runBlocking {
        val dao = FakeTransactionDao()
        val categoryDao = FakeCategoryDao()
        val baseTime = 1700000000000L

        // 1. Bank SMS arrives first (has bank & refNo, generic counterparty "UPI")
        val smsTx = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 500.0,
            sourceType = SourceType.UPI,
            counterparty = "UPI",
            bank = "HDFC Bank",
            refNo = "425678912345",
            account = "••8477",
            rawText = "A/c ...8477 credited with Rs 500.00 Ref 425678912345",
            timestamp = baseTime
        )

        val res1 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, smsTx)
        assertTrue(res1 is DeduplicationResult.Inserted)
        assertEquals(1, dao.transactions.size)
        assertEquals("UPI", dao.transactions[0].counterparty)

        // 2. Google Pay notification arrives 30s later (has counterparty "Rohit Sharma", same amount ₹500)
        val notifTx = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 500.0,
            sourceType = SourceType.UPI,
            counterparty = "Rohit Sharma",
            bank = "Google Pay",
            refNo = null,
            account = null,
            rawText = "Rohit Sharma sent you ₹500 via Google Pay",
            timestamp = baseTime + 30_000L // 30 seconds later
        )

        val res2 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, notifTx)
        assertTrue("Notification within 5 min should merge and enrich existing SMS transaction", res2 is DeduplicationResult.Enriched)
        assertEquals("Should not insert a second row", 1, dao.transactions.size)
        assertEquals("Counterparty should be enriched from notification", "Rohit Sharma", dao.transactions[0].counterparty)
        assertEquals("Bank should remain HDFC Bank", "HDFC Bank", dao.transactions[0].bank)
        assertEquals("RefNo should remain 425678912345", "425678912345", dao.transactions[0].refNo)
    }

    @Test
    fun testNotificationFirst_ThenSmsWithin5Min_MergesAndEnrichesBankAndRefNo() = runBlocking {
        val dao = FakeTransactionDao()
        val categoryDao = FakeCategoryDao()
        val baseTime = 1700000000000L

        // 1. Google Pay notification arrives first
        val notifTx = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 1000.0,
            sourceType = SourceType.UPI,
            counterparty = "Priya Patel",
            bank = "Google Pay",
            refNo = null,
            account = null,
            rawText = "Priya Patel sent you ₹1,000",
            timestamp = baseTime
        )

        val res1 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, notifTx)
        assertTrue(res1 is DeduplicationResult.Inserted)
        assertEquals(1, dao.transactions.size)
        assertEquals("Google Pay", dao.transactions[0].bank)
        assertEquals(null, dao.transactions[0].refNo)

        // 2. Bank SMS arrives 45 seconds later
        val smsTx = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 1000.0,
            sourceType = SourceType.UPI,
            counterparty = "UPI",
            bank = "State Bank of India",
            refNo = "SBI9988776655",
            account = "••1234",
            rawText = "Your A/C ...1234 is credited with Rs 1,000.00 Ref SBI9988776655",
            timestamp = baseTime + 45_000L
        )

        val res2 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, smsTx)
        assertTrue("SMS within 5 min should merge into notification transaction", res2 is DeduplicationResult.Enriched)
        assertEquals("Should remain single merged transaction", 1, dao.transactions.size)
        assertEquals("Counterparty from notification should be preserved", "Priya Patel", dao.transactions[0].counterparty)
        assertEquals("Bank should be enriched to State Bank of India", "State Bank of India", dao.transactions[0].bank)
        assertEquals("RefNo should be enriched from bank SMS", "SBI9988776655", dao.transactions[0].refNo)
    }

    @Test
    fun testTwoPaymentsMoreThan5MinutesApart_KeptAsTwoSeparateEntries() = runBlocking {
        val dao = FakeTransactionDao()
        val categoryDao = FakeCategoryDao()
        val baseTime = 1700000000000L

        // First payment
        val tx1 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 500.0,
            sourceType = SourceType.UPI,
            counterparty = "Rohit",
            bank = "HDFC Bank",
            refNo = null,
            account = null,
            rawText = "Tx 1",
            timestamp = baseTime
        )
        val res1 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx1)
        assertTrue(res1 is DeduplicationResult.Inserted)

        // Second payment 6 minutes later (360,000 ms > 300,000 ms window)
        val tx2 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 500.0,
            sourceType = SourceType.UPI,
            counterparty = "Rohit",
            bank = "HDFC Bank",
            refNo = null,
            account = null,
            rawText = "Tx 2",
            timestamp = baseTime + 6 * 60 * 1000L
        )
        val res2 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx2)
        assertTrue("Payments > 5m apart must be inserted as separate entries", res2 is DeduplicationResult.Inserted)
        assertEquals("Should have 2 distinct transactions", 2, dao.transactions.size)
    }

    @Test
    fun testDifferentAmountsWithin5Minutes_KeptAsSeparateEntries() = runBlocking {
        val dao = FakeTransactionDao()
        val categoryDao = FakeCategoryDao()
        val baseTime = 1700000000000L

        val tx1 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 500.0,
            sourceType = SourceType.UPI,
            counterparty = "User A",
            bank = "HDFC Bank",
            refNo = null,
            account = null,
            rawText = "₹500",
            timestamp = baseTime
        )
        val tx2 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 250.0,
            sourceType = SourceType.UPI,
            counterparty = "User A",
            bank = "HDFC Bank",
            refNo = null,
            account = null,
            rawText = "₹250",
            timestamp = baseTime + 10_000L
        )

        NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx1)
        val res2 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx2)
        assertTrue(res2 is DeduplicationResult.Inserted)
        assertEquals(2, dao.transactions.size)
    }

    @Test
    fun testDifferentRefNumbersWithin5Minutes_KeptAsSeparateEntries() = runBlocking {
        val dao = FakeTransactionDao()
        val categoryDao = FakeCategoryDao()
        val baseTime = 1700000000000L

        val tx1 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 100.0,
            sourceType = SourceType.UPI,
            counterparty = "UPI",
            bank = "SBI",
            refNo = "UTR_FIRST_111",
            account = null,
            rawText = "UTR_FIRST_111",
            timestamp = baseTime
        )
        val tx2 = ParsedTransaction(
            type = TransactionType.CREDIT,
            amount = 100.0,
            sourceType = SourceType.UPI,
            counterparty = "UPI",
            bank = "SBI",
            refNo = "UTR_SECOND_222",
            account = null,
            rawText = "UTR_SECOND_222",
            timestamp = baseTime + 20_000L
        )

        NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx1)
        val res2 = NotificationDeduplicationHelper.processIncomingTransaction(dao, categoryDao, tx2)
        assertTrue("Different UTRs must not be merged", res2 is DeduplicationResult.Inserted)
        assertEquals(2, dao.transactions.size)
    }

    @Test
    fun testUpiCreditDeduplication_duplicateIgnoredByWalletBalances() = runBlocking {
        val rows = listOf(
            com.omkarnub.kanri.data.wallet.WalletTxnRow(
                id = 1L,
                type = "CREDIT",
                amount = 1500.0,
                wallet = "ONLINE",
                transferToWallet = null,
                timestamp = 1700000000000L,
                isDuplicate = false
            ),
            com.omkarnub.kanri.data.wallet.WalletTxnRow(
                id = 2L,
                type = "CREDIT",
                amount = 1500.0,
                wallet = "ONLINE",
                transferToWallet = null,
                timestamp = 1700000010000L,
                isDuplicate = true // Duplicate row from notification dedupe
            )
        )

        val balance = com.omkarnub.kanri.data.wallet.WalletBalanceCalculator.balance(
            wallet = "ONLINE",
            openingAmount = 10000.0,
            openingTimestamp = 1699990000000L,
            transactions = rows
        )

        // 10,000 + 1,500 = 11,500 (duplicate 1,500 row strictly ignored)
        assertEquals(11500.0, balance, 0.001)
    }
}
