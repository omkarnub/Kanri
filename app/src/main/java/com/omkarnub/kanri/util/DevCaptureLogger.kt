package com.omkarnub.kanri.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.util.Locale

/**
 * Diagnostic logger and Toast utility for transaction capture verification in developer builds.
 * Displays toast alerts indicating whether a payment was caught by SMS, notification, or merged via dedup.
 */
object DevCaptureLogger {
    private const val TAG = "KanriCapture"

    private fun isDevBuild(context: Context): Boolean {
        return try {
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }

    fun logCapture(
        context: Context,
        source: String, // "SMS" or "Notification"
        amount: Double,
        type: String, // "DEBIT" or "CREDIT"
        counterparty: String?,
        bank: String?
    ) {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val details = listOfNotNull(
            counterparty?.takeIf { it.isNotBlank() },
            bank?.takeIf { it.isNotBlank() }
        ).joinToString(" | ")
        val msg = "Kanri [$source]: ₹$formattedAmount $type ($details)"

        Log.i(TAG, msg)

        if (isDevBuild(context)) {
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to display dev toast", t)
                }
            }
        }
    }

    fun logDeduplicationMerge(
        context: Context,
        source: String, // "SMS" or "Notification"
        amount: Double,
        counterparty: String?,
        existingId: Long
    ) {
        val formattedAmount = String.format(Locale.US, "%.2f", amount)
        val msg = "Kanri [Dedup Merged]: ₹$formattedAmount from $source merged into Tx #$existingId ($counterparty)"

        Log.i(TAG, msg)

        if (isDevBuild(context)) {
            Handler(Looper.getMainLooper()).post {
                try {
                    Toast.makeText(context.applicationContext, msg, Toast.LENGTH_LONG).show()
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed to display dedup toast", t)
                }
            }
        }
    }
}
