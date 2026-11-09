package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.SafeToSpendWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class SafeToSpendWidgetProvider : BasePixelWidgetProvider<SafeToSpendWidgetState>() {

    override suspend fun loadState(context: Context): SafeToSpendWidgetState {
        return WidgetDataSource.getSafeToSpendState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_BUDGET

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, SafeToSpendWidgetProvider::class.java, playBurst)
        }
    }
}
