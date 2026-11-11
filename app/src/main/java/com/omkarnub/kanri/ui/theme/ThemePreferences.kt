package com.omkarnub.kanri.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    AMOLED("AMOLED Dark Mode"),
    GLASS("Glass mode");

    val isGlass: Boolean
        get() = this == GLASS
}

class ThemePreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeModeFlow = MutableStateFlow(loadThemeMode())
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()

    private val _dynamicColorFlow = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, false))
    val dynamicColorFlow: StateFlow<Boolean> = _dynamicColorFlow.asStateFlow()

    private val _reduceTransparencyFlow = MutableStateFlow(prefs.getBoolean(KEY_REDUCE_TRANSPARENCY, false))
    val reduceTransparencyFlow: StateFlow<Boolean> = _reduceTransparencyFlow.asStateFlow()

    private val _appIconStyleFlow = MutableStateFlow(loadAppIconStyle())
    val appIconStyleFlow: StateFlow<AppIconStyle> = _appIconStyleFlow.asStateFlow()

    private val _glassBackgroundFlow = MutableStateFlow(loadGlassBackground())
    val glassBackgroundFlow: StateFlow<com.omkarnub.kanri.ui.common.GlassBackgroundType> = _glassBackgroundFlow.asStateFlow()

    private val _glassCustomizationFlow = MutableStateFlow(loadGlassCustomization())
    val glassCustomizationFlow: StateFlow<GlassCustomization> = _glassCustomizationFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_theme_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_ICON_STYLE = "key_app_icon_style"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_REDUCE_TRANSPARENCY = "key_reduce_transparency"
        private const val KEY_GLASS_BACKGROUND = "key_glass_background"

        private const val KEY_GLASS_BLUR = "key_glass_blur"
        private const val KEY_GLASS_OPACITY = "key_glass_opacity"
        private const val KEY_GLASS_TINT_LIGHTNESS = "key_glass_tint_lightness"
        private const val KEY_GLASS_BORDER_OPACITY = "key_glass_border_opacity"
        private const val KEY_GLASS_BORDER_WIDTH = "key_glass_border_width"
        private const val KEY_GLASS_CORNER_RADIUS = "key_glass_corner_radius"
        private const val KEY_GLASS_BUTTON_OPACITY = "key_glass_button_opacity"
        private const val KEY_GLASS_SHADOW = "key_glass_shadow"
        private const val KEY_GLASS_EXPENSE_COLOR = "key_glass_expense_color"
        private const val KEY_GLASS_INCOME_COLOR = "key_glass_income_color"
        private const val KEY_GLASS_EXPENSE_TINT_OPACITY = "key_glass_expense_tint_opacity"
        private const val KEY_GLASS_INCOME_TINT_OPACITY = "key_glass_income_tint_opacity"

        @Volatile
        private var INSTANCE: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemePreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadGlassCustomization(): GlassCustomization {
        return GlassCustomization(
            blurRadiusDp = prefs.getFloat(KEY_GLASS_BLUR, 24f),
            surfaceOpacity = prefs.getFloat(KEY_GLASS_OPACITY, 0.08f),
            tintLightness = prefs.getFloat(KEY_GLASS_TINT_LIGHTNESS, 0.0f),
            borderOpacity = prefs.getFloat(KEY_GLASS_BORDER_OPACITY, 0.28f),
            borderWidthDp = prefs.getFloat(KEY_GLASS_BORDER_WIDTH, 1.0f),
            cornerRadiusDp = prefs.getFloat(KEY_GLASS_CORNER_RADIUS, 20f),
            buttonOpacity = prefs.getFloat(KEY_GLASS_BUTTON_OPACITY, 0.16f),
            shadowIntensity = prefs.getFloat(KEY_GLASS_SHADOW, 0.25f),
            expenseColorHex = prefs.getLong(KEY_GLASS_EXPENSE_COLOR, 0xFFE54D2EL),
            incomeColorHex = prefs.getLong(KEY_GLASS_INCOME_COLOR, 0xFF30A46CL),
            expenseTintOpacity = prefs.getFloat(KEY_GLASS_EXPENSE_TINT_OPACITY, 0.18f),
            incomeTintOpacity = prefs.getFloat(KEY_GLASS_INCOME_TINT_OPACITY, 0.18f)
        )
    }

    private fun loadThemeMode(): ThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, ThemeMode.GLASS.name) ?: ThemeMode.GLASS.name
        return when (savedName) {
            "GLASS_DARK", "GLASS_LIGHT", "GLASS" -> ThemeMode.GLASS
            else -> try {
                ThemeMode.valueOf(savedName)
            } catch (e: Exception) {
                ThemeMode.GLASS
            }
        }
    }

    private fun loadGlassBackground(): com.omkarnub.kanri.ui.common.GlassBackgroundType {
        val savedName = prefs.getString(KEY_GLASS_BACKGROUND, com.omkarnub.kanri.ui.common.GlassBackgroundType.SUMI_E_MIST.name)
        return try {
            com.omkarnub.kanri.ui.common.GlassBackgroundType.valueOf(savedName ?: com.omkarnub.kanri.ui.common.GlassBackgroundType.SUMI_E_MIST.name)
        } catch (e: Exception) {
            com.omkarnub.kanri.ui.common.GlassBackgroundType.SUMI_E_MIST
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
        }

    var appIconStyle: AppIconStyle
        get() = loadAppIconStyle()
        set(value) {
            prefs.edit().putString(KEY_APP_ICON_STYLE, value.name).commit()
            _appIconStyleFlow.value = value
        }

    var glassBackground: com.omkarnub.kanri.ui.common.GlassBackgroundType
        get() = loadGlassBackground()
        set(value) {
            prefs.edit().putString(KEY_GLASS_BACKGROUND, value.name).apply()
            _glassBackgroundFlow.value = value
        }

    var isDynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
            _dynamicColorFlow.value = value
        }

    var isReduceTransparency: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_TRANSPARENCY, false)
        set(value) {
            prefs.edit().putBoolean(KEY_REDUCE_TRANSPARENCY, value).apply()
            _reduceTransparencyFlow.value = value
        }

    var glassCustomization: GlassCustomization
        get() = loadGlassCustomization()
        set(value) {
            prefs.edit()
                .putFloat(KEY_GLASS_BLUR, value.blurRadiusDp)
                .putFloat(KEY_GLASS_OPACITY, value.surfaceOpacity)
                .putFloat(KEY_GLASS_TINT_LIGHTNESS, value.tintLightness)
                .putFloat(KEY_GLASS_BORDER_OPACITY, value.borderOpacity)
                .putFloat(KEY_GLASS_BORDER_WIDTH, value.borderWidthDp)
                .putFloat(KEY_GLASS_CORNER_RADIUS, value.cornerRadiusDp)
                .putFloat(KEY_GLASS_BUTTON_OPACITY, value.buttonOpacity)
                .putFloat(KEY_GLASS_SHADOW, value.shadowIntensity)
                .putLong(KEY_GLASS_EXPENSE_COLOR, value.expenseColorHex)
                .putLong(KEY_GLASS_INCOME_COLOR, value.incomeColorHex)
                .putFloat(KEY_GLASS_EXPENSE_TINT_OPACITY, value.expenseTintOpacity)
                .putFloat(KEY_GLASS_INCOME_TINT_OPACITY, value.incomeTintOpacity)
                .apply()
            _glassCustomizationFlow.value = value
        }

    fun resetGlassCustomization() {
        glassCustomization = GlassPresets.DEFAULT
    }

    fun reset() {
        themeMode = ThemeMode.GLASS
        appIconStyle = AppIconStyle.DARK
        glassBackground = com.omkarnub.kanri.ui.common.GlassBackgroundType.SUMI_E_MIST
        isDynamicColor = false
        isReduceTransparency = false
        resetGlassCustomization()
    }
}
