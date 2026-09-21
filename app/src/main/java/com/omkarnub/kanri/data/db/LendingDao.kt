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
}

