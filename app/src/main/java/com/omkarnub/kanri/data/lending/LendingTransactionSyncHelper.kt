package com.omkarnub.kanri.data.lending

import android.content.Context
import com.omkarnub.kanri.data.db.CategoryDao
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Synchronizes Lend & Borrow activities (records, repayments, edits, deletes)
 * with the core Kanri transaction history and income/expense flows.
 *
 * Financial mapping:
 * - LENT (money given to someone) -> DEBIT (Expense)
 * - BORROWED (money received from someone) -> CREDIT (Income)
 * - Repayment of LENT (borrower pays back) -> CREDIT (Income)
 * - Repayment of BORROWED (user pays lender back) -> DEBIT (Expense)
 */
object LendingTransactionSyncHelper {

    const val CATEGORY_NAME = "Lend & Borrow"
    const val CATEGORY_COLOR = "#30A46C"
    const val CATEGORY_ICON = "payments"
    const val SOURCE_TYPE_LENDING = "LENDING"
    const val REF_PREFIX_RECORD = "LENDING_"
    const val REF_PREFIX_REPAY = "LENDING_REPAY_"

    suspend fun getOrCreateLendBorrowCategoryId(categoryDao: CategoryDao): Long {
        val existing = categoryDao.getCategoryByName(CATEGORY_NAME)
        if (existing != null) {
            return existing.id
        }
        val all = categoryDao.getAllCategoriesSync()
        val found = all.find { it.name.trim().equals(CATEGORY_NAME, ignoreCase = true) }
        if (found != null) {
            return found.id
        }

        val newCat = CategoryEntity(
            name = CATEGORY_NAME,
            colorHex = CATEGORY_COLOR,
            iconName = CATEGORY_ICON,
            isCustom = false
        )
        return categoryDao.insertCategory(newCat)
    }

