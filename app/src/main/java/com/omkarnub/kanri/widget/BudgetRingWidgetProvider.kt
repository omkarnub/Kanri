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

class BudgetRingWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, BudgetRingWidgetProvider::class.java)
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

                // Current month calculations
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
                val monthEnd = now.timeInMillis

                val monthTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(monthStart, monthEnd)
                val monthSpent = monthTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                    .sumOf { it.transaction.amount }

                val ringBitmap = WidgetCanvasRenderer.renderBudgetRing(
                    context = context,
                    spent = monthSpent,
                    budget = budgetLimit,
                    sizePx = 480
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_budget_ring)

                    // Check resize dimensions to adapt layout gracefully
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    if (minHeight in 1..85) {
                        // Compact vertical resize: hide bottom row so ring stays clear
                        views.setViewVisibility(R.id.widget_ring_bottom_row, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_ring_bottom_row, android.view.View.VISIBLE)
                    }

                    if (minHeight in 1..55) {
                        // Extremely small: hide header too
                        views.setViewVisibility(R.id.widget_budget_ring_header, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_budget_ring_header, android.view.View.VISIBLE)
                    }

                    views.setImageViewBitmap(R.id.widget_ring_chart_image, ringBitmap)
                    views.setTextViewText(
                        R.id.widget_ring_spent_text,
                        "Spent: ${CurrencyUtils.formatCompactCurrency(monthSpent)}"
                    )
                    views.setTextViewText(
                        R.id.widget_ring_budget_text,
                        "Budget: ${CurrencyUtils.formatCompactCurrency(budgetLimit)}"
                    )

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_HOME,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_budget_ring_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
