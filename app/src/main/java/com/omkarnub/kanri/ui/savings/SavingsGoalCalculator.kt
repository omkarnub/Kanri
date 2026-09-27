package com.omkarnub.kanri.ui.savings

import com.omkarnub.kanri.data.db.SavingsGoalEntity
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.max

object SavingsGoalCalculator {

    fun calculateProgressPercentage(currentAmount: Double, targetAmount: Double): Float {
        if (targetAmount <= 0.0) return 0f
        val ratio = (currentAmount / targetAmount) * 100f
        return ratio.toFloat().coerceAtLeast(0f)
    }

    fun calculateRemainingAmount(currentAmount: Double, targetAmount: Double): Double {
        return max(0.0, targetAmount - currentAmount)
    }

    fun isGoalCompleted(currentAmount: Double, targetAmount: Double): Boolean {
        return targetAmount > 0.0 && currentAmount >= targetAmount
    }

    fun calculateDeposit(currentAmount: Double, depositAmount: Double): Double {
        if (depositAmount <= 0.0) return currentAmount
        return currentAmount + depositAmount
    }

    fun calculateWithdraw(currentAmount: Double, withdrawAmount: Double): Double {
        if (withdrawAmount <= 0.0) return currentAmount
        return max(0.0, currentAmount - withdrawAmount)
    }

    /**
     * Calculates days remaining until target deadline.
     * Returns null if no deadline is set, negative if overdue, positive if in future.
     */
    fun calculateDaysRemaining(targetDateMillis: Long?, currentTimeMillis: Long = System.currentTimeMillis()): Long? {
        if (targetDateMillis == null || targetDateMillis <= 0L) return null
        val diff = targetDateMillis - currentTimeMillis
        return TimeUnit.MILLISECONDS.toDays(diff)
    }

    /**
     * Calculates recommended monthly savings needed to hit deadline.
     */
    fun calculateRequiredMonthlySavings(
        currentAmount: Double,
        targetAmount: Double,
        targetDateMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Double? {
        val remaining = calculateRemainingAmount(currentAmount, targetAmount)
        if (remaining <= 0.0) return 0.0
        val days = calculateDaysRemaining(targetDateMillis, currentTimeMillis) ?: return null
        if (days <= 0) return remaining // Due now or overdue

        val months = max(1.0, days / 30.416)
        return ceil(remaining / months)
    }

    /**
     * Calculates recommended daily savings needed to hit deadline.
     */
    fun calculateRequiredDailySavings(
        currentAmount: Double,
        targetAmount: Double,
        targetDateMillis: Long?,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Double? {
        val remaining = calculateRemainingAmount(currentAmount, targetAmount)
        if (remaining <= 0.0) return 0.0
        val days = calculateDaysRemaining(targetDateMillis, currentTimeMillis) ?: return null
        if (days <= 0) return remaining
        return ceil(remaining / days)
    }

    fun calculateOverallStats(goals: List<SavingsGoalEntity>): OverallSavingsStats {
        var totalSaved = 0.0
        var totalTarget = 0.0
        var completedCount = 0

        for (goal in goals) {
            totalSaved += goal.currentAmount
            totalTarget += goal.targetAmount
            if (goal.isCompleted || (goal.targetAmount > 0.0 && goal.currentAmount >= goal.targetAmount)) {
                completedCount++
            }
        }

        val overallPercentage = if (totalTarget > 0.0) {
            ((totalSaved / totalTarget) * 100f).coerceIn(0.0, 100.0).toFloat()
        } else {
            0f
        }

        return OverallSavingsStats(
            totalSaved = totalSaved,
            totalTarget = totalTarget,
            overallPercentage = overallPercentage,
            totalGoals = goals.size,
            completedGoals = completedCount,
            inProgressGoals = goals.size - completedCount
        )
    }
}

data class OverallSavingsStats(
    val totalSaved: Double,
    val totalTarget: Double,
    val overallPercentage: Float,
    val totalGoals: Int,
    val completedGoals: Int,
    val inProgressGoals: Int = totalGoals - completedGoals
)
