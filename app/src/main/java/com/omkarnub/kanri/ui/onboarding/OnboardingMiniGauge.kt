package com.omkarnub.kanri.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import kotlin.math.roundToInt

/**
 * Compact circular liquid-fill / money-left gauge for the Onboarding Hero Demo.
 * Visibly responds and dips/rises when debit/credit transactions land.
 *
 * Designed with zero per-frame object allocations inside draw scope for 60fps performance.
 */
@Composable
fun OnboardingMiniGauge(
    level: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    strokeWidth: Dp = 6.dp
) {
    val clampedRatio = level.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedRatio,
        animationSpec = tween(
            durationMillis = 650,
            easing = FastOutSlowInEasing
        ),
        label = "MiniGaugeProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val secondaryColor = accentColor.copy(alpha = 0.65f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.toPx() - strokePx
            val radius = diameter / 2f
            val centerOffset = Offset(size.toPx() / 2f, size.toPx() / 2f)

            // Background Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokePx)
            )

            // Reactive Progress Arc
            if (animatedProgress > 0f) {
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(accentColor, secondaryColor, accentColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    ),
                    size = Size(diameter, diameter),
                    topLeft = Offset(strokePx / 2f, strokePx / 2f)
                )
            }
        }

        // Center Quantitative Summary - smoothly tracks animated progress
        val displayPercent = (animatedProgress * 100).roundToInt()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$displayPercent%",
                fontFamily = GoogleSansFlex,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "left",
                fontFamily = GoogleSansFlex,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
