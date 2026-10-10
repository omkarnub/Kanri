package com.omkarnub.kanri.data.analytics

import androidx.compose.runtime.Immutable
import com.omkarnub.kanri.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Immutable
data class DayStreakStatus(
    val dayLabel: String,
    val dateEpochDay: Long,
    val isNoSpend: Boolean,
    val isToday: Boolean
)

@Immutable
data class StreakState(
    val currentStreak: Int = 0,
    val lastStreak: Int = 0,
    val bestStreak: Int = 0,
    val noSpendDaysThisMonth: Int = 0,
    val currentMonthName: String = "",
    val last7Days: List<DayStreakStatus> = emptyList(),
    val isStreakActive: Boolean = true,
    val isFirstUse: Boolean = false
)

object StreakCalculator {

    /**
     * Calculates no-spend streak metrics.
     *
     * @param transactions All transactions from database
     * @param nowCalendar Current reference calendar (defaults to Calendar.getInstance())
     */
    fun calculateStreak(
        transactions: List<TransactionEntity>,
        nowCalendar: Calendar = Calendar.getInstance()
    ): StreakState {
        val monthFormat = SimpleDateFormat("MMMM", Locale.getDefault())
        val currentMonthName = monthFormat.format(nowCalendar.time)

        if (transactions.isEmpty()) {
            return StreakState(
                currentStreak = 0,
                lastStreak = 0,
                bestStreak = 0,
                noSpendDaysThisMonth = 0,
                currentMonthName = currentMonthName,
                last7Days = generateLast7Days(emptySet(), null, nowCalendar),
                isStreakActive = false,
                isFirstUse = true
            )
        }

        // Expenses only: filter type == "DEBIT" (excluding transfers)
        val debitTransactions = transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer
        }

        // Find earliest transaction timestamp (any type: establishes user start date)
        val earliestTxTime = transactions.minOf { it.timestamp }
        val firstDayCal = getStartOfDayCalendar(earliestTxTime)
        val todayCal = getStartOfDayCalendar(nowCalendar.timeInMillis)

        // If earliest transaction is in the future compared to nowCalendar
        if (firstDayCal.after(todayCal)) {
            return StreakState(
                currentStreak = 0,
                lastStreak = 0,
                bestStreak = 0,
                noSpendDaysThisMonth = 0,
                currentMonthName = currentMonthName,
                last7Days = generateLast7Days(emptySet(), todayCal.timeInMillis, nowCalendar),
                isStreakActive = false,
                isFirstUse = true
            )
        }

        // Map dates with expenses (Set of start-of-day millis)
        val expenseDates = HashSet<Long>()
        for (tx in debitTransactions) {
            val txDayMillis = getStartOfDayMillis(tx.timestamp)
            expenseDates.add(txDayMillis)
        }

        val todayMillis = todayCal.timeInMillis
        val todayHasSpend = expenseDates.contains(todayMillis)

        // 1. Calculate current streak and last streak
        var currentStreak = 0
        var lastStreak = 0

        if (!todayHasSpend) {
            // Count backwards from today
            val checkCal = todayCal.clone() as Calendar
            while (!checkCal.before(firstDayCal)) {
                val dayMillis = checkCal.timeInMillis
                if (!expenseDates.contains(dayMillis)) {
                    currentStreak++
                    checkCal.add(Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            }
            lastStreak = currentStreak
        } else {
            // Today has spend -> currentStreak = 0, compute lastStreak ending yesterday
            val checkCal = (todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -1)
            }
            while (!checkCal.before(firstDayCal)) {
                val dayMillis = checkCal.timeInMillis
                if (!expenseDates.contains(dayMillis)) {
                    lastStreak++
                    checkCal.add(Calendar.DAY_OF_MONTH, -1)
                } else {
                    break
                }
            }
        }

        // 2. Calculate best streak (all-time since firstDayCal up to today)
        var bestStreak = 0
        var runningStreak = 0
        val iterCal = firstDayCal.clone() as Calendar
        while (!iterCal.after(todayCal)) {
            val dayMillis = iterCal.timeInMillis
            if (!expenseDates.contains(dayMillis)) {
                runningStreak++
                if (runningStreak > bestStreak) {
                    bestStreak = runningStreak
                }
            } else {
                runningStreak = 0
            }
            iterCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // 3. Calculate no-spend days this month (from 1st of month or firstDayCal up to today)
        val startOfMonthCal = (todayCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val monthStartCal = if (firstDayCal.after(startOfMonthCal)) firstDayCal else startOfMonthCal
        var noSpendDaysThisMonth = 0

        val monthIterCal = monthStartCal.clone() as Calendar
        while (!monthIterCal.after(todayCal)) {
            val dayMillis = monthIterCal.timeInMillis
            if (!expenseDates.contains(dayMillis)) {
                noSpendDaysThisMonth++
            }
            monthIterCal.add(Calendar.DAY_OF_MONTH, 1)
        }

        // 4. Generate 7-day strip
        val last7Days = generateLast7Days(expenseDates, firstDayCal.timeInMillis, nowCalendar)

        return StreakState(
            currentStreak = currentStreak,
            lastStreak = lastStreak,
            bestStreak = bestStreak,
            noSpendDaysThisMonth = noSpendDaysThisMonth,
            currentMonthName = currentMonthName,
            last7Days = last7Days,
            isStreakActive = !todayHasSpend,
            isFirstUse = false
        )
    }

    private fun generateLast7Days(
        expenseDates: Set<Long>,
        firstDayMillis: Long?,
        nowCalendar: Calendar
    ): List<DayStreakStatus> {
        val list = mutableListOf<DayStreakStatus>()
        val dayNameFormat = SimpleDateFormat("EEEEE", Locale.getDefault()) // Single letter e.g. "M", "T"

        for (i in 6 downTo 0) {
            val cal = (nowCalendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayMillis = cal.timeInMillis
            val isToday = (i == 0)
            val isBeforeFirstDay = firstDayMillis == null || dayMillis < firstDayMillis
            val isNoSpend = !isBeforeFirstDay && !expenseDates.contains(dayMillis)

            // Day label
            val label = dayNameFormat.format(cal.time)

            list.add(
                DayStreakStatus(
                    dayLabel = label,
                    dateEpochDay = dayMillis / (24 * 60 * 60 * 1000L),
                    isNoSpend = isNoSpend,
                    isToday = isToday
                )
            )
        }
        return list
    }

    private fun getStartOfDayMillis(timestamp: Long): Long {
        return getStartOfDayCalendar(timestamp).timeInMillis
    }

    private fun getStartOfDayCalendar(timestamp: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
