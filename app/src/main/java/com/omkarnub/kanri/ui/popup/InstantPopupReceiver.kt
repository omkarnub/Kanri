package com.omkarnub.kanri.ui.popup

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InstantPopupReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CATEGORIZE = "com.omkarnub.kanri.ACTION_POPUP_CATEGORIZE"
        const val ACTION_DISMISS = "com.omkarnub.kanri.ACTION_POPUP_DISMISS"

        const val EXTRA_TX_ID = "extra_tx_id"
        const val EXTRA_CATEGORY_ID = "extra_category_id"
        const val EXTRA_COUNTERPARTY = "extra_counterparty"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        if (notificationId != -1) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(notificationId)
        }

        when (intent.action) {
            ACTION_CATEGORIZE -> {
                val secPrefs = com.omkarnub.kanri.data.security.SecurityPreferences.getInstance(context)
                if (secPrefs.isLockEnabled && com.omkarnub.kanri.data.security.SecurityManager.isAppLocked.value) {
                    Log.w("InstantPopupReceiver", "App lock is active. Direct notification categorization rejected.")
                    val launchIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(context).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(launchIntent)
                    return
                }

                val txId = intent.getLongExtra(EXTRA_TX_ID, -1L)
                val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
                val counterparty = intent.getStringExtra(EXTRA_COUNTERPARTY) ?: ""

                if (txId > 0L && categoryId > 0L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = KanriDatabase.getDatabase(context)
                            db.transactionDao().updateCategoryId(txId, categoryId)
                            if (counterparty.isNotBlank()) {
                                db.transactionDao().updateCategoryForCounterparty(counterparty, categoryId)
                                db.categoryDao().setMapping(
                                    CounterpartyCategoryMapEntity(
                                        counterparty = counterparty.trim().lowercase(java.util.Locale.ROOT),
                                        categoryId = categoryId
                                    )
                                )
                            }
                            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(context.applicationContext)
                            if (com.omkarnub.kanri.BuildConfig.DEBUG) {
                                Log.d("InstantPopupReceiver", "Assigned category $categoryId to tx $txId")
                            }
                        } catch (e: Exception) {
                            Log.e("InstantPopupReceiver", "Error assigning category from notification action", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_DISMISS -> {
                if (com.omkarnub.kanri.BuildConfig.DEBUG) {
                    Log.d("InstantPopupReceiver", "Notification dismissed by user")
                }
            }
        }
    }
}
