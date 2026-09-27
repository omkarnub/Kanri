package com.omkarnub.kanri.data.lending

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

class LendingReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "LendingReminderWorker"
        const val CHANNEL_ID = "kanri_lending_reminders"
        const val NOTIFICATION_ID = 8820
    }

    override suspend fun doWork(): Result {
        return try {
            val db = KanriDatabase.getDatabase(applicationContext)
            val openRecords = db.lendingDao().getAllRecordsWithRepaymentsSync()
                .filter { !it.lending.isSettled && it.lending.dueDate != null }

            if (openRecords.isEmpty()) {
                return Result.success()
            }

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfToday = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val startOfTomorrow = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val startOfDayAfterTomorrow = calendar.timeInMillis

            // Categorize dues
            val overdue = mutableListOf<String>()
            val dueToday = mutableListOf<String>()
            val dueSoon = mutableListOf<String>()

            for (entry in openRecords) {
                val due = entry.lending.dueDate ?: continue
                val amountStr = CurrencyUtils.formatCurrency(entry.outstanding)
                val name = entry.lending.personName
                val isLent = entry.lending.type.equals("LENT", ignoreCase = true)

                if (due < startOfToday) {
                    val daysOverdue = ((startOfToday - due) / 86_400_000L).coerceAtLeast(1)
                    val desc = if (isLent) {
                        "$name owes you $amountStr ($daysOverdue d overdue)"
                    } else {
                        "You owe $name $amountStr ($daysOverdue d overdue)"
                    }
                    overdue.add(desc)
                } else if (due < startOfTomorrow) {
                    val desc = if (isLent) {
                        "$name owes you $amountStr (Due today)"
                    } else {
                        "You owe $name $amountStr (Due today)"
                    }
                    dueToday.add(desc)
                } else if (due < startOfDayAfterTomorrow) {
                    val desc = if (isLent) {
                        "$name owes you $amountStr (Due tomorrow)"
                    } else {
                        "You owe $name $amountStr (Due tomorrow)"
                    }
                    dueSoon.add(desc)
                }
            }

            val totalAlerts = overdue.size + dueToday.size + dueSoon.size
            if (totalAlerts > 0) {
                fireNotification(overdue, dueToday, dueSoon)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun fireNotification(
        overdue: List<String>,
        dueToday: List<String>,
        dueSoon: List<String>
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lend & Borrow Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for upcoming and overdue loan and debt due dates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = (applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: Intent(applicationContext, MainActivityDarkDark::class.java)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "LEND_BORROW")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalCount = overdue.size + dueToday.size + dueSoon.size
        val title = when {
            overdue.isNotEmpty() -> "⚠️ Overdue Dues Alert (${overdue.size})"
            dueToday.isNotEmpty() -> "⏰ Dues Due Today (${dueToday.size})"
            else -> "📅 Upcoming Dues Reminder"
        }

        val lines = mutableListOf<String>()
        if (overdue.isNotEmpty()) lines.addAll(overdue)
        if (dueToday.isNotEmpty()) lines.addAll(dueToday)
        if (dueSoon.isNotEmpty()) lines.addAll(dueSoon)

        val previewText = lines.firstOrNull() ?: "$totalCount dues require your attention"

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .setSummaryText("$totalCount pending dues")
            .bigText(lines.joinToString("\n"))

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(previewText)
            .setStyle(bigTextStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
