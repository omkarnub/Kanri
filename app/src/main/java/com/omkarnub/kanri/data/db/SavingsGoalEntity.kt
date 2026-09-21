package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String, // e.g. "Emergency Fund", "New Laptop", "Trip to Goa"

    @ColumnInfo(name = "target_amount")
    val targetAmount: Double,

    @ColumnInfo(name = "current_amount")
    val currentAmount: Double = 0.0,

    @ColumnInfo(name = "target_date")
    val targetDate: Long? = null, // Epoch ms optional deadline

    @ColumnInfo(name = "color_hex")
    val colorHex: String = "#06D6A0",

    @ColumnInfo(name = "emoji")
    val emoji: String = "🎯",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)
