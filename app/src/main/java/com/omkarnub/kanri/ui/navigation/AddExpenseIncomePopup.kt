package com.omkarnub.kanri.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import com.omkarnub.kanri.util.rememberKanriHaptics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.luminance
import com.omkarnub.kanri.ui.theme.lerpColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.ThemeMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke

import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.theme.KanriMotion

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeGreen = Color(0xFF30A46C)

/**
 * Frosted Glass Popup Menu for the Floating '+' Button:
 * 1. Hardware-accelerated frosted glass card floating directly above the dock.
 * 2. Fluid spring scale-in and fade entrance animation.
 * 3. Two clean options: "Expense" (in crisp red) and "Income" (in sage green).
 * 4. Zero mention of "Debit" or "Credit".
 * 5. Dismissible by tapping anywhere outside or on the '+' button.
 */
@Composable
fun AddExpenseIncomePopup(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    themeMode: ThemeMode? = null
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val glassCustomization by themePrefs.glassCustomizationFlow.collectAsState()
    val tokens = LocalKanriGlass.current

    val effectiveExpenseColor = tokens?.expenseColor ?: glassCustomization.expenseColor
    val effectiveIncomeColor = tokens?.incomeColor ?: glassCustomization.incomeColor
    val expenseTintOpacity = tokens?.expenseTintOpacity ?: glassCustomization.expenseTintOpacity
    val incomeTintOpacity = tokens?.incomeTintOpacity ?: glassCustomization.incomeTintOpacity
    val cornerRadius = tokens?.cornerRadius ?: glassCustomization.cornerRadiusDp.dp
    val borderWidth = tokens?.borderWidth ?: glassCustomization.borderWidthDp.dp
    val tintLightness = glassCustomization.tintLightness

    val glassTheme = rememberKanriGlassTheme(themeMode)

    // Full screen overlay with tap-outside dismiss
    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
        exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.30f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }

    // Floating Frosted Glass Menu Card
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(animationSpec = tween(180, easing = KanriMotion.EaseOut)) +
                    scaleIn(
                        initialScale = 0.90f,
                        animationSpec = KanriMotion.SnappySpring
                    ) +
                    slideInVertically(
                        initialOffsetY = { (it * 0.10f).toInt() },
                        animationSpec = KanriMotion.SmoothSlideSpring
                    ),
            exit = fadeOut(animationSpec = tween(140, easing = KanriMotion.EaseOut)) +
                    scaleOut(targetScale = 0.94f, animationSpec = tween(140, easing = KanriMotion.EaseOut)) +
                    slideOutVertically(targetOffsetY = { (it * 0.08f).toInt() }, animationSpec = tween(140, easing = KanriMotion.EaseOut))
        ) {
            KanriGlassCard(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 86.dp)
                    .width(320.dp),
                level = GlassLevel.Strong,
                shape = RoundedCornerShape(26.dp),
                hazeState = hazeState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PopupOptionCard(
                        title = "Expense",
                        subtitle = "Log spent money",
                        accentColor = effectiveExpenseColor,
                        icon = { tint -> ExpenseFlowIcon(tint = tint, size = 24.dp) },
                        hazeState = hazeState,
                        tintOpacity = expenseTintOpacity,
                        tintLightness = tintLightness,
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        onClick = {
                            onDismiss()
                            onAddExpense()
                        }
                    )
                    PopupOptionCard(
                        title = "Income",
                        subtitle = "Log received money",
                        accentColor = effectiveIncomeColor,
                        icon = { tint -> IncomeFlowIcon(tint = tint, size = 24.dp) },
                        hazeState = hazeState,
                        tintOpacity = incomeTintOpacity,
                        tintLightness = tintLightness,
                        cornerRadius = cornerRadius,
                        borderWidth = borderWidth,
                        onClick = {
                            onDismiss()
                            onAddIncome()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupOptionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    hazeState: HazeState? = null,
    tintOpacity: Float = 0.16f,
    tintLightness: Float = 0.0f,
    cornerRadius: Dp = 18.dp,
    borderWidth: Dp = 1.2.dp
) {
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = KanriMotion.PressSpring,
        label = "popupOptionScale"
    )

    val safeTint = tintOpacity.coerceIn(0.04f, 0.50f)
    val cardShape = RoundedCornerShape(cornerRadius.coerceIn(12.dp, 28.dp))

    val isDark = LocalKanriGlass.current?.isDark ?: (MaterialTheme.colorScheme.surface.luminance() < 0.5f)
    val isAmoled = MaterialTheme.colorScheme.background == Color.Black || MaterialTheme.colorScheme.surface == Color.Black
    val cardFillColor = when {
        !isDark -> Color.White.copy(alpha = 0.55f)
        isAmoled -> Color(0xFF0C0C0C).copy(alpha = 0.55f)
        else -> Color(0xFF141414).copy(alpha = 0.45f)
    }
    val titleTextColor = if (isDark) Color(0xFFF2F2F2) else Color(0xFF12141B)
    val subtitleTextColor = if (isDark) Color(0xFFE0E0E0).copy(alpha = 0.70f) else Color(0xFF5A5C66)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                spotColor = accentColor.copy(alpha = 0.28f),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(cardShape)
            .background(cardFillColor)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                        accentColor.copy(alpha = if (isDark) 0.05f else 0.02f)
                    )
                )
            )
            .border(
                BorderStroke(
                    borderWidth.coerceIn(0.8.dp, 2.dp),
                    Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = if (isDark) 0.50f else 0.40f),
                            accentColor.copy(alpha = if (isDark) 0.15f else 0.10f)
                        )
                    )
                ),
                shape = cardShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.16f))
                    .border(
                        0.8.dp,
                        accentColor.copy(alpha = 0.45f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                icon(accentColor)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = titleTextColor,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = subtitleTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Custom Angled Downward Financial Flow Arrow (Expense SVG).
 */
@Composable
fun ExpenseFlowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.toPx()
        val h = size.toPx()

        // Main diagonal arrow shaft: top-left to bottom-right
        val shaftPath = Path().apply {
            moveTo(w * 0.30f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.70f)
        }
        drawPath(
            path = shaftPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Arrowhead pointing down-right
        val headPath = Path().apply {
            moveTo(w * 0.40f, h * 0.70f)
            lineTo(w * 0.70f, h * 0.70f)
            lineTo(w * 0.70f, h * 0.40f)
        }
        drawPath(
            path = headPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Custom Angled Upward Financial Flow Arrow (Income SVG).
 */
@Composable
fun IncomeFlowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.toPx()
        val h = size.toPx()

        // Main diagonal arrow shaft: bottom-left to top-right
        val shaftPath = Path().apply {
            moveTo(w * 0.30f, h * 0.70f)
            lineTo(w * 0.70f, h * 0.30f)
        }
        drawPath(
            path = shaftPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Arrowhead pointing up-right
        val headPath = Path().apply {
            moveTo(w * 0.40f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.30f)
            lineTo(w * 0.70f, h * 0.60f)
        }
        drawPath(
            path = headPath,
            color = tint,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Custom Bidirectional Exchange Flow Arrows (Lend & Borrow SVG).
 */
@Composable
fun LendBorrowFlowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.toPx()
        val h = size.toPx()

        // Top horizontal arrow pointing right
        val topShaft = Path().apply {
            moveTo(w * 0.25f, h * 0.38f)
            lineTo(w * 0.75f, h * 0.38f)
        }
        drawPath(path = topShaft, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        val topHead = Path().apply {
            moveTo(w * 0.60f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.38f)
            lineTo(w * 0.60f, h * 0.51f)
        }
        drawPath(path = topHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Bottom horizontal arrow pointing left
        val bottomShaft = Path().apply {
            moveTo(w * 0.75f, h * 0.65f)
            lineTo(w * 0.25f, h * 0.65f)
        }
        drawPath(path = bottomShaft, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        val bottomHead = Path().apply {
            moveTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.25f, h * 0.65f)
            lineTo(w * 0.40f, h * 0.78f)
        }
        drawPath(path = bottomHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}


/**
 * Custom Diverging Financial Flow (Split Bill / Split Expense SVG).
 */
@Composable
fun SplitBillFlowIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 2.2.dp.toPx()
        val w = size.toPx()
        val h = size.toPx()

        // Base vertical stem rising from bottom center
        val stem = Path().apply {
            moveTo(w * 0.50f, h * 0.80f)
            lineTo(w * 0.50f, h * 0.52f)
        }
        drawPath(path = stem, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Left diverging branch
        val leftBranch = Path().apply {
            moveTo(w * 0.50f, h * 0.52f)
            cubicTo(w * 0.50f, h * 0.40f, w * 0.30f, h * 0.38f, w * 0.28f, h * 0.24f)
        }
        drawPath(path = leftBranch, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Left arrowhead
        val leftHead = Path().apply {
            moveTo(w * 0.20f, h * 0.34f)
            lineTo(w * 0.28f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.30f)
        }
        drawPath(path = leftHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Right diverging branch
        val rightBranch = Path().apply {
            moveTo(w * 0.50f, h * 0.52f)
            cubicTo(w * 0.50f, h * 0.40f, w * 0.70f, h * 0.38f, w * 0.72f, h * 0.24f)
        }
        drawPath(path = rightBranch, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))

        // Right arrowhead
        val rightHead = Path().apply {
            moveTo(w * 0.62f, h * 0.30f)
            lineTo(w * 0.72f, h * 0.24f)
            lineTo(w * 0.80f, h * 0.34f)
        }
        drawPath(path = rightHead, color = tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
