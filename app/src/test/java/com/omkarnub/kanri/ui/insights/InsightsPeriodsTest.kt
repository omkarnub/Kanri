package com.omkarnub.kanri.ui.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class InsightsPeriodsTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    @Test
    fun testMonthWindow_completedMonth() {
        val ym = YearMonth.of(2026, 1) // Jan 2026 (31 days)
        val today = LocalDate.of(2026, 3, 10)

        val window = InsightsPeriods.calculateWindow(InsightsRange.Month(ym), zone, today)
        assertEquals(LocalDate.of(2026, 1, 1), window.startDate)
        assertEquals(LocalDate.of(2026, 1, 31), window.endDate)
        assertEquals(31, window.daysCount)
        assertEquals(31, window.elapsedDaysToCompare)

        val prevWindow = InsightsPeriods.calculatePreviousWindow(InsightsRange.Month(ym), zone, today)
        assertEquals(LocalDate.of(2025, 12, 1), prevWindow.startDate)
        assertEquals(LocalDate.of(2025, 12, 31), prevWindow.endDate)
        assertEquals(31, prevWindow.daysCount)
    }

    @Test
    fun testMonthWindow_inProgressMonth_likeForLikeClamp() {
        val ym = YearMonth.of(2026, 3) // March 2026
        val today = LocalDate.of(2026, 3, 15) // Day 15

        val window = InsightsPeriods.calculateWindow(InsightsRange.Month(ym), zone, today)
        assertEquals(15, window.elapsedDaysToCompare)

        // Previous month is Feb 2026 (28 days)
        val prevWindow = InsightsPeriods.calculatePreviousWindow(InsightsRange.Month(ym), zone, today)
        assertEquals(LocalDate.of(2026, 2, 1), prevWindow.startDate)
        assertEquals(LocalDate.of(2026, 2, 15), prevWindow.endDate)
        assertEquals(15, prevWindow.daysCount)
    }

    @Test
    fun testMonthWindow_leapYearFeb_31to29Clamp() {
        val ym = YearMonth.of(2024, 3) // March 2024 (leap year)
        val today = LocalDate.of(2024, 3, 31) // Day 31

        val prevWindow = InsightsPeriods.calculatePreviousWindow(InsightsRange.Month(ym), zone, today)
        assertEquals(LocalDate.of(2024, 2, 1), prevWindow.startDate)
        // Clamped to Feb 29 in leap year
        assertEquals(LocalDate.of(2024, 2, 29), prevWindow.endDate)
        assertEquals(29, prevWindow.daysCount)
    }

    @Test
    fun testDays30Window() {
        val today = LocalDate.of(2026, 9, 21)
        val window = InsightsPeriods.calculateWindow(InsightsRange.Days30, zone, today)

        assertEquals(LocalDate.of(2026, 8, 23), window.startDate)
        assertEquals(today, window.endDate)
        assertEquals(30, window.daysCount)

        val prevWindow = InsightsPeriods.calculatePreviousWindow(InsightsRange.Days30, zone, today)
        assertEquals(LocalDate.of(2026, 7, 24), prevWindow.startDate)
        assertEquals(LocalDate.of(2026, 8, 22), prevWindow.endDate)
        assertEquals(30, prevWindow.daysCount)
    }

    @Test
    fun testMonths3Window() {
        val today = LocalDate.of(2026, 9, 21)
        val window = InsightsPeriods.calculateWindow(InsightsRange.Months3, zone, today)

        // First day of current month - 2 => July 1, 2026
        assertEquals(LocalDate.of(2026, 7, 1), window.startDate)
        assertEquals(today, window.endDate)
        assertTrue(window.daysCount > 80)
    }

    @Test
    fun testYearWindow() {
        val today = LocalDate.of(2026, 9, 21)
        val window = InsightsPeriods.calculateWindow(InsightsRange.Year, zone, today)

        // First day of current month - 11 => October 1, 2025
        assertEquals(LocalDate.of(2025, 10, 1), window.startDate)
        assertEquals(today, window.endDate)
    }

    @Test
    fun testCustomWindow_singleDay() {
        val date = LocalDate.of(2026, 5, 12)
        val window = InsightsPeriods.calculateWindow(InsightsRange.Custom(date, date), zone)

        assertEquals(date, window.startDate)
        assertEquals(date, window.endDate)
        assertEquals(1, window.daysCount)

        val prevWindow = InsightsPeriods.calculatePreviousWindow(InsightsRange.Custom(date, date), zone)
        assertEquals(LocalDate.of(2026, 5, 11), prevWindow.startDate)
        assertEquals(LocalDate.of(2026, 5, 11), prevWindow.endDate)
        assertEquals(1, prevWindow.daysCount)
    }
}
