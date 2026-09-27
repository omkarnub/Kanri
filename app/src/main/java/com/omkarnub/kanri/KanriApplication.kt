package com.omkarnub.kanri

import android.app.Application
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.notification.DailyReminderScheduler

class KanriApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
        DailyReminderScheduler.init(this)
        com.omkarnub.kanri.widget.MidnightWidgetResetWorker.schedule(this)
        com.omkarnub.kanri.ui.notification.NotificationListenerWatchdogWorker.schedule(this)
    }
}
