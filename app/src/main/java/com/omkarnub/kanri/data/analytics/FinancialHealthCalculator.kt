package com.omkarnub.kanri.data.analytics

import androidx.compose.runtime.Immutable
import com.omkarnub.kanri.data.budget.BudgetCalculator
import com.omkarnub.kanri.util.CurrencyUtils
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

enum class HealthGrade(
    val letter: String,
    val title: String,
    val colorLong: Long
) {
    EXCELLENT("A+", "Exceptional", 0xFFFFFFFF),
    GOOD("A", "Healthy", 0xFFE0E0E0),
    FAIR("B", "Balanced", 0xFFCCCCCC),
    NEEDS_ATTENTION("C", "Caution", 0xFFAAAAAA),
    AT_RISK("D", "At Risk", 0xFF888888)
}

enum class PacingStatus(val label: String, val description: String) {
    UNDER_PACING("Frugal Pacing", "Spending slower than calendar progress"),
    ON_TRACK("On Track", "Spending evenly aligned with monthly budget"),
    ACCELERATED("Accelerated", "Spending faster than calendar progress"),
    CRITICAL_BURN("Budget Overrun", "Budget exceeded or nearing exhaustion")
}

@Immutable
data class FinancialHealthPillar(
    val title: String,
    val score: Int,
    val maxScore: Int,
    val status: String,
    val description: String,
    val metricDisplay: String
)

@Immutable
data class FinancialHealthTip(
    val title: String,
    val description: String,
    val isPositive: Boolean = false
)

@Immutable
data class FinancialHealthData(
    val score: Int, // 0..100
    val grade: HealthGrade,
    val summaryHeadline: String,
    val diagnosticSummary: String,
    // 4 Mathematical Pillars
    val savingsPillar: FinancialHealthPillar,
    val budgetPillar: FinancialHealthPillar,
    val velocityPillar: FinancialHealthPillar,
    val debtPillar: FinancialHealthPillar,
    // Core Financial Metrics & Statistics
    val monthIncome: Double,
    val monthSpent: Double,
    val netSavingsAmount: Double,
    val savingsRatePercent: Double,
    val monthlyBudget: Double,
    val remainingBudget: Double,
    val budgetSpentPercent: Double,
    val monthElapsedPercent: Double,
    val pacingStatus: PacingStatus,
    val avgDailySpend: Double,
    val safeDailySpend: Double,
    val daysRemaining: Int,
    val daysElapsed: Int,
    val totalDaysInMonth: Int,
    val totalBorrowed: Double,
    val totalLent: Double,
    val projectedMonthEndSpend: Double,
    val projectedBudgetVariance: Double,
    val strengths: List<String>,
    val riskFactors: List<String>,
    val tips: List<FinancialHealthTip>
)

object FinancialHealthCalculator {

