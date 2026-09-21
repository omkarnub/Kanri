package com.omkarnub.kanri.ui.insights.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.R
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.MonthlyRecapData
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

@Composable
fun MonthlyRecapCard(
    data: MonthlyRecapData,
    isDarkStyle: Boolean,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier
) {
    val panchangFont = FontFamily(Font(R.font.panchang_bold))
    val currentDensity = LocalDensity.current

    val bgColor = if (isDarkStyle) Color(0xFF141414) else Color(0xFFFEFAEC)
    val cardSurface = if (isDarkStyle) Color(0xFF1E1E1E) else Color(0xFFF7F2DE)
    val onSurface = if (isDarkStyle) Color(0xFFEDEDED) else Color(0xFF1C1C1C)
    val onSurfaceVariant = if (isDarkStyle) Color(0xFF8E8E8E) else Color(0xFF706E65)
    val outlineColor = if (isDarkStyle) Color(0xFF2C2C2C) else Color(0xFFE5DECB)

    CompositionLocalProvider(
        LocalDensity provides Density(density = currentDensity.density, fontScale = 1f)
    ) {
        Surface(
            modifier = modifier
                .size(width = 360.dp, height = 450.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, outlineColor, RoundedCornerShape(24.dp)),
            color = bgColor,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: KANRI wordmark and Month/Year
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KANRI",
                        fontFamily = panchangFont,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = onSurface
                    )

                    val monthTitle = data.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase()
                    Text(
                        text = if (data.isSoFar) "$monthTitle (SO FAR)" else monthTitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = onSurfaceVariant
                    )
                }

                // Hero: Total Spent + Delta
                Column {
                    Text(
                        text = "TOTAL SPENT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val spentText = if (hideAmounts) "100% SPENT" else formatCurrency(data.totalSpent)
                        Text(
                            text = spentText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSurface
                        )

                        if (data.deltaVsPreviousPercent != null) {
                            val arrow = if (data.deltaVsPreviousPercent >= 0) "↑" else "↓"
                            val deltaColor = if (data.deltaVsPreviousPercent > 0) ExpenseRed else IncomeSage
                            val pctText = "$arrow ${abs(data.deltaVsPreviousPercent).toInt()}%"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = deltaColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = pctText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = deltaColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Mini silhouette of daily spend bars
                if (data.dailySpends.isNotEmpty()) {
                    val maxDay = (data.dailySpends.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        val barSlot = size.width / data.dailySpends.size
                        val barWidth = barSlot * 0.65f
                        val h = size.height

                        data.dailySpends.forEachIndexed { i, amt ->
                            val bh = ((amt / maxDay).toFloat() * h).coerceAtLeast(2f)
                            val x = i * barSlot + (barSlot - barWidth) / 2f
                            val y = h - bh

                            drawRoundRect(
                                color = onSurface.copy(alpha = 0.45f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, bh),
                                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // Top 3 Categories Row
                if (data.topCategories.isNotEmpty()) {
                    Column {
                        Text(
                            text = "TOP CATEGORIES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            data.topCategories.take(3).forEach { cat ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = cardSurface,
                                    border = BorderStroke(0.5.dp, outlineColor)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CategoryIcon(
                                            categoryName = cat.name,
                                            iconName = cat.iconName,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val amountDisplay = if (hideAmounts) {
                                                "${cat.percent.toInt()}%"
                                            } else {
                                                formatCurrency(cat.amount)
                                            }
                                            Text(
                                                text = amountDisplay,
                                                fontSize = 9.sp,
                                                color = onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2x2 Highlights Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        data.topPayee?.let { payee ->
                            MiniStatItem(
                                label = "TOP PAYEE",
                                value = payee,
                                sub = null,
                                onSurface = onSurface,
                                onSurfaceVariant = onSurfaceVariant
                            )
                        }

                        if (data.noSpendDaysCount > 0) {
                            MiniStatItem(
                                label = "NO-SPEND DAYS",
                                value = "${data.noSpendDaysCount} days",
                                sub = null,
                                onSurface = onSurface,
                                onSurfaceVariant = onSurfaceVariant
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        data.biggestExpensePayee?.let { biggest ->
                            MiniStatItem(
                                label = "LARGEST EXPENSE",
                                value = biggest,
                                sub = if (hideAmounts || data.biggestExpenseAmount == null) null else formatCurrency(data.biggestExpenseAmount),
                                onSurface = onSurface,
                                onSurfaceVariant = onSurfaceVariant
                            )
                        }

                        if (data.savingsRatePercent != null) {
                            val savingsText = "${data.savingsRatePercent.toInt()}%"
                            MiniStatItem(
                                label = "SAVINGS RATE",
                                value = savingsText,
                                sub = if (hideAmounts) null else "Net ${formatCurrency(data.netAmount)}",
                                onSurface = IncomeSage,
                                onSurfaceVariant = onSurfaceVariant
                            )
                        }
                    }
                }

                // Footer: Budget Result
                if (data.budgetResultLabel != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = cardSurface,
                        border = BorderStroke(0.5.dp, outlineColor)
                    ) {
                        Text(
                            text = data.budgetResultLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStatItem(
    label: String,
    value: String,
    sub: String?,
    onSurface: Color,
    onSurfaceVariant: Color
) {
    Column {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sub != null) {
            Text(
                text = sub,
                fontSize = 9.sp,
                color = onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
