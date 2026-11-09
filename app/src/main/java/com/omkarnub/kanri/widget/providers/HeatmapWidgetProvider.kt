package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.HeatmapWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class HeatmapWidgetProvider : BasePixelWidgetProvider<HeatmapWidgetState>() {

    override suspend fun loadState(context: Context): HeatmapWidgetState {
        return WidgetDataSource.getHeatmapState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_HEATMAP

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, HeatmapWidgetProvider::class.java, playBurst)
        }
    }
}

