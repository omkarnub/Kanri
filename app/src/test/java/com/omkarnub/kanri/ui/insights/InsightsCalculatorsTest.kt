package com.omkarnub.kanri.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class InsightsCalculatorsTest {

    // -------------------------------------------------------------------------
    // 1. Projection Tests
    // -------------------------------------------------------------------------
    @Test
    fun testProjection_earlyMonthGuard() {
        // Day 1, 2, 3 should return null (insufficient data)
        for (day in 1..3) {
            val result = InsightsCalculators.calculateProjection(
                spentSoFar = 3000.0,
                daysElapsed = day,
                daysRemaining = 27,
                activeRecurringUnpaid = 1000.0,
                activeRecurringPaidSoFar = 500.0,
                oneOffSpendsSoFar = 0.0,
                monthlyBudget = 20000.0,
                lastMonthTotal = 25000.0
            )
            assertNull("Projection must be null on day $day", result)
        }
    }

    @Test
    fun testProjection_normalCalculation() {
        // Day 10 of 30, spent so far 10,000, recurring paid 2,000, one-off 1,000.
        // variableRunRate = (10,000 - 2,000 - 1,000) / 10 = 700 / day
        // daysRemaining = 20, unpaid recurring = 3,000
        // projected = 10,000 + (700 * 20) + 3,000 = 27,000
        val result = InsightsCalculators.calculateProjection(
            spentSoFar = 10000.0,
            daysElapsed = 10,
            daysRemaining = 20,
            activeRecurringUnpaid = 3000.0,
            activeRecurringPaidSoFar = 2000.0,
            oneOffSpendsSoFar = 1000.0,
            monthlyBudget = 25000.0,
            lastMonthTotal = 24000.0,
            referenceDate = LocalDate.of(2026, 9, 10)
        )

        assertNotNull(result)
        assertEquals(27000.0, result!!.projectedTotal, 0.01)
        assertEquals(2000.0, result.gap!!, 0.01) // 27,000 - 25,000 = 2,000 over budget
        assertTrue(result.isOverBudget)
        assertNotNull(result.budgetExhaustionDate)
    }

    @Test
    fun testProjection_clampedToSpentSoFar() {
        // If run rate is 0 or negative due to refunds, projected is clamped to spentSoFar
        val result = InsightsCalculators.calculateProjection(
            spentSoFar = 15000.0,
            daysElapsed = 15,
            daysRemaining = 15,
            activeRecurringUnpaid = 0.0,
            activeRecurringPaidSoFar = 20000.0, // paid more than spentSoFar
            oneOffSpendsSoFar = 0.0,
            monthlyBudget = null,
            lastMonthTotal = 15000.0
        )

        assertNotNull(result)
        assertEquals(15000.0, result!!.projectedTotal, 0.01)
        assertNull(result.budget)
    }

    @Test
    fun testOneOffIdentification() {
        // With budget: spend >= 25% of budget
        val budget = 40000.0
        val medianTx = 200.0

        val (isOneOff1, _) = InsightsCalculators.identifyOneOffSpend(12000.0, budget, medianTx)
        assertTrue("12,000 is 30% of 40,000 budget => one-off", isOneOff1)

        val (isOneOff2, _) = InsightsCalculators.identifyOneOffSpend(5000.0, budget, medianTx)
        assertFalse("5,000 is 12.5% of budget => not one-off", isOneOff2)

        // Without budget: spend >= 5x median
        val (isOneOff3, _) = InsightsCalculators.identifyOneOffSpend(1200.0, null, medianTx)
        assertTrue("1,200 is 6x median => one-off", isOneOff3)
    }

    // -------------------------------------------------------------------------
    // 2. Quantiles Bucketing Tests
    // -------------------------------------------------------------------------
    @Test
    fun testQuantileThresholdsAndLevel() {
        val nonZeroAmounts = listOf(50.0, 100.0, 150.0, 200.0, 500.0, 1000.0, 2000.0, 5000.0)
        val thresholds = InsightsCalculators.calculateQuantileThresholds(nonZeroAmounts)

        assertEquals(4, thresholds.size)
        // Check levels
        assertEquals(0, InsightsCalculators.getQuantileLevel(0.0, thresholds))
        assertEquals(1, InsightsCalculators.getQuantileLevel(40.0, thresholds))
        assertEquals(5, InsightsCalculators.getQuantileLevel(10000.0, thresholds))
    }

    // -------------------------------------------------------------------------
    // 3. Income Regularity Classifier Tests
    // -------------------------------------------------------------------------
    @Test
    fun testIncomeRegularity_regularSteady() {
        // Paid in 3 of last 4 months, amounts within ±15%
        val history = listOf(
            IncomeHistoryRecord(YearMonth.of(2026, 8), 50000.0, 1),
            IncomeHistoryRecord(YearMonth.of(2026, 7), 51000.0, 1),
            IncomeHistoryRecord(YearMonth.of(2026, 6), 49500.0, 1)
        )
        val tag = InsightsCalculators.classifyIncomeRegularity(history, YearMonth.of(2026, 9))
        assertEquals(IncomeRegularityTag.REGULAR_STEADY, tag)
    }

    @Test
    fun testIncomeRegularity_regularNotSteady() {
        // Paid in 3 of last 4 months, but amounts vary by > 15%
        val history = listOf(
            IncomeHistoryRecord(YearMonth.of(2026, 8), 50000.0, 1),
            IncomeHistoryRecord(YearMonth.of(2026, 7), 80000.0, 1),
            IncomeHistoryRecord(YearMonth.of(2026, 6), 30000.0, 1)
        )
        val tag = InsightsCalculators.classifyIncomeRegularity(history, YearMonth.of(2026, 9))
        assertEquals(IncomeRegularityTag.REGULAR, tag)
    }

    @Test
    fun testIncomeRegularity_irregular() {
        // Paid in only 1 of last 4 months
        val history = listOf(
            IncomeHistoryRecord(YearMonth.of(2026, 7), 50000.0, 1)
        )
        val tag = InsightsCalculators.classifyIncomeRegularity(history, YearMonth.of(2026, 9))
        assertEquals(IncomeRegularityTag.IRREGULAR, tag)
    }

    // -------------------------------------------------------------------------
    // 4. Suggested Budget Rounding Tests
    // -------------------------------------------------------------------------
    @Test
    fun testSuggestedBudgetRounding() {
        // Under ₹5,000 rounds up to nearest ₹100
        assertEquals(2400.0, InsightsCalculators.roundSuggestedBudget(2305.0), 0.01)
        assertEquals(4900.0, InsightsCalculators.roundSuggestedBudget(4820.0), 0.01)

        // Above ₹5,000 rounds up to nearest ₹500
        assertEquals(5500.0, InsightsCalculators.roundSuggestedBudget(5010.0), 0.01)
        assertEquals(12500.0, InsightsCalculators.roundSuggestedBudget(12100.0), 0.01)
    }

    @Test
    fun testSuggestedBudget_medianVsMean() {
        // 3 months uses median
        val threeMonths = listOf(1000.0, 5000.0, 1200.0) // sorted: 1000, 1200, 5000 => median 1200
        val res3 = InsightsCalculators.calculateSuggestedCategoryBudget(threeMonths)
        assertEquals(1200.0, res3, 0.01)

        // 2 months uses mean
        val twoMonths = listOf(2000.0, 3000.0) // mean = 2500
        val res2 = InsightsCalculators.calculateSuggestedCategoryBudget(twoMonths)
        assertEquals(2500.0, res2, 0.01)
    }

    // -------------------------------------------------------------------------
    // 5. Weekend vs Weekday Tests
    // -------------------------------------------------------------------------
    @Test
    fun testWeekendVsWeekdayRatio() {
        // Weekday avg = 1000, Weekend avg = 1800 => 1.8x
        val res = InsightsCalculators.calculateWeekendVsWeekday(
            weekdayTotal = 10000.0,
            weekdayDaysCount = 10,
            weekdayTopCat = "Food",
            weekendTotal = 7200.0,
            weekendDaysCount = 4,
            weekendTopCat = "Shopping"
        )
        assertEquals(1.8, res.ratio, 0.05)
        assertEquals("You spend 1.8× more per day on weekends.", res.insightSentence)

        // Balanced: ratio between 0.9 and 1.1
        val resBalanced = InsightsCalculators.calculateWeekendVsWeekday(
            weekdayTotal = 5000.0,
            weekdayDaysCount = 5,
            weekdayTopCat = "Food",
            weekendTotal = 2050.0,
            weekendDaysCount = 2,
            weekendTopCat = "Food"
        )
        assertEquals("You spend about the same per day on weekends as on weekdays.", resBalanced.insightSentence)
    }

    // -------------------------------------------------------------------------
    // 6. Small Spends Tests
    // -------------------------------------------------------------------------
    @Test
    fun testSmallSpendsExtrapolation() {
        val totalSmall = 2000.0
        val daysInRange = 20
        // (2000 / 20) * 365 = 36,500
        val pace = InsightsCalculators.calculateYearlyPace(totalSmall, daysInRange)
        assertEquals(36500.0, pace, 0.01)
    }

    // -------------------------------------------------------------------------
    // 7. No-Spend Streak Tests
    // -------------------------------------------------------------------------
    @Test
    fun testNoSpendStreakLogic() {
        val today = LocalDate.of(2026, 9, 21)
        val spendDates = setOf(
            LocalDate.of(2026, 9, 10),
            LocalDate.of(2026, 9, 15)
        )
        val startDate = LocalDate.of(2026, 9, 1)

        val (currentStreak, longestStreak, longestRange) = InsightsCalculators.calculateNoSpendStreaks(
            spendDates = spendDates,
            firstDate = startDate,
            today = today
        )

        // Between 15 and 20 (today not ended) => 16, 17, 18, 19, 20 = 5 days
        assertEquals(5, currentStreak)
        assertTrue(longestStreak >= 5)
        assertNotNull(longestRange)
    }

    // -------------------------------------------------------------------------
    // 8. Category Movers Thresholds Tests
    // -------------------------------------------------------------------------
    @Test
    fun testCategoryMoversThresholds() {
        val currentSpends = mapOf<Long?, Double>(
            1L to 50.0,   // both < 100 => ignore
            2L to 500.0,  // was 450 => delta 50 (< 100) => ignore
            3L to 1500.0, // was 500 => delta +1000 => Up mover
            4L to 200.0   // was 1200 => delta -1000 => Down mover
        )
        val prevSpends = mapOf<Long?, Double>(
            1L to 60.0,
            2L to 450.0,
            3L to 500.0,
            4L to 1200.0
        )
        val meta = mapOf<Long?, Triple<String, String, String>>(
            1L to Triple("Snacks", "food", "#123456"),
            2L to Triple("Bills", "bill", "#123456"),
            3L to Triple("Travel", "flight", "#123456"),
            4L to Triple("Shopping", "cart", "#123456")
        )

        val (up, down) = InsightsCalculators.calculateMovers(currentSpends, prevSpends, meta)
        assertEquals(1, up.size)
        assertEquals("Travel", up[0].categoryName)
        assertEquals(1000.0, up[0].deltaAmount, 0.01)

        assertEquals(1, down.size)
        assertEquals("Shopping", down[0].categoryName)
        assertEquals(-1000.0, down[0].deltaAmount, 0.01)
    }

    // -------------------------------------------------------------------------
    // 9. Delta Color Semantics Tests
    // -------------------------------------------------------------------------
    @Test
    fun testDeltaColorSemantics() {
        // Spent Up = Red (Expense Accent), Spent Down = Sage (Income Accent)
        val spentUp = InsightsCalculators.calculateDelta(1200.0, 1000.0, isExpense = true)
        assertEquals(DeltaColorSemantic.EXPENSE_ACCENT, spentUp.colorSemantic)

        val spentDown = InsightsCalculators.calculateDelta(800.0, 1000.0, isExpense = true)
        assertEquals(DeltaColorSemantic.INCOME_ACCENT, spentDown.colorSemantic)

        // Income Up = Sage (Income Accent), Income Down = Red (Expense Accent)
        val incomeUp = InsightsCalculators.calculateDelta(12000.0, 10000.0, isExpense = false)
        assertEquals(DeltaColorSemantic.INCOME_ACCENT, incomeUp.colorSemantic)

        val incomeDown = InsightsCalculators.calculateDelta(8000.0, 10000.0, isExpense = false)
        assertEquals(DeltaColorSemantic.EXPENSE_ACCENT, incomeDown.colorSemantic)

        // No comparison / 0 delta = Muted
        val zeroDelta = InsightsCalculators.calculateDelta(1000.0, 1000.0, isExpense = true)
        assertEquals(DeltaColorSemantic.MUTED, zeroDelta.colorSemantic)
    }
}
