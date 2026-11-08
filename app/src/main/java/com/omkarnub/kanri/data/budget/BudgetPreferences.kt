package com.omkarnub.kanri.data.budget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BudgetPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _budgetCycleStartDayFlow = MutableStateFlow(loadBudgetCycleStartDay())
    val budgetCycleStartDayFlow: StateFlow<Int> = _budgetCycleStartDayFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_budget_prefs"
        private const val KEY_CYCLE_START_DAY = "key_budget_cycle_start_day"
        const val DEFAULT_CYCLE_START_DAY = 1

        @Volatile
        private var INSTANCE: BudgetPreferences? = null

        fun getInstance(context: Context): BudgetPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BudgetPreferences(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private fun loadBudgetCycleStartDay(): Int {
        return prefs.getInt(KEY_CYCLE_START_DAY, DEFAULT_CYCLE_START_DAY).coerceIn(1, 31)
    }

    var budgetCycleStartDay: Int
        get() = loadBudgetCycleStartDay()
        set(value) {
            val clamped = value.coerceIn(1, 31)
            prefs.edit().putInt(KEY_CYCLE_START_DAY, clamped).apply()
            _budgetCycleStartDayFlow.value = clamped
        }
}

