package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "type")
    val type: String, // "DEBIT" or "CREDIT"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "source_type")
    val sourceType: String, // "UPI", "ATM", "CARD", "BANK_TRANSFER", "UNKNOWN"

    @ColumnInfo(name = "counterparty")
    val counterparty: String?, // VPA, merchant, account, or ATM TID

    @ColumnInfo(name = "display_name")
    val displayName: String? = null,

    @ColumnInfo(name = "bank")
    val bank: String?,

    @ColumnInfo(name = "ref_no")
    val refNo: String?,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long, // Epoch milliseconds

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "raw_sms")
    val rawSms: String,

    @ColumnInfo(name = "is_duplicate")
    val isDuplicate: Boolean = false,

    @ColumnInfo(name = "is_manual_entry")
    val isManualEntry: Boolean = false,

    @ColumnInfo(name = "needs_review", defaultValue = "0")
    val needsReview: Boolean = false,

    @ColumnInfo(name = "review_reason")
    val reviewReason: String? = null,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
