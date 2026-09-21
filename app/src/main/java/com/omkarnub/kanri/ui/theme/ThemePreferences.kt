package com.omkarnub.kanri.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    AMOLED("AMOLED Dark Mode"),
    LIGHT("Light")
}

class ThemePreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(loadThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    private val _dynamicColorFlow = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColorFlow: StateFlow<Boolean> = _dynamicColorFlow.asStateFlow()

    private val _appIconStyleFlow = MutableStateFlow(loadAppIconStyle())
    val appIconStyleFlow: StateFlow<AppIconStyle> = _appIconStyleFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_theme_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_ICON_STYLE = "key_app_icon_style"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"

        @Volatile
        private var INSTANCE: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, ThemeMode.DARK.name) ?: ThemeMode.DARK.name
        return try {
            ThemeMode.valueOf(savedName)
        } catch (e: Exception) {
            ThemeMode.DARK
        }
    }

    private fun loadAppIconStyle(): AppIconStyle {
        val saved = prefs.getString(KEY_APP_ICON_STYLE, null)
        if (saved != null) {
            try {
                return AppIconStyle.valueOf(saved)
            } catch (e: Exception) {}
        }
        return AppIconManager.getCurrentIconStyle(context)
    }

    var themeMode: ThemeMode
        get() = loadThemeMode()
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
            _themeModeFlow.value = value
            try {
                val targetClass = AppIconManager.getTargetActivityClass(appIconStyle, value)
                context.packageManager.setComponentEnabledSetting(
                    android.content.ComponentName(context, targetClass),
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {}
        }

    var appIconStyle: AppIconStyle
        get() = loadAppIconStyle()
        set(value) {
            prefs.edit().putString(KEY_APP_ICON_STYLE, value.name).commit()
            _appIconStyleFlow.value = value
        }

    var isDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
            _dynamicColorFlow.value = value
        }

    fun reset() {
        themeMode = ThemeMode.DARK
        appIconStyle = AppIconStyle.DARK
        isDynamicColor = false
    }
}
