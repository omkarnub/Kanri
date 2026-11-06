package com.omkarnub.kanri

import android.os.Bundle
import android.view.animation.PathInterpolator
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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

    @Volatile
    private var isAppReady = false

    private val smsPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val launchTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            !isAppReady && (System.currentTimeMillis() - launchTime < 1000L)
        }
        splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashView = splashScreenViewProvider.view
            val iconView = splashScreenViewProvider.iconView
            val easeOut = PathInterpolator(0.23f, 1f, 0.32f, 1f)

            // 1. Elegantly lift and scale the branding wordmark as it dissolves
            iconView?.animate()
                ?.scaleX(1.10f)
                ?.scaleY(1.10f)
                ?.translationY(-resources.displayMetrics.density * 16f)
                ?.alpha(0f)
                ?.setDuration(320L)
                ?.setInterpolator(easeOut)
                ?.start()

            // 2. Smoothly cross-fade the splash screen background
            splashView.animate()
                .alpha(0f)
                .setDuration(380L)
                .setInterpolator(easeOut)
                .withEndAction {
                    splashScreenViewProvider.remove()
                }
                .start()
        }

        val themePrefs = ThemePreferences.getInstance(applicationContext)
        val isLight = when (themePrefs.themeMode) {
            ThemeMode.LIGHT -> true
            ThemeMode.DARK, ThemeMode.AMOLED, ThemeMode.GLASS -> false
            ThemeMode.SYSTEM -> {
                val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        if (isLight) {
            setTheme(R.style.Theme_Kanri_Light)
        } else {
            setTheme(R.style.Theme_Kanri)
        }
        val backdropColor = when (themePrefs.themeMode) {
            ThemeMode.GLASS -> android.graphics.Color.parseColor("#0B0B0E")
            ThemeMode.AMOLED -> android.graphics.Color.BLACK
            ThemeMode.DARK -> android.graphics.Color.parseColor("#121212")
            ThemeMode.LIGHT -> android.graphics.Color.parseColor("#FEFAEC")
            ThemeMode.SYSTEM -> if (isLight) android.graphics.Color.parseColor("#FEFAEC") else android.graphics.Color.parseColor("#121212")
        }
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(backdropColor))
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        SecurityManager.init(applicationContext)
        ExportScheduler.init(applicationContext)
        com.omkarnub.kanri.data.lending.LendingReminderScheduler.init(applicationContext)
        com.omkarnub.kanri.util.AppShortcutManager.publishShortcuts(applicationContext)
        com.omkarnub.kanri.widget.KanriWidgetActions.handleIntent(intent)
        if (intent.getBooleanExtra(com.omkarnub.kanri.data.update.AppUpdateManager.EXTRA_SHOW_UPDATE_POPUP, false)) {
            com.omkarnub.kanri.data.update.AppUpdateManager.getInstance(applicationContext).triggerUpdatePopup()
        }

        val onboardingPrefs = com.omkarnub.kanri.ui.onboarding.OnboardingPreferences.getInstance(applicationContext)
        if (onboardingPrefs.hasCompletedOnboarding && !com.omkarnub.kanri.util.SmsPermissionHelper.hasSmsPermissions(this)) {
            smsPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.READ_SMS
                )
            )
        }
        if (onboardingPrefs.hasCompletedOnboarding) {
            com.omkarnub.kanri.util.BatteryOptimizationHelper.promptOnceIfAppropriate(this)
        }

        setContent {
            KanriTheme {
                var contentVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    isAppReady = true
                    contentVisible = true
                }

                val contentAlpha by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0.85f,
                    animationSpec = tween(
                        durationMillis = 380,
                        easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                    ),
                    label = "splashEntranceAlpha"
                )
                val contentScale by animateFloatAsState(
                    targetValue = if (contentVisible) 1f else 0.98f,
                    animationSpec = tween(
                        durationMillis = 380,
                        easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
                    ),
                    label = "splashEntranceScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        }
                ) {
                    com.omkarnub.kanri.ui.common.KanriBackdropHost {
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
        }
    }

    override fun onStart() {
        super.onStart()
        SecurityManager.onAppForegrounded()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        com.omkarnub.kanri.widget.KanriWidgetActions.handleIntent(intent)
        if (intent.getBooleanExtra(com.omkarnub.kanri.data.update.AppUpdateManager.EXTRA_SHOW_UPDATE_POPUP, false)) {
            com.omkarnub.kanri.data.update.AppUpdateManager.getInstance(applicationContext).triggerUpdatePopup()
        }
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