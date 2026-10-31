package com.omkarnub.kanri.ui.month

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassPill
import com.omkarnub.kanri.ui.common.KanriModalBottomSheet
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.frostedGlass
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.home.formatCurrency
import java.util.Locale
import com.omkarnub.kanri.ui.common.kokonut.KokonutSpendingRing
import com.omkarnub.kanri.ui.common.kokonut.SpendingCategory
import com.omkarnub.kanri.util.CurrencyUtils

val CategoryGradientPairs = listOf(
    // 0: Soft Sage Green to Forest Green
    Pair(Color(0xFF86CF99), Color(0xFF489E5F)),
    // 1: Soft Coral to Terracotta
    Pair(Color(0xFFF0958B), Color(0xFFC85A4E)),
    // 2: Soft Lavender to Periwinkle
    Pair(Color(0xFFB29EF0), Color(0xFF7B5EC7)),
    // 3: Soft Amber to Warm Mustard
    Pair(Color(0xFFF2C77D), Color(0xFFC99238)),
    // 4: Soft Mint to Dusty Teal
    Pair(Color(0xFF73CCC6), Color(0xFF3B9992)),
    // 5: Soft Mauve to Dusty Rose
    Pair(Color(0xFFD694B4), Color(0xFFA6587D)),
    // 6: Soft Sky to Slate Blue
    Pair(Color(0xFF84BCED), Color(0xFF4884BA)),
    // 7: Soft Khaki to Sand
    Pair(Color(0xFFCFC0BB), Color(0xFF9E8780))
)

fun getCategoryGradient(index: Int): Pair<Color, Color> {
    return CategoryGradientPairs[index % CategoryGradientPairs.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDonutChart(
    categorySpends: List<CategorySpendItem>,
    totalSpent: Double,
    modifier: Modifier = Modifier,
    onCategoryClick: ((CategorySpendItem) -> Unit)? = null
) {
    var showDistributionSheet by remember { mutableStateOf(false) }

    val kokonutPalette = remember {
        listOf(
            Color(0xFF38BDF8),
            Color(0xFF10B981),
            Color(0xFFF43F5E),
            Color(0xFFFBBF24),
            Color(0xFF8B5CF6),
            Color(0xFFEC4899),
            Color(0xFF14B8A6),
            Color(0xFFF97316),
            Color(0xFF6366F1),
            Color(0xFF71717A)
        )
    }

    val ringCategories = remember(categorySpends) {
        if (categorySpends.isEmpty()) {
            listOf(
                SpendingCategory(
                    name = "No spending",
                    amount = 0.0,
                    change = 0.0,
                    color = Color(0xFF71717A)
                )
            )
        } else {
            categorySpends.mapIndexed { idx, item ->
                SpendingCategory(
                    name = item.name,
                    amount = item.totalAmount,
                    change = 0.0,
                    color = if (item.color != Color.Unspecified && item.color != Color.Transparent) item.color else kokonutPalette[idx % kokonutPalette.size]
                )
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KokonutSpendingRing(
            title = "SPENDING BY CATEGORY",
            categories = ringCategories,
            modifier = Modifier.fillMaxWidth()
        )

        if (categorySpends.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            KanriGlassCard(
                level = GlassLevel.Flat,
                shape = RoundedCornerShape(14.dp),
                onClick = { showDistributionSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Category Distribution (" + categorySpends.size.toString() + " categories)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "See More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    // Distribution Popup Sheet
    if (showDistributionSheet) {
        CategoryDistributionSheet(
            categorySpends = categorySpends,
            totalSpent = totalSpent,
            onDismiss = { showDistributionSheet = false },
            onCategoryClick = { item ->
                showDistributionSheet = false
                onCategoryClick?.invoke(item)
            }
        )
    }
}

@Composable
fun CategoryBreakdownRow(
    item: CategorySpendItem,
    gradient: Pair<Color, Color>? = null,
    showPercentage: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val iconBg = gradient?.first?.copy(alpha = 0.14f) ?: MaterialTheme.colorScheme.surfaceVariant
    val iconTint = gradient?.first ?: MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(vertical = 2.dp)
                }
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIcon(
                        categoryName = item.name,
                        tint = iconTint,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showPercentage) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", item.percentage),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = formatCurrency(item.totalAmount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View Category Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val progressBrush = if (gradient != null) {
            Brush.horizontalGradient(listOf(gradient.first, gradient.second))
        } else {
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((item.percentage / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(progressBrush)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDistributionSheet(
    categorySpends: List<CategorySpendItem>,
    totalSpent: Double,
    onDismiss: () -> Unit,
    onCategoryClick: ((CategorySpendItem) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = LocalKanriGlass.current

    KanriModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Distribution",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${categorySpends.size} categories • Total: ${formatCurrency(totalSpent)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stacked distribution bar visualizer
            if (categorySpends.isNotEmpty()) {
                val trackBg = if (tokens != null) tokens.track else MaterialTheme.colorScheme.surfaceVariant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(trackBg)
                ) {
                    categorySpends.forEachIndexed { index, item ->
                        val weight = (item.percentage / 100f).coerceAtLeast(0.005f)
                        val grad = getCategoryGradient(index)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .height(10.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(grad.first, grad.second)
                                    )
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Full list of categories with proper glass distribution cards
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(categorySpends, key = { _, it -> it.categoryId ?: it.name }) { index, item ->
                    val grad = getCategoryGradient(index)
                    KanriGlassCard(
                        level = GlassLevel.Flat,
                        shape = RoundedCornerShape(16.dp),
                        onClick = if (onCategoryClick != null) { { onCategoryClick(item) } } else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(grad.first.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            categoryName = item.name,
                                            tint = grad.first,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KanriGlassPill(
                                        level = GlassLevel.Flat,
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", item.percentage),
                                            color = grad.first,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = formatCurrency(item.totalAmount),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (onCategoryClick != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Details",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val innerTrackBg = if (tokens != null) tokens.track else MaterialTheme.colorScheme.surfaceVariant
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(innerTrackBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((item.percentage / 100f).coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(grad.first, grad.second)
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
