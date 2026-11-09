package com.omkarnub.kanri.widget.providers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.SkyWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource
import java.util.concurrent.TimeUnit

class SkyWidgetProvider : BasePixelWidgetProvider<SkyWidgetState>() {

    override suspend fun loadState(context: Context): SkyWidgetState {
        return WidgetDataSource.getSkyState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_HOME

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, SkyWidgetProvider::class.java, playBurst)
        }

        fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<SkyPeriodicRefreshWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "kanri_sky_widget_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

class SkyPeriodicRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        SkyWidgetProvider.updateAll(applicationContext, playBurst = false)
        return Result.success()
    }
}

