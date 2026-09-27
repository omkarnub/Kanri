package com.omkarnub.kanri.widget

import android.content.Context

/**
 * Central coordinator to refresh all active Kanri home screen widgets on the device.
 */
object KanriWidgetsUpdater {

    fun updateAllWidgets(context: Context) {
        try {
            BudgetRingWidgetProvider.updateAll(context)
            BudgetProgressWidgetProvider.updateAll(context)
            LendBorrowWidgetProvider.updateAll(context)
            GoalsRingWidgetProvider.updateAll(context)
            SplitBillWidgetProvider.updateAll(context)
            QuickAddWidgetProvider.updateAll(context)
            MoneySpentTodayWidgetProvider.updateAll(context)
        } catch (e: Exception) {
            com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(
                context, "KanriWidgetsUpdater.updateAllWidgets", e
            )
        }
    }
}
