package com.omkarnub.kanri.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.cloud.BackupMode
import com.omkarnub.kanri.data.cloud.CloudBackupPreferences
import com.omkarnub.kanri.data.cloud.CloudBackupWorker
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import com.omkarnub.kanri.data.security.SecurityPreferences
import com.omkarnub.kanri.ui.common.kokonut.KokonutAvatarPicker
import com.omkarnub.kanri.ui.common.stepper.StepItem
import com.omkarnub.kanri.ui.common.stepper.Stepper
import com.omkarnub.kanri.ui.common.stepper.StepperSize
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.ui.popup.OverlayPermissionHelper
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.MonochromeBlack
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.BatteryOptimizationHelper
import com.omkarnub.kanri.util.SmsPermissionHelper
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two-way JavaScript bridge coordinating the living scroll canvas with Compose.
 */
private class KanriOnboardingBridge(
    private val onPaintingFinishedAction: (Int) -> Unit,
    private val onGlideFinishedAction: (Int) -> Unit
) {
    @JavascriptInterface
    fun onPaintingFinished(step: Int) {
        onPaintingFinishedAction(step)
    }

    @JavascriptInterface
    fun onGlideFinished(step: Int) {
        onGlideFinishedAction(step)
    }
}

/**
 * Clean, rebuilt OnboardingScreen from scratch.
 *
 * Sequence:
 * 1. Screen fade-in revealing Shan Shui living scroll paper canvas.
 * 2. Background paints Scene 0 ink washes (3800ms) with foreground UI hidden.
 * 3. On paint complete (via bridge callback or safety timeout), 100% ChapterPlayer
 *    intro animation runs (emblem landing, kinetic KANRI letters, subtitle).
 * 4. At lt >= 2.95s, "Get Started" button fades in.
 * 5. On click, logo and button smoothly fade out first (320ms exit).
 * 6. Background scroll glides to Scene 2.
 * 7. Avatar picker card enters smoothly.
 * 8. Steps 1-7 follow identical rhythm: card fade-out -> background glide -> next card fade-in.
 */
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val coroutineScope = rememberCoroutineScope()

    val cloudBackupPrefs = remember { CloudBackupPreferences.getInstance(context) }
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val themeMode by themePrefs.themeModeFlow.collectAsState()
    val isLight = themeMode == ThemeMode.LIGHT

    // Reduce motion check
    val reduceMotion = remember {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    // Step state: 0 = Hero, 1 = Profile, 2 = SMS, 3 = UPI, 4 = Overlay, 5 = Battery, 6 = Alerts, 7 = Lock
    var currentSlide by remember { mutableIntStateOf(0) }
    val totalSlides = 8

    // App launch screen fade-in
    val screenAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        screenAlpha.animateTo(1f, tween(400, easing = LinearEasing))
    }

    // Painting & Bridge state
    var isScene0PaintingDone by remember { mutableStateOf(false) }
    var isIntroReadyForButton by remember { mutableStateOf(false) }
    var isHeroExiting by remember { mutableStateOf(false) }
    var isStepCardVisible by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Fallback safety timeout for Scene 0 painting (4000ms)
    LaunchedEffect(Unit) {
        delay(4000L)
        if (!isScene0PaintingDone) {
            isScene0PaintingDone = true
        }
    }

    // Permission states
    var isLockConfigured by remember { mutableStateOf(securityPrefs.isLockEnabled) }
    var hasSmsPermission by remember { mutableStateOf(SmsPermissionHelper.hasSmsPermissions(context)) }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasSmsPermission = SmsPermissionHelper.hasSmsPermissions(context)
    }

    var hasNotificationAccess by remember {
        mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }
    var hasBatteryExemption by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    val postNotificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    // Lifecycle observer to re-check permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSmsPermission = SmsPermissionHelper.hasSmsPermissions(context)
                hasNotificationAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
                hasOverlayPermission = OverlayPermissionHelper.canDrawOverlays(context)
                hasBatteryExemption = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                hasNotificationPermission = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun finishOnboarding() {
        haptics.click()
        OnboardingPreferences.getInstance(context).hasCompletedOnboarding = true
        cloudBackupPrefs.backupMode = BackupMode.OFFLINE
        CloudBackupWorker.cancel(context)
        onComplete()
    }

    // Stepper items for steps 1-7
    val onboardingSteps = remember {
        listOf(
            StepItem(id = "profile", label = "Profile", icon = Icons.Default.Person),
            StepItem(id = "sms", label = "SMS", icon = Icons.Default.Sms),
            StepItem(id = "notifications", label = "Apps", icon = Icons.Default.AccountBalanceWallet),
            StepItem(id = "overlay", label = "Overlay", icon = Icons.Default.Bolt),
            StepItem(id = "battery", label = "Battery", icon = Icons.Default.PowerSettingsNew),
            StepItem(id = "alerts", label = "Alerts", icon = Icons.Default.Notifications),
            StepItem(id = "lock", label = "Lock", icon = Icons.Default.Fingerprint)
        )
    }

    // Transition functions maintaining the sequential choreography
    fun advanceToStep(targetStep: Int) {
        haptics.click()
        coroutineScope.launch {
            isStepCardVisible = false
            delay(280L)
            currentSlide = targetStep
            webViewInstance?.evaluateJavascript("window.setOnboardingStep($targetStep);", null)
            delay(350L)
            isStepCardVisible = true
        }
    }

    fun stepBack() {
        haptics.click()
        coroutineScope.launch {
            isStepCardVisible = false
            delay(280L)
            if (currentSlide <= 1) {
                currentSlide = 0
                isHeroExiting = false
                webViewInstance?.evaluateJavascript("window.setOnboardingStep(0);", null)
            } else {
                val prev = currentSlide - 1
                currentSlide = prev
                webViewInstance?.evaluateJavascript("window.setOnboardingStep($prev);", null)
                delay(350L)
                isStepCardVisible = true
            }
        }
    }

    // Update WebView theme dynamically
    LaunchedEffect(isLight, webViewInstance) {
        val paperTheme = if (isLight) "light" else "dark"
        webViewInstance?.evaluateJavascript("if (window.setPaperTheme) window.setPaperTheme('$paperTheme');", null)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
    ) {
        // Layer 0: Continuous 8-Step Panoramic Shan Shui Living Scroll Canvas
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        loadWithOverviewMode = true
                        useWideViewPort = true
                    }
                    val bridge = KanriOnboardingBridge(
                        onPaintingFinishedAction = { step ->
                            if (step == 0) {
                                coroutineScope.launch {
                                    isScene0PaintingDone = true
                                }
                            }
                        },
                        onGlideFinishedAction = {
                            // Glide finished
                        }
                    )
                    addJavascriptInterface(bridge, "AndroidBridge")

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                            android.util.Log.d("ShanShuiWebView", "${consoleMessage?.message()} [line ${consoleMessage?.lineNumber()}]")
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("ShanShuiWebView", "WebView error: ${error?.description} (${error?.errorCode}) url: ${request?.url}")
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val paperTheme = if (isLight) "light" else "dark"
                            view?.evaluateJavascript(
                                "if (window.setPaperTheme) window.setPaperTheme('$paperTheme'); if (window.setOnboardingStep) window.setOnboardingStep($currentSlide, true);",
                                null
                            )
                        }
                    }
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    isClickable = false
                    isFocusable = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    setOnTouchListener { _, _ -> false }
                    loadUrl("file:///android_asset/backgrounds/shan_shui_onboarding.html")
                }
            },
            update = { webView ->
                webViewInstance = webView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Layer 1: Foreground UI Choreography
        if (currentSlide == 0) {
            // STEP 0: HERO (100% ChapterPlayer Intro Animation + Get Started Button)
            HeroSlideView(
                isLight = isLight,
                isPaintingDone = isScene0PaintingDone,
                isReadyForButton = isIntroReadyForButton,
                isExiting = isHeroExiting,
                reduceMotion = reduceMotion,
                onIntroReadyForButton = { isIntroReadyForButton = true },
                onGetStarted = {
                    haptics.click()
                    coroutineScope.launch {
                        // 1. First fade out logo, emblem and button cleanly
                        isHeroExiting = true
                        delay(320L)

                        // 2. Only then progress background scroll
                        currentSlide = 1
                        webViewInstance?.evaluateJavascript("window.setOnboardingStep(1);", null)

                        // 3. Fade in Step 1 Avatar Picker card
                        delay(380L)
                        isStepCardVisible = true
                    }
                }
            )
        } else {
            // STEPS 1 TO 7: SLIDER WORKFLOW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .displayCutoutPadding()
            ) {
                // Top Stepper Navigation Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { stepBack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Stepper(
                        steps = onboardingSteps,
                        activeStep = currentSlide - 1,
                        onStepClick = { stepIndex ->
                            advanceToStep(stepIndex + 1)
                        },
                        size = StepperSize.SM,
                        showLabels = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Step Counter & Title
                val currentStepItem = onboardingSteps.getOrNull(currentSlide - 1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STEP $currentSlide OF ${onboardingSteps.size}",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    if (currentStepItem?.label != null) {
                        Text(
                            text = currentStepItem.label.uppercase(),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Main Step Content Card with Smooth Fade & Scale Transition
                val cardAlpha by animateFloatAsState(
                    targetValue = if (isStepCardVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "card_alpha"
                )
                val cardScale by animateFloatAsState(
                    targetValue = if (isStepCardVisible) 1f else 0.96f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "card_scale"
                )
                val cardOffsetY by animateFloatAsState(
                    targetValue = if (isStepCardVisible) 0f else 16f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "card_offset_y"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = cardAlpha
                            scaleX = cardScale
                            scaleY = cardScale
                            translationY = cardOffsetY * density
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when (currentSlide) {
                        1 -> ProfileSetupStepView(
                            onContinue = { advanceToStep(2) }
                        )

                        2 -> PermissionCardView(
                            icon = Icons.Default.Sms,
                            title = "Bank SMS Detection",
                            description = "Intercepts bank debit & credit alerts 100% offline. The only reliable source for sent payments (UPI, ATM, cards).",
                            isGranted = hasSmsPermission,
                            grantLabel = "Enable SMS Detection",
                            grantedLabel = "SMS Detection Active",
                            onGrant = {
                                smsLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.RECEIVE_SMS,
                                        Manifest.permission.READ_SMS
                                    )
                                )
                            },
                            onNext = { advanceToStep(3) },
                            onSkip = { advanceToStep(3) }
                        )

                        3 -> PermissionCardView(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Payment Apps Listener",
                            description = "Catches incoming money notifications and scratch cards from Google Pay, PhonePe, Paytm, and CRED.",
                            isGranted = hasNotificationAccess,
                            grantLabel = "Grant Access",
                            grantedLabel = "Access Granted",
                            onGrant = {
                                NotificationAccessHelper.openNotificationAccessSettings(context)
                            },
                            onNext = { advanceToStep(4) },
                            onSkip = { advanceToStep(4) }
                        )

                        4 -> PermissionCardView(
                            icon = Icons.Default.Bolt,
                            title = "Instant Overlay",
                            description = "Display a lightweight popup right after you pay, so you can review and categorize payments the moment they happen.",
                            isGranted = hasOverlayPermission,
                            grantLabel = "Enable Overlay",
                            grantedLabel = "Overlay Enabled",
                            onGrant = {
                                OverlayPermissionHelper.requestOverlayPermission(context)
                            },
                            onNext = { advanceToStep(5) },
                            onSkip = { advanceToStep(5) }
                        )

                        5 -> PermissionCardView(
                            icon = Icons.Default.PowerSettingsNew,
                            title = "Background Endurance",
                            description = "Keep Kanri running unrestricted in the background so Android never puts your expense trackers to sleep.",
                            isGranted = hasBatteryExemption,
                            grantLabel = "Allow Background Run",
                            grantedLabel = "Background Run Active",
                            onGrant = {
                                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                            },
                            onNext = { advanceToStep(6) },
                            onSkip = { advanceToStep(6) }
                        )

                        6 -> PermissionCardView(
                            icon = Icons.Default.Notifications,
                            title = "System Alerts",
                            description = "Receive mindful spend nudges, daily summaries, and backup status notifications right on your phone.",
                            isGranted = hasNotificationPermission,
                            grantLabel = "Enable Notifications",
                            grantedLabel = "Notifications Active",
                            onGrant = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onNext = { advanceToStep(7) },
                            onSkip = { advanceToStep(7) }
                        )

                        7 -> PermissionCardView(
                            icon = Icons.Default.Fingerprint,
                            title = "Biometric Security",
                            description = "Require Fingerprint, Face Unlock, or Device Lock every time you open your financial scroll.",
                            isGranted = isLockConfigured,
                            grantLabel = "Enable App Lock",
                            grantedLabel = "App Lock Enabled",
                            onGrant = {
                                val newState = !isLockConfigured
                                isLockConfigured = newState
                                securityPrefs.isLockEnabled = newState
                            },
                            onNext = { finishOnboarding() },
                            onSkip = { finishOnboarding() },
                            isLastSlide = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 0: Hero with 100% ChapterPlayer Intro Logo + Synchronized Get Started Button.
 */
@Composable
private fun HeroSlideView(
    isLight: Boolean,
    isPaintingDone: Boolean,
    isReadyForButton: Boolean,
    isExiting: Boolean,
    reduceMotion: Boolean,
    onIntroReadyForButton: () -> Unit,
    onGetStarted: () -> Unit
) {
    val haptics = rememberKanriHaptics()
    var showPrivacySheet by remember { mutableStateOf(false) }

    val buttonAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val buttonOffsetY = remember { Animatable(if (reduceMotion) 0f else 18f) }

    LaunchedEffect(isReadyForButton, reduceMotion) {
        if (isReadyForButton) {
            launch {
                buttonAlpha.animateTo(1f, tween(400, easing = FastOutSlowInEasing))
            }
            launch {
                buttonOffsetY.animateTo(0f, tween(400, easing = FastOutSlowInEasing))
            }
        }
    }

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "hero_btn_press"
    )

    LaunchedEffect(isButtonPressed) {
        if (isButtonPressed) haptics.tick()
    }

    val buttonBgColor = if (isLight) MonochromeBlack else DarkOffWhite
    val buttonContentColor = if (isLight) DarkOffWhite else MonochromeBlack

    val exitAlpha = if (isExiting) 0f else 1f
    val exitAlphaAnimated by animateFloatAsState(
        targetValue = exitAlpha,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "hero_exit_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .graphicsLayer { alpha = exitAlphaAnimated }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Center Block: 100% ChapterPlayer Intro Animation
            KanriIntroLogo(
                isLight = isLight,
                startAnimation = isPaintingDone,
                isExiting = isExiting,
                onIntroReadyForButton = onIntroReadyForButton,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Block: Get Started Button & Privacy Link
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        alpha = buttonAlpha.value
                        translationY = buttonOffsetY.value * density
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGetStarted,
                    interactionSource = buttonInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = buttonScale
                            scaleY = buttonScale
                        },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBgColor,
                        contentColor = buttonContentColor
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_get_started),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.onboarding_privacy_link),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClick = {
                                haptics.click()
                                showPrivacySheet = true
                            }
                        )
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                )
            }
        }
    }

    if (showPrivacySheet) {
        OnboardingPrivacySheet(
            onDismiss = { showPrivacySheet = false }
        )
    }
}

