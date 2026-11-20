package com.omkarnub.kanri.ui.common.agsl

import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omkarnub.kanri.MainActivity
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Single constant for background resolution scaling.
 * 0.5x scaling slashes fragment shading by 75% (from 2.6M fragments to 650K on 1080x2400 displays)
 * and upscales via GPU hardware bilinear texture filtering.
 */
private const val SHADER_RESOLUTION_SCALE = 0.5f

/**
 * Hardware-accelerated native AGSL background composable for Android 13+ (API 33+).
 *
 * Implements full Mali GPU optimization:
 * - A: Shader created once with remember(agslSource), cached brush, no allocations in draw path.
 * - B: Phase isolation: time read strictly inside drawWithCache scope.
 * - C: Layer isolation: isolated graphicsLayer with CompositingStrategy.Offscreen so UI draws never dirty the shader.
 * - D: Precomputed aspect ratio uniform (u_aspect) hoisted out of per-pixel math.
 * - E: 0.5x resolution scaling target with GPU bilinear upscale.
 * - H: Paced 30 FPS update cadence paused during touches and flings.
 * - I: Android Lifecycle observation: pauses completely when app is paused/stopped.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun AgslShaderBackground(
    backgroundType: GlassBackgroundType,
    modifier: Modifier = Modifier
) {
    val agslSource = remember(backgroundType) {
        AgslShaderLibrary.getAgslSource(backgroundType)
    } ?: return

    val runtimeShader = remember(agslSource) {
        try {
            RuntimeShader(agslSource)
        } catch (_: Exception) {
            null
        }
    } ?: return

    val lifecycleOwner = LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isResumed = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var timeVal by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(backgroundType, isResumed) {
        if (!isResumed) return@LaunchedEffect
        var lastTime = SystemClock.uptimeMillis()
        while (isActive) {
            delay(33L) // 30 FPS steady cadence
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastTime) / 1000f).coerceIn(0f, 0.1f)
            lastTime = now
            if (!MainActivity.isUserInteracting.value) {
                timeVal += dt
            }
        }
    }

    val shaderBrush = remember(runtimeShader) {
        ShaderBrush(runtimeShader)
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Layer isolation: foreground UI animations/draws will not trigger shader redraws
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .layout { measurable, constraints ->
                val renderW = (constraints.maxWidth * SHADER_RESOLUTION_SCALE).toInt().coerceAtLeast(1)
                val renderH = (constraints.maxHeight * SHADER_RESOLUTION_SCALE).toInt().coerceAtLeast(1)
                val placeable = measurable.measure(Constraints.fixed(renderW, renderH))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    val scaleFactor = 1f / SHADER_RESOLUTION_SCALE
                    placeable.placeRelativeWithLayer(0, 0) {
                        this.scaleX = scaleFactor
                        this.scaleY = scaleFactor
                        this.transformOrigin = TransformOrigin(0f, 0f)
                    }
                }
            }
            .drawWithCache {
                val aspect = size.width / size.height
                onDrawBehind {
                    runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("u_aspect", aspect)
                    runtimeShader.setFloatUniform("u_time", timeVal)
                    drawRect(brush = shaderBrush)
                }
            }
    )
}
