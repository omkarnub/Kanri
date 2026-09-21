package com.omkarnub.kanri.ui.month

import com.omkarnub.kanri.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AllTimeTrendsCalculator {

    fun calculate(
        transactions: List<TransactionEntity>,
        selectedMetric: TrendMetricType = TrendMetricType.ALL,
        referenceCalendar: Calendar = Calendar.getInstance()
    ): AllTimeTrendsUiState {
        if (transactions.isEmpty()) {
            return AllTimeTrendsUiState(
                monthlyTrends = emptyList(),
                selectedMetric = selectedMetric,
                isLoading = false
            )
        }

        val txCal = Calendar.getInstance()

        // 1. Group transactions by "yyyy-MM"
        val monthBuckets = mutableMapOf<String, MutableList<TransactionEntity>>()
        var earliestYear = referenceCalendar.get(Calendar.YEAR)
        var earliestMonth = referenceCalendar.get(Calendar.MONTH)

        for (tx in transactions) {
            txCal.timeInMillis = tx.timestamp
            val year = txCal.get(Calendar.YEAR)
            val month = txCal.get(Calendar.MONTH) // 0..11
            val key = String.format(Locale.US, "%04d-%02d", year, month + 1)

            monthBuckets.getOrPut(key) { mutableListOf() }.add(tx)

            if (year < earliestYear || (year == earliestYear && month < earliestMonth)) {
                earliestYear = year
                earliestMonth = month
            }
        }

        // 2. Ensure a continuous timeline: from at least 5 months ago up to current month (minimum 6 months)
        val endCal = referenceCalendar.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, 1)

        val startCal = referenceCalendar.clone() as Calendar
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.add(Calendar.MONTH, -5) // default 6 months window

        val earliestCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, earliestYear)
            set(Calendar.MONTH, earliestMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        if (earliestCal.before(startCal)) {
            startCal.timeInMillis = earliestCal.timeInMillis
        }

        val fullMonthFormatter = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val shortMonthFormatter = SimpleDateFormat("MMM", Locale.getDefault())

        val monthlyTrends = mutableListOf<MonthTrendItem>()
        val iterCal = startCal.clone() as Calendar

        var prevSpent: Double? = null

        while (!iterCal.after(endCal)) {
            val y = iterCal.get(Calendar.YEAR)
            val m = iterCal.get(Calendar.MONTH)
            val key = String.format(Locale.US, "%04d-%02d", y, m + 1)

            val monthTxs = monthBuckets[key] ?: emptyList()

            var spent = 0.0
            var received = 0.0

            for (tx in monthTxs) {
                if (tx.type.equals("DEBIT", ignoreCase = true)) {
                    spent += tx.amount
                } else if (tx.type.equals("CREDIT", ignoreCase = true)) {
                    received += tx.amount
                }
            }

            val netSavings = received - spent
            val savingsRate = if (received > 0) {
                ((netSavings / received) * 100.0).toFloat().coerceIn(-100f, 100f)
            } else 0f

            val momSpendDelta: Double? = if (prevSpent != null && prevSpent!! > 0) {
                ((spent - prevSpent!!) / prevSpent!!) * 100.0
            } else null

            val item = MonthTrendItem(
                year = y,
                month = m,
                monthKey = key,
                label = fullMonthFormatter.format(iterCal.time),
                shortLabel = shortMonthFormatter.format(iterCal.time),
                totalSpent = spent,
                totalReceived = received,
                netSavings = netSavings,
                savingsRate = savingsRate,
                transactionCount = monthTxs.size,
                momSpendDeltaPercent = momSpendDelta
            )

            monthlyTrends.add(item)
            if (spent > 0 || monthTxs.isNotEmpty()) {
                prevSpent = spent
            }

            iterCal.add(Calendar.MONTH, 1)
        }

        val totalSpent = monthlyTrends.sumOf { it.totalSpent }
        val totalReceived = monthlyTrends.sumOf { it.totalReceived }
        val totalSavings = totalReceived - totalSpent

        // Average over active months (months with transactions, or at least 1)
        val activeMonthsCount = monthlyTrends.count { it.transactionCount > 0 }.coerceAtLeast(1)
        val avgSpent = totalSpent / activeMonthsCount
        val avgReceived = totalReceived / activeMonthsCount
        val avgSavings = totalSavings / activeMonthsCount

        val highestSpend = monthlyTrends.filter { it.totalSpent > 0 }.maxByOrNull { it.totalSpent }
        val highestSavings = monthlyTrends.filter { it.netSavings > 0 }.maxByOrNull { it.netSavings }

        return AllTimeTrendsUiState(
            monthlyTrends = monthlyTrends,
            totalAllTimeSpent = totalSpent,
            totalAllTimeReceived = totalReceived,
            totalAllTimeSavings = totalSavings,
            averageMonthlySpent = avgSpent,
            averageMonthlyReceived = avgReceived,
            averageMonthlySavings = avgSavings,
            highestSpendMonth = highestSpend,
            highestSavingsMonth = highestSavings,
            selectedMetric = selectedMetric,
            isLoading = false
        )
    }
}
