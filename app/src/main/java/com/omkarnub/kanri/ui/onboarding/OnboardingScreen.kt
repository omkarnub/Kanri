package com.omkarnub.kanri.ui.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.MotionEvent
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
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
import androidx.compose.runtime.CompositionLocalProvider
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.kokonut.KokonutAvatarPicker
import com.omkarnub.kanri.ui.theme.KanriTheme
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
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
    val isLight = true

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

    // Step state: 0 = Hero, 1 = Profile, 2 = SMS, 3 = UPI, 4 = Overlay, 5 = Battery, 6 = Alerts, 7 = Lock, 8 = All Set
    var currentSlide by remember { mutableIntStateOf(0) }
    val totalSlides = 9

    // App launch screen fade-in
    val screenAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(Unit) {
        screenAlpha.animateTo(1f, tween(360, easing = LinearEasing))
    }

    // Painting & Bridge state
    var isScene0PaintingDone by remember { mutableStateOf(false) }
    var isIntroReadyForButton by remember { mutableStateOf(false) }
    var isHeroExiting by remember { mutableStateOf(false) }
    var isStepCardVisible by remember { mutableStateOf(false) }
    var awaitAutoAdvanceForStep by remember { mutableIntStateOf(-1) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Fallback safety timeout for Scene 0 painting (1800ms)
    LaunchedEffect(Unit) {
        delay(1800L)
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

    var pendingSkipConfig by remember { mutableStateOf<PermissionStepConfig?>(null) }

    val permissionConfigs = remember {
        mapOf(
            2 to PermissionStepConfig(
                step = 2,
                icon = Icons.Default.Sms,
                title = "Bank SMS Detection",
                description = "Intercepts bank debit & credit alerts 100% offline. The only reliable source for sent payments (UPI, ATM, cards).",
                grantLabel = "Enable SMS Detection",
                grantedLabel = "SMS Detection Active",
                skipDialogTitle = "SMS Detection is Essential",
                skipDialogRationale = "Kanri operates 100% offline without connecting to your bank servers. Bank SMS is the only way Kanri can detect debit transactions, ATM withdrawals, and card spends. Without this, you will have to enter every single expense manually."
            ),
            3 to PermissionStepConfig(
                step = 3,
                icon = Icons.Default.AccountBalanceWallet,
                title = "Payment Apps Listener",
                description = "Catches incoming money notifications and scratch cards from Google Pay, PhonePe, Paytm, and CRED.",
                grantLabel = "Grant Access",
                grantedLabel = "Access Granted",
                skipDialogTitle = "Incoming UPI Tracking",
                skipDialogRationale = "Banks rarely send SMS alerts for peer-to-peer transfers or cashback rewards received via UPI. Notification access allows Kanri to capture money received on Google Pay, PhonePe, Paytm, and CRED in real-time."
            ),
            4 to PermissionStepConfig(
                step = 4,
                icon = Icons.Default.Bolt,
                title = "Instant Overlay",
                description = "Display a lightweight popup right after you pay, so you can review and categorize payments the moment they happen.",
                grantLabel = "Enable Overlay",
                grantedLabel = "Overlay Enabled",
                skipDialogTitle = "Instant Categorization",
                skipDialogRationale = "The instant overlay pops up for 2 seconds right after any payment so you can tag food, fuel, or bills with a single tap. Without it, transactions accumulate uncategorized until you open the app."
            ),
            5 to PermissionStepConfig(
                step = 5,
                icon = Icons.Default.PowerSettingsNew,
                title = "Background Endurance",
                description = "Keep Kanri running unrestricted in the background so Android never puts your expense trackers to sleep.",
                grantLabel = "Allow Background Run",
                grantedLabel = "Background Run Active",
                skipDialogTitle = "Prevent Background Termination",
                skipDialogRationale = "Android aggressively terminates apps in the background to save battery. Disabling optimization guarantees that Kanri stays alive to capture SMS alerts even when your phone has been asleep for hours."
            ),
            6 to PermissionStepConfig(
                step = 6,
                icon = Icons.Default.Notifications,
                title = "System Alerts",
                description = "Receive mindful spend nudges, daily summaries, and backup status notifications right on your phone.",
                grantLabel = "Enable Notifications",
                grantedLabel = "Notifications Active",
                skipDialogTitle = "Mindful Budget Alerts",
                skipDialogRationale = "Notifications keep you mindful with daily spending summaries, budget threshold alerts, and offline backup reminders. Without them, you won't receive timely budget nudges."
            ),
            7 to PermissionStepConfig(
                step = 7,
                icon = Icons.Default.Fingerprint,
                title = "Biometric Security",
                description = "Require Fingerprint, Face Unlock, or Device Lock every time you open your financial scroll.",
                grantLabel = "Enable App Lock",
                grantedLabel = "App Lock Enabled",
                skipDialogTitle = "Protect Financial Privacy",
                skipDialogRationale = "Your transaction history and spending habits are sensitive personal data. App Lock prevents anyone with physical access to your unlocked phone from viewing your financial sanctuary."
            )
        )
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
        awaitAutoAdvanceForStep = -1
        coroutineScope.launch {
            isStepCardVisible = false
            delay(240L)
            val prefs = OnboardingPreferences.getInstance(context)
            prefs.hasPromptedPostOnboardingSms = true
            BatteryOptimizationHelper.setPromptedOnce(context, true)
            prefs.hasCompletedOnboarding = true
            cloudBackupPrefs.backupMode = BackupMode.OFFLINE
            CloudBackupWorker.cancel(context)
            onComplete()
        }
    }

    // Transition functions maintaining the sequential choreography:
    // First fade out current UI -> progress painting & background scroll -> fade in new step
    fun advanceToStep(targetStep: Int) {
        haptics.click()
        awaitAutoAdvanceForStep = -1
        val clampedStep = targetStep.coerceIn(0, totalSlides - 1)
        coroutineScope.launch {
            isStepCardVisible = false
            delay(240L)
            currentSlide = clampedStep
            webViewInstance?.evaluateJavascript("window.setOnboardingStep($clampedStep);", null)
            delay(1750L)
            isStepCardVisible = true
        }
    }

    fun stepBack() {
        haptics.click()
        awaitAutoAdvanceForStep = -1
        coroutineScope.launch {
            isStepCardVisible = false
            delay(240L)
            if (currentSlide <= 1) {
                currentSlide = 0
                isHeroExiting = false
                webViewInstance?.evaluateJavascript("window.setOnboardingStep(0);", null)
            } else {
                val prev = currentSlide - 1
                currentSlide = prev
                webViewInstance?.evaluateJavascript("window.setOnboardingStep($prev);", null)
                delay(1750L)
                isStepCardVisible = true
            }
        }
    }

    fun launchPermissionForStep(step: Int) {
        haptics.click()
        awaitAutoAdvanceForStep = step
        when (step) {
            2 -> smsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
            3 -> NotificationAccessHelper.openNotificationAccessSettings(context)
            4 -> OverlayPermissionHelper.requestOverlayPermission(context)
            5 -> BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
            6 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        // Settings screen unavailable on restricted profiles
                    }
                }
            }
            7 -> {
                isLockConfigured = true
                securityPrefs.isLockEnabled = true
            }
        }
    }

    val isCurrentPermissionGranted = when (currentSlide) {
        2 -> hasSmsPermission
        3 -> hasNotificationAccess
        4 -> hasOverlayPermission
        5 -> hasBatteryExemption
        6 -> hasNotificationPermission
        7 -> isLockConfigured
        else -> false
    }

    LaunchedEffect(currentSlide, isStepCardVisible, awaitAutoAdvanceForStep, isCurrentPermissionGranted) {
        if (currentSlide in 2..7 &&
            awaitAutoAdvanceForStep == currentSlide &&
            isStepCardVisible &&
            isCurrentPermissionGranted
        ) {
            awaitAutoAdvanceForStep = -1
            delay(520L)
            advanceToStep(currentSlide + 1)
        }
    }

    CompositionLocalProvider(
        LocalKanriGlass provides null,
        LocalHazeState provides null
    ) {
        KanriTheme(
            themeMode = ThemeMode.LIGHT,
            dynamicColor = false
        ) {
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
                                domStorageEnabled = false
                                allowFileAccess = false
                                allowContentAccess = false
                                @Suppress("DEPRECATION")
                                allowFileAccessFromFileURLs = false
                                @Suppress("DEPRECATION")
                                allowUniversalAccessFromFileURLs = false
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
                                    if (com.omkarnub.kanri.BuildConfig.DEBUG) {
                                        android.util.Log.d("ShanShuiWebView", "${consoleMessage?.message()} [line ${consoleMessage?.lineNumber()}]")
                                    }
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: android.webkit.WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    return !url.startsWith("file:///android_asset/")
                                }

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
                                    view?.evaluateJavascript(
                                        "if (window.setPaperTheme) window.setPaperTheme('light'); if (window.setOnboardingStep) window.setOnboardingStep($currentSlide, true);",
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
                            setOnTouchListener { view, event ->
                                if (event.action == MotionEvent.ACTION_UP) {
                                    view.performClick()
                                }
                                false
                            }
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
                        isLight = true,
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
                                delay(260L)

                                // 2. Only then progress background scroll & paint
                                currentSlide = 1
                                webViewInstance?.evaluateJavascript("window.setOnboardingStep(1);", null)

                                // 3. Give the living scroll calm time to glide and paint (~1800ms)
                                delay(1800L)

                                // 4. Fade in Step 1 Avatar Picker card
                                isStepCardVisible = true
                            }
                        }
                    )
                } else {
                    // STEPS 1 TO 8: SLIDER WORKFLOW
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .displayCutoutPadding()
                    ) {
                        // Ultra-clean centered progress bar on setup steps 1..7; hidden on final Step 8 ("You're all set")
                        if (currentSlide in 1..7) {
                            val progressValue = ((currentSlide - 1).coerceAtLeast(0).toFloat() / 6f).coerceIn(0f, 1f)
                            val animatedProgress by animateFloatAsState(
                                targetValue = progressValue,
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                                label = "onboarding_progress"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .widthIn(max = 380.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "STEP $currentSlide OF 7",
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            letterSpacing = 1.2.sp,
                                            color = Color(0xFF1E2229)
                                        )
                                        Text(
                                            text = "${(progressValue * 100).toInt()}%",
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF555555)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF0F0E0C),
                                        trackColor = Color(0xFF0F0E0C).copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }

                        // Main Step Content with Smooth Fade & Scale Transition
                        val cardAlpha by animateFloatAsState(
                            targetValue = if (isStepCardVisible) 1f else 0f,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            label = "card_alpha"
                        )
                        val cardScale by animateFloatAsState(
                            targetValue = if (isStepCardVisible) 1f else 0.97f,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            label = "card_scale"
                        )
                        val cardOffsetY by animateFloatAsState(
                            targetValue = if (isStepCardVisible) 0f else 14f,
                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
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
                                }
                        ) {
                            when (currentSlide) {
                                1 -> ProfileSetupStepView(
                                    onContinue = { advanceToStep(2) }
                                )

                                in 2..7 -> {
                                    val cfg = permissionConfigs[currentSlide]
                                    if (cfg != null) {
                                        PaintingPermissionView(
                                            config = cfg,
                                            isGranted = isCurrentPermissionGranted,
                                            isAutoAdvancing = awaitAutoAdvanceForStep == currentSlide,
                                            onGrant = { launchPermissionForStep(currentSlide) },
                                            onNext = { advanceToStep(currentSlide + 1) },
                                            onRequestSkip = {
                                                haptics.tick()
                                                pendingSkipConfig = cfg
                                            }
                                        )
                                    }
                                }

                                8 -> FinalAllSetSlideView(
                                    onFinish = { finishOnboarding() }
                                )
                            }
                        }

                        // Bottom Navigation Row: Back button kept cleanly on bottom-left
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { stepBack() },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF141416).copy(alpha = 0.65f))
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.35f),
                                                Color.White.copy(alpha = 0.10f)
                                            )
                                        ),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Educational Permission Warning Modal when user attempts to skip
                pendingSkipConfig?.let { cfg ->
                    PermissionSkipWarningDialog(
                        config = cfg,
                        onGrant = {
                            val step = cfg.step
                            pendingSkipConfig = null
                            launchPermissionForStep(step)
                        },
                        onSkipAnyway = {
                            val step = cfg.step
                            pendingSkipConfig = null
                            advanceToStep(step + 1)
                        },
                        onDismiss = { pendingSkipConfig = null }
                    )
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

    val buttonBgColor = Color(0xFF0F0E0C)
    val buttonContentColor = Color.White

    val exitAlpha = if (isExiting) 0f else 1f
    val exitAlphaAnimated by animateFloatAsState(
        targetValue = exitAlpha,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
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
                        containerColor = Color(0xFF141416).copy(alpha = 0.78f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.45f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
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
                        letterSpacing = 0.3.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.onboarding_privacy_link),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF555555),
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = androidx.compose.ui.BiasAlignment(0f, -0.38f)
    ) {
        KokonutAvatarPicker(
            title = "Personalize your sanctuary",
            submitLabel = "Continue",
            initialName = initialName,
            initialVariant = initialVariant,
            forceLight = true,
            useGlassCard = true,
            onComplete = { name, variant, _ ->
                profilePrefs.userName = name
                profilePrefs.avatarVariant = variant
                onContinue()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private data class PermissionStepConfig(
    val step: Int,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val grantLabel: String,
    val grantedLabel: String,
    val skipDialogTitle: String,
    val skipDialogRationale: String
)

/**
 * Slides 2 to 7: Minimalist permission title, caption, primary action, and small Skip button
 * anchored cleanly in the calm open lower river foreground below the sampan boat.
 */
@Composable
private fun PaintingPermissionView(
    config: PermissionStepConfig,
    isGranted: Boolean,
    isAutoAdvancing: Boolean,
    onGrant: () -> Unit,
    onNext: () -> Unit,
    onRequestSkip: () -> Unit
) {
    val showGrantedState = isGranted && isAutoAdvancing
    val primaryLabel = when {
        showGrantedState -> config.grantedLabel
        isGranted -> "Continue"
        else -> config.grantLabel
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, bottom = 14.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = config.title,
                fontFamily = GoogleSansFlex,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF121418),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFEFEAE0).copy(alpha = 0.94f),
                        offset = Offset.Zero,
                        blurRadius = 12f
                    )
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = config.description,
                fontFamily = GoogleSansFlex,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF282D37),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFEFEAE0).copy(alpha = 0.94f),
                        offset = Offset.Zero,
                        blurRadius = 10f
                    )
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (isGranted) {
                            if (!isAutoAdvancing) onNext()
                        } else {
                            onGrant()
                        }
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGranted) Color(0xFF1B5E20).copy(alpha = 0.88f)
                        else Color(0xFF141416).copy(alpha = 0.84f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isGranted) Color(0xFF81C784).copy(alpha = 0.65f)
                        else Color.White.copy(alpha = 0.35f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                ) {
                    if (isGranted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = primaryLabel,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        color = Color.White
                    )
                }

                if (!isGranted) {
                    TextButton(
                        onClick = onRequestSkip,
                        modifier = Modifier.height(44.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Skip",
                            fontFamily = GoogleSansFlex,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3E444F),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFFEFEAE0).copy(alpha = 0.94f),
                                    offset = Offset.Zero,
                                    blurRadius = 10f
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Slide 8: Serene dawn completion view with static Kanri logo, "You're all set", and Finish button.
 */
@Composable
private fun FinalAllSetSlideView(
    onFinish: () -> Unit
) {
    val haptics = rememberKanriHaptics()
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "final_finish_press"
    )

    LaunchedEffect(isButtonPressed) {
        if (isButtonPressed) haptics.tick()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .align(androidx.compose.ui.BiasAlignment(0f, -0.34f))
                .widthIn(max = 360.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            KanriLogoLockup()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You're all set",
                fontFamily = GoogleSansFlex,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF121418),
                textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFEFEAE0).copy(alpha = 0.92f),
                        offset = Offset.Zero,
                        blurRadius = 12f
                    )
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your private financial sanctuary is ready.",
                fontFamily = GoogleSansFlex,
                fontSize = 14.5.sp,
                lineHeight = 20.sp,
                color = Color(0xFF3E444F),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFFEFEAE0).copy(alpha = 0.92f),
                        offset = Offset.Zero,
                        blurRadius = 10f
                    )
                )
            )
        }

        Button(
            onClick = {
                haptics.click()
                onFinish()
            },
            interactionSource = buttonInteractionSource,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(56.dp)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF141416).copy(alpha = 0.84f),
                contentColor = Color.White
            ),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.15f)
                    )
                )
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Text(
                text = "Finish",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.3.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Educational modal warning when the user attempts to skip an important permission.
 */
@Composable
private fun PermissionSkipWarningDialog(
    config: PermissionStepConfig,
    onGrant: () -> Unit,
    onSkipAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF141416).copy(alpha = 0.95f))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.38f),
                            Color.White.copy(alpha = 0.10f)
                        )
                    ),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC43B38).copy(alpha = 0.18f))
                        .border(1.dp, Color(0xFFC43B38).copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = config.skipDialogTitle,
                    fontFamily = GoogleSansFlex,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = config.skipDialogRationale,
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = Color.White.copy(alpha = 0.84f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onGrant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF141416)
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                ) {
                    Text(
                        text = "Enable Permission",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF141416)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onSkipAnyway) {
                    Text(
                        text = "Skip Anyway",
                        fontFamily = GoogleSansFlex,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}
