package com.omkarnub.kanri.ui.month

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import java.util.Locale
import kotlin.math.abs

@Composable
fun AllTimeTrendsView(
    state: AllTimeTrendsUiState,
    onMetricSelect: (TrendMetricType) -> Unit,
    onScrubbedIndexChange: (Int?) -> Unit,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. High-Level Summary Card
        item {
            AllTimeOverviewCard(state = state)
        }

        // 2. Metric Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrendMetricType.entries.forEach { metric ->
                    val isSelected = state.selectedMetric == metric
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMetricSelect(metric) },
                        label = {
                            Text(
                                text = metric.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = metric.color.copy(alpha = 0.2f),
                            selectedLabelColor = metric.color,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = metric.color,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }

        // 3. Interactive Multi-Month Canvas Line Chart
        item {
            MultiMonthTrendsChart(
                monthlyTrends = state.monthlyTrends,
                selectedMetric = state.selectedMetric,
                scrubbedIndex = state.scrubbedIndex,
                onScrubbedIndexChange = onScrubbedIndexChange
            )
        }

        // 4. Section Header for Month-by-Month Trajectory Ledger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTH-BY-MONTH TRAJECTORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF8B949E),
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "Tap month to drill down",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF58A6FF),
                        fontSize = 11.sp
                    )
                )
            }
        }

        // 5. Itemized Chronological Month Cards (Newest First)
        val sortedMonths = state.monthlyTrends.reversed()
        items(sortedMonths, key = { it.monthKey }) { monthItem ->
            MonthTrajectoryCard(
                item = monthItem,
                onClick = { onSelectMonth(monthItem.year, monthItem.month) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AllTimeOverviewCard(state: AllTimeTrendsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ALL-TIME FINANCIAL TOTALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${state.monthlyTrends.size} Months Tracked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Row: Spent, Income, Net Savings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Total Spent
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(state.totalAllTimeSpent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF476F)
                    )
                }

                // Total Income
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total Income", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(state.totalAllTimeReceived),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06D6A0)
                    )
                }

                // Net Savings
                Column(modifier = Modifier.weight(1f)) {
                    Text("Net Savings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatCurrency(state.totalAllTimeSavings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.totalAllTimeSavings >= 0) MaterialTheme.colorScheme.primary else Color(0xFFEF476F)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-metrics Bar: Monthly Averages
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avg Spend: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            formatCurrency(state.averageMonthlySpent) + "/mo",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avg Savings: ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text(
                            formatCurrency(state.averageMonthlySavings) + "/mo",
                            color = if (state.averageMonthlySavings >= 0) Color(0xFF06D6A0) else Color(0xFFEF476F),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthTrajectoryCard(
    item: MonthTrendItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // MoM Spend Delta Indicator
                    if (item.momSpendDeltaPercent != null) {
                        val isIncrease = item.momSpendDeltaPercent > 0
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isIncrease) Color(0x33EF476F) else Color(0x3306D6A0)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isIncrease) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isIncrease) Color(0xFFEF476F) else Color(0xFF06D6A0),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = String.format(Locale.US, "%s%.1f%%", if (isIncrease) "+" else "", item.momSpendDeltaPercent),
                                    color = if (isIncrease) Color(0xFFEF476F) else Color(0xFF06D6A0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Spent: ${formatCurrency(item.totalSpent)}",
                        color = Color(0xFFEF476F),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Income: ${formatCurrency(item.totalReceived)}",
                        color = Color(0xFF06D6A0),
                        fontSize = 12.sp
                    )
                }
            }

            // Right side: Net Savings & Chevron
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(item.netSavings),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.netSavings >= 0) Color(0xFF06D6A0) else Color(0xFFEF476F)
                    )
                    Text(
                        text = "${item.transactionCount} txs",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "View Details",
                    tint = Color(0xFF6E7681),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
