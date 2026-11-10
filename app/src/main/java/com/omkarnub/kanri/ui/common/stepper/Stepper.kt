package com.omkarnub.kanri.ui.common.stepper

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.MonochromeBlack
import com.omkarnub.kanri.util.rememberKanriHaptics

/**
 * Visual variant for the Stepper.
 * - CIRCLE: Connected circular badges with step numbers/icons and checkmarks.
 * - CIRCLE_ALT: Centered vertical layout per step with labels.
 * - LINE: Segmented horizontal progress lines across steps.
 */
enum class StepperVariant {
    CIRCLE,
    CIRCLE_ALT,
    LINE
}

/**
 * Layout orientation for the Stepper.
 */
enum class StepperOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * State of an individual step.
 */
enum class StepState {
    DEFAULT,
    LOADING,
    ERROR
}

/**
 * Sizing presets for Stepper badges.
 */
enum class StepperSize(
    val containerSize: Dp,
    val iconSize: Dp,
    val fontSize: TextUnit,
    val strokeWidth: Dp
) {
    SM(containerSize = 26.dp, iconSize = 13.dp, fontSize = 11.sp, strokeWidth = 1.5.dp),
    MD(containerSize = 34.dp, iconSize = 16.dp, fontSize = 13.sp, strokeWidth = 2.dp),
    LG(containerSize = 42.dp, iconSize = 20.dp, fontSize = 15.sp, strokeWidth = 2.dp)
}

/**
 * Data model for a step in the Stepper.
 */
data class StepItem(
    val id: String? = null,
    val label: String? = null,
    val description: String? = null,
    val icon: ImageVector? = null,
    val optional: Boolean = false,
    val state: StepState = StepState.DEFAULT
)

/**
 * Modern interactive Stepper component in Jetpack Compose, translating the
 * full functionality of the React/Next Stepper design system.
 *
 * Supports:
 * - Active, completed, and pending step states
 * - Animated checkmark on step completion
 * - Animated connector progress lines
 * - Error and loading states
 * - Interactive step clicks with haptic feedback
 * - Flexible CIRCLE, CIRCLE_ALT, and LINE variants
 */
@Composable
fun Stepper(
    steps: List<StepItem>,
    activeStep: Int,
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,
    variant: StepperVariant = StepperVariant.CIRCLE,
    orientation: StepperOrientation = StepperOrientation.HORIZONTAL,
    size: StepperSize = StepperSize.SM,
    showLabels: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onPrimaryColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    when (orientation) {
        StepperOrientation.HORIZONTAL -> {
            HorizontalStepper(
                steps = steps,
                activeStep = activeStep,
                modifier = modifier,
                onStepClick = onStepClick,
                variant = variant,
                size = size,
                showLabels = showLabels,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor
            )
        }
        StepperOrientation.VERTICAL -> {
            VerticalStepper(
                steps = steps,
                activeStep = activeStep,
                modifier = modifier,
                onStepClick = onStepClick,
                variant = variant,
                size = size,
                primaryColor = primaryColor,
                onPrimaryColor = onPrimaryColor
            )
        }
    }
}

@Composable
private fun HorizontalStepper(
    steps: List<StepItem>,
    activeStep: Int,
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,
    variant: StepperVariant = StepperVariant.CIRCLE,
    size: StepperSize = StepperSize.SM,
    showLabels: Boolean = false,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onPrimaryColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val haptics = rememberKanriHaptics()
    val isLight = MaterialTheme.colorScheme.background == Color(0xFFFAF9F5)
    val activeColor = if (isLight) MonochromeBlack else DarkOffWhite
    val inactiveColor = if (isLight) Color(0x33000000) else Color(0x28FFFFFF)
    val inactiveContentColor = if (isLight) Color(0x77000000) else Color(0x66FFFFFF)

    if (variant == StepperVariant.LINE) {
        // Line variant: segmented progress lines across top
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, step ->
                val isCompleted = index < activeStep
                val isCurrent = index == activeStep
                val lineBg by animateColorAsState(
                    targetValue = when {
                        isCompleted || isCurrent -> activeColor
                        else -> inactiveColor
                    },
                    animationSpec = tween(300),
                    label = "line_bg_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(lineBg)
                        .then(
                            if (onStepClick != null) {
                                Modifier.clickable(
                                    role = Role.Button,
                                    onClick = {
                                        haptics.tick()
                                        onStepClick(index)
                                    }
                                )
                            } else Modifier
                        )
                )
            }
        }
        return
    }

    // CIRCLE and CIRCLE_ALT variants
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index < activeStep
            val isCurrent = index == activeStep
            val isClickable = onStepClick != null

            // Step Button + Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                StepCircleButton(
                    index = index,
                    step = step,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    isClickable = isClickable,
                    size = size,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    inactiveContentColor = inactiveContentColor,
                    onClick = {
                        if (isClickable) {
                            haptics.tick()
                            onStepClick(index)
                        }
                    }
                )

                if (showLabels && !step.label.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.label,
                        fontFamily = GoogleSansFlex,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent || isCompleted) activeColor else inactiveContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Connector line between this step and the next
            if (index < steps.size - 1) {
                val lineCompleted = index < activeStep
                val lineColor by animateColorAsState(
                    targetValue = if (lineCompleted) activeColor else inactiveColor,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "connector_line_$index"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(lineColor)
                )
            }
        }
    }
}

