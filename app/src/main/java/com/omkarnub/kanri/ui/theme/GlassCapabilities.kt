package com.omkarnub.kanri.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

object GlassCapabilities {

    /**
     * Verifies if real-time GPU backdrop blur is supported and performant on this device:
     * 1. Android 12+ (API level >= 31)
     * 2. Device is not low RAM (ActivityManager.isLowRamDevice)
     * 3. Battery saver is not active
     * 4. User has not enabled "Reduce transparency"
     */
    fun isBlurSupported(context: Context, isReduceTransparency: Boolean = false): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false
        }
        if (isReduceTransparency) {
            return false
        }
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager?.isLowRamDevice == true) {
                return false
            }
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isPowerSaveMode == true) {
                return false
            }
        } catch (_: Exception) {
            // Fail safe
        }
        return true
    }

    fun isMotionAllowed(context: Context, isReduceTransparency: Boolean = false): Boolean {
        if (!isBlurSupported(context, isReduceTransparency)) return false
        return try {
            val scale = android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
            scale > 0f
        } catch (_: Exception) {
            true
        }
    }

    @Composable
    fun isBlurEnabled(): Boolean {
        val context = LocalContext.current
        val themePrefs = remember { ThemePreferences.getInstance(context) }
        val reduceTransparency by themePrefs.reduceTransparencyFlow.collectAsState()
        return remember(context, reduceTransparency) {
            isBlurSupported(context, reduceTransparency)
        }
    }

    @Composable
    fun isMotionEnabled(): Boolean {
        val context = LocalContext.current
        val themePrefs = remember { ThemePreferences.getInstance(context) }
        val reduceTransparency by themePrefs.reduceTransparencyFlow.collectAsState()
        return remember(context, reduceTransparency) {
            isMotionAllowed(context, reduceTransparency)
        }
    }
}
