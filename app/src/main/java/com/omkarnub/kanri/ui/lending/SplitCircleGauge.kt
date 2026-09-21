package com.omkarnub.kanri.ui.lending

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency

private val SLICE_COLORS = listOf(
    Color(0xFF00B4D8), // Cyan
    Color(0xFF06D6A0), // Emerald
    Color(0xFF8338EC), // Purple
    Color(0xFFFFB703), // Amber
    Color(0xFFE63946), // Crimson
    Color(0xFF3A86FF), // Blue
    Color(0xFFFF5964), // Coral
    Color(0xFF2EC4B6), // Mint
    Color(0xFFFF70A6), // Pink
    Color(0xFFFF9770)  // Peach
)

@Composable
fun SplitCircleGauge(
    totalAmount: Double,
    peopleCount: Int,
    modifier: Modifier = Modifier
) {
    val count = peopleCount.coerceAtLeast(1)
    val amountPerPerson = if (count > 0) totalAmount / count else 0.0

    val animatedSweep by animateFloatAsState(
        targetValue = 360f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "split_circle_anim"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val strokeWidth = 22.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val arcSize = Size(radius * 2, radius * 2)
            val topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius)

            if (totalAmount <= 0.0) {
                // Dimmed placeholder ring when no amount is entered
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth)
                )
            } else {
                val gapAngle = if (count > 1) 4f else 0f
                val totalGaps = count * gapAngle
                val sliceAngle = (360f - totalGaps) / count

                var currentAngle = -90f
                for (i in 0 until count) {
                    val color = SLICE_COLORS[i % SLICE_COLORS.size]
                    val effectiveSweep = (sliceAngle * (animatedSweep / 360f)).coerceAtLeast(0.1f)

                    drawArc(
                        color = color,
                        startAngle = currentAngle,
                        sweepAngle = effectiveSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    currentAngle += sliceAngle + gapAngle
                }
            }
        }

        // Center Typography
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatCurrency(amountPerPerson),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "per person",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = "($count ${if (count == 1) "person" else "people"})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            )
        }
    }
}
