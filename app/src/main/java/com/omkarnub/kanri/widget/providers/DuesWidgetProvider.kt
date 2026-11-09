package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.DuesWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class DuesWidgetProvider : BasePixelWidgetProvider<DuesWidgetState>() {

    override suspend fun loadState(context: Context): DuesWidgetState {
        return WidgetDataSource.getDuesState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_LENDING

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, DuesWidgetProvider::class.java, playBurst)
        }
    }
}

