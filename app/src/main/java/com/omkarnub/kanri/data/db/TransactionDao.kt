package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE ref_no = :refNo LIMIT 1")
    suspend fun findByRefNo(refNo: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeTransactionWithCategory(id: Long): Flow<TransactionWithCategory?>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionWithCategoryById(id: Long): TransactionWithCategory?

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions WHERE ref_no = :refNo")
    suspend fun deleteByRefNo(refNo: String)

    @Query("DELETE FROM transactions WHERE ref_no LIKE :pattern")
    suspend fun deleteByRefNoPattern(pattern: String)

    @Query("SELECT * FROM transactions WHERE ref_no LIKE :pattern")
    suspend fun getTransactionsByRefNoPattern(pattern: String): List<TransactionEntity>

    @Query("UPDATE transactions SET counterparty = :newName, display_name = :newName WHERE counterparty = :oldName COLLATE NOCASE AND source_type = 'LENDING'")
    suspend fun updateCounterpartyForLending(oldName: String, newName: String)

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getTransactionsWithCategoryBetweenSync(startTime: Long, endTime: Long): List<TransactionWithCategory>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsWithCategorySync(): List<TransactionWithCategory>

    @Query("SELECT COUNT(*) FROM transactions WHERE needs_review = 1 OR category_id IS NULL")
    fun observeReviewCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE needs_review = 1 OR category_id IS NULL ORDER BY timestamp DESC")
    fun observeReviewQueue(): Flow<List<TransactionEntity>>

    @Query("UPDATE transactions SET needs_review = 0, review_reason = NULL WHERE id = :transactionId")
    suspend fun clearReviewFlag(transactionId: Long)

    @Query("UPDATE transactions SET needs_review = 0, review_reason = NULL WHERE id IN (:transactionIds)")
    suspend fun clearReviewFlags(transactionIds: List<Long>)

    @Query("UPDATE transactions SET needs_review = 0, review_reason = NULL")
    suspend fun clearAllReviewFlags()

    @Query("UPDATE transactions SET category_id = :categoryId, needs_review = 0, review_reason = NULL WHERE id = :transactionId")
    suspend fun updateCategoryId(transactionId: Long, categoryId: Long)

    @Query("UPDATE transactions SET category_id = :categoryId, notes = :notes, needs_review = 0, review_reason = NULL WHERE id = :transactionId")
    suspend fun updateCategoryAndNotes(transactionId: Long, categoryId: Long, notes: String?)

    @Query("UPDATE transactions SET category_id = :categoryId, needs_review = 0, review_reason = NULL WHERE counterparty = :counterparty")
    suspend fun updateCategoryForCounterparty(counterparty: String, categoryId: Long)

    @Query("UPDATE transactions SET category_id = :categoryId, needs_review = 0, review_reason = NULL WHERE counterparty LIKE '%' || :keyword || '%' OR display_name LIKE '%' || :keyword || '%'")
    suspend fun applyCategoryToMatchingTransactions(keyword: String, categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE counterparty LIKE '%' || :keyword || '%' OR display_name LIKE '%' || :keyword || '%'")
    suspend fun countTransactionsMatchingKeyword(keyword: String): Int

    @Query("UPDATE transactions SET category_id = :fallbackCategoryId WHERE category_id = :deletedCategoryId")
    suspend fun reassignTransactionsCategory(deletedCategoryId: Long, fallbackCategoryId: Long?)

    @Query("SELECT * FROM transactions WHERE type = :type AND ABS(amount - :amount) < 0.01 AND timestamp >= :minTimestamp AND timestamp <= :maxTimestamp LIMIT 1")
    suspend fun findRecentMatching(type: String, amount: Double, minTimestamp: Long, maxTimestamp: Long): TransactionEntity?

    @Query("UPDATE transactions SET counterparty = :counterparty, display_name = :displayName WHERE id = :id AND (counterparty IS NULL OR counterparty = '' OR counterparty = 'UPI')")
    suspend fun enrichCounterpartyIfEmpty(id: Long, counterparty: String, displayName: String)

    @Query("UPDATE transactions SET ref_no = :refNo WHERE id = :id AND (ref_no IS NULL OR ref_no = '')")
    suspend fun enrichRefNoIfEmpty(id: Long, refNo: String)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getCount(): Int

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTransactionsSync(limit: Int = 3): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    // -------------------------------------------------------------------------
    // Additive Insights Queries
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsBetweenSync(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsByTypeBetweenSync(type: String, startTime: Long, endTime: Long): List<TransactionEntity>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE counterparty = :counterparty AND is_duplicate = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsWithCategoryByCounterpartySync(counterparty: String): List<TransactionWithCategory>

    @androidx.room.Transaction
    @Query("SELECT * FROM transactions WHERE counterparty = :counterparty AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0 ORDER BY timestamp DESC")
    suspend fun getTransactionsWithCategoryByCounterpartyBetweenSync(counterparty: String, startTime: Long, endTime: Long): List<TransactionWithCategory>

    @Query("""
        SELECT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch', 'localtime') AS dayString,
               SUM(amount) AS totalAmount,
               COUNT(*) AS txCount
        FROM transactions
        WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0
        GROUP BY dayString
        ORDER BY dayString ASC
    """)
    suspend fun getDailySpendAggregates(type: String, startTime: Long, endTime: Long): List<DailySpendAggregate>

    @Query("""
        SELECT counterparty AS counterparty,
               display_name AS displayName,
               SUM(amount) AS totalAmount,
               COUNT(*) AS txCount
        FROM transactions
        WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0
              AND counterparty IS NOT NULL AND TRIM(counterparty) != '' AND source_type != 'ATM'
        GROUP BY LOWER(TRIM(counterparty))
        ORDER BY totalAmount DESC
        LIMIT :limit
    """)
    suspend fun getTopPayeeAggregates(type: String, startTime: Long, endTime: Long, limit: Int = 15): List<PayeeSpendAggregate>

    @Query("""
        SELECT source_type AS sourceType,
               SUM(amount) AS totalAmount,
               COUNT(*) AS txCount
        FROM transactions
        WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0
        GROUP BY source_type
        ORDER BY totalAmount DESC
    """)
    suspend fun getSourceTypeAggregates(type: String, startTime: Long, endTime: Long): List<SourceTypeAggregate>

    @Query("""
        SELECT category_id AS categoryId,
               SUM(amount) AS totalAmount,
               COUNT(*) AS txCount
        FROM transactions
        WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0
        GROUP BY category_id
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategorySpendAggregates(type: String, startTime: Long, endTime: Long): List<CategorySpendAggregate>

    @androidx.room.Transaction
    @Query("""
        SELECT * FROM transactions
        WHERE type = :type AND timestamp >= :startTime AND timestamp <= :endTime AND is_duplicate = 0
        ORDER BY amount DESC
        LIMIT :limit
    """)
    suspend fun getBiggestTransactionsWithCategory(type: String, startTime: Long, endTime: Long, limit: Int = 5): List<TransactionWithCategory>

    @Query("""
        SELECT * FROM transactions
        WHERE type = 'DEBIT' AND is_duplicate = 0
        ORDER BY amount DESC
        LIMIT 1
    """)
    suspend fun getBiggestSingleExpenseSync(): TransactionEntity?
}

data class PayeeSpendAggregate(
    val counterparty: String?,
    val displayName: String?,
    val totalAmount: Double,
    val txCount: Int
)

data class SourceTypeAggregate(
    val sourceType: String,
    val totalAmount: Double,
    val txCount: Int
)

data class CategorySpendAggregate(
    val categoryId: Long?,
    val totalAmount: Double,
    val txCount: Int
)

data class DailySpendAggregate(
    val dayString: String,
    val totalAmount: Double,
    val txCount: Int
)

