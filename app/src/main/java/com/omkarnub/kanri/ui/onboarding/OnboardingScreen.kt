package com.omkarnub.kanri.ui.onboarding

import android.Manifest
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import com.omkarnub.kanri.util.SmsPermissionHelper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.omkarnub.kanri.util.BatteryOptimizationHelper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.cloud.BackupMode
import com.omkarnub.kanri.data.cloud.CloudBackupPreferences
import com.omkarnub.kanri.data.cloud.CloudBackupWorker
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import com.omkarnub.kanri.data.security.SecurityPreferences
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.ui.onboarding.animation.FoldHinge
import com.omkarnub.kanri.ui.onboarding.animation.FoldText
import com.omkarnub.kanri.ui.popup.OverlayPermissionHelper
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.MonochromeBlack
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.common.GrainGradientBackground
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.omkarnub.kanri.R
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cloudBackupPrefs = remember { CloudBackupPreferences.getInstance(context) }
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }

    var currentSlide by remember { mutableIntStateOf(0) }
    var isLockConfigured by remember { mutableStateOf(securityPrefs.isLockEnabled) }

    var hasSmsPermission by remember {
        mutableStateOf(SmsPermissionHelper.hasSmsPermissions(context))
    }
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

    // Total slides: 0=hero, 1=profile-setup, 2=sms, 3=notification-listener, 4=overlay, 5=battery, 6=notifications, 7=security
    val totalSlides = 8

    // Helper to finish onboarding in offline-first mode
    fun finishOnboarding() {
        OnboardingPreferences.getInstance(context).hasCompletedOnboarding = true
        cloudBackupPrefs.backupMode = BackupMode.OFFLINE
        CloudBackupWorker.cancel(context)
        onComplete()
    }

    // Main container — smooth horizontal slide AnimatedContent between slides
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = currentSlide,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> fullWidth }
                    ) + fadeIn(animationSpec = tween(400))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeOut(animationSpec = tween(400)))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        initialOffsetX = { fullWidth -> -fullWidth }
                    ) + fadeIn(animationSpec = tween(400))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(400, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> fullWidth }
                    ) + fadeOut(animationSpec = tween(400)))
                }
            },
            label = "onboarding_slide",
            modifier = Modifier.fillMaxSize()
        ) { slide ->
            when (slide) {
                0 -> HeroSlide(
                    onGetStarted = { currentSlide = 1 }
                )

                1 -> ProfileSetupSlide(
                    onContinue = { currentSlide = 2 }
                )

                2 -> PermissionSlide(
                    icon = Icons.Default.Sms,
                    title = "Bank SMS Detection",
                    description = "Primary detection engine: Intercepts bank debit & credit alerts 100% offline. The only reliable source for sent payments (UPI, ATM, cards).",
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
                    slideIndex = 2,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 3 },
                    onSkip = { currentSlide = 3 }
                )

                3 -> PermissionSlide(
                    icon = Icons.Default.Notifications,
                    title = "Payment Apps Listener",
                    description = "Supplementary engine: Catches incoming money notifications and reward scratch cards from Google Pay, Amazon Pay, Paytm & FamPay.",
                    isGranted = hasNotificationAccess,
                    grantLabel = "Grant Access",
                    grantedLabel = "Access Granted",
                    onGrant = {
                        NotificationAccessHelper.openNotificationAccessSettings(context)
                    },
                    slideIndex = 3,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 4 },
                    onSkip = { currentSlide = 4 }
                )

                4 -> PermissionSlide(
                    icon = Icons.Default.Bolt,
                    title = "Display Overlay",
                    description = "Show a proper on-screen popup right after you pay, so you can review and categorize your transactions instantly.",
                    isGranted = hasOverlayPermission,
                    grantLabel = "Enable Overlay",
                    grantedLabel = "Overlay Enabled",
                    onGrant = {
                        OverlayPermissionHelper.requestOverlayPermission(context)
                    },
                    slideIndex = 4,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 5 },
                    onSkip = { currentSlide = 5 }
                )

                5 -> PermissionSlide(
                    icon = Icons.Default.PowerSettingsNew,
                    title = "Run in Background",
                    description = "Allow Kanri to run unrestricted in the background so Android never puts SMS detection or payment monitors to sleep.",
                    isGranted = hasBatteryExemption,
                    grantLabel = "Allow Background Run",
                    grantedLabel = "Background Run Active",
                    onGrant = {
                        BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
                    },
                    slideIndex = 5,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 6 },
                    onSkip = { currentSlide = 6 }
                )

                6 -> PermissionSlide(
                    icon = Icons.Default.Notifications,
                    title = "System Notifications",
                    description = "Receive instant transaction alerts, reminders, and backup status notifications right on your phone.",
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
                    slideIndex = 6,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 7 },
                    onSkip = { currentSlide = 7 }
                )

                7 -> PermissionSlide(
                    icon = Icons.Default.Fingerprint,
                    title = "Device Lock",
                    description = "Require biometric or device credential authentication every time Kanri opens.",
                    isGranted = isLockConfigured,
                    grantLabel = "Enable Lock",
                    grantedLabel = "Lock Enabled",
                    onGrant = {
                        val newState = !isLockConfigured
                        isLockConfigured = newState
                        securityPrefs.isLockEnabled = newState
                    },
                    slideIndex = 7,
                    totalSlides = totalSlides,
                    onNext = { finishOnboarding() },
                    onSkip = { finishOnboarding() },
                    isLastSlide = true
                )
            }
        }
    }
}

