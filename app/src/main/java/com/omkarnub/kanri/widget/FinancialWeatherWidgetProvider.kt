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

/**
 * 9th Kanri Home Screen Widget: Financial Sky Weather.
 * A living dot-matrix celestial sky tracking the user's financial climate, day/night arc,
 * and 7-day spending pulse.
 */
class FinancialWeatherWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, FinancialWeatherWidgetProvider::class.java)
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
                ComponentName(context, FinancialWeatherWidgetProvider::class.java)
            ).toSet()
            val targetIds = appWidgetIds.filter { it in validIds }.toIntArray()
            if (targetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val db = KanriDatabase.getDatabase(context)

                // 1. Calculate today's spent
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
                val debitTx = todayTx.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                val todaySpent = debitTx.sumOf { it.transaction.amount }

                // 2. Monthly Budget & Safe to Spend
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
                val monthTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(monthCal.timeInMillis, now.timeInMillis)
                val monthSpent = monthTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                    .sumOf { it.transaction.amount }

                val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                val daysLeft = maxOf(1, maxDays - now.get(Calendar.DAY_OF_MONTH))
                val remainingBudget = (budgetLimit - monthSpent).coerceAtLeast(0.0)
                val safeToSpendDaily = remainingBudget / daysLeft

                // 3. Past 5 days spend records
                val dayFormat = SimpleDateFormat("EEE", Locale.US)
                val weekSpends = mutableListOf<Pair<String, Double>>()
                for (offset in 4 downTo 1) {
                    val dCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -offset)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val sStart = dCal.timeInMillis
                    dCal.set(Calendar.HOUR_OF_DAY, 23)
                    dCal.set(Calendar.MINUTE, 59)
                    val sEnd = dCal.timeInMillis
                    val dTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(sStart, sEnd)
                    val dSpend = dTx.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                        .sumOf { it.transaction.amount }
                    weekSpends.add(Pair(dayFormat.format(dCal.time), dSpend))
                }
                weekSpends.add(Pair("Today", todaySpent))

                val widgetStyle = WidgetPreferences.getInstance(context).widgetStyle
                val bgRes = when (widgetStyle) {
                    WidgetStyle.IPHONE -> R.drawable.widget_bg_iphone
                    WidgetStyle.PIXEL_MATRIX -> R.drawable.widget_bg_pixel
                    WidgetStyle.GLASS_100X -> R.drawable.widget_bg_glass_100x
                }

                val weatherBitmap = WidgetCanvasRenderer.renderFinancialWeather(
                    context = context,
                    todaySpent = todaySpent,
                    monthBudget = budgetLimit,
                    monthSpent = monthSpent,
                    safeToSpendDaily = safeToSpendDaily,
                    weekSpends = weekSpends,
                    widthPx = 600,
                    heightPx = 360,
                    widgetStyle = widgetStyle
                )

                val timeStr = SimpleDateFormat("HH:mm", Locale.US).format(now.time)

                for (widgetId in targetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_financial_weather)
                    views.setInt(R.id.widget_financial_weather_root, "setBackgroundResource", bgRes)
                    views.setImageViewBitmap(R.id.widget_weather_canvas_image, weatherBitmap)
                    views.setTextViewText(R.id.widget_weather_clock, "Live • $timeStr")
                    views.setTextViewText(
                        R.id.widget_weather_spent_text,
                        "Spent: " + CurrencyUtils.formatCurrency(todaySpent)
                    )
                    views.setTextViewText(
                        R.id.widget_weather_safe_text,
                        "Safe " + CurrencyUtils.formatCompactCurrency(safeToSpendDaily) + "/d"
                    )

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_HOME,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_financial_weather_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}

