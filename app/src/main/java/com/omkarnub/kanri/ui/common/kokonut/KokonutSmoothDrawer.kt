package com.omkarnub.kanri.ui.common.kokonut

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.kanriGlass
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 1:1 Port of Kokonut UI Smooth Drawer (A3. smooth-drawer.tsx)
 *
 * Exact constants:
 * PEEK_HEIGHT = 200dp
 * MAX_SHEET_HEIGHT = 680dp
 * TOP_GAP = 24dp
 * Handle: 24dp height row with 36x4dp pill
 * Rotation: ChevronUp 14dp rotates 180 deg (300ms cubic-bezier(0.32, 0.72, 0, 1))
 * Details expansion: 300ms cubic-bezier(0.32, 0.72, 0, 1)
 * Rolling amount: 240ms ease-out cubic-bezier(0.23, 1, 0.32, 1), dir * 70%
 */

const val DRAWER_PEEK_HEIGHT = 240f
const val DRAWER_MAX_HEIGHT = 680f
const val DRAWER_TOP_GAP = 24f

val DRAWER_EASE_CURVE = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
val ROLLING_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

data class DrawerDetailRow(
    val title: String,
    val subtitle: String? = null,
    val value: String
)

@Composable
fun RollingAmount(
    amount: Double,
    modifier: Modifier = Modifier
) {
    var previousAmount by remember { mutableDoubleStateOf(amount) }
    val direction = if (amount >= previousAmount) 1 else -1

    LaunchedEffect(amount) {
        previousAmount = amount
    }

    val formatted = remember(amount) {
        val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
        "₹${df.format(amount)}"
    }

    val dark = isSystemInDarkTheme()
    val chars = formatted.toList()

    Row(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        chars.forEachIndexed { index, ch ->
            val slotFromRight = chars.size - index
            if (ch in '0'..'9') {
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = ch,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(240, easing = ROLLING_EASE_OUT)) +
                                    slideInVertically(
                                        animationSpec = tween(240, easing = ROLLING_EASE_OUT),
                                        initialOffsetY = { (direction * it * 0.7f).roundToInt() }
                                    )) togetherWith
                                    (fadeOut(animationSpec = tween(240, easing = ROLLING_EASE_OUT)) +
                                            slideOutVertically(
                                                animationSpec = tween(240, easing = ROLLING_EASE_OUT),
                                                targetOffsetY = { (-direction * it * 0.7f).roundToInt() }
                                            ))
                        },
                        label = "rollDigit-$slotFromRight"
                    ) { targetChar ->
                        Text(
                            text = targetChar.toString(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (dark) Zinc50 else Zinc900,
                            letterSpacing = (-0.02).sp
                        )
                    }
                }
            } else {
                Text(
                    text = ch.toString(),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dark) Zinc50 else Zinc900,
                    letterSpacing = (-0.02).sp
                )
            }
        }
    }
}

