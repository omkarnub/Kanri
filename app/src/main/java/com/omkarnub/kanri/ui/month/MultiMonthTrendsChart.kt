package com.omkarnub.kanri.ui.month

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MultiMonthTrendsChart(
    monthlyTrends: List<MonthTrendItem>,
    selectedMetric: TrendMetricType,
    scrubbedIndex: Int?,
    onScrubbedIndexChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Metric Indicator & Instructions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRAJECTORY OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = if (scrubbedIndex != null) "Tap to dismiss" else "Drag to inspect",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrubbed Tooltip Banner
            AnimatedVisibility(
                visible = scrubbedIndex != null && scrubbedIndex in monthlyTrends.indices,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (scrubbedIndex != null && scrubbedIndex in monthlyTrends.indices) {
                    val activeItem = monthlyTrends[scrubbedIndex]
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = activeItem.label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${activeItem.transactionCount} transactions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (selectedMetric == TrendMetricType.ALL || selectedMetric == TrendMetricType.SPEND) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Spend", color = Color(0xFF8B949E), fontSize = 10.sp)
                                        Text(
                                            formatCurrency(activeItem.totalSpent),
                                            color = Color(0xFFEF476F),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (selectedMetric == TrendMetricType.ALL || selectedMetric == TrendMetricType.RECEIVED) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Income", color = Color(0xFF8B949E), fontSize = 10.sp)
                                        Text(
                                            formatCurrency(activeItem.totalReceived),
                                            color = Color(0xFF06D6A0),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                if (selectedMetric == TrendMetricType.ALL || selectedMetric == TrendMetricType.SAVINGS) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Savings", color = Color(0xFF8B949E), fontSize = 10.sp)
                                        Text(
                                            formatCurrency(activeItem.netSavings),
                                            color = if (activeItem.netSavings >= 0) Color(0xFF118AB2) else Color(0xFFEF476F),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val gridLineColor = MaterialTheme.colorScheme.outlineVariant
            val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            val scrubbedLabelColor = MaterialTheme.colorScheme.onSurface
            val dotInnerColor = MaterialTheme.colorScheme.surface

            // Canvas Chart Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyTrends) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (monthlyTrends.isEmpty()) return@detectTapGestures
                                    val leftPad = 48.dp.toPx()
                                    val rightPad = 16.dp.toPx()
                                    val chartWidth = size.width - leftPad - rightPad
                                    if (chartWidth > 0 && offset.x in leftPad..(size.width - rightPad)) {
                                        val stepX = chartWidth / (monthlyTrends.size - 1).coerceAtLeast(1)
                                        val rawIdx = ((offset.x - leftPad + stepX / 2f) / stepX).toInt()
                                        val clampedIdx = rawIdx.coerceIn(0, monthlyTrends.size - 1)
                                        if (scrubbedIndex == clampedIdx) {
                                            onScrubbedIndexChange(null)
                                        } else {
                                            onScrubbedIndexChange(clampedIdx)
                                        }
                                    } else {
                                        onScrubbedIndexChange(null)
                                    }
                                }
                            )
                        }
                        .pointerInput(monthlyTrends) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val leftPad = 48.dp.toPx()
                                    val rightPad = 16.dp.toPx()
                                    val chartWidth = size.width - leftPad - rightPad
                                    if (chartWidth > 0 && monthlyTrends.isNotEmpty()) {
                                        val stepX = chartWidth / (monthlyTrends.size - 1).coerceAtLeast(1)
                                        val rawIdx = ((offset.x - leftPad + stepX / 2f) / stepX).toInt()
                                        onScrubbedIndexChange(rawIdx.coerceIn(0, monthlyTrends.size - 1))
                                    }
                                },
                                onDragEnd = { },
                                onDragCancel = { },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val leftPad = 48.dp.toPx()
                                    val rightPad = 16.dp.toPx()
                                    val chartWidth = size.width - leftPad - rightPad
                                    if (chartWidth > 0 && monthlyTrends.isNotEmpty()) {
                                        val stepX = chartWidth / (monthlyTrends.size - 1).coerceAtLeast(1)
                                        val rawIdx = ((change.position.x - leftPad + stepX / 2f) / stepX).toInt()
                                        onScrubbedIndexChange(rawIdx.coerceIn(0, monthlyTrends.size - 1))
                                    }
                                }
                            )
                        }
                ) {
                    if (monthlyTrends.isEmpty()) return@Canvas

                    val leftPad = 48.dp.toPx()
                    val rightPad = 16.dp.toPx()
                    val topPad = 20.dp.toPx()
                    val bottomPad = 32.dp.toPx()

                    val chartWidth = size.width - leftPad - rightPad
                    val chartHeight = size.height - topPad - bottomPad

                    if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

                    // 1. Calculate Maximum Range
                    var maxVal = 1000.0
                    for (item in monthlyTrends) {
                        when (selectedMetric) {
                            TrendMetricType.ALL -> {
                                maxVal = max(maxVal, max(item.totalSpent, item.totalReceived))
                            }
                            TrendMetricType.SPEND -> {
                                maxVal = max(maxVal, item.totalSpent)
                            }
                            TrendMetricType.RECEIVED -> {
                                maxVal = max(maxVal, item.totalReceived)
                            }
                            TrendMetricType.SAVINGS -> {
                                maxVal = max(maxVal, max(abs(item.netSavings), item.totalSpent))
                            }
                        }
                    }
                    // Add 15% headroom
                    val ceiling = maxVal * 1.15

                    // 2. Horizontal Dotted Gridlines & Y-Axis Labels
                    val gridSteps = 3
                    val labelStyle = TextStyle(
                        color = axisLabelColor,
                        fontSize = 10.sp
                    )

                    for (i in 0..gridSteps) {
                        val fraction = i.toFloat() / gridSteps.toFloat()
                        val y = topPad + chartHeight * (1f - fraction)
                        val valueAtLine = ceiling * fraction

                        // Draw dotted line
                        drawLine(
                            color = gridLineColor,
                            start = Offset(leftPad, y),
                            end = Offset(size.width - rightPad, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        // Draw Y-axis currency text
                        val labelText = formatAxisAmount(valueAtLine)
                        val textLayout = textMeasurer.measure(labelText, labelStyle)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = (leftPad - textLayout.size.width - 8.dp.toPx()).coerceAtLeast(0f),
                                y = y - textLayout.size.height / 2f
                            )
                        )
                    }

                    // 3. Compute Coordinates for Each Month
                    val pointCount = monthlyTrends.size
                    val stepX = chartWidth / (pointCount - 1).coerceAtLeast(1)

                    fun getPoint(idx: Int, value: Double): Offset {
                        val x = leftPad + idx * stepX
                        val yFraction = (value / ceiling).toFloat().coerceIn(0f, 1f)
                        val y = topPad + chartHeight * (1f - yFraction)
                        return Offset(x, y)
                    }

                    // Draw X-Axis Month Labels
                    for (i in monthlyTrends.indices) {
                        val x = leftPad + i * stepX
                        val monthLabel = monthlyTrends[i].shortLabel
                        val isScrubbed = i == scrubbedIndex
                        val textLayout = textMeasurer.measure(
                            text = monthLabel,
                            style = TextStyle(
                                color = if (isScrubbed) scrubbedLabelColor else axisLabelColor,
                                fontWeight = if (isScrubbed) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        )
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                x = x - textLayout.size.width / 2f,
                                y = size.height - bottomPad + 8.dp.toPx()
                            )
                        )
                    }

                    // Helper to draw a smooth cubic curve and optional underfill
                    fun drawMetricCurve(
                        values: List<Double>,
                        lineColor: Color,
                        drawFill: Boolean
                    ) {
                        if (values.isEmpty()) return
                        val points = values.mapIndexed { idx, v -> getPoint(idx, v) }

                        val linePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX = (p0.x + p1.x) / 2f
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }

                        // Fill under curve
                        if (drawFill) {
                            val fillPath = Path().apply {
                                addPath(linePath)
                                lineTo(points.last().x, topPad + chartHeight)
                                lineTo(points.first().x, topPad + chartHeight)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.30f),
                                        lineColor.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    startY = topPad,
                                    endY = topPad + chartHeight
                                )
                            )
                        }

                        // Draw curve stroke
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw data dots
                        for (p in points) {
                            drawCircle(
                                color = lineColor,
                                radius = 4.dp.toPx(),
                                center = p
                            )
                            drawCircle(
                                color = dotInnerColor,
                                radius = 2.dp.toPx(),
                                center = p
                            )
                        }
                    }

                    // 4. Draw Lines Based on Selected Metric
                    when (selectedMetric) {
                        TrendMetricType.ALL -> {
                            // Spend curve (Coral)
                            drawMetricCurve(
                                values = monthlyTrends.map { it.totalSpent },
                                lineColor = Color(0xFFEF476F),
                                drawFill = false
                            )
                            // Income curve (Emerald)
                            drawMetricCurve(
                                values = monthlyTrends.map { it.totalReceived },
                                lineColor = Color(0xFF06D6A0),
                                drawFill = false
                            )
                            // Net Savings curve (Azure/Cyan)
                            drawMetricCurve(
                                values = monthlyTrends.map { it.netSavings.coerceAtLeast(0.0) },
                                lineColor = Color(0xFF58A6FF),
                                drawFill = false
                            )
                        }
                        TrendMetricType.SPEND -> {
                            drawMetricCurve(
                                values = monthlyTrends.map { it.totalSpent },
                                lineColor = Color(0xFFEF476F),
                                drawFill = true
                            )
                        }
                        TrendMetricType.RECEIVED -> {
                            drawMetricCurve(
                                values = monthlyTrends.map { it.totalReceived },
                                lineColor = Color(0xFF06D6A0),
                                drawFill = true
                            )
                        }
                        TrendMetricType.SAVINGS -> {
                            drawMetricCurve(
                                values = monthlyTrends.map { it.netSavings.coerceAtLeast(0.0) },
                                lineColor = Color(0xFF118AB2),
                                drawFill = true
                            )
                        }
                    }

                    // 5. Draw Active Scrubbed Guideline & Highlight Halo
                    if (scrubbedIndex != null && scrubbedIndex in monthlyTrends.indices) {
                        val activeX = leftPad + scrubbedIndex * stepX

                        // Vertical dashed line
                        drawLine(
                            color = Color(0x99FFFFFF),
                            start = Offset(activeX, topPad),
                            end = Offset(activeX, topPad + chartHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Highlight dots
                        fun drawHalo(valAmount: Double, color: Color) {
                            val pt = getPoint(scrubbedIndex, valAmount)
                            // Outer glowing halo
                            drawCircle(
                                color = color.copy(alpha = 0.35f),
                                radius = 9.dp.toPx(),
                                center = pt
                            )
                            // Solid core
                            drawCircle(
                                color = color,
                                radius = 5.dp.toPx(),
                                center = pt
                            )
                            // White pinpoint center
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = pt
                            )
                        }

                        val activeItem = monthlyTrends[scrubbedIndex]
                        when (selectedMetric) {
                            TrendMetricType.ALL -> {
                                drawHalo(activeItem.totalSpent, Color(0xFFEF476F))
                                drawHalo(activeItem.totalReceived, Color(0xFF06D6A0))
                                drawHalo(activeItem.netSavings.coerceAtLeast(0.0), Color(0xFF58A6FF))
                            }
                            TrendMetricType.SPEND -> drawHalo(activeItem.totalSpent, Color(0xFFEF476F))
                            TrendMetricType.RECEIVED -> drawHalo(activeItem.totalReceived, Color(0xFF06D6A0))
                            TrendMetricType.SAVINGS -> drawHalo(activeItem.netSavings.coerceAtLeast(0.0), Color(0xFF118AB2))
                        }
                    }
                }
            }

            // Bottom Legend (when ALL metrics selected)
            if (selectedMetric == TrendMetricType.ALL) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendBadge(label = "Spend", color = Color(0xFFEF476F))
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendBadge(label = "Income", color = Color(0xFF06D6A0))
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendBadge(label = "Net Savings", color = Color(0xFF58A6FF))
                }
            }
        }
    }
}

@Composable
private fun LegendBadge(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color(0xFF8B949E),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatAxisAmount(amount: Double): String {
    return when {
        amount >= 100000 -> "₹${(amount / 100000).toInt()}L"
        amount >= 1000 -> "₹${(amount / 1000).toInt()}k"
        else -> "₹${amount.toInt()}"
    }
}
