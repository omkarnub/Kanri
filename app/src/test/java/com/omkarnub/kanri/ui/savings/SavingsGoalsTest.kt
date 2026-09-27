package com.omkarnub.kanri.ui.savings

import com.omkarnub.kanri.data.db.SavingsGoalEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalsTest {

    @Test
    fun testProgressPercentage() {
        val percent = SavingsGoalCalculator.calculateProgressPercentage(2500.0, 10000.0)
        assertEquals(25.0f, percent, 0.01f)

        val zeroPercent = SavingsGoalCalculator.calculateProgressPercentage(0.0, 10000.0)
        assertEquals(0.0f, zeroPercent, 0.01f)

        val overHundred = SavingsGoalCalculator.calculateProgressPercentage(12000.0, 10000.0)
        assertEquals(120.0f, overHundred, 0.01f)

        val zeroTarget = SavingsGoalCalculator.calculateProgressPercentage(100.0, 0.0)
        assertEquals(0.0f, zeroTarget, 0.01f)
    }

    @Test
    fun testRemainingAmount() {
        val remaining = SavingsGoalCalculator.calculateRemainingAmount(4000.0, 10000.0)
        assertEquals(6000.0, remaining, 0.01)

        val achievedRemaining = SavingsGoalCalculator.calculateRemainingAmount(12000.0, 10000.0)
        assertEquals(0.0, achievedRemaining, 0.01)
    }

    @Test
    fun testIsGoalCompleted() {
        assertFalse(SavingsGoalCalculator.isGoalCompleted(4000.0, 10000.0))
        assertTrue(SavingsGoalCalculator.isGoalCompleted(10000.0, 10000.0))
        assertTrue(SavingsGoalCalculator.isGoalCompleted(10500.0, 10000.0))
        assertFalse(SavingsGoalCalculator.isGoalCompleted(500.0, 0.0))
    }

    @Test
    fun testDepositAndWithdraw() {
        var current = 1000.0
        current = SavingsGoalCalculator.calculateDeposit(current, 500.0)
        assertEquals(1500.0, current, 0.01)

        // Invalid negative deposit should not change balance
        current = SavingsGoalCalculator.calculateDeposit(current, -200.0)
        assertEquals(1500.0, current, 0.01)

        // Valid withdraw
        current = SavingsGoalCalculator.calculateWithdraw(current, 300.0)
        assertEquals(1200.0, current, 0.01)

        // Over-withdrawal should clamp to zero
        current = SavingsGoalCalculator.calculateWithdraw(current, 2000.0)
        assertEquals(0.0, current, 0.01)
    }

    @Test
    fun testOverallStatsCalculation() {
        val goals = listOf(
            SavingsGoalEntity(id = 1, title = "Laptop", targetAmount = 50000.0, currentAmount = 25000.0),
            SavingsGoalEntity(id = 2, title = "Emergency Fund", targetAmount = 50000.0, currentAmount = 50000.0, isCompleted = true),
            SavingsGoalEntity(id = 3, title = "Goa Trip", targetAmount = 20000.0, currentAmount = 5000.0)
        )

        val stats = SavingsGoalCalculator.calculateOverallStats(goals)
        assertEquals(80000.0, stats.totalSaved, 0.01)
        assertEquals(120000.0, stats.totalTarget, 0.01)
        assertEquals(66.67f, stats.overallPercentage, 0.1f)
        assertEquals(3, stats.totalGoals)
        assertEquals(1, stats.completedGoals)
    }

    @Test
    fun testDaysRemainingAndPaceCalculators() {
        val now = 1000000000L
        val oneDayMillis = 86_400_000L
        val thirtyDaysLater = now + 30 * oneDayMillis

        // 30 days remaining
        val days = SavingsGoalCalculator.calculateDaysRemaining(thirtyDaysLater, now)
        assertEquals(30L, days)

        // Past deadline
        val pastDays = SavingsGoalCalculator.calculateDaysRemaining(now - oneDayMillis, now)
        assertEquals(-1L, pastDays)

        // Required monthly pace (remaining 30,000 in 30 days ~= 1 month)
        val monthly = SavingsGoalCalculator.calculateRequiredMonthlySavings(
            currentAmount = 20000.0,
            targetAmount = 50000.0,
            targetDateMillis = thirtyDaysLater,
            currentTimeMillis = now
        )
        // 30,000 / 1.0 month = 30000
        assertEquals(30000.0, monthly ?: 0.0, 100.0)

        // Required daily pace (30,000 / 30 = 1000)
        val daily = SavingsGoalCalculator.calculateRequiredDailySavings(
            currentAmount = 20000.0,
            targetAmount = 50000.0,
            targetDateMillis = thirtyDaysLater,
            currentTimeMillis = now
        )
        assertEquals(1000.0, daily ?: 0.0, 10.0)
    }

    @Test
    fun testGoalIconHelper() {
        val laptopRes = getGoalIconRes("laptop")
        assertEquals(com.omkarnub.kanri.R.drawable.ic_goal_laptop, laptopRes)

        // Test fallback for unknown key
        val unknownRes = getGoalIconRes("random_unknown_key")
        assertEquals(com.omkarnub.kanri.R.drawable.ic_goal_target, unknownRes)

        // Test legacy emoji mapping
        val emojiRes = getGoalIconRes("🚗")
        assertEquals(com.omkarnub.kanri.R.drawable.ic_goal_car, emojiRes)
    }
}
