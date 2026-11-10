package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.isActive
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * 1:1 Port of Kokonut UI Spending Ring (A1. spending-ring.tsx)
 *
 * Exact constants:
 * SIZE = 240, C = 120, R = 90, STROKE = 22, SELECTED_STROKE = 30
 * GAP = 2.4 deg, RATE = 11, DAMPING = 0.78, PROJECT = 0.2s, TAP_SLOP = 5px
 */

const val SPENDING_RING_SIZE = 240f
const val SPENDING_RING_C = 120f
const val SPENDING_RING_R = 90f
const val SPENDING_RING_STROKE = 22f
const val SPENDING_RING_SELECTED_STROKE = 30f
const val SPENDING_RING_GAP = 2.4f
const val SPENDING_RING_RATE = 11f
const val SPENDING_RING_DAMPING = 0.78f
const val SPENDING_RING_PROJECT = 0.2f
const val SPENDING_RING_TAP_SLOP = 5f

val KOKONUT_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

// Pure neutral monochrome scale (equal RGB, zero blue/cool tint)
val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF5F5F5)
val Zinc200 = Color(0xFFE5E5E5)
val Zinc300 = Color(0xFFD4D4D4)
val Zinc400 = Color(0xFFA3A3A3)
val Zinc500 = Color(0xFF737373)
val Zinc600 = Color(0xFF525252)
val Zinc700 = Color(0xFF404040)
val Zinc800 = Color(0xFF262626)
val Zinc900 = Color(0xFF171717)
val Zinc950 = Color(0xFF0A0A0A)

val Emerald600 = Color(0xFF059669)
val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)

val Rose600 = Color(0xFFE11D48)
val Rose500 = Color(0xFFF43F5E)
val Rose400 = Color(0xFFFB7185)

val Sky500 = Color(0xFF0EA5E9)
val Sky400 = Color(0xFF38BDF8)

val Amber400 = Color(0xFFFBBF24)
val Amber300 = Color(0xFFFCD34D)

val Violet500 = Color(0xFF8B5CF6)
val Violet400 = Color(0xFFA78BFA)

data class SpendingCategory(
    val name: String,
    val amount: Double,
    val change: Double? = null,
    val color: Color = Color.Unspecified
)

data class SliceSpan(
    val from: Float,
    val to: Float,
    val mid: Float
)

/** Wraps an angle into -180..180. */
fun spendingRingWrap(deg: Float): Float = ((((deg + 180f) % 360f) + 360f) % 360f) - 180f

/** A point on the ring, with 0 deg at the top and angles running clockwise. */
fun spendingRingPolar(deg: Float, r: Float = SPENDING_RING_R, c: Float = SPENDING_RING_C): Pair<Float, Float> {
    val rad = (deg * PI.toFloat()) / 180f
    return Pair(c + r * kotlin.math.sin(rad), c - r * kotlin.math.cos(rad))
}

/** SVG Arc Path representation matching Kokonut arc() */
fun spendingRingArc(from: Float, to: Float, r: Float = SPENDING_RING_R, c: Float = SPENDING_RING_C): String {
    val p0 = spendingRingPolar(from, r, c)
    val p1 = spendingRingPolar(to, r, c)
    val large = if (to - from > 180f) 1 else 0
    fun f(v: Float) = String.format(Locale.US, "%.2f", v)
    val rInt = r.toInt()
    return "M${f(p0.first)} ${f(p0.second)} A$rInt $rInt 0 $large 1 ${f(p1.first)} ${f(p1.second)}"
}

/** The category whose slice covers an angle on the ring. */
fun spendingRingSliceAt(spans: List<SliceSpan>, deg: Float): Int {
    if (spans.isEmpty()) return 0
    val a = ((deg % 360f) + 360f) % 360f
    val index = spans.indexOfFirst { a >= it.from && a < it.to }
    return if (index == -1) 0 else index
}

/** One step of a damped spring, in small slices for stability. */
fun spendingRingSpringStep(state: FloatArray, target: Float, dt: Float): Boolean {
    val slices = ceil(dt / (1f / 240f)).toInt().coerceAtLeast(1)
    val h = dt / slices
    for (i in 0 until slices) {
        val accel = -SPENDING_RING_RATE * SPENDING_RING_RATE * (state[0] - target) - 2f * SPENDING_RING_DAMPING * SPENDING_RING_RATE * state[1]
        state[1] += accel * h
        state[0] += state[1] * h
    }
    return abs(state[0] - target) < 0.05f && abs(state[1]) < 2f
}

