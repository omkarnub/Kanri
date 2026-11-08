package com.omkarnub.kanri.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex

/**
 * 1:1 Port of GSAP RollingText into Jetpack Compose.
 *
 * Implements deterministic mulberry32 PRNG reel generation:
 * - Each character has a vertical reel with (cycles + 1) copies.
 * - cycles = minCycles + floor(rand() * cycleVariance)
 * - duration = baseDuration + rand() * durationVariance
 * - expo.out easing: CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f)
 * - All characters and digits strictly use GoogleSansFlex font.
 */

private class Mulberry32(private var seed: Int) {
    fun nextFloat(): Float {
        seed = seed + 0x6D2B79F5
        var t = seed xor (seed ushr 15)
        t *= (1 or seed)
        t = t xor (t + (t xor (t ushr 7)) * (61 or t))
        val unsigned = (t xor (t ushr 14)).toLong() and 0xFFFFFFFFL
        return (unsigned.toDouble() / 4294967296.0).toFloat()
    }
}

private data class ReelConfig(
    val minCycles: Int = 3,
    val cycleVariance: Int = 3,
    val durationSeconds: Float = 1.6f,
    val durationVarianceSeconds: Float = 0.5f
)

private data class Reel(
    val copies: Int,
    val to: Int,
    val durationMillis: Int
)

private fun buildReel(charIndex: Int, config: ReelConfig): Reel {
    val rand = Mulberry32(charIndex * 1013 + 7)
    val cycles = config.minCycles + (rand.nextFloat() * config.cycleVariance).toInt()
    val durSec = config.durationSeconds + rand.nextFloat() * config.durationVarianceSeconds
    return Reel(
        copies = cycles + 1,
        to = cycles,
        durationMillis = (durSec * 1000f).toInt()
    )
}

@Composable
fun RollingText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = 38.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily = GoogleSansFlex,
    minCycles: Int = 3,
    cycleVariance: Int = 3,
    durationSeconds: Float = 1.5f,
    durationVarianceSeconds: Float = 0.5f,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val style = remember(fontSize, fontWeight, fontFamily) {
        TextStyle(
            fontFamily = fontFamily,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = (-0.5).sp
        )
    }

    val config = remember(minCycles, cycleVariance, durationSeconds, durationVarianceSeconds) {
        ReelConfig(minCycles, cycleVariance, durationSeconds, durationVarianceSeconds)
    }

    // Measure digit dimensions so all digits align with identical cell box
    val digitMeasure = remember(style, density) {
        val size = textMeasurer.measure("8", style).size
        val wDp = with(density) { size.width.toDp() }
        val hDp = with(density) { size.height.toDp() }
        Triple(wDp, hDp, size.height.toFloat())
    }
    val defaultDigitWidthDp = digitMeasure.first
    val digitHeightDp = digitMeasure.second
    val digitHeightPx = digitMeasure.third

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        text.forEachIndexed { charIndex, char ->
            if (char == ' ') {
                Box(modifier = Modifier.width(with(density) { textMeasurer.measure(" ", style).size.width.toDp() }))
            } else {
                val reel = remember(charIndex, char, config) {
                    buildReel(charIndex, config)
                }

                val charSize = remember(char, style, density) {
                    if (char.isDigit()) {
                        defaultDigitWidthDp to digitHeightDp
                    } else {
                        val m = textMeasurer.measure(char.toString(), style).size
                        with(density) { m.width.toDp() } to digitHeightDp
                    }
                }

                SingleReelCell(
                    char = char,
                    reel = reel,
                    style = style,
                    textColor = textColor,
                    widthDp = charSize.first,
                    heightDp = charSize.second,
                    heightPx = digitHeightPx
                )
            }
        }
    }
}

@Composable
private fun SingleReelCell(
    char: Char,
    reel: Reel,
    style: TextStyle,
    textColor: Color,
    widthDp: Dp,
    heightDp: Dp,
    heightPx: Float
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(reel) {
        animatable.animateTo(
            targetValue = reel.to.toFloat(),
            animationSpec = tween(
                durationMillis = reel.durationMillis,
                easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) // expo.out
            )
        )
    }

    Box(
        modifier = Modifier
            .width(widthDp)
            .height(heightDp)
            .clipToBounds(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                .graphicsLayer {
                    translationY = -animatable.value * heightPx
                }
        ) {
            for (copy in 0 until reel.copies) {
                val displayChar = if (char.isDigit()) {
                    if (copy == reel.to) {
                        char
                    } else {
                        val offset = (reel.to - copy) * 3
                        val d = (char.digitToInt() + offset) % 10
                        d.toString()[0]
                    }
                } else {
                    char
                }

                Box(
                    modifier = Modifier
                        .width(widthDp)
                        .height(heightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayChar.toString(),
                        style = style,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

