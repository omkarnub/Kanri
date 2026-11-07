package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.min

/**
 * 1:1 Port of Kokonut UI Amount Pad (#A2 amount-pad.tsx)
 *
 * Polished to the core with Apple Fluid Design & Emil Kowalski Principles:
 * - Starts from ZERO (clean 0 display)
 * - Bold rolling digits with GoogleSansFlex font matching app typography
 * - Tactile spring keypress feedback (0.91x scale, haptic clicks)
 * - Additive / Set presets (+100, +500, +1,000, +2,000)
 * - Centered drag handle pill & top-right close icon (no badge)
 * - Fluid scale-to-fit amount container
 * - Smooth "Continue" CTA button
 */

const val AMOUNT_PAD_MAX_AMOUNT = 250000.0
val AmountPadKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "back")

sealed class AmountPadKeyResult {
    data class Success(val newRawInput: String) : AmountPadKeyResult()
    data object RefusedInvalid : AmountPadKeyResult()
    data object RefusedCapped : AmountPadKeyResult()
}

fun amountPadToNumber(rawInput: String): Double {
    return rawInput.toDoubleOrNull() ?: 0.0
}

fun amountPadMoney(value: Double, currency: String = "₹"): String {
    val df = DecimalFormat(if (value % 1.0 == 0.0) "#,##0" else "#,##0.00", DecimalFormatSymbols(Locale.US))
    return "$currency${df.format(value)}"
}

fun amountPadProcessKey(
    rawInput: String,
    key: String,
    maxAmount: Double = AMOUNT_PAD_MAX_AMOUNT
): AmountPadKeyResult {
    if (key == "back") {
        val next = if (rawInput.isNotEmpty()) rawInput.dropLast(1) else ""
        return AmountPadKeyResult.Success(next)
    }

    if (key == ".") {
        if (rawInput.contains(".")) {
            return AmountPadKeyResult.RefusedInvalid
        }
        val next = if (rawInput.isEmpty()) "0." else "$rawInput."
        return AmountPadKeyResult.Success(next)
    }

    // Key is 0..9
    val dotIdx = rawInput.indexOf('.')
    if (dotIdx != -1 && (rawInput.length - dotIdx) > 2) {
        return AmountPadKeyResult.RefusedInvalid
    }

    val nextInput = if (rawInput.isEmpty() || rawInput == "0") {
        key
    } else {
        rawInput + key
    }

    val nextNum = nextInput.toDoubleOrNull() ?: 0.0
    if (nextNum > maxAmount) {
        return AmountPadKeyResult.RefusedCapped
    }

    return AmountPadKeyResult.Success(nextInput)
}



