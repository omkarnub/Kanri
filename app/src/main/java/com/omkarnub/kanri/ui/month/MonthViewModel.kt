package com.omkarnub.kanri.ui.month

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.home.parseHexColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategorySpendItem(
    val categoryId: Long?,
    val name: String,
    val emoji: String,
    val color: Color,
    val totalAmount: Double,
    val percentage: Float
)

data class DailySpendItem(
    val day: Int,
    val totalAmount: Double
)

data class MonthUiState(
    val monthTitle: String = "",
    val monthKey: String = "",
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val netBalance: Double = 0.0,
    val categorySpends: List<CategorySpendItem> = emptyList(),
    val dailySpends: List<DailySpendItem> = emptyList(),
    val daysInMonth: Int = 30,
    val currentDay: Int = 1,
    val isCurrentMonth: Boolean = true,
    val selectedYear: Int = 0,
    val selectedMonth: Int = 0,
    val transactionCount: Int = 0,
    val isLoading: Boolean = false
)

class MonthViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    private val selectedCalendar = MutableStateFlow(Calendar.getInstance())

    val uiState: StateFlow<MonthUiState> = combine(
        transactionDao.getTransactionsWithCategory(),
        categoryDao.getAllCategories(),
        selectedCalendar
    ) { txList, _, cal ->
        val calClone = cal.clone() as Calendar

        // Month Boundaries
        calClone.set(Calendar.DAY_OF_MONTH, 1)
        calClone.set(Calendar.HOUR_OF_DAY, 0)
        calClone.set(Calendar.MINUTE, 0)
        calClone.set(Calendar.SECOND, 0)
        calClone.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calClone.timeInMillis

        val daysInMonth = calClone.getActualMaximum(Calendar.DAY_OF_MONTH)
        calClone.set(Calendar.DAY_OF_MONTH, daysInMonth)
        calClone.set(Calendar.HOUR_OF_DAY, 23)
        calClone.set(Calendar.MINUTE, 59)
        calClone.set(Calendar.SECOND, 59)
        calClone.set(Calendar.MILLISECOND, 999)
        val endOfMonth = calClone.timeInMillis

        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val monthTitle = monthFormatter.format(cal.time)
        val keyFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthKey = keyFormatter.format(cal.time)

        val nowCal = Calendar.getInstance()
        val isCurrentMonth = nowCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        val currentDay = if (isCurrentMonth) nowCal.get(Calendar.DAY_OF_MONTH) else daysInMonth

        // Filter transactions
        val monthTransactions = txList.filter {
            it.transaction.timestamp in startOfMonth..endOfMonth
        }

        var spent = 0.0
        var received = 0.0

        val categoryAmountMap = mutableMapOf<Long?, Double>()
        val categoryObjectMap = mutableMapOf<Long?, com.omkarnub.kanri.data.db.CategoryEntity?>()
        val dailyMap = mutableMapOf<Int, Double>()

        for (day in 1..daysInMonth) {
            dailyMap[day] = 0.0
        }

        for (item in monthTransactions) {
            val tx = item.transaction
            val isDebit = tx.type.equals("DEBIT", ignoreCase = true)
            val isCredit = tx.type.equals("CREDIT", ignoreCase = true)

            if (isDebit) {
                spent += tx.amount
                val catId = tx.categoryId
                categoryAmountMap[catId] = (categoryAmountMap[catId] ?: 0.0) + tx.amount
                if (!categoryObjectMap.containsKey(catId)) {
                    categoryObjectMap[catId] = item.category
                }

                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val day = txCal.get(Calendar.DAY_OF_MONTH)
                dailyMap[day] = (dailyMap[day] ?: 0.0) + tx.amount
            } else if (isCredit) {
                received += tx.amount
            }
        }

        val categorySpends = categoryAmountMap.map { (catId, amount) ->
            val cat = categoryObjectMap[catId]
            val name = cat?.name ?: "Uncategorized"
            val emoji = if (cat != null) getEmojiForCategory(cat.name) else "❓"
            val color = if (cat != null) parseHexColor(cat.colorHex) else Color(0xFF6E7681)
            val percentage = if (spent > 0) ((amount / spent) * 100).toFloat() else 0f

            CategorySpendItem(
                categoryId = catId,
                name = name,
                emoji = emoji,
                color = color,
                totalAmount = amount,
                percentage = percentage
            )
        }.sortedByDescending { it.totalAmount }

        val dailySpends = dailyMap.map { (day, amount) ->
            DailySpendItem(day = day, totalAmount = amount)
        }.sortedBy { it.day }

        MonthUiState(
            monthTitle = monthTitle,
            monthKey = monthKey,
            totalSpent = spent,
            totalReceived = received,
            netBalance = received - spent,
            categorySpends = categorySpends,
            dailySpends = dailySpends,
            daysInMonth = daysInMonth,
            currentDay = currentDay,
            isCurrentMonth = isCurrentMonth,
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH),
            transactionCount = monthTransactions.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthUiState(isLoading = true)
    )

    private val selectedTrendMetric = MutableStateFlow(TrendMetricType.ALL)
    private val scrubbedTrendIndex = MutableStateFlow<Int?>(null)

    val allTimeTrendsUiState: StateFlow<AllTimeTrendsUiState> = combine(
        transactionDao.getAllTransactions(),
        selectedTrendMetric,
        scrubbedTrendIndex
    ) { allTxs, metric, scrubIdx ->
        val calculated = AllTimeTrendsCalculator.calculate(
            transactions = allTxs,
            selectedMetric = metric
        )
        calculated.copy(scrubbedIndex = scrubIdx)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AllTimeTrendsUiState(isLoading = true)
    )

    fun selectTrendMetric(metric: TrendMetricType) {
        selectedTrendMetric.value = metric
    }

    fun setScrubbedMonth(index: Int?) {
        scrubbedTrendIndex.value = index
    }

    fun selectMonth(year: Int, month: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        selectedCalendar.value = cal
    }

    fun previousMonth() {
        val cal = selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        selectedCalendar.value = cal
    }

    fun nextMonth() {
        val cal = selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        selectedCalendar.value = cal
    }

    companion object {
        fun getEmojiForCategory(categoryName: String): String {
            return when (categoryName.lowercase()) {
                "food & dining" -> "🍔"
                "groceries" -> "🛒"
                "shopping" -> "🛍️"
                "transport" -> "🚗"
                "bills & utilities" -> "💡"
                "salary & income" -> "💰"
                "health & medical" -> "🏥"
                "cash & atm" -> "🏧"
                else -> "📦"
            }
        }
    }
}
