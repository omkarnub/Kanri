package com.omkarnub.kanri.widget.visuals

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.omkarnub.kanri.widget.core.PixelTheme
import com.omkarnub.kanri.widget.font.PixelFont
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * Shared Pixel DotTiles visuals library.
 * Reusable Canvas primitives: Slider, Steps, Dots, Blocks, Rise, Grid, Arc, and Vector Icons.
 * Every visual fully supports smooth progress entry animations (0.0f -> 1.0f).
 */
object DotVisuals {

    private val fillPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    private val strokePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    enum class SliderMode { GLOW, FILL }

    enum class DotIcon {
        SPARK, FLAME, CLOCK, PULSE, BOLT, BATTERY, DROP, MOON, SUN, HEART, PLUS, INBOX, SPLIT
    }

    // Official Louis Studios SVG icon path data (viewBox 0 0 24 24)
    private val SVG_PATHS: Map<DotIcon, String> = mapOf(
        DotIcon.SPARK to "M12 1.2c.9 5.9 4.9 9.9 10.8 10.8-5.9.9-9.9 4.9-10.8 10.8C11.1 16.9 7.1 12.9 1.2 12 7.1 11.1 11.1 7.1 12 1.2Z",
        DotIcon.FLAME to "M12.6 1.4c.6 3.8 6.6 6.6 6.6 12.4a7.2 7.2 0 0 1-14.4 0c0-3.2 1.8-5.2 3.6-6.6 0 2.2.8 3.6 2.2 4.2.2-4.2 1.6-7.2 2-10Z",
        DotIcon.BOLT to "M15.2 1 4.6 13.6h6.2L8.9 23 19.6 10.2h-6.3L15.2 1Z",
        DotIcon.DROP to "M12 1.8s7.2 8 7.2 13a7.2 7.2 0 0 1-14.4 0c0-5 7.2-13 7.2-13Z",
        DotIcon.MOON to "M12.7 2.23A9.8 9.8 0 1 0 21.8 11.71 6.6 6.6 0 0 1 12.7 2.23Z",
        DotIcon.HEART to "M12 21.2C9.4 19.1 1.8 14 1.8 8.3 1.8 5.1 4.2 2.8 7 2.8c2.1 0 3.8 1.1 5 2.9 1.2-1.8 2.9-2.9 5-2.9 2.8 0 5.2 2.3 5.2 5.5 0 5.7-7.6 10.8-10.2 12.9Z",
        DotIcon.BATTERY to "M7.2 5.4h9.6a5.2 5.2 0 0 1 5.2 5.2v2.8a5.2 5.2 0 0 1-5.2 5.2H7.2A5.2 5.2 0 0 1 2 13.4v-2.8a5.2 5.2 0 0 1 5.2-5.2Zm-.6 3.5a1 1 0 0 0-1 1v4.2a1 1 0 0 0 1 1h3.6a1 1 0 0 0 1-1V9.9a1 1 0 0 0-1-1H6.6Z",
        DotIcon.PLUS to "M11 5v6H5v2h6v6h2v-6h6v-2h-6V5z",
        DotIcon.INBOX to "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 10h-4c0 1.66-1.34 3-3 3s-3-1.34-3-3H5V5h14v8z",
        DotIcon.SPLIT to "M6 4a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm12 12a2 2 0 1 0 0 4 2 2 0 0 0 0-4zm-6-5a2 2 0 1 0 0 4 2 2 0 0 0 0-4z"
    )

    private val cachedPaths: MutableMap<DotIcon, Path?> = mutableMapOf()

    private fun getPath(icon: DotIcon): Path? {
        if (cachedPaths.containsKey(icon)) return cachedPaths[icon]
        val pathData = SVG_PATHS[icon] ?: return null
        val path = try {
            PathParser.createPathFromPathData(pathData)
        } catch (_: Throwable) {
            null
        }
        cachedPaths[icon] = path
        return path
    }

