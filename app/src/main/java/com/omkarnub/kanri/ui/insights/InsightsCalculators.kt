package com.omkarnub.kanri.ui.insights

import androidx.compose.ui.graphics.Color
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToLong
data class IncomeHistoryRecord(
    val month: YearMonth,
    val amount: Double,
    val count: Int
)

enum class DeltaColorSemantic {
    EXPENSE_ACCENT,
    INCOME_ACCENT,
    MUTED
}

data class DeltaResult(
    val percentage: Double?,
    val deltaAmount: Double,
    val colorSemantic: DeltaColorSemantic
)

object InsightsCalculators {

    // -------------------------------------------------------------------------
    // 5.3 Month-End Projection Logic (Pure Function)
    // -------------------------------------------------------------------------
    data class ProjectionInput(
        val spentSoFar: Double,
        val daysElapsed: Int,
        val daysRemaining: Int,
        val monthlyBudget: Double?,
        val transactionAmounts: List<Double>,
        val recurringAlreadyPaid: Double,
        val unpaidRecurringTotal: Double,
        val lastMonthTotal: Double?
    )

    fun calculateProjection(input: ProjectionInput): MonthEndProjectionData? {
        // Guard: only from day 4 of the month
        if (input.daysElapsed < 4) {
            return null
        }

        val budget = input.monthlyBudget?.takeIf { it > 0.0 }

        // 2. Identify one-off large spends: >= 25% of budget, or >= 5x median transaction
        val medianTx = calculateMedian(input.transactionAmounts)
        val oneOffThreshold = if (budget != null) {
            0.25 * budget
        } else {
            5.0 * medianTx
        }

        val oneOffsSum = input.transactionAmounts
            .filter { it >= oneOffThreshold && it > 0.0 }
            .sum()

        // 3. Variable run rate = (spentSoFar - recurringAlreadyPaid - oneOffs) / daysElapsed
        val baseForRunRate = (input.spentSoFar - input.recurringAlreadyPaid - oneOffsSum).coerceAtLeast(0.0)
        val variableRunRate = if (input.daysElapsed > 0) baseForRunRate / input.daysElapsed else 0.0

        // 4. Projected = spentSoFar + variableRunRate * daysRemaining + unpaidRecurring
        val rawProjected = input.spentSoFar + (variableRunRate * input.daysRemaining) + input.unpaidRecurringTotal
        val projectedTotal = rawProjected.coerceAtLeast(input.spentSoFar)

        // 5. Budget gap & exhaustion date
        val gap = budget?.let { projectedTotal - it }
        val isOverBudget = gap != null && gap > 0.0
        val isNearLimit = budget != null && !isOverBudget && projectedTotal >= (0.95 * budget)

        var budgetExhaustionDate: LocalDate? = null
        if (budget != null && isOverBudget) {
            val remainingBudget = budget - input.spentSoFar
            if (remainingBudget <= 0.0) {
                budgetExhaustionDate = LocalDate.now()
            } else if (variableRunRate > 0.0) {
                val daysUntilExhaustion = ceil(remainingBudget / variableRunRate).toLong()
                budgetExhaustionDate = LocalDate.now().plusDays(daysUntilExhaustion)
            }
        }

        return MonthEndProjectionData(
            projectedTotal = projectedTotal,
            budget = budget,
            gap = gap,
            isOverBudget = isOverBudget,
            isNearLimit = isNearLimit,
            budgetExhaustionDate = budgetExhaustionDate,
            lastMonthTotal = input.lastMonthTotal,
            spentSoFar = input.spentSoFar,
            variableRunRate = variableRunRate,
            unpaidRecurringTotal = input.unpaidRecurringTotal,
            daysElapsed = input.daysElapsed,
            daysRemaining = input.daysRemaining
        )
    }

