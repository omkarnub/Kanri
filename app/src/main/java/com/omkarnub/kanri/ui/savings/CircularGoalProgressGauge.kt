package com.omkarnub.kanri.ui.savings

import androidx.compose.animation.core.animateFloatAsState
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

@Composable
fun CircularGoalProgressGauge(
    progressPercent: Float,
    modifier: Modifier = Modifier,
    iconKey: String? = null,
    emoji: String = "target",
    size: Dp = 88.dp,
    strokeWidth: Dp = 7.dp,
    primaryColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
) {
    val clampedRatio = (progressPercent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedRatio,
        animationSpec = tween(durationMillis = 800),
        label = "GoalProgressAnimation"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val effectiveKey = if (!iconKey.isNullOrBlank()) iconKey else emoji

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidthPx = strokeWidth.toPx()
            val diameter = size.toPx() - strokeWidthPx
            val radius = diameter / 2f
            val centerOffset = Offset(size.toPx() / 2f, size.toPx() / 2f)

            // Background Track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidthPx)
            )

            // Progress Arc
            if (animatedProgress > 0f) {
                val sweepAngle = animatedProgress * 360f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primaryColor, secondaryColor, primaryColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(
                        width = strokeWidthPx,
                        cap = StrokeCap.Round
                    ),
                    size = Size(diameter, diameter),
                    topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconSize = if (size > 80.dp) 22.dp else 18.dp
            GoalIcon(
                iconKey = effectiveKey,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${progressPercent.toInt()}%",
                fontSize = if (size > 80.dp) 11.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
