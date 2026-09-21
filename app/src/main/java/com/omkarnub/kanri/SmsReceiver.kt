package com.omkarnub.kanri

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return

                val sender = messages[0].originatingAddress ?: ""
                val fullBody = messages.joinToString("") { it.messageBody ?: "" }

                Log.d("Kanri", "SMS received from $sender: $fullBody")

                val parsed = SmsParser.parse(fullBody, sender)
                if (parsed == null) {
                    Log.d("Kanri", "SMS not recognized as transaction or ignored (OTP/promotional).")
                    return
                }

                Log.d(
                    "Kanri",
                    "Parsed transaction: Type=${parsed.type}, Amount=${parsed.amount}, Source=${parsed.sourceType}, Counterparty=${parsed.counterparty}, Bank=${parsed.bank}, Ref=${parsed.refNo}"
                )

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = KanriDatabase.getDatabase(context)
                        val result = com.omkarnub.kanri.data.parser.NotificationDeduplicationHelper.processIncomingTransaction(
                            dao = db.transactionDao(),
                            categoryDao = db.categoryDao(),
                            parsed = parsed
                        )

                        when (result) {
                            is com.omkarnub.kanri.data.parser.DeduplicationResult.Inserted -> {
                                val rowId = result.id
                                val entity = result.entity
                                Log.d("Kanri", "Saved transaction to Room DB successfully with ID: $rowId")

                                // Milestone 25: Trigger Truecaller-style Instant Popup or Notification Fallback
                                try {
                                    val counterpartyName = entity.counterparty ?: entity.displayName ?: ""
                                    if (com.omkarnub.kanri.ui.popup.OverlayPermissionHelper.canDrawOverlays(context)) {
                                        val serviceIntent = Intent(context, com.omkarnub.kanri.ui.popup.InstantPopupService::class.java).apply {
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_TRANSACTION_ID, rowId)
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_AMOUNT, entity.amount)
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_IS_DEBIT, entity.type == "DEBIT")
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_COUNTERPARTY, counterpartyName)
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_BANK, entity.bank)
                                            putExtra(com.omkarnub.kanri.ui.popup.InstantPopupService.EXTRA_SOURCE_TYPE, entity.sourceType)
                                        }
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(serviceIntent)
                                        } else {
                                            context.startService(serviceIntent)
                                        }
                                    } else {
                                        com.omkarnub.kanri.ui.popup.InstantPopupNotificationHelper.showInstantTransactionNotification(
                                            context = context,
                                            txId = rowId,
                                            amount = entity.amount,
                                            isDebit = entity.type == "DEBIT",
                                            counterparty = counterpartyName,
                                            bank = entity.bank,
                                            sourceType = entity.sourceType
                                        )
                                    }
                                    com.omkarnub.kanri.widget.KanriAppWidgetProvider.updateAllWidgets(context)
                                } catch (popupEx: Exception) {
                                    Log.e("Kanri", "Error launching instant popup/notification", popupEx)
                                    com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(context, "SmsReceiver.popup", popupEx)
                                }
                            }
                            is com.omkarnub.kanri.data.parser.DeduplicationResult.Enriched -> {
                                Log.d("Kanri", "Enriched existing transaction ${result.id} with counterparty: ${result.updatedCounterparty}")
                            }
                            is com.omkarnub.kanri.data.parser.DeduplicationResult.SkippedDuplicate -> {
                                Log.d("Kanri", "Duplicate transaction detected (already captured in tx ${result.existingId}). Skipping.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Kanri", "Error saving transaction to database", e)
                        com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(context, "SmsReceiver.save", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e("Kanri", "Unhandled exception in SmsReceiver", t)
            com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(context, "SmsReceiver.onReceive", t)
        }
    }
}