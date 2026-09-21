package com.omkarnub.kanri.ui.savings

import com.omkarnub.kanri.data.db.SavingsGoalEntity
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
            completedGoals = completedCount
        )
    }
}

data class OverallSavingsStats(
    val totalSaved: Double,
    val totalTarget: Double,
    val overallPercentage: Float,
    val totalGoals: Int,
    val completedGoals: Int
)
