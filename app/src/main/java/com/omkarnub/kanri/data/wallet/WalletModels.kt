package com.omkarnub.kanri.data.wallet

import androidx.room.ColumnInfo

data class WalletTxnRow(
    val id: Long,
    val type: String,
    val amount: Double,
    val wallet: String,
    @ColumnInfo(name = "transfer_to_wallet")
    val transferToWallet: String?,
    val timestamp: Long,
    @ColumnInfo(name = "is_duplicate")
    val isDuplicate: Boolean
)

data class WalletBalances(
    val cash: Double = 0.0,
    val online: Double = 0.0,
    val total: Double = 0.0,
    val isConfigured: Boolean = false,
    val cashOpeningTimestamp: Long? = null,
    val onlineOpeningTimestamp: Long? = null
)
