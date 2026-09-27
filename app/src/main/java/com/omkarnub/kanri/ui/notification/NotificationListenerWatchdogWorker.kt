package com.omkarnub.kanri.ui.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager watchdog (~15 minute interval) that verifies whether
 * KanriNotificationListenerService is still actively bound by Android's NotificationManagerService.
 *
 * Known Android Issue:
 * In Android 8.0+ (and especially on aggressive OEM battery savers like MIUI, ColorOS, FunTouch),
 * NotificationListenerService can silently disconnect / unbind without calling onDestroy(),
 * leaving the app deaf to incoming notifications until a manual toggle or system reboot.
 *
 * Fix:
 * The watchdog checks `KanriNotificationListenerService.isConnected`. If the permission is granted
 * but the service is disconnected, it immediately triggers `KanriNotificationListenerService.rebindService()`.
 */
class NotificationListenerWatchdogWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "NotificationWatchdog"
        const val WORK_NAME = "KanriNotificationListenerWatchdog"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationListenerWatchdogWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(
                Constraints.Builder().build()
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "NotificationListenerWatchdogWorker scheduled with 15-minute interval")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing notification watchdog liveness check...")

        val isPermissionGranted = NotificationAccessHelper.isNotificationAccessGranted(context)
        if (!isPermissionGranted) {
            Log.d(TAG, "Notification listener permission is not granted. Watchdog idle.")
            return Result.success()
        }

        val isBound = KanriNotificationListenerService.isConnected
        Log.d(TAG, "Watchdog status check: permissionGranted=true, isListenerBound=$isBound")

        if (!isBound) {
            Log.w(TAG, "NotificationListenerService was silently disconnected by Android! Re-requesting binding now...")
            KanriNotificationListenerService.rebindService(context)
        } else {
            Log.d(TAG, "NotificationListenerService is healthy and active.")
        }

        return Result.success()
    }
}
