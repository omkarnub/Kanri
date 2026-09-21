package com.omkarnub.kanri.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.omkarnub.kanri.MainActivity
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.KanriDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class KanriAppWidgetProvider : AppWidgetProvider() {

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
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, KanriAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            }
        }

        fun resolveLayoutForDimensions(minWidth: Int, minHeight: Int): Int {
            return when {
                minHeight in 1..99 || (minWidth in 1..149 && minHeight < 150) -> R.layout.widget_small
                minHeight >= 180 -> R.layout.widget_large
                else -> R.layout.widget_medium
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = KanriDatabase.getDatabase(context)

                // Today's range
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
                val todaySpent = todayTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
                    .sumOf { it.transaction.amount }

                // Month's range and budget
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

                val monthTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(monthStart, todayEnd)
                val monthSpent = monthTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
                    .sumOf { it.transaction.amount }

                val remainingBudget = (budgetLimit - monthSpent).coerceAtLeast(0.0)
                val safeToSpendPerDay = remainingBudget / daysLeft
                val percentRemaining = if (budgetLimit > 0) {
                    ((remainingBudget / budgetLimit) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }

                val recentTx = db.transactionDao().getRecentTransactionsSync(3)

                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
                    maximumFractionDigits = 0
                }

                for (widgetId in appWidgetIds) {
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    val layoutId = resolveLayoutForDimensions(minWidth, minHeight)

                    val views = RemoteViews(context.packageName, layoutId)

                    // Common fields across small, medium, large
                    views.setTextViewText(
                        R.id.widget_spent_today,
                        currencyFormat.format(todaySpent)
                    )

                    // Medium & Large specific fields
                    if (layoutId == R.layout.widget_medium || layoutId == R.layout.widget_large) {
                        views.setProgressBar(
                            R.id.widget_progress_bar,
                            100,
                            percentRemaining,
                            false
                        )

                        val statusText = "${currencyFormat.format(remainingBudget)} Left • Safe: ${currencyFormat.format(safeToSpendPerDay)}/d"
                        views.setTextViewText(R.id.widget_budget_status, statusText)
                    }

                    // Large specific fields (recent 3 transactions)
                    if (layoutId == R.layout.widget_large) {
                        if (recentTx.isEmpty()) {
                            views.setViewVisibility(R.id.widget_no_tx, View.VISIBLE)
                            views.setViewVisibility(R.id.widget_tx1_row, View.GONE)
                            views.setViewVisibility(R.id.widget_tx2_row, View.GONE)
                            views.setViewVisibility(R.id.widget_tx3_row, View.GONE)
                        } else {
                            views.setViewVisibility(R.id.widget_no_tx, View.GONE)

                            // Row 1
                            if (recentTx.isNotEmpty()) {
                                val tx1 = recentTx[0]
                                views.setViewVisibility(R.id.widget_tx1_row, View.VISIBLE)
                                views.setTextViewText(R.id.widget_tx1_title, tx1.displayName ?: tx1.counterparty ?: "Transaction")
                                val isDebit = tx1.type.equals("DEBIT", ignoreCase = true)
                                views.setTextViewText(R.id.widget_tx1_amount, "${if (isDebit) "-" else "+"}${currencyFormat.format(tx1.amount)}")
                                views.setTextColor(R.id.widget_tx1_amount, if (isDebit) 0xFFFF7B72.toInt() else 0xFF06D6A0.toInt())
                            } else {
                                views.setViewVisibility(R.id.widget_tx1_row, View.GONE)
                            }

                            // Row 2
                            if (recentTx.size > 1) {
                                val tx2 = recentTx[1]
                                views.setViewVisibility(R.id.widget_tx2_row, View.VISIBLE)
                                views.setTextViewText(R.id.widget_tx2_title, tx2.displayName ?: tx2.counterparty ?: "Transaction")
                                val isDebit = tx2.type.equals("DEBIT", ignoreCase = true)
                                views.setTextViewText(R.id.widget_tx2_amount, "${if (isDebit) "-" else "+"}${currencyFormat.format(tx2.amount)}")
                                views.setTextColor(R.id.widget_tx2_amount, if (isDebit) 0xFFFF7B72.toInt() else 0xFF06D6A0.toInt())
                            } else {
                                views.setViewVisibility(R.id.widget_tx2_row, View.GONE)
                            }

                            // Row 3
                            if (recentTx.size > 2) {
                                val tx3 = recentTx[2]
                                views.setViewVisibility(R.id.widget_tx3_row, View.VISIBLE)
                                views.setTextViewText(R.id.widget_tx3_title, tx3.displayName ?: tx3.counterparty ?: "Transaction")
                                val isDebit = tx3.type.equals("DEBIT", ignoreCase = true)
                                views.setTextViewText(R.id.widget_tx3_amount, "${if (isDebit) "-" else "+"}${currencyFormat.format(tx3.amount)}")
                                views.setTextColor(R.id.widget_tx3_amount, if (isDebit) 0xFFFF7B72.toInt() else 0xFF06D6A0.toInt())
                            } else {
                                views.setViewVisibility(R.id.widget_tx3_row, View.GONE)
                            }
                        }
                    }

                    // Tap to launch active MainActivity / MainActivityLight
                    val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
                        ?: Intent(context, MainActivity::class.java)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
