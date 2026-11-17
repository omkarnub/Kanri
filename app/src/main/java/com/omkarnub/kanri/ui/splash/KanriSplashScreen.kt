package com.omkarnub.kanri.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import com.omkarnub.kanri.ui.onboarding.KanriIntroLogo
import kotlinx.coroutines.delay

private val EaseOutDecel = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/**
 * Animated Kanri Splash Screen Logo Reveal.
 *
 * Runs the authentic Kanri brand intro animation (emblem landing, smooth settle shift to the left,
 * and kinetic Panchang KANRI letters rising up into the iconic brand lockup) matching the get-started page,
 * followed by a seamless cross-fade exit once the application Compose tree is fully rendered.
 *
 * @param isAppReady Signals that the main application has completed its initial frame rendering.
 * @param isLightMode Whether the app is in light theme mode (determines void & ink contrast).
 * @param onSplashComplete Invoked when the exit transition finishes to cleanly unmount the overlay.
 */
@Composable
fun KanriSplashScreen(
    isAppReady: Boolean,
    isLightMode: Boolean = false,
    onSplashComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val voidColor = if (isLightMode) Color(0xFFFEFAEC) else Color(0xFF000000)
    val inkColor = if (isLightMode) Color(0xFF121212) else Color(0xFFFDFAEE)

    var isIntroFinished by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(isIntroFinished, isAppReady) {
        if (isIntroFinished && isAppReady && !isExiting) {
            isExiting = true
            delay(120L)
            splashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 360,
                    easing = EaseOutDecel
                )
            )
            onSplashComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = splashAlpha.value }
            .background(voidColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isAppReady && !isExiting) {
                        isIntroFinished = true
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        KanriIntroLogo(
            isLight = isLightMode,
            startAnimation = true,
            isExiting = isExiting,
            onIntroReadyForButton = {
                isIntroFinished = true
            },
            customInkColor = inkColor,
            scaleFactor = 1.25f
        )
    }
}
