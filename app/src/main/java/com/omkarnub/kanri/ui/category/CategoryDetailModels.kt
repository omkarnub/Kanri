package com.omkarnub.kanri.ui.category

import androidx.compose.ui.graphics.Color
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CategoryTimeScope(val label: String) {
    THIS_MONTH("This Month"),
    LAST_3_MONTHS("Last 3M"),
    LAST_6_MONTHS("Last 6M"),
    ALL_TIME("All Time")
}

data class MonthlyTrendPoint(
    val year: Int,
    val month: Int, // 0-based: 0 = January, 11 = December
    val monthLabel: String, // e.g. "Apr", "May"
    val totalSpent: Double,
    val transactionCount: Int
)

data class TopPayeeItem(
    val name: String,
    val amount: Double,
    val percentage: Float, // 0..100
    val transactionCount: Int
)

data class CategoryDetailUiState(
    val categoryId: Long? = null,
    val categoryName: String = "",
    val emoji: String = "📦",
    val color: Color = Color(0xFF58A6FF),
    val selectedScope: CategoryTimeScope = CategoryTimeScope.THIS_MONTH,
    val currentMonthSpent: Double = 0.0,
    val previousMonthSpent: Double = 0.0,
    val monthOverMonthDeltaPercent: Float? = null, // null if no previous month spending
    val categorySharePercent: Float = 0f,
    val sixMonthTrend: List<MonthlyTrendPoint> = emptyList(),
    val sixMonthAvg: Double = 0.0,
    val peakMonth: MonthlyTrendPoint? = null,
    val topPayees: List<TopPayeeItem> = emptyList(),
    val filteredTransactions: List<TransactionWithCategory> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val totalInScope: Double = 0.0,
    val countInScope: Int = 0,
    val isLoading: Boolean = true
)

object CategoryDetailUtils {

    /**
     * Calculates month-over-month spending change in percentage.
     * Returns null if previous month was 0.0, or percentage (e.g. +25.0f, -10.5f).
     */
    fun calculateMoMDeltaPercent(currentSpent: Double, previousSpent: Double): Float? {
        if (previousSpent <= 0.0) return null
        val diff = currentSpent - previousSpent
        return ((diff / previousSpent) * 100.0).toFloat()
    }

    /**
     * Builds the 6-month historical trend line points relative to targetYear and targetMonth (0-indexed).
     * Returns a list of exactly 6 MonthlyTrendPoints in chronological order (oldest to newest).
     */
    fun computeSixMonthTrend(
        transactions: List<TransactionWithCategory>,
        categoryId: Long?,
        targetYear: Int,
        targetMonth: Int
    ): List<MonthlyTrendPoint> {
        val result = mutableListOf<MonthlyTrendPoint>()
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())

        // Generate the 6 calendar intervals
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, targetMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -i)
            }

            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val startMs = cal.timeInMillis

            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val endCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, maxDays)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endMs = endCal.timeInMillis

            var monthSpend = 0.0
            var txCount = 0

            for (item in transactions) {
                val tx = item.transaction
                val matchesCategory = if (categoryId == null) {
                    tx.categoryId == null
                } else {
                    tx.categoryId == categoryId
                }

                if (matchesCategory && tx.type.equals("DEBIT", ignoreCase = true) && tx.timestamp in startMs..endMs) {
                    monthSpend += tx.amount
                    txCount++
                }
            }

            result.add(
                MonthlyTrendPoint(
                    year = y,
                    month = m,
                    monthLabel = monthFormat.format(cal.time),
                    totalSpent = monthSpend,
                    transactionCount = txCount
                )
            )
        }

        return result
    }

    /**
     * Filters transactions belonging to categoryId by the given scope relative to targetYear & targetMonth.
     */
    fun filterTransactionsByScope(
        transactions: List<TransactionWithCategory>,
        categoryId: Long?,
        scope: CategoryTimeScope,
        targetYear: Int,
        targetMonth: Int
    ): List<TransactionWithCategory> {
        val (startMs, endMs) = calculateScopeBounds(scope, targetYear, targetMonth)

        return transactions.filter { item ->
            val tx = item.transaction
            val matchesCategory = if (categoryId == null) {
                tx.categoryId == null
            } else {
                tx.categoryId == categoryId
            }

            val inTime = if (startMs == null || endMs == null) {
                true
            } else {
                tx.timestamp in startMs..endMs
            }

            matchesCategory && inTime
        }.sortedByDescending { it.transaction.timestamp }
    }

    /**
     * Computes the timestamp range [startMs, endMs] for a given time scope.
     * Returns Pair(null, null) for ALL_TIME.
     */
    fun calculateScopeBounds(
        scope: CategoryTimeScope,
        targetYear: Int,
        targetMonth: Int
    ): Pair<Long?, Long?> {
        if (scope == CategoryTimeScope.ALL_TIME) return Pair(null, null)

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, maxDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endMs = endCal.timeInMillis

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (scope) {
            CategoryTimeScope.THIS_MONTH -> {
                // Already set to start of month
            }
            CategoryTimeScope.LAST_3_MONTHS -> {
                startCal.add(Calendar.MONTH, -2) // 3 months inclusive
            }
            CategoryTimeScope.LAST_6_MONTHS -> {
                startCal.add(Calendar.MONTH, -5) // 6 months inclusive
            }
            CategoryTimeScope.ALL_TIME -> return Pair(null, null)
        }

        return Pair(startCal.timeInMillis, endMs)
    }

    /**
     * Aggregates transactions into top payees sorted by total spend descending.
     */
    fun computeTopPayees(
        transactions: List<TransactionWithCategory>,
        maxCount: Int = 5
    ): List<TopPayeeItem> {
        val debitTxs = transactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val totalDebit = debitTxs.sumOf { it.transaction.amount }
        if (totalDebit <= 0.0) return emptyList()

        val payeeMap = mutableMapOf<String, MutableList<Double>>()

        for (item in debitTxs) {
            val rawName = item.transaction.counterparty?.trim()
            val name = when {
                !rawName.isNullOrBlank() -> rawName
                !item.transaction.displayName.isNullOrBlank() -> item.transaction.displayName.trim()
                !item.transaction.sourceType.isNullOrBlank() -> item.transaction.sourceType.trim()
                else -> "Unknown Payee"
            }
            payeeMap.getOrPut(name) { mutableListOf() }.add(item.transaction.amount)
        }

        return payeeMap.map { (name, amounts) ->
            val sum = amounts.sum()
            val percentage = ((sum / totalDebit) * 100.0).toFloat().coerceIn(0f, 100f)
            TopPayeeItem(
                name = name,
                amount = sum,
                percentage = percentage,
                transactionCount = amounts.size
            )
        }.sortedByDescending { it.amount }.take(maxCount)
    }
}
