package com.omkarnub.kanri.ui.insights

import androidx.compose.runtime.Immutable
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import java.time.LocalDate
import java.time.YearMonth

// -----------------------------------------------------------------------------
// Core Range & Mode Enums/Sealed Types
// -----------------------------------------------------------------------------

sealed class InsightsRange {
    data class Month(val yearMonth: YearMonth = YearMonth.now()) : InsightsRange()
    data object Days30 : InsightsRange()
    data object Months3 : InsightsRange()
    data object Year : InsightsRange()
    data class Custom(val from: LocalDate, val to: LocalDate) : InsightsRange()

    val label: String
        get() = when (this) {
            is Month -> yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + yearMonth.year
            is Days30 -> "Last 30 Days"
            is Months3 -> "Last 3 Months"
            is Year -> "Last 12 Months"
            is Custom -> "${from.dayOfMonth} ${from.month.name.take(3)} – ${to.dayOfMonth} ${to.month.name.take(3)}"
        }
}

enum class InsightsMode {
    EXPENSE,
    INCOME
}

// -----------------------------------------------------------------------------
// Generic Async Section State
// -----------------------------------------------------------------------------

sealed interface SectionState<out T> {
    data object Loading : SectionState<Nothing>
    data class Empty(val message: String = "No data available") : SectionState<Nothing> {
        val reason: String get() = message
    }
    data class Data<out T>(val data: T) : SectionState<T>
}

// -----------------------------------------------------------------------------
// 5.2 Month Summary Strip Models
// -----------------------------------------------------------------------------

@Immutable
data class SummaryStripData(
    val spent: Double,
    val income: Double,
    val net: Double,
    val transactionCount: Int,
    val averagePerDay: Double,
    val deltaSpentPercent: Double?,
    val deltaSpentAmount: Double?,
    val deltaIncomePercent: Double?,
    val deltaIncomeAmount: Double?,
    val deltaNetPercent: Double?,
    val deltaNetAmount: Double?,
    val previousPeriodLabel: String
)

// -----------------------------------------------------------------------------
// 5.3 Month-End Projection Models
// -----------------------------------------------------------------------------

@Immutable
data class MonthEndProjectionData(
    val projectedTotal: Double,
    val budget: Double?,
    val gap: Double?,
    val isOverBudget: Boolean,
    val isNearLimit: Boolean,
    val budgetExhaustionDate: LocalDate?,
    val lastMonthTotal: Double?,
    val spentSoFar: Double,
    val variableRunRate: Double,
    val unpaidRecurringTotal: Double,
    val daysElapsed: Int,
    val daysRemaining: Int
)

// -----------------------------------------------------------------------------
// 5.4 Cumulative Spend Line Models
// -----------------------------------------------------------------------------

@Immutable
data class CumulativePoint(
    val dayIndex: Int, // 1..N
    val dateLabel: String,
    val cumulativeAmount: Double,
    val dailyAmount: Double
) {
    val day: Int get() = dayIndex
    val amount: Double get() = cumulativeAmount
}

@Immutable
data class CumulativeLineData(
    val currentPoints: List<CumulativePoint>,
    val ghostPoints: List<CumulativePoint>,
    val projectedPoints: List<CumulativePoint>,
    val budgetLimit: Double?,
    val maxAmount: Double,
    val previousPeriodLabel: String
)

typealias CumulativeSpendLineData = CumulativeLineData
typealias CumulativeSpendData = CumulativeLineData

// -----------------------------------------------------------------------------
// 5.5 Budget vs Actual per Category Models
// -----------------------------------------------------------------------------

@Immutable
data class CategoryBudgetRow(
    val categoryId: Long,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorHex: String,
    val budgetedAmount: Double,
    val spentAmount: Double,
    val percentUsed: Float,
    val remainingAmount: Double,
    val isOverBudget: Boolean
)

@Immutable
data class UnbudgetedCategoryRow(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIconName: String,
    val spentAmount: Double
)

@Immutable
data class BudgetVsActualData(
    val overallBudget: Double,
    val overallSpent: Double,
    val overallPercentUsed: Float,
    val overallRemaining: Double,
    val isOverallOverBudget: Boolean,
    val categoryRows: List<CategoryBudgetRow>,
    val unbudgetedSpendTotal: Double,
    val unbudgetedCategories: List<UnbudgetedCategoryRow>,
    val sumOfCategoriesExceedsOverall: Boolean
)

// -----------------------------------------------------------------------------
// 5.6 Calendar Heatmap Models
// -----------------------------------------------------------------------------

@Immutable
data class CalendarDayData(
    val date: LocalDate,
    val dayOfMonth: Int,
    val dayOfWeek: Int, // 1 = Monday .. 7 = Sunday
    val amount: Double,
    val quantileLevel: Int, // 0..4 (0=none/empty, 1..4=quantiles)
    val isFuture: Boolean,
    val isToday: Boolean,
    val hasSpend: Boolean
)

