package com.omkarnub.kanri.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.res.ResourcesCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.util.CurrencyUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * High-performance Canvas renderer that draws rich data visuals across three distinct styles:
 * 1. [WidgetStyle.IPHONE]: Apple HIG Activity Rings with overlapping shadow & Cupertino typography.
 * 2. [WidgetStyle.GLASS_100X]: 100x Apple Liquid Glass with specular highlights & neon ambient bloom.
 * 3. [WidgetStyle.PIXEL_MATRIX]: Louis Studios DotTiles with LED dot bezels & dot-matrix numerals.
 */
object WidgetCanvasRenderer {

    // 7-row dot matrix glyph definition
    private val GLYPHS: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(".XXX.", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."),
        '1' to arrayOf("..X..", ".XX..", "X.X..", "..X..", "..X..", "..X..", "XXXXX"),
        '2' to arrayOf(".XXX.", "X...X", "....X", "..XX.", ".X...", "X....", "XXXXX"),
        '3' to arrayOf("XXXXX", "....X", "...X.", "..XX.", "....X", "X...X", ".XXX."),
        '4' to arrayOf("...X.", "..XX.", ".X.X.", "X..X.", "XXXXX", "...X.", "...X."),
        '5' to arrayOf("XXXXX", "X....", "XXXX.", "....X", "....X", "X...X", ".XXX."),
        '6' to arrayOf("..XX.", ".X...", "X....", "XXXX.", "X...X", "X...X", ".XXX."),
        '7' to arrayOf("XXXXX", "....X", "...X.", "..X..", ".X...", ".X...", ".X..."),
        '8' to arrayOf(".XXX.", "X...X", "X...X", ".XXX.", "X...X", "X...X", ".XXX."),
        '9' to arrayOf(".XXX.", "X...X", "X...X", ".XXXX", "....X", "...X.", ".XX.."),
        ':' to arrayOf(".", ".", "X", ".", "X", ".", "."),
        '.' to arrayOf(".", ".", ".", ".", ".", ".", "X"),
        '-' to arrayOf("...", "...", "...", "XXX", "...", "...", "..."),
        ' ' to arrayOf("...", "...", "...", "...", "...", "...", "..."),
        '%' to arrayOf("X...X", "....X", "...X.", "..X..", ".X...", "X....", "X...X"),
        'k' to arrayOf("X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X"),
        'K' to arrayOf("X...X", "X..X.", "X.X..", "XX...", "X.X..", "X..X.", "X...X"),
        'M' to arrayOf("X...X", "XX.XX", "X.X.X", "X...X", "X...X", "X...X", "X...X"),
        '₹' to arrayOf("XXXXX", "..X..", "XXXXX", "..X..", "..X.X", "..XX.", "..X.X"),
        '$' to arrayOf("..X..", ".XXXX", "X.X..", ".XXX.", "..X.X", "XXXX.", "..X..")
    )

    /**
     * Renders a circular budget ring chart according to the selected [WidgetStyle].
     */
    fun renderBudgetRing(
        context: Context,
        spent: Double,
        budget: Double,
        sizePx: Int = 360,
        style: WidgetStyle = WidgetPreferences.getInstance(context).widgetStyle
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val ratio = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0) else 0.0
        val isOver = spent > budget
        val remaining = (budget - spent).coerceAtLeast(0.0)
        val amountText = CurrencyUtils.formatCompactCurrency(if (isOver) spent - budget else remaining)

        when (style) {
            WidgetStyle.IPHONE -> {
                renderIPhoneBudgetRing(context, canvas, sizePx, spent, budget, ratio, isOver, amountText)
            }
            WidgetStyle.GLASS_100X -> {
                renderGlass100xBudgetRing(context, canvas, sizePx, spent, budget, ratio, isOver, amountText)
            }
            WidgetStyle.PIXEL_MATRIX -> {
                renderPixelBudgetRing(context, canvas, sizePx, spent, budget, ratio, isOver, amountText)
            }
        }

