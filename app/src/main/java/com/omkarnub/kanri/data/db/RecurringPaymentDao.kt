package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringPaymentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurringPayment: RecurringPaymentEntity): Long

    @Update
    suspend fun update(recurringPayment: RecurringPaymentEntity)

    @Delete
    suspend fun delete(recurringPayment: RecurringPaymentEntity)

    @Query("SELECT * FROM recurring_payments ORDER BY next_due_timestamp ASC")
    fun getAllRecurringPayments(): Flow<List<RecurringPaymentEntity>>

    @Query("SELECT * FROM recurring_payments ORDER BY next_due_timestamp ASC")
    suspend fun getAllRecurringPaymentsSync(): List<RecurringPaymentEntity>

    @Query("SELECT * FROM recurring_payments WHERE is_active = 1 AND next_due_timestamp <= :cutoffTime ORDER BY next_due_timestamp ASC")
    suspend fun getDueSoon(cutoffTime: Long): List<RecurringPaymentEntity>

    @Query("SELECT * FROM recurring_payments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecurringPaymentEntity?

    @Query("SELECT COUNT(*) FROM recurring_payments")
    suspend fun getCount(): Int

    @Query("DELETE FROM recurring_payments")
    suspend fun deleteAll()
}
