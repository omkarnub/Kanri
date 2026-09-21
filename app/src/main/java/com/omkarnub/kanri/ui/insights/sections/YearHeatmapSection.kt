package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.YearHeatmapCell
import com.omkarnub.kanri.ui.insights.YearHeatmapData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val IncomeSage = Color(0xFF30A46C)

@Composable
fun YearHeatmapSection(
    data: YearHeatmapData,
    mode: InsightsMode,
    onYearChange: (Int) -> Unit,
    onViewDayInHistory: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCell by remember { mutableStateOf<YearHeatmapCell?>(null) }
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()

    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val today = LocalDate.now()
    LaunchedEffect(data.year) {
        if (data.year == today.year) {
            val startOfYear = LocalDate.of(data.year, 1, 1)
            val weeksElapsed = ChronoUnit.WEEKS.between(startOfYear, today).toInt()
            val targetScroll = (weeksElapsed * 16).dp.value.toInt()
            scrollState.scrollTo(targetScroll.coerceAtLeast(0))
        }
    }

    fun intensityColor(intensity: Int): Color {
        return if (mode == InsightsMode.EXPENSE) {
            when (intensity) {
                0 -> surfaceVariant.copy(alpha = 0.3f)
                1 -> onSurface.copy(alpha = 0.15f)
                2 -> onSurface.copy(alpha = 0.35f)
                3 -> onSurface.copy(alpha = 0.60f)
                4 -> onSurface.copy(alpha = 0.85f)
                5 -> onSurface
                else -> Color.Transparent
            }
        } else {
            when (intensity) {
                0 -> surfaceVariant.copy(alpha = 0.3f)
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
                contentDescription = "Year heatmap for ${data.year}: ${data.spendingDaysCount} spending days, ${data.noSpendDaysCount} no-spend days"
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
            // Header with Year Stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "YEAR AT A GLANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Stepper: ‹ 2026 ›
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val canGoBack = data.availableYears.contains(data.year - 1)
                    IconButton(
                        onClick = { if (canGoBack) onYearChange(data.year - 1) },
                        enabled = canGoBack,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous year",
                            tint = if (canGoBack) onSurface else onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = data.year.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )
                    )

                    val canGoForward = data.availableYears.contains(data.year + 1)
                    IconButton(
                        onClick = { if (canGoForward) onYearChange(data.year + 1) },
                        enabled = canGoForward,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next year",
                            tint = if (canGoForward) onSurface else onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 53-week horizontally scrollable single-Canvas grid
            val startOfYear = LocalDate.of(data.year, 1, 1)
            val dayOfWeekOffset = startOfYear.dayOfWeek.value - 1
            val totalWeeks = 54
            val cellSize = 13.dp
            val cellSpacing = 3.dp
            val totalWidthDp = ((cellSize + cellSpacing) * totalWeeks) + 20.dp
            val totalHeightDp = ((cellSize + cellSpacing) * 7) + 24.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(width = totalWidthDp, height = totalHeightDp)
                        .pointerInput(data.cells) {
                            detectTapGestures { offset ->
                                val cellPx = (cellSize + cellSpacing).toPx()
                                val col = (offset.x / cellPx).toInt()
                                val row = ((offset.y - 18.dp.toPx()) / cellPx).toInt()

                                if (row in 0..6 && col in 0 until totalWeeks) {
                                    val cellIndex = col * 7 + row
                                    val dayIndex = cellIndex - dayOfWeekOffset
                                    if (dayIndex in data.cells.indices) {
                                        selectedCell = data.cells[dayIndex]
                                    } else {
                                        selectedCell = null
                                    }
                                }
                            }
                        }
                ) {
                    val cellPx = cellSize.toPx()
                    val spacingPx = cellSpacing.toPx()
                    val corner = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                    val topMargin = 18.dp.toPx()

                    // Draw month labels along the top
                    val months = listOf(
                        1 to "Jan", 2 to "Feb", 3 to "Mar", 4 to "Apr", 5 to "May", 6 to "Jun",
                        7 to "Jul", 8 to "Aug", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dec"
                    )
                    months.forEach { (m, label) ->
                        val firstOfMonth = LocalDate.of(data.year, m, 1)
                        val daysFromStart = ChronoUnit.DAYS.between(startOfYear, firstOfMonth).toInt()
                        val weekCol = (daysFromStart + dayOfWeekOffset) / 7
                        val labelX = weekCol * (cellPx + spacingPx)

                        val textLayout = textMeasurer.measure(
                            text = label,
                            style = TextStyle(
                                fontSize = 9.sp,
                                color = onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        drawText(textLayout, topLeft = Offset(labelX, 0f))
                    }

                    // Draw all 365/366 cells
                    for (i in data.cells.indices) {
                        val cell = data.cells[i]
                        val cellIndex = dayOfWeekOffset + i
                        val col = cellIndex / 7
                        val row = cellIndex % 7

                        val x = col * (cellPx + spacingPx)
                        val y = topMargin + row * (cellPx + spacingPx)

                        val color = if (cell.isBeforeFirstTx) {
                            surfaceVariant.copy(alpha = 0.15f)
                        } else {
                            intensityColor(cell.quantileLevel)
                        }

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            cornerRadius = corner
                        )

                        if (selectedCell?.date == cell.date) {
                            drawRoundRect(
                                color = onSurface,
                                topLeft = Offset(x - 1f, y - 1f),
                                size = Size(cellPx + 2f, cellPx + 2f),
                                cornerRadius = corner,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${data.spendingDaysCount} spending days  ·  ${data.noSpendDaysCount} no-spend days",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = onSurfaceVariant,
                    fontSize = 12.sp
                )
            )

            selectedCell?.let { sel ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val dtf = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
                            Text(
                                text = sel.date.format(dtf),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = onSurface
                                )
                            )
                            val spendText = if (sel.amount > 0) {
                                "${formatCurrency(sel.amount)} (${sel.txCount} txns)"
                            } else {
                                "No spending"
                            }
                            Text(
                                text = spendText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (sel.txCount > 0) {
                            TextButton(
                                onClick = { onViewDayInHistory(sel.date) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("View day", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
