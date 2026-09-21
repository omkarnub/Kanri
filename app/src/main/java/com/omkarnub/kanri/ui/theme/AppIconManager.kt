package com.omkarnub.kanri.ui.theme

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.omkarnub.kanri.MainActivity
import com.omkarnub.kanri.MainActivityDarkDark
import com.omkarnub.kanri.MainActivityDarkLight
import com.omkarnub.kanri.MainActivityLightDark
import com.omkarnub.kanri.MainActivityLightLight

enum class AppIconStyle(val label: String) {
    DARK("Dark"),
    LIGHT("Light")
}

object AppIconManager {

    fun getTargetActivityClass(iconStyle: AppIconStyle, themeMode: ThemeMode): Class<out MainActivity> {
        return when (iconStyle) {
            AppIconStyle.DARK -> when (themeMode) {
                ThemeMode.DARK, ThemeMode.AMOLED -> MainActivityDarkDark::class.java
                ThemeMode.LIGHT -> MainActivityDarkLight::class.java
            }
            AppIconStyle.LIGHT -> when (themeMode) {
                ThemeMode.DARK, ThemeMode.AMOLED -> MainActivityLightDark::class.java
                ThemeMode.LIGHT -> MainActivityLightLight::class.java
            }
        }
    }

    fun syncLauncherComponents(context: Context, iconStyle: AppIconStyle, themeMode: ThemeMode) {
        val pm = context.packageManager
        val targetClass = getTargetActivityClass(iconStyle, themeMode)
        val targetComp = ComponentName(context, targetClass)

        // Enable target component first
        if (pm.getComponentEnabledSetting(targetComp) != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            pm.setComponentEnabledSetting(
                targetComp,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Disable other launcher components
        val allTargetClasses = listOf(
            MainActivityDarkDark::class.java,
            MainActivityDarkLight::class.java,
            MainActivityLightDark::class.java,
            MainActivityLightLight::class.java
        )

        for (cls in allTargetClasses) {
            if (cls != targetClass) {
                val comp = ComponentName(context, cls)
                if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }
        }

        // Clean up legacy components if present
        val legacy = listOf(
            "com.omkarnub.kanri.MainActivity",
            "com.omkarnub.kanri.MainActivityLight"
        )
        for (className in legacy) {
            try {
                val comp = ComponentName(context, className)
                if (pm.getComponentEnabledSetting(comp) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (ignored: Exception) {}
        }
    }

    fun getCurrentIconStyle(context: Context): AppIconStyle {
        val pm = context.packageManager
        val lightDarkComp = ComponentName(context, MainActivityLightDark::class.java)
        val lightLightComp = ComponentName(context, MainActivityLightLight::class.java)
        val legacyLightComp = ComponentName(context, "com.omkarnub.kanri.MainActivityLight")

        val isLight = pm.getComponentEnabledSetting(lightDarkComp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                pm.getComponentEnabledSetting(lightLightComp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                pm.getComponentEnabledSetting(legacyLightComp) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        return if (isLight) AppIconStyle.LIGHT else AppIconStyle.DARK
    }

    fun restartApp(context: Context, targetStyle: AppIconStyle, currentTheme: ThemeMode) {
        val pm = context.packageManager
        val targetClass = getTargetActivityClass(targetStyle, currentTheme)
        val targetComp = ComponentName(context, targetClass)

        // 1. Enable target component first
        pm.setComponentEnabledSetting(
            targetComp,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 2. Launch target activity
        val restartIntent = Intent(context, targetClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(restartIntent)

        // 3. Disable all other launcher components
        val allTargetClasses = listOf(
            MainActivityDarkDark::class.java,
            MainActivityDarkLight::class.java,
            MainActivityLightDark::class.java,
            MainActivityLightLight::class.java
        )

        for (cls in allTargetClasses) {
            if (cls != targetClass) {
                val comp = ComponentName(context, cls)
                try {
                    pm.setComponentEnabledSetting(
                        comp,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                } catch (ignored: Exception) {}
            }
        }

        val legacy = listOf(
            "com.omkarnub.kanri.MainActivity",
            "com.omkarnub.kanri.MainActivityLight"
        )
        for (className in legacy) {
            try {
                val comp = ComponentName(context, className)
                pm.setComponentEnabledSetting(
                    comp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (ignored: Exception) {}
        }

        if (context is Activity) {
            context.finishAndRemoveTask()
        }
    }
}
