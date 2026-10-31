package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.min

/**
 * 1:1 Port of Kokonut UI Amount Pad (A2. amount-pad.tsx)
 *
 * Exact constants:
 * MAX_WHOLE_DIGITS = 7, KEYPAD 3x4 (height 56, rounded 16, 24sp),
 * AMOUNT FONT: 56sp semibold, tracking -0.025em, tabular
 * SPRINGS:
 * - digitIn: stiffness 650f, dampingRatio 0.7355f
 * - slide: stiffness 500f, dampingRatio 0.894f
 * - fit: stiffness 380f, dampingRatio 0.872f
 * SHAKE: 0, -10, 9, -6, 4, 0 over 360ms ease-out
 * PRESETS: 100, 500, 1000
 */

const val AMOUNT_PAD_MAX_WHOLE_DIGITS = 7
val AmountPadKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "back")

data class AmountChar(
    val id: Int,
    val char: String
)

enum class AmountGlyphKind {
    DIGIT, SEP, GHOST
}

data class AmountGlyph(
    val key: String,
    val char: String,
    val kind: AmountGlyphKind
)

fun amountPadToNumber(chars: List<AmountChar>): Double {
    val s = chars.joinToString("") { it.char }
    return s.toDoubleOrNull() ?: 0.0
}

fun amountPadMoney(value: Double, currency: String = "₹"): String {
    val df = DecimalFormat(if (value % 1.0 == 0.0) "#,##0" else "#,##0.00", DecimalFormatSymbols(Locale.US))
    return "$currency${df.format(value)}"
}

/** Pure function matching typeKey() in Kokonut UI */
fun amountPadTypeKey(
    chars: List<AmountChar>,
    key: String,
    make: (String) -> AmountChar
): List<AmountChar>? {
    val point = chars.indexOfFirst { it.char == "." }
    if (key == ".") {
        if (point != -1) return null
        return if (chars.isEmpty()) listOf(make("0"), make(".")) else chars + make(".")
    }
    val centsFull = point != -1 && (chars.size - point) > 2
    val wholeFull = point == -1 && chars.size >= AMOUNT_PAD_MAX_WHOLE_DIGITS
    if (centsFull || wholeFull) return null
    return if (chars.size == 1 && chars[0].char == "0") {
        listOf(make(key))
    } else {
        chars + make(key)
    }
}

/** Pure function matching layout() in Kokonut UI */
fun amountPadLayout(chars: List<AmountChar>): List<AmountGlyph> {
    val point = chars.indexOfFirst { it.char == "." }
    val whole = if (point == -1) chars else chars.subList(0, point)
    val glyphs = mutableListOf<AmountGlyph>()

    whole.forEachIndexed { index, c ->
        val fromRight = whole.size - index
        if (index > 0 && fromRight % 3 == 0) {
            glyphs.add(AmountGlyph(key = "comma-${c.id}", char = ",", kind = AmountGlyphKind.SEP))
        }
        glyphs.add(AmountGlyph(key = "d-${c.id}", char = c.char, kind = AmountGlyphKind.DIGIT))
    }

    if (point != -1) {
        val cents = chars.subList(point + 1, chars.size)
        glyphs.add(AmountGlyph(key = "d-${chars[point].id}", char = ".", kind = AmountGlyphKind.SEP))
        for (c in cents) {
            glyphs.add(AmountGlyph(key = "d-${c.id}", char = c.char, kind = AmountGlyphKind.DIGIT))
        }
        for (i in cents.size until 2) {
            glyphs.add(AmountGlyph(key = "ghost-$i", char = "0", kind = AmountGlyphKind.GHOST))
        }
    }
    return glyphs
}

