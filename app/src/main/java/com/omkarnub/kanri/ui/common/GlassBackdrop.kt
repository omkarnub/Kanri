package com.omkarnub.kanri.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.omkarnub.kanri.ui.theme.KanriGlassTokens
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.ThemePreferences

/**
 * Curated ambient glass backdrop.
 * Renders the chosen colorful hardware-accelerated animated shader background,
 * layered with a tone-balancing contrast scrim and micro-grain overlay for analogue film texture.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    tokens: KanriGlassTokens? = LocalKanriGlass.current
) {
    if (tokens == null) return

    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val selectedBackground by themePrefs.glassBackgroundFlow.collectAsState()

    // Cached procedural micro-grain shader (64x64 ARGB_8888, ~16KB, tileable)
    val grainShader = remember(tokens.grainAlpha) {
        if (tokens.grainAlpha <= 0f) null
        else {
            val size = 64
            val pixels = IntArray(size * size)
            val random = java.util.Random(42)
            for (i in pixels.indices) {
                val v = random.nextInt(256)
                pixels[i] = android.graphics.Color.argb(
                    (v * tokens.grainAlpha * 2f).toInt().coerceIn(0, 255),
                    v, v, v
                )
            }
            val bmp = android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val shader = ImageShader(
                image = bmp.asImageBitmap(),
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
            ShaderBrush(shader)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Vibrant animated WebGL shader background (non-interactive, paused on lifecycle pause)
        GlassShaderBackground(
            backgroundType = selectedBackground,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Translucent tone-balancing scrim to maintain pristine WCAG contrast and readability
        // Keeps dark glass deep and mysterious, and light glass crisp and luminous.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (tokens.isDark) Color.Black.copy(alpha = 0.20f)
                    else Color.White.copy(alpha = 0.30f)
                )
        )

        // 3. Subtle micro-grain overlay
        if (grainShader != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = grainShader)
            }
        }
    }
}

