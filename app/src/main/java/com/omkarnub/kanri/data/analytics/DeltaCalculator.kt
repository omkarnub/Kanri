package com.omkarnub.kanri.data.analytics

import androidx.compose.runtime.Immutable
import com.omkarnub.kanri.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
sealed interface Delta {
    @Immutable
    data class Up(val percent: Int, val absolute: Double, val prevComparisonDesc: String) : Delta

    @Immutable
    data class Down(val percent: Int, val absolute: Double, val prevComparisonDesc: String) : Delta

    @Immutable
    data class Flat(val absolute: Double, val prevComparisonDesc: String) : Delta

    @Immutable
    data object NoData : Delta
}

object DeltaCalculator {

    /**
     * Calculates Month-Over-Month expense delta.
     *
     * @param selectedYear Selected month's calendar year (e.g. 2026)
     * @param selectedMonth Selected month's 1-based index (1 = Jan, 12 = Dec)
     * @param transactions All transactions from database
     * @param nowCalendar Current reference time (defaults to Calendar.getInstance())
     */
    fun calculateDelta(
        selectedYear: Int,
        selectedMonth: Int, // 1-based (1..12)
        transactions: List<TransactionEntity>,
        nowCalendar: Calendar = Calendar.getInstance()
    ): Delta {
        // Expenses only: filter type == "DEBIT"
        val expenseTransactions = transactions.filter {
            it.type.equals("DEBIT", ignoreCase = true)
        }

        val currentYear = nowCalendar.get(Calendar.YEAR)
        val currentMonth = nowCalendar.get(Calendar.MONTH) + 1 // 1-based
        val isCurrentMonth = (selectedYear == currentYear && selectedMonth == currentMonth)

        // Previous month (M-1)
        val prevMonthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1) // 0-based
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        val prevMonth = prevMonthCal.get(Calendar.MONTH) + 1
        val prevMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(prevMonthCal.time)

        // Define start/end millis for current selected month M
        val startMCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endMCal = (startMCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        // Define start/end millis for previous month M-1
        val startPrevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, prevYear)
            set(Calendar.MONTH, prevMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endPrevCal: Calendar
        val prevComparisonDesc: String

        if (isCurrentMonth) {
            val currentDayOfMonth = nowCalendar.get(Calendar.DAY_OF_MONTH)
            val maxDayInPrevMonth = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val clampedDay = currentDayOfMonth.coerceAtMost(maxDayInPrevMonth)

            endPrevCal = (startPrevCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, clampedDay)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            prevComparisonDesc = if (clampedDay == 1) {
                "$prevMonthName 1"
            } else {
                "$prevMonthName 1–$clampedDay"
            }
        } else {
            // Past month: full month vs full month
            endPrevCal = (startPrevCal.clone() as Calendar).apply {
                add(Calendar.MONTH, 1)
            }
            prevComparisonDesc = prevMonthName
        }

        val curExpenses = expenseTransactions.filter {
            it.timestamp >= startMCal.timeInMillis && it.timestamp < endMCal.timeInMillis
        }.sumOf { it.amount }

        val prevExpenses = expenseTransactions.filter {
            it.timestamp >= startPrevCal.timeInMillis && it.timestamp <= endPrevCal.timeInMillis
        }.sumOf { it.amount }

        // Guard divide-by-zero
        if (prevExpenses <= 0.0) {
            return Delta.NoData
        }

        val diff = curExpenses - prevExpenses
        val rawPercent = (diff / prevExpenses) * 100.0
        val percent = abs(rawPercent).roundToInt()

        return when {
            abs(rawPercent) < 1.0 -> Delta.Flat(abs(diff), prevComparisonDesc)
            curExpenses > prevExpenses -> Delta.Up(percent, abs(diff), prevComparisonDesc)
            else -> Delta.Down(percent, abs(diff), prevComparisonDesc)
        }
    }
}
