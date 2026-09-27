package com.omkarnub.kanri.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.omkarnub.kanri.R

class SplitBillWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, SplitBillWidgetProvider::class.java)
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
                val views = RemoteViews(context.packageName, R.layout.widget_split_bill)

                // Check resize dimensions
                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0

                if (minWidth in 1..130) {
                    // In narrow width, hide subtitle to prevent text clipping
                    views.setViewVisibility(R.id.widget_split_bill_subtitle, android.view.View.GONE)
                } else {
                    views.setViewVisibility(R.id.widget_split_bill_subtitle, android.view.View.VISIBLE)
                }

                val pendingIntent = KanriWidgetActions.createPendingIntent(
                    context,
                    KanriWidgetActions.ACTION_SPLIT_BILL,
                    widgetId
                )
                views.setOnClickPendingIntent(R.id.widget_split_bill_root, pendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
