package com.omkarnub.kanri.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex

data class SetupStepItem(
    val label: String,
    val completed: Boolean
)

/**
 * 1:1 Translation of Setup Progress Pattern (shadcn/ui / lucide-react) to Jetpack Compose.
 * Features:
 * - "Setup Progress" & "X of Y steps" label header
 * - Smooth animated LinearProgressIndicator bar
 * - Step items with CircleCheckIcon (success) and CircleIcon (muted outline)
 * - Tactile back button integration
 */
@Composable
fun SetupProgressPattern(
    steps: List<SetupStepItem>,
    activeStepIndex: Int,
    onStepClick: ((Int) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val completedSteps = steps.filter { it.completed }.size
    val targetProgress = if (steps.isEmpty()) 0f else completedSteps.toFloat() / steps.size.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 400),
        label = "setup_progress"
    )

    val scrollState = rememberScrollState()
    LaunchedEffect(activeStepIndex) {
        if (activeStepIndex >= 0 && steps.isNotEmpty()) {
            val targetScroll = (activeStepIndex * 90).coerceAtLeast(0)
            scrollState.animateScrollTo(targetScroll)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(1.5.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.04f))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.90f))
            .border(1.dp, Color(0xFF141414).copy(alpha = 0.10f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Back button (if available), Title & Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onBackClick != null) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF1E2229),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Setup Progress",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = Color(0xFF1E2229)
                    )
                }

                Text(
                    text = "$completedSteps of ${steps.size} steps",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            // Progress Bar (LinearProgressIndicator)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF0F0E0C),
                trackColor = Color(0xFFE8E5DD)
            )

            // Step Items Row with CircleCheckIcon and CircleIcon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    val isCurrent = index == activeStepIndex
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = onStepClick != null) {
                                onStepClick?.invoke(index)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        if (step.completed) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF2E7D32), // text-success
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .border(
                                        width = if (isCurrent) 1.8.dp else 1.2.dp,
                                        color = if (isCurrent) Color(0xFF0F0E0C) else Color(0xFFAAAAAA),
                                        shape = CircleShape
                                    )
                            )
                        }

                        Text(
                            text = step.label,
                            fontFamily = GoogleSansFlex,
                            fontSize = 12.5.sp,
                            fontWeight = if (isCurrent || step.completed) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isCurrent) Color(0xFF0F0E0C)
                            else if (step.completed) Color(0xFF2E7D32)
                            else Color(0xFF888888)
                        )
                    }
                }
            }
        }
    }
}

