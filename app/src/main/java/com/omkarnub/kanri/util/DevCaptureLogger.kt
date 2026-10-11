package com.omkarnub.kanri.util

import android.content.Context
import android.util.Log
import java.util.Locale

/**
 * Diagnostic logger for transaction capture verification in developer builds.
 * Logs capture events to Logcat without displaying intrusive system toasts.
 */
object DevCaptureLogger {
    private const val TAG = "KanriCapture"

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
    }
}
