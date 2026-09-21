package com.omkarnub.kanri.ui.greeting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GreetingResolverTest {

    private fun createBaseContext(
        userName: String = "Alex",
        now: LocalDateTime = LocalDateTime.of(2026, 9, 20, 10, 0),
        dayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
        dayOfMonth: Int = 20,
        daysLeftInMonth: Int = 10,
        monthlyBudget: Double? = 20000.0,
        spentThisMonth: Double = 5000.0,
        spentToday: Double = 200.0,
        safeToSpendPerDay: Double? = 1500.0,
        lastMonthSpentSameDay: Double? = null,
        streakDays: Int = 0,
        needsReviewCount: Int = 0,
        isFirstLaunchToday: Boolean = true,
        seed: Long = 12345L,
        lastSubtitleRuleId: Int? = null,
        lastShownEpochDay: Long? = null,
        lastWarningBucket: String? = null,
        lastWarningEpochDay: Long? = null
    ): GreetingContext {
        return GreetingContext(
            userName = userName,
            now = now,
            dayOfWeek = dayOfWeek,
            dayOfMonth = dayOfMonth,
            daysLeftInMonth = daysLeftInMonth,
            monthlyBudget = monthlyBudget,
            spentThisMonth = spentThisMonth,
            spentToday = spentToday,
            safeToSpendPerDay = safeToSpendPerDay,
            lastMonthSpentSameDay = lastMonthSpentSameDay,
            streakDays = streakDays,
            needsReviewCount = needsReviewCount,
            isFirstLaunchToday = isFirstLaunchToday,
            seed = seed,
            lastSubtitleRuleId = lastSubtitleRuleId,
            lastShownEpochDay = lastShownEpochDay,
            lastWarningBucket = lastWarningBucket,
            lastWarningEpochDay = lastWarningEpochDay
        )
    }

    // 1. Time-bucket boundaries
    @Test
    fun testTimeBucketBoundaries() {
        assertEquals(TimeBucket.LateNight, GreetingResolver.getTimeBucket(LocalTime.of(4, 59)))
        assertEquals(TimeBucket.Morning, GreetingResolver.getTimeBucket(LocalTime.of(5, 0)))
        assertEquals(TimeBucket.Morning, GreetingResolver.getTimeBucket(LocalTime.of(11, 59)))
        assertEquals(TimeBucket.Afternoon, GreetingResolver.getTimeBucket(LocalTime.of(12, 0)))
        assertEquals(TimeBucket.Afternoon, GreetingResolver.getTimeBucket(LocalTime.of(16, 59)))
        assertEquals(TimeBucket.Evening, GreetingResolver.getTimeBucket(LocalTime.of(17, 0)))
        assertEquals(TimeBucket.Evening, GreetingResolver.getTimeBucket(LocalTime.of(20, 59)))
        assertEquals(TimeBucket.Night, GreetingResolver.getTimeBucket(LocalTime.of(21, 0)))
        assertEquals(TimeBucket.Night, GreetingResolver.getTimeBucket(LocalTime.of(23, 59)))
        assertEquals(TimeBucket.LateNight, GreetingResolver.getTimeBucket(LocalTime.of(0, 0)))
    }

    // 2. Determinism: Same seed gives same output; different seed gives different variant
    @Test
    fun testDeterminismWithSeed() {
        val context1 = createBaseContext(seed = 42L)
        val context2 = createBaseContext(seed = 42L)
        val result1 = GreetingResolver.resolve(context1)
        val result2 = GreetingResolver.resolve(context2)

        assertEquals(result1.greeting, result2.greeting)
        assertEquals(result1.subtitle, result2.subtitle)
        assertEquals(result1.tone, result2.tone)

        // Different day/seed
        val contextDiff = createBaseContext(
            now = LocalDateTime.of(2026, 9, 21, 10, 0),
            seed = 99999L
        )
        val resultDiff = GreetingResolver.resolve(contextDiff)
        // With different seeds, variations are deterministic
        assertFalse(resultDiff.greeting.isBlank())
    }

    // 3. Priority Order: Over-budget beats streak milestone
    @Test
    fun testOverBudgetBeatsStreakMilestone() {
        val context = createBaseContext(
            monthlyBudget = 10000.0,
            spentThisMonth = 12500.0, // Over budget by 2,500
            streakDays = 7,          // 7-day milestone streak
            seed = 10L               // Seed where finance rules are enabled
        )
        val result = GreetingResolver.resolve(context)

        assertEquals(1, result.ruleId)
        assertEquals(Tone.Warning, result.tone)
        assertTrue(result.subtitle.contains("crossed your budget by"))
        assertEquals(GreetingAction.OpenInsights, result.action)
    }

    // 4. Null or zero budget skips finance rules
    @Test
    fun testNullOrZeroBudgetSkipsFinanceRules() {
        val contextNull = createBaseContext(
            monthlyBudget = null,
            spentThisMonth = 25000.0,
            dayOfMonth = 20,
            seed = 10L
        )
        val resultNull = GreetingResolver.resolve(contextNull)
        // Must skip Rule 1, 2, 5 and not crash with divide-by-zero
        assertNotEquals(1, resultNull.ruleId)
        assertNotEquals(2, resultNull.ruleId)
        assertNotEquals(5, resultNull.ruleId)

        val contextZero = createBaseContext(
            monthlyBudget = 0.0,
            spentThisMonth = 5000.0,
            dayOfMonth = 20,
            seed = 10L
        )
        val resultZero = GreetingResolver.resolve(contextZero)
        assertNotEquals(1, resultZero.ruleId)
        assertNotEquals(2, resultZero.ruleId)
        assertNotEquals(5, resultZero.ruleId)
    }

    // 5. Blank name formatting
    @Test
    fun testBlankNameFormatting() {
        val blankContext = createBaseContext(
            userName = "",
            now = LocalDateTime.of(2026, 9, 20, 9, 0)
        )
        val result = GreetingResolver.resolve(blankContext)
        assertFalse(result.greeting.contains(","))
        assertFalse(result.greeting.endsWith(" "))
        assertTrue(
            result.greeting == "Good morning" ||
                result.greeting == "Morning" ||
                result.greeting == "Rise and shine" ||
                result.greeting == "Fresh day"
        )
    }

    // 6. Anti-repetition suppression and fall-through
    @Test
    fun testAntiRepetitionSuppressionAndFallThrough() {
        val today = LocalDate.of(2026, 9, 20)
        val todayEpochDay = today.toEpochDay()
        val yesterdayEpochDay = todayEpochDay - 1

        // Case A: Rule 4 (Streak) shown yesterday -> Suppressed today, falls through to next
        val streakContextSuppressed = createBaseContext(
            now = LocalDateTime.of(today, LocalTime.of(10, 0)),
            streakDays = 7,
            seed = 10L, // Finance rules allowed
            lastSubtitleRuleId = 4,
            lastShownEpochDay = yesterdayEpochDay
        )
        val resultSuppressed = GreetingResolver.resolve(streakContextSuppressed)
        assertNotEquals(4, resultSuppressed.ruleId)

        // Case B: Rule 1 (Over budget) shown yesterday -> NOT suppressed (allowed consecutive days)
        val overBudgetContext = createBaseContext(
            now = LocalDateTime.of(today, LocalTime.of(10, 0)),
            monthlyBudget = 10000.0,
            spentThisMonth = 15000.0,
            lastSubtitleRuleId = 1,
            lastShownEpochDay = yesterdayEpochDay
        )
        val resultOverBudget = GreetingResolver.resolve(overBudgetContext)
        assertEquals(1, resultOverBudget.ruleId)
        assertEquals(Tone.Warning, resultOverBudget.tone)

        // Case C: Warnings appear at most once per time bucket
        val warningContextSuppressed = createBaseContext(
            now = LocalDateTime.of(today, LocalTime.of(10, 0)), // Morning bucket
            monthlyBudget = 10000.0,
            spentThisMonth = 15000.0,
            lastWarningBucket = "Morning",
            lastWarningEpochDay = todayEpochDay
        )
        val resultWarningSuppressed = GreetingResolver.resolve(warningContextSuppressed)
        // Warning is suppressed for Morning bucket today, so Rule 1 falls through
        assertNotEquals(1, resultWarningSuppressed.ruleId)
        assertNotEquals(Tone.Warning, resultWarningSuppressed.tone)
    }

    // 7. Month start/end edge cases (28-31 day months)
    @Test
    fun testMonthStartAndEndEdgeCases() {
        // Day 1: New month, fresh budget
        val monthStartContext = createBaseContext(
            now = LocalDateTime.of(2026, 9, 1, 10, 0),
            dayOfMonth = 1,
            daysLeftInMonth = 29,
            seed = 90L // Non-finance seed
        )
        val resultStart = GreetingResolver.resolve(monthStartContext)
        assertEquals(9, resultStart.ruleId)
        assertEquals("New month, fresh budget", resultStart.subtitle)
        assertEquals(Tone.Neutral, resultStart.tone)

        // Month end (<= 3 days left): February 28 in non-leap year (daysLeft = 1)
        val monthEndContext = createBaseContext(
            now = LocalDateTime.of(2026, 2, 28, 10, 0),
            dayOfMonth = 28,
            daysLeftInMonth = 1,
            safeToSpendPerDay = 850.0,
            seed = 90L // Non-finance seed
        )
        val resultEnd = GreetingResolver.resolve(monthEndContext)
        assertEquals(10, resultEnd.ruleId)
        assertTrue(resultEnd.subtitle.contains("1 day left"))
        assertEquals(Tone.Neutral, resultEnd.tone)
    }
}
