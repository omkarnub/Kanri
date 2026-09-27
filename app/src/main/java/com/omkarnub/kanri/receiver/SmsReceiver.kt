package com.omkarnub.kanri.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.parser.DeduplicationResult
import com.omkarnub.kanri.data.parser.NotificationDeduplicationHelper
import com.omkarnub.kanri.data.parser.SmsParser
import com.omkarnub.kanri.ui.popup.InstantPopupNotificationHelper
import com.omkarnub.kanri.ui.popup.InstantPopupService
import com.omkarnub.kanri.ui.popup.OverlayPermissionHelper
import com.omkarnub.kanri.util.DevCaptureLogger
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manifest-registered BroadcastReceiver for SMS alerts (Primary transaction capture pipeline).
 *
 * Reliability fixes & architecture:
 * 1. Manifest-registered with priority 999 and BROADCAST_SMS permission:
 *    Ensures this receiver wakes up and executes on-device even if Kanri's app process is closed
 *    or killed by Android memory reclamation.
 * 2. Asynchronous execution via goAsync() + CoroutineScope(Dispatchers.IO):
 *    Prevents Android Main thread ANRs while writing to encrypted Room DB / SQLCipher.
 * 3. Multi-part SMS PDU reassembly:
 *    Concatenates fragmented SMS parts so long bank debit alerts are parsed as a unified payload.
 * 4. PDU Carrier Timestamp extraction:
 *    Uses carrier timestamp from SMS PDU (or parsed date) to handle delayed-delivery SMS correctly.
 * 5. Primary source of truth:
 *    Bank SMS is the ONLY authoritative source for money SENT (UPI debits, ATM cash withdrawals, card swipes).
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                return
            }

            // Extract SMS messages from intent PDUs
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) {
                return
            }

            // 1. Reassemble multi-part SMS messages into a single complete string
            val sender = messages[0].originatingAddress ?: ""
            val fullBody = messages.joinToString("") { it.messageBody ?: "" }
            val carrierTimestamp = messages[0].timestampMillis.takeIf { it > 0 } ?: System.currentTimeMillis()

            Log.d(TAG, "SMS broadcast received from '$sender': $fullBody")

            // 2. High-precision regex parse for amount, type, counterparty, bank, account, refNo, timestamp
            val parsed = SmsParser.parse(
                body = fullBody,
                sender = sender,
                timestamp = carrierTimestamp
            )

            if (parsed == null) {
                Log.d(TAG, "SMS not recognized as financial transaction or ignored (OTP/promo/request).")
                return
            }

            Log.i(
                TAG,
                "Parsed SMS Transaction: Type=${parsed.type}, Amount=${parsed.amount}, Counterparty=${parsed.counterparty}, Bank=${parsed.bank}, Ref=${parsed.refNo}"
            )

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = KanriDatabase.getDatabase(context)
                    val result = NotificationDeduplicationHelper.processIncomingTransaction(
                        dao = db.transactionDao(),
                        categoryDao = db.categoryDao(),
                        parsed = parsed
                    )

                    when (result) {
                        is DeduplicationResult.Inserted -> {
                            val rowId = result.id
                            val entity = result.entity
                            val isDebit = entity.type.equals("DEBIT", ignoreCase = true)
                            val counterparty = entity.counterparty ?: entity.displayName ?: if (isDebit) "Expense" else "Income"

                            Log.i(TAG, "Saved SMS transaction to Room DB with ID: $rowId")

                            // Developer debug toast & log
                            DevCaptureLogger.logCapture(
                                context = context,
                                source = "SMS",
                                amount = entity.amount,
                                type = entity.type,
                                counterparty = counterparty,
                                bank = entity.bank
                            )

                            // Trigger Truecaller-style Instant Popup or Notification Fallback
                            try {
                                if (OverlayPermissionHelper.canDrawOverlays(context)) {
                                    val serviceIntent = Intent(context, InstantPopupService::class.java).apply {
                                        putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, rowId)
                                        putExtra(InstantPopupService.EXTRA_AMOUNT, entity.amount)
                                        putExtra(InstantPopupService.EXTRA_IS_DEBIT, isDebit)
                                        putExtra(InstantPopupService.EXTRA_COUNTERPARTY, counterparty)
                                        putExtra(InstantPopupService.EXTRA_BANK, entity.bank)
                                        putExtra(InstantPopupService.EXTRA_SOURCE_TYPE, entity.sourceType)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }
                                } else {
                                    InstantPopupNotificationHelper.showInstantTransactionNotification(
                                        context = context,
                                        txId = rowId,
                                        amount = entity.amount,
                                        isDebit = isDebit,
                                        counterparty = counterparty,
                                        bank = entity.bank,
                                        sourceType = entity.sourceType
                                    )
                                }
                                KanriWidgetsUpdater.updateAllWidgets(context)
                            } catch (popupEx: Exception) {
                                Log.e(TAG, "Error launching instant popup/notification", popupEx)
                                CrashLogger.logHandledException(context, "SmsReceiver.popup", popupEx)
                            }
                        }
                        is DeduplicationResult.Enriched -> {
                            Log.i(TAG, "SMS enriched existing transaction ${result.id} with counterparty: ${result.updatedCounterparty}")
                            DevCaptureLogger.logDeduplicationMerge(
                                context = context,
                                source = "SMS",
                                amount = parsed.amount,
                                counterparty = result.updatedCounterparty,
                                existingId = result.id
                            )
                            KanriWidgetsUpdater.updateAllWidgets(context)
                        }
                        is DeduplicationResult.SkippedDuplicate -> {
                            Log.d(TAG, "Duplicate transaction detected (already captured in tx ${result.existingId}). Skipping.")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving SMS transaction to database", e)
                    CrashLogger.logHandledException(context, "SmsReceiver.save", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Unhandled exception in SmsReceiver", t)
            CrashLogger.logHandledException(context, "SmsReceiver.onReceive", t)
        }
    }
}
