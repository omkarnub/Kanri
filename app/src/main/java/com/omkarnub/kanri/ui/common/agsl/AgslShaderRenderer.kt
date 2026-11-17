package com.omkarnub.kanri.ui.common.agsl

import android.graphics.RuntimeShader
import android.os.Build
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Hardware-accelerated native AGSL background composable for Android 13+ (API 33+).
 * Runs at a battery-friendly, thermally efficient 30 FPS update cadence so that
 * user interactions, scroll gestures, and tab navigation execute at a rock-solid 120 FPS.
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

    // Steady 30 FPS timer: eliminates per-VSYNC recomposition and GPU pipeline contention
    var timeVal by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(backgroundType) {
        var lastTime = SystemClock.uptimeMillis()
        while (isActive) {
            delay(33L) // 30 FPS cadence
            val now = SystemClock.uptimeMillis()
            val dt = ((now - lastTime) / 1000f).coerceIn(0f, 0.1f)
            lastTime = now
            timeVal += dt
        }
    }

    val shaderBrush = remember(runtimeShader) {
        ShaderBrush(runtimeShader)
    }

    Spacer(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                runtimeShader.setFloatUniform("u_resolution", size.width, size.height)
                runtimeShader.setFloatUniform("u_time", timeVal)
                drawRect(brush = shaderBrush)
            }
    )
}
