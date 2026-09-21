package com.omkarnub.kanri.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.analytics.DayStreakStatus
import com.omkarnub.kanri.data.analytics.StreakDataStore
import com.omkarnub.kanri.data.analytics.StreakState

private val ColorFlameAmber = Color(0xFFF5A524)

@Composable
fun NoSpendStreakCard(
    streakState: StreakState,
    lastCelebratedMilestone: Int,
    onMilestoneCelebrated: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Milestone micro-animation (scale + glow)
    val scaleAnim = remember { Animatable(1f) }
    val glowAnim = remember { Animatable(0f) }

    val isMilestone = streakState.currentStreak in StreakDataStore.MILESTONES &&
            streakState.currentStreak > lastCelebratedMilestone

    LaunchedEffect(streakState.currentStreak, lastCelebratedMilestone) {
        if (isMilestone) {
            // Trigger animation
            scaleAnim.animateTo(
                targetValue = 1.25f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            glowAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            glowAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
            onMilestoneCelebrated(streakState.currentStreak)
        }
    }

    val accessibilityDesc = if (streakState.isFirstUse || (streakState.currentStreak == 0 && streakState.lastStreak == 0)) {
        "No spend streak. Start your streak today."
    } else {
        "No spend streak: ${streakState.currentStreak} days. Best streak: ${streakState.bestStreak} days. ${streakState.noSpendDaysThisMonth} no-spend days in ${streakState.currentMonthName}."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityDesc },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            // 1. Label Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NO-SPEND STREAK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (streakState.currentStreak >= 3) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ColorFlameAmber.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, ColorFlameAmber.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "ON FIRE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                fontSize = 10.sp
                            ),
                            color = ColorFlameAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Hero Row: Flame Icon + Streak Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.scale(scaleAnim.value)
            ) {
                // Flame Vector Icon with glow effect on milestone
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (glowAnim.value > 0f) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ColorFlameAmber.copy(alpha = 0.35f * glowAnim.value))
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "No-spend streak flame",
                        tint = if (streakState.currentStreak > 0) ColorFlameAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (streakState.isFirstUse || (streakState.currentStreak == 0 && streakState.lastStreak == 0)) {
                    // Zero state
                    Text(
                        text = "Start your streak today",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (streakState.currentStreak > 0) {
                    Text(
                        text = "${streakState.currentStreak} ${if (streakState.currentStreak == 1) "day" else "days"}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    // Today spent: current streak is 0, show last streak
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "0 days",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "(Last: ${streakState.lastStreak} ${if (streakState.lastStreak == 1) "day" else "days"})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Subtitle
            val subtitleText = if (streakState.isFirstUse || (streakState.currentStreak == 0 && streakState.lastStreak == 0)) {
                "Every day without spending builds financial discipline."
            } else {
                "Best: ${streakState.bestStreak} ${if (streakState.bestStreak == 1) "day" else "days"} · ${streakState.noSpendDaysThisMonth} no-spend days in ${streakState.currentMonthName}"
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. 7-Dot Week Strip
            WeekStripRow(
                days = streakState.last7Days,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WeekStripRow(
    days: List<DayStreakStatus>,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            days.forEach { dayStatus ->
                DayDotItem(dayStatus = dayStatus)
            }
        }
    }
}

@Composable
private fun DayDotItem(dayStatus: DayStreakStatus) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Dot representation:
        // isToday: ring (14dp with border, filled if isNoSpend)
        // isNoSpend: filled dot (10dp)
        // hollow: spent / not no-spend (10dp hollow)
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (dayStatus.isToday) {
                // Today: Ring
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.8.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )
                        .background(
                            if (dayStatus.isNoSpend) MaterialTheme.colorScheme.onSurface else Color.Transparent
                        )
                )
            } else if (dayStatus.isNoSpend) {
                // Filled dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                )
            } else {
                // Hollow dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .border(
                            width = 1.2.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }

        // Day label (e.g. M, T, W, T, F, S, S)
        Text(
            text = dayStatus.dayLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (dayStatus.isToday) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            ),
            color = if (dayStatus.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}
