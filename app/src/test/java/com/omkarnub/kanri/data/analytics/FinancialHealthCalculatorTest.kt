package com.omkarnub.kanri.data.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class FinancialHealthCalculatorTest {

    private fun createCalendar(dayOfMonth: Int, totalDaysInMonth: Int = 30): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER) // September has 30 days
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
    }

    @Test
    fun calculate_exceptionalFinancialHealth_returnsHighScoreAndGradeA() {
        val calendar = createCalendar(dayOfMonth = 15) // Mid-month (Day 15/30 = 50% elapsed)

        val health = FinancialHealthCalculator.calculate(
            monthSpent = 15000.0,
            monthIncome = 60000.0, // 75% savings rate!
            monthlyBudget = 30000.0, // Spent 50% of budget when 50% elapsed -> on track
            daysRemaining = 15,
            totalBorrowed = 0.0,
            totalLent = 5000.0,
            calendar = calendar
        )

        assertEquals(HealthGrade.EXCELLENT, health.grade)
        assertTrue("Score should be >= 88 for exceptional health, was ${health.score}", health.score >= 88)
        assertEquals(35, health.savingsPillar.score) // Max savings score
        assertEquals(15, health.debtPillar.score) // Debt-free
        assertTrue(health.strengths.isNotEmpty())
        assertTrue(health.savingsRatePercent >= 70.0)
    }

    @Test
    fun calculate_overBudgetScenario_returnsCriticalPacingAndLowerGrade() {
        val calendar = createCalendar(dayOfMonth = 10) // Early in month

        val health = FinancialHealthCalculator.calculate(
            monthSpent = 35000.0,
            monthIncome = 30000.0, // Deficit!
            monthlyBudget = 25000.0, // Over budget by 10k
            daysRemaining = 20,
            totalBorrowed = 8000.0,
            totalLent = 0.0,
            calendar = calendar
        )

        assertEquals(PacingStatus.CRITICAL_BURN, health.pacingStatus)
        assertTrue("Budget pillar score should be low due to overrun, was ${health.budgetPillar.score}", health.budgetPillar.score <= 10)
        assertTrue("Overall score should be low, was ${health.score}", health.score < 55)
        assertTrue(health.riskFactors.any { it.contains("Deficit") || it.contains("exceeded") })
    }

    @Test
    fun calculate_noIncomeRecorded_handlesGracefullyWithoutCrash() {
        val calendar = createCalendar(dayOfMonth = 5)

        val health = FinancialHealthCalculator.calculate(
            monthSpent = 2000.0,
            monthIncome = 0.0, // No income logged yet
            monthlyBudget = 20000.0,
            daysRemaining = 25,
            totalBorrowed = 0.0,
            totalLent = 0.0,
            calendar = calendar
        )

        assertTrue(health.score in 50..85)
        assertEquals("Pending", health.savingsPillar.metricDisplay)
        assertTrue(health.tips.any { it.title.contains("Inflow") })

        // Pure start of month with 0 spends and 0 income
        val zeroHealth = FinancialHealthCalculator.calculate(
            monthSpent = 0.0,
            monthIncome = 0.0,
            monthlyBudget = 20000.0,
            daysRemaining = 30,
            calendar = calendar
        )
        assertEquals("--", zeroHealth.savingsPillar.metricDisplay)
    }

    @Test
    fun calculate_debtCoveredByReceivables_awardsHighDebtScore() {
        val calendar = createCalendar(dayOfMonth = 15)

        val health = FinancialHealthCalculator.calculate(
            monthSpent = 10000.0,
            monthIncome = 30000.0,
            monthlyBudget = 20000.0,
            daysRemaining = 15,
            totalBorrowed = 5000.0,
            totalLent = 6000.0, // Receivables exceed liabilities
            calendar = calendar
        )

        assertTrue("Debt score should be >= 13 when covered, was ${health.debtPillar.score}", health.debtPillar.score >= 13)
        assertEquals("Covered", health.debtPillar.metricDisplay)
    }

    @Test
    fun calculate_projectedMonthEndSpend_computesAccurateVelocity() {
        val calendar = createCalendar(dayOfMonth = 10) // 10 days elapsed of 30

        val health = FinancialHealthCalculator.calculate(
            monthSpent = 10000.0, // 1,000/day
            monthIncome = 40000.0,
            monthlyBudget = 25000.0,
            daysRemaining = 20,
            totalBorrowed = 0.0,
            totalLent = 0.0,
            calendar = calendar
        )

        // 10,000 / 10 days * 30 days = 30,000 projected
        assertEquals(30000.0, health.projectedMonthEndSpend, 1.0)
        assertEquals(-5000.0, health.projectedBudgetVariance, 1.0)
    }
}
