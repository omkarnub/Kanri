package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LendingDao {

    @Query("SELECT * FROM lending_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<LendingEntity>>

    @Query("SELECT * FROM lending_records WHERE type = :type ORDER BY date DESC")
    fun getRecordsByType(type: String): Flow<List<LendingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: LendingEntity): Long

    @Update
    suspend fun update(record: LendingEntity)

    @Delete
    suspend fun delete(record: LendingEntity)

    @Query("UPDATE lending_records SET is_settled = :settled WHERE id = :id")
    suspend fun setSettled(id: Long, settled: Boolean)

    @Query("SELECT SUM(amount) FROM lending_records WHERE type = 'LENT' AND is_settled = 0")
    fun getTotalLentPending(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM lending_records WHERE type = 'BORROWED' AND is_settled = 0")
    fun getTotalBorrowedPending(): Flow<Double?>

    @Query("SELECT * FROM lending_records ORDER BY date DESC")
    suspend fun getAllRecordsSync(): List<LendingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<LendingEntity>)

    @Query("DELETE FROM lending_records")
    suspend fun deleteAllRecords()

    @Query("SELECT * FROM lending_records WHERE date >= :startTime AND date <= :endTime AND (notes LIKE 'Split:%' OR notes LIKE 'Split Bill%') ORDER BY date DESC")
    suspend fun getSplitRecordsBetweenSync(startTime: Long, endTime: Long): List<LendingEntity>

    @Query("SELECT * FROM lending_records WHERE id = :id")
    suspend fun getRecordById(id: Long): LendingEntity?

    @androidx.room.Transaction
    @Query("SELECT * FROM lending_records ORDER BY date DESC")
    fun getAllRecordsWithRepayments(): Flow<List<LendingWithRepayments>>

    @androidx.room.Transaction
    @Query("SELECT * FROM lending_records WHERE id = :id")
    suspend fun getRecordWithRepaymentsById(id: Long): LendingWithRepayments?

    @androidx.room.Transaction
    @Query("SELECT * FROM lending_records ORDER BY date DESC")
    suspend fun getAllRecordsWithRepaymentsSync(): List<LendingWithRepayments>

    @Query("SELECT * FROM lending_records WHERE type = :type AND is_settled = 0 ORDER BY date ASC, id ASC")
    suspend fun getOpenRecordsByTypeSync(type: String): List<LendingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LendingRepaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRepayments(repayments: List<LendingRepaymentEntity>)

    @Delete
    suspend fun deleteRepayment(repayment: LendingRepaymentEntity)

    @Query("DELETE FROM lending_repayments WHERE id = :id")
    suspend fun deleteRepaymentById(id: Long)

    @Query("DELETE FROM lending_repayments WHERE lending_id = :lendingId")
    suspend fun deleteRepaymentsForLending(lendingId: Long)

    @Query("SELECT * FROM lending_repayments WHERE lending_id = :lendingId ORDER BY paid_at DESC, id DESC")
    suspend fun getRepaymentsForLending(lendingId: Long): List<LendingRepaymentEntity>

    @Query("SELECT * FROM lending_repayments ORDER BY paid_at DESC, id DESC")
    suspend fun getAllRepaymentsSync(): List<LendingRepaymentEntity>

    @Query("DELETE FROM lending_repayments")
    suspend fun deleteAllRepayments()

    @Query("UPDATE lending_records SET person_name = :newName WHERE person_name = :oldName COLLATE NOCASE")
    suspend fun updatePersonName(oldName: String, newName: String)

    @androidx.room.Transaction
    suspend fun repayRecord(
        recordId: Long,
        repaymentAmount: Double,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): Result<Long> {
        if (repaymentAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("Repayment amount must be greater than zero"))
        }
        val record = getRecordById(recordId)
            ?: return Result.failure(IllegalArgumentException("Lending record not found"))
        if (record.isSettled) {
            return Result.failure(IllegalStateException("Cannot repay an already settled record"))
        }
        val outstanding = record.amount
        if (repaymentAmount > outstanding + 0.0001) {
            return Result.failure(IllegalArgumentException("Repayment amount cannot exceed outstanding balance ($outstanding)"))
        }

        val original = record.originalAmount ?: record.amount
        val repaymentId = insertRepayment(
            LendingRepaymentEntity(
                lendingId = recordId,
                amount = repaymentAmount,
                paidAt = paidAt,
                note = note
            )
        )

        val isExact = kotlin.math.abs(repaymentAmount - outstanding) < 0.0001 || repaymentAmount >= outstanding
        if (isExact) {
            update(
                record.copy(
                    originalAmount = original,
                    amount = record.amount,
                    isSettled = true
                )
            )
        } else {
            update(
                record.copy(
                    originalAmount = original,
                    amount = (record.amount - repaymentAmount).coerceAtLeast(0.0),
                    isSettled = false
                )
            )
        }

        return Result.success(repaymentId)
    }

    @androidx.room.Transaction
    suspend fun settleAllRecord(
        recordId: Long,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): Result<Long> {
        val record = getRecordById(recordId)
            ?: return Result.failure(IllegalArgumentException("Lending record not found"))
        if (record.isSettled) {
            return Result.failure(IllegalStateException("Record is already settled"))
        }
        return repayRecord(recordId, record.amount, paidAt, note)
    }

    @androidx.room.Transaction
    suspend fun undoLastRepayment(recordId: Long): Result<Unit> {
        val record = getRecordById(recordId)
            ?: return Result.failure(IllegalArgumentException("Lending record not found"))
        val repayments = getRepaymentsForLending(recordId)
        if (repayments.isEmpty()) {
            return Result.failure(IllegalStateException("No repayments to undo"))
        }
        val latest = repayments.first()

        if (record.isSettled) {
            update(
                record.copy(
                    isSettled = false
                )
            )
        } else {
            update(
                record.copy(
                    amount = record.amount + latest.amount
                )
            )
        }

        deleteRepayment(latest)
        return Result.success(Unit)
    }

    @androidx.room.Transaction
    suspend fun reopenLegacyRecord(recordId: Long): Result<Unit> {
        val record = getRecordById(recordId)
            ?: return Result.failure(IllegalArgumentException("Lending record not found"))
        val repayments = getRepaymentsForLending(recordId)
        if (repayments.isNotEmpty()) {
            return Result.failure(IllegalStateException("Use undoLastRepayment for records with repayment history"))
        }
        update(record.copy(isSettled = false))
        return Result.success(Unit)
    }

    @androidx.room.Transaction
    suspend fun deleteRecordWithRepayments(recordId: Long) {
        val record = getRecordById(recordId) ?: return
        deleteRepaymentsForLending(recordId)
        delete(record)
    }

    @androidx.room.Transaction
    suspend fun restoreRecordWithRepayments(
        record: LendingEntity,
        repayments: List<LendingRepaymentEntity>
    ) {
        insert(record)
        if (repayments.isNotEmpty()) {
            insertAllRepayments(repayments)
        }
    }

    @androidx.room.Transaction
    suspend fun allocatePersonRepayment(
        personName: String,
        type: String,
        repaymentAmount: Double,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): Result<Double> {
        if (repaymentAmount <= 0.0) {
            return Result.failure(IllegalArgumentException("Repayment amount must be greater than zero"))
        }
        val allOpen = getOpenRecordsByTypeSync(type).filter {
            it.personName.trim().equals(personName.trim(), ignoreCase = true)
        }
        val totalOutstanding = allOpen.sumOf { it.amount }
        val toAllocate = kotlin.math.min(repaymentAmount, totalOutstanding)
        var remaining = toAllocate

        for (entry in allOpen) {
            if (remaining <= 0.0001) break
            val repayNow = kotlin.math.min(remaining, entry.amount)
            val res = repayRecord(entry.id, repayNow, paidAt, note)
            if (res.isFailure) {
                return Result.failure(res.exceptionOrNull() ?: Exception("Failed to allocate repayment"))
            }
            remaining -= repayNow
        }

        return Result.success(toAllocate)
    }
}

