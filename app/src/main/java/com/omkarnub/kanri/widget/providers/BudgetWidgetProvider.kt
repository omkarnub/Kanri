package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.BudgetWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class BudgetWidgetProvider : BasePixelWidgetProvider<BudgetWidgetState>() {

    override suspend fun loadState(context: Context): BudgetWidgetState {
        return WidgetDataSource.getBudgetState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_BUDGET

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, BudgetWidgetProvider::class.java, playBurst)
        }
    }
}
