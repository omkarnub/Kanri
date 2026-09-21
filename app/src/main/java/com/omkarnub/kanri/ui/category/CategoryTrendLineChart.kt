package com.omkarnub.kanri.ui.category

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency

@Composable
fun CategoryTrendLineChart(
    trendPoints: List<MonthlyTrendPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(trendPoints) {
        mutableStateOf(if (trendPoints.isNotEmpty()) trendPoints.size - 1 else -1)
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(trendPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 850))
    }

    val textMeasurer = rememberTextMeasurer()

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Title & Selected Data Point summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "6-MONTH SPEND TREND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Monthly Spending Pattern",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                }

                // Interactive selection badge
                val activePoint = trendPoints.getOrNull(selectedIndex)
                if (activePoint != null) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "${activePoint.monthLabel}: ${formatCurrency(activePoint.totalSpent)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = lineColor
                                )
                            )
                            Text(
                                text = "${activePoint.transactionCount} transactions",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val maxSpend = trendPoints.maxOfOrNull { it.totalSpent } ?: 0.0
            val allZero = maxSpend <= 0.0

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(trendPoints) {
                            detectTapGestures { offset ->
                                val paddingLeft = 32.dp.toPx()
                                val paddingRight = 16.dp.toPx()
                                val chartWidth = size.width - paddingLeft - paddingRight
                                val stepX = chartWidth / (trendPoints.size - 1).coerceAtLeast(1)

                                if (trendPoints.isNotEmpty()) {
                                    var closestIdx = 0
                                    var minDiff = Float.MAX_VALUE
                                    trendPoints.indices.forEach { i ->
                                        val pointX = paddingLeft + i * stepX
                                        val diff = kotlin.math.abs(offset.x - pointX)
                                        if (diff < minDiff) {
                                            minDiff = diff
                                            closestIdx = i
                                        }
                                    }
                                    selectedIndex = closestIdx
                                }
                            }
                        }
                ) {
                    val paddingLeft = 32.dp.toPx()
                    val paddingRight = 16.dp.toPx()
                    val paddingTop = 16.dp.toPx()
                    val paddingBottom = 30.dp.toPx()

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom
                    val baselineY = size.height - paddingBottom

                    // Draw Horizontal Gridlines (Top, Mid, Base)
                    val gridSteps = 2
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 0..gridSteps) {
                        val y = paddingTop + (chartHeight / gridSteps) * i
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        // Draw Grid Label on Left
                        val gridValue = if (allZero) 0.0 else maxSpend * (1.0 - (i.toDouble() / gridSteps))
                        val labelText = if (gridValue >= 1000) "₹${(gridValue / 1000).toInt()}k" else "₹${gridValue.toInt()}"
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelText,
                            topLeft = Offset(4.dp.toPx(), y - 7.dp.toPx()),
                            style = TextStyle(
                                color = labelColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }

                    if (trendPoints.isEmpty()) return@Canvas

                    val ceilingAmount = if (allZero) 100.0 else maxSpend * 1.18
                    val stepX = chartWidth / (trendPoints.size - 1).coerceAtLeast(1)

                    val coordinates = trendPoints.mapIndexed { index, point ->
                        val x = paddingLeft + index * stepX
                        val normalizedSpend = (point.totalSpent / ceilingAmount).coerceIn(0.0, 1.0)
                        val y = baselineY - (chartHeight * normalizedSpend.toFloat() * animatedProgress.value)
                        Offset(x, y)
                    }

                    // Draw Gradient Fill Path
                    val fillPath = Path().apply {
                        moveTo(coordinates.first().x, baselineY)
                        coordinates.forEach { lineTo(it.x, it.y) }
                        lineTo(coordinates.last().x, baselineY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.38f),
                                lineColor.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = paddingTop,
                            endY = baselineY
                        )
                    )

                    // Draw Smooth Line Path
                    val linePath = Path().apply {
                        coordinates.forEachIndexed { index, offset ->
                            if (index == 0) {
                                moveTo(offset.x, offset.y)
                            } else {
                                val prev = coordinates[index - 1]
                                val cX1 = (prev.x + offset.x) / 2f
                                val cY1 = prev.y
                                val cX2 = (prev.x + offset.x) / 2f
                                val cY2 = offset.y
                                cubicTo(cX1, cY1, cX2, cY2, offset.x, offset.y)
                            }
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 2.8.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Draw Data Points & X-Axis Month Labels
                    coordinates.forEachIndexed { index, point ->
                        val isSelected = index == selectedIndex

                        // Selected vertical reference line
                        if (isSelected) {
                            drawLine(
                                color = lineColor.copy(alpha = 0.5f),
                                start = Offset(point.x, paddingTop),
                                end = Offset(point.x, baselineY),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }

                        // Outer point glow
                        drawCircle(
                            color = if (isSelected) lineColor.copy(alpha = 0.4f) else lineColor.copy(alpha = 0.2f),
                            radius = if (isSelected) 8.dp.toPx() else 5.dp.toPx(),
                            center = point
                        )

                        // Main point node
                        drawCircle(
                            color = if (isSelected) onSurfaceColor else lineColor,
                            radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                            center = point
                        )

                        // Draw X-axis label
                        val monthLabel = trendPoints[index].monthLabel
                        val textLayout = textMeasurer.measure(
                            text = monthLabel,
                            style = TextStyle(
                                color = if (isSelected) onSurfaceColor else onSurfaceVariantColor,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                point.x - (textLayout.size.width / 2f),
                                baselineY + 8.dp.toPx()
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(lineColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap on any month node to inspect exact spend",
                    fontSize = 11.sp,
                    color = Color(0xFF6E7681)
                )
            }
        }
    }
}
