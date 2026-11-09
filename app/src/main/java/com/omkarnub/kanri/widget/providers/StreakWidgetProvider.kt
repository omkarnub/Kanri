package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.StreakWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class StreakWidgetProvider : BasePixelWidgetProvider<StreakWidgetState>() {

    override suspend fun loadState(context: Context): StreakWidgetState {
        return WidgetDataSource.getStreakState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_STREAK

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, StreakWidgetProvider::class.java, playBurst)
        }
    }
}

