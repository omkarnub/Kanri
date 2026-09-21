package com.omkarnub.kanri.ui.common

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.omkarnub.kanri.util.CurrencyUtils

/**
 * Animated number component inspired by Framer Motion's AnimatedNumber.
 * Each digit is rendered in a clipped vertical strip (0..9) that rolls smoothly
 * up or down based on value changes, with cascading delays per digit.
 * Non-digit characters (currency symbols, commas, decimals) stay anchored.
 */
@Composable
fun AnimatedNumberText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    animateFromZero: Boolean = true,
    durationMillis: Int = 850,
    delayPerDigitMillis: Int = 30,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val effectiveStyle = remember(style, fontSize) {
        if (fontSize != TextUnit.Unspecified) style.copy(fontSize = fontSize) else style
    }

    // Measure digit dimensions using sample digits to ensure uniform box sizing
    val digitMetrics = remember(effectiveStyle, density) {
        val samples = (0..9).map { d ->
            textMeasurer.measure(d.toString(), effectiveStyle).size
        }
        val maxWidth = samples.maxOf { it.width }
        val maxHeight = samples.maxOf { it.height }
        maxWidth to maxHeight
    }

    val digitWidthDp = with(density) { digitMetrics.first.toDp() }
    val digitHeightDp = with(density) { digitMetrics.second.toDp() }
    val digitHeightPx = digitMetrics.second.toFloat()

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement
    ) {
        var digitCounter = 0
        text.forEachIndexed { charIndex, char ->
            key(charIndex) {
                if (char.isDigit()) {
                    SingleDigitStrip(
                        digit = char.digitToInt(),
                        index = digitCounter++,
                        style = effectiveStyle,
                        color = color,
                        digitWidthDp = digitWidthDp,
                        digitHeightDp = digitHeightDp,
                        digitHeightPx = digitHeightPx,
                        animateFromZero = animateFromZero,
                        durationMillis = durationMillis,
                        delayPerDigit = delayPerDigitMillis
                    )
                } else {
                    Text(
                        text = char.toString(),
                        style = effectiveStyle,
                        color = color
                    )
                }
            }
        }
    }
}

/**
 * Convenience wrapper taking a numeric Double value (e.g. total spent amount),
 * formatting it with CurrencyUtils.formatCurrency and applying the rolling digit animation.
 */
@Composable
fun AnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    animateFromZero: Boolean = true,
    durationMillis: Int = 850,
    delayPerDigitMillis: Int = 30,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center
) {
    val formatted = remember(value) { CurrencyUtils.formatCurrency(value) }
    AnimatedNumberText(
        text = formatted,
        modifier = modifier,
        style = style,
        color = color,
        fontSize = fontSize,
        animateFromZero = animateFromZero,
        durationMillis = durationMillis,
        delayPerDigitMillis = delayPerDigitMillis,
        horizontalArrangement = horizontalArrangement
    )
}

/**
 * Single digit holder containing a vertical strip of 0..9 digits.
 * Animates the translationY to position the target digit into view.
 */
@Composable
private fun SingleDigitStrip(
    digit: Int,
    index: Int,
    style: TextStyle,
    color: Color,
    digitWidthDp: Dp,
    digitHeightDp: Dp,
    digitHeightPx: Float,
    animateFromZero: Boolean,
    durationMillis: Int,
    delayPerDigit: Int
) {
    var hasAppeared by remember { mutableStateOf(!animateFromZero) }

    LaunchedEffect(digit) {
        hasAppeared = true
    }

    val targetValue = if (hasAppeared) digit.toFloat() else 0f

    val animatedDigit by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMillis,
            delayMillis = index * delayPerDigit,
            easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) // smooth deceleration curve
        ),
        label = "animatedDigit_$index"
    )

    Box(
        modifier = Modifier
            .size(width = digitWidthDp, height = digitHeightDp)
            .clipToBounds(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                .graphicsLayer {
                    translationY = -animatedDigit * digitHeightPx
                }
        ) {
            for (i in 0..9) {
                Box(
                    modifier = Modifier.size(width = digitWidthDp, height = digitHeightDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = i.toString(),
                        style = style,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Score-style animated number with scale and color feedback when value increases or decreases.
 */
@Composable
fun AnimatedScore(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    neutralColor: Color = Color.White,
    positiveColor: Color = Color(0xFF34C759),
    negativeColor: Color = Color(0xFFE5484D),
    fontSize: TextUnit = TextUnit.Unspecified,
    durationMillis: Int = 400
) {
    var prevValue by remember { mutableFloatStateOf(value.toFloat()) }
    val scale = remember { Animatable(1f) }
    val animatedColor = remember { androidx.compose.animation.Animatable(neutralColor) }

    LaunchedEffect(value) {
        val current = value.toFloat()
        if (current != prevValue) {
            val isIncrease = current > prevValue
            prevValue = current

            val flashColor = if (isIncrease) positiveColor else negativeColor
            animatedColor.snapTo(flashColor)

            // Scale burst and return to 1f
            scale.animateTo(1.25f, tween(durationMillis = 120, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(durationMillis = durationMillis, easing = FastOutSlowInEasing))

            // Fade color back to neutral
            animatedColor.animateTo(neutralColor, tween(durationMillis = durationMillis))
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    ) {
        AnimatedNumber(
            value = value,
            style = style,
            color = animatedColor.value,
            fontSize = fontSize
        )
    }
}
