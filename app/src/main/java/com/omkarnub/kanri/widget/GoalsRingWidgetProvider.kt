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

class GoalsRingWidgetProvider : AppWidgetProvider() {

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
                ComponentName(context, GoalsRingWidgetProvider::class.java)
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
                val activeGoals = db.savingsGoalDao().getActiveGoalsSync()

                val topGoal = activeGoals.firstOrNull()

                val progressPercent = if (topGoal != null && topGoal.targetAmount > 0) {
                    ((topGoal.currentAmount / topGoal.targetAmount) * 100).toFloat().coerceIn(0f, 100f)
                } else {
                    0f
                }

                val ringBitmap = WidgetCanvasRenderer.renderGoalRing(
                    context = context,
                    progressPercent = progressPercent,
                    emoji = topGoal?.emoji ?: "🎯",
                    sizePx = 480
                )

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_goals_ring)

                    // Check resize dimensions
                    val options = appWidgetManager.getAppWidgetOptions(widgetId)
                    val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                    if (minHeight in 1..85) {
                        // Compact vertical resize: hide amounts so ring stays clear
                        views.setViewVisibility(R.id.widget_goal_amounts, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_goal_amounts, android.view.View.VISIBLE)
                    }

                    if (minHeight in 1..55) {
                        // Extremely small: hide header too
                        views.setViewVisibility(R.id.widget_goals_ring_header, android.view.View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_goals_ring_header, android.view.View.VISIBLE)
                    }

                    views.setImageViewBitmap(R.id.widget_goal_ring_image, ringBitmap)

                    if (topGoal != null) {
                        views.setTextViewText(R.id.widget_goal_title, topGoal.title)
                        views.setTextViewText(
                            R.id.widget_goal_amounts,
                            "${CurrencyUtils.formatCompactCurrency(topGoal.currentAmount)} of ${CurrencyUtils.formatCompactCurrency(topGoal.targetAmount)}"
                        )
                    } else {
                        views.setTextViewText(R.id.widget_goal_title, "No active goals")
                        views.setTextViewText(R.id.widget_goal_amounts, "Tap to create a savings goal")
                    }

                    val pendingIntent = KanriWidgetActions.createPendingIntent(
                        context,
                        KanriWidgetActions.ACTION_OPEN_GOALS,
                        widgetId
                    )
                    views.setOnClickPendingIntent(R.id.widget_goals_ring_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }
    }
}
