package com.omkarnub.kanri.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MidnightWidgetResetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        KanriWidgetsUpdater.updateAllWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "kanri_midnight_widget_reset"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)

            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5) // 5 seconds past midnight
                set(Calendar.MILLISECOND, 0)
            }

            val initialDelayMillis = (midnight.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)

            val request = PeriodicWorkRequestBuilder<MidnightWidgetResetWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
