package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.TodayWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class TodayWidgetProvider : BasePixelWidgetProvider<TodayWidgetState>() {

    override suspend fun loadState(context: Context): TodayWidgetState {
        return WidgetDataSource.getTodayState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_TODAY

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, TodayWidgetProvider::class.java, playBurst)
        }
    }
}

