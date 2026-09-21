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
        if (!NotificationParser.SUPPORTED_PACKAGES.contains(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        Log.d(TAG, "Intercepted notification from $packageName: Title='$title', Text='$text', BigText='$bigText'")

        val parsed = NotificationParser.parse(
            packageName = packageName,
            title = title,
            text = bigText ?: text,
            subText = subText,
            timestamp = sbn.postTime
        )

        if (parsed == null) {
            Log.d(TAG, "Notification from $packageName is not a received payment/reward. Skipping.")
            return
        }

        Log.d(TAG, "Parsed received payment: Amount=${parsed.amount}, Sender=${parsed.counterparty}, Bank=${parsed.bank}, Ref=${parsed.refNo}")

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
                        Log.d(TAG, "Successfully recorded received payment with ID: ${result.id}")

                        // Trigger Truecaller-style Instant Popup or Notification Fallback
                        val counterparty = result.entity.counterparty ?: "UPI Payment"
                        if (OverlayPermissionHelper.canDrawOverlays(applicationContext)) {
                            val popupIntent = Intent(applicationContext, InstantPopupService::class.java).apply {
                                putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, result.id)
                                putExtra(InstantPopupService.EXTRA_AMOUNT, result.entity.amount)
                                putExtra(InstantPopupService.EXTRA_IS_DEBIT, false) // Received credit
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
                                isDebit = false,
                                counterparty = counterparty,
                                bank = result.entity.bank,
                                sourceType = result.entity.sourceType
                            )
                        }
                        com.omkarnub.kanri.widget.KanriAppWidgetProvider.updateAllWidgets(applicationContext)
                    }
                    is DeduplicationResult.Enriched -> {
                        Log.d(TAG, "Enriched existing transaction ${result.id} with sender: ${result.updatedCounterparty}")
                    }
                    is DeduplicationResult.SkippedDuplicate -> {
                        Log.d(TAG, "Skipped duplicate received payment (already captured in tx ${result.existingId})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification payment", e)
                com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(applicationContext, "NotificationListener.process", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
