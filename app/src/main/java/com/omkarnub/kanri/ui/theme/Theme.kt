package com.omkarnub.kanri.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    val effectiveThemeMode = themeMode ?: currentThemeMode
    val effectiveDynamicColor = dynamicColor ?: isDynamicEnabled

    val isDark = when (effectiveThemeMode) {
        ThemeMode.DARK, ThemeMode.AMOLED -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = when {
        effectiveDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        effectiveThemeMode == ThemeMode.AMOLED -> KanriAmoledColorScheme
        effectiveThemeMode == ThemeMode.DARK -> KanriDarkColorScheme
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
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}