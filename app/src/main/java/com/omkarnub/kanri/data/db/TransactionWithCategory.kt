package com.omkarnub.kanri.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithCategory(
    @Embedded
    val transaction: TransactionEntity,

    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: CategoryEntity? = null
) {
    val isTransfer: Boolean
        get() = transaction.isTransfer
}