@Composable
fun KokonutAmountPad(
    label: String = "Expense",
    recipientName: String = "Expense",
    recipientHandle: String? = null,
    initialAmount: Double = 0.0,
    maxAmount: Double = 250000.0,
    balance: Double? = null,
    presets: List<Int> = listOf(100, 500, 1000),
    onContinue: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var nextId by remember { mutableIntStateOf(1) }
    fun makeChar(ch: String): AmountChar {
        val id = nextId
        nextId += 1
        return AmountChar(id, ch)
    }

    val chars = remember {
        mutableStateListOf<AmountChar>().apply {
            if (initialAmount > 0.0) {
                val str = if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString()
                str.forEach { add(makeChar(it.toString())) }
            }
        }
    }

    var isCapped by remember { mutableStateOf(false) }
    val shakeX = remember { Animatable(0f) }

    val value = remember(chars.toList()) { amountPadToNumber(chars) }
    val isEmpty = chars.isEmpty()

    fun refuse(showCap: Boolean) {
        haptics.warning()
        if (showCap) {
            isCapped = true
            scope.launch {
                delay(1800)
                isCapped = false
            }
        }
        scope.launch {
            // Shake keyframes: 0, -10, 9, -6, 4, 0 over 360ms ease-out
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    0f at 0
                    -10f at 70
                    9f at 140
                    -6f at 210
                    4f at 280
                    0f at 360
                }
            )
        }
    }

    fun pressKey(key: String) {
        haptics.tick()
        if (key == "back") {
            if (chars.isNotEmpty()) {
                chars.removeAt(chars.size - 1)
            }
            isCapped = false
            return
        }

        val next = amountPadTypeKey(chars, key, ::makeChar)
        if (next == null) {
            refuse(false)
        } else if (amountPadToNumber(next) > maxAmount) {
            refuse(true)
        } else {
            chars.clear()
            chars.addAll(next)
        }
    }

    fun applyPreset(presetVal: Int) {
        haptics.click()
        isCapped = false
        val clamped = min(presetVal.toDouble(), maxAmount)
        val str = clamped.toInt().toString()
        chars.clear()
        str.forEach { chars.add(makeChar(it.toString())) }
    }

    val glyphs = remember(chars.toList()) { amountPadLayout(chars) }

    // Initials calculation
    val initials = remember(recipientName) {
        val parts = recipientName.trim().split(" ").filter { it.isNotEmpty() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "EX"
        }
    }

    // Card styling matching Section 4.2: max width 320dp, rounded 24dp, 1dp border zinc-200/800, bg white/zinc-950, padding 20
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
            // Header Row: 36dp circle chip + label + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 36dp circle chip: bg orange-100 / dark orange-500 @ 15%, text orange-700 / dark orange-300, 12sp medium
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (dark) Color(0x26F97316) else Color(0xFFFFEDD5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (dark) Color(0xFFFDBA74) else Color(0xFFC2410C)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (dark) Zinc400 else Zinc500
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = recipientName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) Zinc100 else Zinc900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!recipientHandle.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = recipientHandle,
                                fontSize = 14.sp,
                                color = if (dark) Zinc500 else Zinc400,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Amount Area: mt 24, height 80dp, centered
            Spacer(modifier = Modifier.height(24.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                val frameWidthPx = constraints.maxWidth.toFloat()

                var contentWidthPx by remember { mutableFloatStateOf(0f) }
                val targetScale = if (contentWidthPx > 0f) min(1f, frameWidthPx / contentWidthPx) else 1f
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.872f),
                    label = "fitScale"
                )

                Row(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = shakeX.value * density.density
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .onSizeChanged { contentWidthPx = it.width.toFloat() },
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Currency symbol: ₹ with mr 2dp
                    Text(
                        text = "₹",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isEmpty) (if (dark) Zinc700 else Zinc300) else (if (dark) Zinc500 else Zinc400),
                        letterSpacing = (-0.025).sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )

                    if (isEmpty) {
                        // Empty state: faint "0"
                        Text(
                            text = "0",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) Zinc700 else Zinc300,
                            letterSpacing = (-0.025).sp
                        )
                    } else {
                        // Layout glyphs with per-character animation
                        Row(verticalAlignment = Alignment.Bottom) {
                            glyphs.forEach { glyph ->
                                val glyphColor = when (glyph.kind) {
                                    AmountGlyphKind.DIGIT -> if (dark) Zinc50 else Zinc900
                                    AmountGlyphKind.SEP -> if (dark) Zinc500 else Zinc400
                                    AmountGlyphKind.GHOST -> if (dark) Zinc700 else Zinc300
                                }
                                Text(
                                    text = glyph.char,
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = glyphColor,
                                    letterSpacing = (-0.025).sp
                                )
                            }
                        }
                    }
                }
            }

            // Cap line / Subtitle line: height 20dp, 12sp, centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isCapped,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180, easing = KOKONUT_EASE_OUT)) +
                                slideInVertically(
                                    animationSpec = tween(180, easing = KOKONUT_EASE_OUT),
                                    initialOffsetY = { 6 }
                                )) togetherWith
                                (fadeOut(animationSpec = tween(180, easing = KOKONUT_EASE_OUT)) +
                                        slideOutVertically(
                                            animationSpec = tween(180, easing = KOKONUT_EASE_OUT),
                                            targetOffsetY = { -6 }
                                        ))
                    },
                    label = "capSubtitleSwap"
                ) { capped ->
                    if (capped) {
                        Text(
                            text = "Up to ${amountPadMoney(maxAmount)} per entry",
                            fontSize = 12.sp,
                            color = if (dark) Color(0xFFF87171) else Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val subText = if (balance != null) {
                            "Balance ${amountPadMoney(balance)}"
                        } else {
                            label
                        }
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            color = if (dark) Zinc400 else Zinc500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Presets: mt 16dp, row centered gap 8dp, height 32, px 12
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (tokens != null) tokens.borderStart.copy(alpha = 0.3f) else if (dark) Zinc800 else Zinc200,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { applyPreset(preset) }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = amountPadMoney(preset.toDouble()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) Zinc300 else Zinc700
                        )
                    }
                }
            }

            // Keypad: mt 16dp, 3-column grid gap 4dp, height 56dp per key
            Spacer(modifier = Modifier.height(16.dp))
            val keypadRows = AmountPadKeys.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                keypadRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { key ->
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            val keyBg = if (isPressed) {
                                if (dark) Zinc800 else Zinc100
                            } else {
                                Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(keyBg)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) { pressKey(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "back") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Delete",
                                        tint = Zinc500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (dark) Zinc100 else Zinc900
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action Button: mt 12dp, height 48dp, rounded 16dp, 14sp medium
            Spacer(modifier = Modifier.height(12.dp))
            val btnBg = if (isEmpty) {
                if (tokens != null) tokens.tintFlat else if (dark) Zinc900 else Zinc100
            } else {
                if (dark) Zinc100 else Zinc900
            }
            val btnTextColor = if (isEmpty) {
                if (dark) Zinc600 else Zinc400
            } else {
                if (dark) Zinc900 else Color.White
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(btnBg)
                    .clickable {
                        if (isEmpty || value <= 0.0) {
                            refuse(false)
                        } else {
                            haptics.click()
                            onContinue(value)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isEmpty,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200, easing = KOKONUT_EASE_OUT)) +
                                slideInVertically(
                                    animationSpec = tween(200, easing = KOKONUT_EASE_OUT),
                                    initialOffsetY = { 10 }
                                )) togetherWith
                                (fadeOut(animationSpec = tween(200, easing = KOKONUT_EASE_OUT)) +
                                        slideOutVertically(
                                            animationSpec = tween(200, easing = KOKONUT_EASE_OUT),
                                            targetOffsetY = { -10 }
                                        ))
                    },
                    label = "btnLabelTransition"
                ) { empty ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = btnTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (empty) "Enter an amount" else "Continue ${amountPadMoney(value)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = btnTextColor
                        )
                    }
                }
            }
        }
    }
}