    fun calculateProjection(
        spentSoFar: Double,
        daysElapsed: Int,
        daysRemaining: Int,
        activeRecurringUnpaid: Double,
        activeRecurringPaidSoFar: Double,
        oneOffSpendsSoFar: Double,
        monthlyBudget: Double?,
        lastMonthTotal: Double?,
        referenceDate: LocalDate = LocalDate.now()
    ): MonthEndProjectionData? {
        if (daysElapsed < 4) return null
        val budget = monthlyBudget?.takeIf { it > 0.0 }
        val baseForRunRate = (spentSoFar - activeRecurringPaidSoFar - oneOffSpendsSoFar).coerceAtLeast(0.0)
        val variableRunRate = if (daysElapsed > 0) baseForRunRate / daysElapsed else 0.0
        val rawProjected = spentSoFar + (variableRunRate * daysRemaining) + activeRecurringUnpaid
        val projectedTotal = rawProjected.coerceAtLeast(spentSoFar)

        val gap = budget?.let { projectedTotal - it }
        val isOverBudget = gap != null && gap > 0.0
        val isNearLimit = budget != null && !isOverBudget && projectedTotal >= (0.95 * budget)

        var budgetExhaustionDate: LocalDate? = null
        if (budget != null && isOverBudget) {
            val remainingBudget = budget - spentSoFar
            if (remainingBudget <= 0.0) {
                budgetExhaustionDate = referenceDate
            } else if (variableRunRate > 0.0) {
                val daysUntilExhaustion = ceil(remainingBudget / variableRunRate).toLong()
                budgetExhaustionDate = referenceDate.plusDays(daysUntilExhaustion)
            }
        }

        return MonthEndProjectionData(
            projectedTotal = projectedTotal,
            budget = budget,
            gap = gap,
            isOverBudget = isOverBudget,
            isNearLimit = isNearLimit,
            budgetExhaustionDate = budgetExhaustionDate,
            lastMonthTotal = lastMonthTotal,
            spentSoFar = spentSoFar,
            variableRunRate = variableRunRate,
            unpaidRecurringTotal = activeRecurringUnpaid,
            daysElapsed = daysElapsed,
            daysRemaining = daysRemaining
        )
    }

    fun identifyOneOffSpend(
        amount: Double,
        monthlyBudget: Double?,
        medianTransaction: Double
    ): Pair<Boolean, Double> {
        val threshold = if (monthlyBudget != null && monthlyBudget > 0.0) {
            0.25 * monthlyBudget
        } else {
            5.0 * medianTransaction
        }
        return Pair(amount >= threshold, threshold)
    }

    // -------------------------------------------------------------------------
    // 5.6 & 5.17 Quantile Bucketing (5 Levels: 0=none, 1..4=quantiles)
    // -------------------------------------------------------------------------
    fun calculateQuantileThresholds(values: List<Double>): List<Double> {
        val nonZero = values.filter { it > 0.0 }.sorted()
        if (nonZero.isEmpty()) return listOf(0.0, 0.0, 0.0, 0.0)
        if (nonZero.size < 4) {
            val max = nonZero.last()
            return listOf(max * 0.25, max * 0.50, max * 0.75, max)
        }

        val q1 = nonZero[(nonZero.size * 0.25).toInt().coerceIn(0, nonZero.size - 1)]
        val q2 = nonZero[(nonZero.size * 0.50).toInt().coerceIn(0, nonZero.size - 1)]
        val q3 = nonZero[(nonZero.size * 0.75).toInt().coerceIn(0, nonZero.size - 1)]
        val q4 = nonZero.last()

        return listOf(q1, q2, q3, q4)
    }

    fun getQuantileLevel(amount: Double, thresholds: List<Double>): Int {
        if (amount <= 0.0) return 0
        if (thresholds.size < 4) return 1
        return when {
            amount <= thresholds[0] -> 1
            amount <= thresholds[1] -> 2
            amount <= thresholds[2] -> 3
            amount <= thresholds[3] -> 4
            else -> 5
        }
    }

