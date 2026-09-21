package com.omkarnub.kanri.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.omkarnub.kanri.data.db.KanriDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class BackupRepository(private val context: Context) {

    private val db = KanriDatabase.getDatabase(context)

    suspend fun createBackupPayload(): BackupPayload = withContext(Dispatchers.IO) {
        val txs = db.transactionDao().getAllTransactionsSync()
        val cats = db.categoryDao().getAllCategoriesSync()
        val budgets = db.budgetDao().getAllBudgetsSync()
        val lending = db.lendingDao().getAllRecordsSync()
        val repayments = db.lendingDao().getAllRepaymentsSync()
        val mappings = db.categoryDao().getAllMappings()

        BackupPayload(
            transactions = txs,
            categories = cats,
            budgets = budgets,
            lendingRecords = lending,
            counterpartyMappings = mappings,
            lendingRepayments = repayments
        )
    }

    suspend fun exportBackupToUri(uri: Uri, passphrase: String? = null): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val payload = createBackupPayload()
            val jsonString = BackupJsonParser.toJson(payload)
            val outputBytes = if (!passphrase.isNullOrBlank()) {
                BackupCryptoHelper.encryptJson(jsonString, passphrase)
            } else {
                jsonString.toByteArray(Charsets.UTF_8)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(outputBytes)
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("Could not open destination file"))

            val stats = BackupStats(
                transactionCount = payload.transactions.size,
                categoryCount = payload.categories.size,
                budgetCount = payload.budgets.size,
                lendingCount = payload.lendingRecords.size,
                mappingCount = payload.counterpartyMappings.size,
                repaymentCount = payload.lendingRepayments.size
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromUri(
        uri: Uri,
        passphrase: String? = null,
        clearExisting: Boolean = true
    ): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@withContext Result.failure(Exception("Could not open source file"))

            val jsonString = if (BackupCryptoHelper.isEncryptedBackup(bytes)) {
                if (passphrase.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("This backup file is encrypted. Please enter the backup passphrase."))
                }
                try {
                    BackupCryptoHelper.decryptJson(bytes, passphrase)
                } catch (e: Exception) {
                    return@withContext Result.failure(Exception("Incorrect backup password or corrupted backup file."))
                }
            } else {
                String(bytes, Charsets.UTF_8)
            }

            restoreBackupFromJson(jsonString, clearExisting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackupFromJson(jsonString: String, clearExisting: Boolean = true): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val payload = BackupJsonParser.fromJson(jsonString)

            db.withTransaction {
                if (clearExisting) {
                    db.transactionDao().deleteAllTransactions()
                    db.budgetDao().deleteAllBudgets()
                    db.lendingDao().deleteAllRepayments()
                    db.lendingDao().deleteAllRecords()
                    db.categoryDao().deleteAllMappings()
                    db.categoryDao().deleteAllCategories()
                }

                if (payload.categories.isNotEmpty()) {
                    db.categoryDao().insertCategories(payload.categories)
                }
                if (payload.transactions.isNotEmpty()) {
                    db.transactionDao().insertAll(payload.transactions)
                }
                if (payload.budgets.isNotEmpty()) {
                    db.budgetDao().insertAllBudgets(payload.budgets)
                }
                if (payload.lendingRecords.isNotEmpty()) {
                    db.lendingDao().insertAll(payload.lendingRecords)
                }
                if (payload.lendingRepayments.isNotEmpty()) {
                    db.lendingDao().insertAllRepayments(payload.lendingRepayments)
                }
                if (payload.counterpartyMappings.isNotEmpty()) {
                    db.categoryDao().insertMappings(payload.counterpartyMappings)
                }
            }

            val stats = BackupStats(
                transactionCount = payload.transactions.size,
                categoryCount = payload.categories.size,
                budgetCount = payload.budgets.size,
                lendingCount = payload.lendingRecords.size,
                mappingCount = payload.counterpartyMappings.size,
                repaymentCount = payload.lendingRepayments.size
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
