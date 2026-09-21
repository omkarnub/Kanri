package com.omkarnub.kanri.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Minimalist Linear Wobble Loader:
 * Replicating the CSS wobble animation:
 * - Size: 80px wide, 5px stroke height
 * - Background track with 10% opacity
 * - Rounded pill indicator oscillating horizontally from -95% to +95% with ease-in-out
 */
@Composable
fun KanriWobbleLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    width: Dp = 80.dp,
    height: Dp = 5.dp,
    bgOpacity: Float = 0.1f,
    durationMillis: Int = 1750
) {
    val cornerRadius = height / 2
    val shape = RoundedCornerShape(cornerRadius)
    val density = LocalDensity.current
    val widthPx = with(density) { width.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "wobbleTransition")

    // Animates from -95% to +95% and reverses back to -95%
    val translationFraction by infiniteTransition.animateFloat(
        initialValue = -0.95f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = durationMillis / 2,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobbleTranslation"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(shape)
            .background(color.copy(alpha = bgOpacity))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = widthPx * translationFraction
                }
                .clip(shape)
                .background(color)
        )
    }
}
