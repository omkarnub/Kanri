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

    private val titlePaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
            color = PixelTheme.COLOR_DOT_ON_INT
        }
    }

    private val subPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansRegular
            color = PixelTheme.COLOR_DOT_MID_INT
        }
    }

    private val metricPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG)
    }

    private val prefixPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
        }
    }

    private val labelPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = sansMedium
        }
    }

    private val titlePaint: Paint get() = titlePaintLocal.get()!!
    private val subPaint: Paint get() = subPaintLocal.get()!!
    private val metricPaint: Paint get() = metricPaintLocal.get()!!
    private val prefixPaint: Paint get() = prefixPaintLocal.get()!!
    private val labelPaint: Paint get() = labelPaintLocal.get()!!

    private val sortedCurrencySymbols: List<String> by lazy {
        com.omkarnub.kanri.util.AppCurrency.entries
            .map { it.symbol }
            .distinct()
            .sortedByDescending { it.length }
    }

    /**
     * Formats an amount into compact format respecting the active currency:
     * - INR: `₹850`, `₹1.2k`, `₹12.4k`, `₹1.2L`, `₹15L`, `₹1.5Cr`
     * - Other currencies: `$850`, `$1.2k`, `$120k`, `$1.5M`, `$1.2B`
     */
    fun formatIndianCompact(
        amount: Double,
        prefix: String = com.omkarnub.kanri.util.CurrencyUtils.getCurrencySymbol()
    ): String {
        return DotMatrixFont.formatIndianCompact(amount, prefix)
    }

    /**
     * Splits formatted currency text like "-₹12.4k /d" or "CA$500 /d" into
     * prefix ("-₹" / "CA$"), number ("12.4k" / "500"), and unit (" /d").
     */
    fun splitCurrencyAndUnit(text: String): Triple<String, String, String> {
        val trimmed = text.trim()
        val parts = trimmed.split(" ", limit = 2)
        val mainToken = parts[0]
        val unit = if (parts.size > 1) " " + parts[1] else ""

        val hasMinus = mainToken.startsWith("-")
        val hasPlus = !hasMinus && mainToken.startsWith("+")
        val withoutSign = if (hasMinus || hasPlus) mainToken.substring(1) else mainToken
        val signPrefix = if (hasMinus) "-" else if (hasPlus) "+" else ""

        val matchedSymbol = sortedCurrencySymbols.firstOrNull { sym ->
            withoutSign.startsWith(sym) && withoutSign.length > sym.length &&
                (withoutSign[sym.length].isDigit() || withoutSign[sym.length] == '.')
        }

        val prefix: String
        val number: String
        if (matchedSymbol != null) {
            prefix = signPrefix + matchedSymbol
            number = withoutSign.substring(matchedSymbol.length).trim()
        } else if (hasPlus) {
            prefix = "+"
            number = withoutSign.trim()
        } else if (hasMinus) {
            prefix = "-"
            number = withoutSign.trim()
        } else {
            prefix = ""
            number = mainToken.trim()
        }

        return Triple(prefix, number, unit)
    }

    fun splitCurrency(text: String): Pair<String, String> {
        val (prefix, number, unit) = splitCurrencyAndUnit(text)
        return Pair(prefix, number + unit)
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
     * Draws a hero financial metric using Ndot57 for digits and sans for currency sign,
     * optically centering baseline and cap-heights.
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
        val (prefix, number, unit) = splitCurrencyAndUnit(text)
        val ndotTf = getNdotTypeface(context)

        metricPaint.typeface = ndotTf
        metricPaint.textSize = sizePx
        metricPaint.color = color

        var currentX = x

        // Optical baseline alignment for currency sign
        if (prefix.isNotEmpty()) {
            val prefixSize = sizePx * 0.82f
            prefixPaint.color = color
            prefixPaint.textSize = prefixSize
            val fmNdot = metricPaint.fontMetrics
            val fmPrefix = prefixPaint.fontMetrics
            // Align baselines with slight optical lift
            val prefixBaseline = baselineY + (fmPrefix.descent - fmNdot.descent) * 0.3f
            canvas.drawText(prefix, currentX, prefixBaseline, prefixPaint)
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
        currentX += metricPaint.measureText(displayText)

        // Draw unit if present (e.g. " /d", " DAYS") in clean sans or ndot
        if (unit.isNotEmpty()) {
            val unitSize = sizePx * 0.55f
            prefixPaint.color = PixelTheme.COLOR_DOT_MID_INT
            prefixPaint.textSize = unitSize
            canvas.drawText(unit, currentX + (sizePx * 0.05f), baselineY, prefixPaint)
        }
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
     * Draws a right-aligned label. If [useNdot] is true, currency signs are cleanly
     * separated in sans typography while digits use authentic Ndot57.
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
        if (!useNdot) {
            labelPaint.color = color
            labelPaint.textSize = sizePx
            labelPaint.typeface = sansMedium
            val w = labelPaint.measureText(text)
            canvas.drawText(text, rightX - w, baselineY, labelPaint)
            return
        }

        val (prefix, number, unit) = splitCurrencyAndUnit(text)
        val ndotTf = getNdotTypeface(context)

        metricPaint.typeface = ndotTf
        metricPaint.textSize = sizePx
        metricPaint.color = color

        prefixPaint.typeface = sansMedium
        prefixPaint.color = color
        prefixPaint.textSize = sizePx * 0.82f

        var totalW = 0f
        if (prefix.isNotEmpty()) {
            totalW += prefixPaint.measureText(prefix) + (sizePx * 0.06f)
        }
        totalW += metricPaint.measureText(number)
        if (unit.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.7f
            totalW += prefixPaint.measureText(unit)
        }

        var currentX = rightX - totalW
        if (prefix.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.82f
            canvas.drawText(prefix, currentX, baselineY, prefixPaint)
            currentX += prefixPaint.measureText(prefix) + (sizePx * 0.06f)
        }
        canvas.drawText(number, currentX, baselineY, metricPaint)
        currentX += metricPaint.measureText(number)

        if (unit.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.7f
            canvas.drawText(unit, currentX, baselineY, prefixPaint)
        }
    }

    /**
     * Measures total width of a metric text at [sizePx].
     */
    fun measureMetricWidth(context: Context?, text: String, sizePx: Float): Float {
        val (prefix, number, unit) = splitCurrencyAndUnit(text)
        var total = 0f
        if (prefix.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.82f
            total += prefixPaint.measureText(prefix) + (sizePx * 0.08f)
        }
        metricPaint.typeface = getNdotTypeface(context)
        metricPaint.textSize = sizePx
        total += metricPaint.measureText(number)
        if (unit.isNotEmpty()) {
            prefixPaint.textSize = sizePx * 0.55f
            total += prefixPaint.measureText(unit) + (sizePx * 0.05f)
        }
        return total
    }
}
