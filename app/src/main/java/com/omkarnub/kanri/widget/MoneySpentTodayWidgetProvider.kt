package com.omkarnub.kanri.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class MoneySpentTodayWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, MoneySpentTodayWidgetProvider::class.java)
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
            CoroutineScope(Dispatchers.IO).launch {
                val db = KanriDatabase.getDatabase(context)

                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val todayEnd = cal.timeInMillis

                val todayTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(todayStart, todayEnd)
                val debitTx = todayTx.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
                val todaySpent = debitTx.sumOf { it.transaction.amount }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_spent_today)

                    // Check resize dimensions
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    if (minHeight in 1..60) {
                        // Compact vertical resize: hide status text to keep amount legible
                        views.setViewVisibility(R.id.widget_today_status, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_today_status, android.view.View.VISIBLE)
                    }

                    views.setTextViewText(R.id.widget_today_amount, CurrencyUtils.formatCurrency(todaySpent))

                    val statusText = if (debitTx.isEmpty()) {
                        "No spends recorded today 🎉"
                    } else if (debitTx.size == 1) {
                        "1 expense recorded today"
                    } else {
                        "${debitTx.size} expenses recorded today"
                    }
                    views.setTextViewText(R.id.widget_today_status, statusText)

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_HOME,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_spent_today_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
