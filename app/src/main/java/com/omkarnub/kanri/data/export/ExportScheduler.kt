package com.omkarnub.kanri.data.export

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ExportScheduler {

    private const val PREFS_NAME = "kanri_export_scheduler_prefs"
    private const val KEY_SCHEDULE = "scheduled_export_frequency"
    const val UNIQUE_WORK_NAME = "kanri_periodic_statement_export"

    enum class ExportSchedule(val label: String) {
        OFF("Off"),
        MONTHLY("Monthly (1st of month)"),
        WEEKLY("Weekly")
    }

    private val _scheduleFlow = MutableStateFlow(ExportSchedule.OFF)
    val scheduleFlow: StateFlow<ExportSchedule> = _scheduleFlow.asStateFlow()

    fun init(context: Context) {
        _scheduleFlow.value = getSavedSchedule(context)
    }

    fun getSavedSchedule(context: Context): ExportSchedule {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SCHEDULE, ExportSchedule.OFF.name) ?: ExportSchedule.OFF.name
        return try {
            ExportSchedule.valueOf(name)
        } catch (e: Exception) {
            ExportSchedule.OFF
        }
    }

    fun setSchedule(context: Context, schedule: ExportSchedule) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SCHEDULE, schedule.name).apply()
        _scheduleFlow.value = schedule

        val workManager = WorkManager.getInstance(context)

        when (schedule) {
            ExportSchedule.OFF -> {
                workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            }
            ExportSchedule.WEEKLY -> {
                val workRequest = PeriodicWorkRequestBuilder<ScheduledExportWorker>(7, TimeUnit.DAYS)
                    .setInputData(workDataOf(ScheduledExportWorker.KEY_SCHEDULE_TYPE to ExportSchedule.WEEKLY.name))
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            }
            ExportSchedule.MONTHLY -> {
                val now = Calendar.getInstance()
                val targetFirst = Calendar.getInstance().apply {
                    add(Calendar.MONTH, 1)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 8)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val initialDelayMs = (targetFirst.timeInMillis - now.timeInMillis).coerceAtLeast(0)

                val workRequest = PeriodicWorkRequestBuilder<ScheduledExportWorker>(30, TimeUnit.DAYS)
                    .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                    .setInputData(workDataOf(ScheduledExportWorker.KEY_SCHEDULE_TYPE to ExportSchedule.MONTHLY.name))
                    .build()

                workManager.enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
            }
        }
    }
}
