package com.omkarnub.kanri.ui.common.agsl

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ShaderBrush
import com.omkarnub.kanri.ui.common.GlassBackgroundType

/**
 * Hardware-accelerated native AGSL background composable for Android 13+ (API 33+).
 * Executes in the app's Skia RenderNode pipeline at full display refresh rate (60/90/120 FPS)
 * without Chromium or WebView IPC.
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

    val infiniteTransition = rememberInfiniteTransition(label = "agslShaderTransition")
    val timeVal by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "agslTime"
    )

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
