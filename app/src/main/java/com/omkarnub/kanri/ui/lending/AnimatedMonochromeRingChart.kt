package com.omkarnub.kanri.ui.lending

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Curated high-contrast luxury monochrome tones for the ring chart segments.
 * Alternates in luminance so adjacent segments are distinctly separated.
 */
private val DarkRingTones = listOf(
    Color(0xFFFFFFFF), // 0: Radiant Pure White (You / Primary segment)
    Color(0xFF484C58), // 1: Slate Titanium
    Color(0xFFD4D8E4), // 2: Polished Platinum
    Color(0xFF282A32), // 3: Deep Obsidian
    Color(0xFFA0A5B4), // 4: Cool Silver
    Color(0xFF383B46), // 5: Graphite Charcoal
    Color(0xFFB8BFCE), // 6: Frosted Nickel
    Color(0xFF1E2026)  // 7: Midnight Carbon
)

private val LightRingTones = listOf(
    Color(0xFF141518), // 0: Deep Carbon (You / Primary segment)
    Color(0xFF8C92A0), // 1: Slate Steel
    Color(0xFFFFFFFF), // 2: Pure White
    Color(0xFF343842), // 3: Charcoal
    Color(0xFFD6DBE4), // 4: Silver Mist
    Color(0xFF5A606E), // 5: Titanium
    Color(0xFFF0F2F6), // 6: Platinum Grey
    Color(0xFF202227)  // 7: Obsidian
)

/**
 * A sleek, fully-animated monochrome ring (donut) chart.
 * Segments smoothly interpolate when participant count changes, and
 * expand radially when the split animation triggers.
 */
@Composable
fun AnimatedMonochromeRingChart(
    peopleCount: Int,
    perPersonShare: Double,
    totalAmount: Double,
    splitProgress: Float, // 0f = unified cohesive ring, 1f = exploded radial aperture
    modifier: Modifier = Modifier,
    sizeDp: Dp = 260.dp,
    strokeWidthDp: Dp = 20.dp
) {
    val isDark = isSystemInDarkTheme()
    val ringPalette = if (isDark) DarkRingTones else LightRingTones
    val count = peopleCount.coerceAtLeast(1)

    // Smooth continuous floating physics
    val infiniteTransition = rememberInfiniteTransition(label = "ring_levitation")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Subtle slow rotation shimmer
    val ambientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambientRotation"
    )

    // Breathing glow
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = CubicBezierEasing(0.4f, 0f, 0.6f, 1f)),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    // Animated interpolation for people count transitions
    val animatedCount by animateFloatAsState(
        targetValue = count.toFloat(),
        animationSpec = tween(durationMillis = 350, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "animatedCount"
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp)) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val currentFloatY = floatY * (1f - splitProgress * 0.4f)
            val centerY = height / 2f + currentFloatY

            val strokeWidthPx = strokeWidthDp.toPx()
            val ringRadius = (minOf(width, height) - strokeWidthPx) / 2f * 0.88f

            // 1. Soft Dynamic Floor Shadow
            val shadowY = height * 0.90f
            val shadowWidth = ringRadius * 1.8f
            val shadowHeight = 22.dp.toPx()
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        (if (isDark) Color.Black else Color(0xFF1E2024)).copy(alpha = if (isDark) 0.35f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, shadowY),
                    radius = shadowWidth / 2f
                ),
                topLeft = Offset(centerX - shadowWidth / 2f, shadowY - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight)
            )

            // 2. Ambient Underglow around the ring
            val glowColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.04f * glowPulse)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(centerX, centerY),
                    radius = ringRadius * 1.35f
                ),
                radius = ringRadius * 1.35f,
                center = Offset(centerX, centerY)
            )

            // 3. Subtle background track ring
            drawCircle(
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
                radius = ringRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidthPx)
            )

            // 4. Draw Segments of the Ring
            val curCount = animatedCount.coerceAtLeast(1f)
            val sweepPerSegment = 360f / curCount
            val gapAngle = if (curCount > 1.2f) 5.5f else 0f
            val effectiveSweep = (sweepPerSegment - gapAngle).coerceAtLeast(0.5f)

            // Radial explode distance when splitting
            val explodeMaxPx = 28.dp.toPx()
            val currentExplodePx = explodeMaxPx * splitProgress

            for (i in 0 until count) {
                val startAngle = -90f + (i * sweepPerSegment) + (gapAngle / 2f) + (ambientRotation * 0.05f)
                val midAngleRad = ((startAngle + effectiveSweep / 2f) * PI / 180f).toFloat()

                // Radial displacement vector for this ring segment
                val segmentDx = cos(midAngleRad) * currentExplodePx
                val segmentDy = sin(midAngleRad) * currentExplodePx
                val segmentCenter = Offset(centerX + segmentDx, centerY + segmentDy)

                val segmentRect = Size(ringRadius * 2, ringRadius * 2)
                val topLeft = Offset(segmentCenter.x - ringRadius, segmentCenter.y - ringRadius)

                val tone = ringPalette[i % ringPalette.size]

                // Draw Segment Arc with rounded caps for modern elegance
                drawArc(
                    color = tone,
                    startAngle = startAngle,
                    sweepAngle = effectiveSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = segmentRect,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )

                // Specular border highlight along the outer edge
                if (i == 0) {
                    drawArc(
                        color = Color.White.copy(alpha = if (isDark) 0.6f else 0.4f),
                        startAngle = startAngle,
                        sweepAngle = effectiveSweep * 0.5f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = segmentRect,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // ==========================================
        // HOLLOW CENTER: DYNAMIC METRICS
        // ==========================================
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.size(sizeDp * 0.58f)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = if (splitProgress > 0.4f) "EACH PAYS" else if (totalAmount > 0.0) "SHARE" else "SPLIT",
                fontFamily = GoogleSansFlex,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            AnimatedContent(
                targetState = if (totalAmount > 0.0) CurrencyUtils.formatCurrency(perPersonShare) else "₹0",
                transitionSpec = {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                },
                label = "center_amount_anim"
            ) { formattedShare ->
                Text(
                    text = formattedShare,
                    fontFamily = GoogleSansFlex,
                    fontSize = if (formattedShare.length > 8) 22.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$count people",
                fontFamily = GoogleSansFlex,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
