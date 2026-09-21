package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.IncomeRegularityTag
import com.omkarnub.kanri.ui.insights.IncomeSourceItem
import com.omkarnub.kanri.ui.insights.IncomeSourcesData
import java.time.format.DateTimeFormatter

private val IncomeSage = Color(0xFF30A46C)

@Composable
fun IncomeSourcesSection(
    data: IncomeSourcesData,
    onSourceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = data.regularTotal + data.irregularTotal

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Income sources: total ${formatCurrency(totalIncome)}, ${data.sources.size} sources"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "INCOME SOURCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Two-segment bar "Regular ₹X · Irregular ₹Y" with percentages
            if (totalIncome > 0) {
                val regularPct = (data.regularPercent / 100.0).toFloat().coerceIn(0f, 1f)
                val irregularPct = (data.irregularPercent / 100.0).toFloat().coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Regular: ${formatCurrency(data.regularTotal)} (${data.regularPercent.toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = IncomeSage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Irregular: ${formatCurrency(data.irregularTotal)} (${data.irregularPercent.toInt()}%)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                ) {
                    val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = corner
                    )
                    if (regularPct > 0f) {
                        drawRoundRect(
                            color = IncomeSage,
                            topLeft = Offset.Zero,
                            size = Size(size.width * regularPct, size.height),
                            cornerRadius = corner
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sources List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.sources.forEach { source ->
                    IncomeSourceRow(
                        source = source,
                        onClick = { onSourceClick(source.rawCounterparty) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeSourceRow(
    source: IncomeSourceItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Regularity Tag
                val (tagBg, tagText, tagLabel) = when (source.tag) {
                    IncomeRegularityTag.REGULAR_STEADY -> Triple(IncomeSage.copy(alpha = 0.15f), IncomeSage, "Regular · steady")
                    IncomeRegularityTag.REGULAR -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, "Regular")
                    IncomeRegularityTag.IRREGULAR -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Irregular")
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagBg
                ) {
                    Text(
                        text = tagLabel,
                        color = tagText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Sub-lines: Last received date & typical arrival day
            val dtf = DateTimeFormatter.ofPattern("d MMM")
            val subDetails = mutableListOf<String>()
            subDetails.add("${source.transactionCount} payment${if (source.transactionCount == 1) "" else "s"}")
            source.lastReceivedDate?.let { date ->
                subDetails.add("last ${date.format(dtf)}")
            }
            source.typicalArrivalDayOfMonth?.let { day ->
                val suffix = when (day) {
                    1, 21, 31 -> "st"
                    2, 22 -> "nd"
                    3, 23 -> "rd"
                    else -> "th"
                }
                subDetails.add("usually around the $day$suffix")
            }

            Text(
                text = subDetails.joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = formatCurrency(source.totalReceived),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = IncomeSage
            )
        )
    }
}
