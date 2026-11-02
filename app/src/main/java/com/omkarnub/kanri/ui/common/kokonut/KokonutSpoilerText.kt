package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * 1:1 Port of Kokonut UI Spoiler Text (A7. spoiler-text.tsx)
 *
 * Exact constants:
 * DENSITY = 1 / 6 specks per px²
 * MAX_SPECKS = 2400
 * DRIFT = 7 px/s
 * WAVE_MS = 520 ms
 * FADE_MS = 360 ms
 * THROW = 70 px
 * EASING = 450ms CubicBezierEasing(0.23, 1, 0.32, 1)
 */

const val SPOILER_DENSITY = 1f / 6f
const val SPOILER_MAX_SPECKS = 2400
const val SPOILER_DRIFT = 7f
const val SPOILER_WAVE_MS = 520f
const val SPOILER_FADE_MS = 360f
const val SPOILER_THROW = 70f

val SPOILER_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

fun spoilerFarthestCorner(at: Offset, w: Float, h: Float): Float {
    return max(
        max(hypot(at.x, at.y), hypot(w - at.x, at.y)),
        max(hypot(at.x, h - at.y), hypot(w - at.x, h - at.y))
    )
}

fun spoilerEaseOut(t: Float): Float = 1f - (1f - t.coerceIn(0f, 1f)).pow(3)

class SpoilerSpeck(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var phase: Float,
    var rate: Float,
    var size: Float,
    var hitTime: Long = 0L
)

