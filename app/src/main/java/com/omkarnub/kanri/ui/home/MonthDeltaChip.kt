package com.omkarnub.kanri.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.analytics.Delta
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ColorDeltaUp = Color(0xFFE5484D)
private val ColorDeltaDown = Color(0xFF34C759)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthDeltaChip(
    delta: Delta,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = delta !is Delta.NoData,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = delta,
            transitionSpec = {
                fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
            },
            label = "monthDeltaCrossfade"
        ) { currentDelta ->
            when (currentDelta) {
                is Delta.NoData -> Box(Modifier.sizeIn(minWidth = 0.dp, minHeight = 0.dp))
                is Delta.Up, is Delta.Down, is Delta.Flat -> {
                    val tooltipState = rememberTooltipState(isPersistent = false)
                    val scope = rememberCoroutineScope()

                    val (percentVal, absVal, desc, textColor, symbol) = when (currentDelta) {
                        is Delta.Up -> DeltaDisplayInfo(
                            percent = currentDelta.percent,
                            absolute = currentDelta.absolute,
                            desc = currentDelta.prevComparisonDesc,
                            color = ColorDeltaUp,
                            symbol = "▲"
                        )
                        is Delta.Down -> DeltaDisplayInfo(
                            percent = currentDelta.percent,
                            absolute = currentDelta.absolute,
                            desc = currentDelta.prevComparisonDesc,
                            color = ColorDeltaDown,
                            symbol = "▼"
                        )
                        is Delta.Flat -> DeltaDisplayInfo(
                            percent = 0,
                            absolute = currentDelta.absolute,
                            desc = currentDelta.prevComparisonDesc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            symbol = "–"
                        )
                        else -> DeltaDisplayInfo()
                    }

                    // Animate count-up with animateFloatAsState (600ms, FastOutSlowInEasing)
                    val animatedPercent by animateFloatAsState(
                        targetValue = percentVal.toFloat(),
                        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                        label = "deltaPercentCountUp"
                    )

                    val tooltipText = when (currentDelta) {
                        is Delta.Up -> "${CurrencyUtils.formatCurrency(absVal)} more than $desc"
                        is Delta.Down -> "${CurrencyUtils.formatCurrency(absVal)} less than $desc"
                        is Delta.Flat -> "About the same as $desc"
                        else -> ""
                    }

                    val chipText = if (currentDelta is Delta.Flat) {
                        "$symbol vs ${extractShortDesc(desc)}"
                    } else {
                        "$symbol ${animatedPercent.roundToInt()}% vs ${extractShortDesc(desc)}"
                    }

                    val accessibilityDescription = when (currentDelta) {
                        is Delta.Up -> "Spending increased by ${percentVal}% compared to $desc"
                        is Delta.Down -> "Spending decreased by ${percentVal}% compared to $desc"
                        is Delta.Flat -> "Spending flat compared to $desc"
                        else -> ""
                    }

                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip(
                                shape = RoundedCornerShape(8.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = tooltipText,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    fontSize = 12.sp
                                )
                            }
                        },
                        state = tooltipState
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                0.5.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 28.dp)
                                .semantics {
                                    contentDescription = accessibilityDescription
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        scope.launch {
                                            if (tooltipState.isVisible) {
                                                tooltipState.dismiss()
                                            } else {
                                                tooltipState.show()
                                            }
                                        }
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = chipText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.2.sp
                                    ),
                                    color = textColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DeltaDisplayInfo(
    val percent: Int = 0,
    val absolute: Double = 0.0,
    val desc: String = "",
    val color: Color = Color.Unspecified,
    val symbol: String = ""
)

/**
 * Extracts month name if desc is like "August 1–20", returning "August".
 */
private fun extractShortDesc(desc: String): String {
    val spaceIndex = desc.indexOf(' ')
    return if (spaceIndex > 0) desc.substring(0, spaceIndex) else desc
}
