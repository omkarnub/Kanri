package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.SmallSpendPayee
import com.omkarnub.kanri.ui.insights.SmallSpendsData

@Composable
fun SmallSpendsSection(
    data: SmallSpendsData,
    onThresholdSelected: (Double) -> Unit,
    onCardClick: () -> Unit,
    onPayeeClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onCardClick)
            .semantics {
                contentDescription = "Small spends: total ${formatCurrency(data.totalAmount)}, ${data.count} payments under threshold"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header with Threshold Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SMALL SPENDS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Chips: ₹50 · ₹100 · ₹200
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(50.0, 100.0, 200.0).forEach { th ->
                        val isSelected = data.threshold == th
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onThresholdSelected(th) }
                        ) {
                            Text(
                                text = "₹${th.toInt()}",
                                color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (data.count == 0) {
                Text(
                    text = "No small payments in this range",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                // Hero: Total of small payments
                AnimatedNumberText(
                    text = formatCurrency(data.totalAmount),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 24.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Sub-line: "47 payments · avg ₹73 · 14% of spending"
                val pct = String.format(java.util.Locale.US, "%.0f%%", data.percentOfTotalSpend)
                Text(
                    text = "${data.count} payments  ·  avg ${formatCurrency(data.averageAmount)}  ·  $pct of spending",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )

                // Top 3 payees within small spends
                if (data.topPayees.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.topPayees.forEach { payee ->
                            SmallSpendPayeeRow(
                                payee = payee,
                                onClick = { onPayeeClick(payee.name) }
                            )
                        }
                    }
                }

                // Yearly extrapolation line
                if (data.yearlyPaceAmount > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "At this pace ≈ ${formatCurrency(data.yearlyPaceAmount)} a year",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallSpendPayeeRow(
    payee: SmallSpendPayee,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = payee.name,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "(${payee.count} txns)",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }

        Text(
            text = formatCurrency(payee.total),
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
