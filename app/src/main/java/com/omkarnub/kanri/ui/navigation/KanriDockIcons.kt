package com.omkarnub.kanri.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Home Dock Icon: Clean outline house with peaked roof and doorway.
 * Fluidly scales and gently glides when selected without any bounce.
 */
@Composable
fun HomeDockIcon(
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val animScale = remember { Animatable(1f) }
    val doorProgress = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            launch {
                animScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                doorProgress.snapTo(0.3f)
                doorProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
        } else {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            doorProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val totalW = size.toPx()
        val totalH = size.toPx()

        // Roof Path
        val roofPath = Path().apply {
            moveTo(totalW * 0.16f, totalH * 0.44f)
            lineTo(totalW * 0.50f, totalH * 0.16f)
            lineTo(totalW * 0.84f, totalH * 0.44f)
        }
        drawPath(
            path = roofPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // House walls & base
        val housePath = Path().apply {
            moveTo(totalW * 0.25f, totalH * 0.42f)
            lineTo(totalW * 0.25f, totalH * 0.84f)
            lineTo(totalW * 0.75f, totalH * 0.84f)
            lineTo(totalW * 0.75f, totalH * 0.42f)
        }
        drawPath(
            path = housePath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Doorway with gentle arched top, fluidly drawing upward
        val doorTop = totalH * (0.84f - (0.84f - 0.52f) * doorProgress.value)
        val doorPath = Path().apply {
            moveTo(totalW * 0.41f, totalH * 0.84f)
            lineTo(totalW * 0.41f, doorTop + (totalH * 0.08f))
            arcTo(
                rect = Rect(
                    left = totalW * 0.41f,
                    top = doorTop,
                    right = totalW * 0.59f,
                    bottom = doorTop + (totalH * 0.16f)
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(totalW * 0.59f, totalH * 0.84f)
        }
        drawPath(
            path = doorPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * History Dock Icon: Clean, standard clock rewind icon.
 * Features an outer counter-clockwise rewind circular arc with a crisp arrowhead at top-left,
 * and clock hands (hour and minute) that smoothly sweep forward when selected.
 */
@Composable
fun HistoryDockIcon(
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val animScale = remember { Animatable(1f) }
    val clockHandAngle = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            launch {
                animScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                clockHandAngle.snapTo(0f)
                clockHandAngle.animateTo(
                    targetValue = 90f,
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                )
            }
        } else {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            clockHandAngle.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val totalW = size.toPx()
        val s = totalW / 24f // Normalized 24x24 coordinate scale

        // 1. Counter-Clockwise Rewind Arc
        // Centered at (12, 12), radius = 8.5
        val arcPath = Path().apply {
            val ovalRect = Rect(3.5f * s, 3.5f * s, 20.5f * s, 20.5f * s)
            addArc(
                oval = ovalRect,
                startAngleDegrees = -130f,
                sweepAngleDegrees = 310f
            )
        }
        drawPath(
            path = arcPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Crisp Arrowhead at top-left opening pointing counter-clockwise (down/left)
        // Connected seamlessly to the arc start point at -130° (6.54, 5.49)
        val arrowTip = Offset(6.54f * s, 5.49f * s)
        val arrowPath = Path().apply {
            moveTo(3.2f * s, 3.5f * s)
            lineTo(arrowTip.x, arrowTip.y)
            lineTo(3.2f * s, 8.5f * s)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 3. Clock Hands inside (from center 12, 12)
        val center = Offset(12f * s, 12f * s)
        val rot = clockHandAngle.value

        // Hour Hand: pointing to 3 o'clock (0°), smoothly sweeps with rot
        val hourAngle = Math.toRadians((rot + 0.0)).toFloat()
        val hourLen = 4.2f * s
        drawLine(
            color = tint,
            start = center,
            end = Offset(
                center.x + cos(hourAngle) * hourLen,
                center.y + sin(hourAngle) * hourLen
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Minute Hand: pointing to 12 o'clock (-90°), smoothly sweeps with rot * 2
        val minuteAngle = Math.toRadians((rot * 2.0 - 90.0)).toFloat()
        val minuteLen = 5.6f * s
        drawLine(
            color = tint,
            start = center,
            end = Offset(
                center.x + cos(minuteAngle) * minuteLen,
                center.y + sin(minuteAngle) * minuteLen
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Analytics Dock Icon: 3 ascending vertical trend bars.
 * Fluidly raises the 3 bars in a smooth, non-bouncy wave when selected.
 */
@Composable
fun AnalyticsDockIcon(
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val animScale = remember { Animatable(1f) }
    val bar1Progress = remember { Animatable(1f) }
    val bar2Progress = remember { Animatable(1f) }
    val bar3Progress = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            launch {
                animScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                bar1Progress.snapTo(0.45f)
                bar1Progress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            }
            launch {
                delay(40)
                bar2Progress.snapTo(0.45f)
                bar2Progress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            }
            launch {
                delay(80)
                bar3Progress.snapTo(0.45f)
                bar3Progress.animateTo(1f, tween(durationMillis = 300, easing = FastOutSlowInEasing))
            }
        } else {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            bar1Progress.animateTo(1f, tween(180))
            bar2Progress.animateTo(1f, tween(180))
            bar3Progress.animateTo(1f, tween(180))
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val totalW = size.toPx()
        val totalH = size.toPx()
        val barWidth = 3.2.dp.toPx()
        val corner = 1.6.dp.toPx()

        val baseY = totalH * 0.84f

        // Bar 1 (Short)
        val bar1H = (totalH * 0.30f) * bar1Progress.value
        drawRoundRect(
            color = tint,
            topLeft = Offset(totalW * 0.22f, baseY - bar1H),
            size = Size(barWidth, bar1H),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Bar 2 (Medium)
        val bar2H = (totalH * 0.50f) * bar2Progress.value
        drawRoundRect(
            color = tint,
            topLeft = Offset(totalW * 0.46f, baseY - bar2H),
            size = Size(barWidth, bar2H),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Bar 3 (Tall)
        val bar3H = (totalH * 0.70f) * bar3Progress.value
        drawRoundRect(
            color = tint,
            topLeft = Offset(totalW * 0.70f, baseY - bar3H),
            size = Size(barWidth, bar3H),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )

        // Baseline
        drawLine(
            color = tint,
            start = Offset(totalW * 0.14f, baseY + 1.dp.toPx()),
            end = Offset(totalW * 0.86f, baseY + 1.dp.toPx()),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Lend/Borrow Dock Icon: Two reciprocal transfer arrows.
 * Smoothly and fluidly glides the transfer arrows when selected without any bounce.
 */
@Composable
fun LendBorrowDockIcon(
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val animScale = remember { Animatable(1f) }
    val topArrowShift = remember { Animatable(0f) }
    val bottomArrowShift = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            launch {
                animScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                topArrowShift.animateTo(2.5f, tween(160, easing = FastOutSlowInEasing))
                topArrowShift.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
            launch {
                bottomArrowShift.animateTo(-2.5f, tween(160, easing = FastOutSlowInEasing))
                bottomArrowShift.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
            }
        } else {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            topArrowShift.animateTo(0f, tween(150))
            bottomArrowShift.animateTo(0f, tween(150))
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val totalW = size.toPx()
        val totalH = size.toPx()

        val topShiftPx = topArrowShift.value.dp.toPx()
        val bottomShiftPx = bottomArrowShift.value.dp.toPx()

        // Top Arrow: pointing right (Lent)
        val topY = totalH * 0.38f
        drawLine(
            color = tint,
            start = Offset(totalW * 0.18f + topShiftPx, topY),
            end = Offset(totalW * 0.78f + topShiftPx, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val topArrowHead = Path().apply {
            moveTo(totalW * 0.62f + topShiftPx, topY - 3.5.dp.toPx())
            lineTo(totalW * 0.78f + topShiftPx, topY)
            lineTo(totalW * 0.62f + topShiftPx, topY + 3.5.dp.toPx())
        }
        drawPath(
            path = topArrowHead,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bottom Arrow: pointing left (Borrowed)
        val bottomY = totalH * 0.64f
        drawLine(
            color = tint,
            start = Offset(totalW * 0.82f + bottomShiftPx, bottomY),
            end = Offset(totalW * 0.22f + bottomShiftPx, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        val bottomArrowHead = Path().apply {
            moveTo(totalW * 0.38f + bottomShiftPx, bottomY - 3.5.dp.toPx())
            lineTo(totalW * 0.22f + bottomShiftPx, bottomY)
            lineTo(totalW * 0.38f + bottomShiftPx, bottomY + 3.5.dp.toPx())
        }
        drawPath(
            path = bottomArrowHead,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Add Dock Icon: Clean, centered plus (+) sign with rounded stroke caps, perfectly
 * matching the line stroke style and weight of the floating action button in the reference.
 */
@Composable
fun AddDockIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    strokeWidth: Dp = 2.2.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val sw = strokeWidth.toPx()
        val totalW = size.toPx()
        val totalH = size.toPx()
        val centerX = totalW * 0.5f
        val centerY = totalH * 0.5f
        val armLength = totalW * 0.30f

        // Horizontal line
        drawLine(
            color = tint,
            start = Offset(centerX - armLength, centerY),
            end = Offset(centerX + armLength, centerY),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )

        // Vertical line
        drawLine(
            color = tint,
            start = Offset(centerX, centerY - armLength),
            end = Offset(centerX, centerY + armLength),
            strokeWidth = sw,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Goals Dock Icon: Modern, precision target / bullseye icon representing financial goals.
 * Smoothly scales and pulses the bullseye center when selected.
 */
@Composable
fun GoalsDockIcon(
    tint: Color,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val animScale = remember { Animatable(1f) }
    val pulseProgress = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            launch {
                animScale.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                )
            }
            launch {
                pulseProgress.snapTo(0.4f)
                pulseProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
        } else {
            animScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
            pulseProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
            )
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
            }
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val totalW = size.toPx()
        val totalH = size.toPx()
        val center = Offset(totalW * 0.5f, totalH * 0.5f)
        val outerRadius = totalW * 0.42f
        val innerRadius = totalW * 0.24f
        val centerRadius = totalW * 0.10f * pulseProgress.value

        // Outer target ring
        drawCircle(
            color = tint,
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Middle ring
        drawCircle(
            color = tint.copy(alpha = 0.65f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = strokeWidth * 0.9f)
        )

        // Center bullseye dot
        drawCircle(
            color = tint,
            radius = centerRadius,
            center = center
        )
    }
}