@Composable
fun KokonutAmountPad(
    label: String = "Expense",
    recipientName: String = "Expense",
    recipientHandle: String? = null,
    initialAmount: Double = 0.0,
    maxAmount: Double = AMOUNT_PAD_MAX_AMOUNT,
    balance: Double? = null,
    presets: List<Int> = listOf(100, 500, 1000, 2000),
    currency: String = CurrencyUtils.getCurrencySymbol(),
    showHandle: Boolean = true,
    accentColor: Color? = null,
    onClose: (() -> Unit)? = null,
    onContinue: (Double) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val dark = isAppInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Raw input string representing typed characters (e.g. "", "5", "50", "50.5")
    var rawInput by remember {
        mutableStateOf(
            if (initialAmount > 0.0) {
                if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString()
            } else ""
        )
    }

    var isCapped by remember { mutableStateOf(false) }
    val shakeX = remember { Animatable(0f) }

    val numericValue = remember(rawInput) {
        rawInput.toDoubleOrNull() ?: 0.0
    }
    val isEmpty = rawInput.isEmpty() || numericValue <= 0.0

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
        when (val res = amountPadProcessKey(rawInput, key, maxAmount)) {
            is AmountPadKeyResult.Success -> {
                rawInput = res.newRawInput
                isCapped = false
            }
            is AmountPadKeyResult.RefusedInvalid -> {
                refuse(false)
            }
            is AmountPadKeyResult.RefusedCapped -> {
                refuse(true)
            }
        }
    }

    fun applyPreset(presetVal: Int) {
        haptics.click()
        isCapped = false
        val currentNum = rawInput.toDoubleOrNull() ?: 0.0
        val nextNum = if (currentNum <= 0.0) {
            presetVal.toDouble()
        } else {
            currentNum + presetVal.toDouble()
        }

        if (nextNum > maxAmount) {
            refuse(true)
            return
        }

        rawInput = if (nextNum % 1.0 == 0.0) nextNum.toLong().toString() else "%.2f".format(Locale.US, nextNum)
    }

    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.EntrySheet,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top Bar: Centered drag handle pill + right-aligned close icon (No badge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(36.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (showHandle) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(if (dark) Zinc700 else Zinc300)
                        )
                    }
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }

            // Amount Display Area: Bold GoogleSansFlex, Rolling Digits, Scale-Fit
            Spacer(modifier = Modifier.height(14.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                contentAlignment = Alignment.Center
            ) {
                val frameWidthPx = constraints.maxWidth.toFloat()
                var contentWidthPx by remember { mutableFloatStateOf(0f) }
                val targetScale = if (contentWidthPx > 0f) min(1f, (frameWidthPx - 32f) / contentWidthPx) else 1f
                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(stiffness = 380f, dampingRatio = 0.88f),
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Currency Symbol (e.g. ₹)
                    Text(
                        text = currency,
                        fontSize = 54.sp,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Black,
                        color = if (isEmpty) (if (dark) Zinc700 else Zinc300) else (tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500),
                        letterSpacing = (-0.025).sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Rolling Digits Display
                    RollingAmountDisplay(
                        rawInput = rawInput,
                        fontSize = 54.sp,
                        color = if (isEmpty) (if (dark) Zinc700 else Zinc300) else (tokens?.textPrimary ?: if (dark) Zinc50 else Zinc900),
                        dark = dark
                    )
                }
            }

            // Cap Warning / Subtitle line: height 20dp
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
                            text = "Up to $currency${DecimalFormat("#,##0").format(maxAmount)} per entry",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (dark) Color(0xFFF87171) else Color(0xFFDC2626),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        val subText = if (balance != null) {
                            "Balance $currency${DecimalFormat("#,##0.00").format(balance)}"
                        } else {
                            "Tap keypad to set amount"
                        }
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Presets Row: Tactile spring buttons (+100, +500, +1,000, +2,000)
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                presets.forEach { preset ->
                    TactilePresetChip(
                        preset = preset,
                        currency = currency,
                        onClick = { applyPreset(preset) },
                        tokens = tokens,
                        dark = dark
                    )
                }
            }

            // Keypad: 3-column grid, 54dp key height with tactile spring press feedback
            Spacer(modifier = Modifier.height(14.dp))
            val keypadRows = AmountPadKeys.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                keypadRows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowKeys.forEach { key ->
                            TactileKeypadKey(
                                key = key,
                                onClick = { pressKey(key) },
                                modifier = Modifier.weight(1f),
                                tokens = tokens,
                                dark = dark
                            )
                        }
                    }
                }
            }

            // Action Button: Full-width "Continue" with tactile press feedback
            Spacer(modifier = Modifier.height(14.dp))
            val interactionSource = remember { MutableInteractionSource() }
            val isBtnPressed by interactionSource.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (isBtnPressed) 0.96f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f),
                label = "btnScale"
            )

            val btnBg = if (isEmpty) {
                if (tokens != null) {
                    Color.Black.copy(alpha = 0.10f)
                } else {
                    if (dark) Zinc900 else Zinc100
                }
            } else {
                if (dark) Zinc100 else Zinc900
            }
            val btnTextColor = if (isEmpty) {
                if (dark) Zinc500 else Zinc400
            } else {
                if (dark) Zinc900 else Color.White
            }
            val btnBorderColor = if (isEmpty) {
                if (tokens != null) {
                    Color.White.copy(alpha = 0.10f)
                } else {
                    if (dark) Zinc800 else Zinc200
                }
            } else {
                Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(btnBg)
                    .border(
                        0.8.dp,
                        btnBorderColor,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        if (isEmpty || numericValue <= 0.0) {
                            refuse(false)
                        } else {
                            haptics.click()
                            onContinue(numericValue)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isEmpty,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180, easing = KOKONUT_EASE_OUT)) +
                                slideInVertically(
                                    animationSpec = tween(180, easing = KOKONUT_EASE_OUT),
                                    initialOffsetY = { 8 }
                                )) togetherWith
                                (fadeOut(animationSpec = tween(180, easing = KOKONUT_EASE_OUT)) +
                                        slideOutVertically(
                                            animationSpec = tween(180, easing = KOKONUT_EASE_OUT),
                                            targetOffsetY = { -8 }
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
                        val formattedAmount = remember(numericValue) {
                            val df = DecimalFormat(if (numericValue % 1.0 == 0.0) "#,##0" else "#,##0.00", DecimalFormatSymbols(Locale.US))
                            df.format(numericValue)
                        }
                        Text(
                            text = if (empty) "Enter an amount" else "Continue  •  $currency$formattedAmount",
                            fontSize = 15.sp,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            color = btnTextColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tactile Keypad Key with 0.91f spring scale on press
 */
@Composable
private fun TactileKeypadKey(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tokens: com.omkarnub.kanri.ui.theme.KanriGlassTokens?,
    dark: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()

    // Tactile key compression
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(stiffness = 460f, dampingRatio = 0.72f),
        label = "keyScale"
    )

    // Digit punch / pop spring: compresses when pressed, punches overshoot (1.15f) on release
    val digitScale = remember { Animatable(1f) }
    LaunchedEffect(isPressed) {
        if (isPressed) {
            digitScale.animateTo(0.84f, spring(stiffness = 650f, dampingRatio = 0.75f))
        } else if (digitScale.value < 0.99f) {
            digitScale.animateTo(1.15f, spring(stiffness = 550f, dampingRatio = 0.62f))
            digitScale.animateTo(1.0f, spring(stiffness = 400f, dampingRatio = 0.75f))
        }
    }

    val keyBg = if (tokens != null) {
        if (isPressed) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.10f)
    } else {
        if (isPressed) {
            if (dark) Zinc800 else Zinc200
        } else {
            if (dark) Zinc900 else Zinc100
        }
    }

    val keyBorder = if (tokens != null) {
        if (isPressed) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f)
    } else {
        if (dark) Zinc800 else Zinc200
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(keyBg)
            .border(
                0.8.dp,
                keyBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    scope.launch {
                        digitScale.snapTo(0.84f)
                        digitScale.animateTo(1.15f, spring(stiffness = 600f, dampingRatio = 0.62f))
                        digitScale.animateTo(1.0f, spring(stiffness = 450f, dampingRatio = 0.75f))
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = digitScale.value
                scaleY = digitScale.value
            },
            contentAlignment = Alignment.Center
        ) {
            if (key == "back") {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = if (isPressed) {
                        tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
                    } else {
                        tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                    },
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = key,
                    fontSize = 24.sp,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.SemiBold,
                    color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
                )
            }
        }
    }
}

