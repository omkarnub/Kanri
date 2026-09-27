package com.omkarnub.kanri.service

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.R
import com.omkarnub.kanri.widget.KanriWidgetActions

/**
 * Android System Quick Settings Tile for Kanri.
 * Allows quick transaction entry directly from the notification pull-down shade.
 */
class QuickAddTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = getString(R.string.quick_add_tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(R.string.quick_add_tile_subtitle)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_add)
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val launchIntent = (packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivityDarkDark::class.java)).apply {
            action = KanriWidgetActions.ACTION_ADD_EXPENSE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            1005,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val executeLaunch = Runnable {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        }

        if (isLocked) {
            unlockAndRun {
                executeLaunch.run()
            }
        } else {
            executeLaunch.run()
        }
    }
}
