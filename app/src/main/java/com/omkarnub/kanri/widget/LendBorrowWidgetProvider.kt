package com.omkarnub.kanri.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LendBorrowWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, LendBorrowWidgetProvider::class.java)
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
                val allLending = db.lendingDao().getAllRecordsWithRepaymentsSync()
                val now = System.currentTimeMillis()

                // Filter unsettled records that have remaining balance
                val activeItems = allLending.mapNotNull { item ->
                    val totalRepaid = item.repayments.sumOf { it.amount }
                    val remaining = item.lending.amount - totalRepaid
                    if (!item.lending.isSettled && remaining > 0.0) {
                        val isOverdue = item.lending.dueDate != null && item.lending.dueDate!! < now
                        val isLent = item.lending.type.equals("LENT", ignoreCase = true)
                        Triple(item.lending, remaining, isOverdue)
                    } else null
                }.sortedWith(
                    compareByDescending<Triple<com.omkarnub.kanri.data.db.LendingEntity, Double, Boolean>> { it.third } // overdue first
                        .thenBy { it.first.dueDate ?: Long.MAX_VALUE }
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_lend_borrow)

                    // Check resize dimensions
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    if (activeItems.isEmpty()) {
                        views.setViewVisibility(R.id.widget_lending_empty, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_lending_list, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_lending_empty, View.GONE)
                        views.setViewVisibility(R.id.widget_lending_list, View.VISIBLE)

                        // Item 1
                        val item1 = activeItems[0]
                        val isLent1 = item1.first.type.equals("LENT", ignoreCase = true)
                        views.setTextViewText(R.id.widget_lending_person1, item1.first.personName)
                        views.setTextViewText(
                            R.id.widget_lending_subtext1,
                            if (isLent1) "owes you" else "you owe"
                        )
                        views.setTextViewText(
                            R.id.widget_lending_amount1,
                            CurrencyUtils.formatCompactCurrency(item1.second)
                        )
                        if (item1.third) {
                            views.setTextViewText(R.id.widget_lending_status1, "Overdue")
                            views.setTextColor(R.id.widget_lending_status1, 0xFFFFFFFF.toInt())
                        } else if (item1.first.dueDate != null) {
                            val daysLeft = ((item1.first.dueDate!! - now) / 86400000L).coerceAtLeast(0)
                            views.setTextViewText(
                                R.id.widget_lending_status1,
                                if (daysLeft == 0L) "Due today" else "Due in ${daysLeft}d"
                            )
                            views.setTextColor(R.id.widget_lending_status1, 0xB3FFFFFF.toInt())
                        } else {
                            views.setTextViewText(R.id.widget_lending_status1, "Pending")
                            views.setTextColor(R.id.widget_lending_status1, 0xB3FFFFFF.toInt())
                        }

                        // Determine item count based on resize height
                        // minHeight < 80: only Item 1
                        // minHeight 80..144: up to Item 2
                        // minHeight >= 145 or default: up to Item 3
                        val allowItem2 = (minHeight == 0 || minHeight >= 80) && activeItems.size > 1
                        val allowItem3 = (minHeight >= 145) && activeItems.size > 2

                        // Item 2
                        if (allowItem2) {
                            val item2 = activeItems[1]
                            val isLent2 = item2.first.type.equals("LENT", ignoreCase = true)
                            views.setViewVisibility(R.id.widget_lending_divider, View.VISIBLE)
                            views.setViewVisibility(R.id.widget_lending_item2, View.VISIBLE)
                            views.setTextViewText(R.id.widget_lending_person2, item2.first.personName)
                            views.setTextViewText(
                                R.id.widget_lending_subtext2,
                                if (isLent2) "owes you" else "you owe"
                            )
                            views.setTextViewText(
                                R.id.widget_lending_amount2,
                                CurrencyUtils.formatCompactCurrency(item2.second)
                            )
                            if (item2.third) {
                                views.setTextViewText(R.id.widget_lending_status2, "Overdue")
                                views.setTextColor(R.id.widget_lending_status2, 0xFFFFFFFF.toInt())
                            } else if (item2.first.dueDate != null) {
                                val daysLeft = ((item2.first.dueDate!! - now) / 86400000L).coerceAtLeast(0)
                                views.setTextViewText(
                                    R.id.widget_lending_status2,
                                    if (daysLeft == 0L) "Due today" else "Due in ${daysLeft}d"
                                )
                                views.setTextColor(R.id.widget_lending_status2, 0xB3FFFFFF.toInt())
                            } else {
                                views.setTextViewText(R.id.widget_lending_status2, "Pending")
                                views.setTextColor(R.id.widget_lending_status2, 0xB3FFFFFF.toInt())
                            }
                        } else {
                            views.setViewVisibility(R.id.widget_lending_divider, View.GONE)
                            views.setViewVisibility(R.id.widget_lending_item2, View.GONE)
                        }

                        // Item 3 (for tall resized widgets)
                        if (allowItem3) {
                            val item3 = activeItems[2]
                            val isLent3 = item3.first.type.equals("LENT", ignoreCase = true)
                            views.setViewVisibility(R.id.widget_lending_divider2, View.VISIBLE)
                            views.setViewVisibility(R.id.widget_lending_item3, View.VISIBLE)
                            views.setTextViewText(R.id.widget_lending_person3, item3.first.personName)
                            views.setTextViewText(
                                R.id.widget_lending_subtext3,
                                if (isLent3) "owes you" else "you owe"
                            )
                            views.setTextViewText(
                                R.id.widget_lending_amount3,
                                CurrencyUtils.formatCompactCurrency(item3.second)
                            )
                            if (item3.third) {
                                views.setTextViewText(R.id.widget_lending_status3, "Overdue")
                                views.setTextColor(R.id.widget_lending_status3, 0xFFFFFFFF.toInt())
                            } else if (item3.first.dueDate != null) {
                                val daysLeft = ((item3.first.dueDate!! - now) / 86400000L).coerceAtLeast(0)
                                views.setTextViewText(
                                    R.id.widget_lending_status3,
                                    if (daysLeft == 0L) "Due today" else "Due in ${daysLeft}d"
                                )
                                views.setTextColor(R.id.widget_lending_status3, 0xB3FFFFFF.toInt())
                            } else {
                                views.setTextViewText(R.id.widget_lending_status3, "Pending")
                                views.setTextColor(R.id.widget_lending_status3, 0xB3FFFFFF.toInt())
                            }
                        } else {
                            views.setViewVisibility(R.id.widget_lending_divider2, View.GONE)
                            views.setViewVisibility(R.id.widget_lending_item3, View.GONE)
                        }
                    }

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_LENDING,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_lend_borrow_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
