package com.omkarnub.kanri.data.cloud

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class CloudBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "CloudBackupWorker"
        const val UNIQUE_WORK_NAME = "kanri_periodic_cloud_backup"
        private const val DEFAULT_REPEAT_INTERVAL_HOURS = 24L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<CloudBackupWorker>(
                DEFAULT_REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
            Log.d(TAG, "Scheduled periodic cloud backup (every $DEFAULT_REPEAT_INTERVAL_HOURS hours)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            Log.d(TAG, "Cancelled periodic cloud backup")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic CloudBackupWorker execution...")
        val prefs = CloudBackupPreferences.getInstance(applicationContext)

        if (prefs.backupMode != BackupMode.CLOUD) {
            Log.d(TAG, "App is in Fully Offline mode. Skipping cloud backup.")
            return Result.success()
        }

        if (prefs.accountEmail.isNullOrBlank()) {
            Log.d(TAG, "No Google account signed in. Skipping cloud backup.")
            return Result.success()
        }

        if (!prefs.isAutoBackupEnabled) {
            Log.d(TAG, "Auto cloud backup is disabled by user.")
            return Result.success()
        }

        val cloudManager = CloudBackupManager.getInstance(applicationContext)
        val result = cloudManager.performCloudBackup()

        return if (result.isSuccess) {
            Log.d(TAG, "Periodic cloud backup completed successfully")
            Result.success()
        } else {
            val error = result.exceptionOrNull()
            Log.e(TAG, "Periodic cloud backup failed: ${error?.message}", error)
            if (error is DriveApiException.NetworkError) {
                Result.retry()
            } else {
                // Non-retriable auth or quota error - preferences already updated with diagnostics
                Result.success()
            }
        }
    }
}
