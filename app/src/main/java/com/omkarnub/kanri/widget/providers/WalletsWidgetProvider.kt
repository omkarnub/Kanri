package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.WalletsWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class WalletsWidgetProvider : BasePixelWidgetProvider<WalletsWidgetState>() {

    override suspend fun loadState(context: Context): WalletsWidgetState {
        return WidgetDataSource.getWalletsState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_WALLET

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, WalletsWidgetProvider::class.java, playBurst)
        }
    }
}

