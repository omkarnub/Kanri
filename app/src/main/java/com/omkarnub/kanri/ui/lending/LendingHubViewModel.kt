package com.omkarnub.kanri.ui.lending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingDao
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.data.lending.LendingMoneyEngine
import com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class LendingHubViewModel @JvmOverloads constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    dao: LendingDao? = null,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) : AndroidViewModel(application) {

    private val db: KanriDatabase = KanriDatabase.getDatabase(application)
    private val lendingDao: LendingDao = dao ?: db.lendingDao()

    init {
        viewModelScope.launch {
            LendingTransactionSyncHelper.syncAllHistoricalRecords(db, application)
        }
    }

    private val _selectedTab = MutableStateFlow(
        LendingTabOption.valueOf(
            savedStateHandle["selected_tab"] ?: LendingTabOption.PEOPLE.name
        )
    )

    private val _selectedPersonKey = MutableStateFlow<String?>(
        savedStateHandle["selected_person_key"]
    )

    private val _timelineFilter = MutableStateFlow(
        TimelineFilter.valueOf(
            savedStateHandle["timeline_filter"] ?: TimelineFilter.ALL.name
        )
    )

    private val _isSettledExpanded = MutableStateFlow(
        savedStateHandle["is_settled_expanded"] ?: false
    )

    private val _pendingMergePrompt = MutableStateFlow<MergeConfirmationPrompt?>(null)

    // Undo delete cache (cached for ~5 seconds)
    private val _deletedRecordCache = MutableStateFlow<LendingWithRepayments?>(null)
    private var undoJob: Job? = null

    val uiState: StateFlow<LendingHubUiState> = combine(
        lendingDao.getAllRecordsWithRepayments(),
        lendingDao.getTotalLentPending(),
        lendingDao.getTotalBorrowedPending(),
        _selectedTab,
        _selectedPersonKey,
        _timelineFilter,
        _isSettledExpanded,
        _pendingMergePrompt,
        _deletedRecordCache
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val records = (args[0] as? List<LendingWithRepayments>) ?: emptyList()
        val totalLent = (args[1] as? Double) ?: 0.0
        val totalBorrowed = (args[2] as? Double) ?: 0.0
        val selectedTab = args[3] as LendingTabOption
        val selectedPersonKey = args[4] as? String
        val timelineFilter = args[5] as TimelineFilter
        val isSettledExpanded = args[6] as Boolean
        val pendingMergePrompt = args[7] as? MergeConfirmationPrompt
        val deletedRecord = args[8] as? LendingWithRepayments

        val now = timeProvider()
        val startOfToday = getStartOfTodayMillis(now)

        // 1. Hero Totals (exact match to Home Pulse Card)
        val netPosition = totalLent - totalBorrowed
        val isAllSettled = totalLent <= 0.0001 && totalBorrowed <= 0.0001
        val heroTotals = HeroTotals(
            toReceive = totalLent,
            toPay = totalBorrowed,
            netPosition = netPosition,
            isAllSettled = isAllSettled
        )

        // 2. Aggregate by Person
        val peopleSummaries = aggregatePeople(records, startOfToday)

        // Partition into sections
        val needsAttention = peopleSummaries.filter { it.openCount > 0 && it.needsAttention }
            .sortedWith(
                compareByDescending<PersonSummary> { it.isOverdue }
                    .thenBy { it.nextDueDate ?: Long.MAX_VALUE }
            )

        val activePeople = peopleSummaries.filter { it.openCount > 0 && !it.needsAttention }
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }

        val settledPeople = peopleSummaries.filter { it.openCount == 0 && it.hasHistory }
            .sortedByDescending { it.lastActivity }

        // 3. Aggregate Timeline View
        val filteredTimelineRecords = filterTimelineRecords(records, timelineFilter, startOfToday)
        val timelineGroups = groupRecordsByMonth(filteredTimelineRecords)

        // 4. Selected Person Summary
        val selectedPersonSummary = selectedPersonKey?.let { key ->
            peopleSummaries.firstOrNull { it.key == key }
        }

        LendingHubUiState(
            isLoading = false,
            selectedTab = selectedTab,
            selectedPersonKey = selectedPersonKey,
            timelineFilter = timelineFilter,
            isSettledSectionExpanded = isSettledExpanded,
            heroTotals = heroTotals,
            needsAttentionPeople = needsAttention,
            activePeople = activePeople,
            settledPeople = settledPeople,
            allPeopleSummaries = peopleSummaries,
            timelineGroups = timelineGroups,
            selectedPersonSummary = selectedPersonSummary,
            pendingMergePrompt = pendingMergePrompt,
            canUndoDelete = deletedRecord != null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LendingHubUiState(isLoading = true)
    )

    // Navigation and UI selections
    fun selectTab(tab: LendingTabOption) {
        _selectedTab.value = tab
        savedStateHandle["selected_tab"] = tab.name
    }

    fun selectPerson(personKey: String?) {
        _selectedPersonKey.value = personKey
        savedStateHandle["selected_person_key"] = personKey
    }

    fun clearSelectedPerson() {
        selectPerson(null)
    }

    fun setTimelineFilter(filter: TimelineFilter) {
        _timelineFilter.value = filter
        savedStateHandle["timeline_filter"] = filter.name
    }

    fun toggleSettledSection() {
        val next = !_isSettledExpanded.value
        _isSettledExpanded.value = next
        savedStateHandle["is_settled_expanded"] = next
    }

    // Money Actions
    fun addEntry(
        personName: String,
        amount: Double,
        type: String,
        date: Long = timeProvider(),
        dueDate: Long? = null,
        notes: String? = null
    ) {
        val normalizedName = cleanPersonName(personName)
        require(normalizedName.isNotEmpty()) { "Person name cannot be blank" }
        require(normalizedName.length <= 40) { "Person name cannot exceed 40 characters" }
        require(amount > 0.0) { "Amount must be greater than zero" }
        if (dueDate != null) {
            require(dueDate >= date) { "Due date cannot be earlier than entry date" }
        }

        viewModelScope.launch {
            val recordId = lendingDao.insert(
                LendingEntity(
                    personName = normalizedName,
                    amount = amount,
                    type = type.uppercase(Locale.ROOT),
                    date = date,
                    dueDate = dueDate,
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
            LendingTransactionSyncHelper.syncLendingRecord(recordId, db, getApplication())
            if (dueDate != null) {
                com.omkarnub.kanri.data.lending.LendingReminderScheduler.checkNow(getApplication())
            }
        }
    }

    fun editEntry(
        id: Long,
        personName: String,
        amount: Double,
        type: String,
        date: Long,
        dueDate: Long?,
        notes: String?
    ) {
        val normalizedName = cleanPersonName(personName)
        require(normalizedName.isNotEmpty()) { "Person name cannot be blank" }
        require(amount > 0.0) { "Amount must be greater than zero" }

        viewModelScope.launch {
            val existing = lendingDao.getRecordById(id) ?: return@launch
            val repayments = lendingDao.getRepaymentsForLending(id)
            if (repayments.isNotEmpty()) {
                // Cannot edit amount or type once repayments exist
                require(amount == existing.amount && type.equals(existing.type, ignoreCase = true)) {
                    "Cannot edit amount or type when repayments exist. Undo repayments first."
                }
            }
            lendingDao.update(
                existing.copy(
                    personName = normalizedName,
                    amount = amount,
                    type = type.uppercase(Locale.ROOT),
                    date = date,
                    dueDate = dueDate,
                    notes = notes?.trim()?.ifBlank { null }
                )
            )
            LendingTransactionSyncHelper.syncLendingRecord(id, db, getApplication())
            if (dueDate != null) {
                com.omkarnub.kanri.data.lending.LendingReminderScheduler.checkNow(getApplication())
            }
        }
    }

    fun repayEntry(
        id: Long,
        repaymentAmount: Double,
        paidAt: Long = timeProvider(),
        note: String? = null
    ) {
        viewModelScope.launch {
            val result = lendingDao.repayRecord(id, repaymentAmount, paidAt, note?.trim()?.ifBlank { null })
            result.onSuccess { repaymentId ->
                LendingTransactionSyncHelper.syncRepayment(repaymentId, db, getApplication())
            }
        }
    }

    fun settleAllEntry(
        id: Long,
        paidAt: Long = timeProvider(),
        note: String? = null
    ) {
        viewModelScope.launch {
            val result = lendingDao.settleAllRecord(id, paidAt, note?.trim()?.ifBlank { null })
            result.onSuccess { repaymentId ->
                LendingTransactionSyncHelper.syncRepayment(repaymentId, db, getApplication())
            }
        }
    }

    fun undoLastRepayment(recordId: Long) {
        viewModelScope.launch {
            val repayments = lendingDao.getRepaymentsForLending(recordId)
            val latest = repayments.firstOrNull()
            val result = lendingDao.undoLastRepayment(recordId)
            if (result.isSuccess && latest != null) {
                LendingTransactionSyncHelper.deleteRepaymentTransaction(latest.id, db, getApplication())
            }
        }
    }

    fun forgiveEntry(recordId: Long) {
        viewModelScope.launch {
            LendingTransactionSyncHelper.forgiveLendingRecord(recordId, db, getApplication())
        }
    }

    fun reopenLegacyEntry(recordId: Long) {
        viewModelScope.launch {
            lendingDao.reopenLegacyRecord(recordId)
            LendingTransactionSyncHelper.syncLendingRecord(recordId, db, getApplication())
        }
    }

    fun deleteEntry(record: LendingWithRepayments) {
        viewModelScope.launch {
            undoJob?.cancel()
            _deletedRecordCache.value = record
            lendingDao.deleteRecordWithRepayments(record.lending.id)
            LendingTransactionSyncHelper.deleteLendingTransactions(record.lending.id, record.repayments, db, getApplication())

            // Auto-clear cache after 5 seconds
            undoJob = launch {
                delay(5000)
                _deletedRecordCache.value = null
            }
        }
    }

    fun undoDelete() {
        val cached = _deletedRecordCache.value ?: return
        viewModelScope.launch {
            undoJob?.cancel()
            lendingDao.restoreRecordWithRepayments(cached.lending, cached.repayments)
            LendingTransactionSyncHelper.syncLendingRecord(cached.lending.id, db, getApplication())
            for (repayment in cached.repayments) {
                LendingTransactionSyncHelper.syncRepayment(repayment.id, db, getApplication())
            }
            _deletedRecordCache.value = null
        }
    }

    fun repayPersonDues(
        personName: String,
        type: String,
        amount: Double,
        paidAt: Long = timeProvider(),
        note: String? = null
    ) {
        viewModelScope.launch {
            lendingDao.allocatePersonRepayment(personName, type, amount, paidAt, note?.trim()?.ifBlank { null })
            LendingTransactionSyncHelper.syncAllUnsynced(db, getApplication())
        }
    }

    fun settleAllForPerson(personKey: String) {
        viewModelScope.launch {
            val allRecords = lendingDao.getAllRecordsWithRepaymentsSync()
            val personRecords = allRecords.filter { normalizePersonKey(it.lending.personName) == personKey && !it.lending.isSettled }
            for (entry in personRecords) {
                val result = lendingDao.settleAllRecord(entry.lending.id, timeProvider())
                result.onSuccess { repaymentId ->
                    LendingTransactionSyncHelper.syncRepayment(repaymentId, db, getApplication())
                }
            }
        }
    }

    fun requestRenamePerson(oldName: String, newName: String) {
        val cleanedNew = cleanPersonName(newName)
        if (cleanedNew.isBlank()) return
        val oldKey = normalizePersonKey(oldName)
        val newKey = normalizePersonKey(cleanedNew)

        if (oldKey == newKey) {
            // Same key, just updating casing
            viewModelScope.launch {
                lendingDao.updatePersonName(oldName, cleanedNew)
                LendingTransactionSyncHelper.updatePersonNameInTransactions(oldName, cleanedNew, db, getApplication())
            }
            return
        }

        // Check if newKey belongs to an existing person
        val allPeople = uiState.value.allPeopleSummaries
        val existingTarget = allPeople.firstOrNull { it.key == newKey }
        if (existingTarget != null) {
            // Show merge prompt
            _pendingMergePrompt.value = MergeConfirmationPrompt(
                sourceName = oldName,
                targetName = existingTarget.displayName,
                targetPersonKey = newKey
            )
        } else {
            // No merge needed, direct rename
            viewModelScope.launch {
                lendingDao.updatePersonName(oldName, cleanedNew)
                LendingTransactionSyncHelper.updatePersonNameInTransactions(oldName, cleanedNew, db, getApplication())
                if (_selectedPersonKey.value == oldKey) {
                    selectPerson(newKey)
                }
            }
        }
    }

    fun confirmMergePerson() {
        val prompt = _pendingMergePrompt.value ?: return
        viewModelScope.launch {
            lendingDao.updatePersonName(prompt.sourceName, prompt.targetName)
            LendingTransactionSyncHelper.updatePersonNameInTransactions(prompt.sourceName, prompt.targetName, db, getApplication())
            if (_selectedPersonKey.value == normalizePersonKey(prompt.sourceName)) {
                selectPerson(prompt.targetPersonKey)
            }
            _pendingMergePrompt.value = null
        }
    }

    fun dismissMergePrompt() {
        _pendingMergePrompt.value = null
    }

    companion object {
        fun normalizePersonKey(name: String): String {
            return name.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)
        }

        fun cleanPersonName(name: String): String {
            return name.trim().replace(Regex("\\s+"), " ")
        }

        fun getStartOfTodayMillis(nowMillis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun aggregatePeople(
            records: List<LendingWithRepayments>,
            startOfTodayMillis: Long
        ): List<PersonSummary> {
            val grouped = records.groupBy { normalizePersonKey(it.lending.personName) }

            return grouped.map { (key, entries) ->
                // Display name is the most recently used spelling
                val displayName = entries.maxByOrNull { it.lending.date }?.lending?.personName?.let { cleanPersonName(it) }
                    ?: cleanPersonName(entries.first().lending.personName)

                var toReceive = 0.0
                var toPay = 0.0
                val openEntries = mutableListOf<LendingWithRepayments>()
                val settledEntries = mutableListOf<LendingWithRepayments>()

                var nextDue: Long? = null
                var isOverdue = false
                var earliestOverdueDate: Long? = null
                var hasDueSoon = false
                var maxActivity = 0L

                for (item in entries) {
                    val entry = item.lending
                    if (entry.date > maxActivity) maxActivity = entry.date
                    for (rep in item.repayments) {
                        if (rep.paidAt > maxActivity) maxActivity = rep.paidAt
                    }

                    if (entry.isSettled) {
                        settledEntries.add(item)
                    } else {
                        openEntries.add(item)
                        val out = item.outstanding
                        if (entry.type.equals("LENT", ignoreCase = true)) {
                            toReceive += out
                        } else if (entry.type.equals("BORROWED", ignoreCase = true)) {
                            toPay += out
                        }

                        // Check due date
                        val due = entry.dueDate
                        if (due != null) {
                            if (due < startOfTodayMillis) {
                                isOverdue = true
                                if (earliestOverdueDate == null || due < earliestOverdueDate) {
                                    earliestOverdueDate = due
                                }
                            } else {
                                if (due <= startOfTodayMillis + 7L * 86_400_000L) {
                                    hasDueSoon = true
                                }
                                if (nextDue == null || due < nextDue) {
                                    nextDue = due
                                }
                            }
                        }
                    }
                }

                val effectiveNextDueDate = if (isOverdue) earliestOverdueDate else nextDue
                val overdueDays = if (isOverdue && earliestOverdueDate != null) {
                    ((startOfTodayMillis - earliestOverdueDate) / 86_400_000L).toInt().coerceAtLeast(1)
                } else {
                    0
                }

                PersonSummary(
                    key = key,
                    displayName = displayName,
                    toReceive = toReceive,
                    toPay = toPay,
                    net = toReceive - toPay,
                    openCount = openEntries.size,
                    nextDueDate = effectiveNextDueDate,
                    isOverdue = isOverdue,
                    overdueDays = overdueDays,
                    isDueSoon = hasDueSoon,
                    needsAttention = isOverdue || hasDueSoon,
                    lastActivity = maxActivity,
                    hasHistory = entries.isNotEmpty(),
                    openEntries = openEntries.sortedByDescending { it.lending.date },
                    settledEntries = settledEntries.sortedByDescending { it.lending.date }
                )
            }
        }

        fun filterTimelineRecords(
            records: List<LendingWithRepayments>,
            filter: TimelineFilter,
            startOfTodayMillis: Long
        ): List<LendingWithRepayments> {
            return when (filter) {
                TimelineFilter.ALL -> records
                TimelineFilter.YOU_LL_GET -> records.filter {
                    !it.lending.isSettled && it.lending.type.equals("LENT", ignoreCase = true)
                }
                TimelineFilter.YOU_OWE -> records.filter {
                    !it.lending.isSettled && it.lending.type.equals("BORROWED", ignoreCase = true)
                }
                TimelineFilter.SETTLED -> records.filter { it.lending.isSettled }
            }
        }

        fun groupRecordsByMonth(records: List<LendingWithRepayments>): List<MonthTimelineGroup> {
            val sorted = records.sortedByDescending { it.lending.date }

            val groups = mutableMapOf<String, MutableList<LendingWithRepayments>>()
            for (record in sorted) {
                val key = LendingDateFormatters.formatMonthHeader(record.lending.date)
                groups.getOrPut(key) { mutableListOf() }.add(record)
            }

            return groups.map { (monthYearKey, items) ->
                MonthTimelineGroup(monthYearKey, items)
            }
        }
    }
}
