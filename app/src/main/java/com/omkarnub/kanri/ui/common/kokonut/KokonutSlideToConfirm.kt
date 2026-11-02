package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1:1 Port of Kokonut UI Slide to Confirm (#A4 slide-to-confirm.tsx)
 *
 * Polished to the core with Apple Fluid Design:
 * - Direct 1:1 finger manipulation with ZERO latency
 * - Synchronous drag offset tracking so the slider NEVER gets stuck
 * - Smooth rubberband overdrag resistance (band curve)
 * - Fluid ink trailing layer
 * - Tactile release commit (threshold 0.85 or flick)
 * - Saving spinner -> Saved emerald checkmark
 */

const val SLIDE_HEIGHT = 56f
const val SLIDE_INSET = 4f
const val SLIDE_THUMB = 48f
const val SLIDE_COMMIT_AT = 0.85f
const val SLIDE_FLICK_SPEED = 800f
const val SLIDE_FLICK_FROM = 0.40f
const val SLIDE_OVERDRAG = 8f

val SLIDE_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
val SLIDE_EASE_IN_OUT = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)



enum class SlidePhase {
    IDLE, DRAGGING, CONFIRMING, DONE
}

/**
 * Slide confirmation entry type defining the completion colors and banner message
 */
enum class SlideConfirmationType(
    val defaultDoneLabel: String,
    val lightColor: Color,
    val darkColor: Color
) {
    INCOME(
        defaultDoneLabel = "Income entry added!",
        lightColor = Color(0xFF059669), // Emerald 600
        darkColor = Color(0xFF10B981)   // Emerald 500
    ),
    EXPENSE(
        defaultDoneLabel = "Expense entry added!",
        lightColor = Color(0xFFE11D48), // Crimson / Rose 600
        darkColor = Color(0xFFF43F5E)   // Crimson / Rose 500
    ),
    LEND(
        defaultDoneLabel = "Lending entry added!",
        lightColor = Color(0xFF4F46E5), // Indigo 600
        darkColor = Color(0xFF6366F1)   // Indigo 500
    ),
    BORROW(
        defaultDoneLabel = "Borrowing entry added!",
        lightColor = Color(0xFFD97706), // Amber 600
        darkColor = Color(0xFFF59E0B)   // Amber 500
    ),
    DEFAULT(
        defaultDoneLabel = "Saved",
        lightColor = Color(0xFF059669),
        darkColor = Color(0xFF10B981)
    )
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
    doneLabel: String? = null,
    confirmationType: SlideConfirmationType = SlideConfirmationType.DEFAULT,
    onConfirm: suspend () -> Unit,
    resetAfter: Long? = null,
    disabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveDoneLabel = doneLabel ?: confirmationType.defaultDoneLabel
    val dark = isAppInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var phase by remember { mutableStateOf(SlidePhase.IDLE) }
    var isArmed by remember { mutableStateOf(false) }

    // Direct synchronous drag offset for 0-latency tracking
    var dragX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val xAnim = remember { Animatable(0f) }
    val coverAnim = remember { Animatable(0f) }

    val isDone = phase == SlidePhase.DONE
    val isBusy = phase == SlidePhase.CONFIRMING

    fun settleBack(velocity: Float = 0f) {
        isArmed = false
        phase = SlidePhase.IDLE
        scope.launch {
            launch { coverAnim.animateTo(0f, tween(280, easing = SLIDE_EASE_OUT)) }
            launch {
                xAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(stiffness = 520f, dampingRatio = 0.92f, visibilityThreshold = 0.5f),
                    initialVelocity = velocity
                )
            }
        }
    }

    fun commit(maxDist: Float, velocity: Float = 0f) {
        phase = SlidePhase.CONFIRMING
        isArmed = true
        haptics.tick()

        scope.launch {
            val landingJob = async {
                xAnim.animateTo(
                    targetValue = maxDist,
                    animationSpec = spring(stiffness = 520f, dampingRatio = 0.92f),
                    initialVelocity = velocity
                )
            }

            val floodingJob = async {
                coverAnim.animateTo(1f, tween(320, easing = SLIDE_EASE_OUT))
            }

            val minDelayJob = async {
                delay(600)
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
                    launch { xAnim.animateTo(0f, tween(500, easing = SLIDE_EASE_IN_OUT)) }
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

        // Current X position: Direct touch tracking during drag, animated otherwise
        val currentX = if (isDragging) dragX else xAnim.value
        val progress = if (maxDistPx > 0f) (currentX / maxDistPx).coerceIn(0f, 1f) else 0f
        val cover = coverAnim.value

        // Label opacities
        val emptyLabelOpacity = max(0f, 1f - progress * 0.70f)
        val fillOpacity = min(1f, currentX / with(density) { 16.dp.toPx() })

        // 1. Gesture Tracking Layer (1:1 Direct touch tracking with zero coroutine latency)
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

                        // Generous grab area: allows grabbing thumb from anywhere near it
                        val hitThumb = touchX <= (thumbRight + with(density) { 44.dp.toPx() })
                        if (!hitThumb) return@awaitEachGesture
                        if (phase == SlidePhase.CONFIRMING || phase == SlidePhase.DONE) return@awaitEachGesture

                        val grabOffset = touchX - xAnim.value
                        velocityTracker.resetTracking()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var draggingStarted = false
                        val startTouchX = touchX

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                if (!draggingStarted) {
                                    // Tap nudge
                                    scope.launch {
                                        xAnim.animateTo(with(density) { 24.dp.toPx() }, tween(160, easing = SLIDE_EASE_OUT))
                                        if (phase == SlidePhase.IDLE) {
                                            xAnim.animateTo(0f, spring(stiffness = 520f, dampingRatio = 0.92f))
                                        }
                                    }
                                } else {
                                    val vel = velocityTracker.calculateVelocity().x
                                    val curProgress = if (maxDistPx > 0f) dragX / maxDistPx else 0f
                                    isDragging = false
                                    if (slideShouldCommit(curProgress, vel)) {
                                        scope.launch {
                                            xAnim.snapTo(dragX)
                                            commit(maxDistPx, vel)
                                        }
                                    } else {
                                        scope.launch {
                                            xAnim.snapTo(dragX)
                                            settleBack(vel)
                                        }
                                    }
                                }
                                break
                            }

                            if (change.positionChanged()) {
                                val dx = change.position.x - startTouchX
                                if (!draggingStarted && abs(dx) > with(density) { 3.dp.toPx() }) {
                                    draggingStarted = true
                                    isDragging = true
                                    phase = SlidePhase.DRAGGING
                                    dragX = xAnim.value
                                }

                                if (draggingStarted) {
                                    change.consume()
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    val targetX = change.position.x - grabOffset
                                    val banded = slideBand(targetX, maxDistPx)
                                    dragX = banded

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
            // Track Background
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
                    .padding(start = 28.dp)
                    .graphicsLayer { alpha = emptyLabelOpacity },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 14.5.sp,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                    maxLines = 1
                )
            }

            // 3. Ink Fill Layer (custom colored when done, monochrome dark/light during drag)
            val inkBgColor = if (isDone) {
                if (dark) confirmationType.darkColor else confirmationType.lightColor
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

            // Ink Label
            val inkTextColor = if (isDone) {
                Color.White
            } else {
                if (dark) Zinc900 else Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = if (phase == SlidePhase.IDLE || phase == SlidePhase.DRAGGING) 28.dp else 0.dp)
                    .padding(end = if (isBusy || isDone) 28.dp else 0.dp)
                .graphicsLayer { alpha = fillOpacity },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, easing = SLIDE_EASE_OUT)) +
                                slideInVertically(
                                    animationSpec = tween(220, easing = SLIDE_EASE_OUT),
                                    initialOffsetY = { 8 }
                                )) togetherWith
                                (fadeOut(animationSpec = tween(220, easing = SLIDE_EASE_OUT)) +
                                        slideOutVertically(
                                            animationSpec = tween(220, easing = SLIDE_EASE_OUT),
                                            targetOffsetY = { -8 }
                                        ))
                    },
                    label = "inkStageLabel"
                ) { targetPhase ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        if (targetPhase == SlidePhase.CONFIRMING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = inkTextColor,
                                strokeWidth = 2.dp
                            )
                        } else if (targetPhase == SlidePhase.DONE) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = inkTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        val text = when (targetPhase) {
                            SlidePhase.CONFIRMING -> confirmingLabel
                            SlidePhase.DONE -> effectiveDoneLabel
                            else -> label
                        }
                        Text(
                            text = text,
                            fontSize = 14.5.sp,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            color = inkTextColor,
                            maxLines = 1
                        )
                    }
                }
            }

            // 4. Draggable Thumb (Circle 48dp)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (insetPx + currentX).roundToInt(),
                            y = insetPx.roundToInt()
                        )
                    }
                    .size(SLIDE_THUMB.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isDone) {
                            if (dark) Color.White else Color.White
                        } else {
                            if (dark) Zinc100 else Zinc900
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                val thumbIconColor = if (isDone) {
                    if (dark) Emerald600 else Emerald600
                } else {
                    if (dark) Zinc900 else Color.White
                }

                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = thumbIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = thumbIconColor,
                        strokeWidth = 2.2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Slide thumb",
                        tint = thumbIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
