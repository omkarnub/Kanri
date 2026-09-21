package com.omkarnub.kanri.ui.month

import androidx.compose.ui.graphics.Color

enum class TrendMetricType(val label: String, val color: Color) {
    ALL("All Metrics", Color(0xFF58A6FF)),
    SPEND("Spend", Color(0xFFEF476F)),
    RECEIVED("Income", Color(0xFF06D6A0)),
    SAVINGS("Net Savings", Color(0xFF118AB2))
}

data class MonthTrendItem(
    val year: Int,
    val month: Int, // 0-indexed (Calendar.MONTH, e.g. 0 = Jan, 8 = Sep)
    val monthKey: String, // "2026-09"
    val label: String, // "Sep 2026"
    val shortLabel: String, // "Sep"
    val totalSpent: Double,
    val totalReceived: Double,
    val netSavings: Double, // totalReceived - totalSpent
    val savingsRate: Float, // (netSavings / totalReceived * 100) if received > 0, else 0%
    val transactionCount: Int,
    val momSpendDeltaPercent: Double? = null // e.g. +14.2% or -5.1% compared to previous month
)

data class AllTimeTrendsUiState(
    val monthlyTrends: List<MonthTrendItem> = emptyList(),
    val totalAllTimeSpent: Double = 0.0,
    val totalAllTimeReceived: Double = 0.0,
    val totalAllTimeSavings: Double = 0.0,
    val averageMonthlySpent: Double = 0.0,
    val averageMonthlyReceived: Double = 0.0,
    val averageMonthlySavings: Double = 0.0,
    val highestSpendMonth: MonthTrendItem? = null,
    val highestSavingsMonth: MonthTrendItem? = null,
    val selectedMetric: TrendMetricType = TrendMetricType.ALL,
    val scrubbedIndex: Int? = null,
    val isLoading: Boolean = false
)
