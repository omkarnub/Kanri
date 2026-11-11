package com.omkarnub.kanri.util

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.R
import com.omkarnub.kanri.service.QuickAddTileService
import com.omkarnub.kanri.ui.theme.AppIconManager
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.widget.KanriWidgetActions
import java.util.function.Consumer

object AppShortcutManager {

    /**
     * Publishes dynamic launcher shortcuts that align with the currently enabled launcher activity.
     */
    fun publishShortcuts(context: Context) {
        try {
            val themePrefs = ThemePreferences.getInstance(context)
            val targetClass = AppIconManager.getTargetActivityClass(themePrefs.appIconStyle, themePrefs.themeMode)

            val shortcuts = listOf(
                ShortcutInfoCompat.Builder(context, "shortcut_add_expense")
                    .setShortLabel(context.getString(R.string.shortcut_add_expense_short))
                    .setLongLabel(context.getString(R.string.shortcut_add_expense_long))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_expense))
                    .setRank(0)
                    .setIntent(
                        Intent(context, targetClass).apply {
                            action = KanriWidgetActions.ACTION_ADD_EXPENSE
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),

                ShortcutInfoCompat.Builder(context, "shortcut_add_income")
                    .setShortLabel(context.getString(R.string.shortcut_add_income_short))
                    .setLongLabel(context.getString(R.string.shortcut_add_income_long))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_income))
                    .setRank(1)
                    .setIntent(
                        Intent(context, targetClass).apply {
                            action = KanriWidgetActions.ACTION_ADD_INCOME
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),

                ShortcutInfoCompat.Builder(context, "shortcut_split_bill")
                    .setShortLabel(context.getString(R.string.shortcut_split_bill_short))
                    .setLongLabel(context.getString(R.string.shortcut_split_bill_long))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_split))
                    .setRank(2)
                    .setIntent(
                        Intent(context, targetClass).apply {
                            action = KanriWidgetActions.ACTION_SPLIT_BILL
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build(),

                ShortcutInfoCompat.Builder(context, "shortcut_lend_borrow")
                    .setShortLabel(context.getString(R.string.shortcut_lend_borrow_short))
                    .setLongLabel(context.getString(R.string.shortcut_lend_borrow_long))
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_lending))
                    .setRank(3)
                    .setIntent(
                        Intent(context, targetClass).apply {
                            action = KanriWidgetActions.ACTION_OPEN_LENDING
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                    .build()
            )

            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (ignored: Exception) {
            // Best effort dynamic shortcut publishing
        }
    }

    fun reportShortcutUsage(context: Context, action: String?) {
        val shortcutId = when (action) {
            KanriWidgetActions.ACTION_ADD_EXPENSE -> "shortcut_add_expense"
            KanriWidgetActions.ACTION_ADD_INCOME -> "shortcut_add_income"
            KanriWidgetActions.ACTION_SPLIT_BILL -> "shortcut_split_bill"
            KanriWidgetActions.ACTION_OPEN_LENDING -> "shortcut_lend_borrow"
            else -> return
        }
        try {
            ShortcutManagerCompat.reportShortcutUsed(context, shortcutId)
        } catch (_: Exception) {
            // Best effort dynamic shortcut usage signal
        }
    }

    /**
     * Checks if the system supports directly requesting to add the Quick Settings Tile (Android 13+ / API 33+).
     */
    fun isQuickSettingsTileRequestSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    /**
     * Triggers the native Android 13+ dialog asking the user to add the Quick Add tile to their quick settings shade.
     */
    fun requestAddQuickSettingsTile(
        context: Context,
        onResult: ((Int) -> Unit)? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val statusBarManager = context.getSystemService(StatusBarManager::class.java)
                val component = ComponentName(context, QuickAddTileService::class.java)
                val label = context.getString(R.string.quick_add_tile_label)
                val icon = Icon.createWithResource(context, R.drawable.ic_quick_add_tile)
                val executor = ContextCompat.getMainExecutor(context)

                statusBarManager?.requestAddTileService(
                    component,
                    label,
                    icon,
                    executor,
                    Consumer { resultCode ->
                        onResult?.invoke(resultCode)
                    }
                )
            } catch (ignored: Exception) {
                onResult?.invoke(-1)
            }
        }
    }
}
