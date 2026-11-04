package com.omkarnub.kanri.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GlassBlob(
    val color: Color,
    val center: Offset, // Fraction of size, e.g. Offset(0.2f, 0.2f)
    val radius: Dp
)

@Immutable
data class KanriGlassTokens(
    val isDark: Boolean,
    // Backdrop
    val backdropGradient: List<Color>,        // vertical stops, top to bottom
    val blobs: List<GlassBlob>,               // radial ambient glows
    val grainAlpha: Float,                    // 0.03 - 0.05
    // Surfaces
    val blurRadius: Dp,
    val tintBlur: Color,                      // fill over blurred backdrop
    val tintFlat: Color,                      // fill when no blur (list rows, chips, inputs)
    val tintStrong: Color,                    // dialogs, sheets, popups
    val fallbackSurface: Color,               // no-blur fallback (API < 31, reduce transparency)
    val borderStart: Color,
    val borderEnd: Color,
    val edgeHairline: Color,                  // outer hairline separator (crucial on light glass)
    val shadow: Color,
    // Content
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    val credit: Color,
    val debit: Color,                         // semantic money colors
    val track: Color,                         // gauge / chart tracks
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 20.dp,
    val buttonOpacity: Float = 0.16f,
    val expenseColor: Color = Color(0xFFE54D2E),
    val incomeColor: Color = Color(0xFF30A46C),
    val expenseTintOpacity: Float = 0.18f,
    val incomeTintOpacity: Float = 0.18f,
    val tintLightness: Float = 0.0f
)

val LocalKanriGlass = staticCompositionLocalOf<KanriGlassTokens?> { null }
val LocalIsDarkTheme = staticCompositionLocalOf<Boolean> { true }

/**
 * Returns true if the active app theme is dark (DARK, AMOLED, or GLASS with dark background).
 * Never relies directly on system OS dark theme when user has explicitly selected a theme.
 */
@Composable
fun isAppInDarkTheme(): Boolean = LocalIsDarkTheme.current


/**
 * User-configurable factors for customizing glass cards, components, and buttons.
 */
@Immutable
data class GlassCustomization(
    val blurRadiusDp: Float = 24f,
    val surfaceOpacity: Float = 0.08f,
    val tintLightness: Float = 0.0f,     // -1.0f (darkest charcoal) .. +1.0f (lightest white frost)
    val borderOpacity: Float = 0.28f,
    val borderWidthDp: Float = 1.0f,
    val cornerRadiusDp: Float = 20f,
    val buttonOpacity: Float = 0.16f,
    val shadowIntensity: Float = 0.25f,
    val expenseColorHex: Long = 0xFFE54D2EL,
    val incomeColorHex: Long = 0xFF30A46CL,
    val expenseTintOpacity: Float = 0.18f,
    val incomeTintOpacity: Float = 0.18f
) {
    val rawExpenseColor: Color get() = Color(expenseColorHex)
    val rawIncomeColor: Color get() = Color(incomeColorHex)
    val expenseColor: Color get() = computeAdaptedAccentColor(rawExpenseColor, tintLightness)
    val incomeColor: Color get() = computeAdaptedAccentColor(rawIncomeColor, tintLightness)
}

