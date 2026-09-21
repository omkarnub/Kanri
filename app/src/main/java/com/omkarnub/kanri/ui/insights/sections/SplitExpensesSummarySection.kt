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
import com.omkarnub.kanri.ui.insights.SplitExpensesSummaryData
import com.omkarnub.kanri.ui.insights.SplitPendingPerson

private val IncomeSage = Color(0xFF30A46C)
private val WarningAmber = Color(0xFFF5A524)

@Composable
fun SplitExpensesSummarySection(
    data: SplitExpensesSummaryData,
    onOpenLending: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenLending)
            .semantics {
                contentDescription = "Split expenses: your share ${formatCurrency(data.myOwnShareTotal)} of ${formatCurrency(data.billsPaidByMeTotal)}"
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPLIT EXPENSES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "${data.splitEventsCount} split${if (data.splitEventsCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hero: Your share ₹X of ₹Y in bills
            Text(
                text = "Your share: ${formatCurrency(data.myOwnShareTotal)} of ${formatCurrency(data.billsPaidByMeTotal)} in bills",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar: Recovered ₹A · Pending ₹B
            val totalLent = data.recoveredTotal + data.pendingTotal
            val recoveredRatio = if (totalLent > 0) (data.recoveredTotal / totalLent).toFloat().coerceIn(0f, 1f) else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recovered: ${formatCurrency(data.recoveredTotal)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = IncomeSage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "Pending: ${formatCurrency(data.pendingTotal)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WarningAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            ) {
                val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                drawRoundRect(
                    color = WarningAmber.copy(alpha = 0.35f),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = corner
                )
                if (recoveredRatio > 0f) {
                    drawRoundRect(
                        color = IncomeSage,
                        topLeft = Offset.Zero,
                        size = Size(size.width * recoveredRatio, size.height),
                        cornerRadius = corner
                    )
                }
            }

            // Top pending people
            if (data.topPendingPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "PENDING BY PERSON",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.topPendingPeople.forEach { debtor ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = debtor.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = formatCurrency(debtor.pendingAmount),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarningAmber
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
