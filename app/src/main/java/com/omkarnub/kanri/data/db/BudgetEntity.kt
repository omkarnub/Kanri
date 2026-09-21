package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    @ColumnInfo(name = "month_key")
    val monthKey: String, // format "yyyy-MM", e.g. "2026-09"

    @ColumnInfo(name = "monthly_limit")
    val monthlyLimit: Double
)
