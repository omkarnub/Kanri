package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_payments")
data class RecurringPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String, // e.g. "Netflix", "House Rent", "WiFi Bill"

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "billing_cycle")
    val billingCycle: String = "MONTHLY", // "MONTHLY", "YEARLY", "WEEKLY"

    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,

    @ColumnInfo(name = "counterparty")
    val counterparty: String? = null,

    @ColumnInfo(name = "next_due_timestamp")
    val nextDueTimestamp: Long, // Epoch ms

    @ColumnInfo(name = "auto_detect_keyword")
    val autoDetectKeyword: String? = null,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "last_paid_timestamp")
    val lastPaidTimestamp: Long? = null
)
