package com.omkarnub.kanri.ui.onboarding

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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

    var hasNotificationAccess by remember {
        mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(OverlayPermissionHelper.canDrawOverlays(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationAccess = NotificationAccessHelper.isNotificationAccessGranted(context)
                hasOverlayPermission = OverlayPermissionHelper.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Total slides: 0=hero, 1=profile-setup, 2=notification, 3=overlay, 4=security
    val totalSlides = 5

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
                    icon = Icons.Default.Notifications,
                    title = "Notification Access",
                    description = "Automatically detect transactions from payment apps and bank alerts. Everything stays on-device.",
                    isGranted = hasNotificationAccess,
                    grantLabel = "Grant Access",
                    grantedLabel = "Access Granted",
                    onGrant = {
                        NotificationAccessHelper.openNotificationAccessSettings(context)
                    },
                    slideIndex = 2,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 3 },
                    onSkip = { currentSlide = 3 }
                )

                3 -> PermissionSlide(
                    icon = Icons.Default.Bolt,
                    title = "Display Overlay",
                    description = "Show a quick receipt popup right after you pay, so you can categorize with a single tap.",
                    isGranted = hasOverlayPermission,
                    grantLabel = "Enable Overlay",
                    grantedLabel = "Overlay Enabled",
                    onGrant = {
                        OverlayPermissionHelper.requestOverlayPermission(context)
                    },
                    slideIndex = 3,
                    totalSlides = totalSlides,
                    onNext = { currentSlide = 4 },
                    onSkip = { currentSlide = 4 }
                )

                4 -> PermissionSlide(
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
                    slideIndex = 4,
                    totalSlides = totalSlides,
                    onNext = { finishOnboarding() },
                    onSkip = { finishOnboarding() },
                    isLastSlide = true
                )
            }
        }
    }
}

// ─── Slide 0: Hero / Plain Dark Mode + KANRI FoldText ─────────────────────

@Composable
private fun HeroSlide(onGetStarted: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Centered KANRI fold text logo
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp),
            contentAlignment = Alignment.Center
        ) {
            FoldText(
                text = "KANRI",
                fontFamily = Panchang,
                fontWeight = FontWeight.Bold,
                fontSize = 50.sp,
                color = Color(0xFFF7F2E8),
                letterSpacing = 8.dp,
                hinge = FoldHinge.TOP,
                durationMs = 650,
                staggerMs = 50,
                creaseShading = 0.55f,
                loop = true,
                repeatDelayMs = 3000L
            )
        }

        // Bottom-pinned "Get Started" button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 52.dp)
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF7F2E8),
                    contentColor = Color(0xFF121212)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Get Started",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                )
            }
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

// ─── Slides 2–4: Permission Slides ──────────────────────────────────────

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
