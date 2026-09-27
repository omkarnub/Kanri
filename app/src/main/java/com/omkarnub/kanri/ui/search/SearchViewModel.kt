package com.omkarnub.kanri.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.db.KanriDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchFilterState(
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
    val sortOption: SortOption = SortOption.DATE_DESC
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()

    private val _filterState = MutableStateFlow(SearchFilterState())

    // Flow combining filters with database
    val uiState: StateFlow<SearchUiState> = combine(
        transactionDao.getTransactionsWithCategory(),
        categoryDao.getAllCategories(),
        _filterState
    ) { txList, categories, filters ->
        val (startTime, endTime) = SearchFilterUtils.getDateRangeBounds(
            preset = filters.datePreset,
            customStart = filters.customStartDate,
            customEnd = filters.customEndDate
        )

        val minAmount = if (filters.amountPreset == AmountRangePreset.CUSTOM) filters.customMinAmount else filters.amountPreset.min
        val maxAmount = if (filters.amountPreset == AmountRangePreset.CUSTOM) filters.customMaxAmount else filters.amountPreset.max

        val filtered = txList.filter { item ->
            SearchFilterUtils.matchesFilter(
                item = item,
                query = filters.query,
                typeFilter = filters.typeFilter,
                startTime = startTime,
                endTime = endTime,
                categoryIds = filters.selectedCategoryIds,
                sourceTypes = filters.selectedSourceTypes,
                minAmount = minAmount,
                maxAmount = maxAmount
            )
        }

        val sorted = when (filters.sortOption) {
            SortOption.DATE_DESC -> filtered.sortedByDescending { it.transaction.timestamp }
            SortOption.DATE_ASC -> filtered.sortedBy { it.transaction.timestamp }
            SortOption.AMOUNT_DESC -> filtered.sortedByDescending { it.transaction.amount }
            SortOption.AMOUNT_ASC -> filtered.sortedBy { it.transaction.amount }
        }

        var spent = 0.0
        var received = 0.0
        for (item in sorted) {
            if (item.transaction.type.equals("DEBIT", ignoreCase = true)) {
                spent += item.transaction.amount
            } else if (item.transaction.type.equals("CREDIT", ignoreCase = true)) {
                received += item.transaction.amount
            }
        }

        SearchUiState(
            query = filters.query,
            typeFilter = filters.typeFilter,
            datePreset = filters.datePreset,
            customStartDate = filters.customStartDate,
            customEndDate = filters.customEndDate,
            selectedCategoryIds = filters.selectedCategoryIds,
            selectedSourceTypes = filters.selectedSourceTypes,
            amountPreset = filters.amountPreset,
            customMinAmount = filters.customMinAmount,
            customMaxAmount = filters.customMaxAmount,
            sortOption = filters.sortOption,
            filteredTransactions = sorted,
            availableCategories = categories,
            totalSpent = spent,
            totalReceived = received,
            netAmount = received - spent,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState(isLoading = true)
    )

    fun applyFilterState(state: SearchFilterState) {
        _filterState.value = state
    }

    fun setQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun setTypeFilter(type: TransactionTypeFilter) {
        _filterState.update { it.copy(typeFilter = type) }
    }

    fun setDatePreset(preset: DateRangePreset) {
        _filterState.update { it.copy(datePreset = preset) }
    }

    fun setCustomDateRange(start: Long?, end: Long?) {
        _filterState.update {
            it.copy(
                customStartDate = start,
                customEndDate = end,
                datePreset = DateRangePreset.CUSTOM
            )
        }
    }

    fun toggleCategory(categoryId: Long) {
        _filterState.update { current ->
            val set = current.selectedCategoryIds.toMutableSet()
            if (set.contains(categoryId)) {
                set.remove(categoryId)
            } else {
                set.add(categoryId)
            }
            current.copy(selectedCategoryIds = set)
        }
    }

    fun clearCategories() {
        _filterState.update { it.copy(selectedCategoryIds = emptySet()) }
    }

    fun toggleSourceType(sourceType: String) {
        _filterState.update { current ->
            val set = current.selectedSourceTypes.toMutableSet()
            val match = set.firstOrNull { it.equals(sourceType, ignoreCase = true) }
            if (match != null) {
                set.remove(match)
            } else {
                set.add(sourceType)
            }
            current.copy(selectedSourceTypes = set)
        }
    }

    fun clearSourceTypes() {
        _filterState.update { it.copy(selectedSourceTypes = emptySet()) }
    }

    fun setAmountPreset(preset: AmountRangePreset, customMin: Double? = null, customMax: Double? = null) {
        _filterState.update {
            if (preset == AmountRangePreset.CUSTOM) {
                it.copy(
                    amountPreset = preset,
                    customMinAmount = customMin,
                    customMaxAmount = customMax
                )
            } else {
                it.copy(
                    amountPreset = preset,
                    customMinAmount = preset.min,
                    customMaxAmount = preset.max
                )
            }
        }
    }

    fun setSortOption(sort: SortOption) {
        _filterState.update { it.copy(sortOption = sort) }
    }

    fun clearAllFilters() {
        _filterState.value = SearchFilterState()
    }

    fun updateTransactionCategory(transactionId: Long, categoryId: Long, note: String? = null) {
        viewModelScope.launch {
            transactionDao.updateCategoryAndNotes(transactionId, categoryId, note)
            com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper.onTransactionCategoryChanged(transactionId, categoryId, db, getApplication())
        }
    }
}
