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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.frostedGlass
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.home.formatCurrency
import java.util.Locale

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
    var animationTriggered by remember { mutableStateOf(false) }
    var showDistributionSheet by remember { mutableStateOf(false) }

    LaunchedEffect(categorySpends) {
        animationTriggered = true
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "donutProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val top3Spends = remember(categorySpends) { categorySpends.take(3) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SPENDING BY CATEGORY",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (categorySpends.isEmpty() || totalSpent <= 0.0) {
                // Empty state donut
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        val arcRadius = (size.minDimension - strokeWidth) / 2f
                        drawCircle(
                            color = trackColor.copy(alpha = 0.35f),
                            radius = arcRadius,
                            center = Offset(size.width / 2f, size.height / 2f),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "₹0.00",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "No spends",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Animated Donut Canvas
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        val arcPadding = strokeWidth / 2f
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val arcTopLeft = Offset(arcPadding, arcPadding)
                        val arcRadius = (size.minDimension - strokeWidth) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Subtle track ring background
                        drawCircle(
                            color = trackColor.copy(alpha = 0.35f),
                            radius = arcRadius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        var startAngle = -90f
                        val gapAngle = if (categorySpends.size > 1) 3.5f else 0f

                        for ((index, item) in categorySpends.withIndex()) {
                            val rawSweep = (item.percentage / 100f) * 360f
                            val effectiveGap = if (rawSweep > 4f) gapAngle else (rawSweep * 0.3f)
                            val sweepAngle = (rawSweep - effectiveGap).coerceAtLeast(0.5f) * animatedProgress
                            val gradientPair = getCategoryGradient(index)

                            val startRad = Math.toRadians(startAngle.toDouble())
                            val endRad = Math.toRadians((startAngle + sweepAngle).toDouble())

                            val startPoint = Offset(
                                x = (center.x + arcRadius * kotlin.math.cos(startRad)).toFloat(),
                                y = (center.y + arcRadius * kotlin.math.sin(startRad)).toFloat()
                            )
                            val endPoint = Offset(
                                x = (center.x + arcRadius * kotlin.math.cos(endRad)).toFloat(),
                                y = (center.y + arcRadius * kotlin.math.sin(endRad)).toFloat()
                            )

                            val brush = if (categorySpends.size == 1 && sweepAngle >= 359f) {
                                Brush.sweepGradient(
                                    colors = listOf(gradientPair.first, gradientPair.second, gradientPair.first),
                                    center = center
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(gradientPair.first, gradientPair.second),
                                    start = startPoint,
                                    end = endPoint
                                )
                            }

                            drawArc(
                                brush = brush,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += rawSweep
                        }
                    }

                    // Inside hole content
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL SPENT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = formatCurrency(totalSpent),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Top 3 Category Breakdown List
            if (top3Spends.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    top3Spends.forEachIndexed { index, item ->
                        CategoryBreakdownRow(
                            item = item,
                            gradient = getCategoryGradient(index),
                            showPercentage = false,
                            onClick = if (onCategoryClick != null) { { onCategoryClick(item) } } else null
                        )
                    }
                }

                // See More button to open full distribution popup
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    onClick = { showDistributionSheet = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 14.dp),
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
                                text = if (categorySpends.size > 3) "See More (${categorySpends.size} categories)" else "See Distribution",
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stacked distribution bar visualizer
            if (categorySpends.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
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

            // Full list of categories with proper distribution (percentages, amounts, progress bars)
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(categorySpends, key = { _, it -> it.categoryId ?: it.name }) { index, item ->
                    val grad = getCategoryGradient(index)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (onCategoryClick != null) {
                                    Modifier.clickable { onCategoryClick(item) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
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
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(grad.first.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            categoryName = item.name,
                                            tint = grad.first,
                                            modifier = Modifier.size(16.dp)
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
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = grad.first.copy(alpha = 0.14f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "%.1f%%", item.percentage),
                                            color = grad.first,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                            Spacer(modifier = Modifier.height(8.dp))

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
