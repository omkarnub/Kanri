package com.omkarnub.kanri.widget.font

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.widget.core.PixelTheme
import java.util.Locale
import kotlin.math.abs

/**
 * Pixel typography engine for Kanri DotTiles widgets.
 * Combines authentic Nothing Dot-Matrix typography (`Ndot57`) for numbers & metrics
 * with clean, high-contrast system sans typography for titles, subtitles, and labels.
 */
object PixelFont {

    @Volatile
    private var cachedNdot: Typeface? = null

    /**
     * Retrieves the authentic Ndot57 dot-matrix typeface.
     * Returns null if running in unmocked JVM unit tests or if loading fails.
     */
    fun getNdotTypeface(context: Context?): Typeface? {
        cachedNdot?.let { return it }
        if (context != null) {
            try {
                val tf = ResourcesCompat.getFont(context, R.font.ndot57)
                if (tf != null) {
                    cachedNdot = tf
                    return tf
                }
            } catch (_: Throwable) {
                // Graceful fallback for test runner / mock context
            }
        }
        return try {
            Typeface.MONOSPACE
        } catch (_: Throwable) {
            null
        }
    }

    val sansMedium: Typeface? by lazy {
        try {
            Typeface.create("sans-serif-medium", Typeface.NORMAL)
        } catch (_: Throwable) {
            null
        }
    }

    val sansRegular: Typeface? by lazy {
        try {
            Typeface.create("sans-serif", Typeface.NORMAL)
        } catch (_: Throwable) {
            null
        }
    }

    val sansBold: Typeface? by lazy {
        try {
            Typeface.create("sans-serif", Typeface.BOLD)
        } catch (_: Throwable) {
            null
        }
    }

    private val titlePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
            color = PixelTheme.COLOR_DOT_ON_INT
        }
    }

    private val subPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansRegular
            color = PixelTheme.COLOR_DOT_MID_INT
        }
    }

    private val metricPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val prefixPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
        }
    }

    private val labelPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
        }
    }

    /**
     * Formats an amount into compact Indian format:
     * - `₹850` (for < 1,000)
     * - `₹1.2k` (for >= 1,000)
     * - `₹12.4k` (for >= 10,000)
     * - `₹1.2L` (for >= 1,00,000)
     * - `₹15L` (for >= 10,00,000)
     * - `₹1.5Cr` (for >= 1,00,00,000)
     */
    fun formatIndianCompact(amount: Double, prefix: String = "₹"): String {
        return DotMatrixFont.formatIndianCompact(amount, prefix)
    }

    /**
     * Splits formatted currency text like "-₹12.4k" into prefix "-₹" and number "12.4k".
     */
    fun splitCurrency(text: String): Pair<String, String> {
        val trimmed = text.trim()
        val hasMinus = trimmed.startsWith("-")
        val withoutMinus = if (hasMinus) trimmed.substring(1) else trimmed
        return if (withoutMinus.startsWith("₹")) {
            val prefix = if (hasMinus) "-₹" else "₹"
            Pair(prefix, withoutMinus.substring(1).trim())
        } else if (withoutMinus.startsWith("$")) {
            val prefix = if (hasMinus) "-$" else "$"
            Pair(prefix, withoutMinus.substring(1).trim())
        } else {
            Pair("", trimmed)
        }
    }

    /**
     * Draws the widget title at top-left in clean sans-serif.
     */
    fun drawTitle(
        canvas: Canvas,
        title: String,
        x: Float,
        topY: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_ON_INT
    ) {
        titlePaint.color = color
        titlePaint.textSize = sizePx
        val fm = titlePaint.fontMetrics
        val baseline = topY - fm.ascent
        canvas.drawText(title, x, baseline, titlePaint)
    }

    /**
     * Draws the widget subtitle right below the title.
     */
    fun drawSubtitle(
        canvas: Canvas,
        subtitle: String,
        x: Float,
        topY: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_MID_INT
    ) {
        subPaint.color = color
        subPaint.textSize = sizePx
        val fm = subPaint.fontMetrics
        val baseline = topY - fm.ascent
        canvas.drawText(subtitle, x, baseline, subPaint)
    }

    /**
     * Draws a hero financial metric using Ndot57 for digits and sans for currency sign.
     */
    fun drawHeroMetric(
        canvas: Canvas,
        context: Context?,
        text: String,
        x: Float,
        baselineY: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_ON_INT,
        progress: Float = 1.0f
    ) {
        val (prefix, number) = splitCurrency(text)
        val ndotTf = getNdotTypeface(context)

        metricPaint.typeface = ndotTf
        metricPaint.textSize = sizePx
        metricPaint.color = color

        var currentX = x
        if (prefix.isNotEmpty()) {
            prefixPaint.color = color
            prefixPaint.textSize = sizePx * 0.72f
            canvas.drawText(prefix, currentX, baselineY, prefixPaint)
            currentX += prefixPaint.measureText(prefix) + (sizePx * 0.08f)
        }

        // Draw digits in Ndot57
        val displayText = if (progress >= 1.0f) {
            number
        } else {
            val visibleChars = (number.length * progress.coerceIn(0f, 1f)).toInt().coerceAtLeast(1)
            number.take(visibleChars)
        }
        canvas.drawText(displayText, currentX, baselineY, metricPaint)
    }

    /**
     * Draws a single label (e.g. tag, date, status).
     */
    fun drawLabel(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_MID_INT,
        useNdot: Boolean = false,
        context: Context? = null
    ) {
        labelPaint.color = color
        labelPaint.textSize = sizePx
        labelPaint.typeface = if (useNdot) getNdotTypeface(context) else sansMedium
        canvas.drawText(text, x, baselineY, labelPaint)
    }

    /**
     * Draws a right-aligned label.
     */
    fun drawRightLabel(
        canvas: Canvas,
        text: String,
        rightX: Float,
        baselineY: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_MID_INT,
        useNdot: Boolean = false,
        context: Context? = null
    ) {
        labelPaint.color = color
        labelPaint.textSize = sizePx
        labelPaint.typeface = if (useNdot) getNdotTypeface(context) else sansMedium
        val w = labelPaint.measureText(text)
        canvas.drawText(text, rightX - w, baselineY, labelPaint)
    }

    /**
     * Measures total width of a metric text (prefix + number) at [sizePx].
     */
    fun measureMetricWidth(context: Context?, text: String, sizePx: Float): Float {
        val (prefix, number) = splitCurrency(text)
        var total = 0f
        if (prefix.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.72f
            total += prefixPaint.measureText(prefix) + (sizePx * 0.08f)
        }
        metricPaint.typeface = getNdotTypeface(context)
        metricPaint.textSize = sizePx
        total += metricPaint.measureText(number)
        return total
    }
}
