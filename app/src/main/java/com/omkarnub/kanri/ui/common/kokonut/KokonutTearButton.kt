package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.semantics.onClick
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 1:1 Port of Kokonut UI Tear Button (A5. tear-button.tsx)
 *
 * Exact constants:
 * HEIGHT = 68, STUB_WIDTH = 76, TEAR_ANGLE = 34 deg, HOLES = 7
 * HOLD = 0.55, HOLD_SHARE = 0.3
 * SWING_STIFFNESS = 900, SWING_DAMPING = 42
 * RETURN_STIFFNESS = 520, RETURN_DAMPING = 22
 * GRAVITY = 2600, FALL_DISTANCE = 220, STEP = 1/120, NOTCH = 7
 * FIBRES = [1.6, 0.4, 2.2, 0.9, 1.8, 0.2, 2.4, 1.1, 0.5, 2, 1.4, 0.3, 2.3, 0.8, 1.7, 0.1, 2.1, 1.2, 0.6, 1.9]
 */

const val TEAR_HEIGHT = 68f
const val TEAR_STUB_WIDTH = 76f
const val TEAR_ANGLE = 34f
const val TEAR_HOLES = 7
const val TEAR_HOLD = 0.55f
const val TEAR_HOLD_SHARE = 0.3f
const val TEAR_SWING_STIFFNESS = 900f
const val TEAR_SWING_DAMPING = 42f
const val TEAR_RETURN_STIFFNESS = 520f
const val TEAR_RETURN_DAMPING = 22f
const val TEAR_GRAVITY = 2600f
const val TEAR_FALL_DISTANCE = 220f
const val TEAR_STEP = 1f / 120f
const val TEAR_NOTCH = 7f

val TEAR_FIBRES = listOf(
    1.6f, 0.4f, 2.2f, 0.9f, 1.8f, 0.2f, 2.4f, 1.1f, 0.5f, 2f,
    1.4f, 0.3f, 2.3f, 0.8f, 1.7f, 0.1f, 2.1f, 1.2f, 0.6f, 1.9f
)

val TEAR_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

enum class TearPhase {
    IDLE, TEARING, FLYING, TORN
}

enum class TearTone {
    DEFAULT, MUTED, DANGER
}

/** Pure stickSlip math matching Kokonut UI */
fun tearStickSlip(pull: Float): Float {
    val scaled = pull.coerceIn(0f, 1f) * TEAR_HOLES
    val hole = min(floor(scaled).toInt(), TEAR_HOLES - 1)
    val f = scaled - hole
    val given = if (f < TEAR_HOLD) {
        (f / TEAR_HOLD) * TEAR_HOLD_SHARE
    } else {
        TEAR_HOLD_SHARE + ((f - TEAR_HOLD) / (1f - TEAR_HOLD)) * (1f - TEAR_HOLD_SHARE)
    }
    return (hole + given) / TEAR_HOLES
}

/** Pure tornEdge points list matching Kokonut UI */
fun tearTornEdgePoints(tip: Float, width: Float, side: Int): List<Pair<Float, Float>> {
    val points = mutableListOf<Pair<Float, Float>>()
    val edge = if (side == 1) width else 0f
    val far = if (side == 1) 0f else width

    points.add(Pair(far, 0f))
    val rows = ceil((tip / TEAR_HEIGHT) * TEAR_FIBRES.size).toInt()
    for (k in 0..rows) {
        val y = min((k.toFloat() / TEAR_FIBRES.size) * TEAR_HEIGHT, tip)
        val fibre = TEAR_FIBRES[k % TEAR_FIBRES.size]
        val x = edge - side * fibre
        points.add(Pair(x, y))
    }
    points.add(Pair(edge, tip))
    points.add(Pair(edge, TEAR_HEIGHT))
    points.add(Pair(far, TEAR_HEIGHT))
    return points
}