    // -------------------------------------------------------------------------
    // 5.7 Category Movers Thresholds
    // -------------------------------------------------------------------------
    fun calculateMovers(
        currentSpends: Map<Long?, Double>,
        previousSpends: Map<Long?, Double>,
        categoryMeta: Map<Long?, Triple<String, String, String>> // id -> (name, icon, colorHex)
    ): Pair<List<CategoryMoverItem>, List<CategoryMoverItem>> {
        val allCategoryIds = currentSpends.keys + previousSpends.keys
        val items = mutableListOf<CategoryMoverItem>()

        for (catId in allCategoryIds) {
            val current = currentSpends[catId] ?: 0.0
            val previous = previousSpends[catId] ?: 0.0
            val delta = current - previous

            // Rule: Ignore categories where both periods under ₹100 or absolute change under ₹100
            if (current < 100.0 && previous < 100.0) continue
            if (abs(delta) < 100.0) continue

            val isNew = (previous == 0.0 && current >= 100.0)
            val deltaPercent = if (previous > 0.0) {
                (delta / previous) * 100.0
            } else null

            val meta = categoryMeta[catId] ?: Triple("Uncategorized", "category", "#8D6E63")

            items.add(
                CategoryMoverItem(
                    categoryId = catId,
                    categoryName = meta.first,
                    categoryIconName = meta.second,
                    categoryColorHex = meta.third,
                    currentSpend = current,
                    previousSpend = previous,
                    deltaAmount = delta,
                    deltaPercent = deltaPercent,
                    isNew = isNew
                )
            )
        }

        val upMovers = items.filter { it.deltaAmount > 0 }
            .sortedByDescending { it.deltaAmount }
            .take(3)

        val downMovers = items.filter { it.deltaAmount < 0 }
            .sortedBy { it.deltaAmount } // largest drop first (most negative)
            .take(3)

        return Pair(upMovers, downMovers)
    }

    // -------------------------------------------------------------------------
    // 5.12 Weekend vs Weekday Normalized Averages & Ratio
    // -------------------------------------------------------------------------
    fun calculateWeekendWeekday(
        weekdayTotal: Double,
        weekdayDays: Int,
        weekendTotal: Double,
        weekendDays: Int,
        weekdayTopCategory: String?,
        weekendTopCategory: String?
    ): WeekendVsWeekdayData? {
        if (weekdayDays == 0 || weekendDays == 0) return null

        val weekdayAvg = weekdayTotal / weekdayDays
        val weekendAvg = weekendTotal / weekendDays

        val ratio = if (weekdayAvg > 0.0) weekendAvg / weekdayAvg else 1.0

        val wording = when {
            ratio in 0.9..1.1 -> "You spend about the same per day on weekends as on weekdays."
            ratio > 1.1 -> {
                val formattedRatio = String.format(java.util.Locale.US, "%.1f", ratio).removeSuffix(".0")
                "You spend ${formattedRatio}× more per day on weekends."
            }
            else -> {
                val inverseRatio = if (weekendAvg > 0.0) weekdayAvg / weekendAvg else 1.0
                val formattedRatio = String.format(java.util.Locale.US, "%.1f", inverseRatio).removeSuffix(".0")
                "You spend ${formattedRatio}× more per day on weekdays."
            }
        }

        return WeekendVsWeekdayData(
            weekdayTotal = weekdayTotal,
            weekdayDaysCount = weekdayDays,
            weekdayAvgPerDay = weekdayAvg,
            weekdayTopCategory = weekdayTopCategory,
            weekendTotal = weekendTotal,
            weekendDaysCount = weekendDays,
            weekendAvgPerDay = weekendAvg,
            weekendTopCategory = weekendTopCategory,
            ratio = ratio,
            insightSentence = wording
        )
    }

