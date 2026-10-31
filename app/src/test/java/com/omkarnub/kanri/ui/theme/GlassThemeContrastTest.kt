package com.omkarnub.kanri.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * JVM unit test validating WCAG contrast compliance for Kanri's Glass Dark and Glass Light themes.
 *
 * Requirements:
 * - Body text >= 4.5:1
 * - Large text & amounts >= 4.5:1 minimum (aiming for 7:1)
 * - Checked against worst-case composites: tintBlur, tintFlat, and tintStrong over sampled backdrop regions.
 */
class GlassThemeContrastTest {

    private fun channelLuminance(c: Float): Double {
        val cd = c.toDouble().coerceIn(0.0, 1.0)
        return if (cd <= 0.04045) {
            cd / 12.92
        } else {
            ((cd + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = channelLuminance(color.red)
        val g = channelLuminance(color.green)
        val b = channelLuminance(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun composite(foreground: Color, background: Color): Color {
        val a = foreground.alpha
        val r = (foreground.red * a + background.red * (1f - a)).coerceIn(0f, 1f)
        val g = (foreground.green * a + background.green * (1f - a)).coerceIn(0f, 1f)
        val b = (foreground.blue * a + background.blue * (1f - a)).coerceIn(0f, 1f)
        return Color(red = r, green = g, blue = b, alpha = 1f)
    }

    private fun contrastRatio(fg: Color, bg: Color): Double {
        // If foreground has alpha, composite it over the surface first
        val effectiveFg = if (fg.alpha < 1f) composite(fg, bg) else fg
        val lum1 = relativeLuminance(effectiveFg)
        val lum2 = relativeLuminance(bg)
        val lighter = maxOf(lum1, lum2)
        val darker = minOf(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testGlassContrastCompliance() {
        val tokens = GlassTokens

        // Sample backdrop gradient stops and blob composite regions
        val backdropSamples = mutableListOf<Color>()
        backdropSamples.addAll(tokens.backdropGradient)
        // Add blob samples over backdrop stops
        for (blob in tokens.blobs) {
            for (bgStop in tokens.backdropGradient) {
                backdropSamples.add(composite(blob.color, bgStop))
            }
        }

        val surfaceTints = listOf(
            "tintBlur" to tokens.tintBlur,
            "tintFlat" to tokens.tintFlat,
            "tintStrong" to tokens.tintStrong
        )

        val elementsToTest = listOf(
            "textPrimary" to tokens.textPrimary,
            "textSecondary" to tokens.textSecondary,
            "credit" to tokens.credit,
            "debit" to tokens.debit,
            "accent" to tokens.accent
        )

        for ((tintName, tint) in surfaceTints) {
            for (sampleBg in backdropSamples) {
                val effectiveSurface = composite(tint, sampleBg)

                for ((elemName, elemColor) in elementsToTest) {
                    val ratio = contrastRatio(elemColor, effectiveSurface)
                    assertTrue(
                        "Glass mode: $elemName on $tintName failed WCAG 4.5:1. Ratio was: ${String.format("%.2f", ratio)}:1",
                        ratio >= 4.5
                    )
                }

                // Verify textPrimary achieves >= 7:1 for prominent stat numbers
                val primaryRatio = contrastRatio(tokens.textPrimary, effectiveSurface)
                assertTrue(
                    "Glass mode: textPrimary on $tintName should achieve >= 7:1. Ratio was: ${String.format("%.2f", primaryRatio)}:1",
                    primaryRatio >= 7.0
                )
            }
        }
    }
}
