package com.omkarnub.kanri.ui.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.parser.DeduplicationResult
import com.omkarnub.kanri.data.parser.NotificationDeduplicationHelper
import com.omkarnub.kanri.data.parser.NotificationParser
import com.omkarnub.kanri.data.parser.TransactionType
import com.omkarnub.kanri.ui.popup.InstantPopupNotificationHelper
import com.omkarnub.kanri.ui.popup.InstantPopupService
import com.omkarnub.kanri.ui.popup.OverlayPermissionHelper
import com.omkarnub.kanri.util.DevCaptureLogger
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Supplementary NotificationListenerService scoped exclusively to incoming money and rewards
 * from payment apps (Google Pay, Amazon Pay, Paytm, FamPay, PhonePe, BHIM).
 *
 * Architecture & Core Principle:
 * - Bank SMS is the ONLY authoritative source for money SENT (UPI pay, ATM withdrawal, card swipes).
 * - NotificationListenerService is ONLY a supplementary source for money RECEIVED (credits and rewards),
 *   since payment apps never post notifications for "you just paid" (only banks send a debit SMS).
 * - Both SMS and Notification pipelines run permanently and simultaneously.
 * - Any outbound/debit alerts posted by payment apps are strictly ignored here to eliminate
 *   false positives and duplication with bank SMS.
 * - Incorporates connection lifecycle logging and rebind watchdog support.
 */
class KanriNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "KanriNotification"

        @Volatile
        var isConnected: Boolean = false
            private set

        /**
         * Re-requests system binding for NotificationListenerService.
         * Addresses known Android OS bug where NotificationListenerService dies silently
         * after process kills, app updates, or aggressive OEM background cleanup.
         */
        fun rebindService(context: Context) {
            val componentName = ComponentName(context, KanriNotificationListenerService::class.java)
            Log.i(TAG, "Requesting rebind for NotificationListenerService: $componentName")

            // Method 1: On API 24+, call requestRebind directly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(componentName)
                    Log.d(TAG, "requestRebind invoked successfully for $componentName")
                } catch (e: Exception) {
                    Log.e(TAG, "requestRebind failed, proceeding to component toggle fallback", e)
                }
            }

            // Method 2: Toggle component enabled state to force NotificationManagerService to re-evaluate
            try {
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.d(TAG, "Component enabled state toggled to force Android system rebind")
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling component state for rebind", e)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "KanriNotificationListenerService connected successfully to Android NotificationManager")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.w(TAG, "KanriNotificationListenerService disconnected by system! Requesting rebind...")
        rebindService(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // 1. Strictly scoped to supported receive-side payment apps (GPay, Amazon Pay, Paytm, FamPay, PhonePe, BHIM)
        if (!NotificationParser.isReceiveAppSupported(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        Log.d(TAG, "Intercepted payment notification from $packageName: Title='$title', Text='$text', BigText='$bigText'")

        // 2. Parse payment notification payload
        val parsed = NotificationParser.parse(
            packageName = packageName,
            title = title,
            text = bigText ?: text,
            subText = subText,
            timestamp = sbn.postTime
        )

        if (parsed == null) {
            Log.d(TAG, "Notification from $packageName is not a recognized payment. Skipping.")
            return
        }

        // 3. CORE PRINCIPLE ENFORCEMENT: Only process CREDIT (incoming money/rewards).
        // Debit events from payment apps are strictly dropped because bank SMS is the ONLY reliable authority for debits.
        if (parsed.type != TransactionType.CREDIT) {
            Log.d(TAG, "Ignoring non-credit notification (${parsed.type}) from $packageName. SMS is the sole authority for debits.")
            return
        }

        Log.i(
            TAG,
            "Parsed Incoming Notification: Type=${parsed.type}, Amount=${parsed.amount}, Counterparty=${parsed.counterparty}, Bank=${parsed.bank}, Ref=${parsed.refNo}"
        )

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
                        val counterparty = result.entity.counterparty ?: "Income"
                        Log.i(TAG, "Successfully recorded incoming payment from notification with ID: ${result.id}")

                        // Dev build toast & log
                        DevCaptureLogger.logCapture(
                            context = applicationContext,
                            source = "Notification",
                            amount = result.entity.amount,
                            type = result.entity.type,
                            counterparty = counterparty,
                            bank = result.entity.bank
                        )

                        // Trigger Truecaller-style Instant Popup or Notification Fallback
                        if (OverlayPermissionHelper.canDrawOverlays(applicationContext)) {
                            val popupIntent = Intent(applicationContext, InstantPopupService::class.java).apply {
                                putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, result.id)
                                putExtra(InstantPopupService.EXTRA_AMOUNT, result.entity.amount)
                                putExtra(InstantPopupService.EXTRA_IS_DEBIT, false)
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
                        KanriWidgetsUpdater.updateAllWidgets(applicationContext)
                    }
                    is DeduplicationResult.Enriched -> {
                        Log.i(TAG, "Notification enriched existing transaction ${result.id} with counterparty: ${result.updatedCounterparty}")
                        DevCaptureLogger.logDeduplicationMerge(
                            context = applicationContext,
                            source = "Notification",
                            amount = parsed.amount,
                            counterparty = result.updatedCounterparty,
                            existingId = result.id
                        )
                        KanriWidgetsUpdater.updateAllWidgets(applicationContext)
                    }
                    is DeduplicationResult.SkippedDuplicate -> {
                        Log.d(TAG, "Skipped duplicate transaction (already captured in tx ${result.existingId})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification transaction", e)
                CrashLogger.logHandledException(applicationContext, "NotificationListener.process", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        serviceScope.cancel()
    }
}
