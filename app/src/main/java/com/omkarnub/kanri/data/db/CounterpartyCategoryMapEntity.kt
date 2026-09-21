package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counterparty_category_map")
data class CounterpartyCategoryMapEntity(
    @PrimaryKey
    @ColumnInfo(name = "counterparty")
    val counterparty: String,

    @ColumnInfo(name = "category_id")
    val categoryId: Long
)
