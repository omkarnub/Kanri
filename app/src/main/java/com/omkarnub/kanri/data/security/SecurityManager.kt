package com.omkarnub.kanri.data.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityManager {

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private var prefs: SecurityPreferences? = null
    private var isUnlockedInCurrentSession: Boolean = false
    private var isReceiverRegistered: Boolean = false

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // When physical device screen turns off or locks, require authentication next time
                val securityPrefs = prefs
                if (securityPrefs != null && securityPrefs.isLockEnabled) {
                    isUnlockedInCurrentSession = false
                    _isAppLocked.value = true
                }
            }
        }
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        val securityPrefs = SecurityPreferences.getInstance(appContext)
        prefs = securityPrefs
        if (securityPrefs.isLockEnabled) {
            _isAppLocked.value = !isUnlockedInCurrentSession
        } else {
            _isAppLocked.value = false
        }

        if (!isReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
            appContext.registerReceiver(screenOffReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun onAppBackgrounded() {
        // Do NOT lock immediately on app switch/background
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

        // Only lock if device screen was locked/turned off or initial app launch
        if (!isUnlockedInCurrentSession) {
            _isAppLocked.value = true
        }
    }

    fun unlock() {
        isUnlockedInCurrentSession = true
        _isAppLocked.value = false
    }

    fun lockImmediately() {
        val securityPrefs = prefs
        if (securityPrefs != null && securityPrefs.isLockEnabled) {
            isUnlockedInCurrentSession = false
            _isAppLocked.value = true
        }
    }

    fun setLockedState(locked: Boolean) {
        if (!locked) {
            isUnlockedInCurrentSession = true
        } else {
            isUnlockedInCurrentSession = false
        }
        _isAppLocked.value = locked
    }
}
