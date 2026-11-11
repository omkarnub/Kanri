package com.omkarnub.kanri.data.reset

import android.content.Context
import androidx.room.withTransaction
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.db.DatabaseKeyManager
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.security.SecurityPreferences
import com.omkarnub.kanri.ui.onboarding.OnboardingPreferences
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FactoryResetManager {

    suspend fun executeFactoryReset(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = KanriDatabase.getDatabase(context)

            // 1. Wipe all tables atomically and restore default categories
            db.withTransaction {
                db.transactionDao().deleteAllTransactions()
                db.budgetDao().deleteAllBudgets()
                db.lendingDao().deleteAllRepayments()
                db.lendingDao().deleteAllRecords()
                db.recurringPaymentDao().deleteAll()
                db.savingsGoalDao().deleteAllContributions()
                db.savingsGoalDao().deleteAll()
                db.walletDao().deleteAll()
                db.categoryDao().deleteAllMappings()
                db.categoryDao().deleteAllCategories()
                db.categoryDao().insertCategories(KanriDatabase.DEFAULT_CATEGORIES)
            }

            // 2. Clear security preferences (disable app lock)
            SecurityPreferences.getInstance(context).isLockEnabled = false

            // 3. Reset onboarding state
            OnboardingPreferences.getInstance(context).reset()

            // 4. Reset theme, profile, and wallet preferences
            ThemePreferences.getInstance(context).reset()
            com.omkarnub.kanri.data.profile.UserProfilePreferences.getInstance(context).reset()
            com.omkarnub.kanri.data.wallet.WalletPreferences.getInstance(context).reset()

            // 5. Delete all local crash logs
            CrashLogger.clearAllLogs(context)

            // 6. Cancel scheduled export tasks
            try {
                com.omkarnub.kanri.data.export.ExportScheduler.cancelScheduledExport(context)
            } catch (e: Exception) {
                // Ignore if scheduler was not initialized
            }

            // 7. Dismiss all active notifications
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.cancelAll()
            } catch (e: Exception) {
                // Ignore notification service errors
            }

            // 8. Delete generated statements and exports from internal and external storage
            try {
                java.io.File(context.filesDir, "auto_exports").deleteRecursively()
                context.getExternalFilesDir(null)?.let { externalDir ->
                    java.io.File(externalDir, "Kanri/AutoExports").deleteRecursively()
                    java.io.File(externalDir, "Kanri").deleteRecursively()
                }
                java.io.File(context.cacheDir, "exports").deleteRecursively()
            } catch (e: Exception) {
                // Ignore file cleanup errors
            }

            // 9. Delete database files and rotate encryption key in Keystore
            try {
                KanriDatabase.resetDatabase(context)
                DatabaseKeyManager.rotatePassphrase(context)
            } catch (e: Exception) {
                // Ignore in non-keystore environments
            }

            // 10. Invalidate widgets
            try {
                KanriWidgetsUpdater.updateAllWidgets(context)
            } catch (e: Exception) {
                // Ignore widget failures if not placed
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