    /**
     * Computes holistic Financial Health score, pillar ratings, projections,
     * and quantitative statistics for the current or selected month.
     */
    fun calculate(
        monthSpent: Double,
        monthIncome: Double,
        monthlyBudget: Double,
        daysRemaining: Int,
        totalBorrowed: Double = 0.0,
        totalLent: Double = 0.0,
        calendar: Calendar = Calendar.getInstance()
    ): FinancialHealthData {
        val totalDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val daysElapsed = currentDay.coerceAtLeast(1)
        val effectiveDaysRemaining = daysRemaining.coerceAtLeast(1)
        val effectiveBudget = if (monthlyBudget > 0.0) monthlyBudget else BudgetCalculator.DEFAULT_MONTHLY_BUDGET

        val netSavingsAmount = monthIncome - monthSpent
        val savingsRatePercent = if (monthIncome > 0.0) {
            (netSavingsAmount / monthIncome) * 100.0
        } else 0.0

        val budgetSpentPercent = if (effectiveBudget > 0.0) {
            (monthSpent / effectiveBudget) * 100.0
        } else 0.0

        val monthElapsedPercent = if (totalDaysInMonth > 0) {
            (daysElapsed.toDouble() / totalDaysInMonth.toDouble()) * 100.0
        } else 50.0

        val remainingBudget = effectiveBudget - monthSpent
        val avgDailySpend = if (daysElapsed > 0) monthSpent / daysElapsed else 0.0
        val safeDailySpend = if (remainingBudget > 0.0) remainingBudget / effectiveDaysRemaining else 0.0

        // =========================================================================
        // PILLAR 1: SAVINGS & CASH FLOW (35 Max Points)
        // =========================================================================
        val (savingsScore, savingsStatus, savingsDesc, savingsDisplay) = computeSavingsPillar(
            monthIncome = monthIncome,
            monthSpent = monthSpent,
            savingsRatePercent = savingsRatePercent
        )
        val savingsPillar = FinancialHealthPillar(
            title = "Cash Flow & Savings",
            score = savingsScore,
            maxScore = 35,
            status = savingsStatus,
            description = savingsDesc,
            metricDisplay = savingsDisplay
        )

        // =========================================================================
        // PILLAR 2: BUDGET DISCIPLINE & PACING (35 Max Points)
        // =========================================================================
        val (budgetScore, budgetStatus, budgetDesc, budgetDisplay, pacingStatus) = computeBudgetPillar(
            monthSpent = monthSpent,
            effectiveBudget = effectiveBudget,
            budgetSpentPercent = budgetSpentPercent,
            monthElapsedPercent = monthElapsedPercent,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth
        )
        val budgetPillar = FinancialHealthPillar(
            title = "Budget Adherence",
            score = budgetScore,
            maxScore = 35,
            status = budgetStatus,
            description = budgetDesc,
            metricDisplay = budgetDisplay
        )

        // =========================================================================
        // PILLAR 3: SPENDING VELOCITY & SAFE RUN-RATE (15 Max Points)
        // =========================================================================
        val (velocityScore, velocityStatus, velocityDesc, velocityDisplay) = computeVelocityPillar(
            monthSpent = monthSpent,
            avgDailySpend = avgDailySpend,
            safeDailySpend = safeDailySpend,
            remainingBudget = remainingBudget
        )
        val velocityPillar = FinancialHealthPillar(
            title = "Daily Spend Velocity",
            score = velocityScore,
            maxScore = 15,
            status = velocityStatus,
            description = velocityDesc,
            metricDisplay = velocityDisplay
        )

        // =========================================================================
        // PILLAR 4: DEBT & OBLIGATION EXPOSURE (15 Max Points)
        // =========================================================================
        val (debtScore, debtStatus, debtDesc, debtDisplay) = computeDebtPillar(
            totalBorrowed = totalBorrowed,
            totalLent = totalLent,
            monthIncome = monthIncome,
            effectiveBudget = effectiveBudget
        )
        val debtPillar = FinancialHealthPillar(
            title = "Debt & Lending Ratio",
            score = debtScore,
            maxScore = 15,
            status = debtStatus,
            description = debtDesc,
            metricDisplay = debtDisplay
        )

        // Composite Score (0..100)
        val totalScore = (savingsScore + budgetScore + velocityScore + debtScore).coerceIn(0, 100)
        val grade = when {
            totalScore >= 88 -> HealthGrade.EXCELLENT
            totalScore >= 72 -> HealthGrade.GOOD
            totalScore >= 55 -> HealthGrade.FAIR
            totalScore >= 40 -> HealthGrade.NEEDS_ATTENTION
            else -> HealthGrade.AT_RISK
        }

        // Projections
        val projectedMonthEndSpend = if (daysElapsed > 0) {
            avgDailySpend * totalDaysInMonth
        } else monthSpent
        val projectedBudgetVariance = effectiveBudget - projectedMonthEndSpend

        // Diagnostics, Strengths, Risks & Factual Notes
        val strengths = mutableListOf<String>()
        val risks = mutableListOf<String>()
        val tips = mutableListOf<FinancialHealthTip>()

        generateFactualDiagnostics(
            monthIncome = monthIncome,
            monthSpent = monthSpent,
            netSavings = netSavingsAmount,
            savingsRate = savingsRatePercent,
            effectiveBudget = effectiveBudget,
            budgetSpentPercent = budgetSpentPercent,
            monthElapsedPercent = monthElapsedPercent,
            avgDailySpend = avgDailySpend,
            safeDailySpend = safeDailySpend,
            totalBorrowed = totalBorrowed,
            totalLent = totalLent,
            projectedSpend = projectedMonthEndSpend,
            strengths = strengths,
            risks = risks,
            tips = tips
        )

        val summaryHeadline = generateSummaryHeadline(grade, pacingStatus, netSavingsAmount)
        val diagnosticSummary = generateDiagnosticSummary(netSavingsAmount, budgetSpentPercent, monthElapsedPercent, remainingBudget)

        return FinancialHealthData(
            score = totalScore,
            grade = grade,
            summaryHeadline = summaryHeadline,
            diagnosticSummary = diagnosticSummary,
            savingsPillar = savingsPillar,
            budgetPillar = budgetPillar,
            velocityPillar = velocityPillar,
            debtPillar = debtPillar,
            monthIncome = monthIncome,
            monthSpent = monthSpent,
            netSavingsAmount = netSavingsAmount,
            savingsRatePercent = savingsRatePercent,
            monthlyBudget = effectiveBudget,
            remainingBudget = remainingBudget,
            budgetSpentPercent = budgetSpentPercent,
            monthElapsedPercent = monthElapsedPercent,
            pacingStatus = pacingStatus,
            avgDailySpend = avgDailySpend,
            safeDailySpend = safeDailySpend,
            daysRemaining = effectiveDaysRemaining,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDaysInMonth,
            totalBorrowed = totalBorrowed,
            totalLent = totalLent,
            projectedMonthEndSpend = projectedMonthEndSpend,
            projectedBudgetVariance = projectedBudgetVariance,
            strengths = strengths,
            riskFactors = risks,
            tips = tips
        )
    }

