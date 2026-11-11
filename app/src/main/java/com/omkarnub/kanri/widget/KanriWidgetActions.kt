package com.omkarnub.kanri.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.omkarnub.kanri.MainActivityDarkDark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetActionEvent(
    val action: String,
    val id: Long = System.nanoTime()
)

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
    const val ACTION_OPEN_WALLET = "com.omkarnub.kanri.action.OPEN_WALLET"
    const val ACTION_OPEN_BUDGET = "com.omkarnub.kanri.action.OPEN_BUDGET"
    const val ACTION_OPEN_REVIEW = "com.omkarnub.kanri.action.OPEN_REVIEW"
    const val ACTION_OPEN_HEATMAP = "com.omkarnub.kanri.action.OPEN_HEATMAP"
    const val ACTION_OPEN_STREAK = "com.omkarnub.kanri.action.OPEN_STREAK"
    const val ACTION_OPEN_TODAY = "com.omkarnub.kanri.action.OPEN_TODAY"

    const val EXTRA_AMOUNT = "extra_split_amount"
    const val EXTRA_DESCRIPTION = "extra_split_description"

    private val _pendingActionEvent = MutableStateFlow<WidgetActionEvent?>(null)
    val pendingActionEvent = _pendingActionEvent.asStateFlow()

    private val _pendingAction = MutableStateFlow<String?>(null)
    val pendingAction = _pendingAction.asStateFlow()

    var pendingSplitAmount: Double? = null
        private set
    var pendingSplitDescription: String? = null
        private set

    fun triggerSplitBill(amount: Double?, description: String?) {
        pendingSplitAmount = amount
        pendingSplitDescription = description
        _pendingAction.value = ACTION_SPLIT_BILL
        _pendingActionEvent.value = WidgetActionEvent(ACTION_SPLIT_BILL)
    }

    fun handleAction(action: String?, amount: Double? = null, description: String? = null) {
        if (action != null && action.startsWith("com.omkarnub.kanri.action.")) {
            if (action == ACTION_SPLIT_BILL) {
                if (amount != null) pendingSplitAmount = amount
                if (description != null) pendingSplitDescription = description
            }
            _pendingAction.value = action
            _pendingActionEvent.value = WidgetActionEvent(action)
        }
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val amount = if (intent.hasExtra(EXTRA_AMOUNT)) intent.getDoubleExtra(EXTRA_AMOUNT, 0.0) else null
        val desc = intent.getStringExtra(EXTRA_DESCRIPTION)
        handleAction(intent.action, amount, desc)
    }

    fun consumeAction(): String? {
        val action = _pendingAction.value
        _pendingAction.value = null
        _pendingActionEvent.value = null
        return action
    }

    fun consumeSplitPayload(): Pair<Double?, String?> {
        val payload = Pair(pendingSplitAmount, pendingSplitDescription)
        pendingSplitAmount = null
        pendingSplitDescription = null
        return payload
    }

    fun createPendingIntent(
        context: Context,
        action: String,
        requestCode: Int
    ): PendingIntent {
        val intent = if (
            action == ACTION_ADD_EXPENSE ||
            action == ACTION_ADD_INCOME ||
            action == ACTION_SPLIT_BILL
        ) {
            Intent(context, com.omkarnub.kanri.ui.quickentry.QuickEntryActivity::class.java).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(context).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
