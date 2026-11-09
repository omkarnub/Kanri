package com.omkarnub.kanri.widget.visuals

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.omkarnub.kanri.widget.core.PixelTheme
import com.omkarnub.kanri.widget.font.DotMatrixFont
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared Pixel DotTiles visuals library.
 * Reusable Canvas primitives: Slider, Steps, Dots, Blocks, Rise, Grid, Arc, and 7x7 DotIcon.
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
        PLUS, MINUS, SPLIT, INBOX, FLAME, ARROW_UP, ARROW_DOWN, CHECK, SUN, MOON
    }

    private val ICON_BITMAPS: Map<DotIcon, IntArray> = mapOf(
        DotIcon.PLUS to intArrayOf(
            0b0000000,
            0b0001000,
            0b0001000,
            0b0111110,
            0b0001000,
            0b0001000,
            0b0000000
        ),
        DotIcon.MINUS to intArrayOf(
            0b0000000,
            0b0000000,
            0b0000000,
            0b0111110,
            0b0000000,
            0b0000000,
            0b0000000
        ),
        DotIcon.SPLIT to intArrayOf(
            0b0010100,
            0b0100010,
            0b1000001,
            0b0011100,
            0b1000001,
            0b0100010,
            0b0010100
        ),
        DotIcon.INBOX to intArrayOf(
            0b1111111,
            0b1000001,
            0b1000001,
            0b1100011,
            0b1011101,
            0b1000001,
            0b1111111
        ),
        DotIcon.FLAME to intArrayOf(
            0b0001000,
            0b0011000,
            0b0110100,
            0b0111110,
            0b1111111,
            0b1111111,
            0b0111110
        ),
        DotIcon.ARROW_UP to intArrayOf(
            0b0001000,
            0b0011100,
            0b0111110,
            0b1101011,
            0b0001000,
            0b0001000,
            0b0001000
        ),
        DotIcon.ARROW_DOWN to intArrayOf(
            0b0001000,
            0b0001000,
            0b0001000,
            0b1101011,
            0b0111110,
            0b0011100,
            0b0001000
        ),
        DotIcon.CHECK to intArrayOf(
            0b0000000,
            0b0000001,
            0b0000010,
            0b1000100,
            0b0101000,
            0b0010000,
            0b0000000
        ),
        DotIcon.SUN to intArrayOf(
            0b0010100,
            0b0000000,
            0b1011101,
            0b0011100,
            0b1011101,
            0b0000000,
            0b0010100
        ),
        DotIcon.MOON to intArrayOf(
            0b0011100,
            0b0111000,
            0b1110000,
            0b1110000,
            0b1110000,
            0b0111000,
            0b0011100
        )
    )

    /**
     * Draws a 7x7 dot-matrix icon.
     */
    fun drawIcon(
        canvas: Canvas,
        icon: DotIcon,
        originX: Float,
        originY: Float,
        dotPitch: Float = 4f,
        dotRadius: Float = dotPitch * 0.38f,
        color: Int = PixelTheme.COLOR_DOT_ON_INT
    ) {
        val rows = ICON_BITMAPS[icon] ?: return
        fillPaint.color = color
        for (r in 0 until 7) {
            val bits = rows[r]
            val cy = originY + r * dotPitch + dotRadius
            for (c in 0 until 7) {
                if ((bits and (1 shl (6 - c))) != 0) {
                    val cx = originX + c * dotPitch + dotRadius
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                }
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
        ticks: Int = 40,
        markerColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        trackColor: Int = PixelTheme.COLOR_DOT_DIM_INT,
        pacingMarker: Float? = null,
        labels: Pair<String, String>? = null
    ) {
        val clampedVal = value.coerceIn(0f, 1f)
        val tickWidth = 2.5f
        val tickHeight = bounds.height() * 0.65f
        val spacing = (bounds.width() - tickWidth) / (ticks - 1).coerceAtLeast(1)
        val markerIndex = (clampedVal * (ticks - 1)).toInt()

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
            val top = if (dist <= 1) bounds.top else bounds.top + (bounds.height() - tickHeight) / 2f
            val h = if (dist <= 1) bounds.height() else tickHeight
            canvas.drawRoundRect(tx, top, tx + tickWidth, top + h, 1.5f, 1.5f, fillPaint)
        }

        // Draw expected pacing marker if present
        if (pacingMarker != null) {
            val px = bounds.left + pacingMarker.coerceIn(0f, 1f) * (bounds.width() - tickWidth)
            fillPaint.color = PixelTheme.COLOR_CYAN_INT
            canvas.drawCircle(px + tickWidth / 2f, bounds.top - 4f, 2.5f, fillPaint)
        }

        // Optional left and right small captions
        if (labels != null) {
            val labelY = bounds.bottom + 4f
            DotMatrixFont.draw(
                canvas, labels.first, bounds.left, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
            DotMatrixFont.drawRightAligned(
                canvas, labels.second, bounds.right, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
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
        labels: Pair<String, String>? = null
    ) {
        if (data.isEmpty()) return
        val count = data.size
        val dotPitch = (bounds.height() / maxDotsPerBar).coerceAtLeast(3f)
        val dotRadius = dotPitch * 0.38f
        val colSpacing = bounds.width() / count.coerceAtLeast(1)

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
            val labelY = bounds.bottom + 5f
            DotMatrixFont.draw(
                canvas, labels.first, bounds.left, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
            DotMatrixFont.drawRightAligned(
                canvas, labels.second, bounds.right, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
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
        labels: Pair<String, String>? = null
    ) {
        val slots = data.size.coerceAtLeast(1)
        val slotWidth = bounds.width() / slots
        val cy = bounds.centerY()

        for (i in 0 until slots) {
            val cx = bounds.left + i * slotWidth + slotWidth / 2f
            val value = data[i]

            if (value != null && value > 0f) {
                val r = 2f + (value.coerceIn(0f, 1f) * 4.5f)
                fillPaint.color = dotColor
                canvas.drawCircle(cx, cy, r, fillPaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(cx, cy, 1.8f, fillPaint)
            }

            if (i == activeSlotIndex) {
                // Highlight current slot with a delicate tick indicator
                strokePaint.color = PixelTheme.COLOR_CYAN_INT
                canvas.drawCircle(cx, cy, 7.5f, strokePaint)
            }
        }

        if (labels != null) {
            val labelY = bounds.bottom + 4f
            DotMatrixFont.draw(
                canvas, labels.first, bounds.left, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
            DotMatrixFont.drawRightAligned(
                canvas, labels.second, bounds.right, labelY,
                dotPitch = PixelTheme.PITCH_LABEL * 0.7f,
                color = PixelTheme.COLOR_DOT_MID_INT
            )
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
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val total = maxSlots.coerceAtLeast(1)
        val gap = 3f
        val blockWidth = ((bounds.width() - (total - 1) * gap) / total).coerceAtLeast(2f)

        for (i in 0 until total) {
            val bx = bounds.left + i * (blockWidth + gap)
            val isFilled = i < count
            if (isFilled) {
                // Older items dimmer
                val fade = (1f - (i.toFloat() / total) * 0.5f)
                val alpha = (fade * 255).toInt().coerceIn(100, 255)
                fillPaint.color = (alpha shl 24) or (activeColor and 0x00FFFFFF)
            } else {
                fillPaint.color = dimColor
            }
            canvas.drawRoundRect(bx, bounds.top, bx + blockWidth, bounds.bottom, 2f, 2f, fillPaint)
        }
    }

    /**
     * 4-tier stepped gauge / progress rise.
     */
    fun drawRise(
        canvas: Canvas,
        bounds: RectF,
        ratio: Float, // 0..1
        tiers: Int = 4,
        activeColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val gap = 4f
        val tierWidth = (bounds.width() - (tiers - 1) * gap) / tiers
        val activeTiers = (ratio.coerceIn(0f, 1f) * tiers).toInt()

        for (t in 0 until tiers) {
            val tx = bounds.left + t * (tierWidth + gap)
            val isLit = t < activeTiers || (t == 0 && ratio > 0.05f)
            val h = bounds.height() * ((t + 1).toFloat() / tiers)
            val top = bounds.bottom - h

            fillPaint.color = if (isLit) activeColor else dimColor
            canvas.drawRoundRect(tx, top, tx + tierWidth, bounds.bottom, 2f, 2f, fillPaint)
        }
    }

    /**
     * Calendar or generic dot grid with per-cell intensity.
     */
    fun drawGrid(
        canvas: Canvas,
        bounds: RectF,
        cols: Int = 7,
        rows: Int = 5,
        intensities: FloatArray, // 0..1 per cell
        todayIndex: Int = -1,
        onColor: Int = PixelTheme.COLOR_DOT_ON_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val dotPitch = (bounds.width() / cols).coerceAtLeast(4f)
        val dotRadius = dotPitch * 0.35f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val index = r * cols + c
                if (index >= intensities.size) break

                val cx = bounds.left + c * dotPitch + dotPitch / 2f
                val cy = bounds.top + r * dotPitch + dotPitch / 2f
                val intensity = intensities[index].coerceIn(0f, 1f)

                if (intensity > 0f) {
                    val alpha = (70 + intensity * 185).toInt()
                    fillPaint.color = (alpha shl 24) or (onColor and 0x00FFFFFF)
                    canvas.drawCircle(cx, cy, dotRadius, fillPaint)
                } else {
                    fillPaint.color = dimColor
                    canvas.drawCircle(cx, cy, dotRadius * 0.7f, fillPaint)
                }

                if (index == todayIndex) {
                    strokePaint.color = PixelTheme.COLOR_CYAN_INT
                    strokePaint.strokeWidth = 1.5f
                    canvas.drawCircle(cx, cy, dotRadius + 3f, strokePaint)
                }
            }
        }
    }

    /**
     * Dotted arc traversed by a celestial body.
     */
    fun drawArc(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngleDeg: Float,
        sweepAngleDeg: Float,
        progress: Float, // 0..1 position of celestial body
        dotCount: Int = 24,
        onColor: Int = PixelTheme.COLOR_AMBER_INT,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val markerIndex = (progress.coerceIn(0f, 1f) * (dotCount - 1)).toInt()

        for (i in 0 until dotCount) {
            val frac = i.toFloat() / (dotCount - 1)
            val angleRad = Math.toRadians((startAngleDeg + frac * sweepAngleDeg).toDouble())
            val x = centerX + radius * cos(angleRad).toFloat()
            val y = centerY + radius * sin(angleRad).toFloat()

            if (i == markerIndex) {
                fillPaint.color = onColor
                canvas.drawCircle(x, y, 4.5f, fillPaint)
                strokePaint.color = onColor
                strokePaint.strokeWidth = 1.5f
                canvas.drawCircle(x, y, 7f, strokePaint)
            } else {
                fillPaint.color = dimColor
                canvas.drawCircle(x, y, 1.8f, fillPaint)
            }
        }
    }
}