    private fun computeSavingsPillar(
        monthIncome: Double,
        monthSpent: Double,
        savingsRatePercent: Double
    ): PillarResult {
        if (monthIncome <= 0.0) {
            return if (monthSpent <= 0.0) {
                PillarResult(
                    score = 25,
                    status = "Neutral",
                    description = "No outflows or inflows recorded for this period.",
                    display = "--"
                )
            } else {
                PillarResult(
                    score = 18,
                    status = "Pending Inflows",
                    description = "Inflows not logged. Savings rate calculation pending.",
                    display = "Pending"
                )
            }
        }

        return when {
            savingsRatePercent >= 35.0 -> PillarResult(
                score = 35,
                status = "Exceptional",
                description = "Retaining ${savingsRatePercent.roundToInt()}% of inflows. High cash retention rate.",
                display = "+${savingsRatePercent.roundToInt()}%"
            )
            savingsRatePercent >= 20.0 -> {
                val score = (28 + ((savingsRatePercent - 20.0) / 15.0) * 7).roundToInt()
                PillarResult(
                    score = score.coerceIn(28, 35),
                    status = "Healthy",
                    description = "Retaining ${savingsRatePercent.roundToInt()}% of monthly income.",
                    display = "+${savingsRatePercent.roundToInt()}%"
                )
            }
            savingsRatePercent >= 10.0 -> {
                val score = (20 + ((savingsRatePercent - 10.0) / 10.0) * 8).roundToInt()
                PillarResult(
                    score = score.coerceIn(20, 27),
                    status = "Moderate",
                    description = "Retaining ${savingsRatePercent.roundToInt()}% of monthly income.",
                    display = "+${savingsRatePercent.roundToInt()}%"
                )
            }
            savingsRatePercent >= 0.0 -> {
                val score = (12 + (savingsRatePercent / 10.0) * 8).roundToInt()
                PillarResult(
                    score = score.coerceIn(12, 19),
                    status = "Narrow Surplus",
                    description = "Inflows exceed outflows by ${savingsRatePercent.roundToInt()}%.",
                    display = "+${savingsRatePercent.roundToInt()}%"
                )
            }
            else -> { // Deficit
                val absRate = abs(savingsRatePercent)
                val score = when {
                    absRate <= 25.0 -> 8
                    absRate <= 60.0 -> 4
                    else -> 1
                }
                PillarResult(
                    score = score,
                    status = "Cash Deficit",
                    description = "Outflows exceed inflows by ${CurrencyUtils.formatCompactCurrency(monthSpent - monthIncome)} (${absRate.roundToInt()}% deficit).",
                    display = "-${absRate.roundToInt()}%"
                )
            }
        }
    }

