package com.omkarnub.kanri.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 7-row dot matrix glyph specifications ported from Louis Studios DotTiles.
 */
object DotMatrixFont {
    val GLYPHS: Map<Char, Array<String>> = mapOf(
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
        '$' to arrayOf("..X..", ".XXXX", "X.X..", ".XXX.", "..X.X", "XXXX.", "..X.."),
        '/' to arrayOf("....X", "...X.", "..X..", ".X...", "X....", ".....", ".....")
    )

    data class TextLayout(val totalCols: Int, val litCells: Set<Int>)

    fun layoutText(text: String): TextLayout {
        val lit = mutableSetOf<Int>()
        var x = 0
        var first = true
        for (ch in text) {
            val glyph = GLYPHS[ch] ?: GLYPHS[' ']!!
            if (!first) x += 1
            first = false
            val w = glyph[0].length
            for (r in 0 until 7) {
                val row = glyph[r]
                for (c in 0 until w) {
                    if (c < row.length && row[c] == 'X') {
                        lit.add(r * 64 + x + c)
                    }
                }
            }
            x += w
        }
        return TextLayout(totalCols = max(1, x), litCells = lit)
    }
}

/**
 * Bottom data visuals in DotTiles.
 */
sealed class DotTileVisual {
    /**
     * A row of fine ticks with a marker block easing into place.
     */
    data class Slider(
        val value: Float,
        val mode: String = "glow", // "glow" or "fill"
        val ticks: Int = 36,
        val labels: Pair<String, String>? = null
    ) : DotTileVisual()

    /**
     * Stepped histogram bar chart (like sleep stages or spending rhythm).
     */
    data class Steps(
        val data: List<Float>,
        val from: Float = 0f,
        val to: Float = 1f,
        val slots: Int = 15,
        val labels: Pair<String, String>? = null
    ) : DotTileVisual()

    /**
     * Dotted timeline with sized dots.
     */
    data class Dots(
        val data: List<Float?>,
        val slots: Int = 15,
        val labels: Pair<String, String>? = null
    ) : DotTileVisual()

    /**
     * Sparse solid timeline blocks for active spending sessions.
     */
    data class Blocks(
        val data: List<Pair<Float, Float>>,
        val slots: Int = 15,
        val labels: Pair<String, String>? = null
    ) : DotTileVisual()

    /**
     * A bar that rises in 4 distinct steps up to value.
     */
    data class Rise(
        val value: Float,
        val labels: Pair<String, String>? = null
    ) : DotTileVisual()
}

/**
 * Animated Dot-Matrix Text display.
 * Counts up smoothly on mount and features subtle organic jitter.
 */