/**
 * Tactile Preset Chip with 0.94f spring scale on press
 */
@Composable
private fun TactilePresetChip(
    preset: Int,
    currency: String,
    onClick: () -> Unit,
    tokens: com.omkarnub.kanri.ui.theme.KanriGlassTokens?,
    dark: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.78f),
        label = "presetScale"
    )

    val chipBg = if (tokens != null) {
        if (isPressed) Color.Black.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.10f)
    } else {
        if (isPressed) {
            if (dark) Zinc800 else Zinc200
        } else {
            if (dark) Zinc900 else Zinc100
        }
    }

    val chipBorder = if (tokens != null) {
        if (isPressed) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f)
    } else {
        if (dark) Zinc800 else Zinc200
    }

    Box(
        modifier = Modifier
            .height(34.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(17.dp))
            .border(
                0.8.dp,
                chipBorder,
                RoundedCornerShape(17.dp)
            )
            .background(chipBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        val formatted = remember(preset) {
            val df = DecimalFormat("#,##0")
            df.format(preset)
        }
        Text(
            text = "+$currency$formatted",
            fontSize = 12.5.sp,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            color = tokens?.textPrimary ?: if (dark) Zinc200 else Zinc800
        )
    }
}

/**
 * Rolling digits display for Amount Pad using GoogleSansFlex Black typography
 * and the exact cubic bezier deceleration curve from Expense Overview.
 */
