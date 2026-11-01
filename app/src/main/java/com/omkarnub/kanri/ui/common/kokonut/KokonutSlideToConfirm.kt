package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChanged
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import kotlin.math.abs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1:1 Port of Kokonut UI Slide to Confirm (A4. slide-to-confirm.tsx)
 *
 * Exact constants:
 * HEIGHT = 56, INSET = 4, THUMB = 48 (HEIGHT - 2 * INSET)
 * COMMIT_AT = 0.9, FLICK_SPEED = 900 px/s, FLICK_FROM = 0.45
 * OVERDRAG = 8, TAP_SLOP = 4
 * SPRINGS:
 * - settle / landing: stiffness = 520f, dampingRatio = 0.921f
 * EASINGS:
 * - glide: 420ms CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
 * - flooding cover: 360ms CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
 */

const val SLIDE_HEIGHT = 56f
const val SLIDE_INSET = 4f
const val SLIDE_THUMB = 48f
const val SLIDE_COMMIT_AT = 0.9f
const val SLIDE_FLICK_SPEED = 900f
const val SLIDE_FLICK_FROM = 0.45f
const val SLIDE_OVERDRAG = 8f
const val SLIDE_TAP_SLOP = 4f

val SLIDE_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val SLIDE_EASE_IN_OUT = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

enum class SlidePhase {
    IDLE, DRAGGING, CONFIRMING, DONE
}

/** Pure overdrag math matching Kokonut band() */
fun slideBand(raw: Float, max: Float): Float {
    if (raw > max) {
        return max + SLIDE_OVERDRAG * (1f - exp(-(raw - max) / 40f))
    }
    if (raw < 0f) {
        return -SLIDE_OVERDRAG * (1f - exp(raw / 40f))
    }
    return raw
}

/** Pure commit decision matching Kokonut release logic */
fun slideShouldCommit(progress: Float, velocity: Float): Boolean {
    val flicked = (velocity > SLIDE_FLICK_SPEED) && (progress > SLIDE_FLICK_FROM)
    return progress >= SLIDE_COMMIT_AT || flicked
}

