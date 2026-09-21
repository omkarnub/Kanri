package com.omkarnub.kanri.data.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityManager {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var prefs: SecurityPreferences? = null

    fun init(context: Context) {
        val securityPrefs = SecurityPreferences.getInstance(context)
        prefs = securityPrefs
        if (securityPrefs.isLockEnabled) {
            _isAppLocked.value = true
        } else {
            _isAppLocked.value = false
        }
    }

    fun onAppBackgrounded() {
        // No background timer needed
    }

    var isTemporarilyExempt: Boolean = false

    fun onAppForegrounded() {
        if (isTemporarilyExempt) {
            isTemporarilyExempt = false
            return
        }

        val securityPrefs = prefs ?: return
        if (!securityPrefs.isLockEnabled) {
            _isAppLocked.value = false
            return
        }

        _isAppLocked.value = true
    }

    fun unlock() {
        _isAppLocked.value = false
    }

    fun lockImmediately() {
        val securityPrefs = prefs
        if (securityPrefs != null && securityPrefs.isLockEnabled) {
            _isAppLocked.value = true
        }
    }

    fun setLockedState(locked: Boolean) {
        _isAppLocked.value = locked
    }
}
