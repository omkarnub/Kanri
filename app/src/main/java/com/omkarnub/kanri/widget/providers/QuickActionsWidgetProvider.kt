package com.omkarnub.kanri.widget.providers

import android.content.Context
import android.widget.RemoteViews
import com.omkarnub.kanri.R
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.QuickActionsWidgetState
import com.omkarnub.kanri.widget.data.WidgetDataSource

class QuickActionsWidgetProvider : BasePixelWidgetProvider<QuickActionsWidgetState>() {

    override suspend fun loadState(context: Context): QuickActionsWidgetState {
        return WidgetDataSource.getQuickActionsState()
    }

    override fun getClickAction(): String = KanriWidgetActions.ACTION_OPEN_HOME

    override fun getLayoutResId(): Int = R.layout.widget_quick_actions

    override fun bindClickTargets(context: Context, views: RemoteViews, state: QuickActionsWidgetState) {
        val expenseIntent = KanriWidgetActions.createPendingIntent(
            context,
            KanriWidgetActions.ACTION_ADD_EXPENSE,
            requestCode = 201
        )
        val incomeIntent = KanriWidgetActions.createPendingIntent(
            context,
            KanriWidgetActions.ACTION_ADD_INCOME,
            requestCode = 202
        )
        val splitIntent = KanriWidgetActions.createPendingIntent(
            context,
            KanriWidgetActions.ACTION_SPLIT_BILL,
            requestCode = 203
        )

        views.setOnClickPendingIntent(R.id.btn_action_expense, expenseIntent)
        views.setOnClickPendingIntent(R.id.btn_action_income, incomeIntent)
        views.setOnClickPendingIntent(R.id.btn_action_split, splitIntent)
    }

    companion object {
        fun updateAll(context: Context, playBurst: Boolean = false) {
            triggerUpdate(context, QuickActionsWidgetProvider::class.java, playBurst)
        }
    }
}