    /**
     * Draws an icon at ([x], [y]) with size [sizePx].
     */
    fun drawIcon(
        canvas: Canvas,
        icon: DotIcon,
        x: Float,
        y: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_ON_INT
    ) {
        val path = getPath(icon)
        if (path != null) {
            val matrix = Matrix()
            val scale = sizePx / 24f
            matrix.setScale(scale, scale)
            matrix.postTranslate(x, y)
            val transformed = Path()
            path.transform(matrix, transformed)
            fillPaint.color = color
            canvas.drawPath(transformed, fillPaint)
        } else {
            if (icon == DotIcon.CLOCK) {
                val cx = x + sizePx / 2f
                val cy = y + sizePx / 2f
                strokePaint.color = color
                strokePaint.strokeWidth = sizePx * 0.1f
                canvas.drawCircle(cx, cy, sizePx * 0.42f, strokePaint)
                canvas.drawLine(cx, cy, cx, cy - sizePx * 0.28f, strokePaint)
                canvas.drawLine(cx, cy, cx + sizePx * 0.22f, cy, strokePaint)
            } else if (icon == DotIcon.PULSE) {
                strokePaint.color = color
                strokePaint.strokeWidth = sizePx * 0.1f
                val cy = y + sizePx / 2f
                val p = Path()
                p.moveTo(x, cy)
                p.lineTo(x + sizePx * 0.25f, cy)
                p.lineTo(x + sizePx * 0.4f, y + sizePx * 0.15f)
                p.lineTo(x + sizePx * 0.6f, y + sizePx * 0.85f)
                p.lineTo(x + sizePx * 0.75f, cy)
                p.lineTo(x + sizePx, cy)
                canvas.drawPath(p, strokePaint)
            } else {
                fillPaint.color = color
                canvas.drawCircle(x + sizePx / 2f, y + sizePx / 2f, sizePx * 0.35f, fillPaint)
            }
        }
    }

