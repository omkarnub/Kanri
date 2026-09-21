package com.omkarnub.kanri.data.crash

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLogger {

    private const val TAG = "CrashLogger"
    private const val CRASH_DIR_NAME = "crash_logs"
    private const val MAX_SAVED_LOGS = 10

    @Volatile
    private var isInstalled = false

    fun install(context: Context) {
        if (isInstalled) return
        isInstalled = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrashLog(context.applicationContext, throwable, thread, isFatal = true)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write uncaught crash log", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    fun logHandledException(context: Context, tag: String, throwable: Throwable) {
        try {
            writeCrashLog(context.applicationContext, throwable, Thread.currentThread(), isFatal = false, customTag = tag)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log handled exception", e)
        }
    }

    fun getCrashLogsDir(context: Context): File {
        val dir = File(context.filesDir, CRASH_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getCrashLogs(context: Context): List<File> {
        val dir = getCrashLogsDir(context)
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    fun clearAllLogs(context: Context): Boolean {
        val dir = getCrashLogsDir(context)
        var allDeleted = true
        dir.listFiles()?.forEach { file ->
            if (!file.delete()) {
                allDeleted = false
            }
        }
        return allDeleted
    }

    fun pruneOldLogs(context: Context, maxLogs: Int = MAX_SAVED_LOGS) {
        val logs = getCrashLogs(context)
        if (logs.size > maxLogs) {
            logs.drop(maxLogs).forEach { file ->
                file.delete()
            }
        }
    }

    fun writeCrashLog(
        context: Context,
        throwable: Throwable,
        thread: Thread?,
        isFatal: Boolean = true,
        customTag: String? = null
    ): File {
        val dir = getCrashLogsDir(context)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val prefix = if (isFatal) "fatal_crash" else "error"
        val logFile = File(dir, "${prefix}_$timeStamp.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTraceString = sw.toString()

        var appVersionName = "Unknown"
        var appVersionCode = 1L
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            appVersionName = pInfo.versionName ?: "Unknown"
            appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            appVersionName = "1.0"
            appVersionCode = 1L
        }

        val logContent = buildString {
            appendLine("=========================================")
            appendLine("KANRI CRASH / ERROR REPORT")
            appendLine("=========================================")
            appendLine("Type: ${if (isFatal) "UNCAUGHT CRASH (FATAL)" else "CAUGHT WARNING (NON-FATAL)"}")
            if (customTag != null) {
                appendLine("Tag: $customTag")
            }
            appendLine("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())}")
            appendLine("App Version: $appVersionName ($appVersionCode)")
            appendLine("Package: ${context.packageName}")
            appendLine("-----------------------------------------")
            appendLine("DEVICE INFORMATION")
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Product: ${Build.PRODUCT}")
            appendLine("Android OS Release: ${Build.VERSION.RELEASE}")
            appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
            appendLine("Fingerprint: ${Build.FINGERPRINT}")
            appendLine("-----------------------------------------")
            appendLine("THREAD INFORMATION")
            appendLine("Thread ID: ${thread?.id ?: -1}")
            appendLine("Thread Name: ${thread?.name ?: "Unknown"}")
            appendLine("-----------------------------------------")
            appendLine("EXCEPTION DETAILS")
            appendLine("Exception Class: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "No message provided"}")
            appendLine("Stack Trace:")
            appendLine(stackTraceString)
            appendLine("=========================================")
        }

        logFile.writeText(logContent, Charsets.UTF_8)
        pruneOldLogs(context, MAX_SAVED_LOGS)
        return logFile
    }
}
