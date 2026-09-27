package com.omkarnub.kanri.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.util.CurrencyUtils
import java.util.Calendar

/**
 * Daily reminder worker that fires once a day (scheduled around 9 PM).
 *
 * Checks:
 * 1. Uncategorized transactions → reminds user to categorize
 * 2. No transactions recorded today → nudges to add any missed expense/income
 * 3. Lending dues approaching / overdue → quick summary alert
 */
class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "DailyReminderWorker"
        const val CHANNEL_ID = "kanri_daily_reminders"
        const val NOTIFICATION_ID_UNCATEGORIZED = 9901
        const val NOTIFICATION_ID_NO_TRANSACTIONS = 9902
    }

    override suspend fun doWork(): Result {
        return try {
            val db = KanriDatabase.getDatabase(applicationContext)
            ensureChannel()

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = todayCal.timeInMillis
            todayCal.set(Calendar.HOUR_OF_DAY, 23)
            todayCal.set(Calendar.MINUTE, 59)
            todayCal.set(Calendar.SECOND, 59)
            todayCal.set(Calendar.MILLISECOND, 999)
            val todayEnd = todayCal.timeInMillis

            // 1. Count uncategorized (needs_review or no category) transactions
            val allTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(todayStart, todayEnd)
            val uncategorizedCount = allTx.count { it.transaction.categoryId == null }
            val needsReviewCount = allTx.count { it.transaction.needsReview }
            val totalToReview = maxOf(uncategorizedCount, needsReviewCount)

            // 2. Check if no transactions at all today
            val todayTxCount = allTx.size

            // 3. Overdue / due-soon lending
            val openLending = db.lendingDao().getAllRecordsWithRepaymentsSync()
                .filter { !it.lending.isSettled && it.lending.dueDate != null }
            val now = System.currentTimeMillis()
            val overdueCount = openLending.count { it.lending.dueDate!! < todayStart }
            val dueTodayCount = openLending.count {
                it.lending.dueDate!! in todayStart..todayEnd
            }

            // Fire notifications based on priority
            if (totalToReview > 0) {
                fireUncategorizedNotification(totalToReview)
            } else if (todayTxCount == 0) {
                fireNoTransactionsNotification()
            }

            // Lending dues are handled by LendingReminderWorker, so we skip here
            // to avoid duplicate notifications

            Result.success()
        } catch (e: Exception) {
            com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(
                applicationContext, "DailyReminderWorker.doWork", e
            )
            Result.retry()
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "End-of-day reminders to categorize transactions and track expenses"
            }
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun getLaunchPendingIntent(notificationId: Int): PendingIntent {
        val intent = (applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: Intent(applicationContext, MainActivityDarkDark::class.java)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun fireUncategorizedNotification(count: Int) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (count == 1) "1 transaction needs a category" else "$count transactions need categories"
        val body = "Take a moment to categorize today's transactions for better insights."

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(NOTIFICATION_ID_UNCATEGORIZED))
            .build()

        nm.notify(NOTIFICATION_ID_UNCATEGORIZED, notification)
    }

    private fun fireNoTransactionsNotification() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("No expenses tracked today")
            .setContentText("Did you forget to add a transaction? Tap to add one now.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setContentIntent(getLaunchPendingIntent(NOTIFICATION_ID_NO_TRANSACTIONS))
            .build()

        nm.notify(NOTIFICATION_ID_NO_TRANSACTIONS, notification)
    }
}
