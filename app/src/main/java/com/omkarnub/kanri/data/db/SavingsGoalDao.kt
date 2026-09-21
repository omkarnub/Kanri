package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: SavingsGoalEntity): Long

    @Update
    suspend fun update(goal: SavingsGoalEntity)

    @Delete
    suspend fun delete(goal: SavingsGoalEntity)

    @Query("SELECT * FROM savings_goals ORDER BY is_completed ASC, id DESC")
    fun getAllGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY is_completed ASC, id DESC")
    suspend fun getAllGoalsSync(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavingsGoalEntity?

    @Query("UPDATE savings_goals SET current_amount = :newAmount, is_completed = :isCompleted WHERE id = :id")
    suspend fun updateProgress(id: Long, newAmount: Double, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM savings_goals")
    suspend fun getCount(): Int

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAll()
}