@Composable
fun KokonutSmoothDrawer(
    title: String = "Review entry",
    description: String? = null,
    totalAmount: Double,
    slideLabel: String = "Slide to confirm",
    detailRows: List<DrawerDetailRow> = emptyList(),
    accountType: String = "Cash",
    balanceAfter: Double? = null,
    onConfirm: suspend () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: dev.chrisbanes.haze.HazeState? = LocalHazeState.current
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    val viewportHeightDp = config.screenHeightDp.toFloat()
    val sheetHeightDp = max(DRAWER_PEEK_HEIGHT, min(DRAWER_MAX_HEIGHT, viewportHeightDp - DRAWER_TOP_GAP))

    var isExpanded by remember { mutableStateOf(false) }

    // Height offset: 0 = full expansion, (sheetHeightDp - DRAWER_PEEK_HEIGHT) = peek
    val maxOffsetDp = sheetHeightDp - DRAWER_PEEK_HEIGHT
    val offsetAnim = remember { Animatable(maxOffsetDp) }

    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) 0f else maxOffsetDp
        offsetAnim.animateTo(target, tween(300, easing = DRAWER_EASE_CURVE))
    }

    val progress = if (maxOffsetDp > 0f) 1f - (offsetAnim.value / maxOffsetDp).coerceIn(0f, 1f) else 1f

    // Scrim overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (hazeState != null) {
                    Modifier.background(Color.Black.copy(alpha = 0.5f * (0.4f + 0.6f * progress)))
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.5f * (0.4f + 0.6f * progress)))
                }
            )
            .clickable { onDismiss() }
    ) {
        // Bottom-anchored Sheet Card: max width 420dp, rounded top corners 24dp (rounded-t-2xl), 1dp border zinc-200/800
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .width(420.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = with(density) { offsetAnim.value.dp.toPx().roundToInt() }
                    )
                }
                .height(sheetHeightDp.dp)
                .then(
                    if (tokens != null) {
                        Modifier.kanriGlass(
                            level = GlassLevel.Strong,
                            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                            hazeState = hazeState
                        )
                    } else {
                        Modifier
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(if (dark) Zinc950 else Color.White)
                            .border(
                                width = 1.dp,
                                color = if (dark) Zinc800 else Zinc200,
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                    }
                )
                .clickable(enabled = false) {}
                .pointerInput(maxOffsetDp) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val cur = offsetAnim.value
                            val dragDp = dragAmount / density.density
                            val next = (cur + dragDp).coerceIn(0f, maxOffsetDp + 60f)
                            scope.launch { offsetAnim.snapTo(next) }
                        },
                        onDragEnd = {
                            val cur = offsetAnim.value
                            if (cur > maxOffsetDp + 30f) {
                                onDismiss()
                            } else if (cur < maxOffsetDp * 0.5f) {
                                isExpanded = true
                            } else {
                                isExpanded = false
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Drag Handle row: 24dp height with 36x4dp pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (dark) Zinc700 else Zinc300)
                    )
                }

                // Peek content: px 20, pt 4
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tokens?.textPrimary ?: if (dark) Zinc50 else Zinc900,
                                letterSpacing = (-0.02).sp
                            )
                            if (!description.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = description,
                                    fontSize = 13.sp,
                                    color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total",
                                fontSize = 13.sp,
                                color = if (dark) Zinc400 else Zinc500
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            RollingAmount(amount = totalAmount)
                        }
                    }

                    // Primary Action: Slide to Confirm (mt 16)
                    Spacer(modifier = Modifier.height(16.dp))
                    KokonutSlideToConfirm(
                        label = slideLabel,
                        confirmingLabel = "Saving…",
                        doneLabel = "Saved",
                        onConfirm = {
                            onConfirm()
                            kotlinx.coroutines.delay(450)
                            onDismiss()
                        },
                        resetAfter = null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Details toggle: mt 4, height 40, rounded 8, 13sp zinc-500/400 + ChevronUp rotating 180 deg
                    Spacer(modifier = Modifier.height(4.dp))
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        animationSpec = tween(300, easing = DRAWER_EASE_CURVE),
                        label = "chevronRot"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isExpanded = !isExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isExpanded) "Hide details" else "Details",
                                fontSize = 13.sp,
                                color = if (dark) Zinc400 else Zinc500
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = null,
                                tint = if (dark) Zinc400 else Zinc500,
                                modifier = Modifier
                                    .size(14.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }
                }

                // Details Area: fills the rest, px 20, pb 24, top border zinc-200/800
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer {
                            alpha = progress
                            translationY = (1f - progress) * with(density) { 12.dp.toPx() }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider(
                            color = if (tokens != null) tokens.borderStart.copy(alpha = 0.25f) else if (dark) Zinc800 else Zinc200,
                            thickness = 1.dp
                        )

                        // (a) List of rows (40dp rounded-8 box with letter, name, sub-line, amount on right)
                        detailRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (tokens != null) tokens.tintFlat else if (dark) Zinc900 else Zinc50)
                                        .border(1.dp, if (tokens != null) tokens.borderStart.copy(alpha = 0.25f) else if (dark) Zinc800 else Zinc200, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = row.title.firstOrNull()?.toString() ?: "",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = row.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
                                    )
                                    if (!row.subtitle.isNullOrEmpty()) {
                                        Text(
                                            text = row.subtitle,
                                            fontSize = 13.sp,
                                            color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500
                                        )
                                    }
                                }

                                Text(
                                    text = row.value,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
                                )
                            }
                        }

                        // (b) Breakdown dl block: Subtotal / Fee / Total
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Amount",
                                    fontSize = 14.sp,
                                    color = if (dark) Zinc400 else Zinc500
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(totalAmount),
                                    fontSize = 14.sp,
                                    color = if (dark) Zinc100 else Zinc900
                                )
                            }
                            HorizontalDivider(
                                color = if (dark) Zinc800 else Zinc200,
                                thickness = 1.dp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) Zinc100 else Zinc900
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(totalAmount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (dark) Zinc100 else Zinc900
                                )
                            }
                        }

                        // (c) Bordered account row: px 12, py 10, rounded-12
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, if (dark) Zinc800 else Zinc200, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (accountType.contains("Cash", ignoreCase = true)) Icons.Default.Payments else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (dark) Zinc400 else Zinc500,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = accountType,
                                fontSize = 14.sp,
                                color = if (dark) Zinc100 else Zinc900,
                                modifier = Modifier.weight(1f)
                            )
                            if (balanceAfter != null) {
                                Text(
                                    text = "After: ${CurrencyUtils.formatCurrency(balanceAfter)}",
                                    fontSize = 13.sp,
                                    color = if (dark) Zinc500 else Zinc400
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
