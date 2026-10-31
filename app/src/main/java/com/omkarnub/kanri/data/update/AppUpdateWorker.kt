package com.omkarnub.kanri.data.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AppUpdateWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = AppUpdatePreferences.getInstance(applicationContext)
        val updateManager = AppUpdateManager.getInstance(applicationContext)

        // Secretly check for new updates without disturbing user
        val result = updateManager.checkForUpdate()
        prefs.lastCheckTimestamp = System.currentTimeMillis()

        result.onSuccess { updateInfo ->
            if (updateInfo != null) {
                // An update is available
                prefs.pendingUpdate = updateInfo

                // Post notification if the user hasn't dismissed this specific version
                if (prefs.dismissedVersion != updateInfo.versionName) {
                    updateManager.postUpdateAvailableNotification(applicationContext, updateInfo)
                }
            } else {
                prefs.pendingUpdate = null
            }
        }.onFailure {
            // Secret check failed (e.g. offline or GitHub rate limit); fail quietly
            return Result.success()
        }

        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "kanri_daily_app_update_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // Run once every 24 hours secretly in the background
            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
