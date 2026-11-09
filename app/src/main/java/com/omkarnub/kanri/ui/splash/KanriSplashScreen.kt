package com.omkarnub.kanri.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Exact Panchang vector glyph path data extracted from official brand assets (panchang_light / panchang_dark)
private const val PATH_K = "M32.97,90.72L32.97,77.68L49.09,67.07L75.01,90.72L96.8,90.72L61.88,58.89L95.18,35.48L72.58,35.48L42.69,55.24L32.97,63.1L32.97,35.48L16.52,35.48L16.52,90.72Z"
private const val PATH_A = "M172.45,90.72L192.38,90.72L156.41,35.48L134.54,35.48L98.5,90.72L118.26,90.72L124.99,80.03L165.81,80.03ZM132.11,68.61L145.4,47.39L158.6,68.61Z"
private const val PATH_N = "M212.3,90.72L212.3,52.73L260.17,90.72L278.56,90.72L278.56,35.48L262.11,35.48L262.11,72.82L214.65,35.48L195.85,35.48L195.85,90.72Z"
private const val PATH_R = "M303.75,90.72L303.75,74.68L324.73,74.68L344.57,90.72L365.23,90.72L347.08,76.46L342.71,73.79L346.27,73.71C358.5,72.74 365.71,66.91 365.71,54.76C365.71,40.26 356.24,35.48 342.63,35.48L287.3,35.48L287.3,90.72ZM303.75,49.57L341.01,49.57C346.43,49.57 348.7,51.11 348.7,56.05C348.7,60.59 346.43,62.37 341.01,62.37L303.75,62.37Z"
private const val PATH_I = "M389.37,90.72L389.37,35.48L372.92,35.48L372.92,90.72Z"

// SVG coordinate boundaries of the KANRI mark
private const val SVG_TOTAL_MIN_X = 16.52f
private const val SVG_TOTAL_MAX_X = 389.37f
private const val SVG_TOTAL_MIN_Y = 35.48f
private const val SVG_TOTAL_MAX_Y = 90.72f
private const val SVG_TOTAL_WIDTH = SVG_TOTAL_MAX_X - SVG_TOTAL_MIN_X // 372.85f
private const val SVG_TOTAL_HEIGHT = SVG_TOTAL_MAX_Y - SVG_TOTAL_MIN_Y // 55.24f

// Slicing X intervals in SVG coordinate space for clipping each letter
private val SLICE_BOUNDS_X = floatArrayOf(
    16.52f, 97.5f,   // Letter K
    97.5f, 194.0f,   // Letter A
    194.0f, 282.0f,  // Letter N
    282.0f, 369.0f,  // Letter R
    369.0f, 392.0f   // Letter I
)

// Motion easing curves matching the Apple-grade reveal specification
private val EaseOutDecel = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val EaseRuleInOut = CubicBezierEasing(0.7f, 0f, 0.3f, 1f)
private val EaseExitIn = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

