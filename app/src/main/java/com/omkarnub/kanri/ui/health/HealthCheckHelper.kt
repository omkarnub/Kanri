package com.omkarnub.kanri.ui.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class HealthCheckItem(
    val id: String,
    val title: String,
    val description: String,
    val isHealthy: Boolean,
    val actionLabel: String,
    val isCritical: Boolean
)

data class HealthStatus(
    val items: List<HealthCheckItem>,
    val hasCriticalIssue: Boolean,
    val hasAnyIssue: Boolean,
    val warningSummary: String?
)

object HealthCheckHelper {

    fun checkHealth(context: Context): HealthStatus {
        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        val notificationListenerHealthy = enabledListeners.contains(context.packageName)

        val overlayHealthy = Settings.canDrawOverlays(context)

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryHealthy = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true

        val items = listOf(
            HealthCheckItem(
                id = "notification",
                title = "Payment & Bank Alert Listener",
                description = if (notificationListenerHealthy) "Actively detecting UPI and banking spend alerts on-device" else "Notification access paused. Transactions won't auto-log.",
                isHealthy = notificationListenerHealthy,
                actionLabel = "Grant Access",
                isCritical = true
            ),
            HealthCheckItem(
                id = "overlay",
                title = "Instant Floating Popup",
                description = if (overlayHealthy) "Truecaller-style quick tag overlay enabled" else "Overlay permission missing. Notification fallback will be used.",
                isHealthy = overlayHealthy,
                actionLabel = "Enable Popup",
                isCritical = false
            ),
            HealthCheckItem(
                id = "battery",
                title = "Background Battery Exemption",
                description = if (batteryHealthy) "Unrestricted background operation active" else "Battery optimization may kill background listeners when phone sleeps.",
                isHealthy = batteryHealthy,
                actionLabel = "Exempt Battery",
                isCritical = false
            )
        )

        val hasCritical = items.any { it.isCritical && !it.isHealthy }
        val hasAny = items.any { !it.isHealthy }

        val summary = when {
            !notificationListenerHealthy -> "Notification access paused: Transaction alerts from payment apps won't auto-log."
            !overlayHealthy -> "Instant floating overlay disabled: Tap to allow display over other apps."
            !batteryHealthy -> "Battery optimization active: Android may sleep background listeners."
            else -> null
        }

        return HealthStatus(
            items = items,
            hasCriticalIssue = hasCritical,
            hasAnyIssue = hasAny,
            warningSummary = summary
        )
    }

    fun openAction(context: Context, itemId: String) {
        when (itemId) {
            "notification" -> {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "overlay" -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "battery" -> {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