    fun calculateWeekendVsWeekday(
        weekdayTotal: Double,
        weekdayDaysCount: Int,
        weekdayTopCat: String?,
        weekendTotal: Double,
        weekendDaysCount: Int,
        weekendTopCat: String?
    ): WeekendVsWeekdayData {
        return calculateWeekendWeekday(
            weekdayTotal = weekdayTotal,
            weekdayDays = weekdayDaysCount,
            weekendTotal = weekendTotal,
            weekendDays = weekendDaysCount,
            weekdayTopCategory = weekdayTopCat,
            weekendTopCategory = weekendTopCat
        ) ?: WeekendVsWeekdayData(
            weekdayTotal = weekdayTotal,
            weekdayDaysCount = weekdayDaysCount,
            weekdayAvgPerDay = 0.0,
            weekdayTopCategory = weekdayTopCat,
            weekendTotal = weekendTotal,
            weekendDaysCount = weekendDaysCount,
            weekendAvgPerDay = 0.0,
            weekendTopCategory = weekendTopCat,
            ratio = 1.0,
            insightSentence = "You spend about the same per day on weekends as on weekdays."
        )
    }

    // -------------------------------------------------------------------------
    // 5.13 Small Spends Extrapolation
    // -------------------------------------------------------------------------
    fun calculateSmallSpendsPace(totalSmallSpend: Double, daysInRange: Int): Double {
        if (daysInRange <= 0) return 0.0
        return (totalSmallSpend / daysInRange) * 365.0
    }

    fun calculateYearlyPace(totalSmallSpend: Double, daysInRange: Int): Double {
        return calculateSmallSpendsPace(totalSmallSpend, daysInRange)
    }

    // -------------------------------------------------------------------------
    // 5.14 Income Regularity Classifier
    // -------------------------------------------------------------------------
    fun classifyIncomeSource(
        monthlyAmounts: Map<YearMonth, List<Double>>, // YearMonth -> list of payments
        referenceMonth: YearMonth = YearMonth.now()
    ): Pair<IncomeRegularityTag, Int?> {
        // Last 4 completed calendar months before referenceMonth
        val last4Months = (1..4).map { referenceMonth.minusMonths(it.toLong()) }
        val monthsPaidCount = last4Months.count { ym -> (monthlyAmounts[ym]?.sum() ?: 0.0) > 0.0 }

        val isRegular = monthsPaidCount >= 3

        if (!isRegular) {
            return Pair(IncomeRegularityTag.IRREGULAR, null)
        }

        // Check if amounts are steady (within ±15% of median)
        val allAmounts = monthlyAmounts.values.flatten().filter { it > 0.0 }
        val median = calculateMedian(allAmounts)

        val isSteady = if (median > 0.0 && allAmounts.isNotEmpty()) {
            allAmounts.all { abs(it - median) / median <= 0.15 }
        } else false

        val tag = if (isSteady) IncomeRegularityTag.REGULAR_STEADY else IncomeRegularityTag.REGULAR
        return Pair(tag, null)
    }

    fun classifyIncomeRegularity(
        history: List<IncomeHistoryRecord>,
        referenceMonth: YearMonth = YearMonth.now()
    ): IncomeRegularityTag {
        val last4Months = (1..4).map { referenceMonth.minusMonths(it.toLong()) }
        val paidMonths = history.filter { it.month in last4Months && it.amount > 0.0 }
        if (paidMonths.size < 3) {
            return IncomeRegularityTag.IRREGULAR
        }
        val amounts = paidMonths.map { it.amount }
        val median = calculateMedian(amounts)
        val isSteady = if (median > 0.0) {
            amounts.all { abs(it - median) / median <= 0.15 }
        } else false
        return if (isSteady) IncomeRegularityTag.REGULAR_STEADY else IncomeRegularityTag.REGULAR
    }