/**
 * Slide 1: Profile Setup (Avatar Picker).
 */
@Composable
private fun ProfileSetupStepView(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val profilePrefs = remember { UserProfilePreferences.getInstance(context) }
    val initialName = remember { if (profilePrefs.userName == "Alex") "" else profilePrefs.userName }
    val initialVariant = remember { profilePrefs.avatarVariant }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        KokonutAvatarPicker(
            title = "Personalize your sanctuary",
            submitLabel = "Continue",
            initialName = initialName,
            initialVariant = initialVariant,
            onComplete = { name, variant, _ ->
                profilePrefs.userName = name
                profilePrefs.avatarVariant = variant
                onContinue()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Slides 2 to 7: Permission & Security Cards with clean Glassmorphism.
 */
@Composable
private fun PermissionCardView(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    grantLabel: String,
    grantedLabel: String,
    onGrant: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastSlide: Boolean = false
) {
    val tokens = com.omkarnub.kanri.ui.theme.LocalKanriGlass.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = tokens?.tintStrong ?: MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            border = BorderStroke(
                1.dp,
                tokens?.borderStart ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Monochrome Icon Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.5.dp,
                            color = if (isGranted)
                                MaterialTheme.colorScheme.onBackground
                            else
                                MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = description,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Grant Button
                Button(
                    onClick = { if (!isGranted) onGrant() },
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted)
                            Color.Transparent
                        else
                            MaterialTheme.colorScheme.onBackground,
                        contentColor = if (isGranted)
                            MaterialTheme.colorScheme.onBackground
                        else
                            MaterialTheme.colorScheme.background
                    ),
                    border = if (isGranted)
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
                    else
                        null,
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isGranted) grantedLabel else grantLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Continue / Finish Button
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text(
                        text = if (isLastSlide) "Finish Setup" else "Continue",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                }

                if (!isLastSlide) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onSkip) {
                        Text(
                            text = "Skip for now",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}
