package com.omkarnub.kanri.widget

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Three distinct widget styles supported across Kanri:
 * 1. [IPHONE] - Apple HIG aesthetic: squircle continuous curves, Cupertino vibrancy, fluid spring animations.
 * 2. [GLASS_100X] - Apple Liquid Glass 100x: high-specular rim reflections, ambient glowing backlights, frosted glass.
 * 3. [PIXEL_MATRIX] - Louis Studios DotTiles: 7-row dot-matrix numerals, animated counting dots, live jitter, tick sliders.
 */
enum class WidgetStyle(
    val key: String,
    val displayName: String,
    val tag: String,
    val subtitle: String
) {
    IPHONE(
        key = "iphone",
        displayName = "iPhone Fluid",
        tag = " iOS HIG",
        subtitle = "Continuous squircles, Cupertino vibrancy, activity rings & fluid spring physics"
    ),
    GLASS_100X(
        key = "glass_100x",
        displayName = "100x Liquid Glass",
        tag = "✦ Liquid Glass",
        subtitle = "Specular top-rim shine, ambient neon glow, shimmering reflections & frosted depth"
    ),
    PIXEL_MATRIX(
        key = "pixel_matrix",
        displayName = "Pixel DotTiles",
        tag = "⠶ Louis Studios",
        subtitle = "7-row dot-matrix numerals, live jitter drift, tick sliders & step histograms"
    );

    companion object {
        fun fromKey(key: String?): WidgetStyle {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: GLASS_100X
        }
    }
}

/**
 * Persistent store for home screen widget styling and preview configurations.
 */
class WidgetPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _widgetStyleFlow = MutableStateFlow(loadWidgetStyle())
    val widgetStyleFlow: StateFlow<WidgetStyle> = _widgetStyleFlow.asStateFlow()

    var widgetStyle: WidgetStyle
        get() = _widgetStyleFlow.value
        set(value) {
            prefs.edit().putString(KEY_WIDGET_STYLE, value.key).apply()
            _widgetStyleFlow.value = value
        }

    private fun loadWidgetStyle(): WidgetStyle {
        val raw = prefs.getString(KEY_WIDGET_STYLE, WidgetStyle.GLASS_100X.key)
        return WidgetStyle.fromKey(raw)
    }

    companion object {
        private const val PREFS_NAME = "kanri_widget_preferences"
        private const val KEY_WIDGET_STYLE = "key_active_widget_style"

        @Volatile
        private var INSTANCE: WidgetPreferences? = null

        fun getInstance(context: Context): WidgetPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

