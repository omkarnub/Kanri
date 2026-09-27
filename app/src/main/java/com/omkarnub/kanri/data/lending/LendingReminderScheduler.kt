package com.omkarnub.kanri.data.lending

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LendingReminderScheduler {

    const val UNIQUE_PERIODIC_WORK_NAME = "kanri_lending_due_reminder_periodic"
    const val UNIQUE_ONE_TIME_WORK_NAME = "kanri_lending_due_reminder_onetime"

    fun init(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Run daily (every 24 hours) check for due dates
        val periodicRequest = PeriodicWorkRequestBuilder<LendingReminderWorker>(24, TimeUnit.HOURS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun checkNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val oneTimeRequest = OneTimeWorkRequestBuilder<LendingReminderWorker>()
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
