package com.omkarnub.kanri.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hasCompletedFlow = MutableStateFlow(prefs.getBoolean(KEY_COMPLETED, false))
    val hasCompletedFlow: StateFlow<Boolean> = _hasCompletedFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_onboarding_prefs"
        private const val KEY_COMPLETED = "has_completed_onboarding"

        @Volatile
        private var INSTANCE: OnboardingPreferences? = null

        fun getInstance(context: Context): OnboardingPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OnboardingPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_COMPLETED, value).apply()
            _hasCompletedFlow.value = value
        }

    fun reset() {
        hasCompletedOnboarding = false
    }
}
