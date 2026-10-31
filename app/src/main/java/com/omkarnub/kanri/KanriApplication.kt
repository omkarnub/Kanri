package com.omkarnub.kanri

import android.app.Application
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.notification.DailyReminderScheduler
import com.omkarnub.kanri.data.update.AppUpdateManager
import com.omkarnub.kanri.data.update.AppUpdateWorker

class KanriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        DailyReminderScheduler.init(this)
        com.omkarnub.kanri.util.CurrencyPreferences.getInstance(this)
        com.omkarnub.kanri.widget.MidnightWidgetResetWorker.schedule(this)
        com.omkarnub.kanri.ui.notification.NotificationListenerWatchdogWorker.schedule(this)
        AppUpdateManager.createNotificationChannel(this)
        AppUpdateWorker.schedule(this)
        com.omkarnub.kanri.util.AppShortcutManager.publishShortcuts(this)
    }
}
