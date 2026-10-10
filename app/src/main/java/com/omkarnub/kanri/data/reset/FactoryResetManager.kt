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
                db.lendingDao().deleteAllRecords()
                db.recurringPaymentDao().deleteAll()
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

            // 6. Delete database files and rotate encryption key in Keystore
            try {
                KanriDatabase.resetDatabase(context)
                DatabaseKeyManager.rotatePassphrase(context)
            } catch (e: Exception) {
                // Ignore in non-keystore environments
            }

            // 7. Invalidate widgets
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
