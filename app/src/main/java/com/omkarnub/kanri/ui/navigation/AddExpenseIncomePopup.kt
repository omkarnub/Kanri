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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.omkarnub.kanri.ui.theme.ThemeMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

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
                .background(Color.Black.copy(alpha = 0.35f))
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
            enter = fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                    scaleIn(
                        initialScale = 0.82f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
            exit = fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 0.88f, animationSpec = tween(160)) +
                    slideOutVertically(targetOffsetY = { it / 6 }, animationSpec = tween(160))
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 86.dp) // Sits gracefully right above the 64dp floating dock
                    .width(300.dp)
                    .shadow(
                        elevation = glassTheme.shadowElevation + 6.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = glassTheme.shadowColor,
                        ambientColor = glassTheme.shadowColor.copy(alpha = glassTheme.shadowColor.alpha * 0.7f)
                    )
                    .clip(RoundedCornerShape(26.dp))
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = glassTheme.popupHazeStyle
                            )
                        } else {
                            Modifier
                        }
                    )
                    .border(
                        glassTheme.glassBorder,
                        shape = RoundedCornerShape(26.dp)
                    ),
                shape = RoundedCornerShape(26.dp),
                color = if (hazeState != null) Color.Transparent else glassTheme.fallbackBackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Expense Option
                    PopupOptionCard(
                        title = "Expense",
                        subtitle = "Log spent money",
                        accentColor = ExpenseRed,
                        icon = { tint -> ExpenseFlowIcon(tint = tint, size = 24.dp) },
                        onClick = {
                            onDismiss()
                            onAddExpense()
                        }
                    )

                    // 2. Income Option
                    PopupOptionCard(
                        title = "Income",
                        subtitle = "Log received money",
                        accentColor = IncomeGreen,
                        icon = { tint -> IncomeFlowIcon(tint = tint, size = 24.dp) },
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
    onClick: () -> Unit
) {
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "popupOptionScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            ),
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                icon(accentColor)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accentColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.Gray,
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
