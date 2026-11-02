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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.LocalHazeState
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
import kotlin.math.roundToInt

/**
 * 1:1 Port of Kokonut UI Smooth Drawer (#A3 smooth-drawer.tsx)
 *
 * Polished to the core with Apple Fluid Design:
 * - Fluid spring popup entrance from below
 * - Centered drag handle pill
 * - Bold rolling amount display with GoogleSansFlex typography
 * - Clean metadata rows (Type, Category, Date & Time, Note, Payment, Account)
 * - Bottom Slide to Confirm (#A4)
 * - Drag-to-dismiss gesture physics
 */

data class DrawerDetailRow(
    val title: String,
    val subtitle: String? = null,
    val value: String
)



val DRAWER_EASE_CURVE = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val ROLLING_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

@Composable
fun KokonutSmoothDrawer(
    title: String = "Review entry",
    description: String? = null,
    totalAmount: Double,
    slideLabel: String = "Slide to confirm",
    detailRows: List<DrawerDetailRow> = emptyList(),
    accountType: String = "Cash",
    balanceAfter: Double? = null,
    confirmationType: SlideConfirmationType = SlideConfirmationType.DEFAULT,
    onConfirm: suspend () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag-down to dismiss offset
    val dragOffsetY = remember { Animatable(0f) }

    KanriGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(0, dragOffsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount > 0f || dragOffsetY.value > 0f) {
                            val next = (dragOffsetY.value + dragAmount).coerceAtLeast(0f)
                            scope.launch { dragOffsetY.snapTo(next) }
                        }
                    },
                    onDragEnd = {
                        if (dragOffsetY.value > with(density) { 90.dp.toPx() }) {
                            haptics.tick()
                            onDismiss()
                        } else {
                            scope.launch {
                                dragOffsetY.animateTo(
                                    0f,
                                    spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
                                )
                            }
                        }
                    }
                )
            },
        level = GlassLevel.Strong,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        hazeState = hazeState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Drag Handle Row
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
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (dark) Zinc700 else Zinc300)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Header Title & Subtitle
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        color = tokens?.textPrimary ?: if (dark) Zinc50 else Zinc900,
                        letterSpacing = (-0.02).sp
                    )
                    if (!description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            fontSize = 12.5.sp,
                            fontFamily = GoogleSansFlex,
                            color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Rolling Total Amount on Top Right
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL",
                        fontSize = 11.sp,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    DrawerRollingAmount(
                        amount = totalAmount,
                        color = tokens?.textPrimary ?: if (dark) Zinc50 else Zinc900
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metadata Detail Cards Container
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (tokens != null) tokens.tintFlat.copy(alpha = 0.35f) else (if (dark) Zinc900.copy(alpha = 0.45f) else Zinc100.copy(alpha = 0.6f)),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (tokens != null) tokens.borderStart.copy(alpha = 0.25f) else (if (dark) Zinc800 else Zinc200)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detailRows.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = row.title,
                                fontSize = 12.5.sp,
                                fontFamily = GoogleSansFlex,
                                color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                            )
                            Text(
                                text = row.value,
                                fontSize = 13.sp,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (index < detailRows.size - 1) {
                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = if (dark) Zinc800.copy(alpha = 0.6f) else Zinc200.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Primary Slide to Confirm CTA (#A4)
            Spacer(modifier = Modifier.height(18.dp))
            KokonutSlideToConfirm(
                label = slideLabel,
                confirmingLabel = "Saving…",
                confirmationType = confirmationType,
                onConfirm = {
                    onConfirm()
                    delay(350)
                    onDismiss()
                },
                resetAfter = null,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

/**
 * Bold rolling amount for drawer header using AnimatedNumberText
 */
@Composable
private fun DrawerRollingAmount(
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val currency = CurrencyUtils.getCurrencySymbol()
    val formatted = remember(amount, currency) {
        val df = DecimalFormat(if (amount % 1.0 == 0.0) "#,##0" else "#,##0.00", DecimalFormatSymbols(Locale.US))
        "$currency${df.format(amount)}"
    }

    AnimatedNumberText(
        text = formatted,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = (-0.02).sp
        ),
        color = color,
        durationMillis = 380,
        delayPerDigitMillis = 14,
        animateFromZero = false
    )
}