        return bitmap
    }

    /**
     * Renders a circular savings goals ring chart according to the selected [WidgetStyle].
     */
    fun renderGoalRing(
        context: Context,
        progressPercent: Float,
        emoji: String,
        sizePx: Int = 360,
        style: WidgetStyle = WidgetPreferences.getInstance(context).widgetStyle
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (style) {
            WidgetStyle.IPHONE -> {
                renderIPhoneGoalRing(context, canvas, sizePx, progressPercent, emoji)
            }
            WidgetStyle.GLASS_100X -> {
                renderGlass100xGoalRing(context, canvas, sizePx, progressPercent, emoji)
            }
            WidgetStyle.PIXEL_MATRIX -> {
                renderPixelGoalRing(context, canvas, sizePx, progressPercent, emoji)
            }
        }

        return bitmap
    }

    // =========================================================================
    // IPHONE FLUID RENDERERS
    // =========================================================================

    private fun renderIPhoneBudgetRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        spent: Double,
        budget: Double,
        ratio: Double,
        isOver: Boolean,
        amountText: String
    ) {
        val strokeWidth = sizePx * 0.125f
        val padding = strokeWidth / 2f + sizePx * 0.04f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // Apple Activity ring background track (dark translucent)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = if (isOver) 0x33FF453A.toInt() else 0x33FF2D55.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // Cupertino Fitness Coral / Neon Crimson ring
        val sweep = if (isOver) 360f else (ratio * 360.0).toFloat().coerceIn(1f, 360f)
        val ringColor = if (isOver) 0xFFFF453A.toInt() else 0xFFFF2D55.toInt()

        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = ringColor
        }
        canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)

        // Overlapping shadow at progress tip (Apple Activity Ring signature)
        if (sweep > 30f && !isOver) {
            val angleRad = Math.toRadians((sweep - 90.0))
            val radius = (sizePx - padding * 2) / 2f
            val cx = sizePx / 2f + (radius * cos(angleRad)).toFloat()
            val cy = sizePx / 2f + (radius * sin(angleRad)).toFloat()
            val tipShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x55000000
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, strokeWidth / 2f + 2f, tipShadowPaint)
            // Re-draw tip circle in ring color
            val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ringColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, cy, strokeWidth / 2f, tipPaint)
        }

        // Center typography: Apple clean sans
        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val fontMedium = ResourcesCompat.getFont(context, R.font.google_sans_medium)
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontMedium
            color = 0xB3FFFFFF.toInt()
            textSize = sizePx * 0.085f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText(if (isOver) "OVER BUDGET" else "LEFT TO SPEND", centerX, centerY - sizePx * 0.07f, subPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.175f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(amountText, centerX, centerY + sizePx * 0.10f, textPaint)
    }

    private fun renderIPhoneGoalRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        progressPercent: Float,
        emoji: String
    ) {
        val strokeWidth = sizePx * 0.125f
        val padding = strokeWidth / 2f + sizePx * 0.04f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // Apple Mint track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0x3330D158.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // Apple Mint / Cupertino Green progress
        val ratio = (progressPercent / 100f).coerceIn(0f, 1f)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0xFF30D158.toInt()
        }
        if (ratio > 0f) {
            canvas.drawArc(arcRect, -90f, ratio * 360f, false, progressPaint)
        }

        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f

        // Emoji in center
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emoji.ifBlank { "🎯" }, centerX, centerY - sizePx * 0.02f, emojiPaint)

        // Percent
        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.13f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${progressPercent.toInt()}%", centerX, centerY + sizePx * 0.14f, percentPaint)
    }

    // =========================================================================
    // 100X LIQUID GLASS RENDERERS
    // =========================================================================

    private fun renderGlass100xBudgetRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        spent: Double,
        budget: Double,
        ratio: Double,
        isOver: Boolean,
        amountText: String
    ) {
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val strokeWidth = sizePx * 0.11f
        val padding = strokeWidth / 2f + sizePx * 0.05f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // 1. Ambient radial backlight bloom
        val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val bloomColor = if (isOver) 0x33FF3B30.toInt() else 0x3300E5FF.toInt()
            shader = RadialGradient(
                centerX, centerY, sizePx * 0.45f,
                bloomColor, 0x00000000, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, centerY, sizePx * 0.45f, bloomPaint)

        // 2. Frosted glass track with subtle specular rim
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0x2EFFFFFF.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // 3. Neon gradient arc (Cyan to Emerald, or Coral to Amber if over)
        val sweep = if (isOver) 360f else (ratio * 360.0).toFloat().coerceIn(1f, 360f)
        val colors = if (isOver) {
            intArrayOf(0xFFFF3B30.toInt(), 0xFFFF9500.toInt())
        } else {
            intArrayOf(0xFF00E5FF.toInt(), 0xFF00E676.toInt())
        }
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawArc(arcRect, -90f, sweep, false, progressPaint)

        // 4. Specular tip highlight
        val angleRad = Math.toRadians((sweep - 90.0))
        val radius = (sizePx - padding * 2) / 2f
        val tipX = centerX + (radius * cos(angleRad)).toFloat()
        val tipY = centerY + (radius * sin(angleRad)).toFloat()
        val tipGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(tipX, tipY, strokeWidth * 0.28f, tipGlowPaint)

        // 5. Typography
        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val fontMedium = ResourcesCompat.getFont(context, R.font.google_sans_medium)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontMedium
            color = 0xCCFFFFFF.toInt()
            textSize = sizePx * 0.088f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.06f
        }
        canvas.drawText(if (isOver) "OVER LIMIT" else "SAFE TO SPEND", centerX, centerY - sizePx * 0.08f, subPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.175f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(amountText, centerX, centerY + sizePx * 0.10f, textPaint)
    }

    private fun renderGlass100xGoalRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        progressPercent: Float,
        emoji: String
    ) {
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val strokeWidth = sizePx * 0.11f
        val padding = strokeWidth / 2f + sizePx * 0.05f
        val arcRect = RectF(padding, padding, sizePx - padding, sizePx - padding)

        // Ambient Violet/Cyan bloom
        val bloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                centerX, centerY, sizePx * 0.45f,
                0x33BF5AF2.toInt(), 0x00000000, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, centerY, sizePx * 0.45f, bloomPaint)

        // Frosted track
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = 0x2EFFFFFF.toInt()
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // Electric Violet to Cyan progress arc
        val ratio = (progressPercent / 100f).coerceIn(0f, 1f)
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(
                0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                intArrayOf(0xFFBF5AF2.toInt(), 0xFF0A84FF.toInt()), null, Shader.TileMode.CLAMP
            )
        }
        if (ratio > 0f) {
            canvas.drawArc(arcRect, -90f, ratio * 360f, false, progressPaint)
        }

        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emoji.ifBlank { "🌟" }, centerX, centerY - sizePx * 0.02f, emojiPaint)

        val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = sizePx * 0.14f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${progressPercent.toInt()}%", centerX, centerY + sizePx * 0.14f, percentPaint)
    }

    // =========================================================================
    // PIXEL MATRIX (DOTTILES) RENDERERS
    // =========================================================================

    private fun renderPixelBudgetRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        spent: Double,
        budget: Double,
        ratio: Double,
        isOver: Boolean,
        amountText: String
    ) {
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val ringRadius = sizePx * 0.41f
        val totalDots = 36
        val litDots = if (isOver) totalDots else (ratio * totalDots).toInt().coerceIn(1, totalDots)
        val dotRadius = sizePx * 0.022f

        // Draw 36 discrete circular LED bezel dots
        for (i in 0 until totalDots) {
            val angleDeg = -90.0 + (i.toDouble() / totalDots) * 360.0
            val angleRad = Math.toRadians(angleDeg)
            val dx = centerX + (ringRadius * cos(angleRad)).toFloat()
            val dy = centerY + (ringRadius * sin(angleRad)).toFloat()

            val isLit = i < litDots
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLit) {
                    if (isOver) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
                } else {
                    0x26FFFFFF.toInt()
                }
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dx, dy, if (isLit) dotRadius * 1.15f else dotRadius * 0.85f, dotPaint)
        }

        // Draw Dot-Matrix Subtitle
        val subText = if (isOver) "OVER" else "LEFT"
        drawDotMatrixText(
            canvas = canvas,
            text = subText,
            cx = centerX,
            cy = centerY - sizePx * 0.14f,
            dotRadius = sizePx * 0.007f,
            dotPitch = sizePx * 0.018f,
            color = 0x88FFFFFF.toInt()
        )

        // Draw Dot-Matrix Amount Value
        val cleanAmount = amountText.replace("₹", "").replace("$", "").trim()
        drawDotMatrixText(
            canvas = canvas,
            text = cleanAmount,
            cx = centerX,
            cy = centerY + sizePx * 0.04f,
            dotRadius = sizePx * 0.011f,
            dotPitch = sizePx * 0.027f,
            color = if (isOver) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
        )
    }

    private fun renderPixelGoalRing(
        context: Context,
        canvas: Canvas,
        sizePx: Int,
        progressPercent: Float,
        emoji: String
    ) {
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val ringRadius = sizePx * 0.41f
        val totalDots = 36
        val ratio = (progressPercent / 100f).coerceIn(0f, 1f)
        val litDots = (ratio * totalDots).toInt().coerceIn(1, totalDots)
        val dotRadius = sizePx * 0.022f

        for (i in 0 until totalDots) {
            val angleDeg = -90.0 + (i.toDouble() / totalDots) * 360.0
            val angleRad = Math.toRadians(angleDeg)
            val dx = centerX + (ringRadius * cos(angleRad)).toFloat()
            val dy = centerY + (ringRadius * sin(angleRad)).toFloat()

            val isLit = i < litDots
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isLit) 0xFF50E3C2.toInt() else 0x26FFFFFF.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(dx, dy, if (isLit) dotRadius * 1.15f else dotRadius * 0.85f, dotPaint)
        }

        // Emoji
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = sizePx * 0.16f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(emoji.ifBlank { "🎯" }, centerX, centerY - sizePx * 0.05f, emojiPaint)

        // Percentage in dot matrix
        drawDotMatrixText(
            canvas = canvas,
            text = "${progressPercent.toInt()}%",
            cx = centerX,
            cy = centerY + sizePx * 0.12f,
            dotRadius = sizePx * 0.010f,
            dotPitch = sizePx * 0.025f,
            color = 0xFFFFFFFF.toInt()
        )
    }

    /**
     * Draws dot matrix 7-row characters centered at (cx, cy) on an Android Canvas.
     */
    fun drawDotMatrixText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        dotRadius: Float,
        dotPitch: Float,
        color: Int
    ) {
        if (text.isBlank()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        // Calculate total columns width
        var totalCols = 0
        for (i in text.indices) {
            val glyph = GLYPHS[text[i]] ?: GLYPHS[' ']!!
            totalCols += glyph[0].length
            if (i < text.length - 1) totalCols += 1 // 1 column spacer
        }

        val totalWidth = totalCols * dotPitch
        val totalHeight = 7 * dotPitch
        val startX = cx - totalWidth / 2f + dotPitch / 2f
        val startY = cy - totalHeight / 2f + dotPitch / 2f

        var colOffset = 0
        for (i in text.indices) {
            val glyph = GLYPHS[text[i]] ?: GLYPHS[' ']!!
            val glyphW = glyph[0].length
            for (r in 0 until 7) {
                val rowStr = glyph[r]
                for (c in 0 until glyphW) {
                    if (c < rowStr.length && rowStr[c] == 'X') {
                        val px = startX + (colOffset + c) * dotPitch
                        val py = startY + r * dotPitch
                        canvas.drawCircle(px, py, dotRadius, paint)
                    }
                }
            }
            colOffset += glyphW + 1
        }
    }

    /**
     * Renders the complete Financial Sky Weather scene & 7-Day Spend Pulse for RemoteViews.
     */
    fun renderFinancialWeather(
        context: Context,
        todaySpent: Double,
        monthBudget: Double,
        monthSpent: Double,
        safeToSpendDaily: Double,
        weekSpends: List<Pair<String, Double>>,
        widthPx: Int = 600,
        heightPx: Int = 360,
        widgetStyle: WidgetStyle = WidgetPreferences.getInstance(context).widgetStyle
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY) + cal.get(java.util.Calendar.MINUTE) / 60f
        val isDay = hour in 7.5f..19.5f
        val fontBold = ResourcesCompat.getFont(context, R.font.google_sans_bold)
        val fontMedium = ResourcesCompat.getFont(context, R.font.google_sans_medium)

        // 1. Sky Well (top 55% of height)
        val skyHeight = heightPx * 0.52f
        val skyRect = RectF(10f, 10f, widthPx - 10f, skyHeight)

        val skyBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (widgetStyle) {
                WidgetStyle.PIXEL_MATRIX -> 0xFF050608.toInt()
                WidgetStyle.GLASS_100X -> 0x22FFFFFF.toInt()
                WidgetStyle.IPHONE -> 0x401C1C1E.toInt()
            }
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(skyRect, 18f, 18f, skyBgPaint)

        // Celestial Body (Sun or Moon)
        val td = ((hour - 7.5f) / 12f).coerceIn(0f, 1f)
        val sunX = 30f + td * (widthPx - 80f)
        val sunY = skyHeight * 0.40f - (sin(PI.toFloat() * td) * (skyHeight * 0.22f))

        if (isDay) {
            val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (widgetStyle) {
                    WidgetStyle.GLASS_100X -> 0xFFFFD54F.toInt()
                    WidgetStyle.IPHONE -> 0xFFFF9500.toInt()
                    WidgetStyle.PIXEL_MATRIX -> 0xFFFFFFFF.toInt()
                }
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sunX, sunY, 18f, sunPaint)

            // Sun rays
            val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x66FFD54F.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }
            canvas.drawCircle(sunX, sunY, 26f, rayPaint)
        } else {
            // Moon disc
            val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFFFFFFF.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawCircle(sunX, sunY, 15f, moonPaint)

            // Twinkling stars
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xB3FFFFFF.toInt()
                style = Paint.Style.FILL
            }
            val starOffsets = listOf(
                Pair(50f, 25f), Pair(120f, 40f), Pair(220f, 20f),
                Pair(380f, 35f), Pair(480f, 25f), Pair(540f, 45f)
            )
            for (st in starOffsets) {
                canvas.drawCircle(st.first, st.second, 2.5f, starPaint)
            }
        }

        // Horizon Hills
        val hillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when (widgetStyle) {
                WidgetStyle.PIXEL_MATRIX -> 0xFF1C252C.toInt()
                WidgetStyle.GLASS_100X -> 0x4400E5FF.toInt()
                WidgetStyle.IPHONE -> 0x33FFFFFF.toInt()
            }
            style = Paint.Style.FILL
        }
        val hillPath = android.graphics.Path().apply {
            moveTo(10f, skyHeight)
            lineTo(10f, skyHeight - 16f)
            quadTo(widthPx * 0.35f, skyHeight - 34f, widthPx * 0.65f, skyHeight - 14f)
            quadTo(widthPx * 0.85f, skyHeight - 28f, widthPx - 10f, skyHeight - 10f)
            lineTo(widthPx - 10f, skyHeight)
            close()
        }
        canvas.drawPath(hillPath, hillPaint)

        // Sky Text Overlay (Today's spend & condition)
        val skyTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = 22f
        }
        val conditionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontMedium
            color = if (todaySpent > safeToSpendDaily * 1.5) 0xFFFF5252.toInt() else 0xFF00E676.toInt()
            textSize = 15f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Today: " + CurrencyUtils.formatCurrency(todaySpent), 24f, skyHeight - 10f, skyTextPaint)
        val cond = if (todaySpent > safeToSpendDaily * 1.5) "Stormy Spends" else if (isDay) "Clear Horizons" else "Starry Calm"
        canvas.drawText(cond, widthPx - 24f, skyHeight - 10f, conditionPaint)

        // 2. 7-Day Spending Pulse (bottom 45% of height)
        val weekStartY = skyHeight + 22f
        val pulseDays = if (weekSpends.isNotEmpty()) weekSpends.take(5) else listOf(
            Pair("Mon", 450.0), Pair("Tue", 1200.0), Pair("Wed", 850.0), Pair("Thu", 1600.0), Pair("Today", todaySpent)
        )
        val maxSpend = max(safeToSpendDaily * 2.5, 3000.0)
        val rowHeight = (heightPx - weekStartY - 6f) / pulseDays.size

        val dayLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontMedium
            color = 0x99FFFFFF.toInt()
            textSize = 13f
        }
        val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = fontBold
            color = 0xFFFFFFFF.toInt()
            textSize = 13f
            textAlign = Paint.Align.RIGHT
        }

        pulseDays.forEachIndexed { i, dayRecord ->
            val rowY = weekStartY + i * rowHeight + rowHeight * 0.65f
            canvas.drawText(dayRecord.first, 24f, rowY, dayLabelPaint)

            // Dot scale bar (16 discrete dots)
            val barStartX = 75f
            val barEndX = widthPx - 95f
            val barW = barEndX - barStartX
            val totalDots = 16
            val ratio = (dayRecord.second / maxSpend).coerceIn(0.0, 1.0).toFloat()
            val litDots = (ratio * totalDots).toInt().coerceIn(1, totalDots)
            val isOver = dayRecord.second > safeToSpendDaily

            for (d in 0 until totalDots) {
                val dx = barStartX + d * (barW / totalDots) + 3f
                val isLit = d < litDots
                val dotColor = if (isLit) {
                    if (isOver) 0xFFFF5252.toInt() else 0xFF00E676.toInt()
                } else {
                    0x20FFFFFF.toInt()
                }
                val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = dotColor
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(dx, rowY - 4f, if (isLit) 3.2f else 2.2f, dotP)
            }

            canvas.drawText(CurrencyUtils.formatCompactCurrency(dayRecord.second), widthPx - 24f, rowY, amountPaint)
        }

        return bitmap
    }
}
