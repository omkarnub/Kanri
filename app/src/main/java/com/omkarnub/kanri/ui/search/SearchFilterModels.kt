package com.omkarnub.kanri.ui.search

import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import java.util.Calendar

enum class TransactionTypeFilter(val displayName: String) {
    ALL("All Types"),
    DEBIT("Spent (Debit)"),
    CREDIT("Received (Credit)")
}

enum class DateRangePreset(val displayName: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year"),
    CUSTOM("Custom")
}

enum class AmountRangePreset(
    val displayName: String,
    val min: Double?,
    val max: Double?
) {
    ALL("All Amounts", null, null),
    UNDER_500("Under ₹500", null, 500.0),
    FROM_500_TO_2000("₹500 – ₹2,000", 500.0, 2000.0),
    FROM_2000_TO_10000("₹2,000 – ₹10,000", 2000.0, 10000.0),
    OVER_10000("Over ₹10,000", 10000.0, null),
    CUSTOM("Custom", null, null)
}

enum class SortOption(val displayName: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

data class SearchUiState(
    val query: String = "",
    val typeFilter: TransactionTypeFilter = TransactionTypeFilter.ALL,
    val datePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedSourceTypes: Set<String> = emptySet(),
    val amountPreset: AmountRangePreset = AmountRangePreset.ALL,
    val customMinAmount: Double? = null,
    val customMaxAmount: Double? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val filteredTransactions: List<TransactionWithCategory> = emptyList(),
    val availableCategories: List<CategoryEntity> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netAmount: Double = 0.0,
    val isLoading: Boolean = false
) {
    val activeFilterCount: Int
        get() {
            var count = 0
            if (typeFilter != TransactionTypeFilter.ALL) count++
            if (datePreset != DateRangePreset.ALL_TIME) count++
            if (selectedCategoryIds.isNotEmpty()) count++
            if (selectedSourceTypes.isNotEmpty()) count++
            if (amountPreset != AmountRangePreset.ALL) count++
            return count
        }
}

object SearchFilterUtils {

    fun getDateRangeBounds(
        preset: DateRangePreset,
        customStart: Long? = null,
        customEnd: Long? = null,
        referenceMillis: Long = System.currentTimeMillis()
    ): Pair<Long?, Long?> {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }

        return when (preset) {
            DateRangePreset.ALL_TIME -> Pair(null, null)
            DateRangePreset.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangePreset.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangePreset.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangePreset.LAST_30_DAYS -> {
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Pair(start, end)
            }
            DateRangePreset.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangePreset.CUSTOM -> Pair(customStart, customEnd)
        }
    }

    fun matchesQuery(item: TransactionWithCategory, query: String): Boolean {
        if (query.isBlank()) return true
        val cleanQuery = query.trim().lowercase()
        val tx = item.transaction
        val cat = item.category

        if (tx.counterparty?.lowercase()?.contains(cleanQuery) == true) return true
        if (tx.bank?.lowercase()?.contains(cleanQuery) == true) return true
        if (tx.refNo?.lowercase()?.contains(cleanQuery) == true) return true
        if (tx.displayName?.lowercase()?.contains(cleanQuery) == true) return true
        if (tx.sourceType.lowercase().contains(cleanQuery)) return true
        if (cat?.name?.lowercase()?.contains(cleanQuery) == true) return true

        return false
    }

    fun matchesFilter(
        item: TransactionWithCategory,
        query: String,
        typeFilter: TransactionTypeFilter,
        startTime: Long?,
        endTime: Long?,
        categoryIds: Set<Long>,
        sourceTypes: Set<String>,
        minAmount: Double?,
        maxAmount: Double?
    ): Boolean {
        val tx = item.transaction

        // Query check
        if (!matchesQuery(item, query)) return false

        // Type check
        when (typeFilter) {
            TransactionTypeFilter.DEBIT -> if (!tx.type.equals("DEBIT", ignoreCase = true)) return false
            TransactionTypeFilter.CREDIT -> if (!tx.type.equals("CREDIT", ignoreCase = true)) return false
            TransactionTypeFilter.ALL -> Unit
        }

        // Date check
        if (startTime != null && tx.timestamp < startTime) return false
        if (endTime != null && tx.timestamp > endTime) return false

        // Category check
        if (categoryIds.isNotEmpty()) {
            val catId = item.category?.id ?: -1L
            if (!categoryIds.contains(catId)) return false
        }

        // Source type check
        if (sourceTypes.isNotEmpty()) {
            if (!sourceTypes.any { it.equals(tx.sourceType, ignoreCase = true) }) return false
        }

        // Amount check
        if (minAmount != null && tx.amount < minAmount) return false
        if (maxAmount != null && tx.amount > maxAmount) return false

        return true
    }
}
