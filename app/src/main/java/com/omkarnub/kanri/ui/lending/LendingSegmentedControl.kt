package com.omkarnub.kanri.ui.lending

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass

@Composable
fun LendingSegmentedControl(
    selectedTab: LendingTabOption,
    onTabSelected: (LendingTabOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current

    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Flat,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            LendingTabOption.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val selectedBg = tokens?.accent?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.inverseSurface
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) selectedBg else Color.Transparent,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabBg"
                )
                val selectedText = tokens?.accent ?: MaterialTheme.colorScheme.inverseOnSurface
                val unselectedText = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) selectedText else unselectedText,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "tabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (tab) {
                            LendingTabOption.PEOPLE -> "People"
                            LendingTabOption.TIMELINE -> "Timeline"
                        },
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.5.sp
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}
