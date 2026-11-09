package com.omkarnub.kanri.widget.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.omkarnub.kanri.R
import com.omkarnub.kanri.widget.KanriWidgetActions
import com.omkarnub.kanri.widget.core.PixelMotion
import com.omkarnub.kanri.widget.core.WidgetState
import com.omkarnub.kanri.widget.render.WidgetRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Common base for all 12 Kanri Pixel DotTiles widget providers.
 * Handles dimensions, canvas rendering, deep-link routing, and home screen burst animations.
 */
abstract class BasePixelWidgetProvider<T : WidgetState> : AppWidgetProvider() {

    abstract suspend fun loadState(context: Context): T
    abstract fun getClickAction(): String

    open fun getLayoutResId(): Int = R.layout.widget_tile_canvas

    open fun bindClickTargets(context: Context, views: RemoteViews, state: T) {
        val clickIntent = KanriWidgetActions.createPendingIntent(
            context,
            getClickAction(),
            requestCode = this::class.java.hashCode()
        )
        views.setOnClickPendingIntent(R.id.widget_root, clickIntent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = loadState(context)
                for (id in appWidgetIds) {
                    renderFrame(context, appWidgetManager, id, state, progress = 1.0f)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = loadState(context)
                renderFrame(context, appWidgetManager, appWidgetId, state, progress = 1.0f)
            } finally {
                pendingResult.finish()
            }
        }
    }

    fun renderFrame(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: T,
        progress: Float
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val density = context.resources.displayMetrics.density
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val wDp = if (isPortrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 260)
        }.coerceIn(80, 600)

        val hDp = if (isPortrait) {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 140)
        } else {
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
        }.coerceIn(60, 500)

        val widthPx = (wDp * density).toInt()
        val heightPx = (hDp * density).toInt()

        val bitmap = WidgetRenderer.renderToBitmap(
            context = context,
            state = state,
            widthPx = widthPx,
            heightPx = heightPx,
            progress = progress,
            density = density
        )

        val views = RemoteViews(context.packageName, getLayoutResId()).apply {
            setImageViewBitmap(R.id.widget_canvas_image, bitmap)
            setContentDescription(R.id.widget_canvas_image, state.accessibleDescription)
        }

        bindClickTargets(context, views, state)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    companion object {
        private var lastBurstTime = 0L

        fun <P : BasePixelWidgetProvider<T>, T : WidgetState> triggerUpdate(
            context: Context,
            providerClass: Class<P>,
            playBurst: Boolean = false
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, providerClass)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val now = SystemClock.elapsedRealtime()
            val allowBurst = playBurst && (now - lastBurstTime >= PixelMotion.BURST_RATE_LIMIT_MS)
            if (allowBurst) lastBurstTime = now

            CoroutineScope(Dispatchers.IO).launch {
                val providerInstance = providerClass.getDeclaredConstructor().newInstance()
                val state = providerInstance.loadState(context)

                if (allowBurst) {
                    val frames = PixelMotion.BURST_FRAME_COUNT
                    for (f in 1..frames) {
                        val progress = f.toFloat() / frames
                        for (id in ids) {
                            providerInstance.renderFrame(context, appWidgetManager, id, state, progress)
                        }
                        if (f < frames) delay(PixelMotion.BURST_FRAME_INTERVAL_MS)
                    }
                } else {
                    for (id in ids) {
                        providerInstance.renderFrame(context, appWidgetManager, id, state, progress = 1.0f)
                    }
                }
            }
        }
    }
}
