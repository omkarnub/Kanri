package com.omkarnub.kanri.ui.insights

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class RangeWindow(
    val startMillis: Long,
    val endMillis: Long,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val daysCount: Int,
    val isCompletedPeriod: Boolean,
    val elapsedDaysToCompare: Int = daysCount
)

object InsightsPeriods {

    fun toStartOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun toEndOfDayMillis(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        return date.atTime(23, 59, 59, 999_000_000).atZone(zoneId).toInstant().toEpochMilli()
    }

    /**
     * Calculates the active RangeWindow for the specified InsightsRange.
     */
    fun calculateWindow(
        range: InsightsRange,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: LocalDate = LocalDate.now()
    ): RangeWindow = calculateRangeWindow(range, now, zoneId)

    fun calculatePreviousWindow(
        range: InsightsRange,
        zoneId: ZoneId = ZoneId.systemDefault(),
        now: LocalDate = LocalDate.now()
    ): RangeWindow = calculatePreviousPeriod(range, now, zoneId)

    fun calculateRangeWindow(
        range: InsightsRange,
        now: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RangeWindow {
        return when (range) {
            is InsightsRange.Month -> {
                val ym = range.yearMonth
                val currentYm = YearMonth.from(now)
                val isCurrent = (ym == currentYm)
                val isPast = ym.isBefore(currentYm)

                val start = ym.atDay(1)
                val end = if (isCurrent) {
                    now
                } else if (isPast) {
                    ym.atEndOfMonth()
                } else {
                    ym.atEndOfMonth()
                }

                val daysCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
                val elapsedDays = if (isCurrent) now.dayOfMonth else ym.lengthOfMonth()
                RangeWindow(
                    startMillis = toStartOfDayMillis(start, zoneId),
                    endMillis = toEndOfDayMillis(end, zoneId),
                    startDate = start,
                    endDate = end,
                    daysCount = daysCount,
                    isCompletedPeriod = !isCurrent,
                    elapsedDaysToCompare = elapsedDays
                )
            }

            is InsightsRange.Days30 -> {
                val start = now.minusDays(29)
                RangeWindow(
                    startMillis = toStartOfDayMillis(start, zoneId),
                    endMillis = toEndOfDayMillis(now, zoneId),
                    startDate = start,
                    endDate = now,
                    daysCount = 30,
                    isCompletedPeriod = false
                )
            }

            is InsightsRange.Months3 -> {
                val startMonth = YearMonth.from(now).minusMonths(2)
                val start = startMonth.atDay(1)
                val daysCount = ChronoUnit.DAYS.between(start, now).toInt() + 1
                RangeWindow(
                    startMillis = toStartOfDayMillis(start, zoneId),
                    endMillis = toEndOfDayMillis(now, zoneId),
                    startDate = start,
                    endDate = now,
                    daysCount = daysCount,
                    isCompletedPeriod = false
                )
            }

            is InsightsRange.Year -> {
                val startMonth = YearMonth.from(now).minusMonths(11)
                val start = startMonth.atDay(1)
                val daysCount = ChronoUnit.DAYS.between(start, now).toInt() + 1
                RangeWindow(
                    startMillis = toStartOfDayMillis(start, zoneId),
                    endMillis = toEndOfDayMillis(now, zoneId),
                    startDate = start,
                    endDate = now,
                    daysCount = daysCount,
                    isCompletedPeriod = false
                )
            }

            is InsightsRange.Custom -> {
                val start = range.from
                val end = range.to
                val daysCount = ChronoUnit.DAYS.between(start, end).toInt() + 1
                RangeWindow(
                    startMillis = toStartOfDayMillis(start, zoneId),
                    endMillis = toEndOfDayMillis(end, zoneId),
                    startDate = start,
                    endDate = end,
                    daysCount = daysCount,
                    isCompletedPeriod = end.isBefore(now)
                )
            }
        }
    }

    /**
     * Calculates the previous equivalent comparison period according to Section 3.2 rules:
     * - For Month: previous calendar month. If selected month is in progress, compare like-for-like
     *   elapsed days clamped to previous month length. Full month if completed.
     * - All other ranges: equal-length window immediately preceding range start.
     */
    fun calculatePreviousPeriod(
        range: InsightsRange,
        now: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RangeWindow {
        return when (range) {
            is InsightsRange.Month -> {
                val ym = range.yearMonth
                val currentYm = YearMonth.from(now)
                val isCurrent = (ym == currentYm)
                val prevYm = ym.minusMonths(1)

                val prevStart = prevYm.atDay(1)
                val prevEnd = if (isCurrent) {
                    val elapsedDays = now.dayOfMonth
                    val clampedDays = elapsedDays.coerceAtMost(prevYm.lengthOfMonth())
                    prevYm.atDay(clampedDays)
                } else {
                    prevYm.atEndOfMonth()
                }

                val daysCount = ChronoUnit.DAYS.between(prevStart, prevEnd).toInt() + 1
                RangeWindow(
                    startMillis = toStartOfDayMillis(prevStart, zoneId),
                    endMillis = toEndOfDayMillis(prevEnd, zoneId),
                    startDate = prevStart,
                    endDate = prevEnd,
                    daysCount = daysCount,
                    isCompletedPeriod = !isCurrent,
                    elapsedDaysToCompare = daysCount
                )
            }

            else -> {
                val currentWindow = calculateRangeWindow(range, now, zoneId)
                val lengthInDays = currentWindow.daysCount
                val prevEnd = currentWindow.startDate.minusDays(1)
                val prevStart = prevEnd.minusDays((lengthInDays - 1).toLong())

                RangeWindow(
                    startMillis = toStartOfDayMillis(prevStart, zoneId),
                    endMillis = toEndOfDayMillis(prevEnd, zoneId),
                    startDate = prevStart,
                    endDate = prevEnd,
                    daysCount = lengthInDays,
                    isCompletedPeriod = true,
                    elapsedDaysToCompare = lengthInDays
                )
            }
        }
    }
}
