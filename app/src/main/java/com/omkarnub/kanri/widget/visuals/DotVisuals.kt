package com.omkarnub.kanri.widget.visuals

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.PathParser
import com.omkarnub.kanri.widget.core.PixelTheme
import com.omkarnub.kanri.widget.font.PixelFont
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared Pixel DotTiles visuals library.
 * Reusable Canvas primitives: Slider, Steps, Dots, Blocks, Rise, Grid, Arc, SkyScene, and Vector Icons.
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
            // Clock special case or fallback
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
     * Slider visual: row of fine vertical ticks with a marker block.
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
        density: Float = 2.5f
    ) {
        val clampedVal = value.coerceIn(0f, 1f)
        val tickWidth = (1.5f * density).coerceAtLeast(2f)
        val tickHeight = bounds.height() * 0.55f
        val spacing = (bounds.width() - tickWidth) / (ticks - 1).coerceAtLeast(1)
        val markerIndex = (clampedVal * (ticks - 1)).toInt()

        val markerWidth = (7f * density).coerceAtLeast(tickWidth * 2f)
        val markerHeight = bounds.height() * 0.85f

        for (i in 0 until ticks) {
            val tx = bounds.left + i * spacing
            val dist = abs(i - markerIndex)

            val paintColor = when {
                dist <= 1 -> markerColor
                mode == SliderMode.FILL && i < markerIndex -> markerColor
                mode == SliderMode.GLOW && dist <= 4 -> {
                    val alpha = ((1f - dist / 5f) * 255).toInt().coerceIn(40, 255)
                    (alpha shl 24) or (markerColor and 0x00FFFFFF)
                }
                else -> trackColor
            }

            fillPaint.color = paintColor
            if (dist == 0) {
                val mx = tx - (markerWidth - tickWidth) / 2f
                val my = bounds.centerY() - markerHeight / 2f
                canvas.drawRoundRect(mx, my, mx + markerWidth, my + markerHeight, 3f * density, 3f * density, fillPaint)
            } else {
                val top = bounds.centerY() - tickHeight / 2f
                canvas.drawRoundRect(tx, top, tx + tickWidth, top + tickHeight, 1.5f, 1.5f, fillPaint)
            }
        }

        // Draw expected pacing marker if present
        if (pacingMarker != null) {
            val px = bounds.left + pacingMarker.coerceIn(0f, 1f) * (bounds.width() - tickWidth)
            fillPaint.color = PixelTheme.COLOR_CYAN_INT
            canvas.drawCircle(px + tickWidth / 2f, bounds.top - 4f * density, 2.2f * density, fillPaint)
        }

        // Left and right small captions
        if (labels != null) {
            val labelY = bounds.bottom + 10f * density
            val labelSize = 9.5f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Stepped histogram bars built of dot columns.
     */
    fun drawSteps(
        canvas: Canvas,
        bounds: RectF,
        data: FloatArray, // values 0..1
        maxDotsPerBar: Int = 8,
        activeColor: Int = PixelTheme.COLOR_MINT_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        labels: Pair<String, String>? = null,
        density: Float = 2.5f
    ) {
        if (data.isEmpty()) return
        val count = data.size
        val dotPitch = (bounds.height() / maxDotsPerBar).coerceAtLeast(3f * density)
        val dotRadius = dotPitch * 0.36f
        val colSpacing = bounds.width() / count.coerceAtLeast(1)

        // Faint slot guide lines behind chart
        strokePaint.color = 0x18FFFFFF
        strokePaint.strokeWidth = 1f
        for (i in 0 until count) {
            val cx = bounds.left + i * colSpacing + colSpacing / 2f
            canvas.drawLine(cx, bounds.top, cx, bounds.bottom, strokePaint)
        }

        for (i in 0 until count) {
            val cx = bounds.left + i * colSpacing + colSpacing / 2f
            val barVal = data[i].coerceIn(0f, 1f)
            val litDots = (barVal * maxDotsPerBar).toInt()

            for (r in 0 until maxDotsPerBar) {
                val cy = bounds.bottom - (r * dotPitch + dotRadius)
                val isLit = r < litDots
                fillPaint.color = if (isLit) activeColor else dimColor
                canvas.drawCircle(cx, cy, dotRadius, fillPaint)
            }
        }

        if (labels != null) {
            val labelY = bounds.bottom + 10f * density
            val labelSize = 9.5f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Dotted timeline across slots (e.g. 24 hours).
     */
    fun drawDots(
        canvas: Canvas,
        bounds: RectF,
        data: List<Float?>, // null for idle slot, 0..1 for spend amount
        activeSlotIndex: Int = -1,
        dotColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        labels: Pair<String, String>? = null,
        density: Float = 2.5f
    ) {
        val slots = data.size.coerceAtLeast(1)
        val slotWidth = bounds.width() / slots
        val cy = bounds.centerY()

        for (i in 0 until slots) {
            val cx = bounds.left + i * slotWidth + slotWidth / 2f
            val value = data[i]

            if (value != null && value > 0f) {
                val r = (1.8f + (value.coerceIn(0f, 1f) * 3.5f)) * density
                fillPaint.color = dotColor
                canvas.drawCircle(cx, cy, r, fillPaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(cx, cy, 1.2f * density, fillPaint)
            }

            if (i == activeSlotIndex) {
                strokePaint.color = PixelTheme.COLOR_CYAN_INT
                strokePaint.strokeWidth = 1.5f * density
                canvas.drawCircle(cx, cy, 5.5f * density, strokePaint)
            }
        }

        if (labels != null) {
            val labelY = bounds.bottom + 10f * density
            val labelSize = 9.5f * density
            PixelFont.drawLabel(canvas, labels.first, bounds.left, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
            PixelFont.drawRightLabel(canvas, labels.second, bounds.right, labelY, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        }
    }

    /**
     * Sparse solid session blocks (e.g. for Review Queue).
     */
    fun drawBlocks(
        canvas: Canvas,
        bounds: RectF,
        count: Int,
        maxSlots: Int = 12,
        activeColor: Int = PixelTheme.COLOR_AMBER_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f
    ) {
        val total = maxSlots.coerceAtLeast(1)
        val gap = 3f * density
        val blockWidth = ((bounds.width() - (total - 1) * gap) / total).coerceAtLeast(2f)

        for (i in 0 until total) {
            val bx = bounds.left + i * (blockWidth + gap)
            val isFilled = i < count
            if (isFilled) {
                val fade = (1f - (i.toFloat() / total) * 0.5f)
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
     */
    fun drawRise(
        canvas: Canvas,
        bounds: RectF,
        ratio: Float, // 0..1
        tiers: Int = 4,
        activeColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f
    ) {
        val gap = 4f * density
        val tierWidth = (bounds.width() - (tiers - 1) * gap) / tiers
        val activeTiers = (ratio.coerceIn(0f, 1f) * tiers).toInt()

        for (t in 0 until tiers) {
            val tx = bounds.left + t * (tierWidth + gap)
            val isLit = t < activeTiers || (t == 0 && ratio > 0.05f)
            val h = bounds.height() * ((t + 1).toFloat() / tiers)
            val top = bounds.bottom - h

            fillPaint.color = if (isLit) activeColor else dimColor
            canvas.drawRoundRect(tx, top, tx + tierWidth, bounds.bottom, 2.5f * density, 2.5f * density, fillPaint)
        }
    }

    /**
     * Calendar or generic dot grid with day-of-week headers and per-cell intensity.
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
        density: Float = 2.5f
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
                9f * density, PixelTheme.COLOR_DOT_MID_INT
            )
        }

        val dotRadius = (colWidth * 0.32f).coerceAtMost(rowHeight * 0.32f)

        for (r in 0 until rows) {
            val cy = bounds.top + (r + 1.5f) * rowHeight
            for (c in 0 until cols) {
                val index = r * cols + c
                if (index >= intensities.size) break

                val cx = bounds.left + c * colWidth + colWidth / 2f
                val intensity = intensities[index].coerceIn(0f, 1f)

                if (intensity > 0f) {
                    val alpha = (80 + intensity * 175).toInt().coerceIn(80, 255)
                    fillPaint.color = (alpha shl 24) or (onColor and 0x00FFFFFF)
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                } else {
                    fillPaint.color = dimColor
                    canvas.drawCircle(cx, cy, dotRadius * 0.65f, fillPaint)
                }

                if (index == todayIndex) {
                    strokePaint.color = PixelTheme.COLOR_CYAN_INT
                    strokePaint.strokeWidth = 1.5f * density
                    canvas.drawCircle(cx, cy, dotRadius + 2.5f * density, strokePaint)
                }
            }
        }
    }

    /**
     * Dotted arc traversed by a celestial body (Weather / Sky widget).
     */
    fun drawArc(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        progress: Float,
        dotCount: Int = 24,
        onColor: Int = PixelTheme.COLOR_AMBER_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        density: Float = 2.5f
    ) {
        val markerIndex = (progress.coerceIn(0f, 1f) * (dotCount - 1)).toInt()

        for (i in 0 until dotCount) {
            val frac = i.toFloat() / (dotCount - 1)
            val angleRad = Math.toRadians((startAngleDeg + frac * sweepAngleDeg).toDouble())
            val x = centerX + radius * cos(angleRad).toFloat()
            val y = centerY + radius * sin(angleRad).toFloat()

            if (i == markerIndex) {
                fillPaint.color = onColor
                canvas.drawCircle(x, y, 4.5f * density, fillPaint)
                strokePaint.color = onColor
                strokePaint.strokeWidth = 1.5f * density
                canvas.drawCircle(x, y, 7.5f * density, strokePaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(x, y, 1.6f * density, fillPaint)
            }
        }
    }
}
