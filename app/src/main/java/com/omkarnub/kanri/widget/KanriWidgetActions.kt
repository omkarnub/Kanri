package com.omkarnub.kanri.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.omkarnub.kanri.MainActivityDarkDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles deep linking and action routing from Android phone home screen widgets.
 */
object KanriWidgetActions {
    const val ACTION_SPLIT_BILL = "com.omkarnub.kanri.action.SPLIT_BILL"
    const val ACTION_ADD_EXPENSE = "com.omkarnub.kanri.action.ADD_EXPENSE"
    const val ACTION_ADD_INCOME = "com.omkarnub.kanri.action.ADD_INCOME"
    const val ACTION_OPEN_GOALS = "com.omkarnub.kanri.action.OPEN_GOALS"
    const val ACTION_OPEN_LENDING = "com.omkarnub.kanri.action.OPEN_LENDING"
    const val ACTION_OPEN_HOME = "com.omkarnub.kanri.action.OPEN_HOME"

    private val _pendingAction = MutableStateFlow<String?>(null)
    val pendingAction = _pendingAction.asStateFlow()

    fun handleAction(action: String?) {
        if (action != null && action.startsWith("com.omkarnub.kanri.action.")) {
            _pendingAction.value = action
        }
    }

    fun handleIntent(intent: Intent?) {
        handleAction(intent?.action)
    }

    fun consumeAction(): String? {
        val current = _pendingAction.value
        _pendingAction.value = null
        return current
    }

    fun createPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = (context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivityDarkDark::class.java)).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