@Composable
fun DotMatrixDisplay(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color = Color.White,
    unlitColor: Color = Color(0x18FFFFFF),
    dotRadiusDp: Dp = 2.4.dp,
    dotPitchDp: Dp = 5.8.dp,
    animateCount: Boolean = true,
    jitterMagnitude: Int = 0,
    replayTrigger: Int = 0
) {
    var displayValue by remember(text, replayTrigger) { mutableStateOf(text) }
    val animProgress = remember(text, replayTrigger) { Animatable(0f) }

    LaunchedEffect(text, replayTrigger) {
        if (animateCount) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    // Live subtle jitter drifting every few seconds
    LaunchedEffect(text, jitterMagnitude) {
        if (jitterMagnitude > 0) {
            val num = text.toDoubleOrNull()
            if (num != null) {
                while (true) {
                    delay(2800)
                    val offset = ((-jitterMagnitude..jitterMagnitude).random())
                    displayValue = (num + offset).toInt().toString()
                    delay(1200)
                    displayValue = text
                }
            }
        }
    }

    val currentText = if (animProgress.value < 1f && animateCount) {
        val num = text.toDoubleOrNull()
        if (num != null) {
            (num * animProgress.value).toInt().toString()
        } else {
            text
        }
    } else {
        displayValue
    }

    val layout = remember(currentText) { DotMatrixFont.layoutText(currentText) }

    Canvas(
        modifier = modifier
            .width(dotPitchDp * layout.totalCols)
            .height(dotPitchDp * 7)
    ) {
        val dotRadiusPx = dotRadiusDp.toPx()
        val dotPitchPx = dotPitchDp.toPx()

        for (r in 0 until 7) {
            for (c in 0 until layout.totalCols) {
                val isLit = layout.litCells.contains(r * 64 + c)
                val cx = c * dotPitchPx + dotPitchPx / 2f
                val cy = r * dotPitchPx + dotPitchPx / 2f

                if (isLit) {
                    val scale = if (animProgress.value < 1f) {
                        min(1.2f, animProgress.value * 1.15f)
                    } else 1f
                    drawCircle(
                        color = dotColor,
                        radius = dotRadiusPx * scale,
                        center = Offset(cx, cy)
                    )
                } else {
                    drawCircle(
                        color = unlitColor,
                        radius = dotRadiusPx * 0.45f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Visual component for DotTiles (Slider, Steps, Dots, Blocks, Rise).
 */
@Composable
fun DotTileBottomVisual(
    visual: DotTileVisual,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    replayTrigger: Int = 0
) {
    val drawAnim = remember(visual, replayTrigger) { Animatable(0f) }

    LaunchedEffect(visual, replayTrigger) {
        drawAnim.snapTo(0f)
        drawAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            val w = size.width
            val h = size.height
            val cy = h / 2f
            val draw = drawAnim.value

            when (visual) {
                is DotTileVisual.Slider -> {
                    val count = visual.ticks
                    val markerX = visual.value.coerceIn(0f, 1f) * w * draw
                    val tickSpacing = w / (count - 1)

                    // Draw fine ticks
                    for (i in 0 until count) {
                        val tx = i * tickSpacing
                        val d = abs(tx - markerX)
                        val opacity = if (visual.mode == "fill") {
                            if (tx <= markerX) 0.95f else 0.22f
                        } else {
                            // "glow" mode
                            max(0.20f, 0.95f - (d / (w * 0.25f)))
                        } * draw

                        drawLine(
                            color = color.copy(alpha = opacity),
                            start = Offset(tx, cy - 4.5f),
                            end = Offset(tx, cy + 4.5f),
                            strokeWidth = 1.3f
                        )
                    }

                    // Draw marker block
                    drawRect(
                        color = color.copy(alpha = min(1f, draw * 1.2f)),
                        topLeft = Offset(markerX - 4.5f, cy - 7f),
                        size = Size(9f, 14f)
                    )
                }

                is DotTileVisual.Steps -> {
                    val fromX = visual.from * w
                    val toX = visual.to * w
                    val n = visual.data.size
                    if (n > 0) {
                        val barW = (toX - fromX) / n
                        for (i in 0 until n) {
                            val barH = visual.data[i].coerceIn(0f, 1f) * 16f * draw
                            val bx = fromX + i * barW
                            drawRect(
                                color = color.copy(alpha = 0.85f),
                                topLeft = Offset(bx, h - barH),
                                size = Size(barW - 1f, barH)
                            )
                        }
                    }
                }

                is DotTileVisual.Dots -> {
                    val slots = visual.slots
                    val step = w / (slots - 1)
                    for (i in 0 until slots) {
                        val d = visual.data.getOrNull(i)
                        val cx = i * step
                        if (d != null) {
                            val r = (2f + d * 4f) * draw
                            drawCircle(
                                color = color,
                                radius = r,
                                center = Offset(cx, cy)
                            )
                        } else {
                            // Faint slot guide
                            drawLine(
                                color = color.copy(alpha = 0.15f * draw),
                                start = Offset(cx, cy - 7f),
                                end = Offset(cx, cy + 7f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }

                is DotTileVisual.Blocks -> {
                    for (block in visual.data) {
                        val x0 = block.first.coerceIn(0f, 1f) * w
                        val x1 = block.second.coerceIn(0f, 1f) * w
                        val bw = (x1 - x0) * draw
                        drawRect(
                            color = color.copy(alpha = 0.9f),
                            topLeft = Offset(x0, cy - 5f),
                            size = Size(bw, 10f)
                        )
                    }
                }

                is DotTileVisual.Rise -> {
                    val targetW = visual.value.coerceIn(0f, 1f) * w * draw
                    val steps = 4
                    val stepW = targetW / steps
                    for (s in 0 until steps) {
                        val sh = ((s + 1) / steps.toFloat()) * 14f
                        drawRect(
                            color = color.copy(alpha = 0.88f),
                            topLeft = Offset(s * stepW, h - sh),
                            size = Size(stepW - 1.5f, sh)
                        )
                    }
                }
            }
        }

        // Labels under bottom visual
        val labels = when (visual) {
            is DotTileVisual.Slider -> visual.labels
            is DotTileVisual.Steps -> visual.labels
            is DotTileVisual.Dots -> visual.labels
            is DotTileVisual.Blocks -> visual.labels
            is DotTileVisual.Rise -> visual.labels
        }

        if (labels != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = labels.first,
                    fontSize = 10.sp,
                    fontFamily = GoogleSansFlex,
                    color = color.copy(alpha = 0.45f)
                )
                Text(
                    text = labels.second,
                    fontSize = 10.sp,
                    fontFamily = GoogleSansFlex,
                    color = color.copy(alpha = 0.45f)
                )
            }
        }
    }
}

/**
 * 1:1 Louis Studios DotTile Card Container with black glass finish, dot numerals, and visuals.
 */
@Composable
fun DotTileCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    unit: String? = null,
    visual: DotTileVisual? = null,
    jitterMagnitude: Int = 0,
    dotColor: Color = Color.White,
    tileColor: Color = Color(0xFF08080A),
    onClick: (() -> Unit)? = null
) {
    var replayCounter by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(22.dp))
            .background(tileColor)
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp))
            .clickable(enabled = onClick != null || true) {
                replayCounter++
                onClick?.invoke()
            }
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = dotColor,
                    maxLines = 1
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        color = dotColor.copy(alpha = 0.50f),
                        maxLines = 1
                    )
                }
            }

            // Big Dot Matrix Value Row
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                DotMatrixDisplay(
                    text = value,
                    dotColor = dotColor,
                    jitterMagnitude = jitterMagnitude,
                    replayTrigger = replayCounter
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unit,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = dotColor.copy(alpha = 0.70f)
                    )
                }
            }

            // Bottom Visual
            if (visual != null) {
                DotTileBottomVisual(
                    visual = visual,
                    color = dotColor,
                    replayTrigger = replayCounter
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

