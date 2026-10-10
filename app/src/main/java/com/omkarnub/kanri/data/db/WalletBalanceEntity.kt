package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_balances")
data class WalletBalanceEntity(
    @PrimaryKey
    @ColumnInfo(name = "wallet_id")
    val walletId: String, // "CASH" or "ONLINE"

    @ColumnInfo(name = "opening_amount")
    val openingAmount: Double,

    @ColumnInfo(name = "opening_timestamp")
    val openingTimestamp: Long
)
