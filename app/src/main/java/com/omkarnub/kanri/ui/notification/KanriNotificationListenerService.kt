package com.omkarnub.kanri.ui.notification

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.parser.DeduplicationResult
import com.omkarnub.kanri.data.parser.NotificationDeduplicationHelper
import com.omkarnub.kanri.data.parser.NotificationParser
import com.omkarnub.kanri.data.parser.SmsParser
import com.omkarnub.kanri.data.parser.TransactionType
import com.omkarnub.kanri.ui.popup.InstantPopupNotificationHelper
import com.omkarnub.kanri.ui.popup.InstantPopupService
import com.omkarnub.kanri.ui.popup.OverlayPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class KanriNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "KanriNotification"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (!NotificationParser.isPackageSupported(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        Log.d(TAG, "Intercepted notification from $packageName: Title='$title', Text='$text', BigText='$bigText'")

        // 1. If it's an SMS app notification (e.g. Google Messages, Samsung Messages), parse via SmsParser
        // where title is typically the SMS sender (e.g. VK-HDFCBK) and body is the SMS text
        val parsed = if (NotificationParser.isSmsApp(packageName)) {
            SmsParser.parse(
                body = bigText ?: text ?: "",
                sender = title,
                timestamp = sbn.postTime
            )
        } else {
            // 2. Otherwise parse as a payment or banking app notification, with SmsParser fallback
            NotificationParser.parse(
                packageName = packageName,
                title = title,
                text = bigText ?: text,
                subText = subText,
                timestamp = sbn.postTime
            ) ?: SmsParser.parse(
                body = bigText ?: text ?: "",
                sender = title ?: NotificationParser.getAppNameForPackage(packageName),
                timestamp = sbn.postTime
            )
        }

        if (parsed == null) {
            Log.d(TAG, "Notification from $packageName is not a recognized transaction. Skipping.")
            return
        }

        Log.d(TAG, "Parsed transaction: Type=${parsed.type}, Amount=${parsed.amount}, Counterparty=${parsed.counterparty}, Bank=${parsed.bank}, Ref=${parsed.refNo}")

        serviceScope.launch {
            try {
                val db = KanriDatabase.getDatabase(applicationContext)
                val result = NotificationDeduplicationHelper.processIncomingTransaction(
                    dao = db.transactionDao(),
                    categoryDao = db.categoryDao(),
                    parsed = parsed
                )

                when (result) {
                    is DeduplicationResult.Inserted -> {
                        val isDebit = result.entity.type.equals("DEBIT", ignoreCase = true)
                        val counterparty = result.entity.counterparty ?: if (isDebit) "Expense" else "Income"
                        Log.d(TAG, "Successfully recorded transaction with ID: ${result.id} (isDebit=$isDebit)")

                        // Trigger Truecaller-style Instant Popup or Notification Fallback
                        if (OverlayPermissionHelper.canDrawOverlays(applicationContext)) {
                            val popupIntent = Intent(applicationContext, InstantPopupService::class.java).apply {
                                putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, result.id)
                                putExtra(InstantPopupService.EXTRA_AMOUNT, result.entity.amount)
                                putExtra(InstantPopupService.EXTRA_IS_DEBIT, isDebit)
                                putExtra(InstantPopupService.EXTRA_COUNTERPARTY, counterparty)
                                putExtra(InstantPopupService.EXTRA_BANK, result.entity.bank)
                                putExtra(InstantPopupService.EXTRA_SOURCE_TYPE, result.entity.sourceType)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                applicationContext.startForegroundService(popupIntent)
                            } else {
                                applicationContext.startService(popupIntent)
                            }
                        } else {
                            InstantPopupNotificationHelper.showInstantTransactionNotification(
                                context = applicationContext,
                                txId = result.id,
                                amount = result.entity.amount,
                                isDebit = isDebit,
                                counterparty = counterparty,
                                bank = result.entity.bank,
                                sourceType = result.entity.sourceType
                            )
                        }
                        com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(applicationContext)
                    }
                    is DeduplicationResult.Enriched -> {
                        Log.d(TAG, "Enriched existing transaction ${result.id} with counterparty: ${result.updatedCounterparty}")
                    }
                    is DeduplicationResult.SkippedDuplicate -> {
                        Log.d(TAG, "Skipped duplicate transaction (already captured in tx ${result.existingId})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification transaction", e)
                com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(applicationContext, "NotificationListener.process", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
