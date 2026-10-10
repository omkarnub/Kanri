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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetProgressWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, BudgetProgressWidgetProvider::class.java)
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

                val now = Calendar.getInstance()
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(now.time)
                val budget = db.budgetDao().getBudgetForMonthSync(monthKey)
                val budgetLimit = budget?.monthlyLimit ?: 50000.0

                val monthCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val monthStart = monthCal.timeInMillis
                val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = now.get(Calendar.DAY_OF_MONTH)
                val daysLeft = (maxDay - currentDay + 1).coerceAtLeast(1)

                val monthTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(monthStart, now.timeInMillis)
                val monthSpent = monthTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                    .sumOf { it.transaction.amount }

                val remainingBudget = (budgetLimit - monthSpent).coerceAtLeast(0.0)
                val safeToSpendPerDay = remainingBudget / daysLeft
                val percentUsed = if (budgetLimit > 0) {
                    ((monthSpent / budgetLimit) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                val isOver = monthSpent > budgetLimit

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_budget_progress)

                    // Check resize dimensions
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    if (minHeight in 1..65) {
                        // Compact vertical resize: hide substatus to keep progress bar clear
                        views.setViewVisibility(R.id.widget_progress_substatus, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_progress_substatus, android.view.View.VISIBLE)
                    }

                    views.setTextViewText(R.id.widget_progress_spent, CurrencyUtils.formatCurrency(monthSpent))
                    views.setTextViewText(
                        R.id.widget_progress_budget_total,
                        "of ${CurrencyUtils.formatCompactCurrency(budgetLimit)}"
                    )
                    views.setTextViewText(
                        R.id.widget_progress_percent,
                        if (isOver) "Over budget" else "$percentUsed% used"
                    )
                    views.setTextColor(
                        R.id.widget_progress_percent,
                        0xFFFFFFFF.toInt()
                    )

                    views.setProgressBar(
                        R.id.widget_progress_bar_horizontal,
                        100,
                        percentUsed,
                        false
                    )

                    val subStatus = if (isOver) {
                        "Over by ${CurrencyUtils.formatCompactCurrency(monthSpent - budgetLimit)} • $daysLeft days left"
                    } else {
                        "${CurrencyUtils.formatCompactCurrency(remainingBudget)} left • Safe: ${CurrencyUtils.formatCompactCurrency(safeToSpendPerDay)}/d"
                    }
                    views.setTextViewText(R.id.widget_progress_substatus, subStatus)

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_HOME,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_budget_progress_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