/**
 * Animated Kanri Splash Screen Logo Reveal.
 *
 * Implements the Apple-grade staggered vector letter slide-in, horizontal underline draw/wipe,
 * and seamless exit handoff once the application Compose tree and databases are rendered.
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

    // Pre-parse the 5 letter paths into hardware-accelerated Compose Paths
    val letterPaths = remember {
        listOf(PATH_K, PATH_A, PATH_N, PATH_R, PATH_I).map {
            PathParser.createPathFromPathData(it).asComposePath()
        }
    }

    // Per-letter normalized vertical translation: +1f (hidden below) -> 0f (aligned) -> -1f (exited above)
    val letterAnimatables = remember {
        List(5) { Animatable(1.15f) }
    }

    // Rule (horizontal underline) animation progress: 0f -> 1f (draw phase) -> 2f (wipe phase)
    val ruleAnimatable = remember { Animatable(0f) }

    // Overall splash overlay alpha for exit cross-fade
    val splashAlpha = remember { Animatable(1f) }

    var isEntranceFinished by remember { mutableStateOf(false) }
    var isExitStarted by remember { mutableStateOf(false) }

    // Phase 1: Entrance Choreography (Letters slide in from bottom + Rule draws and wipes)
    LaunchedEffect(Unit) {
        // Staggered letter slide-in from bottom
        letterAnimatables.forEachIndexed { index, anim ->
            launch {
                delay(220L + index * 100L)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 850,
                        easing = EaseOutDecel
                    )
                )
            }
        }

        // Rule draws across left-to-right (0f -> 1f), then wipes out to the right (1f -> 2f)
        launch {
            delay(1150L)
            ruleAnimatable.animateTo(
                targetValue = 2f,
                animationSpec = tween(
                    durationMillis = 1250,
                    easing = EaseRuleInOut
                )
            )
        }

        // Entrance finishes after the letters and rule conclude (~2.3s)
        delay(2300L)
        isEntranceFinished = true
    }

    // Phase 2: Exit Transition triggered when BOTH entrance is complete AND app is ready
    LaunchedEffect(isEntranceFinished, isAppReady) {
        if (isEntranceFinished && isAppReady && !isExitStarted) {
            isExitStarted = true

            // Letters slide up and out, staggered
            letterAnimatables.forEachIndexed { index, anim ->
                launch {
                    delay(index * 60L)
                    anim.animateTo(
                        targetValue = -1.15f,
                        animationSpec = tween(
                            durationMillis = 480,
                            easing = EaseExitIn
                        )
                    )
                }
            }

            // Cross-fade the splash screen background into the fully rendered app
            launch {
                delay(120L)
                splashAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 380,
                        easing = EaseOutDecel
                    )
                )
                onSplashComplete()
            }
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
                    // Tap-to-proceed if app is already rendered
                    if (isAppReady && !isExitStarted) {
                        isEntranceFinished = true
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // Target wordmark width: 62% of display width, clamped to sane range
            val targetWidth = (canvasW * 0.62f).coerceIn(200f, 360.dp.toPx())
            val scale = targetWidth / SVG_TOTAL_WIDTH
            val targetHeight = SVG_TOTAL_HEIGHT * scale

            val startX = (canvasW - targetWidth) / 2f
            val startY = (canvasH - targetHeight) / 2f

            // 1. Draw each of the 5 letters with isolated bounding clip and vertical translation
            for (i in 0 until 5) {
                val sliceSvgLeft = SLICE_BOUNDS_X[i * 2]
                val sliceSvgRight = SLICE_BOUNDS_X[i * 2 + 1]

                val clipLeft = startX + (sliceSvgLeft - SVG_TOTAL_MIN_X) * scale
                val clipRight = startX + (sliceSvgRight - SVG_TOTAL_MIN_X) * scale
                val clipTop = startY
                val clipBottom = startY + targetHeight

                val offsetY = letterAnimatables[i].value * targetHeight

                clipRect(
                    left = clipLeft - 1f,
                    top = clipTop - 1f,
                    right = clipRight + 1f,
                    bottom = clipBottom + 1f
                ) {
                    translate(left = 0f, top = offsetY) {
                        translate(left = startX, top = startY) {
                            scale(scale = scale, pivot = Offset.Zero) {
                                translate(left = -SVG_TOTAL_MIN_X, top = -SVG_TOTAL_MIN_Y) {
                                    drawPath(
                                        path = letterPaths[i],
                                        color = inkColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Draw the thin animated rule (underline) beneath the wordmark
            val ruleVal = ruleAnimatable.value
            if (ruleVal > 0f && ruleVal < 2f) {
                val ruleWidth = targetWidth
                val ruleLeft = startX
                val ruleThickness = 2.dp.toPx()
                val ruleY = startY + targetHeight + 10.dp.toPx()

                if (ruleVal <= 1f) {
                    // Phase 1: Draw from left to right (origin: left center)
                    val drawW = ruleWidth * ruleVal
                    drawRect(
                        color = inkColor,
                        topLeft = Offset(ruleLeft, ruleY),
                        size = Size(drawW, ruleThickness)
                    )
                } else {
                    // Phase 2: Wipe out to the right (origin: right center)
                    val wipeProgress = ruleVal - 1f
                    val currentLeft = ruleLeft + ruleWidth * wipeProgress
                    val currentW = ruleWidth * (1f - wipeProgress)
                    if (currentW > 0f) {
                        drawRect(
                            color = inkColor,
                            topLeft = Offset(currentLeft, ruleY),
                            size = Size(currentW, ruleThickness)
                        )
                    }
                }
            }
        }
    }
}

