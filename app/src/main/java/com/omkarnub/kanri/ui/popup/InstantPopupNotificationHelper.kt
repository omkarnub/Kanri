package com.omkarnub.kanri.ui.popup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.omkarnub.kanri.MainActivity
import com.omkarnub.kanri.R
import com.omkarnub.kanri.ui.home.formatCurrency

object InstantPopupNotificationHelper {

    const val CHANNEL_ID = "kanri_instant_alerts"
    private const val CHANNEL_NAME = "Instant Transaction Alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows instant transaction categorization alerts and action chips"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showInstantTransactionNotification(
        context: Context,
        txId: Long,
        amount: Double,
        isDebit: Boolean,
        counterparty: String,
        bank: String?,
        sourceType: String,
        categoryOptions: List<Pair<Long, String>> = listOf(
            1L to "Food",
            2L to "Groceries",
            3L to "Shopping"
        )
    ) {
        createChannel(context)
        val notificationId = (txId % 100000).toInt().coerceAtLeast(5000)

        val openAppIntent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_TRANSACTION_ID", txId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isDebit) {
            "💸 ${formatCurrency(amount)} spent" + if (counterparty.isNotBlank()) " at $counterparty" else ""
        } else {
            "💰 ${formatCurrency(amount)} received" + if (counterparty.isNotBlank()) " from $counterparty" else ""
        }

        val details = buildString {
            if (!bank.isNullOrBlank()) append(bank).append(" • ")
            append(sourceType)
            append(" — Tap a category below:")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(details)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)

        // Add 1-tap category action chips
        for ((catId, catLabel) in categoryOptions.take(3)) {
            val actionIntent = Intent(context, InstantPopupReceiver::class.java).apply {
                action = InstantPopupReceiver.ACTION_CATEGORIZE
                putExtra(InstantPopupReceiver.EXTRA_TX_ID, txId)
                putExtra(InstantPopupReceiver.EXTRA_CATEGORY_ID, catId)
                putExtra(InstantPopupReceiver.EXTRA_COUNTERPARTY, counterparty)
                putExtra(InstantPopupReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val pendingAction = PendingIntent.getBroadcast(
                context,
                (notificationId * 10 + catId).toInt(),
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, catLabel, pendingAction)
        }

        // Dismiss action
        val dismissIntent = Intent(context, InstantPopupReceiver::class.java).apply {
            action = InstantPopupReceiver.ACTION_DISMISS
            putExtra(InstantPopupReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingDismiss = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 9,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, "Dismiss", pendingDismiss)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(notificationId, builder.build())
    }
}
