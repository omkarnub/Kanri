package com.omkarnub.kanri.widget.core

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Standard animation curves, timings, and spring parameters for Pixel DotTiles.
 */
object PixelMotion {

    // Easing curves
    val FastOutSlowIn = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EaseOutQuart = CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f)

    // Spring spec for slider markers and dial targets
    const val SPRING_DAMPING_RATIO = 0.8f
    const val SPRING_STIFFNESS = 380f

    fun <T> sliderSpring() = spring<T>(
        dampingRatio = SPRING_DAMPING_RATIO,
        stiffness = SPRING_STIFFNESS
    )

    // Stagger & duration constants (ms)
    const val DURATION_ENTRY_MS = 650
    const val DURATION_NUMBER_COUNT_MS = 750
    const val STAGGER_STEP_BARS_MS = 40L
    const val AMBIENT_JITTER_INTERVAL_MS = 5000L
    const val FLAME_PULSE_DURATION_MS = 1000

    // Home screen burst parameters
    const val BURST_FRAME_COUNT = 10
    const val BURST_FRAME_INTERVAL_MS = 85L
    const val BURST_RATE_LIMIT_MS = 5000L
}