object GlassPresets {
    val DEFAULT = GlassCustomization(
        blurRadiusDp = 24f,
        surfaceOpacity = 0.08f,
        tintLightness = 0.0f,
        borderOpacity = 0.28f,
        borderWidthDp = 1.0f,
        cornerRadiusDp = 20f,
        buttonOpacity = 0.16f,
        shadowIntensity = 0.25f,
        expenseColorHex = 0xFFE54D2EL,
        incomeColorHex = 0xFF30A46CL,
        expenseTintOpacity = 0.18f,
        incomeTintOpacity = 0.18f
    )
    val ULTRA_SHEER = GlassCustomization(
        blurRadiusDp = 36f,
        surfaceOpacity = 0.03f,
        tintLightness = 0.15f,
        borderOpacity = 0.16f,
        borderWidthDp = 0.8f,
        cornerRadiusDp = 24f,
        buttonOpacity = 0.08f,
        shadowIntensity = 0.15f,
        expenseColorHex = 0xFFF43F5EL,
        incomeColorHex = 0xFF10B981L,
        expenseTintOpacity = 0.10f,
        incomeTintOpacity = 0.10f
    )
    val DEEP_OBSIDIAN = GlassCustomization(
        blurRadiusDp = 20f,
        surfaceOpacity = 0.20f,
        tintLightness = -0.75f,
        borderOpacity = 0.35f,
        borderWidthDp = 1.0f,
        cornerRadiusDp = 18f,
        buttonOpacity = 0.22f,
        shadowIntensity = 0.35f,
        expenseColorHex = 0xFFDC2626L,
        incomeColorHex = 0xFF059669L,
        expenseTintOpacity = 0.25f,
        incomeTintOpacity = 0.25f
    )
    val LUMINOUS_FROST = GlassCustomization(
        blurRadiusDp = 28f,
        surfaceOpacity = 0.16f,
        tintLightness = 0.70f,
        borderOpacity = 0.42f,
        borderWidthDp = 1.2f,
        cornerRadiusDp = 22f,
        buttonOpacity = 0.24f,
        shadowIntensity = 0.20f,
        expenseColorHex = 0xFFFB7185L,
        incomeColorHex = 0xFF34D399L,
        expenseTintOpacity = 0.22f,
        incomeTintOpacity = 0.22f
    )
    val NEON_CRYSTAL = GlassCustomization(
        blurRadiusDp = 14f,
        surfaceOpacity = 0.05f,
        tintLightness = 0.0f,
        borderOpacity = 0.58f,
        borderWidthDp = 1.6f,
        cornerRadiusDp = 16f,
        buttonOpacity = 0.32f,
        shadowIntensity = 0.30f,
        expenseColorHex = 0xFFFF3366L,
        incomeColorHex = 0xFF84CC16L,
        expenseTintOpacity = 0.32f,
        incomeTintOpacity = 0.32f
    )
}

fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * f,
        green = start.green + (stop.green - start.green) * f,
        blue = start.blue + (stop.blue - start.blue) * f,
        alpha = start.alpha + (stop.alpha - start.alpha) * f
    )
}

fun computeAdaptedAccentColor(baseColor: Color, tintLightness: Float): Color {
    return if (tintLightness < 0f) {
        val darkFactor = (-tintLightness).coerceIn(0f, 1f)
        val deepDarkTarget = Color(
            red = baseColor.red * 0.45f,
            green = baseColor.green * 0.45f,
            blue = baseColor.blue * 0.45f,
            alpha = baseColor.alpha
        )
        lerpColor(baseColor, deepDarkTarget, darkFactor * 0.70f)
    } else {
        val lightFactor = tintLightness.coerceIn(0f, 1f)
        val brightTarget = Color(
            red = (baseColor.red * 1.18f).coerceAtMost(1f),
            green = (baseColor.green * 1.18f).coerceAtMost(1f),
            blue = (baseColor.blue * 1.18f).coerceAtMost(1f),
            alpha = baseColor.alpha
        )
        lerpColor(baseColor, brightTarget, lightFactor * 0.25f)
    }
}