/**
 * Standard palette cycled for categories: Housing, Groceries, Shopping, Dining out, Transport, Subscriptions
 */
@Composable
fun getKokonutPalette(): List<Color> {
    val dark = isAppInDarkTheme()
    val tokens = LocalKanriGlass.current
    return remember(dark) {
        if (dark) {
            listOf(Sky400, Emerald400, Rose400, Amber300, Violet400, Zinc500)
        } else {
            listOf(Sky500, Emerald500, Rose500, Amber400, Violet500, Zinc400)
        }
    }
}

private val currencyFormatter2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
private val currencyFormatter0 = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

fun formatMoney2(amount: Double): String = "₹" + currencyFormatter2.format(amount)
fun formatMoney0(amount: Double): String = "₹" + currencyFormatter0.format(amount)

@Composable
fun KokonutSpendingRing(
    title: String = "Spending Ring",
    categories: List<SpendingCategory>,
    modifier: Modifier = Modifier
) {
    val dark = isAppInDarkTheme()
    val tokens = LocalKanriGlass.current
    val palette = getKokonutPalette()
    val haptics = rememberKanriHaptics()
    val density = LocalDensity.current

    val resolvedCategories = remember(categories, palette) {
        categories.mapIndexed { idx, cat ->
            if (cat.color != Color.Unspecified && cat.color != Color.Transparent) cat
            else cat.copy(color = palette[idx % palette.size])
        }
    }

    val total = remember(resolvedCategories) { resolvedCategories.sumOf { it.amount } }

    val spans = remember(resolvedCategories, total) {
        var at = 0f
        resolvedCategories.map { c ->
            val sweep = if (total > 0) (c.amount / total * 360f).toFloat() else (360f / resolvedCategories.size.coerceAtLeast(1))
            val span = SliceSpan(from = at, to = at + sweep, mid = at + sweep / 2f)
            at += sweep
            span
        }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var currentAngle by remember { mutableFloatStateOf(0f) }
    var targetAngle by remember { mutableFloatStateOf(0f) }
    var angularVelocity by remember { mutableFloatStateOf(0f) }
    var isCoasting by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    // Synchronize initial slice angle to point under top marker (12 o'clock = mid angle negated)
    LaunchedEffect(spans) {
        if (spans.isNotEmpty()) {
            val safeIndex = selectedIndex.coerceIn(0, spans.size - 1)
            val goal = -(spans[safeIndex].mid)
            if (abs(spendingRingWrap(goal - targetAngle)) > 0.001f) {
                currentAngle = goal
                targetAngle = goal
                angularVelocity = 0f
                selectedIndex = safeIndex
            }
        }
    }

    fun turnTo(index: Int, rest: Float = currentAngle, coast: Boolean = false) {
        if (spans.isEmpty()) return
        val safeIndex = index.coerceIn(0, spans.size - 1)
        val goal = -(spans[safeIndex].mid)
        targetAngle = rest + spendingRingWrap(goal - rest)
        isCoasting = coast
        if (!coast) {
            selectedIndex = safeIndex
            haptics.click()
        }
    }

    // Spring integrator loop
    LaunchedEffect(targetAngle, isDragging) {
        if (isDragging) return@LaunchedEffect
        var lastTime = System.nanoTime()
        val state = floatArrayOf(currentAngle, angularVelocity)

        while (isActive) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(1f / 240f, 1f / 30f)
            lastTime = now

            val settled = spendingRingSpringStep(state, targetAngle, dt)
            currentAngle = if (settled) targetAngle else state[0]
            angularVelocity = if (settled) 0f else state[1]

            if (isCoasting) {
                val hovered = spendingRingSliceAt(spans, -currentAngle)
                if (hovered != selectedIndex && hovered in resolvedCategories.indices) {
                    selectedIndex = hovered
                    haptics.tick()
                }
            }

            if (settled) {
                currentAngle = targetAngle
                isCoasting = false
                val finalIdx = spendingRingSliceAt(spans, -currentAngle).coerceIn(0, (resolvedCategories.size - 1).coerceAtLeast(0))
                if (selectedIndex != finalIdx) {
                    selectedIndex = finalIdx
                }
                break
            }
            kotlinx.coroutines.delay(8)
        }
    }

    val current = resolvedCategories.getOrNull(selectedIndex)
    val share = if (total > 0 && current != null) (current.amount / total * 100.0) else 0.0
    val change = current?.change

    // Card matching Kokonut UI: max width 384dp (max-w-sm), rounded 24dp (rounded-3xl), 1dp border zinc-200/800, bg white/zinc-950, p-5
    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row: baseline aligned, space-between
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                )
                Text(
                    text = formatMoney2(total),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Ring area: max 240dp, aspect square, centered
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.CenterHorizontally)
                    .semantics {
                        role = Role.Tab
                        stateDescription = current?.let { "${it.name}, ${formatMoney2(it.amount)}" } ?: ""
                        customActions = listOf(
                            CustomAccessibilityAction("Next Category") {
                                if (resolvedCategories.isNotEmpty()) {
                                    turnTo((selectedIndex + 1) % resolvedCategories.size)
                                    true
                                } else false
                            },
                            CustomAccessibilityAction("Previous Category") {
                                if (resolvedCategories.isNotEmpty()) {
                                    turnTo((selectedIndex - 1 + resolvedCategories.size) % resolvedCategories.size)
                                    true
                                } else false
                            }
                        )
                    }
                    .pointerInput(spans, resolvedCategories) {
                        var dragStartAngle = 0f
                        var lastAngle = 0f
                        var lastTime = 0L
                        var movedDist = 0f
                        var startX = 0f
                        var startY = 0f

                        awaitPointerEventScope {
                            while (true) {
                                val downEvent = awaitPointerEvent()
                                val downChange = downEvent.changes.firstOrNull { it.pressed } ?: continue
                                val centerPx = size.width / 2f
                                startX = downChange.position.x
                                startY = downChange.position.y
                                val relX = startX - centerPx
                                val relY = startY - centerPx

                                val pointerAngleDeg = (atan2(relX.toDouble(), -relY.toDouble()) * 180.0 / PI).toFloat()
                                dragStartAngle = pointerAngleDeg
                                lastAngle = pointerAngleDeg
                                lastTime = System.currentTimeMillis()
                                movedDist = 0f
                                isDragging = true
                                isCoasting = false
                                angularVelocity = 0f

                                var dragOngoing = true
                                while (dragOngoing) {
                                    val moveEvent = awaitPointerEvent()
                                    val moveChange = moveEvent.changes.firstOrNull { it.id == downChange.id }
                                    if (moveChange == null || !moveChange.pressed) {
                                        dragOngoing = false
                                        break
                                    }

                                    val currX = moveChange.position.x
                                    val currY = moveChange.position.y
                                    val dist = hypot(currX - startX, currY - startY)
                                    movedDist = max(movedDist, dist)

                                    if (movedDist >= SPENDING_RING_TAP_SLOP) {
                                        moveChange.consume()
                                        val cX = currX - centerPx
                                        val cY = currY - centerPx
                                        val angle = (atan2(cX.toDouble(), -cY.toDouble()) * 180.0 / PI).toFloat()
                                        val delta = spendingRingWrap(angle - lastAngle)
                                        val nowT = System.currentTimeMillis()
                                        val dt = max((nowT - lastTime) / 1000f, 1f / 240f)

                                        currentAngle += delta
                                        angularVelocity = angularVelocity * 0.6f + (delta / dt) * 0.4f
                                        lastAngle = angle
                                        lastTime = nowT

                                        val liveSlice = spendingRingSliceAt(spans, -currentAngle)
                                        if (liveSlice in resolvedCategories.indices && liveSlice != selectedIndex) {
                                            selectedIndex = liveSlice
                                            haptics.tick()
                                        }
                                    }
                                }

                                isDragging = false
                                val nowT = System.currentTimeMillis()

                                if (movedDist < SPENDING_RING_TAP_SLOP) {
                                    // Tap on ring band (not center)
                                    val r = hypot(startX - centerPx, startY - centerPx)
                                    val innerR = ((SPENDING_RING_R - SPENDING_RING_STROKE) / SPENDING_RING_SIZE) * size.width
                                    if (r > innerR) {
                                        val tapSlice = spendingRingSliceAt(spans, dragStartAngle - currentAngle)
                                        turnTo(tapSlice)
                                    }
                                } else {
                                    // Flick release
                                    if (nowT - lastTime > 80) {
                                        angularVelocity = 0f
                                    }
                                    angularVelocity = angularVelocity.coerceIn(-1440f, 1440f)
                                    val rest = currentAngle + angularVelocity * SPENDING_RING_PROJECT
                                    val targetSlice = spendingRingSliceAt(spans, -rest)
                                    turnTo(targetSlice, rest, coast = true)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Canvas drawing the ring slices and the top marker
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasSize = size.width
                    val scale = canvasSize / SPENDING_RING_SIZE
                    val centerPx = canvasSize / 2f
                    val baseRadiusPx = SPENDING_RING_R * scale
                    val baseStrokePx = SPENDING_RING_STROKE * scale
                    val selectedStrokePx = SPENDING_RING_SELECTED_STROKE * scale

                    // Group rotation
                    rotate(degrees = currentAngle, pivot = Offset(centerPx, centerPx)) {
                        spans.forEachIndexed { i, span ->
                            val cat = resolvedCategories.getOrNull(i) ?: return@forEachIndexed
                            val isSelected = (i == selectedIndex)
                            val sliceStroke = if (isSelected) selectedStrokePx else baseStrokePx
                            val sliceAlpha = if (isSelected) 1f else (if (dark) 0.5f else 0.4f)

                            val arcPadding = sliceStroke / 2f
                            val arcRadius = baseRadiusPx
                            val arcTopLeft = Offset(centerPx - arcRadius, centerPx - arcRadius)
                            val arcSize = Size(arcRadius * 2f, arcRadius * 2f)

                            // 0 deg in polar is top (12 o'clock), which is -90 in Compose drawArc
                            val startAngle = -90f + span.from + (SPENDING_RING_GAP / 2f)
                            val sweepAngle = (span.to - span.from) - SPENDING_RING_GAP

                            if (sweepAngle > 0f) {
                                drawArc(
                                    color = cat.color.copy(alpha = sliceAlpha),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = arcTopLeft,
                                    size = arcSize,
                                    style = Stroke(width = sliceStroke, cap = StrokeCap.Butt)
                                )
                            }
                        }
                    }

                    // Top marker triangle: (C-5, 2) (C+5, 2) (C, 8)
                    val markerPath = Path().apply {
                        moveTo((SPENDING_RING_C - 5f) * scale, 2f * scale)
                        lineTo((SPENDING_RING_C + 5f) * scale, 2f * scale)
                        lineTo(SPENDING_RING_C * scale, 8f * scale)
                        close()
                    }
                    drawPath(
                        path = markerPath,
                        color = if (dark) Zinc100 else Zinc900
                    )
                }

                // Center non-interactive readout
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = current,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200, easing = KOKONUT_EASE_OUT)) +
                                    slideInVertically(
                                        animationSpec = tween(200, easing = KOKONUT_EASE_OUT),
                                        initialOffsetY = { 8 }
                                    )) togetherWith fadeOut(animationSpec = tween(150, easing = KOKONUT_EASE_OUT))
                        },
                        label = "readoutTransition"
                    ) { cat ->
                        if (cat != null) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Dot + Name line (12sp zinc-500/400)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (dark) Zinc400 else Zinc500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Amount: 24sp semibold tabular tracking -0.025em
                                Text(
                                    text = formatMoney2(cat.amount),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (dark) Zinc100 else Zinc900,
                                    letterSpacing = (-0.025).sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Share line: "{share}% of total" 12sp zinc-500/400
                                Text(
                                    text = "${share.toInt()}% of total",
                                    fontSize = 12.sp,
                                    color = if (dark) Zinc400 else Zinc500
                                )

                                // Change line (if change data exists)
                                if (cat.change != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val (changeText, changeColor) = when {
                                        cat.change > 0 -> Pair(
                                            "▲ ${String.format(Locale.US, "%.1f", cat.change)}% vs last month",
                                            if (dark) Rose400 else Rose600
                                        )
                                        cat.change < 0 -> Pair(
                                            "▼ ${String.format(Locale.US, "%.1f", abs(cat.change))}% vs last month",
                                            if (dark) Emerald400 else Emerald600
                                        )
                                        else -> Pair(
                                            "Same as last month",
                                            if (dark) Zinc500 else Zinc400
                                        )
                                    }
                                    Text(
                                        text = changeText,
                                        fontSize = 12.sp,
                                        color = changeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend: 2-column grid, gap 4dp, each row px 8 py 6 rounded 8, 12sp gap 8: [8dp dot][name flex 1 ellipsize][whole amount tabular]
            val rows = resolvedCategories.chunked(2)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rows.forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowCategories.forEach { cat ->
                            val catIdx = resolvedCategories.indexOf(cat)
                            val isSelected = (catIdx == selectedIndex)
                            val rowBg = if (isSelected) {
                                if (tokens != null) tokens.tintFlat else (if (dark) Zinc900 else Zinc100)
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(rowBg)
                                    .clickable {
                                        turnTo(catIdx)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(cat.color)
                                    )
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isSelected) (tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900) else (tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500),
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatMoney0(cat.amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = if (isSelected) (tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900) else (tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500)
                                    )
                                }
                            }
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
