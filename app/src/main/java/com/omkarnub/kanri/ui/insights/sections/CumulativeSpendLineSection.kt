package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.CumulativeSpendLineData

private val ExpenseRed = Color(0xFFE54D2E)
private val WarningAmber = Color(0xFFF5A524)

@Composable
fun CumulativeSpendLineSection(
    data: CumulativeSpendLineData,
    modifier: Modifier = Modifier
) {
    val trimAnimation = remember { Animatable(0f) }
    LaunchedEffect(data) {
        trimAnimation.snapTo(0f)
        trimAnimation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    var scrubbedDay by remember { mutableStateOf<Int?>(null) }

    val totalDays = maxOf(
        data.currentPoints.maxOfOrNull { it.day } ?: 30,
        data.ghostPoints.maxOfOrNull { it.day } ?: 30,
        data.projectedPoints.maxOfOrNull { it.day } ?: 30,
        1
    )

    val maxY = (data.maxAmount * 1.1).coerceAtLeast(100.0)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val latestCurrent = data.currentPoints.lastOrNull()?.amount ?: 0.0
    val latestProjected = data.projectedPoints.lastOrNull()?.amount ?: latestCurrent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Cumulative spend chart: ${formatCurrency(latestCurrent)} so far, projected ${formatCurrency(latestProjected)}"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CUMULATIVE SPEND",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = onSurface, label = "Current")
                    if (data.ghostPoints.isNotEmpty()) {
                        LegendItem(color = onSurface.copy(alpha = 0.25f), label = "Previous")
                    }
                    if (data.budgetLimit != null) {
                        LegendItem(color = WarningAmber, label = "Budget", isDashed = true)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .pointerInput(data) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val day = ((offset.x / size.width) * totalDays).toInt() + 1
                                    scrubbedDay = day.coerceIn(1, totalDays)
                                },
                                onDrag = { change, _ ->
                                    val day = ((change.position.x / size.width) * totalDays).toInt() + 1
                                    scrubbedDay = day.coerceIn(1, totalDays)
                                },
                                onDragEnd = { scrubbedDay = null },
                                onDragCancel = { scrubbedDay = null }
                            )
                        }
                        .pointerInput(data) {
                            detectTapGestures(
                                onPress = { offset ->
                                    val day = ((offset.x / size.width) * totalDays).toInt() + 1
                                    scrubbedDay = day.coerceIn(1, totalDays)
                                    tryAwaitRelease()
                                    scrubbedDay = null
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val daysCount = totalDays.coerceAtLeast(1)

                    fun xForDay(day: Int): Float = ((day - 1).toFloat() / (daysCount - 1).coerceAtLeast(1)) * w
                    fun yForAmount(amt: Double): Float = h - ((amt / maxY).toFloat() * h).coerceIn(0f, h)

                    // Budget line (thin dashed amber line)
                    if (data.budgetLimit != null) {
                        val budgetY = yForAmount(data.budgetLimit)
                        drawLine(
                            color = WarningAmber.copy(alpha = 0.8f),
                            start = Offset(0f, budgetY),
                            end = Offset(w, budgetY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Ghost line for previous period at 25% alpha
                    if (data.ghostPoints.isNotEmpty()) {
                        val prevPath = Path()
                        data.ghostPoints.forEachIndexed { idx, pt ->
                            val px = xForDay(pt.day)
                            val py = yForAmount(pt.amount)
                            if (idx == 0) prevPath.moveTo(px, py) else prevPath.lineTo(px, py)
                        }
                        drawPath(
                            path = prevPath,
                            color = onSurface.copy(alpha = 0.25f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Projected continuation (dotted)
                    if (data.projectedPoints.isNotEmpty()) {
                        val projPath = Path()
                        val startPt = data.currentPoints.lastOrNull() ?: data.projectedPoints.first()
                        projPath.moveTo(xForDay(startPt.day), yForAmount(startPt.amount))
                        data.projectedPoints.forEach { pt ->
                            projPath.lineTo(xForDay(pt.day), yForAmount(pt.amount))
                        }
                        drawPath(
                            path = projPath,
                            color = onSurface.copy(alpha = 0.5f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        )
                    }

                    // Current line (trimmed by animation)
                    if (data.currentPoints.isNotEmpty()) {
                        val animProgress = trimAnimation.value
                        val visibleCount = (data.currentPoints.size * animProgress).toInt().coerceAtLeast(1)
                        val visiblePoints = data.currentPoints.take(visibleCount)

                        if (visiblePoints.size >= 2) {
                            for (i in 0 until visiblePoints.size - 1) {
                                val p1 = visiblePoints[i]
                                val p2 = visiblePoints[i + 1]
                                val x1 = xForDay(p1.day)
                                val y1 = yForAmount(p1.amount)
                                val x2 = xForDay(p2.day)
                                val y2 = yForAmount(p2.amount)

                                val isAboveBudget = data.budgetLimit != null && p2.amount > data.budgetLimit
                                val segmentColor = if (isAboveBudget) ExpenseRed else onSurface

                                drawLine(
                                    color = segmentColor,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        } else if (visiblePoints.size == 1) {
                            val p = visiblePoints[0]
                            drawCircle(
                                color = onSurface,
                                radius = 3.dp.toPx(),
                                center = Offset(xForDay(p.day), yForAmount(p.amount))
                            )
                        }
                    }

                    // Touch-scrub vertical hairline and marker
                    scrubbedDay?.let { day ->
                        val scrubX = xForDay(day)
                        drawLine(
                            color = onSurface.copy(alpha = 0.6f),
                            start = Offset(scrubX, 0f),
                            end = Offset(scrubX, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        val curPt = data.currentPoints.find { it.day == day }
                        if (curPt != null) {
                            drawCircle(
                                color = onSurface,
                                radius = 5.dp.toPx(),
                                center = Offset(scrubX, yForAmount(curPt.amount))
                            )
                        }
                    }
                }
            }

            // Scrub Tooltip or footer
            val activeScrub = scrubbedDay
            if (activeScrub != null) {
                val curPt = data.currentPoints.find { it.day == activeScrub }?.amount
                val prevPt = data.ghostPoints.find { it.day == activeScrub }?.amount
                val diff = if (curPt != null && prevPt != null) curPt - prevPt else null

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Day $activeScrub · ${formatCurrency(curPt ?: 0.0)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (prevPt != null) {
                            val diffText = if (diff != null && diff >= 0) "+${formatCurrency(diff)}" else formatCurrency(diff ?: 0.0)
                            Text(
                                text = "prev ${formatCurrency(prevPt)} ($diffText)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (diff != null && diff > 0) ExpenseRed else onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
        ) {
            Canvas(modifier = Modifier.size(12.dp, 2.dp)) {
                if (isDashed) {
                    drawLine(
                        color = color,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                    )
                } else {
                    drawLine(
                        color = color,
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
