package com.omkarnub.kanri.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

object BirthdayHelper {

    private const val TAG = "BirthdayHelper"
    const val CHANNEL_ID = "kanri_daily_reminders"
    const val NOTIFICATION_ID_BIRTHDAY = 9903

    private val DATE_PATTERNS = listOf(
        "dd MMM yyyy",
        "d MMM yyyy",
        "dd MMMM yyyy",
        "d MMMM yyyy",
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "d-M-yyyy",
        "MMMM d, yyyy",
        "MMM d, yyyy"
    )

    /**
     * Checks if the given date of birth string corresponds to today's day & month.
     */
    fun isBirthdayToday(dobString: String?, today: LocalDate = LocalDate.now()): Boolean {
        if (dobString.isNullOrBlank()) return false
        val clean = dobString.trim()

        for (pattern in DATE_PATTERNS) {
            try {
                val formatter = DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern(pattern)
                    .toFormatter(Locale.ENGLISH)
                val parsed = LocalDate.parse(clean, formatter)
                if (parsed.monthValue == today.monthValue && parsed.dayOfMonth == today.dayOfMonth) {
                    return true
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }
        return false
    }

    /**
     * Checks whether today is the user's birthday, and if so, sends a celebration notification
     * (at most once per year).
     */
    fun checkAndTriggerBirthdayNotification(context: Context) {
        try {
            val userPrefs = UserProfilePreferences.getInstance(context)
            val dob = userPrefs.userDob
            if (dob.isBlank()) return

            val today = LocalDate.now()
            if (!isBirthdayToday(dob, today)) return

            if (userPrefs.lastBirthdayNotifiedYear == today.year) {
                // Already celebrated today this year
                return
            }

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                Log.d(TAG, "Notifications are disabled; skipping birthday notification")
                return
            }

            ensureChannel(context)

            val rawName = userPrefs.userName.trim()
            val firstName = rawName.split("\\s+".toRegex()).firstOrNull()?.trim() ?: "there"

            val launchIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(context).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_BIRTHDAY,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "Happy Birthday, $firstName!"
            val text = "Wishing you joy, good health, and prosperity in the year ahead! Have a fantastic day."
            val bigText = "May your year ahead be filled with happiness, peace, and abundance. Take a moment to celebrate yourself today."

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID_BIRTHDAY, notification)

            userPrefs.lastBirthdayNotifiedYear = today.year
            Log.i(TAG, "Successfully triggered birthday notification for $firstName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send birthday notification", e)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders & Celebrations",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders, goals, and birthday celebrations"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
