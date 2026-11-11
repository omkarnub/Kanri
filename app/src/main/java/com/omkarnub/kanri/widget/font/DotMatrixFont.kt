package com.omkarnub.kanri.widget.font

import android.graphics.Canvas
import android.graphics.Paint
import com.omkarnub.kanri.widget.core.PixelTheme
import java.util.Locale
import kotlin.math.abs

/**
 * 7-row compact array bitmap font (5 columns wide + 1 column spacing).
 * Monospaced tabular digits and symbols with reveal-progress support for animations.
 */
object DotMatrixFont {

    const val ROWS = 7
    const val COLS = 5
    const val CHAR_STRIDE = 6 // 5 dots glyph + 1 dot spacing

    data class Size(val width: Float, val height: Float)

    private val GLYPHS: Map<Char, IntArray> = buildMap {
        // Digits 0-9
        put('0', intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110))
        put('1', intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110))
        put('2', intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b01000, 0b10000, 0b11111))
        put('3', intArrayOf(0b01110, 0b10001, 0b00001, 0b00110, 0b00001, 0b10001, 0b01110))
        put('4', intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010))
        put('5', intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110))
        put('6', intArrayOf(0b01110, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b01110))
        put('7', intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000))
        put('8', intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110))
        put('9', intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110))

        // Uppercase A-Z
        put('A', intArrayOf(0b01110, 0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001))
        put('B', intArrayOf(0b11110, 0b10001, 0b11110, 0b10001, 0b10001, 0b10001, 0b11110))
        put('C', intArrayOf(0b01110, 0b10001, 0b10000, 0b10000, 0b10000, 0b10001, 0b01110))
        put('D', intArrayOf(0b11100, 0b10010, 0b10001, 0b10001, 0b10001, 0b10010, 0b11100))
        put('E', intArrayOf(0b11111, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000, 0b11111))
        put('F', intArrayOf(0b11111, 0b10000, 0b11110, 0b10000, 0b10000, 0b10000, 0b10000))
        put('G', intArrayOf(0b01110, 0b10001, 0b10000, 0b10111, 0b10001, 0b10001, 0b01110))
        put('H', intArrayOf(0b10001, 0b10001, 0b11111, 0b10001, 0b10001, 0b10001, 0b10001))
        put('I', intArrayOf(0b01110, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110))
        put('J', intArrayOf(0b00111, 0b00010, 0b00010, 0b00010, 0b10010, 0b10010, 0b01100))
        put('K', intArrayOf(0b10001, 0b10010, 0b10100, 0b11000, 0b10100, 0b10010, 0b10001))
        put('L', intArrayOf(0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b10000, 0b11111))
        put('M', intArrayOf(0b10001, 0b11011, 0b10101, 0b10001, 0b10001, 0b10001, 0b10001))
        put('N', intArrayOf(0b10001, 0b11001, 0b10101, 0b10011, 0b10001, 0b10001, 0b10001))
        put('O', intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110))
        put('P', intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10000, 0b10000, 0b10000))
        put('Q', intArrayOf(0b01110, 0b10001, 0b10001, 0b10001, 0b10101, 0b10011, 0b01111))
        put('R', intArrayOf(0b11110, 0b10001, 0b10001, 0b11110, 0b10100, 0b10010, 0b10001))
        put('S', intArrayOf(0b01110, 0b10001, 0b10000, 0b01110, 0b00001, 0b10001, 0b01110))
        put('T', intArrayOf(0b11111, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100, 0b00100))
        put('U', intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01110))
        put('V', intArrayOf(0b10001, 0b10001, 0b10001, 0b10001, 0b10001, 0b01010, 0b00100))
        put('W', intArrayOf(0b10001, 0b10001, 0b10001, 0b10101, 0b10101, 0b11011, 0b10001))
        put('X', intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b01010, 0b10001, 0b10001))
        put('Y', intArrayOf(0b10001, 0b10001, 0b01010, 0b00100, 0b00100, 0b00100, 0b00100))
        put('Z', intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b11111))

        // Symbols & Punctuation: : . , - + ₹ $ % / x k M L ▲ ▼ ✓ ·
        put(':', intArrayOf(0b00000, 0b01100, 0b01100, 0b00000, 0b01100, 0b01100, 0b00000))
        put('.', intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100))
        put(',', intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b00100, 0b01000))
        put('-', intArrayOf(0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000))
        put('+', intArrayOf(0b00000, 0b00100, 0b00100, 0b11111, 0b00100, 0b00100, 0b00000))
        put('₹', intArrayOf(0b11111, 0b00100, 0b11110, 0b00101, 0b00110, 0b01010, 0b10001))
        put('$', intArrayOf(0b00100, 0b01111, 0b10100, 0b01110, 0b00101, 0b11110, 0b00100))
        put('%', intArrayOf(0b11001, 0b11010, 0b00100, 0b01000, 0b01011, 0b10011, 0b00000))
        put('/', intArrayOf(0b00001, 0b00010, 0b00100, 0b00100, 0b01000, 0b10000, 0b00000))
        put('▲', intArrayOf(0b00100, 0b01110, 0b11111, 0b00000, 0b00000, 0b00000, 0b00000))
        put('▼', intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b11111, 0b01110, 0b00100))
        put('✓', intArrayOf(0b00000, 0b00001, 0b00010, 0b10100, 0b01000, 0b00000, 0b00000))
        put('·', intArrayOf(0b00000, 0b00000, 0b00000, 0b01100, 0b01100, 0b00000, 0b00000))
        put(' ', intArrayOf(0, 0, 0, 0, 0, 0, 0))
    }

    private val paintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    private val paint: Paint get() = paintLocal.get()!!

    fun getGlyph(c: Char): IntArray {
        val upper = c.uppercaseChar()
        return GLYPHS[upper] ?: GLYPHS[c] ?: GLYPHS[' ']!!
    }

    /**
     * Calculates the rendered size of [text] at given [dotPitch].
     */
    fun measure(text: String, dotPitch: Float): Size {
        if (text.isEmpty()) return Size(0f, 0f)
        val charCount = text.length
        val width = (charCount * CHAR_STRIDE - 1) * dotPitch
        val height = ROWS * dotPitch
        return Size(width, height)
    }

    /**
     * Draws [text] onto [canvas] starting at [originX], [originY] (top-left of first glyph).
     * [progress] in 0f..1f controls reveal progress (dot-by-dot counting animation).
     */
    fun draw(
        canvas: Canvas,
        text: String,
        originX: Float,
        originY: Float,
        dotPitch: Float,
        dotRadius: Float = dotPitch * 0.38f,
        color: Int = PixelTheme.COLOR_DOT_ON_INT,
        progress: Float = 1.0f,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        if (text.isEmpty()) return

        // Compute total active dots for progress gating
        var totalActiveDots = 0
        for (i in text.indices) {
            val glyph = getGlyph(text[i])
            for (r in 0 until ROWS) {
                val rowBits = glyph[r]
                for (c in 0 until COLS) {
                    if ((rowBits and (1 shl (COLS - 1 - c))) != 0) {
                        totalActiveDots++
                    }
                }
            }
        }

        val revealCutoff = (totalActiveDots * progress.coerceIn(0f, 1f)).toInt()
        var currentActiveDotIndex = 0

        var cursorX = originX
        for (i in text.indices) {
            val glyph = getGlyph(text[i])
            for (r in 0 until ROWS) {
                val rowBits = glyph[r]
                val cy = originY + r * dotPitch + dotRadius
                for (c in 0 until COLS) {
                    val cx = cursorX + c * dotPitch + dotRadius
                    val isSet = (rowBits and (1 shl (COLS - 1 - c))) != 0
                    if (isSet) {
                        if (currentActiveDotIndex < revealCutoff) {
                            paint.color = color
                            canvas.drawCircle(cx, cy, dotRadius, paint)
                        } else if (dimColor != 0) {
                            paint.color = dimColor
                            canvas.drawCircle(cx, cy, dotRadius * 0.65f, paint)
                        }
                        currentActiveDotIndex++
                    }
                }
            }
            cursorX += CHAR_STRIDE * dotPitch
        }
    }

    /**
     * Helper to draw text right-aligned against [rightX].
     */
    fun drawRightAligned(
        canvas: Canvas,
        text: String,
        rightX: Float,
        originY: Float,
        dotPitch: Float,
        dotRadius: Float = dotPitch * 0.38f,
        color: Int = PixelTheme.COLOR_DOT_ON_INT,
        progress: Float = 1.0f,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val size = measure(text, dotPitch)
        val originX = rightX - size.width
        draw(canvas, text, originX, originY, dotPitch, dotRadius, color, progress, dimColor)
    }

    /**
     * Helper to draw text centered horizontally between [leftX] and [rightX].
     */
    fun drawCentered(
        canvas: Canvas,
        text: String,
        leftX: Float,
        rightX: Float,
        originY: Float,
        dotPitch: Float,
        dotRadius: Float = dotPitch * 0.38f,
        color: Int = PixelTheme.COLOR_DOT_ON_INT,
        progress: Float = 1.0f,
        dimColor: Int = PixelTheme.COLOR_DOT_DIM_INT
    ) {
        val size = measure(text, dotPitch)
        val originX = leftX + ((rightX - leftX) - size.width) / 2f
        draw(canvas, text, originX, originY, dotPitch, dotRadius, color, progress, dimColor)
    }

    /**
     * Finds the largest dot pitch between [minPitch] and [maxPitch] such that [text] fits within [availableWidth].
     */
    fun findFittingPitch(
        text: String,
        availableWidth: Float,
        maxPitch: Float = PixelTheme.PITCH_HERO_LARGE,
        minPitch: Float = 7f
    ): Float {
        if (text.isEmpty() || availableWidth <= 0f) return minPitch
        val charCount = text.length
        val requiredCols = (charCount * CHAR_STRIDE - 1).coerceAtLeast(1)
        val calculated = availableWidth / requiredCols
        return calculated.coerceIn(minPitch, maxPitch)
    }

    /**
     * Formats an amount into compact Indian format:
     * - `₹1.2k` (for >= 1,000)
     * - `₹12.4k` (for >= 10,000)
     * - `₹1.2L` (for >= 1,00,000)
     * - `₹1.5Cr` (for >= 1,00,00,000)
     * - `₹850` (for < 1,000)
     */
    fun formatIndianCompact(
        amount: Double,
        prefix: String = com.omkarnub.kanri.util.CurrencyUtils.getCurrencySymbol()
    ): String {
        if (amount.isNaN() || amount.isInfinite()) return "${prefix}0"
        val isNegative = amount < 0
        val absVal = abs(amount)

        val formattedNum = if (prefix == "₹") {
            when {
                absVal >= 10_000_000 -> {
                    val cr = absVal / 10_000_000.0
                    if (cr >= 100) String.format(Locale.US, "%.0fCr", cr)
                    else String.format(Locale.US, "%.1fCr", cr).replace(".0Cr", "Cr")
                }
                absVal >= 100_000 -> {
                    val l = absVal / 100_000.0
                    if (l >= 100) String.format(Locale.US, "%.0fL", l)
                    else String.format(Locale.US, "%.1fL", l).replace(".0L", "L")
                }
                absVal >= 1_000 -> {
                    val k = absVal / 1_000.0
                    if (k >= 100) String.format(Locale.US, "%.0fk", k)
                    else String.format(Locale.US, "%.1fk", k).replace(".0k", "k")
                }
                else -> kotlin.math.round(absVal).toInt().toString()
            }
        } else {
            when {
                absVal >= 1_000_000_000 -> {
                    val b = absVal / 1_000_000_000.0
                    if (b >= 100) String.format(Locale.US, "%.0fB", b)
                    else String.format(Locale.US, "%.1fB", b).replace(".0B", "B")
                }
                absVal >= 1_000_000 -> {
                    val m = absVal / 1_000_000.0
                    if (m >= 100) String.format(Locale.US, "%.0fM", m)
                    else String.format(Locale.US, "%.1fM", m).replace(".0M", "M")
                }
                absVal >= 1_000 -> {
                    val k = absVal / 1_000.0
                    if (k >= 100) String.format(Locale.US, "%.0fk", k)
                    else String.format(Locale.US, "%.1fk", k).replace(".0k", "k")
                }
                else -> kotlin.math.round(absVal).toInt().toString()
            }
        }

        val showNegative = isNegative && formattedNum != "0"
        return if (showNegative) "-$prefix$formattedNum" else "$prefix$formattedNum"
    }
}
