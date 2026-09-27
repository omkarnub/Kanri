package com.omkarnub.kanri.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.lending.LendingReminderScheduler
import com.omkarnub.kanri.data.notification.DailyReminderScheduler
import com.omkarnub.kanri.ui.notification.KanriNotificationListenerService
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.ui.notification.NotificationListenerWatchdogWorker
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import com.omkarnub.kanri.widget.MidnightWidgetResetWorker

/**
 * Manifest-registered BroadcastReceiver for BOOT_COMPLETED.
 * Re-initializes background monitors, watchdog workers, and schedulers after device restart
 * so detection survives a phone reboot without requiring manual app launch.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "Boot broadcast received with action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            try {
                // 1. Reschedule periodic notification watchdog worker (15 min)
                NotificationListenerWatchdogWorker.schedule(context)

                // 2. Re-bind NotificationListenerService if permission is active
                if (NotificationAccessHelper.isNotificationAccessGranted(context)) {
                    KanriNotificationListenerService.rebindService(context)
                }

                // 3. Re-initialize background schedulers
                DailyReminderScheduler.init(context)
                MidnightWidgetResetWorker.schedule(context)
                LendingReminderScheduler.init(context)

                // 4. Refresh all glanceable home screen widgets
                KanriWidgetsUpdater.updateAllWidgets(context)

                Log.i(TAG, "Successfully re-initialized Kanri background monitors on boot.")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing components in BootReceiver", e)
                CrashLogger.logHandledException(context, "BootReceiver.onReceive", e)
            }
        }
    }
}
