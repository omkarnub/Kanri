package com.omkarnub.kanri.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper to check and prompt once for Battery Optimization exemption (Doze mode).
 * Crucial for aggressive OEM skins (Xiaomi, Vivo, Oppo, OnePlus) that kill background
 * receivers and NotificationListenerService bindings during phone idle.
 */
object BatteryOptimizationHelper {
    private const val TAG = "BatteryOptimization"
    private const val PREFS_NAME = "kanri_battery_prefs"
    private const val KEY_PROMPTED_ONCE = "has_prompted_battery_opt_once"

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    fun hasPromptedOnce(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PROMPTED_ONCE, false)
    }

    fun setPromptedOnce(context: Context, prompted: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PROMPTED_ONCE, prompted).apply()
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct exemption request failed, opening battery optimization settings list", e)
            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Could not open battery settings", fallbackEx)
            }
        }
    }

    fun promptOnceIfAppropriate(activity: Activity) {
        if (!hasPromptedOnce(activity) && !isIgnoringBatteryOptimizations(activity)) {
            setPromptedOnce(activity, true)
            requestIgnoreBatteryOptimizations(activity)
        }
    }
}
