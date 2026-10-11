package com.omkarnub.kanri.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.omkarnub.kanri.ui.theme.AmoledBackground
import com.omkarnub.kanri.ui.theme.DarkBackground
import com.omkarnub.kanri.ui.theme.LightBackground
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import kotlin.math.max

/**
 * Plain luxury dark-mode background with clean aesthetics, zero noise/grain,
 * and an ambient lighting pulse hook.
 *
 * @param modifier Modifier applied to the outer container.
 * @param pulse Value between 0f..1f that gently raises ambient brightness.
 * @param themeMode Optional override for theme; defaults to the user's active theme.
 * @param content Optional content placed inside the background container.
 */
@Composable
fun GrainGradientBackground(
    modifier: Modifier = Modifier,
    pulse: Float = 0f,
    themeMode: ThemeMode? = null,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val effectiveTheme = themeMode ?: currentThemeMode

    val clampedPulse = pulse.coerceIn(0f, 1f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val baseBg = when (effectiveTheme) {
                ThemeMode.AMOLED -> AmoledBackground
                ThemeMode.DARK -> DarkBackground
                ThemeMode.LIGHT -> LightBackground
            }

            // 1. Solid luxury base color (pure AMOLED black in dark mode)
            drawRect(color = baseBg)

            // 2. Subtle, ultra-clean ambient lighting (zero grain, zero noise)
            val center = Offset(w * 0.5f, h * 0.40f)
            val radius = max(w, h) * 0.65f
            val boost = clampedPulse * 0.12f

            when (effectiveTheme) {
                ThemeMode.AMOLED -> {
                    // Crisp AMOLED: pure black base with a subtle soft deep ambient spotlight
                    val glowColor = Color(0xFF16161B).copy(alpha = (0.22f + boost).coerceAtMost(0.40f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                ThemeMode.DARK -> {
                    // Refined dark charcoal ambient spotlight
                    val glowColor = Color(0xFF26262D).copy(alpha = (0.28f + boost).coerceAtMost(0.48f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                ThemeMode.LIGHT -> {
                    // Soft warm cream glow
                    val glowColor = Color(0xFFF3ECE0).copy(alpha = (0.40f + boost).coerceAtMost(0.65f))
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }

            // 3. Subtle ambient brightness pulse on transaction arrival
            if (boost > 0f) {
                val pulseTint = if (effectiveTheme == ThemeMode.LIGHT) Color.White else Color(0xFFD4D4D8)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pulseTint.copy(alpha = boost * 0.25f), Color.Transparent),
                        center = center,
                        radius = radius * 0.8f
                    ),
                    radius = radius * 0.8f,
                    center = center
                )
            }
        }

        content?.invoke(this)
    }
}
