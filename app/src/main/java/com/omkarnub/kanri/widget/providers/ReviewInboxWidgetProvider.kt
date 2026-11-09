package com.omkarnub.kanri.widget.providers

import android.content.Context
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.ReviewInboxWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class ReviewInboxWidgetProvider : BasePixelWidgetProvider<ReviewInboxWidgetState>() {

    override suspend fun loadState(context: Context): ReviewInboxWidgetState {
        return WidgetDataSource.getReviewInboxState(context)
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_REVIEW

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, ReviewInboxWidgetProvider::class.java, playBurst)
        }
    }
}

