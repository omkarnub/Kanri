package com.omkarnub.kanri.data.export

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omkarnub.kanri.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduledExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "ScheduledExportWorker"
        const val CHANNEL_ID = "kanri_scheduled_exports"
        const val NOTIFICATION_ID = 2026
        const val KEY_SCHEDULE_TYPE = "key_schedule_type"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting scheduled export execution...")
            val repo = StatementExportRepository(applicationContext)

            val scheduleType = inputData.getString(KEY_SCHEDULE_TYPE)
                ?: ExportScheduler.getSavedSchedule(applicationContext).name

            val range: ExportRange = if (scheduleType == ExportScheduler.ExportSchedule.WEEKLY.name) {
                ExportRange.CurrentMonth
            } else {
                ExportRange.LastMonth
            }

            // Fixed app-managed directory
            val exportDir = File(applicationContext.getExternalFilesDir(null), "Kanri/AutoExports").apply {
                mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val pdfFile = File(exportDir, "Kanri_Statement_${range.label.replace(" ", "_")}_$timeStamp.pdf")
            val csvFile = File(exportDir, "Kanri_Statement_${range.label.replace(" ", "_")}_$timeStamp.csv")

            val pdfResult = repo.exportStatementToFile(pdfFile, ExportFormat.PDF, range)
            val csvResult = repo.exportStatementToFile(csvFile, ExportFormat.CSV, range)

            if (pdfResult.isFailure && csvResult.isFailure) {
                Log.e(TAG, "Failed to generate exports: PDF=${pdfResult.exceptionOrNull()}, CSV=${csvResult.exceptionOrNull()}")
                return Result.retry()
            }

            // Also copy to public download directory if accessible
            try {
                val publicDownloads = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kanri/AutoExports")
                if (publicDownloads.exists() || publicDownloads.mkdirs()) {
                    copyFile(pdfFile, File(publicDownloads, pdfFile.name))
                    copyFile(csvFile, File(publicDownloads, csvFile.name))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not copy exports to public Downloads: ${e.message}")
            }

            fireCompletionNotification(pdfFile, range.label)
            Log.d(TAG, "Scheduled export completed successfully: ${pdfFile.absolutePath}")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during scheduled export", e)
            Result.retry()
        }
    }

    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inStream ->
            FileOutputStream(dst).use { outStream ->
                inStream.copyTo(outStream)
            }
        }
    }

    private fun fireCompletionNotification(exportedFile: File, periodLabel: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kanri Scheduled Exports",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for automatic scheduled PDF & CSV statement exports"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fileUri = try {
            FileProvider.getUriForFile(
                applicationContext,
                "${applicationContext.packageName}.fileprovider",
                exportedFile
            )
        } catch (e: Exception) {
            null
        }

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = if (fileUri != null) {
            PendingIntent.getActivity(
                applicationContext,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Kanri Statement Ready ($periodLabel)")
            .setContentText("Your periodic PDF & CSV financial statement has been generated.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Your financial statement for $periodLabel is ready and saved to AutoExports.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                    addAction(android.R.drawable.ic_menu_view, "Open Statement", pendingIntent)
                }
            }
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
