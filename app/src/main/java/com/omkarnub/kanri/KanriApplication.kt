package com.omkarnub.kanri

import android.app.Application
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.export.ExportScheduler
import com.omkarnub.kanri.data.lending.LendingReminderScheduler
import com.omkarnub.kanri.data.notification.DailyReminderScheduler
import com.omkarnub.kanri.data.update.AppUpdateManager
import com.omkarnub.kanri.data.update.AppUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KanriApplication : Application() {

    // Bounded application-lifetime scope used to offload non-UI startup registration off main thread.
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        applicationScope.launch {
            DailyReminderScheduler.init(this@KanriApplication)
            ExportScheduler.init(this@KanriApplication)
            LendingReminderScheduler.init(this@KanriApplication)
            com.omkarnub.kanri.util.CurrencyPreferences.getInstance(this@KanriApplication)
            com.omkarnub.kanri.widget.MidnightWidgetResetWorker.schedule(this@KanriApplication)
            com.omkarnub.kanri.widget.providers.SkyWidgetProvider.schedulePeriodicRefresh(this@KanriApplication)
            com.omkarnub.kanri.ui.notification.NotificationListenerWatchdogWorker.schedule(this@KanriApplication)
            AppUpdateManager.createNotificationChannel(this@KanriApplication)
            AppUpdateWorker.schedule(this@KanriApplication)
            com.omkarnub.kanri.util.AppShortcutManager.publishShortcuts(this@KanriApplication)
        }
    }
}