    suspend fun syncLendingRecord(
        recordId: Long,
        db: KanriDatabase,
        context: Context? = null,
        wallet: String? = null
    ): Long? = withContext(Dispatchers.IO) {
        val record = db.lendingDao().getRecordById(recordId) ?: return@withContext null
        val catId = getOrCreateLendBorrowCategoryId(db.categoryDao())
        val isLent = record.type.equals("LENT", ignoreCase = true)
        val txType = if (isLent) "DEBIT" else "CREDIT"
        val amount = record.originalAmount ?: record.amount
        val refNo = "${REF_PREFIX_RECORD}${record.id}"

        val existing = (record.linkedTransactionId?.let { db.transactionDao().getTransactionById(it) })
            ?: db.transactionDao().findByRefNo(refNo)

        val txId = if (existing != null) {
            val updated = existing.copy(
                type = txType,
                amount = amount,
                sourceType = if (existing.sourceType.isNotBlank() && existing.sourceType != SOURCE_TYPE_LENDING) existing.sourceType else SOURCE_TYPE_LENDING,
                counterparty = record.personName,
                displayName = record.personName,
                refNo = refNo,
                timestamp = record.date,
                categoryId = catId,
                rawSms = if (existing.rawSms.isNotBlank() && existing.sourceType != SOURCE_TYPE_LENDING) existing.rawSms else "${if (isLent) "Lent to" else "Borrowed from"} ${record.personName}",
                isManualEntry = existing.isManualEntry,
                notes = record.notes,
                needsReview = false,
                wallet = wallet ?: existing.wallet
            )
            db.transactionDao().insert(updated)
            if (record.linkedTransactionId != updated.id) {
                db.lendingDao().update(record.copy(linkedTransactionId = updated.id))
            }
            updated.id
        } else {
            val newTx = TransactionEntity(
                type = txType,
                amount = amount,
                sourceType = SOURCE_TYPE_LENDING,
                counterparty = record.personName,
                displayName = record.personName,
                bank = null,
                refNo = refNo,
                timestamp = record.date,
                categoryId = catId,
                rawSms = "${if (isLent) "Lent to" else "Borrowed from"} ${record.personName}",
                isManualEntry = true,
                notes = record.notes,
                wallet = wallet ?: "NONE"
            )
            val insertedId = db.transactionDao().insert(newTx)
            db.lendingDao().update(record.copy(linkedTransactionId = insertedId))
            insertedId
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
        txId
    }

    suspend fun syncRepayment(
        repaymentId: Long,
        db: KanriDatabase,
        context: Context? = null
    ): Long? = withContext(Dispatchers.IO) {
        val repayment = db.lendingDao().getRepaymentById(repaymentId) ?: return@withContext null
        val record = db.lendingDao().getRecordById(repayment.lendingId) ?: return@withContext null
        val catId = getOrCreateLendBorrowCategoryId(db.categoryDao())
        val isLent = record.type.equals("LENT", ignoreCase = true)

        val repayTxType = if (isLent) "CREDIT" else "DEBIT"
        val refNo = "${REF_PREFIX_REPAY}${repayment.id}"
        val rawSms = if (isLent) "Repayment received from ${record.personName}" else "Repaid ${record.personName}"

        val linkedTx = record.linkedTransactionId?.let { db.transactionDao().getTransactionById(it) }
            ?: db.transactionDao().findByRefNo("${REF_PREFIX_RECORD}${record.id}")
        val inheritedWallet = linkedTx?.wallet ?: "NONE"

        val existing = db.transactionDao().findByRefNo(refNo)
        val txId = if (existing != null) {
            val updated = existing.copy(
                type = repayTxType,
                amount = repayment.amount,
                sourceType = SOURCE_TYPE_LENDING,
                counterparty = record.personName,
                displayName = record.personName,
                refNo = refNo,
                timestamp = repayment.paidAt,
                categoryId = catId,
                rawSms = rawSms,
                isManualEntry = true,
                notes = repayment.note,
                wallet = inheritedWallet
            )
            db.transactionDao().insert(updated)
            updated.id
        } else {
            val newTx = TransactionEntity(
                type = repayTxType,
                amount = repayment.amount,
                sourceType = SOURCE_TYPE_LENDING,
                counterparty = record.personName,
                displayName = record.personName,
                bank = null,
                refNo = refNo,
                timestamp = repayment.paidAt,
                categoryId = catId,
                rawSms = rawSms,
                isManualEntry = true,
                notes = repayment.note,
                wallet = inheritedWallet
            )
            db.transactionDao().insert(newTx)
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
        txId
    }

    suspend fun deleteLendingTransactions(
        recordId: Long,
        repayments: List<LendingRepaymentEntity>,
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val record = db.lendingDao().getRecordById(recordId)
        val linkedTx = record?.linkedTransactionId?.let { db.transactionDao().getTransactionById(it) }
            ?: db.transactionDao().findByRefNo("${REF_PREFIX_RECORD}$recordId")

        if (linkedTx != null) {
            if (linkedTx.sourceType != SOURCE_TYPE_LENDING) {
                // If it was an actual bank/SMS transaction, do not delete it from history.
                // Revert its categoryId and refNo so the expense record is preserved.
                db.transactionDao().insert(
                    linkedTx.copy(
                        categoryId = null,
                        refNo = null
                    )
                )
            } else {
                db.transactionDao().delete(linkedTx)
            }
        } else {
            db.transactionDao().deleteByRefNo("${REF_PREFIX_RECORD}$recordId")
        }

        for (repayment in repayments) {
            db.transactionDao().deleteByRefNo("${REF_PREFIX_REPAY}${repayment.id}")
        }
        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    suspend fun onTransactionCategoryChanged(
        transactionId: Long,
        newCategoryId: Long,
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val lendBorrowCatId = getOrCreateLendBorrowCategoryId(db.categoryDao())
        if (newCategoryId != lendBorrowCatId) {
            val record = db.lendingDao().getRecordByLinkedTransactionId(transactionId)
                ?: run {
                    val tx = db.transactionDao().getTransactionById(transactionId)
                    tx?.refNo?.let { ref ->
                        if (ref.startsWith(REF_PREFIX_RECORD)) {
                            ref.removePrefix(REF_PREFIX_RECORD).toLongOrNull()?.let { id ->
                                db.lendingDao().getRecordById(id)
                            }
                        } else if (ref.startsWith("LEND:")) {
                            ref.removePrefix("LEND:").toLongOrNull()?.let { id ->
                                db.lendingDao().getRecordById(id)
                            }
                        } else null
                    }
                }

            if (record != null) {
                val repayments = db.lendingDao().getRepaymentsForLending(record.id)
                if (repayments.isEmpty()) {
                    db.lendingDao().delete(record)
                } else {
                    db.lendingDao().update(record.copy(linkedTransactionId = null))
                }

                val tx = db.transactionDao().getTransactionById(transactionId)
                if (tx != null && (tx.refNo?.startsWith(REF_PREFIX_RECORD) == true || tx.refNo?.startsWith("LEND:") == true)) {
                    db.transactionDao().insert(tx.copy(refNo = null))
                }

                if (context != null) {
                    try {
                        KanriWidgetsUpdater.updateAllWidgets(context)
                    } catch (_: Throwable) {}
                }
            }
        }
    }

    suspend fun deleteRepaymentTransaction(
        repaymentId: Long,
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        db.transactionDao().deleteByRefNo("${REF_PREFIX_REPAY}$repaymentId")
        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    suspend fun updatePersonNameInTransactions(
        oldName: String,
        newName: String,
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        db.transactionDao().updateCounterpartyForLending(oldName.trim(), newName.trim())
        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Forgive / Write-Off debt:
     * Settles the lending record without inserting false cash repayment income.
     * Updates the linked original expense notes to indicate it was forgiven as bad debt.
     */
    suspend fun forgiveLendingRecord(
        lendingId: Long,
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val record = db.lendingDao().getRecordById(lendingId) ?: return@withContext
        val currentNotes = record.notes ?: ""
        val updatedNotes = if (currentNotes.contains("[Debt Forgiven]", ignoreCase = true)) {
            currentNotes
        } else {
            "$currentNotes [Debt Forgiven]".trim()
        }

        db.lendingDao().update(
            record.copy(
                isSettled = true,
                notes = updatedNotes
            )
        )

        // Update the original transaction notes to reflect debt forgiveness
        val refNo = "${REF_PREFIX_RECORD}$lendingId"
        val existingTx = (record.linkedTransactionId?.let { db.transactionDao().getTransactionById(it) })
            ?: db.transactionDao().findByRefNo(refNo)

        if (existingTx != null) {
            val txNotes = existingTx.notes ?: ""
            val newTxNotes = if (txNotes.contains("[Debt Forgiven]", ignoreCase = true)) {
                txNotes
            } else {
                "$txNotes [Debt Forgiven]".trim()
            }
            db.transactionDao().insert(
                existingTx.copy(
                    notes = newTxNotes,
                    rawSms = "${existingTx.rawSms ?: ""} (Forgiven)".trim()
                )
            )
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }

    suspend fun syncAllUnsynced(
        db: KanriDatabase,
        context: Context? = null
    ) = syncAllHistoricalRecords(db, context)

    suspend fun syncAllHistoricalRecords(
        db: KanriDatabase,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val allRecords = db.lendingDao().getAllRecordsWithRepaymentsSync()
        val catId = getOrCreateLendBorrowCategoryId(db.categoryDao())
        val existingLendingTx = db.transactionDao().getTransactionsByRefNoPattern("${REF_PREFIX_RECORD}%")
            .associateBy { it.refNo }
        val existingRepayTx = db.transactionDao().getTransactionsByRefNoPattern("${REF_PREFIX_REPAY}%")
            .associateBy { it.refNo }

        for (item in allRecords) {
            val record = item.lending
            val recordRefNo = "${REF_PREFIX_RECORD}${record.id}"
            val isLent = record.type.equals("LENT", ignoreCase = true)
            val recordTxType = if (isLent) "DEBIT" else "CREDIT"
            val recordAmount = record.originalAmount ?: record.amount

            val existingRecTx = existingLendingTx[recordRefNo]
                ?: (record.linkedTransactionId?.let { db.transactionDao().getTransactionById(it) })

            if (existingRecTx != null) {
                val updated = existingRecTx.copy(
                    type = recordTxType,
                    amount = recordAmount,
                    sourceType = SOURCE_TYPE_LENDING,
                    counterparty = record.personName,
                    displayName = record.personName,
                    refNo = recordRefNo,
                    timestamp = record.date,
                    categoryId = catId,
                    rawSms = "${if (isLent) "Lent to" else "Borrowed from"} ${record.personName}",
                    isManualEntry = true,
                    notes = record.notes
                )
                db.transactionDao().insert(updated)
                if (record.linkedTransactionId != updated.id) {
                    db.lendingDao().update(record.copy(linkedTransactionId = updated.id))
                }
            } else {
                val newTx = TransactionEntity(
                    type = recordTxType,
                    amount = recordAmount,
                    sourceType = SOURCE_TYPE_LENDING,
                    counterparty = record.personName,
                    displayName = record.personName,
                    bank = null,
                    refNo = recordRefNo,
                    timestamp = record.date,
                    categoryId = catId,
                    rawSms = "${if (isLent) "Lent to" else "Borrowed from"} ${record.personName}",
                    isManualEntry = true,
                    notes = record.notes
                )
                val newId = db.transactionDao().insert(newTx)
                db.lendingDao().update(record.copy(linkedTransactionId = newId))
            }

            for (repayment in item.repayments) {
                val repayRefNo = "${REF_PREFIX_REPAY}${repayment.id}"
                val repayTxType = if (isLent) "CREDIT" else "DEBIT"
                val rawSms = if (isLent) "Repayment received from ${record.personName}" else "Repaid ${record.personName}"
                val existingRepTx = existingRepayTx[repayRefNo]

                if (existingRepTx != null) {
                    val updated = existingRepTx.copy(
                        type = repayTxType,
                        amount = repayment.amount,
                        sourceType = SOURCE_TYPE_LENDING,
                        counterparty = record.personName,
                        displayName = record.personName,
                        refNo = repayRefNo,
                        timestamp = repayment.paidAt,
                        categoryId = catId,
                        rawSms = rawSms,
                        isManualEntry = true,
                        notes = repayment.note
                    )
                    db.transactionDao().insert(updated)
                } else {
                    val newTx = TransactionEntity(
                        type = repayTxType,
                        amount = repayment.amount,
                        sourceType = SOURCE_TYPE_LENDING,
                        counterparty = record.personName,
                        displayName = record.personName,
                        bank = null,
                        refNo = repayRefNo,
                        timestamp = repayment.paidAt,
                        categoryId = catId,
                        rawSms = rawSms,
                        isManualEntry = true,
                        notes = repayment.note
                    )
                    db.transactionDao().insert(newTx)
                }
            }
        }

        if (context != null) {
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (_: Throwable) {}
        }
    }
}