    private fun computeBudgetPillar(
        monthSpent: Double,
        effectiveBudget: Double,
        budgetSpentPercent: Double,
        monthElapsedPercent: Double,
        daysElapsed: Int,
        totalDaysInMonth: Int
    ): BudgetPillarResult {
        if (effectiveBudget <= 0.0) {
            return BudgetPillarResult(25, "Uncapped", "Standard budget allocation", "N/A", PacingStatus.ON_TRACK)
        }

        if (monthSpent > effectiveBudget) {
            val overagePercent = budgetSpentPercent - 100.0
            val score = (10 - (overagePercent * 0.25).roundToInt()).coerceIn(0, 8)
            return BudgetPillarResult(
                score = score,
                status = "Over Budget",
                description = "Budget limit exceeded by ${CurrencyUtils.formatCompactCurrency(monthSpent - effectiveBudget)} (${budgetSpentPercent.roundToInt()}% consumed).",
                display = "${budgetSpentPercent.roundToInt()}%",
                pacingStatus = PacingStatus.CRITICAL_BURN
            )
        }

        val timeFraction = (daysElapsed.toDouble() / totalDaysInMonth.toDouble()).coerceAtLeast(0.04)
        val budgetFraction = (monthSpent / effectiveBudget)
        val paceRatio = budgetFraction / timeFraction

        return when {
            paceRatio <= 0.82 -> BudgetPillarResult(
                score = 35,
                status = "Disciplined",
                description = "Spent ${budgetSpentPercent.roundToInt()}% of budget with ${monthElapsedPercent.roundToInt()}% of days elapsed.",
                display = "${budgetSpentPercent.roundToInt()}%",
                pacingStatus = PacingStatus.UNDER_PACING
            )
            paceRatio <= 1.05 -> {
                val score = (30 + ((1.05 - paceRatio) / 0.23) * 5).roundToInt().coerceIn(30, 34)
                BudgetPillarResult(
                    score = score,
                    status = "On Track",
                    description = "Spending aligns with calendar day progression (${budgetSpentPercent.roundToInt()}% used).",
                    display = "${budgetSpentPercent.roundToInt()}%",
                    pacingStatus = PacingStatus.ON_TRACK
                )
            }
            paceRatio <= 1.30 -> {
                val score = (18 + ((1.30 - paceRatio) / 0.25) * 11).roundToInt().coerceIn(18, 29)
                BudgetPillarResult(
                    score = score,
                    status = "Accelerated",
                    description = "Budget consumption is ${(budgetSpentPercent - monthElapsedPercent).roundToInt()}% ahead of calendar progress.",
                    display = "${budgetSpentPercent.roundToInt()}%",
                    pacingStatus = PacingStatus.ACCELERATED
                )
            }
            else -> {
                val score = (8 + ((2.0 - paceRatio).coerceAtLeast(0.0) * 8)).roundToInt().coerceIn(4, 17)
                BudgetPillarResult(
                    score = score,
                    status = "High Burn",
                    description = "High burn rate: ${budgetSpentPercent.roundToInt()}% of budget consumed in ${daysElapsed} days.",
                    display = "${budgetSpentPercent.roundToInt()}%",
                    pacingStatus = PacingStatus.CRITICAL_BURN
                )
            }
        }
    }

    private fun computeVelocityPillar(
        monthSpent: Double,
        avgDailySpend: Double,
        safeDailySpend: Double,
        remainingBudget: Double
    ): PillarResult {
        if (monthSpent <= 0.0) {
            return PillarResult(
                score = 15,
                status = "Zero Outflow",
                description = "No outflows recorded for this period.",
                display = "₹0/d"
            )
        }

        if (remainingBudget <= 0.0) {
            return PillarResult(
                score = 1,
                status = "Budget Exhausted",
                description = "Remaining monthly allocation is ₹0.",
                display = "Exhausted"
            )
        }

        val velocityRatio = if (safeDailySpend > 0.0) avgDailySpend / safeDailySpend else 2.0
        return when {
            velocityRatio <= 0.95 -> PillarResult(
                score = 15,
                status = "Within Safe Limit",
                description = "Average burn of ${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d is under safe limit of ${CurrencyUtils.formatCompactCurrency(safeDailySpend)}/d.",
                display = "${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d"
            )
            velocityRatio <= 1.20 -> {
                val score = (11 + ((1.20 - velocityRatio) / 0.25) * 3).roundToInt().coerceIn(11, 14)
                PillarResult(
                    score = score,
                    status = "Moderate Velocity",
                    description = "Daily burn closely matches safe allowance.",
                    display = "${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d"
                )
            }
            velocityRatio <= 1.50 -> {
                val score = (6 + ((1.50 - velocityRatio) / 0.30) * 4).roundToInt().coerceIn(6, 10)
                PillarResult(
                    score = score,
                    status = "Elevated Velocity",
                    description = "Average burn exceeds safe daily allowance by ${(velocityRatio * 100 - 100).roundToInt()}%.",
                    display = "${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d"
                )
            }
            else -> PillarResult(
                score = 3,
                status = "High Velocity",
                description = "Daily burn rate significantly exceeds daily safe-to-spend allowance.",
                display = "${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d"
            )
        }
    }

