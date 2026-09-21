package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.WeekendVsWeekdayData

@Composable
fun WeekendVsWeekdaySection(
    data: WeekendVsWeekdayData,
    modifier: Modifier = Modifier
) {
    val maxAvg = maxOf(data.weekdayAvgPerDay, data.weekendAvgPerDay, 1.0)
    val weekdayBarRatio = (data.weekdayAvgPerDay / maxAvg).toFloat().coerceIn(0.05f, 1f)
    val weekendBarRatio = (data.weekendAvgPerDay / maxAvg).toFloat().coerceIn(0.05f, 1f)

    val animatedWeekday by animateFloatAsState(
        targetValue = weekdayBarRatio,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "weekdayProgress"
    )
    val animatedWeekend by animateFloatAsState(
        targetValue = weekendBarRatio,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "weekendProgress"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Weekend vs weekday spend: ${data.insightSentence}"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "WEEKEND VS WEEKDAY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two columns: Weekdays and Weekends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Weekday Column
                DayTypeColumn(
                    label = "Weekdays",
                    avgAmount = data.weekdayAvgPerDay,
                    totalAmount = data.weekdayTotal,
                    topCategory = data.weekdayTopCategory,
                    modifier = Modifier.weight(1f)
                )

                // Weekend Column
                DayTypeColumn(
                    label = "Weekends",
                    avgAmount = data.weekendAvgPerDay,
                    totalAmount = data.weekendTotal,
                    topCategory = data.weekendTopCategory,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Normalized Comparison Bars
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Weekday Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Mon–Fri",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedWeekday)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        )
                    }
                }

                // Weekend Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sat–Sun",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedWeekend)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.onSurface)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Insight line
            Text(
                text = data.insightSentence,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
private fun DayTypeColumn(
    label: String,
    avgAmount: Double,
    totalAmount: Double,
    topCategory: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${formatCurrency(avgAmount)} / day",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp
            )
        )

        Text(
            text = "Total: ${formatCurrency(totalAmount)}",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        )

        if (topCategory != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Top: $topCategory",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
