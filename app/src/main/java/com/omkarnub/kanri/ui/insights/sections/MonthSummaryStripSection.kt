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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.SummaryStripData
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

enum class MetricType {
    SPENT,
    INCOME,
    NET
}

@Composable
fun MonthSummaryStripSection(
    data: SummaryStripData,
    mode: InsightsMode,
    onSpentClick: () -> Unit,
    onIncomeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Summary: Spent ${formatCurrency(data.spent)}, Income ${formatCurrency(data.income)}, Net ${formatCurrency(data.net)}"
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
            // Header label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${data.transactionCount} transactions · ${formatCurrency(data.averagePerDay)} / day",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3 Columns: In Expense mode: Spent, Income, Net. In Income mode: Income, Spent, Net.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (mode == InsightsMode.EXPENSE) {
                    SummaryColumn(
                        label = "Spent",
                        amount = data.spent,
                        deltaPercent = data.deltaSpentPercent,
                        deltaAmount = data.deltaSpentAmount,
                        metricType = MetricType.SPENT,
                        isTappable = true,
                        onClick = onSpentClick,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryColumn(
                        label = "Income",
                        amount = data.income,
                        deltaPercent = data.deltaIncomePercent,
                        deltaAmount = data.deltaIncomeAmount,
                        metricType = MetricType.INCOME,
                        isTappable = true,
                        onClick = onIncomeClick,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryColumn(
                        label = "Net",
                        amount = data.net,
                        deltaPercent = data.deltaNetPercent,
                        deltaAmount = data.deltaNetAmount,
                        metricType = MetricType.NET,
                        isTappable = false,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        allowNegativePrefix = true
                    )
                } else {
                    SummaryColumn(
                        label = "Income",
                        amount = data.income,
                        deltaPercent = data.deltaIncomePercent,
                        deltaAmount = data.deltaIncomeAmount,
                        metricType = MetricType.INCOME,
                        isTappable = true,
                        onClick = onIncomeClick,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryColumn(
                        label = "Spent",
                        amount = data.spent,
                        deltaPercent = data.deltaSpentPercent,
                        deltaAmount = data.deltaSpentAmount,
                        metricType = MetricType.SPENT,
                        isTappable = true,
                        onClick = onSpentClick,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryColumn(
                        label = "Net",
                        amount = data.net,
                        deltaPercent = data.deltaNetPercent,
                        deltaAmount = data.deltaNetAmount,
                        metricType = MetricType.NET,
                        isTappable = false,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        allowNegativePrefix = true
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryColumn(
    label: String,
    amount: Double,
    deltaPercent: Double?,
    deltaAmount: Double?,
    metricType: MetricType,
    isTappable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowNegativePrefix: Boolean = false
) {
    var showAbsoluteDifference by remember { mutableStateOf(false) }

    val digitColor = when (metricType) {
        MetricType.SPENT -> ExpenseRed
        MetricType.INCOME -> IncomeSage
        MetricType.NET -> when {
            amount > 0 -> IncomeSage
            amount < 0 -> ExpenseRed
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    Column(
        modifier = modifier
            .then(
                if (isTappable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Hero value with colored rolling digits
        val isNegative = allowNegativePrefix && amount < 0
        val displayAmount = if (isNegative) -amount else amount
        val formattedAmount = (if (isNegative) "- " else "") + formatCurrency(displayAmount)

        AnimatedNumberText(
            text = formattedAmount,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = digitColor,
                fontSize = 18.sp
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Delta Chip: only displayed when there is a valid comparison period (no "New" badges)
        if (deltaPercent != null) {
            SummaryDeltaChip(
                deltaPercent = deltaPercent,
                deltaAmount = deltaAmount,
                metricType = metricType,
                showAbsolute = showAbsoluteDifference,
                onToggle = { showAbsoluteDifference = !showAbsoluteDifference }
            )
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun SummaryDeltaChip(
    deltaPercent: Double,
    deltaAmount: Double?,
    metricType: MetricType,
    showAbsolute: Boolean = false,
    onToggle: () -> Unit = {}
) {
    val deltaColor = when {
        deltaPercent == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        metricType == MetricType.SPENT -> if (deltaPercent > 0) ExpenseRed else IncomeSage
        metricType == MetricType.INCOME -> if (deltaPercent > 0) IncomeSage else ExpenseRed
        metricType == MetricType.NET -> if (deltaPercent > 0) IncomeSage else ExpenseRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val chipText = if (showAbsolute && deltaAmount != null) {
        val arrow = if (deltaAmount >= 0) "↑ " else "↓ "
        "$arrow${formatCurrency(abs(deltaAmount))}"
    } else {
        val arrow = if (deltaPercent >= 0) "↑ " else "↓ "
        val formattedPct = if (abs(deltaPercent) > 999.0) ">999%"
        else if (abs(deltaPercent) >= 10.0) "${abs(deltaPercent).toInt()}%"
        else String.format(java.util.Locale.US, "%.1f%%", abs(deltaPercent))
        "$arrow$formattedPct"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = deltaColor.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Text(
            text = chipText,
            color = deltaColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