@Composable
fun KokonutSlideToConfirm(
    label: String = "Slide to confirm",
    confirmingLabel: String = "Saving…",
    doneLabel: String = "Saved",
    onConfirm: suspend () -> Unit,
    resetAfter: Long? = null,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var phase by remember { mutableStateOf(SlidePhase.IDLE) }
    var isArmed by remember { mutableStateOf(false) }

    val xAnim = remember { Animatable(0f) }
    val coverAnim = remember { Animatable(0f) }

    val isDone = phase == SlidePhase.DONE
    val isBusy = phase == SlidePhase.CONFIRMING

    fun settleBack(velocity: Float = 0f) {
        isArmed = false
        phase = SlidePhase.IDLE
        scope.launch {
            launch { coverAnim.animateTo(0f, tween(300, easing = SLIDE_EASE_OUT)) }
            launch {
                xAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(stiffness = 520f, dampingRatio = 0.921f, visibilityThreshold = 0.5f),
                    initialVelocity = velocity
                )
            }
        }
    }

    fun commit(maxDist: Float, velocity: Float = 0f, glide: Boolean = false) {
        phase = SlidePhase.CONFIRMING
        isArmed = true
        haptics.tick()

        scope.launch {
            val landingJob = async {
                if (glide) {
                    xAnim.animateTo(maxDist, tween(420, easing = SLIDE_EASE_IN_OUT))
                } else {
                    xAnim.animateTo(
                        targetValue = maxDist,
                        animationSpec = spring(stiffness = 520f, dampingRatio = 0.921f),
                        initialVelocity = velocity
                    )
                }
            }

            val floodingJob = async {
                coverAnim.animateTo(1f, tween(360, easing = SLIDE_EASE_OUT))
            }

            val minDelayJob = async {
                delay(800)
            }

            var success = false
            try {
                coroutineScope {
                    val workJob = async { onConfirm() }
                    awaitAll(workJob, minDelayJob, landingJob, floodingJob)
                }
                success = true
            } catch (e: Exception) {
                success = false
            }

            if (success) {
                phase = SlidePhase.DONE
                haptics.success()
                if (resetAfter != null) {
                    delay(resetAfter)
                    isArmed = false
                    phase = SlidePhase.IDLE
                    launch { coverAnim.animateTo(0f, tween(360, easing = SLIDE_EASE_OUT)) }
                    launch { xAnim.animateTo(0f, tween(600, easing = SLIDE_EASE_IN_OUT)) }
                }
            } else {
                settleBack(0f)
            }
        }
    }

    val velocityTracker = remember { VelocityTracker() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .graphicsLayer {
                alpha = if (disabled) 0.5f else 1f
            }
            .semantics {
                role = Role.Button
                contentDescription = "$label. Double tap to confirm."
            }
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat()
        val thumbPx = with(density) { SLIDE_THUMB.dp.toPx() }
        val insetPx = with(density) { SLIDE_INSET.dp.toPx() }
        val maxDistPx = max(0f, trackWidthPx - thumbPx - insetPx * 2f)

        val currentX = xAnim.value
        val progress = if (maxDistPx > 0f) (currentX / maxDistPx).coerceIn(0f, 1f) else 0f
        val cover = coverAnim.value

        // Empty track label opacity: max(0, 1 - progress * 0.55)
        val emptyLabelOpacity = max(0f, 1f - progress * 0.55f)
        // Ink fill opacity: min(1, currentX / 20)
        val fillOpacity = min(1f, currentX / with(density) { 20.dp.toPx() })

        // 1. Gesture Tracking Overlay (fixed coordinates on track, 1:1 direct manipulation tracking)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(disabled, maxDistPx) {
                    if (disabled) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchX = down.position.x
                        val thumbLeft = insetPx + xAnim.value
                        val thumbRight = thumbLeft + thumbPx
                        val hitThumb = touchX >= (thumbLeft - with(density) { 20.dp.toPx() }) &&
                                touchX <= (thumbRight + with(density) { 32.dp.toPx() })

                        if (!hitThumb) return@awaitEachGesture
                        if (phase == SlidePhase.CONFIRMING || phase == SlidePhase.DONE) return@awaitEachGesture

                        val grabOffset = touchX - xAnim.value
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var isDragging = false
                        val startTouchX = touchX

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                if (!isDragging) {
                                    // Tap nudge to 28dp over 180ms ease-out, then spring back
                                    scope.launch {
                                        xAnim.animateTo(with(density) { 28.dp.toPx() }, tween(180, easing = SLIDE_EASE_OUT))
                                        if (phase == SlidePhase.IDLE) {
                                            xAnim.animateTo(0f, spring(stiffness = 520f, dampingRatio = 0.921f))
                                        }
                                    }
                                } else {
                                    val vel = velocityTracker.calculateVelocity().x
                                    val curProgress = if (maxDistPx > 0f) xAnim.value / maxDistPx else 0f
                                    if (slideShouldCommit(curProgress, vel)) {
                                        commit(maxDistPx, vel)
                                    } else {
                                        settleBack(vel)
                                    }
                                }
                                break
                            }

                            if (change.positionChanged()) {
                                val dx = change.position.x - startTouchX
                                if (!isDragging && abs(dx) > with(density) { 4.dp.toPx() }) {
                                    isDragging = true
                                    phase = SlidePhase.DRAGGING
                                    scope.launch { xAnim.stop() }
                                }

                                if (isDragging) {
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    val targetX = change.position.x - grabOffset
                                    val banded = slideBand(targetX, maxDistPx)
                                    scope.launch { xAnim.snapTo(banded) }
                                    val armed = banded >= maxDistPx * SLIDE_COMMIT_AT
                                    if (armed != isArmed) {
                                        isArmed = armed
                                        if (armed) haptics.tick()
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            // Track Background: 1dp border zinc-200/800, bg zinc-100 / dark zinc-900 (or glass tokens if present)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (tokens != null) tokens.tintFlat else (if (dark) Zinc900 else Zinc100)
                    )
                    .border(
                        1.dp,
                        if (tokens != null) tokens.borderStart.copy(alpha = 0.35f) else (if (dark) Zinc800 else Zinc200),
                        RoundedCornerShape(28.dp)
                    )
            )

        // 2. Empty-track Label
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp)
                .graphicsLayer { alpha = emptyLabelOpacity },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                maxLines = 1
            )
        }

        // 3. Ink Layer: bg zinc-900 / dark zinc-100 (turns emerald-600 / dark emerald-500 when done)
        val inkBgColor = if (isDone) {
            if (dark) Emerald500 else Emerald600
        } else {
            if (dark) Zinc100 else Zinc900
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fillOpacity }
        ) {
            val clipRight = (1f - cover) * max(0f, trackWidthPx - currentX - insetPx - thumbPx / 2f)
            val inkWidth = trackWidthPx - clipRight
            if (inkWidth > 0f) {
                drawRoundRect(
                    color = inkBgColor,
                    topLeft = Offset.Zero,
                    size = Size(inkWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
        }

        // Ink label (white / dark zinc-900; done: white / dark emerald-950)
        val inkTextColor = if (isDone) {
            if (dark) Color(0xFF022C22) else Color.White
        } else {
            if (dark) Zinc900 else Color.White
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = if (phase == SlidePhase.IDLE || phase == SlidePhase.DRAGGING) 24.dp else 0.dp)
                .padding(end = if (isBusy || isDone) 24.dp else 0.dp)
                .graphicsLayer { alpha = fillOpacity },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(260, easing = SLIDE_EASE_OUT)) +
                            slideInVertically(
                                animationSpec = tween(260, easing = SLIDE_EASE_OUT),
                                initialOffsetY = { 8 }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(260, easing = SLIDE_EASE_OUT)) +
                                    slideOutVertically(
                                        animationSpec = tween(260, easing = SLIDE_EASE_OUT),
                                        targetOffsetY = { -8 }
                                    ))
                },
                label = "inkStageLabel"
            ) { targetPhase ->
                val text = when (targetPhase) {
                    SlidePhase.CONFIRMING -> confirmingLabel
                    SlidePhase.DONE -> doneLabel
                    else -> label
                }
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = inkTextColor,
                    maxLines = 1
                )
            }
        }

        // 4. Thumb: 48dp circle, left offset INSET - 1 = 3dp, top INSET - 1 = 3dp
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (insetPx - with(density) { 1.dp.toPx() } + currentX).roundToInt(),
                        y = (insetPx - with(density) { 1.dp.toPx() }).roundToInt()
                    )
                }
                .size(48.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    if (tokens != null) tokens.tintStrong else (if (dark) Zinc800 else Color.White)
                )
                .border(
                    width = 1.dp,
                    color = if (tokens != null) tokens.borderStart else (if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Icon Stack: ArrowRight, Check, Spinner
            val infiniteTransition = rememberInfiniteTransition(label = "spinnerTransition")
            val spinAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spinnerAngle"
            )

            // Arrow Right icon
            val arrowOffset by animateFloatAsState(
                targetValue = if (isArmed && !isBusy && !isDone) 2f else if (isBusy || isDone) 6f else 0f,
                label = "arrowOffset"
            )
            val arrowAlpha by animateFloatAsState(
                targetValue = if (isBusy || isDone) 0f else 1f,
                label = "arrowAlpha"
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        translationX = arrowOffset * density.density
                        alpha = arrowAlpha
                    }
            )

            // Spinner (24dp, appears when busy)
            if (isBusy) {
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = spinAngle }
                ) {
                    val strokeW = 2.5f * density.density
                    val r = 9f * density.density
                    // Track circle
                    drawCircle(
                        color = if (dark) Zinc700 else Zinc200,
                        radius = r,
                        style = Stroke(width = strokeW)
                    )
                    // Arc dash
                    drawArc(
                        color = if (dark) Zinc100 else Zinc900,
                        startAngle = 0f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Check icon (20dp, appears with scale 0.5 -> 1 when done)
            val checkScale by animateFloatAsState(
                targetValue = if (isDone) 1f else 0.5f,
                animationSpec = tween(200, easing = SLIDE_EASE_OUT),
                label = "checkScale"
            )
            val checkAlpha by animateFloatAsState(
                targetValue = if (isDone) 1f else 0f,
                animationSpec = tween(200, easing = SLIDE_EASE_OUT),
                label = "checkAlpha"
            )

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (dark) Emerald400 else Emerald600,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkAlpha
                    }
            )
        }
    }
}
}

