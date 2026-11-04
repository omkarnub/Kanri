package com.omkarnub.kanri.ui.greeting

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.FlipFadeText
import com.omkarnub.kanri.ui.theme.LocalIsDarkTheme

/**
 * Section 3: Greeting and dynamic date / financial subtitle header.
 * Displays greeting with FlipFadeText 3D animation, and subtitle with tone-based coloring,
 * 300ms crossfade without layout jump, and contextual click actions.
 */
@Composable
fun GreetingHeader(
    greetingResult: GreetingResult,
    modifier: Modifier = Modifier,
    onNavigateToInsights: () -> Unit = {},
    onOpenReviewQueue: () -> Unit = {},
    onGreetingDisplayed: (GreetingResult) -> Unit = {}
) {
    LaunchedEffect(greetingResult) {
        onGreetingDisplayed(greetingResult)
    }

    val isDark = LocalIsDarkTheme.current
    val neutralColor = if (isDark) Color(0xFFE8E8ED) else Color(0xFF4C4C54)

    val subtitleColor = when (greetingResult.tone) {
        Tone.Neutral -> neutralColor
        Tone.Positive -> if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
        Tone.Warning -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    }

    val isClickable = greetingResult.action != GreetingAction.None
    val clickModifier = if (isClickable) {
        Modifier.clickable {
            when (greetingResult.action) {
                GreetingAction.OpenInsights -> onNavigateToInsights()
                GreetingAction.OpenReviewSheet -> onOpenReviewQueue()
                GreetingAction.None -> Unit
            }
        }
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${greetingResult.greeting}, ${greetingResult.subtitle}"
            },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FlipFadeText(
            text = greetingResult.greeting,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            letterDurationMillis = 420,
            staggerDelayMillis = 18
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .then(clickModifier),
            contentAlignment = Alignment.CenterStart
        ) {
            AnimatedContent(
                targetState = greetingResult.subtitle to subtitleColor,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "SubtitleCrossfade"
            ) { (text, color) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
