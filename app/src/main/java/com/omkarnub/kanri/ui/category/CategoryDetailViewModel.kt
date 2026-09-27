package com.omkarnub.kanri.ui.category

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.ui.home.parseHexColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategoryDetailFilterConfig(
    val categoryId: Long? = null,
    val categoryName: String = "",
    val emoji: String = "📦",
    val color: Color = Color(0xFF58A6FF),
    val targetYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val targetMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedScope: CategoryTimeScope = CategoryTimeScope.THIS_MONTH
)

class CategoryDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    private val configFlow = MutableStateFlow(CategoryDetailFilterConfig())

    val uiState: StateFlow<CategoryDetailUiState> = combine(
        transactionDao.getTransactionsWithCategory(),
        categoryDao.getAllCategories(),
        configFlow
    ) { txList, catList, config ->
        val catId = config.categoryId
        val targetYear = config.targetYear
        val targetMonth = config.targetMonth
        val themeColor = config.color

        // 1. Compute Six Month Trend
        val sixMonthTrend = CategoryDetailUtils.computeSixMonthTrend(
            transactions = txList,
            categoryId = catId,
            targetYear = targetYear,
            targetMonth = targetMonth
        )

        // Current month & previous month spend
        val currentMonthPoint = sixMonthTrend.lastOrNull()
        val prevMonthPoint = if (sixMonthTrend.size >= 2) sixMonthTrend[sixMonthTrend.size - 2] else null

        val currentMonthSpent = currentMonthPoint?.totalSpent ?: 0.0
        val prevMonthSpent = prevMonthPoint?.totalSpent ?: 0.0

        val momDeltaPercent = CategoryDetailUtils.calculateMoMDeltaPercent(
            currentSpent = currentMonthSpent,
            previousSpent = prevMonthSpent
        )

        // 2. Compute category share of overall spending in the target month
        val (monthStartMs, monthEndMs) = CategoryDetailUtils.calculateScopeBounds(
            CategoryTimeScope.THIS_MONTH,
            targetYear,
            targetMonth
        )
        var totalOverallMonthDebit = 0.0
        if (monthStartMs != null && monthEndMs != null) {
            for (item in txList) {
                val tx = item.transaction
                if (tx.type.equals("DEBIT", ignoreCase = true) && tx.timestamp in monthStartMs..monthEndMs) {
                    totalOverallMonthDebit += tx.amount
                }
            }
        }
        val categorySharePercent = if (totalOverallMonthDebit > 0.0) {
            ((currentMonthSpent / totalOverallMonthDebit) * 100.0).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }

        // 3. Six month average & Peak Month
        val nonZeroMonths = sixMonthTrend.filter { it.totalSpent > 0.0 }
        val sixMonthAvg = if (nonZeroMonths.isNotEmpty()) {
            sixMonthTrend.sumOf { it.totalSpent } / sixMonthTrend.size
        } else {
            0.0
        }
        val peakMonth = sixMonthTrend.maxByOrNull { it.totalSpent }?.takeIf { it.totalSpent > 0.0 }

        // 4. Filtered transactions for the chosen scope
        val filteredTxs = CategoryDetailUtils.filterTransactionsByScope(
            transactions = txList,
            categoryId = catId,
            scope = config.selectedScope,
            targetYear = targetYear,
            targetMonth = targetMonth
        )

        // 5. Top Payees for the filtered scope
        val topPayees = CategoryDetailUtils.computeTopPayees(filteredTxs, maxCount = 5)

        // 6. Running stats for scope
        val totalInScope = filteredTxs.filter {
            it.transaction.type.equals("DEBIT", ignoreCase = true)
        }.sumOf { it.transaction.amount }

        CategoryDetailUiState(
            categoryId = catId,
            categoryName = config.categoryName,
            emoji = config.emoji,
            color = themeColor,
            selectedScope = config.selectedScope,
            currentMonthSpent = currentMonthSpent,
            previousMonthSpent = prevMonthSpent,
            monthOverMonthDeltaPercent = momDeltaPercent,
            categorySharePercent = categorySharePercent,
            sixMonthTrend = sixMonthTrend,
            sixMonthAvg = sixMonthAvg,
            peakMonth = peakMonth,
            topPayees = topPayees,
            filteredTransactions = filteredTxs,
            availableCategories = catList,
            totalInScope = totalInScope,
            countInScope = filteredTxs.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoryDetailUiState(isLoading = true)
    )

    fun initCategory(
        categoryId: Long?,
        categoryName: String,
        emoji: String,
        color: Color,
        targetYear: Int,
        targetMonth: Int
    ) {
        configFlow.value = configFlow.value.copy(
            categoryId = categoryId,
            categoryName = categoryName,
            emoji = emoji,
            color = color,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
    }

    fun setScope(scope: CategoryTimeScope) {
        configFlow.value = configFlow.value.copy(selectedScope = scope)
    }

    fun updateTransactionCategory(
        transactionId: Long,
        newCategoryId: Long,
        counterparty: String?,
        note: String? = null,
        applyToAll: Boolean = false
    ) {
        viewModelScope.launch {
            transactionDao.updateCategoryAndNotes(transactionId, newCategoryId, note)
            com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper.onTransactionCategoryChanged(transactionId, newCategoryId, db, getApplication())
            if (applyToAll && !counterparty.isNullOrBlank()) {
                transactionDao.updateCategoryForCounterparty(counterparty, newCategoryId)
                categoryDao.setMapping(
                    com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity(
                        counterparty = counterparty.trim().lowercase(),
                        categoryId = newCategoryId
                    )
                )
            }
        }
    }
}
