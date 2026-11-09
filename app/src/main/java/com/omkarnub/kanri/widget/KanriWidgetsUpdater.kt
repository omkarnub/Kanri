package com.omkarnub.kanri.widget

import android.content.Context
import com.omkarnub.kanri.widget.providers.TodayWidgetProvider
import com.omkarnub.kanri.widget.providers.BudgetWidgetProvider
import com.omkarnub.kanri.widget.providers.WalletsWidgetProvider
import com.omkarnub.kanri.widget.providers.GoalsWidgetProvider
import com.omkarnub.kanri.widget.providers.DuesWidgetProvider
import com.omkarnub.kanri.widget.providers.SkyWidgetProvider
import com.omkarnub.kanri.widget.providers.HeatmapWidgetProvider
import com.omkarnub.kanri.widget.providers.StreakWidgetProvider
import com.omkarnub.kanri.widget.providers.SafeToSpendWidgetProvider
import com.omkarnub.kanri.widget.providers.ReviewInboxWidgetProvider
import com.omkarnub.kanri.widget.providers.MonthDeltaWidgetProvider
import com.omkarnub.kanri.widget.providers.QuickActionsWidgetProvider

/**
 * Central coordinator to refresh all active Kanri home screen widgets on the device.
 * Dispatches updates only to placed widgets across the 12 Pixel DotTiles providers.
 */
object KanriWidgetsUpdater {

    fun updateAllWidgets(context: Context, playBurst: Boolean = true) {
        try {
            TodayWidgetProvider.updateAll(context, playBurst)
            BudgetWidgetProvider.updateAll(context, playBurst)
            WalletsWidgetProvider.updateAll(context, playBurst)
            GoalsWidgetProvider.updateAll(context, playBurst)
            DuesWidgetProvider.updateAll(context, playBurst)
            SkyWidgetProvider.updateAll(context, playBurst)
            HeatmapWidgetProvider.updateAll(context, playBurst)
            StreakWidgetProvider.updateAll(context, playBurst)
            SafeToSpendWidgetProvider.updateAll(context, playBurst)
            ReviewInboxWidgetProvider.updateAll(context, playBurst)
            MonthDeltaWidgetProvider.updateAll(context, playBurst)
            QuickActionsWidgetProvider.updateAll(context, playBurst = false)
        } catch (e: Exception) {
            com.omkarnub.kanri.data.crash.CrashLogger.logHandledException(
                context, "KanriWidgetsUpdater.updateAllWidgets", e
            )
        }
    }
}