@Composable
fun KokonutSpoilerText(
    modifier: Modifier = Modifier,
    isSpoilerActive: Boolean = true,
    initialRevealed: Boolean = false,
    onRevealChange: ((Boolean) -> Unit)? = null,
    speckColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    if (!isSpoilerActive) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            content()
        }
        return
    }

    val dark = isAppInDarkTheme()
    val haptics = rememberKanriHaptics()
    var isRevealed by remember(initialRevealed) { mutableStateOf(initialRevealed) }
    var isClearing by remember { mutableStateOf(false) }

    var waveOrigin by remember { mutableStateOf<Offset?>(null) }
    var waveStartTime by remember { mutableLongStateOf(0L) }

    val blurRadius = remember { Animatable(if (initialRevealed) 0f else 6f) }
    val textAlpha = remember { Animatable(if (initialRevealed) 1f else 0f) }

    val specks = remember { mutableListOf<SpoilerSpeck>() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var frameTick by remember { mutableLongStateOf(0L) }

    val showSpecks = !isRevealed || isClearing

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            blurRadius.animateTo(0f, tween(durationMillis = 450, easing = SPOILER_EASE_OUT))
            textAlpha.animateTo(1f, tween(durationMillis = 450, easing = SPOILER_EASE_OUT))
        } else {
            blurRadius.animateTo(6f, tween(durationMillis = 200))
            textAlpha.animateTo(0f, tween(durationMillis = 200))
            isClearing = false
            waveOrigin = null
        }
    }

    LaunchedEffect(showSpecks) {
        if (!showSpecks) return@LaunchedEffect
        var lastTime = System.currentTimeMillis()
        while (showSpecks) {
            withFrameNanos { _ ->
                val now = System.currentTimeMillis()
                val dt = min(0.05f, (now - lastTime) / 1000f)
                lastTime = now

                val w = canvasSize.width
                val h = canvasSize.height
                if (w > 0 && h > 0) {
                    val wave = waveOrigin
                    val waveProgressMs = if (wave != null) (now - waveStartTime).toFloat() else 0f
                    val waveT = (waveProgressMs / SPOILER_WAVE_MS).coerceIn(0f, 1f)
                    val waveRadius = if (wave != null) {
                        val maxR = spoilerFarthestCorner(wave, w, h)
                        maxR * spoilerEaseOut(waveT)
                    } else -1f

                    var aliveCount = 0
                    for (s in specks) {
                        if (wave != null && s.hitTime == 0L && hypot(s.x - wave.x, s.y - wave.y) <= waveRadius) {
                            s.hitTime = now
                            val dx = s.x - wave.x
                            val dy = s.y - wave.y
                            val length = max(1f, hypot(dx, dy))
                            val push = SPOILER_THROW * (0.5f + Random.nextFloat())
                            s.vx += (dx / length) * push
                            s.vy += (dy / length) * push
                        }

                        val fade = if (s.hitTime != 0L) {
                            1f - (now - s.hitTime).toFloat() / SPOILER_FADE_MS
                        } else 1f

                        if (fade > 0f) {
                            aliveCount++
                            s.x += s.vx * dt
                            s.y += s.vy * dt
                            s.phase = (s.phase + s.rate * dt) % 1f

                            if (s.hitTime == 0L) {
                                if (s.x < 0f || s.x > w) {
                                    s.vx = -s.vx
                                    s.x = s.x.coerceIn(0f, w)
                                }
                                if (s.y < 0f || s.y > h) {
                                    s.vy = -s.vy
                                    s.y = s.y.coerceIn(0f, h)
                                }
                            }
                        }
                    }

                    if (wave != null && aliveCount == 0) {
                        isClearing = false
                        waveOrigin = null
                    }
                    frameTick = now
                }
            }
        }
    }

    val chipBg = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val resolvedSpeckColor = speckColor ?: MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(if (isRevealed) Color.Transparent else chipBg)
            .semantics {
                role = Role.Button
                contentDescription = if (isRevealed) "Revealed balance" else "Hidden text. Activate to reveal."
            }
            .pointerInput(isRevealed) {
                detectTapGestures { offset ->
                    if (!isRevealed) {
                        haptics.click()
                        waveOrigin = offset
                        waveStartTime = System.currentTimeMillis()
                        isClearing = true
                        isRevealed = true
                        onRevealChange?.invoke(true)
                    }
                }
            }
            .onSizeChanged { size ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                canvasSize = Size(w, h)
                if (w > 0 && h > 0 && specks.isEmpty()) {
                    val area = w * h
                    val scale = min(1f, SPOILER_MAX_SPECKS.toFloat() / max(1f, area * SPOILER_DENSITY))
                    val count = (area * SPOILER_DENSITY * scale).toInt()
                    specks.clear()
                    for (i in 0 until count) {
                        val angle = Random.nextFloat() * 2f * PI.toFloat()
                        val speed = SPOILER_DRIFT * (0.3f + Random.nextFloat() * 0.7f)
                        specks.add(
                            SpoilerSpeck(
                                x = Random.nextFloat() * w,
                                y = Random.nextFloat() * h,
                                vx = cos(angle) * speed,
                                vy = sin(angle) * speed,
                                phase = Random.nextFloat(),
                                rate = 0.35f + Random.nextFloat() * 0.9f,
                                size = if (Random.nextFloat() < 0.18f) 1.5f else 1.0f
                            )
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Obfuscated content
        Box(
            modifier = Modifier
                .blur(blurRadius.value.dp)
                .graphicsLayer {
                    alpha = textAlpha.value
                }
        ) {
            content()
        }

        // Shimmering specks canvas
        if (showSpecks) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dummy = frameTick // Triggers recomposition on frame tick
                for (s in specks) {
                    val now = System.currentTimeMillis()
                    val fade = if (s.hitTime != 0L) {
                        (1f - (now - s.hitTime).toFloat() / SPOILER_FADE_MS).coerceIn(0f, 1f)
                    } else 1f

                    if (fade > 0f) {
                        val twinkle = 0.35f + 0.65f * abs(sin(s.phase * PI.toFloat()))
                        val alpha = twinkle * fade * 0.9f
                        drawRect(
                            color = resolvedSpeckColor.copy(alpha = alpha),
                            topLeft = Offset(s.x, s.y),
                            size = Size(s.size * density, s.size * density)
                        )
                    }
                }
            }
        }
    }
}
