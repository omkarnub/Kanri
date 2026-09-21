package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets WHERE month_key = :monthKey LIMIT 1")
    fun getBudgetForMonth(monthKey: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets WHERE month_key = :monthKey LIMIT 1")
    suspend fun getBudgetForMonthSync(monthKey: String): BudgetEntity?

    @Query("SELECT * FROM budgets ORDER BY month_key DESC LIMIT 1")
    fun getLatestBudget(): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetEntity)

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("SELECT * FROM budgets WHERE month_key LIKE :prefix")
    fun getCategoryBudgetsForMonth(prefix: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE month_key LIKE :prefix")
    suspend fun getCategoryBudgetsForMonthSync(prefix: String): List<BudgetEntity>
}

