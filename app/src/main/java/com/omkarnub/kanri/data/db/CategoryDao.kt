package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Query("SELECT * FROM counterparty_category_map WHERE counterparty = :counterparty LIMIT 1")
    suspend fun getMappingForCounterparty(counterparty: String): CounterpartyCategoryMapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMapping(mapping: CounterpartyCategoryMapEntity)

    @Query("SELECT * FROM counterparty_category_map")
    suspend fun getAllMappings(): List<CounterpartyCategoryMapEntity>

    @Query("SELECT * FROM counterparty_category_map ORDER BY counterparty ASC")
    fun getAllMappingsFlow(): Flow<List<CounterpartyCategoryMapEntity>>

    @Query("SELECT * FROM counterparty_category_map WHERE :counterparty LIKE '%' || counterparty || '%' ORDER BY length(counterparty) DESC LIMIT 1")
    suspend fun findSmartRuleForCounterparty(counterparty: String): CounterpartyCategoryMapEntity?

    @Query("DELETE FROM counterparty_category_map WHERE counterparty = :counterparty")
    suspend fun deleteMapping(counterparty: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMappings(mappings: List<CounterpartyCategoryMapEntity>)

    @Query("DELETE FROM counterparty_category_map")
    suspend fun deleteAllMappings()

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @androidx.room.Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM counterparty_category_map WHERE category_id = :categoryId")
    suspend fun deleteMappingsForCategory(categoryId: Long)
}
