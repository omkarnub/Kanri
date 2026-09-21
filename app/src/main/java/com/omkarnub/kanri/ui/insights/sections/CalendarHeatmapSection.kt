package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.CalendarDayData
import com.omkarnub.kanri.ui.insights.CalendarHeatmapData
import com.omkarnub.kanri.ui.insights.InsightsMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val IncomeSage = Color(0xFF30A46C)

@Composable
fun CalendarHeatmapSection(
    data: CalendarHeatmapData,
    mode: InsightsMode,
    onViewDayInHistory: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDay by remember { mutableStateOf<CalendarDayData?>(null) }
    val textMeasurer = rememberTextMeasurer()

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    fun stepColor(intensity: Int): Color {
        return if (mode == InsightsMode.EXPENSE) {
            when (intensity) {
                0 -> Color.Transparent
                1 -> onSurface.copy(alpha = 0.15f)
                2 -> onSurface.copy(alpha = 0.35f)
                3 -> onSurface.copy(alpha = 0.60f)
                4 -> onSurface.copy(alpha = 0.85f)
                5 -> onSurface
                else -> Color.Transparent
            }
        } else {
            when (intensity) {
                0 -> Color.Transparent
                1 -> IncomeSage.copy(alpha = 0.20f)
                2 -> IncomeSage.copy(alpha = 0.40f)
                3 -> IncomeSage.copy(alpha = 0.65f)
                4 -> IncomeSage.copy(alpha = 0.85f)
                5 -> IncomeSage
                else -> Color.Transparent
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Calendar heatmap for ${data.yearMonth.month.name}, ${data.noSpendDaysCount} no-spend days"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, outlineVariant)
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
                    text = "CALENDAR HEATMAP",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Legend: Less ▢▢▢▢▢ More
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Less", fontSize = 9.sp, color = onSurfaceVariant)
                    for (step in 1..5) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = RoundedCornerShape(2.dp),
                            color = stepColor(step)
                        ) {}
                    }
                    Text("More", fontSize = 9.sp, color = onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekday initials row (M T W T F S S)
            val weekDayInitials = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDayInitials.forEach { initial ->
                    Text(
                        text = initial,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val leadingOffset = data.emptyLeadingDays
            val totalCells = leadingOffset + data.days.size
            val numRows = (totalCells + 6) / 7
            val canvasHeightDp = (numRows * 42).dp
            val surfaceColor = MaterialTheme.colorScheme.surface

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeightDp)
                    .pointerInput(data.days) {
                        detectTapGestures { offset ->
                            val cellWidth = size.width / 7f
                            val cellHeight = size.height / numRows.toFloat()

                            val col = (offset.x / cellWidth).toInt().coerceIn(0, 6)
                            val row = (offset.y / cellHeight).toInt().coerceIn(0, numRows - 1)
                            val cellIndex = row * 7 + col

                            val dayIndex = cellIndex - leadingOffset
                            if (dayIndex in data.days.indices) {
                                selectedDay = data.days[dayIndex]
                            } else {
                                selectedDay = null
                            }
                        }
                    }
            ) {
                val cellWidth = size.width / 7f
                val cellHeight = size.height / numRows.toFloat()
                val padding = 3.dp.toPx()
                val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                for (idx in data.days.indices) {
                    val dayItem = data.days[idx]
                    val cellIndex = leadingOffset + idx
                    val row = cellIndex / 7
                    val col = cellIndex % 7

                    val x = col * cellWidth + padding
                    val y = row * cellHeight + padding
                    val w = cellWidth - (padding * 2)
                    val h = cellHeight - (padding * 2)

                    val isSelected = selectedDay?.date == dayItem.date
                    val fill = stepColor(dayItem.quantileLevel)

                    if (dayItem.quantileLevel > 0) {
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = cornerRadius
                        )
                    } else {
                        drawRoundRect(
                            color = surfaceVariant.copy(alpha = 0.5f),
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = cornerRadius
                        )
                    }

                    if (dayItem.isToday) {
                        drawRoundRect(
                            color = onSurface,
                            topLeft = Offset(x, y),
                            size = Size(w, h),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }

                    if (isSelected) {
                        drawRoundRect(
                            color = if (mode == InsightsMode.EXPENSE) onSurface else IncomeSage,
                            topLeft = Offset(x - 1f, y - 1f),
                            size = Size(w + 2f, h + 2f),
                            cornerRadius = cornerRadius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    val dayText = dayItem.dayOfMonth.toString()
                    val textColor = when {
                        dayItem.isFuture -> onSurfaceVariant.copy(alpha = 0.3f)
                        dayItem.quantileLevel >= 3 -> surfaceColor
                        else -> onSurface
                    }

                    val textLayout = textMeasurer.measure(
                        text = dayText,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = if (dayItem.isToday) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    )

                    val textX = x + (w - textLayout.size.width) / 2f
                    val textY = y + (h - textLayout.size.height) / 2f
                    drawText(textLayout, topLeft = Offset(textX, textY))

                    if (!dayItem.isFuture && dayItem.amount == 0.0 && !dayItem.isToday) {
                        drawCircle(
                            color = onSurfaceVariant.copy(alpha = 0.4f),
                            radius = 1.5.dp.toPx(),
                            center = Offset(x + w / 2f, y + h - 5.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val subLineParts = mutableListOf<String>()
            subLineParts.add("${data.noSpendDaysCount} no-spend days")
            if (data.highestDayDate != null && data.highestDayAmount > 0) {
                val dtf = DateTimeFormatter.ofPattern("d MMM")
                subLineParts.add("Highest: ${data.highestDayDate.format(dtf)}, ${formatCurrency(data.highestDayAmount)}")
            }

            Text(
                text = subLineParts.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = onSurfaceVariant,
                    fontSize = 12.sp
                )
            )

            selectedDay?.let { sel ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val dtf = DateTimeFormatter.ofPattern("EEEE, d MMMM")
                            Text(
                                text = sel.date.format(dtf),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )
                            )
                            Text(
                                text = if (sel.amount > 0) {
                                    formatCurrency(sel.amount)
                                } else {
                                    "No spending"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (sel.hasSpend) {
                            TextButton(
                                onClick = { onViewDayInHistory(sel.date) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("View history", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
