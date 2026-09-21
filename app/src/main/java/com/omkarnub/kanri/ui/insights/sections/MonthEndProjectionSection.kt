package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.MonthEndProjectionData
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)
private val WarningAmber = Color(0xFFF5A524)

@Composable
fun MonthEndProjectionSection(
    data: MonthEndProjectionData,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                val budgetDesc = if (data.budget != null) ", budget ${formatCurrency(data.budget)}" else ""
                contentDescription = "Month-end projection: ${formatCurrency(data.projectedTotal)}$budgetDesc"
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTH-END PROJECTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (data.budget == null) {
                    TextButton(onClick = onSetBudgetClick) {
                        Text(
                            text = "Set a budget",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hero and Budget status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Projected Spend",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    AnimatedNumberText(
                        text = formatCurrency(data.projectedTotal),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 26.sp
                        )
                    )
                }

                if (data.budget != null && data.gap != null) {
                    val gap = data.gap
                    val budget = data.budget
                    val isNearLimit = abs(gap) <= (budget * 0.05)

                    val (chipText, chipColor) = when {
                        isNearLimit -> "Near limit" to WarningAmber
                        gap > 0 -> "${formatCurrency(gap)} over budget" to ExpenseRed
                        else -> "${formatCurrency(abs(gap))} under budget" to IncomeSage
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = chipColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = chipText,
                            color = chipColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Three-segment / progress visualization
            val maxScale = maxOf(data.projectedTotal, data.budget ?: 0.0, data.spentSoFar, 1.0)
            val spentRatio = (data.spentSoFar / maxScale).toFloat().coerceIn(0f, 1f)
            val projectedRatio = (data.projectedTotal / maxScale).toFloat().coerceIn(0f, 1f)
            val budgetRatio = ((data.budget ?: 0.0) / maxScale).toFloat().coerceIn(0f, 1f)

            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            val amberColor = WarningAmber

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                // Background track
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = cornerRadius
                )

                // Projected remainder (lighter step)
                if (projectedRatio > 0f) {
                    drawRoundRect(
                        color = onSurfaceColor.copy(alpha = 0.25f),
                        topLeft = Offset.Zero,
                        size = Size(size.width * projectedRatio, size.height),
                        cornerRadius = cornerRadius
                    )
                }

                // Solid spent so far
                if (spentRatio > 0f) {
                    drawRoundRect(
                        color = onSurfaceColor,
                        topLeft = Offset.Zero,
                        size = Size(size.width * spentRatio, size.height),
                        cornerRadius = cornerRadius
                    )
                }

                // Thin vertical tick for budget
                if (data.budget != null && budgetRatio > 0f) {
                    val tickX = size.width * budgetRatio
                    drawLine(
                        color = amberColor,
                        start = Offset(tickX, -2.dp.toPx()),
                        end = Offset(tickX, size.height + 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-lines: Budget exhaustion date and last month spend
            val subLines = mutableListOf<String>()
            if (data.budgetExhaustionDate != null) {
                val dtf = DateTimeFormatter.ofPattern("d MMM")
                subLines.add("Budget runs out around ${data.budgetExhaustionDate.format(dtf)}")
            }
            val lastMonth = data.lastMonthTotal
            if (lastMonth != null && lastMonth > 0) {
                subLines.add("Last month: ${formatCurrency(lastMonth)}")
            }

            if (subLines.isNotEmpty()) {
                Text(
                    text = subLines.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
