package com.omkarnub.kanri.ui.lending

import com.omkarnub.kanri.data.db.LendingWithRepayments

enum class LendingTabOption {
    PEOPLE,
    TIMELINE
}

enum class TimelineFilter {
    ALL,
    YOU_LL_GET,
    YOU_OWE,
    SETTLED
}

data class PersonSummary(
    val key: String,
    val displayName: String,
    val toReceive: Double,
    val toPay: Double,
    val net: Double, // toReceive - toPay
    val openCount: Int,
    val nextDueDate: Long?,
    val isOverdue: Boolean,
    val overdueDays: Int,
    val isDueSoon: Boolean,
    val needsAttention: Boolean,
    val lastActivity: Long,
    val hasHistory: Boolean,
    val openEntries: List<LendingWithRepayments>,
    val settledEntries: List<LendingWithRepayments>
)

data class MonthTimelineGroup(
    val monthYearKey: String,
    val entries: List<LendingWithRepayments>
)

data class HeroTotals(
    val toReceive: Double = 0.0,
    val toPay: Double = 0.0,
    val netPosition: Double = 0.0,
    val isAllSettled: Boolean = true
)

data class MergeConfirmationPrompt(
    val sourceName: String,
    val targetName: String,
    val targetPersonKey: String
)

data class LendingHubUiState(
    val isLoading: Boolean = false,
    val selectedTab: LendingTabOption = LendingTabOption.PEOPLE,
    val selectedPersonKey: String? = null,
    val timelineFilter: TimelineFilter = TimelineFilter.ALL,
    val isSettledSectionExpanded: Boolean = false,
    val heroTotals: HeroTotals = HeroTotals(),
    val needsAttentionPeople: List<PersonSummary> = emptyList(),
    val activePeople: List<PersonSummary> = emptyList(),
    val settledPeople: List<PersonSummary> = emptyList(),
    val allPeopleSummaries: List<PersonSummary> = emptyList(),
    val timelineGroups: List<MonthTimelineGroup> = emptyList(),
    val selectedPersonSummary: PersonSummary? = null,
    val pendingMergePrompt: MergeConfirmationPrompt? = null,
    val canUndoDelete: Boolean = false
)
