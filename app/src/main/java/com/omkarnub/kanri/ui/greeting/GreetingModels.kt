package com.omkarnub.kanri.ui.greeting

import java.time.DayOfWeek
import java.time.LocalDateTime

enum class Tone {
    Neutral,
    Positive,
    Warning
}

enum class TimeBucket {
    Morning,
    Afternoon,
    Evening,
    Night,
    LateNight
}

enum class GreetingAction {
    None,
    OpenInsights,
    OpenReviewSheet
}

data class GreetingContext(
    val userName: String,
    val now: LocalDateTime,
    val dayOfWeek: DayOfWeek,
    val dayOfMonth: Int,
    val daysLeftInMonth: Int,
    val monthlyBudget: Double?,
    val spentThisMonth: Double,
    val spentToday: Double,
    val safeToSpendPerDay: Double?,
    val lastMonthSpentSameDay: Double?,
    val streakDays: Int,
    val needsReviewCount: Int,
    val isFirstLaunchToday: Boolean,
    val seed: Long,
    val lastSubtitleRuleId: Int? = null,
    val lastShownEpochDay: Long? = null,
    val lastWarningBucket: String? = null,
    val lastWarningEpochDay: Long? = null
)

data class GreetingResult(
    val greeting: String,
    val subtitle: String,
    val tone: Tone,
    val ruleId: Int = 11,
    val action: GreetingAction = GreetingAction.None
)