// ─── Slide 0: Redesigned Hero / Know Where Every Rupee Went ───────────────

@Composable
private fun HeroSlide(onGetStarted: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
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

    var showPrivacySheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "headline_sheen")
    val sheenOffset by infiniteTransition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen_offset"
    )

    // Headline and subline fade up (at t=2400ms, 300ms, 12dp rise)
    val textAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val textOffsetY = remember { Animatable(if (reduceMotion) 0f else 12f) }

    // Button rises and fades in (at t=3000ms, 300ms, 12dp rise)
    val buttonAlpha = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val buttonOffsetY = remember { Animatable(if (reduceMotion) 0f else 12f) }

    LaunchedEffect(reduceMotion) {
        if (!reduceMotion) {
            delay(150L)
            launch {
                textAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            }
            launch {
                textOffsetY.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            }
            delay(200L) // t = 350ms
            launch {
                buttonAlpha.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            }
            launch {
                buttonOffsetY.animateTo(0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
            }
        }
    }

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isButtonPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_press_scale"
    )

    LaunchedEffect(isButtonPressed) {
        if (isButtonPressed) {
            haptics.tick()
        }
    }

    val buttonBgColor = if (isLight) MonochromeBlack else DarkOffWhite
    val buttonContentColor = if (isLight) DarkOffWhite else MonochromeBlack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Background: Full-bleed animated mesh gradient + film grain with pulse hook
        GrainGradientBackground(
            modifier = Modifier.fillMaxSize()
        )

        // Single Column layout respecting WindowInsets and edge-to-edge
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top block: Top bar area (~8% height) + Hero Zone (45-50% height) + Text block
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 2. Top bar area: small Kanri wordmark (~20sp height)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "KANRI",
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 2.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3. Hero zone (about 45-50% height): vertically centered demo stage box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp, max = 280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingHeroDemo(
                            isExternalPaused = showPrivacySheet,
                            onPulseChange = {},
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Text block: Headline + Sub line
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = textAlpha.value
                                translationY = textOffsetY.value * density
                            },
                        horizontalAlignment = Alignment.Start
                    ) {
                        val baseColor = MaterialTheme.colorScheme.onSurface
                        val sheenColor = if (isLight) Color(0xFF666666) else Color(0xFFFFFFFF)
                        val headlineBrush = if (!reduceMotion) {
                            Brush.linearGradient(
                                colors = listOf(
                                    baseColor.copy(alpha = 0.90f),
                                    sheenColor,
                                    baseColor.copy(alpha = 0.90f)
                                ),
                                start = Offset(sheenOffset - 220f, 0f),
                                end = Offset(sheenOffset + 80f, 0f)
                            )
                        } else null

                        Text(
                            text = stringResource(R.string.onboarding_headline),
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            lineHeight = 37.sp,
                            style = if (headlineBrush != null) {
                                TextStyle(brush = headlineBrush)
                            } else {
                                TextStyle(color = baseColor)
                            },
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { heading() }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.onboarding_subline),
                            fontFamily = GoogleSansFlex,
                            fontSize = 15.5.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Bottom block (pinned above nav bar inset, 24dp horizontal padding, 24dp bottom padding)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .graphicsLayer {
                            alpha = buttonAlpha.value
                            translationY = buttonOffsetY.value * density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            haptics.click()
                            onGetStarted()
                        },
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

        // 6. Frosted Glass Bottom Sheet: How does Kanri stay private?
        if (showPrivacySheet) {
            OnboardingPrivacySheet(
                onDismiss = { showPrivacySheet = false }
            )
        }
    }
}

