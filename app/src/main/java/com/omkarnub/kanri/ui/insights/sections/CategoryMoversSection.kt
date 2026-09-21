package com.omkarnub.kanri.ui.insights.sections

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.omkarnub.kanri.ui.insights.CategoryMoverItem
import com.omkarnub.kanri.ui.insights.CategoryMoversData
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

@Composable
fun CategoryMoversSection(
    data: CategoryMoversData,
    onCategoryClick: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Category movers ${data.periodLabel}: ${data.upMovers.size} increases, ${data.downMovers.size} decreases"
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
                    text = "CATEGORY MOVERS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = data.periodComparisonLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Increases (Up)
            if (data.upMovers.isNotEmpty()) {
                Text(
                    text = "HIGHER SPEND",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ExpenseRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.upMovers.forEach { mover ->
                        CategoryMoverRow(
                            mover = mover,
                            isUp = true,
                            onClick = { onCategoryClick(mover.categoryId) }
                        )
                    }
                }
            }

            // Decreases (Down)
            if (data.downMovers.isNotEmpty()) {
                if (data.upMovers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Text(
                    text = "LOWER SPEND",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = IncomeSage,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    data.downMovers.forEach { mover ->
                        CategoryMoverRow(
                            mover = mover,
                            isUp = false,
                            onClick = { onCategoryClick(mover.categoryId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryMoverRow(
    mover: CategoryMoverItem,
    isUp: Boolean,
    onClick: () -> Unit
) {
    val accentColor = if (isUp) ExpenseRed else IncomeSage
    val maxVal = maxOf(mover.currentSpend, mover.previousSpend, 1.0)
    val prevRatio = (mover.previousSpend / maxVal).toFloat().coerceIn(0.05f, 1f)
    val currRatio = (mover.currentSpend / maxVal).toFloat().coerceIn(0.05f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            CategoryIcon(
                categoryName = mover.categoryName,
                iconName = mover.categoryIconName,
                modifier = Modifier.size(30.dp)
            )

            Column {
                Text(
                    text = mover.categoryName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Tiny 2-bar comparison
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    // Previous bar
                    Box(
                        modifier = Modifier
                            .width((28 * prevRatio).dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                    // Current bar
                    Box(
                        modifier = Modifier
                            .width((28 * currRatio).dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
            }
        }

        // Delta amount & percent chip
        Column(horizontalAlignment = Alignment.End) {
            val arrow = if (isUp) "+ " else "- "
            Text(
                text = "$arrow${formatCurrency(abs(mover.deltaAmount))}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 13.sp
                )
            )

            val pctLabel = when {
                mover.isNew -> "New"
                mover.deltaPercent != null -> {
                    val p = abs(mover.deltaPercent)
                    if (p > 999.0) ">999%"
                    else if (p >= 10.0) "${p.toInt()}%"
                    else String.format(java.util.Locale.US, "%.1f%%", p)
                }
                else -> ""
            }

            if (pctLabel.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = pctLabel,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}
