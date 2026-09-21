package com.omkarnub.kanri.ui.month

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency

@Composable
fun DailySpendBarChart(
    dailySpends: List<DailySpendItem>,
    daysInMonth: Int,
    currentDay: Int,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier
) {
    val maxSpend = dailySpends.maxOfOrNull { it.totalAmount } ?: 0.0
    val totalSpend = dailySpends.sumOf { it.totalAmount }
    val daysWithData = if (isCurrentMonth) currentDay.coerceAtLeast(1) else daysInMonth
    val avgDailySpend = if (daysWithData > 0) totalSpend / daysWithData else 0.0

    var selectedDay by remember { mutableStateOf<DailySpendItem?>(null) }
    val scrollState = rememberScrollState()

    androidx.compose.runtime.LaunchedEffect(currentDay) {
        if (isCurrentMonth && currentDay > 10) {
            // Smoothly auto-scroll so current day is in view
            val targetOffset = (currentDay - 7) * 55
            scrollState.animateScrollTo(targetOffset.coerceAtLeast(0))
        }
    }

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
            // Header Row: Title & Average Spend Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAILY SPEND TREND",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Avg: ₹%,.0f/day".format(avgDailySpend),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Selected Day Tooltip / Indicator
            Spacer(modifier = Modifier.height(10.dp))
            if (selectedDay != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Day ${selectedDay!!.day}: ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatCurrency(selectedDay!!.totalAmount),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Tap any bar to inspect daily total",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

            // Scrollable Bar Chart Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                dailySpends.forEach { item ->
                    val isToday = isCurrentMonth && item.day == currentDay
                    val isSelected = selectedDay?.day == item.day
                    val barFraction = if (maxSpend > 0.0) {
                        (item.totalAmount / maxSpend).toFloat().coerceIn(0.04f, 1.0f)
                    } else 0.04f
                    val barHeight = (barFraction * 100).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedDay = item }
                            .padding(horizontal = 2.dp)
                    ) {
                        // The Bar
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (item.totalAmount > 0.0) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(emptyBarColor, emptyBarColor)
                                        )
                                    }
                                )
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    } else Modifier
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Day Number
                        Text(
                            text = item.day.toString(),
                            fontSize = 9.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isToday -> MaterialTheme.colorScheme.primary
                                isSelected -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