    private fun computeDebtPillar(
        totalBorrowed: Double,
        totalLent: Double,
        monthIncome: Double,
        effectiveBudget: Double
    ): PillarResult {
        if (totalBorrowed <= 0.0) {
            return if (totalLent > 0.0) {
                PillarResult(
                    score = 15,
                    status = "Net Creditor",
                    description = "Zero debt obligations; ${CurrencyUtils.formatCompactCurrency(totalLent)} in pending receivables.",
                    display = "Zero Debt"
                )
            } else {
                PillarResult(
                    score = 15,
                    status = "Debt Free",
                    description = "No outstanding borrowed obligations.",
                    display = "Zero Debt"
                )
            }
        }

        val netDebt = (totalBorrowed - totalLent).coerceAtLeast(0.0)
        if (netDebt == 0.0) {
            return PillarResult(
                score = 14,
                status = "Covered Debt",
                description = "Borrowed amount is fully offset by pending receivables.",
                display = "Covered"
            )
        }

        val benchmark = if (monthIncome > 0.0) monthIncome else effectiveBudget
        val debtRatio = netDebt / benchmark

        return when {
            debtRatio <= 0.15 -> PillarResult(
                score = 12,
                status = "Low Burden",
                description = "Borrowed liabilities represent ${(debtRatio * 100).roundToInt()}% of monthly baseline.",
                display = CurrencyUtils.formatCompactCurrency(totalBorrowed)
            )
            debtRatio <= 0.40 -> PillarResult(
                score = 8,
                status = "Moderate Burden",
                description = "Borrowed liabilities represent ${(debtRatio * 100).roundToInt()}% of monthly baseline.",
                display = CurrencyUtils.formatCompactCurrency(totalBorrowed)
            )
            else -> PillarResult(
                score = 4,
                status = "High Burden",
                description = "Borrowed liabilities exceed 40% of monthly baseline.",
                display = CurrencyUtils.formatCompactCurrency(totalBorrowed)
            )
        }
    }

