package com.omkarnub.kanri.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    @Query("SELECT * FROM wallet_balances")
    fun observeBalances(): Flow<List<WalletBalanceEntity>>

    @Query("SELECT * FROM wallet_balances")
    suspend fun getBalancesSync(): List<WalletBalanceEntity>

    @Query("SELECT * FROM wallet_balances WHERE wallet_id = :walletId LIMIT 1")
    suspend fun getBalance(walletId: String): WalletBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(balances: List<WalletBalanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(balance: WalletBalanceEntity)

    @Query("UPDATE wallet_balances SET opening_amount = :amount WHERE wallet_id = :walletId")
    suspend fun updateOpeningAmount(walletId: String, amount: Double)

    @Query("DELETE FROM wallet_balances")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM wallet_balances")
    suspend fun getCount(): Int
}