    // -------------------------------------------------------------------------
    // 5.16 No-Spend Streaks
    // -------------------------------------------------------------------------
    fun calculateNoSpendStreaks(
        spendDates: Set<LocalDate>,
        firstDate: LocalDate,
        today: LocalDate
    ): Triple<Int, Int, ClosedRange<LocalDate>?> {
        val yesterday = today.minusDays(1)
        if (firstDate.isAfter(yesterday)) {
            return Triple(0, 0, null)
        }

        var longestStreak = 0
        var longestStart: LocalDate? = null
        var longestEnd: LocalDate? = null

        var currentRun = 0
        var currentRunStart: LocalDate? = null

        var curr = firstDate
        while (!curr.isAfter(yesterday)) {
            val hasSpend = curr in spendDates
            if (!hasSpend) {
                if (currentRun == 0) {
                    currentRunStart = curr
                }
                currentRun++
                if (currentRun > longestStreak) {
                    longestStreak = currentRun
                    longestStart = currentRunStart
                    longestEnd = curr
                }
            } else {
                currentRun = 0
                currentRunStart = null
            }
            curr = curr.plusDays(1)
        }

        var currentStreak = 0
        var backDate = yesterday
        while (!backDate.isBefore(firstDate) && backDate !in spendDates) {
            currentStreak++
            backDate = backDate.minusDays(1)
        }

        val longestRange = if (longestStart != null && longestEnd != null) {
            longestStart..longestEnd
        } else null

        return Triple(currentStreak, longestStreak, longestRange)
    }

    // -------------------------------------------------------------------------
    // 5.15 Suggested Budget Rounding
    // -------------------------------------------------------------------------
    fun roundSuggestedBudget(baseAmount: Double): Double {
        if (baseAmount <= 0.0) return 0.0
        val step = if (baseAmount > 5000.0) 500.0 else 100.0
        return ceil(baseAmount / step) * step
    }

    fun calculateSuggestedCategoryBudget(pastMonthsSpend: List<Double>): Double {
        val nonZero = pastMonthsSpend.filter { it > 0.0 }
        if (nonZero.isEmpty()) return 0.0

        val base = if (nonZero.size >= 3) {
            calculateMedian(nonZero.takeLast(3))
        } else {
            nonZero.average()
        }

        return roundSuggestedBudget(base)
    }

    // -------------------------------------------------------------------------
    // Delta and Percentage Calculations
    // -------------------------------------------------------------------------
    fun calculateDelta(current: Double, previous: Double?): Pair<Double?, Double?> {
        if (previous == null || previous == 0.0) {
            return Pair(null, current)
        }
        val diffAmount = current - previous
        val rawPercent = (diffAmount / previous) * 100.0
        return Pair(rawPercent, diffAmount)
    }

    fun calculateDelta(current: Double, previous: Double?, isExpense: Boolean): DeltaResult {
        if (previous == null || previous == 0.0) {
            return DeltaResult(null, current, DeltaColorSemantic.MUTED)
        }
        val diffAmount = current - previous
        val rawPercent = (diffAmount / previous) * 100.0
        val semantic = if (diffAmount == 0.0) {
            DeltaColorSemantic.MUTED
        } else if (isExpense) {
            if (diffAmount > 0) DeltaColorSemantic.EXPENSE_ACCENT else DeltaColorSemantic.INCOME_ACCENT
        } else {
            if (diffAmount > 0) DeltaColorSemantic.INCOME_ACCENT else DeltaColorSemantic.EXPENSE_ACCENT
        }
        return DeltaResult(rawPercent, diffAmount, semantic)
    }

    fun formatDeltaPercent(percent: Double?): String? {
        if (percent == null) return null
        val absVal = abs(percent)
        return if (absVal > 999.0) {
            ">999%"
        } else if (absVal >= 10.0) {
            String.format(java.util.Locale.US, "%.0f%%", absVal)
        } else {
            String.format(java.util.Locale.US, "%.1f%%", absVal)
        }
    }

    fun getDeltaColor(
        percent: Double?,
        isExpense: Boolean,
        defaultMuted: Color = Color(0xFF8E8E93)
    ): Color {
        if (percent == null || percent == 0.0) return defaultMuted
        return if (isExpense) {
            if (percent > 0) ExpenseRed else IncomeGreen
        } else {
            if (percent > 0) IncomeGreen else ExpenseRed
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    fun calculateMedian(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val n = sorted.size
        return if (n % 2 == 1) {
            sorted[n / 2]
        } else {
            (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
    }
}
