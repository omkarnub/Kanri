package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.GoalsWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class GoalsWidgetProvider : BasePixelWidgetProvider<GoalsWidgetState>() {

    override suspend fun loadState(context: Context): GoalsWidgetState {
        return WidgetDataSource.getGoalsState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_GOALS

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, GoalsWidgetProvider::class.java, playBurst)
        }
    }
}