fun createCustomGlassTokens(c: GlassCustomization): KanriGlassTokens {
    val lightness = c.tintLightness.coerceIn(-1.0f, 1.0f)
    val darkBase = Color(0xFF0A0B10)
    val whiteBase = Color(0xFFFFFFFF)

    // Tint calculation:
    // Middle (0.0f): Neutral crystal glass with no dark or light tint cast
    // Sliding left (< 0.0f): Dark tint (smoky obsidian glass)
    // Sliding right (> 0.0f): Light tint (luminous white frost glass)
    val tintBlur: Color
    val tintFlat: Color
    val tintStrong: Color
    val fallbackSurface: Color

    when {
        lightness < -0.04f -> {
            val d = (-lightness).coerceIn(0f, 1f)
            val base = lerpColor(whiteBase, darkBase, (0.4f + d * 0.6f))
            tintBlur = base.copy(alpha = (0.06f + d * 0.40f).coerceIn(0.04f, 0.65f))
            tintFlat = Color.Black.copy(alpha = (0.08f + d * 0.35f).coerceIn(0.05f, 0.50f))
            tintStrong = Color(0xFF0C0D14).copy(alpha = (0.28f + d * 0.55f).coerceIn(0.20f, 0.90f))
            fallbackSurface = Color(0xFF0A0B10).copy(alpha = (0.80f + d * 0.16f).coerceIn(0.70f, 0.96f))
        }
        lightness > 0.04f -> {
            val l = lightness.coerceIn(0f, 1f)
            tintBlur = Color.White.copy(alpha = (0.05f + l * 0.38f).coerceIn(0.04f, 0.60f))
            tintFlat = Color.White.copy(alpha = (0.08f + l * 0.30f).coerceIn(0.06f, 0.45f))
            tintStrong = Color.White.copy(alpha = (0.22f + l * 0.50f).coerceIn(0.18f, 0.85f))
            fallbackSurface = Color(0xFFFAF9F5).copy(alpha = (0.80f + l * 0.16f).coerceIn(0.70f, 0.96f))
        }
        else -> {
            // Neutral / No tint: pure clear glass with no dark or light cast
            tintBlur = Color.White.copy(alpha = 0.08f)
            tintFlat = Color.White.copy(alpha = 0.08f)
            tintStrong = Color.White.copy(alpha = 0.16f)
            fallbackSurface = Color(0xFF14151B).copy(alpha = 0.80f)
        }
    }

    // In glass mode, borders are specular white rim reflections (never dark gray or black)
    val borderBase = Color.White
    val borderStart = borderBase.copy(alpha = c.borderOpacity.coerceIn(0.0f, 0.85f))
    val borderEnd = borderBase.copy(alpha = (c.borderOpacity * 0.25f).coerceIn(0.0f, 0.35f))
    val shadow = Color.Black.copy(alpha = 0.035f)

    return GlassTokens.copy(
        blurRadius = c.blurRadiusDp.coerceIn(0f, 60f).dp,
        tintBlur = tintBlur,
        tintFlat = tintFlat,
        tintStrong = tintStrong,
        fallbackSurface = fallbackSurface,
        borderStart = borderStart,
        borderEnd = borderEnd,
        shadow = shadow,
        borderWidth = (c.borderWidthDp * 0.8f).coerceIn(0.6f, 1.0f).dp,
        cornerRadius = c.cornerRadiusDp.coerceIn(8f, 40f).dp,
        buttonOpacity = c.buttonOpacity.coerceIn(0.04f, 0.50f),
        tintLightness = lightness,
        expenseColor = computeAdaptedAccentColor(Color(c.expenseColorHex), lightness),
        incomeColor = computeAdaptedAccentColor(Color(c.incomeColorHex), lightness),
        expenseTintOpacity = c.expenseTintOpacity.coerceIn(0.04f, 0.50f),
        incomeTintOpacity = c.incomeTintOpacity.coerceIn(0.04f, 0.50f)
    )
}

/**
 * Curated Glass Mode Token Palette
 * Restrained, deep, luminous monochrome charcoal and frosted platinum glass.
 */
val GlassTokens = KanriGlassTokens(
    isDark = true,
    backdropGradient = listOf(
        Color(0xFF0B0B0E),
        Color(0xFF121216),
        Color(0xFF08080A)
    ),
    blobs = listOf(
        GlassBlob(
            color = Color.White.copy(alpha = 0.05f),
            center = Offset(0.12f, 0.16f),
            radius = 340.dp
        ),
        GlassBlob(
            color = Color.White.copy(alpha = 0.04f),
            center = Offset(0.88f, 0.82f),
            radius = 380.dp
        ),
        GlassBlob(
            color = Color.White.copy(alpha = 0.03f),
            center = Offset(0.92f, 0.44f),
            radius = 220.dp
        )
    ),
    grainAlpha = 0.035f,
    blurRadius = 24.dp,
    tintBlur = Color.White.copy(alpha = 0.08f),
    tintFlat = Color.White.copy(alpha = 0.05f),
    tintStrong = Color.White.copy(alpha = 0.12f),
    fallbackSurface = Color.White.copy(alpha = 0.08f),
    borderStart = Color.White.copy(alpha = 0.25f),
    borderEnd = Color.White.copy(alpha = 0.05f),
    edgeHairline = Color.Transparent,
    shadow = Color.Black.copy(alpha = 0.035f),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFFF5F5F7).copy(alpha = 0.70f),
    textTertiary = Color(0xFFF5F5F7).copy(alpha = 0.44f),
    accent = Color(0xFFF5F5F7),
    onAccent = Color(0xFF121214),
    credit = Color(0xFFFFFFFF),
    debit = Color(0xFFC8C8D2),
    track = Color.White.copy(alpha = 0.10f),
    borderWidth = 0.8.dp,
    cornerRadius = 20.dp,
    buttonOpacity = 0.16f,
    expenseColor = Color(0xFFE54D2E),
    incomeColor = Color(0xFF30A46C),
    expenseTintOpacity = 0.18f,
    incomeTintOpacity = 0.18f
)

val GlassDarkTokens = GlassTokens
val GlassLightTokens = GlassTokens

