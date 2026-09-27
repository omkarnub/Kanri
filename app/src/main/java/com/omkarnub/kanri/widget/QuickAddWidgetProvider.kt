package com.omkarnub.kanri.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.omkarnub.kanri.R

class QuickAddWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, QuickAddWidgetProvider::class.java)
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
            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

                // Check resize dimensions
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                if (minHeight in 1..65) {
                    // In compact 1-row height, hide header to maximize button touch targets
                    views.setViewVisibility(R.id.widget_quick_add_header, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_quick_add_header, android.view.View.VISIBLE)
                }

                // Expense Pending Intent (unique request code 1000 + widgetId)
                val expensePendingIntent = KanriWidgetActions.createPendingIntent(
                    context,
                    KanriWidgetActions.ACTION_ADD_EXPENSE,
                    widgetId * 10 + 1
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePendingIntent)

                // Income Pending Intent (unique request code 2000 + widgetId)
                val incomePendingIntent = KanriWidgetActions.createPendingIntent(
                    context,
                    KanriWidgetActions.ACTION_ADD_INCOME,
                    widgetId * 10 + 2
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add_income, incomePendingIntent)

                // Root fallback opens Home
                val rootPendingIntent = KanriWidgetActions.createPendingIntent(
                    context,
                    KanriWidgetActions.ACTION_OPEN_HOME,
                    widgetId
                )
                views.setOnClickPendingIntent(R.id.widget_quick_add_root, rootPendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
