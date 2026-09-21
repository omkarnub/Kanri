package com.omkarnub.kanri.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 3D Flip & Fade text animation component inspired by Framer Motion's FlipFadeText.
 * Renders text character-by-character with a 3D perspective rotation (rotationX 90 -> 0),
 * vertical translation (translationY 16dp -> 0dp), and opacity fade, with cascading stagger delays.
 */
@Composable
fun FlipFadeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    letterDurationMillis: Int = 420,
    staggerDelayMillis: Int = 20,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start
) {
    val effectiveStyle = remember(style, fontSize) {
        if (fontSize != TextUnit.Unspecified) style.copy(fontSize = fontSize) else style
    }

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            SingleFlipLetter(
                char = char,
                index = index,
                style = effectiveStyle,
                color = color,
                letterDurationMillis = letterDurationMillis,
                staggerDelayMillis = staggerDelayMillis
            )
        }
    }
}

@Composable
private fun SingleFlipLetter(
    char: Char,
    index: Int,
    style: TextStyle,
    color: Color,
    letterDurationMillis: Int,
    staggerDelayMillis: Int
) {
    if (char == ' ') {
        Text(text = " ", style = style)
        return
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(char) {
        progress.snapTo(0f)
        delay((index * staggerDelayMillis).toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = letterDurationMillis,
                easing = CubicBezierEasing(0.2f, 0.65f, 0.3f, 0.9f)
            )
        )
    }

    val density = LocalDensity.current
    Text(
        text = char.toString(),
        style = style,
        color = color,
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            rotationX = (1f - p) * 90f
            translationY = (1f - p) * 16.dp.toPx()
            cameraDistance = 16f * density.density
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
    )
}