@Immutable
data class CalendarHeatmapData(
    val yearMonth: YearMonth,
    val days: List<CalendarDayData>,
    val emptyLeadingDays: Int, // Monday offset
    val noSpendDaysCount: Int,
    val highestDayAmount: Double,
    val highestDayDate: LocalDate?,
    val quantileThresholds: List<Double>
)

// -----------------------------------------------------------------------------
// 5.7 Category Movers Models
// -----------------------------------------------------------------------------

@Immutable
data class CategoryMoverItem(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIconName: String,
    val categoryColorHex: String,
    val currentSpend: Double,
    val previousSpend: Double,
    val deltaAmount: Double,
    val deltaPercent: Double?,
    val isNew: Boolean
)

@Immutable
data class CategoryMoversData(
    val upMovers: List<CategoryMoverItem>,
    val downMovers: List<CategoryMoverItem>,
    val periodComparisonLabel: String
) {
    val periodLabel: String get() = periodComparisonLabel
}

// -----------------------------------------------------------------------------
// 5.8 Top Payees Models
// -----------------------------------------------------------------------------

@Immutable
data class TopPayeeItem(
    val rawCounterparty: String,
    val displayName: String,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentOfTotal: Double,
    val relativeShare: Float // relative to top payee (1.0f)
)

@Immutable
data class TopPayeesData(
    val payees: List<TopPayeeItem>,
    val totalExpenses: Double
)

// -----------------------------------------------------------------------------
// 5.9 Payee Detail Models
// -----------------------------------------------------------------------------

@Immutable
data class PayeeMonthlyBar(
    val yearMonth: YearMonth,
    val label: String,
    val amount: Double
)

@Immutable
data class PayeeDetailData(
    val displayName: String,
    val rawCounterparty: String,
    val category: CategoryEntity?,
    val totalPaid: Double,
    val paymentCount: Int,
    val averagePerPayment: Double,
    val largestPayment: Double,
    val firstPaidDate: LocalDate?,
    val lastPaidDate: LocalDate?,
    val frequencyLabel: String,
    val receivedFromThem: Double,
    val netTotal: Double,
    val last12MonthsBars: List<PayeeMonthlyBar>,
    val transactions: List<TransactionWithCategory>
)

// -----------------------------------------------------------------------------
// 5.10 Biggest Transactions Models
// -----------------------------------------------------------------------------

@Immutable
data class BiggestTransactionItem(
    val id: Long,
    val rank: Int,
    val amount: Double,
    val payeeName: String,
    val categoryName: String,
    val categoryIconName: String,
    val sourceType: String,
    val timestamp: Long,
    val percentOfTotal: Double,
    val rawTransaction: TransactionEntity
)

@Immutable
data class BiggestTransactionsData(
    val items: List<BiggestTransactionItem>,
    val topItemShare: Double,
    val hasDominantItem: Boolean
)

// -----------------------------------------------------------------------------
// 5.11 Payment Method Split Models
// -----------------------------------------------------------------------------

@Immutable
data class PaymentMethodItem(
    val sourceType: String,
    val displayName: String,
    val totalAmount: Double,
    val count: Int,
    val percentage: Double,
    val alphaTone: Float
)

@Immutable
data class PaymentMethodSplitData(
    val methods: List<PaymentMethodItem>,
    val totalAmount: Double
)

// -----------------------------------------------------------------------------
// 5.12 Weekend vs Weekday Split Models
// -----------------------------------------------------------------------------

@Immutable
data class WeekendVsWeekdayData(
    val weekdayTotal: Double,
    val weekdayDaysCount: Int,
    val weekdayAvgPerDay: Double,
    val weekdayTopCategory: String?,
    val weekendTotal: Double,
    val weekendDaysCount: Int,
    val weekendAvgPerDay: Double,
    val weekendTopCategory: String?,
    val ratio: Double,
    val insightSentence: String
)

// -----------------------------------------------------------------------------
// 5.13 Small Spends Total Models
// -----------------------------------------------------------------------------

@Immutable
data class SmallSpendPayee(
    val name: String,
    val count: Int,
    val total: Double
)

@Immutable
data class SmallSpendsData(
    val threshold: Double,
    val totalAmount: Double,
    val count: Int,
    val averageAmount: Double,
    val percentOfTotalSpend: Double,
    val topPayees: List<SmallSpendPayee>,
    val yearlyPaceAmount: Double
)

// -----------------------------------------------------------------------------
// 5.14 Income Sources Models
// -----------------------------------------------------------------------------

enum class IncomeRegularityTag {
    REGULAR_STEADY,
    REGULAR,
    IRREGULAR
}

@Immutable
data class IncomeSourceItem(
    val name: String,
    val rawCounterparty: String,
    val totalReceived: Double,
    val transactionCount: Int,
    val tag: IncomeRegularityTag,
    val lastReceivedDate: LocalDate?,
    val typicalArrivalDayOfMonth: Int?
)

@Immutable
data class IncomeSourcesData(
    val regularTotal: Double,
    val irregularTotal: Double,
    val regularPercent: Double,
    val irregularPercent: Double,
    val sources: List<IncomeSourceItem>
)

