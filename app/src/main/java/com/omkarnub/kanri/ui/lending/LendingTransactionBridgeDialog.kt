package com.omkarnub.kanri.ui.lending

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper
import com.omkarnub.kanri.ui.common.LocalHazeState
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Bridge dialog connecting Transaction Categorization with the Lend & Borrow ledger.
 *
 * Auto-fills:
 * - Amount: from transaction amount (cleanly displayed, not forcing keyboard input)
 * - Date & Time: from transaction timestamp
 * - Type: "LENT" if Debit, "BORROWED" if Credit
 * - Counterparty/Person: checks against existing profiles in Lend & Borrow
 * - Passes all existing Lend & Borrow profiles for single-tap selection
 * - Supports editing if transaction is already linked to a lending record
 * - Synchronizes record insertion, category assignment, reference numbers, and widget updates.
 */
@Composable
fun LendingTransactionBridgeDialog(
    targetTransaction: TransactionWithCategory,
    onDismiss: () -> Unit,
    onRecordSaved: ((recordId: Long) -> Unit)? = null
) {
    val context = LocalContext.current
    val db = remember { KanriDatabase.getDatabase(context) }
    val coroutineScope = rememberCoroutineScope()

    val lendingRecords by db.lendingDao().getAllRecordsWithRepayments().collectAsState(initial = emptyList())

    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val allPeople = remember(lendingRecords, startOfToday) {
        LendingHubViewModel.aggregatePeople(lendingRecords, startOfToday)
    }

    val recentPeople = remember(allPeople) {
        allPeople.map { it.displayName }
    }

    var existingEntry by remember { mutableStateOf<LendingWithRepayments?>(null) }
    var isCheckingExisting by remember { mutableStateOf(true) }

    LaunchedEffect(targetTransaction.transaction.id) {
        val record = db.lendingDao().getRecordWithRepaymentsByLinkedTransactionId(targetTransaction.transaction.id)
            ?: targetTransaction.transaction.refNo?.let { ref ->
                if (ref.startsWith(LendingTransactionSyncHelper.REF_PREFIX_RECORD)) {
                    ref.removePrefix(LendingTransactionSyncHelper.REF_PREFIX_RECORD).toLongOrNull()?.let {
                        db.lendingDao().getRecordWithRepaymentsById(it)
                    }
                } else if (ref.startsWith("LEND:")) {
                    ref.removePrefix("LEND:").toLongOrNull()?.let {
                        db.lendingDao().getRecordWithRepaymentsById(it)
                    }
                } else null
            }
        existingEntry = record
        isCheckingExisting = false
    }

    if (!isCheckingExisting) {
        val isDebit = targetTransaction.transaction.type.equals("DEBIT", ignoreCase = true)
        val initialType = existingEntry?.lending?.type ?: if (isDebit) "LENT" else "BORROWED"
        val initialAmount = existingEntry?.lending?.amount ?: targetTransaction.transaction.amount
        val initialDate = existingEntry?.lending?.date ?: targetTransaction.transaction.timestamp
        val initialDueDate = existingEntry?.lending?.dueDate

        // Match existing person if available
        val matchedPerson = remember(allPeople, targetTransaction) {
            val cp = targetTransaction.transaction.counterparty?.trim() ?: ""
            val dn = targetTransaction.transaction.displayName?.trim() ?: ""
            allPeople.firstOrNull {
                it.displayName.equals(cp, ignoreCase = true) || it.displayName.equals(dn, ignoreCase = true)
            }?.displayName ?: allPeople.firstOrNull {
                cp.isNotBlank() && (it.displayName.contains(cp, ignoreCase = true) || cp.contains(it.displayName, ignoreCase = true))
            }?.displayName ?: ""
        }

        val initialPersonName = existingEntry?.lending?.personName ?: matchedPerson
        val initialNotes = existingEntry?.lending?.notes ?: (targetTransaction.transaction.notes ?: targetTransaction.transaction.counterparty)

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            CompositionLocalProvider(LocalHazeState provides null) {
                LendingEntryDialog(
                    initialType = initialType,
                    initialPersonName = initialPersonName,
                    initialAmount = initialAmount,
                    initialDate = initialDate,
                    initialDueDate = initialDueDate,
                    initialNotes = initialNotes,
                    existingEntry = existingEntry,
                    recentPeople = recentPeople,
                    allPeople = allPeople,
                    onDismiss = onDismiss,
                    onSave = { personName, amount, type, date, dueDate, notes ->
                        coroutineScope.launch {
                            val catId = LendingTransactionSyncHelper.getOrCreateLendBorrowCategoryId(db.categoryDao())
                            val recordId = if (existingEntry != null) {
                                val updatedRecord = existingEntry!!.lending.copy(
                                    personName = personName,
                                    amount = amount,
                                    type = type,
                                    date = date,
                                    dueDate = dueDate,
                                    notes = notes,
                                    linkedTransactionId = targetTransaction.transaction.id
                                )
                                db.lendingDao().update(updatedRecord)
                                existingEntry!!.lending.id
                            } else {
                                db.lendingDao().insert(
                                    LendingEntity(
                                        personName = personName,
                                        amount = amount,
                                        type = type,
                                        date = date,
                                        dueDate = dueDate,
                                        notes = notes,
                                        linkedTransactionId = targetTransaction.transaction.id
                                    )
                                )
                            }

                            // Update the linked transaction with correct financial type and reference
                            val txType = if (type.equals("LENT", ignoreCase = true)) "DEBIT" else "CREDIT"
                            val refNo = "${LendingTransactionSyncHelper.REF_PREFIX_RECORD}$recordId"
                            val updatedTx = targetTransaction.transaction.copy(
                                type = txType,
                                amount = amount,
                                categoryId = catId,
                                refNo = refNo,
                                counterparty = personName,
                                displayName = personName,
                                timestamp = date,
                                notes = notes,
                                needsReview = false
                            )
                            db.transactionDao().insert(updatedTx)

                            // Synchronize with helper for widgets and downstream logic
                            LendingTransactionSyncHelper.syncLendingRecord(recordId, db, context)

                            onRecordSaved?.invoke(recordId)
                            onDismiss()
                        }
                    }
                )
            }
        }
    }
}