@Composable
fun KokonutTearButton(
    title: String,
    subtitle: String,
    confirmedTitle: String = "Settled",
    confirmedSubtitle: String = "Marked as settled",
    tone: TearTone = TearTone.DEFAULT,
    isSettled: Boolean = false,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val density = LocalDensity.current

    var phase by remember { mutableStateOf(if (isSettled) TearPhase.TORN else TearPhase.IDLE) }
    var angle by remember { mutableFloatStateOf(0f) }
    var speed by remember { mutableFloatStateOf(0f) }
    var target by remember { mutableFloatStateOf(0f) }
    var isHeld by remember { mutableStateOf(false) }
    var auto by remember { mutableIntStateOf(0) }
    var holeIdx by remember { mutableIntStateOf(0) }

    // Flight physics state
    var flightX by remember { mutableFloatStateOf(0f) }
    var flightY by remember { mutableFloatStateOf(0f) }
    var flightVx by remember { mutableFloatStateOf(0f) }
    var flightVy by remember { mutableFloatStateOf(0f) }
    var flightSpin by remember { mutableFloatStateOf(0f) }

    // Hand velocity tracking
    var handPvx by remember { mutableFloatStateOf(0f) }
    var handPvy by remember { mutableFloatStateOf(0f) }
    var grabAngle by remember { mutableFloatStateOf(0f) }

    val isDone = phase == TearPhase.FLYING || phase == TearPhase.TORN

    // Fixed 1/120s Integrator Loop
    LaunchedEffect(phase, isHeld, auto) {
        if (phase == TearPhase.IDLE || phase == TearPhase.TORN) return@LaunchedEffect

        var lastTime = System.nanoTime()
        var carry = 0f

        while (isActive) {
            val now = System.nanoTime()
            val elapsed = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
            lastTime = now
            carry += elapsed

            while (carry >= TEAR_STEP) {
                carry -= TEAR_STEP

                if (phase == TearPhase.FLYING) {
                    flightVy += TEAR_GRAVITY * TEAR_STEP
                    flightX += flightVx * TEAR_STEP
                    flightY += flightVy * TEAR_STEP
                    angle += flightSpin * TEAR_STEP
                    if (flightY > TEAR_FALL_DISTANCE) {
                        phase = TearPhase.TORN
                        break
                    }
                } else if (auto == 1) {
                    target = min(target + TEAR_STEP * 60f, TEAR_ANGLE * 1.05f)
                    val force = (target - angle) * TEAR_SWING_STIFFNESS - speed * TEAR_SWING_DAMPING
                    speed += force * TEAR_STEP
                    angle += speed * TEAR_STEP

                    val curHole = floor((angle / TEAR_ANGLE).coerceIn(0f, 1f) * TEAR_HOLES).toInt()
                    if (curHole > holeIdx) {
                        haptics.tick()
                        holeIdx = curHole
                    }

                    if (angle >= TEAR_ANGLE * 0.995f) {
                        // Detach
                        auto = 0
                        haptics.warning()
                        val rad = (angle * PI.toFloat()) / 180f
                        val tangential = ((speed * PI.toFloat()) / 180f) * TEAR_HEIGHT
                        flightX = 0f
                        flightY = 0f
                        flightVx = cos(rad) * tangential + 90f
                        flightVy = sin(rad) * tangential - 120f
                        flightSpin = speed * 0.6f + 140f
                        phase = TearPhase.FLYING
                        onConfirm()
                        break
                    }
                } else {
                    val stiffness = if (isHeld) TEAR_SWING_STIFFNESS else TEAR_RETURN_STIFFNESS
                    val damping = if (isHeld) TEAR_SWING_DAMPING else TEAR_RETURN_DAMPING
                    val force = (target - angle) * stiffness - speed * damping
                    speed += force * TEAR_STEP
                    angle += speed * TEAR_STEP

                    val curHole = floor((angle / TEAR_ANGLE).coerceIn(0f, 1f) * TEAR_HOLES).toInt()
                    if (curHole > holeIdx) {
                        haptics.tick()
                        holeIdx = curHole
                    }

                    if (angle >= TEAR_ANGLE * 0.995f && isHeld) {
                        // Detach
                        isHeld = false
                        haptics.warning()
                        val rad = (angle * PI.toFloat()) / 180f
                        val tangential = ((speed * PI.toFloat()) / 180f) * TEAR_HEIGHT
                        flightX = 0f
                        flightY = 0f
                        flightVx = cos(rad) * tangential + handPvx.coerceIn(-900f, 900f) * 0.6f + 90f
                        flightVy = sin(rad) * tangential + handPvy.coerceIn(-900f, 900f) * 0.6f - 120f
                        flightSpin = speed * 0.6f + 140f
                        phase = TearPhase.FLYING
                        onConfirm()
                        break
                    } else if (!isHeld && abs(angle) < 0.02f && abs(speed) < 0.05f) {
                        angle = 0f
                        speed = 0f
                        phase = TearPhase.IDLE
                        break
                    }
                }
            }
            delay(8)
        }
    }

    // Tone colors from table
    val paperColor = when (tone) {
        TearTone.DEFAULT -> if (dark) Zinc100 else Zinc900
        TearTone.MUTED -> if (dark) Zinc800 else Zinc100
        TearTone.DANGER -> Color(0xFFDC2626)
    }
    val textColor = when (tone) {
        TearTone.DEFAULT -> if (dark) Zinc900 else Color.White
        TearTone.MUTED -> if (dark) Zinc100 else Zinc900
        TearTone.DANGER -> Color.White
    }
    val subColor = when (tone) {
        TearTone.DEFAULT -> if (dark) Zinc900.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
        TearTone.MUTED -> if (dark) Zinc400 else Zinc500
        TearTone.DANGER -> Color.White.copy(alpha = 0.70f)
    }
    val chipBg = when (tone) {
        TearTone.DEFAULT -> if (dark) Zinc900.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.10f)
        TearTone.MUTED -> if (dark) Color.White.copy(alpha = 0.10f) else Zinc900.copy(alpha = 0.05f)
        TearTone.DANGER -> Color.White.copy(alpha = 0.15f)
    }
    val chipDoneBg = when (tone) {
        TearTone.DEFAULT -> if (dark) Zinc900 else Color.White
        TearTone.MUTED -> if (dark) Color.White else Zinc900
        TearTone.DANGER -> Color.White
    }
    val chipDoneTextColor = when (tone) {
        TearTone.DEFAULT -> if (dark) Color.White else Zinc900
        TearTone.MUTED -> if (dark) Zinc900 else Color.White
        TearTone.DANGER -> Color(0xFFDC2626)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val stubWidthPx = with(density) { TEAR_STUB_WIDTH.dp.toPx() }
        val bodyWidthPx = max(0f, totalWidthPx - stubWidthPx)
        val heightPx = with(density) { TEAR_HEIGHT.dp.toPx() }
        val notchRadiusPx = with(density) { TEAR_NOTCH.dp.toPx() }

        val tipPx = ((angle / TEAR_ANGLE).coerceIn(0f, 1f)) * heightPx

        // 1. Ticket Body (left side with rounded-l-16dp)
        Box(
            modifier = Modifier
                .width(with(density) { bodyWidthPx.toDp() })
                .height(68.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(paperColor)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Top-right and bottom-right notch cutouts
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = notchRadiusPx,
                    center = Offset(size.width, 0f)
                )
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = notchRadiusPx,
                    center = Offset(size.width, size.height)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 36dp round chip (icon or check)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDone) chipDoneBg else chipBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = chipDoneTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Handshake,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Text block cross-fading
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = isDone,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(300, easing = TEAR_EASE_OUT)) +
                                    slideInVertically(
                                        animationSpec = tween(300, easing = TEAR_EASE_OUT),
                                        initialOffsetY = { 4 }
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(300, easing = TEAR_EASE_OUT)) +
                                            slideOutVertically(
                                                animationSpec = tween(300, easing = TEAR_EASE_OUT),
                                                targetOffsetY = { -4 }
                                            ))
                        },
                        label = "ticketTextTransition"
                    ) { done ->
                        Column {
                            Text(
                                text = if (done) confirmedTitle else title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (done) confirmedSubtitle else subtitle,
                                fontSize = 12.sp,
                                color = subColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Tear Stub (right side with rounded-r-16dp)
        if (phase != TearPhase.TORN) {
            val stubOpacity = if (phase == TearPhase.FLYING) {
                (1f - (flightY - TEAR_FALL_DISTANCE * 0.5f) / (TEAR_FALL_DISTANCE * 0.5f)).coerceIn(0f, 1f)
            } else 1f

            val stubTranslateX = if (phase == TearPhase.FLYING) flightX else 0f
            val stubTranslateY = if (phase == TearPhase.FLYING) flightY else 0f

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (bodyWidthPx + stubTranslateX).roundToInt(),
                            y = stubTranslateY.roundToInt()
                        )
                    }
                    .width(76.dp)
                    .height(68.dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 1f)
                        rotationZ = angle
                        alpha = stubOpacity
                    }
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    .background(paperColor)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Mark as settled: Pull down or tap to tear off stub"
                        onClick {
                            if (phase == TearPhase.IDLE) {
                                auto = 1
                                target = 0f
                                phase = TearPhase.TEARING
                            }
                            true
                        }
                    }
                    .pointerInput(phase) {
                        if (phase == TearPhase.FLYING || phase == TearPhase.TORN) return@pointerInput

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            val startY = down.position.y
                            var lastT = System.currentTimeMillis()
                            var hasMoved = false

                            phase = TearPhase.TEARING
                            isHeld = true
                            grabAngle = (atan2(startY - heightPx, startX) * 180f / PI.toFloat()) - angle
                            handPvx = 0f
                            handPvy = 0f

                            var prevX = startX
                            var prevY = startY

                            while (isHeld) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (change.changedToUp()) {
                                    change.consume()
                                    isHeld = false
                                    if (!hasMoved) {
                                        // Tap or click on stub triggers auto-tear
                                        if (phase == TearPhase.TEARING || phase == TearPhase.IDLE) {
                                            auto = 1
                                            target = 0f
                                        }
                                    } else {
                                        target = 0f
                                    }
                                    break
                                }

                                if (change.positionChanged()) {
                                    val curX = change.position.x
                                    val curY = change.position.y
                                    val dx = curX - startX
                                    val dy = curY - startY
                                    if (dx * dx + dy * dy > 16f) {
                                        hasMoved = true
                                    }
                                    change.consume()
                                    val nowT = System.currentTimeMillis()
                                    val dt = nowT - lastT
                                    if (dt > 0) {
                                        val dX = curX - prevX
                                        val dY = curY - prevY
                                        handPvx = handPvx * 0.5f + (dX / dt) * 500f
                                        handPvy = handPvy * 0.5f + (dY / dt) * 500f
                                    }
                                    lastT = nowT
                                    prevX = curX
                                    prevY = curY

                                    val raw = (atan2(curY - heightPx, curX) * 180f / PI.toFloat()) - grabAngle
                                    val pull = if (raw < 0f) raw * 0.15f else raw
                                    target = if (pull >= TEAR_ANGLE) {
                                        TEAR_ANGLE * 1.05f
                                    } else {
                                        tearStickSlip(max(pull, 0f) / TEAR_ANGLE) * TEAR_ANGLE + min(pull, 0f)
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Perforation dotted line + notches
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Top-left and bottom-left notches
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = notchRadiusPx,
                        center = Offset(0f, 0f)
                    )
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.15f),
                        radius = notchRadiusPx,
                        center = Offset(0f, size.height)
                    )

                    // 2dp dotted perforation line, inset 10dp top and bottom
                    drawLine(
                        color = subColor.copy(alpha = 0.4f),
                        start = Offset(0f, 10.dp.toPx()),
                        end = Offset(0f, size.height - 10.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = subColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Tear",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = subColor
                    )
                }
            }
        }
    }
}