// -----------------------------------------------------------------------------
// 5.15 Suggested Budget Models
// -----------------------------------------------------------------------------

@Immutable
data class SuggestedCategoryRow(
    val categoryId: Long,
    val categoryName: String,
    val categoryIconName: String,
    val threeMonthAverage: Double,
    val currentBudget: Double?,
    val suggestedBudget: Double
)

@Immutable
data class SuggestedBudgetData(
    val categories: List<SuggestedCategoryRow>,
    val overallCurrentBudget: Double?,
    val overallSuggestedBudget: Double,
    val completedMonthsAvailable: Int
)

// -----------------------------------------------------------------------------
// 5.16 Personal Records Models
// -----------------------------------------------------------------------------

@Immutable
data class PersonalRecordsData(
    val highestSpendMonth: Pair<YearMonth, Double>?,
    val lowestSpendMonth: Pair<YearMonth, Double>?,
    val highestIncomeMonth: Pair<YearMonth, Double>?,
    val bestSavingsMonth: Pair<YearMonth, Double>?,
    val longestNoSpendStreakDays: Int,
    val longestStreakDateRange: String,
    val currentNoSpendStreakDays: Int,
    val biggestSingleExpense: Triple<Double, String, LocalDate>?,
    val highestSpendDay: Pair<LocalDate, Double>?,
    val mostTransactionsInOneDay: Pair<LocalDate, Int>?,
    val newRecordsBroken: Set<String>
)

// -----------------------------------------------------------------------------
// 5.17 Year Heatmap Models
// -----------------------------------------------------------------------------

@Immutable
data class YearHeatmapCell(
    val date: LocalDate,
    val amount: Double,
    val quantileLevel: Int,
    val isSpendDay: Boolean,
    val isBeforeFirstTx: Boolean,
    val txCount: Int,
    val topCategory: String?
)

@Immutable
data class YearHeatmapData(
    val year: Int,
    val availableYears: List<Int>,
    val cells: List<YearHeatmapCell>,
    val spendingDaysCount: Int,
    val noSpendDaysCount: Int,
    val quantileThresholds: List<Double>
)

// -----------------------------------------------------------------------------
// 5.18 Compare Months Models
// -----------------------------------------------------------------------------

@Immutable
data class CompareMonthMetrics(
    val spent: Double,
    val income: Double,
    val net: Double,
    val transactionCount: Int,
    val averagePerDay: Double,
    val largestExpense: Double
)

@Immutable
data class CompareCategoryItem(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIconName: String,
    val amountA: Double,
    val amountB: Double,
    val deltaAmount: Double,
    val deltaPercent: Double?
)

@Immutable
data class CompareMonthsData(
    val monthA: YearMonth,
    val monthB: YearMonth,
    val availableMonths: List<YearMonth>,
    val isSameDaysOnly: Boolean,
    val clampedDayCount: Int,
    val metricsA: CompareMonthMetrics,
    val metricsB: CompareMonthMetrics,
    val dailyCumulativeA: List<DaySpendPoint>,
    val dailyCumulativeB: List<DaySpendPoint>,
    val categoryItems: List<CompareCategoryItem>,
    val summarySentence: String
)

@Immutable
data class DaySpendPoint(
    val dayOfMonth: Int,
    val cumulative: Double,
    val daily: Double
)

// -----------------------------------------------------------------------------
// 5.19 Split Expenses Summary Models
// -----------------------------------------------------------------------------

@Immutable
data class SplitPendingPerson(
    val name: String,
    val pendingAmount: Double,
    val splitCount: Int
)

@Immutable
data class SplitExpensesSummaryData(
    val billsPaidByMeTotal: Double,
    val myOwnShareTotal: Double,
    val othersOweMeTotal: Double,
    val recoveredTotal: Double,
    val pendingTotal: Double,
    val splitEventsCount: Int,
    val averageParticipants: Double,
    val billsPaidByOthersTotal: Double,
    val myBorrowedSharePending: Double,
    val topPendingPeople: List<SplitPendingPerson>
)

typealias SplitExpensesData = SplitExpensesSummaryData

// -----------------------------------------------------------------------------
// 5.20 Monthly Recap Models
// -----------------------------------------------------------------------------

@Immutable
data class RecapTopCategory(
    val name: String,
    val iconName: String,
    val percent: Double,
    val amount: Double
)

@Immutable
data class MonthlyRecapData(
    val yearMonth: YearMonth,
    val isSoFar: Boolean,
    val totalSpent: Double,
    val deltaVsPreviousAmount: Double?,
    val deltaVsPreviousPercent: Double?,
    val topCategories: List<RecapTopCategory>,
    val topPayee: String?,
    val biggestExpenseAmount: Double?,
    val biggestExpensePayee: String?,
    val noSpendDaysCount: Int,
    val totalIncome: Double,
    val netAmount: Double,
    val savingsRatePercent: Double?,
    val budgetAmount: Double?,
    val budgetResultLabel: String?,
    val isUnderBudget: Boolean?,
    val dailySpends: List<Double>
)
