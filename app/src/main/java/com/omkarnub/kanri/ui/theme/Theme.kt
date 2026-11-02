package com.omkarnub.kanri.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Minimalist Dark Palette (Base #121212) - Pure Material Charcoal with Off-White */
private val KanriDarkColorScheme = darkColorScheme(
    primary = DarkOffWhite,
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2C2C2C),
    onPrimaryContainer = DarkOffWhite,
    secondary = Color(0xFFE2E2E2),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color(0xFFE2E2E2),
    tertiary = Color(0xFFCCCCCC),
    onTertiary = Color(0xFF121212),
    tertiaryContainer = Color(0xFF1A1A1A),
    onTertiaryContainer = Color(0xFFCCCCCC),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceDim = Color(0xFF121212),
    surfaceBright = Color(0xFF2C2C2C),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

/** AMOLED Dark Mode Palette (Base #000000) - Pure Black & Off-White Minimalism */
private val KanriAmoledColorScheme = darkColorScheme(
    primary = DarkOffWhite,
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = DarkOffWhite,
    secondary = Color(0xFFE0E0E0),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFFCCCCCC),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF181818),
    onTertiaryContainer = Color(0xFFCCCCCC),
    background = AmoledBackground,
    onBackground = AmoledOnBackground,
    surface = AmoledSurface,
    onSurface = AmoledOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledOnSurfaceVariant,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceDim = Color(0xFF000000),
    surfaceBright = Color(0xFF242424),
    outline = AmoledOutline,
    outlineVariant = AmoledOutlineVariant
)

/** Light Monochrome Palette (Refined Lighter Soft Cream) */
private val KanriLightColorScheme = lightColorScheme(
    primary = MonochromeBlack,
    onPrimary = CreamSplashBackground,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = MonochromeBlack,
    secondary = Color(0xFF1F1E1A),
    onSecondary = CreamSplashBackground,
    secondaryContainer = Color(0xFFE6E3DB),
    onSecondaryContainer = MonochromeBlack,
    tertiary = Color(0xFF3B3931),
    onTertiary = CreamSplashBackground,
    tertiaryContainer = Color(0xFFDFDCD4),
    onTertiaryContainer = MonochromeBlack,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = Color(0xFFEFECE4),
    surfaceContainerHighest = Color(0xFFE6E3DB),
    surfaceContainerLow = Color(0xFFF7F5EF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDFDCD4),
    surfaceBright = LightBackground,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

/** Glass Scheme (Transparent background & surfaces so backdrop and glass cards render cleanly) */
private val KanriGlassColorScheme = darkColorScheme(
    primary = GlassTokens.accent,
    onPrimary = GlassTokens.onAccent,
    primaryContainer = Color.White.copy(alpha = 0.12f),
    onPrimaryContainer = GlassTokens.accent,
    secondary = GlassTokens.textSecondary,
    onSecondary = GlassTokens.textPrimary,
    secondaryContainer = Color.White.copy(alpha = 0.08f),
    onSecondaryContainer = GlassTokens.textPrimary,
    tertiary = GlassTokens.credit,
    onTertiary = GlassTokens.onAccent,
    tertiaryContainer = Color.White.copy(alpha = 0.08f),
    onTertiaryContainer = GlassTokens.credit,
    background = Color.Transparent,
    onBackground = GlassTokens.textPrimary,
    surface = Color.White.copy(alpha = 0.07f),
    onSurface = GlassTokens.textPrimary,
    surfaceVariant = Color.White.copy(alpha = 0.09f),
    onSurfaceVariant = GlassTokens.textSecondary,
    surfaceContainer = Color.White.copy(alpha = 0.07f),
    surfaceContainerHigh = Color.White.copy(alpha = 0.10f),
    surfaceContainerHighest = Color.White.copy(alpha = 0.14f),
    surfaceContainerLow = Color.White.copy(alpha = 0.05f),
    surfaceContainerLowest = Color.White.copy(alpha = 0.03f),
    surfaceDim = Color.White.copy(alpha = 0.04f),
    surfaceBright = Color.White.copy(alpha = 0.12f),
    inverseSurface = Color(0xFFEDEDED),
    inverseOnSurface = Color(0xFF111114),
    outline = GlassTokens.borderStart,
    outlineVariant = GlassTokens.borderEnd
)

@Composable
fun KanriTheme(
    themeMode: ThemeMode? = null,
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePrefs = ThemePreferences.getInstance(context)

    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val isDynamicEnabled by themePrefs.dynamicColorFlow.collectAsState()
    val glassCustomization by themePrefs.glassCustomizationFlow.collectAsState()

    val effectiveThemeMode = themeMode ?: currentThemeMode
    val effectiveDynamicColor = dynamicColor ?: isDynamicEnabled

    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val resolvedThemeMode = when (effectiveThemeMode) {
        ThemeMode.SYSTEM -> if (systemDark) ThemeMode.DARK else ThemeMode.LIGHT
        else -> effectiveThemeMode
    }

    val glassTokens = if (resolvedThemeMode == ThemeMode.GLASS) {
        remember(glassCustomization) {
            createCustomGlassTokens(glassCustomization)
        }
    } else null

    val isDark = when (resolvedThemeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.GLASS -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val dynamicGlassColorScheme = remember(glassTokens) {
        val t = glassTokens ?: GlassTokens
        darkColorScheme(
            primary = t.accent,
            onPrimary = t.onAccent,
            primaryContainer = t.tintFlat,
            onPrimaryContainer = t.accent,
            secondary = t.textSecondary,
            onSecondary = t.textPrimary,
            secondaryContainer = t.tintFlat,
            onSecondaryContainer = t.textPrimary,
            tertiary = t.credit,
            onTertiary = t.onAccent,
            tertiaryContainer = t.tintFlat,
            onTertiaryContainer = t.credit,
            background = Color.Transparent,
            onBackground = t.textPrimary,
            surface = t.fallbackSurface,
            onSurface = t.textPrimary,
            surfaceVariant = t.tintFlat,
            onSurfaceVariant = t.textSecondary,
            surfaceContainer = t.tintBlur,
            surfaceContainerHigh = t.tintStrong,
            surfaceContainerHighest = t.tintStrong,
            surfaceContainerLow = t.tintFlat,
            surfaceContainerLowest = t.tintFlat,
            surfaceDim = t.fallbackSurface,
            surfaceBright = t.tintStrong,
            outline = t.borderStart,
            outlineVariant = t.borderEnd
        )
    }

    val colorScheme = when {
        glassTokens != null -> dynamicGlassColorScheme
        effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        resolvedThemeMode == ThemeMode.AMOLED -> KanriAmoledColorScheme
        resolvedThemeMode == ThemeMode.DARK -> KanriDarkColorScheme
        else -> KanriLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark

                val windowBackdrop = when (resolvedThemeMode) {
                    ThemeMode.GLASS -> android.graphics.Color.parseColor("#0B0B0E")
                    ThemeMode.AMOLED -> android.graphics.Color.BLACK
                    ThemeMode.DARK -> android.graphics.Color.parseColor("#121212")
                    ThemeMode.LIGHT -> android.graphics.Color.parseColor("#FEFAEC")
                    ThemeMode.SYSTEM -> if (!isDark) android.graphics.Color.parseColor("#FEFAEC") else android.graphics.Color.parseColor("#121212")
                }
                window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(windowBackdrop))
            }
        }
    }

    CompositionLocalProvider(LocalKanriGlass provides glassTokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}