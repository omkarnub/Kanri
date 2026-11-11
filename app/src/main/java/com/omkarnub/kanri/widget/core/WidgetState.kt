package com.omkarnub.kanri.widget.core

import androidx.compose.runtime.Immutable

/**
 * Immutable state models representing data for all 12 Kanri Pixel DotTiles widgets.
 */
sealed interface WidgetState {
    val accessibleDescription: String
}

enum class PacingStatus {
    HEALTHY, WARNING, OVER
}

enum class ClimateStatus {
    CLEAR, SUNRISE, CLOUDY, STORM, STARRY
}

enum class QuickActionType {
    ADD_EXPENSE, ADD_INCOME, SPLIT_BILL
}

@Immutable
data class TodayWidgetState(
    val spentToday: Double = 0.0,
    val expenseCount: Int = 0,
    val dailyLimit: Double = 0.0,
    val spentPercent: Float = 0f,
    val morningSpend: Double = 0.0,
    val afternoonSpend: Double = 0.0,
    val eveningSpend: Double = 0.0,
    val nightSpend: Double = 0.0,
    val hourlySpend: List<Float?> = List(24) { null },
    val currentHour: Int = 12,
    val pacingStatus: PacingStatus = PacingStatus.HEALTHY,
    override val accessibleDescription: String = "Today's expenses"
) : WidgetState

@Immutable
data class BudgetWidgetState(
    val hasBudget: Boolean = true,
    val remainingAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val totalBudget: Double = 0.0,
    val percentUsed: Float = 0f,
    val daysLeft: Int = 0,
    val pacingExpectedRatio: Float = 0.5f,
    override val accessibleDescription: String = "Monthly budget summary"
) : WidgetState

@Immutable
data class WalletsWidgetState(
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val netWorth: Double = 0.0,
    val cashProportion: Float = 0.5f,
    override val accessibleDescription: String = "Cash, Bank, and Net Worth balances"
) : WidgetState

@Immutable
data class GoalsWidgetState(
    val hasGoal: Boolean = true,
    val goalName: String = "",
    val savedAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val percentSaved: Float = 0f,
    val isCompleted: Boolean = false,
    override val accessibleDescription: String = "Primary savings goal progress"
) : WidgetState

@Immutable
data class LendingPersonSummary(
    val name: String,
    val netAmount: Double,
    val isReceivable: Boolean
)

@Immutable
data class DuesWidgetState(
    val receivableAmount: Double = 0.0,
    val payableAmount: Double = 0.0,
    val netAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val receivableRatio: Float = 0.5f,
    val payableRatio: Float = 0.5f,
    val topPersons: List<LendingPersonSummary> = emptyList(),
    override val accessibleDescription: String = "Lend and borrow dues summary"
) : WidgetState

@Immutable
data class SkyWidgetState(
    val sunProgress: Float = 0.5f,
    val isDay: Boolean = true,
    val moonPhaseFrac: Float = 0.5f,
    val climateStatus: ClimateStatus = ClimateStatus.CLEAR,
    val conditionText: String = "CLEAR",
    val weatherKind: String = "clear",
    val cityName: String = "LOCAL",
    val todaySpent: Double = 0.0,
    val safeToSpend: Double = 0.0,
    val sevenDayPulse: FloatArray = FloatArray(7) { 0.2f },
    val weekSpends: List<Pair<String, Double>> = emptyList(),
    override val accessibleDescription: String = "Financial weather and climate"
) : WidgetState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SkyWidgetState) return false
        return sunProgress == other.sunProgress &&
                isDay == other.isDay &&
                moonPhaseFrac == other.moonPhaseFrac &&
                climateStatus == other.climateStatus &&
                conditionText == other.conditionText &&
                weatherKind == other.weatherKind &&
                cityName == other.cityName &&
                todaySpent == other.todaySpent &&
                safeToSpend == other.safeToSpend &&
                sevenDayPulse.contentEquals(other.sevenDayPulse) &&
                weekSpends == other.weekSpends
    }

    override fun hashCode(): Int {
        var result = sunProgress.hashCode()
        result = 31 * result + isDay.hashCode()
        result = 31 * result + moonPhaseFrac.hashCode()
        result = 31 * result + climateStatus.hashCode()
        result = 31 * result + conditionText.hashCode()
        result = 31 * result + weatherKind.hashCode()
        result = 31 * result + cityName.hashCode()
        result = 31 * result + todaySpent.hashCode()
        result = 31 * result + safeToSpend.hashCode()
        result = 31 * result + sevenDayPulse.contentHashCode()
        result = 31 * result + weekSpends.hashCode()
        return result
    }
}

@Immutable
data class HeatmapWidgetState(
    val monthName: String = "",
    val monthTotal: Double = 0.0,
    val dailyIntensities: FloatArray = FloatArray(35) { 0f },
    val todayCellIndex: Int = -1,
    val maxActiveCellIndex: Int = -1,
    override val accessibleDescription: String = "Calendar spend heatmap"
) : WidgetState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HeatmapWidgetState) return false
        return monthName == other.monthName &&
                monthTotal == other.monthTotal &&
                dailyIntensities.contentEquals(other.dailyIntensities) &&
                todayCellIndex == other.todayCellIndex &&
                maxActiveCellIndex == other.maxActiveCellIndex
    }

    override fun hashCode(): Int {
        var result = monthName.hashCode()
        result = 31 * result + monthTotal.hashCode()
        result = 31 * result + dailyIntensities.contentHashCode()
        result = 31 * result + todayCellIndex
        result = 31 * result + maxActiveCellIndex
        return result
    }
}

@Immutable
data class StreakWidgetState(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val last30Days: BooleanArray = BooleanArray(30) { false },
    val todayIndex: Int = 29,
    override val accessibleDescription: String = "No-spend day streak"
) : WidgetState {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StreakWidgetState) return false
        return currentStreak == other.currentStreak &&
                bestStreak == other.bestStreak &&
                last30Days.contentEquals(other.last30Days) &&
                todayIndex == other.todayIndex
    }

    override fun hashCode(): Int {
        var result = currentStreak
        result = 31 * result + bestStreak
        result = 31 * result + last30Days.contentHashCode()
        result = 31 * result + todayIndex
        return result
    }
}

@Immutable
data class SafeToSpendWidgetState(
    val dailyAllowance: Double = 0.0,
    val todaySpent: Double = 0.0,
    val spentRatio: Float = 0f,
    val isOverAllowance: Boolean = false,
    val hasBudget: Boolean = true,
    override val accessibleDescription: String = "Safe to spend daily allowance"
) : WidgetState

@Immutable
data class ReviewInboxWidgetState(
    val pendingCount: Int = 0,
    val pendingTotalAmount: Double = 0.0,
    override val accessibleDescription: String = "Pending SMS transactions to review"
) : WidgetState

@Immutable
data class MonthDeltaWidgetState(
    val deltaPercent: Float = 0f,
    val isSpendingMore: Boolean = false,
    val currentMonthSpendToDate: Double = 0.0,
    val previousMonthSpendToDate: Double = 0.0,
    val dayOfMonth: Int = 1,
    override val accessibleDescription: String = "Spending comparison vs same day last month"
) : WidgetState

@Immutable
data class QuickActionsWidgetState(
    val actions: List<QuickActionType> = listOf(
        QuickActionType.ADD_EXPENSE,
        QuickActionType.ADD_INCOME,
        QuickActionType.SPLIT_BILL
    ),
    override val accessibleDescription: String = "Quick financial actions"
) : WidgetState

