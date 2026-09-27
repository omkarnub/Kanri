package com.omkarnub.kanri.ui.onboarding.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.Panchang
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Hinge position determining origin and 3D folding angles.
 * Ported from GSAP FoldText hinge configuration:
 * - TOP: origin '50% 0%', rotateX: -92, rotateY: 0
 * - BOTTOM: origin '50% 100%', rotateX: 92, rotateY: 0
 * - LEFT: origin '0% 50%', rotateX: 0, rotateY: 92
 * - RIGHT: origin '100% 50%', rotateX: 0, rotateY: -92
 */
enum class FoldHinge(
    val transformOrigin: TransformOrigin,
    val initialRotateX: Float,
    val initialRotateY: Float
) {
    TOP(
        transformOrigin = TransformOrigin(0.5f, 0.0f),
        initialRotateX = -92f,
        initialRotateY = 0f
    ),
    BOTTOM(
        transformOrigin = TransformOrigin(0.5f, 1.0f),
        initialRotateX = 92f,
        initialRotateY = 0f
    ),
    LEFT(
        transformOrigin = TransformOrigin(0.0f, 0.5f),
        initialRotateX = 0f,
        initialRotateY = 92f
    ),
    RIGHT(
        transformOrigin = TransformOrigin(1.0f, 0.5f),
        initialRotateX = 0f,
        initialRotateY = -92f
    )
}

/**
 * GSAP power3.out easing: 1 - (1 - t)^3
 * Fast initial motion with smooth deceleration to stop.
 */
val GSAPPower3Out = Easing { t ->
    val inv = 1f - t
    1f - (inv * inv * inv)
}

/**
 * GSAP power3.in easing: t^3
 */
val GSAPPower3In = Easing { t ->
    t * t * t
}

/**
 * FoldText: Native Jetpack Compose 3D Origami/Hinge Unfolding Typography.
 * Staggers characters with 3D perspective rotation, opacity transition, and
 * realistic crease shading along the fold hinge.
 */
@Composable
fun FoldText(
    text: String,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = Panchang,
    fontWeight: FontWeight = FontWeight.Bold,
    fontSize: TextUnit = 50.sp,
    color: Color = Color(0xFFF7F2E8),
    hinge: FoldHinge = FoldHinge.TOP,
    durationMs: Int = 650,
    staggerMs: Int = 45,
    creaseShading: Float = 0.55f,
    loop: Boolean = true,
    repeatDelayMs: Long = 3200L,
    letterSpacing: Dp = 6.dp
) {
    val density = LocalDensity.current.density
    val safeCrease = creaseShading.coerceIn(0f, 1f)

    // Per-character animation progress (0f = folded, 1f = fully unfolded)
    val animatables = remember(text) {
        List(text.length) { Animatable(0f) }
    }

    var replayKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(text, replayKey, loop) {
        while (isActive) {
            // Reset to folded state
            animatables.forEach { it.snapTo(0f) }
            delay(100L)

            // Staggered unfold animation
            coroutineScope {
                animatables.forEachIndexed { index, anim ->
                    launch {
                        delay(index * staggerMs.toLong())
                        anim.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = durationMs,
                                easing = GSAPPower3Out
                            )
                        )
                    }
                }
            }

            if (!loop) break

            // Hold unfolded state
            delay(repeatDelayMs)

            // Staggered fold-back transition before loop cycle
            coroutineScope {
                animatables.forEachIndexed { index, anim ->
                    launch {
                        delay(index * (staggerMs * 2 / 3).toLong())
                        anim.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = (durationMs * 0.75f).toInt(),
                                easing = GSAPPower3In
                            )
                        )
                    }
                }
            }

            delay(350L)
        }
    }

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            replayKey++
        },
        horizontalArrangement = Arrangement.spacedBy(letterSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            if (char == ' ') {
                Spacer(modifier = Modifier.width(letterSpacing * 3))
            } else {
                val progress = animatables.getOrNull(index)?.value ?: 1f
                val rotationX = (1f - progress) * hinge.initialRotateX
                val rotationY = (1f - progress) * hinge.initialRotateY
                val alpha = progress.coerceIn(0f, 1f)
                val currentCrease = (1f - progress).coerceIn(0f, 1f) * safeCrease

                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            this.transformOrigin = hinge.transformOrigin
                            this.cameraDistance = 14f * density
                            this.rotationX = rotationX
                            this.rotationY = rotationY
                            this.alpha = alpha
                            this.compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            if (currentCrease > 0.005f) {
                                val creaseBrush = when (hinge) {
                                    FoldHinge.TOP -> Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = (currentCrease * 0.9f).coerceIn(0f, 1f)),
                                            Color.Black.copy(alpha = (currentCrease * 0.35f).coerceIn(0f, 1f)),
                                            Color.Transparent
                                        ),
                                        startY = 0f,
                                        endY = size.height * 0.85f
                                    )
                                    FoldHinge.BOTTOM -> Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = (currentCrease * 0.35f).coerceIn(0f, 1f)),
                                            Color.Black.copy(alpha = (currentCrease * 0.9f).coerceIn(0f, 1f))
                                        ),
                                        startY = size.height * 0.15f,
                                        endY = size.height
                                    )
                                    FoldHinge.LEFT -> Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = (currentCrease * 0.9f).coerceIn(0f, 1f)),
                                            Color.Transparent
                                        ),
                                        startX = 0f,
                                        endX = size.width * 0.85f
                                    )
                                    FoldHinge.RIGHT -> Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = (currentCrease * 0.9f).coerceIn(0f, 1f))
                                        ),
                                        startX = size.width * 0.15f,
                                        endX = size.width
                                    )
                                }
                                drawRect(
                                    brush = creaseBrush,
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.toString(),
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        fontSize = fontSize,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
