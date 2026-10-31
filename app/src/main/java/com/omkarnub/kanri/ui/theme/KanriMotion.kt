package com.omkarnub.kanri.ui.theme

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import com.omkarnub.kanri.util.rememberKanriHaptics

/**
 * Kanri Motion System.
 * Built on Emil Kowalski's Design Engineering philosophy and Apple's WWDC Fluid Interface principles.
 *
 * Rules implemented:
 * 1. Never animate from scale(0) — always use physical starts (scale 0.92f - 0.96f) with fade.
 * 2. Never use ease-in for UI entrances — fast initial velocity gives instant perception of speed.
 * 3. Directional spatial consistency — navigation pushes forward/backward along the user's axis.
 * 4. Micro-press physics — tactile spring scale on pointer-down confirms the interface heard the user.
 * 5. Interruptible springs maintain velocity rather than restarting from zero.
 */
object KanriMotion {

    // MARK: - Easing Curves
    /** Strong ease-out: starts with instantaneous velocity and decelerates with momentum */
    val EaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Apple HIG iOS system curve for drawer and sheet transitions */
    val EaseDrawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Deceleration curve for numbers and counters */
    val EaseDecel = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    /** Strong ease-in-out for on-screen continuous repositioning */
    val EaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    // MARK: - Spring Physics Specs
    /** Fast and tight spring for button touches and micro-interactions */
    val PressSpring = spring<Float>(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMedium
    )

    /** Snappy spring for cards, popups, and dialog entrances */
    val SnappySpring = spring<Float>(
        dampingRatio = 0.78f,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Subtle bouncy spring for selection dots and icon toggles */
    val BouncySpring = spring<Float>(
        dampingRatio = 0.70f,
        stiffness = Spring.StiffnessMedium
    )

    /** Critically damped spring (zero overshoot) for large layout translations */
    val SmoothSlideSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    // MARK: - Durations
    const val DurationFast = 160
    const val DurationNormal = 240
    const val DurationMedium = 320

    // MARK: - Screen Push & Pop Transitions
    /**
     * Apple HIG-style horizontal screen push:
     * Incoming screen slides in from right edge (+100%) with fluid deceleration.
     * Exiting screen glides subtly to the left (-25%) with progressive fade.
     */
    fun screenPushEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = DurationMedium, easing = EaseOut)
        ) + fadeIn(
            animationSpec = tween(durationMillis = DurationNormal, easing = EaseOut)
        )
    }

    fun screenPushExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(durationMillis = DurationMedium, easing = EaseOut)
        ) + fadeOut(
            animationSpec = tween(durationMillis = DurationFast, easing = EaseOut)
        )
    }

    /**
     * Screen pop (backward) transition:
     * Current screen slides back out to the right (+100%).
     * Previous screen returns from the left (-25%) to center.
     */
    fun screenPopEnter(): EnterTransition {
        return slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(durationMillis = DurationMedium, easing = EaseOut)
        ) + fadeIn(
            animationSpec = tween(durationMillis = DurationNormal, easing = EaseOut)
        )
    }

    fun screenPopExit(): ExitTransition {
        return slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = DurationMedium, easing = EaseOut)
        ) + fadeOut(
            animationSpec = tween(durationMillis = DurationFast, easing = EaseOut)
        )
    }

    // MARK: - Dialog & Modal Scale Transitions
    /**
     * Dimensional scale-in from 0.94f to 1.0f with spring physics (never scales from 0)
     */
    fun dialogEnter(): EnterTransition {
        return scaleIn(
            initialScale = 0.94f,
            animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 180, easing = EaseOut)
        )
    }

    fun dialogExit(): ExitTransition {
        return scaleOut(
            targetScale = 0.96f,
            animationSpec = tween(durationMillis = 140, easing = EaseOut)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 140, easing = EaseOut)
        )
    }

    // MARK: - Directional Tab Switch Transition
    /**
     * Spatially consistent tab transition:
     * When switching to a tab to the right, content glides in from the right (+28%).
     * When switching to a tab to the left, content glides in from the left (-28%).
     */
    fun tabTransition(isForward: Boolean): ContentTransform {
        val enterOffset = if (isForward) 1 else -1
        val exitOffset = if (isForward) -1 else 1

        val enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> (fullWidth * 0.28f * enterOffset).toInt() },
            animationSpec = tween(durationMillis = 260, easing = EaseOut)
        ) + fadeIn(
            animationSpec = tween(durationMillis = 220, easing = EaseOut)
        ) + scaleIn(
            initialScale = 0.98f,
            animationSpec = tween(durationMillis = 260, easing = EaseOut)
        )

        val exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> (fullWidth * 0.18f * exitOffset).toInt() },
            animationSpec = tween(durationMillis = 220, easing = EaseOut)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 180, easing = EaseOut)
        )

        return enter.togetherWith(exit)
    }
}

/**
 * Universal tactile press feedback modifier.
 * Reacts instantly on pointer-down with spring deceleration (scale 0.96f) and tactile haptic tick.
 */
fun Modifier.kanriPressFeedback(
    pressedScale: Float = 0.96f,
    haptic: Boolean = true,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = rememberKanriHaptics()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = KanriMotion.PressSpring,
        label = "kanri_press_scale"
    )

    this
        .scale(scale)
        .then(
            if (onClick != null && enabled) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (haptic) haptics.click()
                        onClick()
                    }
                )
            } else Modifier
        )
}

