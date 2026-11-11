package com.omkarnub.kanri.data.budget

import com.omkarnub.kanri.util.AppCurrency
import com.omkarnub.kanri.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

object BudgetCalculator {

    fun getDefaultMonthlyBudget(currency: AppCurrency = CurrencyUtils.getCurrency()): Double {
        return currency.defaultMonthlyBudget
    }

    val DEFAULT_MONTHLY_BUDGET: Double
        get() = getDefaultMonthlyBudget()

    fun calculateRemaining(budget: Double, spent: Double): Double {
        return budget - spent
    }

    fun calculateSafeToSpendPerDay(budget: Double, spent: Double, daysRemaining: Int): Double {
        val remaining = calculateRemaining(budget, spent)
        if (remaining <= 0.0) return 0.0
        val safeDays = daysRemaining.coerceAtLeast(1)
        return remaining / safeDays
    }

    fun calculatePercentUsed(budget: Double, spent: Double): Float {
        if (budget <= 0.0) return 1f
        return (spent / budget).toFloat().coerceIn(0f, 1f)
    }

    fun calculatePercentRemaining(budget: Double, spent: Double): Float {
        if (budget <= 0.0) return 0f
        return ((budget - spent) / budget).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Returns the start timestamp (in millis) for the active budget cycle.
     * If cycleStartDay == 1: 1st of the current calendar month at 00:00:00.000.
     * If cycleStartDay > 1 (e.g. 25):
     * - If today's day >= cycleStartDay: started on cycleStartDay of the current month.
     * - If today's day < cycleStartDay: started on cycleStartDay of the PREVIOUS month.
     */
    fun getStartOfCycleMillis(
        cycleStartDay: Int = 1,
        calendar: Calendar = Calendar.getInstance()
    ): Long {
        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxCurrentMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val effectiveStartDayCurrent = cycleStartDay.coerceIn(1, maxCurrentMonth)

        if (cycleStartDay <= 1) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            return cal.timeInMillis
        }

        if (currentDay >= effectiveStartDayCurrent) {
            cal.set(Calendar.DAY_OF_MONTH, effectiveStartDayCurrent)
        } else {
            // Started in previous month
            cal.add(Calendar.MONTH, -1)
            val maxPrevMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectiveStartDayPrev = cycleStartDay.coerceIn(1, maxPrevMonth)
            cal.set(Calendar.DAY_OF_MONTH, effectiveStartDayPrev)
        }
        return cal.timeInMillis
    }

    /**
     * Returns the end timestamp (in millis) for the active budget cycle.
     */
    fun getEndOfCycleMillis(
        cycleStartDay: Int = 1,
        calendar: Calendar = Calendar.getInstance()
    ): Long {
        val startMillis = getStartOfCycleMillis(cycleStartDay, calendar)
        val endCal = (calendar.clone() as Calendar).apply {
            timeInMillis = startMillis
            add(Calendar.MONTH, 1)
            val maxNextMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            val effectiveNextStartDay = cycleStartDay.coerceIn(1, maxNextMonth)
            set(Calendar.DAY_OF_MONTH, effectiveNextStartDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MILLISECOND, -1)
        }
        return endCal.timeInMillis
    }

    /**
     * Returns the number of days remaining until the active cycle ends (inclusive of today).
     */
    fun getDaysRemainingInCycle(
        cycleStartDay: Int = 1,
        calendar: Calendar = Calendar.getInstance()
    ): Int {
        if (cycleStartDay <= 1) {
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            return (totalDays - currentDay + 1).coerceAtLeast(1)
        }
        val todayCal = (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = (calendar.clone() as Calendar).apply {
            timeInMillis = getEndOfCycleMillis(cycleStartDay, calendar)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var days = 1
        while (todayCal.before(endCal)) {
            todayCal.add(Calendar.DAY_OF_MONTH, 1)
            days++
        }
        return days.coerceAtLeast(1)
    }

    /**
     * Returns the unique month/cycle key for Room BudgetEntity lookup.
     */
    fun getCurrentCycleKey(
        cycleStartDay: Int = 1,
        calendar: Calendar = Calendar.getInstance()
    ): String {
        if (cycleStartDay <= 1) {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            return String.format(Locale.US, "%d-%02d", year, month)
        }
        val startCal = (calendar.clone() as Calendar).apply {
            timeInMillis = getStartOfCycleMillis(cycleStartDay, calendar)
        }
        val year = startCal.get(Calendar.YEAR)
        val month = startCal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%d-%02d", year, month)
    }

    /**
     * Returns human-readable cycle display name (e.g. "November" or "25 Oct – 24 Nov").
     */
    fun getCurrentCycleDisplayName(
        cycleStartDay: Int = 1,
        calendar: Calendar = Calendar.getInstance()
    ): String {
        if (cycleStartDay <= 1) {
            val formatter = SimpleDateFormat("MMMM", Locale.getDefault())
            return formatter.format(calendar.time)
        }
        val startCal = (calendar.clone() as Calendar).apply {
            timeInMillis = getStartOfCycleMillis(cycleStartDay, calendar)
        }
        val endCal = (calendar.clone() as Calendar).apply {
            timeInMillis = getEndOfCycleMillis(cycleStartDay, calendar)
        }
        val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
        return "${fmt.format(startCal.time)} – ${fmt.format(endCal.time)}"
    }

    // Backward-compatible delegates
    fun getCurrentMonthKey(calendar: Calendar = Calendar.getInstance()): String =
        getCurrentCycleKey(1, calendar)

    fun getCurrentMonthDisplayName(calendar: Calendar = Calendar.getInstance()): String =
        getCurrentCycleDisplayName(1, calendar)

    fun getDaysRemainingInMonth(calendar: Calendar = Calendar.getInstance()): Int =
        getDaysRemainingInCycle(1, calendar)

    fun getStartOfMonthMillis(calendar: Calendar = Calendar.getInstance()): Long =
        getStartOfCycleMillis(1, calendar)
}
