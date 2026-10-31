package com.omkarnub.kanri.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrencyPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currencyFlow = MutableStateFlow(loadCurrency())
    val currencyFlow: StateFlow<AppCurrency> = _currencyFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_currency_prefs"
        private const val KEY_CURRENCY = "key_app_currency"

        @Volatile
        private var INSTANCE: CurrencyPreferences? = null

        fun getInstance(context: Context): CurrencyPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrencyPreferences(context.applicationContext).also {
                    INSTANCE = it
                    // Ensure CurrencyUtils is synced on first access
                    CurrencyUtils.setCurrency(it.currency)
                }
            }
        }
    }

    private fun loadCurrency(): AppCurrency {
        val code = prefs.getString(KEY_CURRENCY, AppCurrency.INR.code)
        return AppCurrency.fromCode(code)
    }

    var currency: AppCurrency
        get() = loadCurrency()
        set(value) {
            prefs.edit().putString(KEY_CURRENCY, value.code).apply()
            _currencyFlow.value = value
            CurrencyUtils.setCurrency(value)
        }
}