@Composable
private fun StepCircleButton(
    index: Int,
    step: StepItem,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isClickable: Boolean,
    size: StepperSize,
    activeColor: Color,
    inactiveColor: Color,
    inactiveContentColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "step_scale_$index"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isCompleted -> activeColor
            isCurrent -> activeColor.copy(alpha = 0.15f)
            else -> Color.Transparent
        },
        animationSpec = tween(250),
        label = "step_bg_$index"
    )

    val borderStroke = when {
        step.state == StepState.ERROR -> BorderStroke(size.strokeWidth, ExpenseRed)
        isCompleted -> null
        isCurrent -> BorderStroke(size.strokeWidth, activeColor)
        else -> BorderStroke(size.strokeWidth, inactiveColor)
    }

    val contentColor by animateColorAsState(
        targetValue = when {
            step.state == StepState.ERROR -> ExpenseRed
            isCompleted -> MonochromeBlack
            isCurrent -> activeColor
            else -> inactiveContentColor
        },
        animationSpec = tween(250),
        label = "step_content_color_$index"
    )

    Surface(
        modifier = Modifier
            .size(size.containerSize * scale)
            .then(
                if (isClickable) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = CircleShape,
        color = bgColor,
        border = borderStroke
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size.containerSize)
        ) {
            AnimatedContent(
                targetState = Triple(isCompleted, isCurrent, step.state),
                transitionSpec = {
                    (scaleIn(tween(220)) + fadeIn(tween(220))) togetherWith
                            (scaleOut(tween(180)) + fadeOut(tween(180)))
                },
                label = "step_icon_$index"
            ) { (completed, current, state) ->
                when {
                    state == StepState.LOADING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(size.iconSize),
                            color = contentColor,
                            strokeWidth = 1.5.dp
                        )
                    }
                    state == StepState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = ExpenseRed,
                            modifier = Modifier.size(size.iconSize)
                        )
                    }
                    completed -> {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MonochromeBlack,
                            modifier = Modifier.size(size.iconSize)
                        )
                    }
                    step.icon != null -> {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = step.label ?: "Step ${index + 1}",
                            tint = contentColor,
                            modifier = Modifier.size(size.iconSize)
                        )
                    }
                    else -> {
                        Text(
                            text = "${index + 1}",
                            fontFamily = GoogleSansFlex,
                            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
                            fontSize = size.fontSize,
                            color = contentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalStepper(
    steps: List<StepItem>,
    activeStep: Int,
    modifier: Modifier = Modifier,
    onStepClick: ((Int) -> Unit)? = null,
    variant: StepperVariant = StepperVariant.CIRCLE,
    size: StepperSize = StepperSize.SM,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    onPrimaryColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val haptics = rememberKanriHaptics()
    val isLight = MaterialTheme.colorScheme.background == Color(0xFFFAF9F5)
    val activeColor = if (isLight) MonochromeBlack else DarkOffWhite
    val inactiveColor = if (isLight) Color(0x33000000) else Color(0x28FFFFFF)
    val inactiveContentColor = if (isLight) Color(0x77000000) else Color(0x66FFFFFF)

    Column(modifier = modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val isCompleted = index < activeStep
            val isCurrent = index == activeStep
            val isClickable = onStepClick != null

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isClickable) {
                            Modifier.clickable {
                                haptics.tick()
                                onStepClick(index)
                            }
                        } else Modifier
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step button
                StepCircleButton(
                    index = index,
                    step = step,
                    isCompleted = isCompleted,
                    isCurrent = isCurrent,
                    isClickable = false,
                    size = size,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    inactiveContentColor = inactiveContentColor,
                    onClick = {}
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Label and Description
                Column(modifier = Modifier.weight(1f)) {
                    if (!step.label.isNullOrBlank()) {
                        Text(
                            text = step.label,
                            fontFamily = GoogleSansFlex,
                            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (isCurrent || isCompleted) activeColor else inactiveContentColor
                        )
                    }
                    if (!step.description.isNullOrBlank()) {
                        Text(
                            text = step.description,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = inactiveContentColor
                        )
                    }
                }
            }

            // Vertical line between steps
            if (index < steps.size - 1) {
                val lineCompleted = index < activeStep
                Box(
                    modifier = Modifier
                        .padding(start = size.containerSize / 2 - 1.dp)
                        .width(2.dp)
                        .height(16.dp)
                        .background(if (lineCompleted) activeColor else inactiveColor)
                )
            }
        }
    }
}

