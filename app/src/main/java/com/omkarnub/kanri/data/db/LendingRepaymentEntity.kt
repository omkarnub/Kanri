package com.omkarnub.kanri.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lending_repayments",
    foreignKeys = [
        ForeignKey(
            entity = LendingEntity::class,
            parentColumns = ["id"],
            childColumns = ["lending_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["lending_id"])
    ]
)
data class LendingRepaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "lending_id")
    val lendingId: Long,

    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "paid_at")
    val paidAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "note")
    val note: String? = null
)
