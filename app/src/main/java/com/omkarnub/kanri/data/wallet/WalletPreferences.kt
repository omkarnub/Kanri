package com.omkarnub.kanri.data.wallet

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WalletPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _atmWithdrawalModeFlow = MutableStateFlow(loadAtmWithdrawalMode())
    val atmWithdrawalModeFlow: StateFlow<AtmWithdrawalMode> = _atmWithdrawalModeFlow.asStateFlow()

    private val _isWalletSetupCompletedFlow = MutableStateFlow(loadIsWalletSetupCompleted())
    val isWalletSetupCompletedFlow: StateFlow<Boolean> = _isWalletSetupCompletedFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_wallet_prefs"
        private const val KEY_ATM_WITHDRAWAL_MODE = "key_atm_withdrawal_mode"
        private const val KEY_WALLET_SETUP_COMPLETED = "key_wallet_setup_completed"

        @Volatile
        private var INSTANCE: WalletPreferences? = null

        fun getInstance(context: Context): WalletPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WalletPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadAtmWithdrawalMode(): AtmWithdrawalMode {
        val raw = prefs.getString(KEY_ATM_WITHDRAWAL_MODE, AtmWithdrawalMode.SPENDING.name)
        return try {
            AtmWithdrawalMode.valueOf(raw ?: AtmWithdrawalMode.SPENDING.name)
        } catch (_: Exception) {
            AtmWithdrawalMode.SPENDING
        }
    }

    private fun loadIsWalletSetupCompleted(): Boolean {
        return prefs.getBoolean(KEY_WALLET_SETUP_COMPLETED, false)
    }

    var atmWithdrawalMode: AtmWithdrawalMode
        get() = loadAtmWithdrawalMode()
        set(value) {
            prefs.edit().putString(KEY_ATM_WITHDRAWAL_MODE, value.name).apply()
            _atmWithdrawalModeFlow.value = value
        }

    var isWalletSetupCompleted: Boolean
        get() = loadIsWalletSetupCompleted()
        set(value) {
            prefs.edit().putBoolean(KEY_WALLET_SETUP_COMPLETED, value).apply()
            _isWalletSetupCompletedFlow.value = value
        }

    fun reset() {
        prefs.edit().clear().apply()
        _atmWithdrawalModeFlow.value = AtmWithdrawalMode.SPENDING
        _isWalletSetupCompletedFlow.value = false
    }
}
