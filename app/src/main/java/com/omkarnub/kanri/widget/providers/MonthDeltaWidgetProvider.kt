package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.MonthDeltaWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class MonthDeltaWidgetProvider : BasePixelWidgetProvider<MonthDeltaWidgetState>() {

    override suspend fun loadState(context: Context): MonthDeltaWidgetState {
        return WidgetDataSource.getMonthDeltaState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_TODAY

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, MonthDeltaWidgetProvider::class.java, playBurst)
        }
    }
}

