package com.omkarnub.kanri.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.data.wallet.WalletBalances
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalletBalanceWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WalletBalanceWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val validIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WalletBalanceWidgetProvider::class.java)
            ).toSet()
            val targetIds = appWidgetIds.filter { it in validIds }.toIntArray()
            if (targetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = KanriDatabase.getDatabase(context)
                    val balanceEntities = db.walletDao().getBalancesSync()
                    val balances = if (balanceEntities.isEmpty()) {
                        WalletBalances(cash = 0.0, online = 0.0, total = 0.0, isConfigured = false)
                    } else {
                        val map = balanceEntities.associateBy { it.walletId.uppercase() }
                        val txRows = db.transactionDao().getWalletTransactionsSync()
                        WalletBalanceCalculator.balances(map, txRows)
                    }

                    val widgetStyle = WidgetPreferences.getInstance(context).widgetStyle
                    val bgRes = when (widgetStyle) {
                        WidgetStyle.IPHONE -> R.drawable.widget_bg_iphone
                        WidgetStyle.PIXEL_MATRIX -> R.drawable.widget_bg_pixel
                        WidgetStyle.GLASS_100X -> R.drawable.widget_bg_glass_100x
                    }

                    for (widgetId in targetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_wallet_balance)
                        views.setInt(R.id.widget_wallet_balance_root, "setBackgroundResource", bgRes)

                        // Check resize dimensions
                        val options = appWidgetManager.getAppWidgetOptions(widgetId)
                        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                        if (minHeight in 1..55) {
                            // Compact vertical resize: hide footer status to save space
                            views.setViewVisibility(R.id.widget_wallet_status, View.GONE)
                        } else {
                            views.setViewVisibility(R.id.widget_wallet_status, View.VISIBLE)
                        }

                        // Populate Balances
                        views.setTextViewText(R.id.widget_cash_amount, CurrencyUtils.formatCurrency(balances.cash))
                        views.setTextViewText(R.id.widget_online_amount, CurrencyUtils.formatCurrency(balances.online))
                        views.setTextViewText(R.id.widget_wallet_total, "Total: " + CurrencyUtils.formatCurrency(balances.total))

                        val statusText = if (!balances.isConfigured) {
                            "Setup opening balances in app"
                        } else {
                            "Tap to manage wallets & balances"
                        }
                        views.setTextViewText(R.id.widget_wallet_status, statusText)

                        val pendingIntent = KanriWidgetActions.createPendingIntent(
                            context,
                            KanriWidgetActions.ACTION_OPEN_WALLET,
                            widgetId
                        )
                        views.setOnClickPendingIntent(R.id.widget_wallet_balance_root, pendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (e: Exception) {
                    com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(
                        context, "WalletBalanceWidgetProvider.updateWidgets", e
                    )
                }
            }
        }
    }
}
