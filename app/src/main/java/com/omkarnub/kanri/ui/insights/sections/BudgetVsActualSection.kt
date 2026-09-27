package com.omkarnub.kanri.ui.insights.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.BudgetVsActualData
import com.omkarnub.kanri.ui.insights.CategoryBudgetRow
import com.omkarnub.kanri.ui.insights.UnbudgetedCategoryRow
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val WarningAmber = Color(0xFFF5A524)

@Composable
fun BudgetVsActualSection(
    data: BudgetVsActualData,
    onCategoryClick: (Long?) -> Unit,
    onEditBudgetClick: (Long?) -> Unit,
    onSetOverallBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showUnbudgetedSection by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Budget versus actual: ${formatCurrency(data.overallSpent)} of ${formatCurrency(data.overallBudget)} spent"
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
                    text = "BUDGET VS ACTUAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (data.overallBudget > 0) {
                    IconButton(
                        onClick = { onEditBudgetClick(null) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit overall budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Overall Budget Hero Row
            if (data.overallBudget > 0) {
                OverallBudgetProgressRow(
                    spent = data.overallSpent,
                    budget = data.overallBudget,
                    remaining = data.overallRemaining,
                    isOverBudget = data.isOverallOverBudget
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No monthly budget set",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    TextButton(onClick = onSetOverallBudgetClick) {
                        Text("Set budget", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (data.sumOfCategoriesExceedsOverall) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Category budgets add up to more than your monthly budget.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WarningAmber,
                        fontSize = 11.sp
                    )
                )
            }

            // Category rows
            if (data.categoryRows.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    data.categoryRows.forEach { item ->
                        CategoryBudgetRowView(
                            item = item,
                            onClick = { onCategoryClick(item.categoryId) },
                            onEdit = { onEditBudgetClick(item.categoryId) }
                        )
                    }
                }
            }

            // Unbudgeted spend row
            if (data.unbudgetedSpendTotal > 0) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unbudgeted spend",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = formatCurrency(data.unbudgetedSpendTotal),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Collapsible "No budget set" group
            if (data.unbudgetedCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showUnbudgetedSection = !showUnbudgetedSection }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No budget set (${data.unbudgetedCategories.size} categories)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                    Icon(
                        imageVector = if (showUnbudgetedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (showUnbudgetedSection) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showUnbudgetedSection) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        data.unbudgetedCategories.forEach { unbudgeted ->
                            UnbudgetedCategoryRowView(
                                item = unbudgeted,
                                onSetBudget = { onEditBudgetClick(unbudgeted.categoryId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallBudgetProgressRow(
    spent: Double,
    budget: Double,
    remaining: Double,
    isOverBudget: Boolean
) {
    val pct = if (budget > 0) (spent / budget).toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = pct.coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "overallProgress"
    )

    val barColor = when {
        isOverBudget -> ExpenseRed
        pct >= 0.8f -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overall Monthly Budget",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatCurrency(spent)} / ${formatCurrency(budget)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = barColor.copy(alpha = 0.12f)
            ) {
                val label = if (isOverBudget) "${formatCurrency(abs(remaining))} over" else "${formatCurrency(remaining)} left"
                Text(
                    text = label,
                    color = barColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(6.dp)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun CategoryBudgetRowView(
    item: CategoryBudgetRow,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    val barColor = when {
        item.isOverBudget -> ExpenseRed
        item.percentUsed >= 80f -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurface
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (item.percentUsed / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "catProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                CategoryIcon(
                    categoryName = item.categoryName,
                    iconName = item.categoryIconName,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "${formatCurrency(item.spentAmount)} / ${formatCurrency(item.budgetedAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val statusText = if (item.isOverBudget) {
                    "${formatCurrency(abs(item.remainingAmount))} over"
                } else {
                    "${formatCurrency(item.remainingAmount)} left"
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = barColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${item.percentUsed.toInt()}% · $statusText",
                        color = barColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget for ${item.categoryName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(4.dp)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun UnbudgetedCategoryRowView(
    item: UnbudgetedCategoryRow,
    onSetBudget: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryIcon(
                categoryName = item.categoryName,
                iconName = item.categoryIconName,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = item.categoryName,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = "(${formatCurrency(item.spentAmount)})",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            )
        }

        OutlinedButton(
            onClick = onSetBudget,
            modifier = Modifier.height(28.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("Set", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