    /**
     * Slider visual: row of fine vertical ticks with an animated marker block.
     * Animates marker position and tick fill progress fluidly from 0 to [value].
     */
    fun drawSlider(
        canvas: Canvas,
        bounds: RectF,
        value: Float, // 0..1
        mode: SliderMode = SliderMode.GLOW,
        ticks: Int = 44,
        markerColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        trackColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        pacingMarker: Float? = null,
        labels: Pair<String, String>? = null,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val animVal = (value.coerceIn(0f, 1f) * progress.coerceIn(0f, 1f))
        val tickWidth = (1.5f * density).coerceAtLeast(1.8f)
        val tickHeight = bounds.height() * 0.55f
        val spacing = (bounds.width() - tickWidth) / (ticks - 1).coerceAtLeast(1)
        val markerIndex = (animVal * (ticks - 1)).toInt()

        val markerWidth = (6.5f * density).coerceAtLeast(tickWidth * 2f)
        val markerHeight = bounds.height() * 0.90f

        for (i in 0 until ticks) {
            val tx = bounds.left + i * spacing
            val dist = abs(i - markerIndex)

            val paintColor = when {
                dist == 0 -> markerColor
                mode == SliderMode.FILL && i < markerIndex -> markerColor
                mode == SliderMode.GLOW && dist <= 4 -> {
                    val alpha = ((1f - dist / 5f) * 255).toInt().coerceIn(35, 255)
                    (alpha shl 24) or (markerColor and 0x00FFFFFF)
                }
                else -> trackColor
            }

            fillPaint.color = paintColor
            if (dist == 0) {
                val mx = tx - (markerWidth - tickWidth) / 2f
                val my = bounds.centerY() - markerHeight / 2f
                canvas.drawRoundRect(mx, my, mx + markerWidth, my + markerHeight, 2.5f * density, 2.5f * density, fillPaint)
            } else {
                val top = bounds.centerY() - tickHeight / 2f
                canvas.drawRoundRect(tx, top, tx + tickWidth, top + tickHeight, 1.2f * density, 1.2f * density, fillPaint)
            }
        }

        // Draw expected pacing marker if present
        if (pacingMarker != null) {
            val px = bounds.left + pacingMarker.coerceIn(0f, 1f) * (bounds.width() - tickWidth)
            fillPaint.color = PixelTheme.COLOR_CYAN_INT
            canvas.drawCircle(px + tickWidth / 2f, bounds.top - 3f * density, 2f * density, fillPaint)
        }

        // Left and right small captions
        if (labels != null) {
            val labelY = bounds.bottom + 9f * density
            val labelSize = 9f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Stepped histogram bars built of dot columns.
     * Animates column heights fluidly upward with [progress].
     */
    fun drawSteps(
        canvas: Canvas,
        bounds: RectF,
        data: FloatArray, // values 0..1
        maxDotsPerBar: Int = 8,
        activeColor: Int = PixelTheme.COLOR_MINT_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        labels: Pair<String, String>? = null,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        if (data.isEmpty()) return
        val count = data.size
        val dotPitch = (bounds.height() / maxDotsPerBar).coerceAtLeast(2.5f * density)
        val dotRadius = dotPitch * 0.35f
        val colSpacing = bounds.width() / count.coerceAtLeast(1)

        // Faint slot guide lines behind chart
        strokePaint.color = 0x14FFFFFF
        strokePaint.strokeWidth = 1f
        for (i in 0 until count) {
            val cx = bounds.left + i * colSpacing + colSpacing / 2f
            canvas.drawLine(cx, bounds.top, cx, bounds.bottom, strokePaint)
        }

        val animProgress = progress.coerceIn(0f, 1f)
        for (i in 0 until count) {
            val cx = bounds.left + i * colSpacing + colSpacing / 2f
            val barVal = data[i].coerceIn(0f, 1f) * animProgress
            val litDots = (barVal * maxDotsPerBar).toInt()

            for (r in 0 until maxDotsPerBar) {
                val cy = bounds.bottom - (r * dotPitch + dotRadius)
                val isLit = r < litDots
                fillPaint.color = if (isLit) activeColor else dimColor
                canvas.drawCircle(cx, cy, dotRadius, fillPaint)
            }
        }

        if (labels != null) {
            val labelY = bounds.bottom + 9f * density
            val labelSize = 9f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Dotted timeline across slots (e.g. 24 hours).
     * Animates dots revealing and growing smoothly across slots with [progress].
     */
    fun drawDots(
        canvas: Canvas,
        bounds: RectF,
        data: List<Float?>, // null for idle slot, 0..1 for spend amount
        activeSlotIndex: Int = -1,
        dotColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        labels: Pair<String, String>? = null,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val slots = data.size.coerceAtLeast(1)
        val slotWidth = bounds.width() / slots
        val cy = bounds.centerY()
        val animProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until slots) {
            val cx = bounds.left + i * slotWidth + slotWidth / 2f
            val value = data[i]

            // Sweep animation across the 24 hours
            val slotThreshold = i.toFloat() / slots
            val slotReveal = ((animProgress - slotThreshold) * slots).coerceIn(0f, 1f)

            if (value != null && value > 0f && slotReveal > 0f) {
                val r = (1.5f + (value.coerceIn(0f, 1f) * 3.2f)) * density * slotReveal
                fillPaint.color = dotColor
                canvas.drawCircle(cx, cy, r, fillPaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(cx, cy, 1.1f * density, fillPaint)
            }

            if (i == activeSlotIndex && animProgress > 0.4f) {
                strokePaint.color = PixelTheme.COLOR_CYAN_INT
                strokePaint.strokeWidth = 1.3f * density
                val ringScale = ((animProgress - 0.4f) / 0.6f).coerceIn(0f, 1f)
                canvas.drawCircle(cx, cy, 5f * density * ringScale, strokePaint)
            }
        }

        if (labels != null) {
            val labelY = bounds.bottom + 9f * density
            val labelSize = 9f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Sparse solid session blocks (e.g. for Review Queue).
     * Animates blocks filling in sequentially with [progress].
     */
    fun drawBlocks(
        canvas: Canvas,
        bounds: RectF,
        count: Int,
        maxSlots: Int = 12,
        activeColor: Int = PixelTheme.COLOR_AMBER_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val total = maxSlots.coerceAtLeast(1)
        val gap = 3f * density
        val blockWidth = ((bounds.width() - (total - 1) * gap) / total).coerceAtLeast(2f)
        val visibleCount = (count * progress.coerceIn(0f, 1f)).toInt()

        for (i in 0 until total) {
            val bx = bounds.left + i * (blockWidth + gap)
            val isFilled = i < visibleCount
            if (isFilled) {
                val fade = (1f - (i.toFloat() / total) * 0.4f)
                val alpha = (fade * 255).toInt().coerceIn(100, 255)
                fillPaint.color = (alpha shl 24) or (activeColor and 0x00FFFFFF)
            } else {
                fillPaint.color = dimColor
            }
            canvas.drawRoundRect(bx, bounds.top, bx + blockWidth, bounds.bottom, 2f * density, 2f * density, fillPaint)
        }
    }

    /**
     * 4-tier to 6-tier stepped gauge / progress rise.
     * Animates tiers rising smoothly with [progress].
     */
    fun drawRise(
        canvas: Canvas,
        bounds: RectF,
        ratio: Float, // 0..1
        tiers: Int = 4,
        activeColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val gap = 4f * density
        val tierWidth = (bounds.width() - (tiers - 1) * gap) / tiers
        val animRatio = ratio.coerceIn(0f, 1f) * progress.coerceIn(0f, 1f)
        val activeTiers = (animRatio * tiers).toInt()

        for (t in 0 until tiers) {
            val tx = bounds.left + t * (tierWidth + gap)
            val isLit = t < activeTiers || (t == 0 && animRatio > 0.05f)
            val h = bounds.height() * ((t + 1).toFloat() / tiers)
            val top = bounds.bottom - h

            fillPaint.color = if (isLit) activeColor else dimColor
            canvas.drawRoundRect(tx, top, tx + tierWidth, bounds.bottom, 2f * density, 2f * density, fillPaint)
        }
    }

    /**
     * Calendar dot grid with day-of-week headers and per-cell intensity.
     * Animates intensity illuminating with [progress].
     */
    fun drawGrid(
        canvas: Canvas,
        bounds: RectF,
        cols: Int = 7,
        rows: Int = 5,
        intensities: FloatArray, // 0..1 per cell
        todayIndex: Int = -1,
        onColor: Int = PixelTheme.COLOR_MINT_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val colWidth = bounds.width() / cols
        val rowHeight = bounds.height() / (rows + 1) // +1 for header row

        // Day of week headers: S M T W T F S
        val dayHeaders = arrayOf("S", "M", "T", "W", "T", "F", "S")
        val headerY = bounds.top + rowHeight * 0.7f
        for (c in 0 until 7) {
            val hx = bounds.left + c * colWidth + colWidth / 2f
            PixelFont.drawLabel(
                canvas, dayHeaders[c],
                hx - 3f * density, headerY,
                8.5f * density, PixelTheme.COLOR_DOT_MID_INT
            )
        }

        val dotRadius = (colWidth * 0.30f).coerceAtMost(rowHeight * 0.30f)
        val animProgress = progress.coerceIn(0f, 1f)

        for (r in 0 until rows) {
            val cy = bounds.top + (r + 1.5f) * rowHeight
            for (c in 0 until cols) {
                val index = r * cols + c
                if (index >= intensities.size) break

                val cx = bounds.left + c * colWidth + colWidth / 2f
                val intensity = intensities[index].coerceIn(0f, 1f) * animProgress

                if (intensity > 0f) {
                    val alpha = (70 + intensity * 185).toInt().coerceIn(70, 255)
                    fillPaint.color = (alpha shl 24) or (onColor and 0x00FFFFFF)
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                } else {
                    fillPaint.color = dimColor
                    canvas.drawCircle(cx, cy, dotRadius * 0.65f, fillPaint)
                }

                if (index == todayIndex && animProgress > 0.5f) {
                    strokePaint.color = PixelTheme.COLOR_CYAN_INT
                    strokePaint.strokeWidth = 1.3f * density
                    canvas.drawCircle(cx, cy, dotRadius + 2f * density, strokePaint)
                }
            }
        }
    }

    /**
     * Parabolic celestial arc inside [bounds] (Weather / Sky widget).
     * Uses strict parabolic formula x(t), y(t) to guarantee the celestial body
     * stays 100% inside [bounds].
     */
    fun drawArc(
        canvas: Canvas,
        bounds: RectF,
        progress: Float, // 0..1 position of celestial body
        dotCount: Int = 20,
        onColor: Int = PixelTheme.COLOR_AMBER_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f
    ) {
        val animProgress = progress.coerceIn(0f, 1f)
        val markerIndex = (animProgress * (dotCount - 1)).toInt()

        val padX = 14f * density
        val arcWidth = bounds.width() - 2 * padX
        val baseHorizonY = bounds.bottom - 4f * density
        val peakHeight = bounds.height() * 0.70f

        for (i in 0 until dotCount) {
            val t = i.toFloat() / (dotCount - 1)
            val x = bounds.left + padX + t * arcWidth
            val y = baseHorizonY - sin(PI * t).toFloat() * peakHeight

            if (i == markerIndex) {
                fillPaint.color = onColor
                canvas.drawCircle(x, y, 3.8f * density, fillPaint)
                strokePaint.color = onColor
                strokePaint.strokeWidth = 1.2f * density
                canvas.drawCircle(x, y, 6.2f * density, strokePaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(x, y, 1.2f * density, fillPaint)
            }
        }
    }

    enum class PixelGlyph {
        EXPENSE, INCOME, SPLIT, TRANSFER, FLAME_LIT, FLAME_DIM
    }

    private val GLYPH_MATRICES = mapOf(
        PixelGlyph.EXPENSE to arrayOf(
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0)
        ),
        PixelGlyph.INCOME to arrayOf(
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0)
        ),
        PixelGlyph.SPLIT to arrayOf(
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0)
        ),
        PixelGlyph.TRANSFER to arrayOf(
            intArrayOf(0, 0, 1, 0, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 1, 0),
            intArrayOf(1, 0, 1, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 1, 0, 1),
            intArrayOf(0, 1, 1, 1, 1, 1, 0),
            intArrayOf(0, 0, 0, 0, 1, 0, 0)
        ),
        PixelGlyph.FLAME_LIT to arrayOf(
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 1, 1, 0, 0, 0),
            intArrayOf(0, 1, 1, 1, 1, 0, 0),
            intArrayOf(0, 1, 1, 0, 1, 1, 0),
            intArrayOf(1, 1, 1, 0, 1, 1, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1),
            intArrayOf(0, 1, 1, 1, 1, 1, 0)
        ),
        PixelGlyph.FLAME_DIM to arrayOf(
            intArrayOf(0, 0, 0, 1, 0, 0, 0),
            intArrayOf(0, 0, 1, 1, 0, 0, 0),
            intArrayOf(0, 1, 0, 0, 1, 0, 0),
            intArrayOf(0, 1, 0, 0, 0, 1, 0),
            intArrayOf(1, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 0, 0, 0, 1, 1),
            intArrayOf(0, 1, 1, 1, 1, 1, 0)
        )
    )

    fun drawPixelGlyph(
        canvas: Canvas,
        glyph: PixelGlyph,
        cx: Float,
        cy: Float,
        sizePx: Float,
        color: Int = PixelTheme.COLOR_DOT_ON_INT
    ) {
        val matrix = GLYPH_MATRICES[glyph] ?: return
        val rows = matrix.size
        val cols = matrix[0].size
        val dotPitch = sizePx / cols
        val dotRadius = dotPitch * 0.42f
        val startX = cx - (sizePx / 2f) + dotPitch / 2f
        val startY = cy - (sizePx / 2f) + dotPitch / 2f

        fillPaint.color = color
        for (r in 0 until rows) {
            val y = startY + r * dotPitch
            for (c in 0 until cols) {
                if (matrix[r][c] == 1) {
                    val x = startX + c * dotPitch
                    canvas.drawCircle(x, y, dotRadius, fillPaint)
                }
            }
        }
    }

    /**
     * Dual Progress Bar for Budget:
     * - Top Bar: Used budget amount with fill progress (0..1)
     * - Bottom Bar: Total budget reference scale with days/remaining indicator
     */
    fun drawDualProgressBar(
        canvas: Canvas,
        bounds: RectF,
        usedRatio: Float,
        totalBudget: Double,
        spentAmount: Double,
        remainingAmount: Double,
        expectedRatio: Float,
        usedColor: Int = PixelTheme.COLOR_MINT_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val animP = progress.coerceIn(0f, 1f)
        val animUsedRatio = (usedRatio * animP).coerceIn(0f, 1f)
        val animSpent = spentAmount * animP
        val animRemaining = remainingAmount * animP

        val labelSize = (9.5f * density).coerceIn(8.5f, 11f * density)
        val barH = (5.5f * density).coerceIn(4f, 7f * density)
        val gap = (8f * density).coerceIn(6f, 12f * density)

        // Upper section: Used Progress
        val topLabelY = bounds.top + labelSize
        val pctUsed = (animUsedRatio * 100).toInt()
        val usedLabel = "USED " + PixelFont.formatIndianCompact(animSpent) + " (" + pctUsed + "%)"
        PixelFont.drawLabel(canvas, usedLabel, bounds.left, topLabelY, labelSize, usedColor)

        val bar1Y = topLabelY + 4f * density
        val bar1Rect = RectF(bounds.left, bar1Y, bounds.right, bar1Y + barH)
        fillPaint.color = PixelTheme.COLOR_DOT_DIM_INT
        canvas.drawRoundRect(bar1Rect, 2f * density, 2f * density, fillPaint)

        if (animUsedRatio > 0.01f) {
            val fillW = (bounds.width() * animUsedRatio).coerceAtLeast(3f * density)
            fillPaint.color = usedColor
            canvas.drawRoundRect(bar1Rect.left, bar1Rect.top, bar1Rect.left + fillW, bar1Rect.bottom, 2f * density, 2f * density, fillPaint)
        }

        // Lower section: Total Budget Reference Scale
        val totalLabelY = bar1Rect.bottom + gap + labelSize
        val totalLabel = "TOTAL " + PixelFont.formatIndianCompact(totalBudget)
        val remLabel = PixelFont.formatIndianCompact(animRemaining) + " LEFT"
        PixelFont.drawLabel(canvas, totalLabel, bounds.left, totalLabelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        PixelFont.drawRightLabel(canvas, remLabel, bounds.right, totalLabelY, labelSize, if (remainingAmount < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT)

        val bar2Y = totalLabelY + 4f * density
        val bar2Rect = RectF(bounds.left, bar2Y, bounds.right, bar2Y + barH)
        val segCount = 20
        val segSpacing = bounds.width() / segCount
        val segW = segSpacing * 0.65f
        for (i in 0 until segCount) {
            val sx = bounds.left + i * segSpacing
            fillPaint.color = if (i.toFloat() / segCount <= animUsedRatio) usedColor else PixelTheme.COLOR_DOT_DIM_INT
            canvas.drawRoundRect(sx, bar2Rect.top, sx + segW, bar2Rect.bottom, 1.2f * density, 1.2f * density, fillPaint)
        }

        // Expected pacing marker
        val markerX = bounds.left + expectedRatio.coerceIn(0f, 1f) * bounds.width()
        fillPaint.color = PixelTheme.COLOR_CYAN_INT
        canvas.drawCircle(markerX, bar2Rect.centerY(), 2.8f * density, fillPaint)
    }

    /**
     * Today Spend Pacing Indicator:
     * Replaces ambiguous 24-dot line with a crystal-clear Pacing Progress Bar
     * comparing Spent vs Daily Limit, plus 4-session checkpoint breakdown.
     */
    fun drawTodayPacing(
        canvas: Canvas,
        bounds: RectF,
        spentToday: Double,
        dailyLimit: Double,
        spentPercent: Float,
        morningSpend: Double,
        afternoonSpend: Double,
        eveningSpend: Double,
        nightSpend: Double,
        activeColor: Int = PixelTheme.COLOR_MINT_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val animP = progress.coerceIn(0f, 1f)
        val animSpent = spentToday * animP
        val animRatio = (spentPercent * animP).coerceIn(0f, 1.5f)
        val labelSize = (9.5f * density).coerceIn(8.5f, 11f * density)

        // Top Pacing Bar
        val barH = (5.5f * density).coerceIn(4f, 7f * density)
        val trackRect = RectF(bounds.left, bounds.top, bounds.right, bounds.top + barH)
        fillPaint.color = PixelTheme.COLOR_DOT_DIM_INT
        canvas.drawRoundRect(trackRect, 2f * density, 2f * density, fillPaint)

        val fillW = (bounds.width() * animRatio.coerceIn(0f, 1f)).coerceAtLeast(if (animSpent > 0) 3f * density else 0f)
        if (fillW > 0) {
            fillPaint.color = activeColor
            canvas.drawRoundRect(trackRect.left, trackRect.top, trackRect.left + fillW, trackRect.bottom, 2f * density, 2f * density, fillPaint)
        }

        // Labels below pacing bar
        val labelY = trackRect.bottom + 9.5f * density
        val pctInt = (animRatio * 100).toInt()
        val limitStr = "LIMIT " + PixelFont.formatIndianCompact(dailyLimit)
        PixelFont.drawLabel(canvas, "$pctInt% OF DAILY LIMIT", bounds.left, labelY, labelSize, activeColor)
        PixelFont.drawRightLabel(canvas, limitStr, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)

        // 4 Day-Part Checkpoint Sessions: MORN · AFTN · EVE · NIGHT
        val sessionY = labelY + 13f * density
        if (sessionY + 10f * density <= bounds.bottom + 10f * density) {
            val sessions = listOf(
                Pair("MORN", morningSpend),
                Pair("AFTN", afternoonSpend),
                Pair("EVE", eveningSpend),
                Pair("NIGHT", nightSpend)
            )
            val slotW = bounds.width() / 4f
            val dotR = 2.6f * density
            for (i in 0 until 4) {
                val (name, amt) = sessions[i]
                val sx = bounds.left + i * slotW + slotW / 2f
                val hasSpend = amt > 0
                fillPaint.color = if (hasSpend) activeColor else PixelTheme.COLOR_DOT_DIM_INT
                canvas.drawCircle(sx, sessionY, dotR, fillPaint)
                PixelFont.drawLabel(
                    canvas, name,
                    sx - (name.length * 2.3f * density),
                    sessionY + 9f * density,
                    7.5f * density,
                    if (hasSpend) PixelTheme.COLOR_DOT_ON_INT else PixelTheme.COLOR_DOT_MID_INT
                )
            }
        }
    }

    /**
     * GitHub-Style Month Heatmap:
     * 7 rows (Mon..Sun) by columns (weeks), with 4 distinct spending intensity tiers
     * and highlighted today cell.
     */
    fun drawGithubHeatmap(
        canvas: Canvas,
        bounds: RectF,
        intensities: FloatArray,
        todayIndex: Int = -1,
        activeColor: Int = PixelTheme.COLOR_MINT_INT,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val animP = progress.coerceIn(0f, 1f)
        val rows = 7 // Mon..Sun
        val cols = 5 // Weeks 1..5
        val leftPad = 12f * density
        val gridW = bounds.width() - leftPad
        val gridH = bounds.height()

        val gap = 2.2f * density
        val tileSize = ((gridW - (cols - 1) * gap) / cols).coerceAtMost((gridH - (rows - 1) * gap) / rows).coerceAtLeast(3f * density)

        // Day labels: Mon (row 0), Wed (row 2), Fri (row 4)
        val dayLabels = mapOf(0 to "M", 2 to "W", 4 to "F")
        for ((r, txt) in dayLabels) {
            val ly = bounds.top + r * (tileSize + gap) + tileSize * 0.8f
            PixelFont.drawLabel(canvas, txt, bounds.left, ly, 8f * density, PixelTheme.COLOR_DOT_MID_INT)
        }

        // Draw cells
        for (c in 0 until cols) {
            val tx = bounds.left + leftPad + c * (tileSize + gap)
            for (r in 0 until rows) {
                val idx = c * 7 + r
                val ty = bounds.top + r * (tileSize + gap)
                val cellRect = RectF(tx, ty, tx + tileSize, ty + tileSize)

                val intensity = if (idx in intensities.indices) intensities[idx] else 0f
                val animIntensity = intensity * animP

                val tileColor = when {
                    animIntensity <= 0.05f -> 0x18FFFFFF
                    animIntensity < 0.35f -> (0x55 shl 24) or (activeColor and 0x00FFFFFF)
                    animIntensity < 0.70f -> (0x99 shl 24) or (activeColor and 0x00FFFFFF)
                    else -> (0xFF shl 24) or (activeColor and 0x00FFFFFF)
                }

                fillPaint.color = tileColor
                canvas.drawRoundRect(cellRect, 1.5f * density, 1.5f * density, fillPaint)

                if (idx == todayIndex && animP > 0.4f) {
                    strokePaint.color = PixelTheme.COLOR_CYAN_INT
                    strokePaint.strokeWidth = 1.2f * density
                    canvas.drawRoundRect(cellRect, 1.5f * density, 1.5f * density, strokePaint)
                }
            }
        }
    }

    /**
     * Weather Sky Scene Canvas Renderer ported directly from Weather.Sky recipe:
     * - Recessed well
     * - Horizon hills
     * - Celestial body (Sun + rays or Moon + phase)
     * - Stars at night
     * - Drifting clouds and rain
     */
    fun drawWeatherScene(
        canvas: Canvas,
        bounds: RectF,
        sunProgress: Float,
        isDay: Boolean,
        moonPhase: Float,
        weatherKind: String,
        density: Float = 2.5f,
        progress: Float = 1.0f
    ) {
        val wellRadius = 8f * density
        fillPaint.color = 0xFF050508.toInt()
        canvas.drawRoundRect(bounds, wellRadius, wellRadius, fillPaint)
        strokePaint.color = 0x22FFFFFF
        strokePaint.strokeWidth = 1f
        canvas.drawRoundRect(bounds, wellRadius, wellRadius, strokePaint)

        val hzY = bounds.bottom - 4f * density
        val cols = 36
        val dotPitch = bounds.width() / cols
        val dotRadius = dotPitch * 0.38f

        // Hills along horizon
        fillPaint.color = 0x55FFFFFF
        for (x in 0 until cols) {
            val hillDots = max(0, ((3.0 - abs(x - cols * 0.8) / 2.4)).toInt()) +
                    max(0, ((1.6 - abs(x - cols * 0.15) / 2.2)).toInt())
            for (dy in 0..hillDots) {
                val cx = bounds.left + x * dotPitch + dotPitch / 2f
                val cy = hzY - dy * (dotPitch * 0.9f)
                canvas.drawCircle(cx, cy, dotRadius * 0.8f, fillPaint)
            }
        }

        // Celestial Body
        val arcW = bounds.width() - 16f * density
        val peakH = bounds.height() * 0.55f
        val celestialT = (sunProgress * progress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        val bodyX = bounds.left + 8f * density + celestialT * arcW
        val bodyY = hzY - sin(PI * celestialT).toFloat() * peakH

        if (isDay) {
            val sunR = 3.5f * density
            fillPaint.color = PixelTheme.COLOR_AMBER_INT
            canvas.drawCircle(bodyX, bodyY, sunR, fillPaint)
            strokePaint.color = PixelTheme.COLOR_AMBER_INT
            strokePaint.strokeWidth = 1f * density
            canvas.drawLine(bodyX, bodyY - sunR - 1f * density, bodyX, bodyY - sunR - 4f * density, strokePaint)
            canvas.drawLine(bodyX, bodyY + sunR + 1f * density, bodyX, bodyY + sunR + 4f * density, strokePaint)
            canvas.drawLine(bodyX - sunR - 1f * density, bodyY, bodyX - sunR - 4f * density, bodyY, strokePaint)
            canvas.drawLine(bodyX + sunR + 1f * density, bodyY, bodyX + sunR + 4f * density, bodyY, strokePaint)
        } else {
            val moonR = 3.2f * density
            fillPaint.color = PixelTheme.COLOR_CYAN_INT
            canvas.drawCircle(bodyX, bodyY, moonR, fillPaint)
            fillPaint.color = 0xAAFFFFFF.toInt()
            val starCoords = listOf(
                Pair(0.15f, 0.25f), Pair(0.35f, 0.18f), Pair(0.55f, 0.30f),
                Pair(0.72f, 0.15f), Pair(0.88f, 0.28f), Pair(0.25f, 0.50f)
            )
            for ((sx, sy) in starCoords) {
                val starX = bounds.left + sx * bounds.width()
                val starY = bounds.top + sy * bounds.height()
                canvas.drawCircle(starX, starY, 1.2f * density, fillPaint)
            }
        }

        // Clouds & rain
        if (weatherKind == "cloud" || weatherKind == "storm" || weatherKind == "partly") {
            val cloudColor = if (weatherKind == "storm") 0x99A0A0A0.toInt() else 0xCCFFFFFF.toInt()
            fillPaint.color = cloudColor
            val cloudBaseX = bounds.left + bounds.width() * 0.45f
            val cloudBaseY = bounds.top + bounds.height() * 0.35f
            val cr = 3f * density
            canvas.drawCircle(cloudBaseX, cloudBaseY, cr, fillPaint)
            canvas.drawCircle(cloudBaseX + cr * 1.2f, cloudBaseY - cr * 0.4f, cr * 1.3f, fillPaint)
            canvas.drawCircle(cloudBaseX + cr * 2.5f, cloudBaseY, cr, fillPaint)

            if (weatherKind == "storm") {
                strokePaint.color = PixelTheme.COLOR_CYAN_INT
                strokePaint.strokeWidth = 1f * density
                for (k in 0..4) {
                    val rx = cloudBaseX + k * (cr * 0.7f)
                    val ry = cloudBaseY + cr + 2f * density
                    canvas.drawLine(rx, ry, rx - 2f * density, ry + 6f * density, strokePaint)
                }
            }
        }
    }
}
