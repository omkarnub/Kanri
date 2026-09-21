package com.omkarnub.kanri.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class LendingWithRepayments(
    @Embedded
    val lending: LendingEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "lending_id"
    )
    val repayments: List<LendingRepaymentEntity> = emptyList()
) {
    val effectiveOriginal: Double
        get() = lending.originalAmount ?: lending.amount

    val outstanding: Double
        get() = if (lending.isSettled) 0.0 else lending.amount

    val totalRepaid: Double
        get() = repayments.sumOf { it.amount }

    fun isOverdue(startOfTodayMillis: Long): Boolean {
        if (lending.isSettled) return false
        val due = lending.dueDate ?: return false
        return due < startOfTodayMillis
    }
}
