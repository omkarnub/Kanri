package com.omkarnub.kanri.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.res.ResourcesCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.util.CurrencyUtils

/**
 * High-performance Canvas renderer that draws pure frosted white ring charts and custom
 * visual elements for Android Phone Home Screen RemoteViews widgets.
 */
object WidgetCanvasRenderer {

    /**
     * Renders a circular budget ring chart with remaining amount in the center,
     * styled with Google Sans typography and clean frosted white arcs.
     */
    fun renderBudgetRing(
        context: Context,
        spent: Double,
        budget: Double,
        sizePx: Int = 360
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = sizePx * 0.11f
        val padding = strokeWidth / 2f + sizePx * 0.04f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // 1. Frosted track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0x33FFFFFF.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // 2. Progress arc (clean white)
        val ratio = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0) else 0.0
        val isOver = spent > budget
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0xFFFFFFFF.toInt()
        }
        if (ratio > 0.0 || isOver) {
            val sweep = if (isOver) 360f else (ratio * 360.0).toFloat().coerceIn(1f, 360f)
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)
        }

        // 3. Center typography using Google Sans (pure white)
        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val fontMedium = ResourcesCompat.getFont(context, R.font.google_sans_medium)

        val centerX = sizePx / 2f
        val centerY = sizePx / 2f

        // "Left" or "Over" subtitle
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontMedium
            color = 0xB3FFFFFF.toInt()
            textSize = sizePx * 0.09f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(if (isOver) "Over" else "Left", centerX, centerY - sizePx * 0.08f, subPaint)

        // Amount center text
        val remaining = (budget - spent).coerceAtLeast(0.0)
        val amountText = CurrencyUtils.formatCompactCurrency(if (isOver) spent - budget else remaining)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.17f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(amountText, centerX, centerY + sizePx * 0.10f, textPaint)

        return bitmap
    }

    /**
     * Renders a circular savings goals ring chart with goal progress percentage and emoji,
     * styled with Google Sans typography and clean frosted white arcs.
     */
    fun renderGoalRing(
        context: Context,
        progressPercent: Float,
        emoji: String,
        sizePx: Int = 360
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val strokeWidth = sizePx * 0.11f
        val padding = strokeWidth / 2f + sizePx * 0.04f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // 1. Frosted track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0x33FFFFFF.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // 2. Progress arc (clean white)
        val ratio = (progressPercent / 100f).coerceIn(0f, 1f)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0xFFFFFFFF.toInt()
        }
        if (ratio > 0f) {
            canvas.drawArc(arcRect, -90f, ratio * 360f, false, progressPaint)
        }

        // 3. Center emoji and percentage (white)
        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f

        // Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emoji.ifEmpty { "🎯" }, centerX, centerY - sizePx * 0.04f, emojiPaint)

        // Percentage text (pure white)
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${progressPercent.toInt()}%", centerX, centerY + sizePx * 0.15f, percentPaint)

        return bitmap
    }
}
