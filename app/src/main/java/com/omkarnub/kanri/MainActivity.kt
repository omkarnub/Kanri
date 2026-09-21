package com.omkarnub.kanri

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.omkarnub.kanri.data.export.ExportScheduler
import com.omkarnub.kanri.data.security.SecurityManager
import com.omkarnub.kanri.ui.navigation.MainNavigationScreen
import com.omkarnub.kanri.ui.onboarding.OnboardingPreferences
import com.omkarnub.kanri.ui.onboarding.OnboardingScreen
import com.omkarnub.kanri.ui.security.LockScreen
import com.omkarnub.kanri.ui.theme.AppIconManager
import com.omkarnub.kanri.ui.theme.AppIconStyle
import com.omkarnub.kanri.ui.theme.KanriTheme
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences

open class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val themePrefs = ThemePreferences.getInstance(applicationContext)
        if (themePrefs.themeMode == ThemeMode.LIGHT) {
            setTheme(R.style.Theme_Kanri_Light)
        } else {
            setTheme(R.style.Theme_Kanri)
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SecurityManager.init(applicationContext)
        ExportScheduler.init(applicationContext)

        setContent {
            KanriTheme {
                val onboardingPrefs = remember { OnboardingPreferences.getInstance(applicationContext) }
                val hasCompletedOnboarding by onboardingPrefs.hasCompletedFlow.collectAsState()
                var isTestingLock by remember { mutableStateOf(false) }

                val isLocked by SecurityManager.isAppLocked.collectAsState()

                when {
                    !hasCompletedOnboarding -> {
                        OnboardingScreen(
                            onComplete = {
                                onboardingPrefs.hasCompletedOnboarding = true
                            }
                        )
                    }
                    isLocked || isTestingLock -> {
                        LockScreen(
                            onUnlocked = {
                                SecurityManager.unlock()
                                isTestingLock = false
                            }
                        )
                    }
                    else -> {
                        MainNavigationScreen(
                            onReplayOnboarding = {
                                onboardingPrefs.hasCompletedOnboarding = false
                            },
                            onTestLockScreen = {
                                isTestingLock = true
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SecurityManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        SecurityManager.onAppBackgrounded()
        val themePrefs = ThemePreferences.getInstance(applicationContext)
        AppIconManager.syncLauncherComponents(applicationContext, themePrefs.appIconStyle, themePrefs.themeMode)
    }
}

/** 1. Dark Icon + Dark Splash (Dark Theme) */
class MainActivityDarkDark : MainActivity()

/** 2. Dark Icon + Light Splash (Light Theme) */
class MainActivityDarkLight : MainActivity()

/** 3. Light Icon + Dark Splash (Dark Theme) */
class MainActivityLightDark : MainActivity()

/** 4. Light Icon + Light Splash (Light Theme) */
class MainActivityLightLight : MainActivity()