    private fun generateFactualDiagnostics(
        monthIncome: Double,
        monthSpent: Double,
        netSavings: Double,
        savingsRate: Double,
        effectiveBudget: Double,
        budgetSpentPercent: Double,
        monthElapsedPercent: Double,
        avgDailySpend: Double,
        safeDailySpend: Double,
        totalBorrowed: Double,
        totalLent: Double,
        projectedSpend: Double,
        strengths: MutableList<String>,
        risks: MutableList<String>,
        tips: MutableList<FinancialHealthTip>
    ) {
        // Strengths (Factual quantitative statements)
        if (savingsRate >= 20.0) {
            strengths.add("Savings Rate: +${savingsRate.roundToInt()}% of monthly inflows retained")
        }
        if (budgetSpentPercent < monthElapsedPercent && monthSpent > 0.0) {
            strengths.add("Budget Pacing: Spending is ${(monthElapsedPercent - budgetSpentPercent).roundToInt()}% slower than calendar progress")
        }
        if (totalBorrowed <= 0.0) {
            strengths.add("Debt Exposure: Zero outstanding borrowed liabilities")
        }
        if (netSavings > 0.0) {
            strengths.add("Cash Flow: Net positive cash surplus of ${CurrencyUtils.formatCompactCurrency(netSavings)}")
        }
        if (avgDailySpend > 0 && avgDailySpend <= safeDailySpend) {
            strengths.add("Daily Velocity: ${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d is within safe limit of ${CurrencyUtils.formatCompactCurrency(safeDailySpend)}/d")
        }
        if (totalLent > 0.0) {
            strengths.add("Receivables: ${CurrencyUtils.formatCompactCurrency(totalLent)} in pending lending assets")
        }
        if (strengths.isEmpty()) {
            strengths.add("Tracking: Active financial logging in progress")
        }

        // Risks / Alerts (Factual quantitative observations)
        if (monthIncome > 0.0 && monthSpent > monthIncome) {
            risks.add("Cash Deficit: Outflows exceed inflows by ${CurrencyUtils.formatCompactCurrency(monthSpent - monthIncome)}")
        }
        if (monthSpent > effectiveBudget) {
            risks.add("Budget Limit: Allocation exceeded by ${CurrencyUtils.formatCompactCurrency(monthSpent - effectiveBudget)}")
        } else if (budgetSpentPercent > monthElapsedPercent + 15.0) {
            risks.add("Burn Acceleration: Budget consumption is ${(budgetSpentPercent - monthElapsedPercent).roundToInt()}% ahead of calendar days")
        }
        if (avgDailySpend > safeDailySpend * 1.30 && safeDailySpend > 0.0) {
            risks.add("Daily Run-Rate: ${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/d vs safe threshold of ${CurrencyUtils.formatCompactCurrency(safeDailySpend)}/d")
        }
        if (totalBorrowed > 0.0 && totalBorrowed > totalLent) {
            risks.add("Net Debt: ${CurrencyUtils.formatCompactCurrency(totalBorrowed - totalLent)} outstanding liabilities")
        }
        if (projectedSpend > effectiveBudget && monthSpent <= effectiveBudget) {
            risks.add("Projected Overrun: Run-rate projects month-end spend at ${CurrencyUtils.formatCompactCurrency(projectedSpend)}")
        }

        // Quantitative Observations (Hard numbers, NO AI conversational slop)
        if (monthIncome > 0.0) {
            val statusStr = if (netSavings >= 0) "Surplus of ${CurrencyUtils.formatCompactCurrency(netSavings)}" else "Deficit of ${CurrencyUtils.formatCompactCurrency(abs(netSavings))}"
            tips.add(
                FinancialHealthTip(
                    title = "Cash Flow Balance",
                    description = "Inflows: ${CurrencyUtils.formatCompactCurrency(monthIncome)} | Outflows: ${CurrencyUtils.formatCompactCurrency(monthSpent)} ($statusStr).",
                    isPositive = netSavings >= 0
                )
            )
        } else {
            tips.add(
                FinancialHealthTip(
                    title = "Cash Inflow Status",
                    description = "No income transactions recorded for this period. Savings rate calculation is pending.",
                    isPositive = false
                )
            )
        }

        if (safeDailySpend > 0.0) {
            tips.add(
                FinancialHealthTip(
                    title = "Daily Spending Threshold",
                    description = "Safe allowance: ${CurrencyUtils.formatCompactCurrency(safeDailySpend)}/day | Current average: ${CurrencyUtils.formatCompactCurrency(avgDailySpend)}/day.",
                    isPositive = avgDailySpend <= safeDailySpend
                )
            )
        }

        if (effectiveBudget > 0.0) {
            val varianceStr = if (effectiveBudget >= projectedSpend) "${CurrencyUtils.formatCompactCurrency(effectiveBudget - projectedSpend)} under cap" else "${CurrencyUtils.formatCompactCurrency(projectedSpend - effectiveBudget)} over cap"
            tips.add(
                FinancialHealthTip(
                    title = "Month-End Run-Rate Projection",
                    description = "Projected spend: ${CurrencyUtils.formatCompactCurrency(projectedSpend)} of ${CurrencyUtils.formatCompactCurrency(effectiveBudget)} budget ($varianceStr).",
                    isPositive = effectiveBudget >= projectedSpend
                )
            )
        }
    }

    private fun generateSummaryHeadline(
        grade: HealthGrade,
        pacing: PacingStatus,
        netSavings: Double
    ): String {
        return when {
            netSavings < 0.0 && pacing == PacingStatus.CRITICAL_BURN -> "Negative Cash Flow • Budget Overrun"
            netSavings < 0.0 -> "Negative Cash Flow • Outflows Exceed Inflows"
            pacing == PacingStatus.CRITICAL_BURN -> "Budget Limit Exceeded • Outflows Constrained"
            pacing == PacingStatus.ACCELERATED -> "Accelerated Spend Velocity • Monitor Pacing"
            pacing == PacingStatus.UNDER_PACING && netSavings > 0 -> "Frugal Spend Pace • Positive Surplus"
            else -> "Controlled Cash Flow • Balanced Pacing"
        }
    }

    private fun generateDiagnosticSummary(
        netSavings: Double,
        budgetSpentPercent: Double,
        monthElapsedPercent: Double,
        remainingBudget: Double
    ): String {
        val cashStr = if (netSavings >= 0) "+${CurrencyUtils.formatCompactCurrency(netSavings)} surplus" else "-${CurrencyUtils.formatCompactCurrency(abs(netSavings))} deficit"
        return "Net cash $cashStr. Budget is ${budgetSpentPercent.roundToInt()}% consumed with ${monthElapsedPercent.roundToInt()}% of month elapsed."
    }

    private data class PillarResult(
        val score: Int,
        val status: String,
        val description: String,
        val display: String
    )

    private data class BudgetPillarResult(
        val score: Int,
        val status: String,
        val description: String,
        val display: String,
        val pacingStatus: PacingStatus
    )
}
