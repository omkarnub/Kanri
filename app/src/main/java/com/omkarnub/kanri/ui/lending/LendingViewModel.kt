package com.omkarnub.kanri.ui.lending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LendingFilter {
    ALL,
    LENT,
    BORROWED,
    PENDING_ONLY
}

data class LendingUiState(
    val records: List<LendingEntity> = emptyList(),
    val totalLentPending: Double = 0.0,
    val totalBorrowedPending: Double = 0.0,
    val netBalance: Double = 0.0,
    val activeFilter: LendingFilter = LendingFilter.ALL,
    val isLoading: Boolean = false
)

class LendingViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val lendingDao = db.lendingDao()

    private val activeFilterFlow = MutableStateFlow(LendingFilter.ALL)

    val uiState: StateFlow<LendingUiState> = combine(
        lendingDao.getAllRecords(),
        activeFilterFlow
    ) { allRecords, filter ->
        var lentPending = 0.0
        var borrowedPending = 0.0

        for (r in allRecords) {
            if (!r.isSettled) {
                if (r.type.equals("LENT", ignoreCase = true)) {
                    lentPending += r.amount
                } else if (r.type.equals("BORROWED", ignoreCase = true)) {
                    borrowedPending += r.amount
                }
            }
        }

        val filteredList = when (filter) {
            LendingFilter.ALL -> allRecords
            LendingFilter.LENT -> allRecords.filter { it.type.equals("LENT", ignoreCase = true) }
            LendingFilter.BORROWED -> allRecords.filter { it.type.equals("BORROWED", ignoreCase = true) }
            LendingFilter.PENDING_ONLY -> allRecords.filter { !it.isSettled }
        }

        LendingUiState(
            records = filteredList,
            totalLentPending = lentPending,
            totalBorrowedPending = borrowedPending,
            netBalance = lentPending - borrowedPending,
            activeFilter = filter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LendingUiState(isLoading = true)
    )

    fun setFilter(filter: LendingFilter) {
        activeFilterFlow.value = filter
    }

    fun addRecord(
        personName: String,
        amount: Double,
        type: String,
        dueDate: Long? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val record = LendingEntity(
                personName = personName.trim(),
                amount = amount,
                type = type.uppercase(),
                date = System.currentTimeMillis(),
                dueDate = dueDate,
                isSettled = false,
                notes = notes?.trim()?.ifBlank { null }
            )
            lendingDao.insert(record)
        }
    }

    fun toggleSettled(record: LendingEntity) {
        viewModelScope.launch {
            lendingDao.setSettled(record.id, !record.isSettled)
        }
    }

    fun deleteRecord(record: LendingEntity) {
        viewModelScope.launch {
            lendingDao.delete(record)
        }
    }

    fun addSplitAsLender(
        friendNames: List<String>,
        perPersonShare: Double,
        eventDescription: String,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val notePrefix = if (eventDescription.isNotBlank()) "Split: ${eventDescription.trim()}" else "Split Bill"
            friendNames.forEachIndexed { index, name ->
                val trimmedName = name.trim().ifBlank { "Friend ${index + 1}" }
                val record = LendingEntity(
                    personName = trimmedName,
                    amount = perPersonShare,
                    type = "LENT",
                    date = now,
                    dueDate = dueDate,
                    isSettled = false,
                    notes = "$notePrefix (1 share)"
                )
                lendingDao.insert(record)
            }
        }
    }

    fun addSplitAsBorrower(
        payerName: String,
        userShare: Double,
        eventDescription: String,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val trimmedPayer = payerName.trim().ifBlank { "Friend" }
            val notePrefix = if (eventDescription.isNotBlank()) "Split: ${eventDescription.trim()}" else "Split Bill"
            val record = LendingEntity(
                personName = trimmedPayer,
                amount = userShare,
                type = "BORROWED",
                date = now,
                dueDate = dueDate,
                isSettled = false,
                notes = "$notePrefix (My share)"
            )
            lendingDao.insert(record)
        }
    }
}