// ─── Slide 1: Profile Setup (Name & Picture, No DOB) ──────────────────────

@Composable
private fun ProfileSetupSlide(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val profilePrefs = remember { UserProfilePreferences.getInstance(context) }

    var nameInput by remember {
        mutableStateOf(if (profilePrefs.userName == "Alex") "" else profilePrefs.userName)
    }
    var profilePhotoPath by remember { mutableStateOf(profilePrefs.profilePhotoPath) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = profilePrefs.savePhotoFromUri(uri)
            if (saved) {
                profilePhotoPath = profilePrefs.profilePhotoPath
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Welcome to Kanri",
                fontFamily = Panchang,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Choose your name and profile picture to personalize your financial space.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Avatar picker
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                    .clickable {
                        photoPickerLauncher.launch("image/*")
                    },
                contentAlignment = Alignment.Center
            ) {
                val photoBitmap = remember(profilePhotoPath) {
                    if (profilePhotoPath != null && File(profilePhotoPath!!).exists()) {
                        try {
                            BitmapFactory.decodeFile(profilePhotoPath)?.asImageBitmap()
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }

                if (photoBitmap != null) {
                    Image(
                        bitmap = photoBitmap,
                        contentDescription = "Profile Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Avatar",
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Camera overlay badge in bottom-right corner
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground)
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = MaterialTheme.colorScheme.background,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { photoPickerLauncher.launch("image/*") }
            ) {
                Text(
                    text = if (profilePhotoPath != null) "Change photo" else "Choose photo",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name input field
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("What should we call you?") },
                placeholder = { Text("e.g. Alex") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }
                ),
                trailingIcon = {
                    if (nameInput.isNotBlank()) {
                        IconButton(onClick = { nameInput = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear name",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Continue Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val trimmed = nameInput.trim()
                    profilePrefs.userName = if (trimmed.isNotBlank()) trimmed else "Alex"
                    onContinue()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Continue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    if (profilePrefs.userName.isBlank()) {
                        profilePrefs.userName = "Alex"
                    }
                    onContinue()
                }
            ) {
                Text(
                    text = "Set up later",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Slides 2–7: Permission Slides ──────────────────────────────────────

@Composable
private fun PermissionSlide(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    grantLabel: String,
    grantedLabel: String,
    onGrant: () -> Unit,
    slideIndex: Int,
    totalSlides: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastSlide: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Clean monochrome icon circle
            Box(
                modifier = Modifier
                    .size(88.dp)
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
                AnimatedContent(
                    targetState = isGranted,
                    transitionSpec = {
                        (scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) +
                                fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                    },
                    label = "icon_switch"
                ) { granted ->
                    Icon(
                        imageVector = if (granted) Icons.Default.Check else icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Grant / Granted button
            Button(
                onClick = { if (!isGranted) onGrant() },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
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
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isGranted) grantedLabel else grantLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        // Bottom navigation: dots + next/skip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dot indicators (only for permission slides: index 2,3,4)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 2 until totalSlides) {
                    val isActive = slideIndex == i
                    val dotWidth by animateDpAsState(
                        targetValue = if (isActive) 20.dp else 6.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dot_width"
                    )
                    val dotAlpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.3f,
                        animationSpec = tween(300),
                        label = "dot_alpha"
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .alpha(dotAlpha)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.onBackground)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = if (isLastSlide) "Finish" else "Continue",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip
            TextButton(
                onClick = onSkip
            ) {
                Text(
                    text = if (isLastSlide) "Skip & Finish" else "Skip",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
