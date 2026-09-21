package com.omkarnub.kanri.data.budget

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object BudgetCalculator {

    const val DEFAULT_MONTHLY_BUDGET = 20000.0

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

    fun getCurrentMonthKey(calendar: Calendar = Calendar.getInstance()): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return "%d-%02d".format(year, month)
    }

    fun getCurrentMonthDisplayName(calendar: Calendar = Calendar.getInstance()): String {
        val formatter = SimpleDateFormat("MMMM", Locale.getDefault())
        return formatter.format(calendar.time)
    }

    fun getDaysRemainingInMonth(calendar: Calendar = Calendar.getInstance()): Int {
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val totalDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (totalDays - currentDay + 1).coerceAtLeast(1)
    }

    fun getStartOfMonthMillis(calendar: Calendar = Calendar.getInstance()): Long {
        val cal = (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