@Composable
private fun RollingAmountDisplay(
    rawInput: String,
    fontSize: TextUnit,
    color: Color,
    dark: Boolean
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val style = remember(fontSize) {
        androidx.compose.ui.text.TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Black,
            fontSize = fontSize,
            letterSpacing = (-0.03).sp
        )
    }

    val digitMetrics = remember(style, density) {
        val samples = (0..9).map { d ->
            textMeasurer.measure(d.toString(), style).size
        }
        val maxWidth = samples.maxOf { it.width }
        val maxHeight = samples.maxOf { it.height }
        maxWidth to maxHeight
    }

    val digitWidthDp = with(density) { digitMetrics.first.toDp() }
    val digitHeightDp = with(density) { digitMetrics.second.toDp() }
    val digitHeightPx = digitMetrics.second.toFloat()

    // Break rawInput into tokens with stable right-anchored positional keys
    data class DisplayToken(val key: String, val text: String, val isDigit: Boolean, val digitValue: Int = 0, val index: Int = 0)

    val tokens = remember(rawInput) {
        if (rawInput.isEmpty() || rawInput == "0") {
            listOf(DisplayToken(key = "whole-0", text = "0", isDigit = true, digitValue = 0, index = 0))
        } else {
            val dotIdx = rawInput.indexOf('.')
            val wholePart = if (dotIdx != -1) rawInput.substring(0, dotIdx) else rawInput
            val decPart = if (dotIdx != -1) rawInput.substring(dotIdx + 1) else null

            val numWhole = wholePart.toLongOrNull() ?: 0L
            val df = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
            val formattedWhole = if (wholePart.isEmpty()) "0" else df.format(numWhole)

            val list = mutableListOf<DisplayToken>()

            // Count total digits in formatted whole to anchor keys from right to left
            val wholeDigits = formattedWhole.filter { it.isDigit() }
            var wholeDigitIndex = 0

            formattedWhole.forEachIndexed { fIdx, ch ->
                if (ch.isDigit()) {
                    val placeFromRight = (wholeDigits.length - 1) - wholeDigitIndex
                    list.add(
                        DisplayToken(
                            key = "whole-$placeFromRight",
                            text = ch.toString(),
                            isDigit = true,
                            digitValue = ch.digitToInt(),
                            index = wholeDigitIndex
                        )
                    )
                    wholeDigitIndex++
                } else {
                    list.add(
                        DisplayToken(
                            key = "sep-$fIdx",
                            text = ch.toString(),
                            isDigit = false
                        )
                    )
                }
            }

            if (dotIdx != -1) {
                list.add(DisplayToken(key = "dot", text = ".", isDigit = false))
                decPart?.forEachIndexed { dIdx, ch ->
                    if (ch.isDigit()) {
                        list.add(
                            DisplayToken(
                                key = "dec-$dIdx",
                                text = ch.toString(),
                                isDigit = true,
                                digitValue = ch.digitToInt(),
                                index = wholeDigitIndex + dIdx
                            )
                        )
                    }
                }
            }

            list
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        tokens.forEach { token ->
            key(token.key) {
                if (token.isDigit) {
                    SingleRollingDigitStrip(
                        digit = token.digitValue,
                        index = token.index,
                        style = style,
                        color = color,
                        width = digitWidthDp,
                        height = digitHeightDp,
                        heightPx = digitHeightPx
                    )
                } else {
                    Text(
                        text = token.text,
                        style = style,
                        color = if (token.text == ".") color else (if (dark) Zinc500 else Zinc400),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SingleRollingDigitStrip(
    digit: Int,
    index: Int,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    heightPx: Float
) {
    val animDigit by animateFloatAsState(
        targetValue = digit.toFloat(),
        animationSpec = tween(
            durationMillis = 380,
            delayMillis = index * 14,
            easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f) // Exact Expense Overview curve
        ),
        label = "rollingDigit_$index"
    )

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clipToBounds(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize(unbounded = true, align = Alignment.TopStart)
                .graphicsLayer {
                    translationY = -animDigit * heightPx
                }
        ) {
            for (i in 0..9) {
                Box(
                    modifier = Modifier.size(width = width, height = height),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = i.toString(),
                        style = style,
                        color = color,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
