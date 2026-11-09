package com.omkarnub.kanri.widget.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for the Kanri Pixel DotTiles design system palette & metrics.
 * No hardcoded colors or dimensions are permitted outside this theme.
 */
object PixelTheme {

    // --- Android 32-bit ARGB Color Ints (Canvas rendering) ---
    const val COLOR_BG_INT: Int = 0xFF08080A.toInt()
    const val COLOR_BORDER_INT: Int = 0x24FFFFFF
    const val COLOR_DOT_ON_INT: Int = 0xFFF2F2F5.toInt()
    const val COLOR_DOT_DIM_INT: Int = 0xFF1E1E24.toInt()
    const val COLOR_DOT_MID_INT: Int = 0xFF6B6B76.toInt()

    // State Accents (Canvas)
    const val COLOR_MINT_INT: Int = 0xFF30D158.toInt()
    const val COLOR_AMBER_INT: Int = 0xFFFFB020.toInt()
    const val COLOR_CORAL_INT: Int = 0xFFFF453A.toInt()
    const val COLOR_CYAN_INT: Int = 0xFF4DD0E1.toInt()

    // --- Jetpack Compose Colors ---
    val ColorBg = Color(0xFF08080A)
    val ColorBorder = Color(0x24FFFFFF)
    val ColorDotOn = Color(0xFFF2F2F5)
    val ColorDotDim = Color(0xFF1E1E24)
    val ColorDotMid = Color(0xFF6B6B76)

    // State Accents (Compose)
    val ColorMint = Color(0xFF30D158)
    val ColorAmber = Color(0xFFFFB020)
    val ColorCoral = Color(0xFFFF453A)
    val ColorCyan = Color(0xFF4DD0E1)

    // --- Dimensions & Spacing ---
    val CornerRadius = 22.dp
    val Padding = 14.dp

    // Pitches (dot-to-dot distance in px / dp)
    const val PITCH_HERO_LARGE = 14f
    const val PITCH_HERO_MEDIUM = 11f
    const val PITCH_LABEL = 6f
    const val PITCH_VISUAL = 8f
    const val PITCH_HEATMAP = 9f
    const val PITCH_TIMELINE = 7f
}
