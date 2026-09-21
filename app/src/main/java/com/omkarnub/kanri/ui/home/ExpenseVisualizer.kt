package com.omkarnub.kanri.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.omkarnub.kanri.ui.common.AnimatedNumber
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.KanriDropdownMenu
import com.omkarnub.kanri.ui.common.KanriDropdownMenuItem
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.frostedGlass
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.budget.BudgetCalculator
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.month.CategoryDistributionSheet
import com.omkarnub.kanri.ui.month.CategorySpendItem
import com.omkarnub.kanri.data.analytics.Delta
import com.omkarnub.kanri.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

val SubtleCategoryPalette = listOf(
    Color(0xFF74B886), // Soft Sage Green
    Color(0xFFE2847A), // Soft Terracotta / Coral
    Color(0xFF9E86E0), // Soft Lavender / Periwinkle
    Color(0xFFE5B567), // Soft Muted Amber / Mustard
    Color(0xFF5CB8B2), // Soft Dusty Teal / Mint
    Color(0xFFC47DA0), // Soft Mauve / Dusty Rose
    Color(0xFF6CA8D6), // Soft Slate Blue
    Color(0xFFBCAAA4)  // Soft Khaki / Sand
)

// -----------------------------------------------------------------------------
// Visualizer Types
// -----------------------------------------------------------------------------
enum class DataVisualType(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    RING("Full Ring", "Circular budget & expense progress", Icons.Default.DonutLarge),
    BAR_GRAPH("Bar Graph", "Daily or monthly spending bars", Icons.Default.BarChart),
    PIE_CHART("Category Pie Chart", "Expense distribution by category", Icons.Default.PieChart),
    DIGITAL("Full Digital", "High-contrast digital metrics & stats", Icons.Default.Tune)
}

// -----------------------------------------------------------------------------
// Month Option Model
// -----------------------------------------------------------------------------
data class MonthOption(
    val key: String,
    val displayName: String,
    val startMillis: Long,
    val endMillis: Long,
    val isCurrent: Boolean
)

fun getRecentMonths(): List<MonthOption> {
    val list = mutableListOf<MonthOption>()
    val cal = Calendar.getInstance()
    val now = Calendar.getInstance()
    val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val keyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    for (i in 0 until 6) {
        val temp = cal.clone() as Calendar
        temp.add(Calendar.MONTH, -i)

        val start = (temp.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }

        val isCurrent = (temp.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                temp.get(Calendar.MONTH) == now.get(Calendar.MONTH))

        val name = if (isCurrent) {
            SimpleDateFormat("MMMM", Locale.getDefault()).format(temp.time)
        } else {
            monthNameFormat.format(temp.time)
        }

        list.add(
            MonthOption(
                key = keyFormat.format(temp.time),
                displayName = name,
                startMillis = start.timeInMillis,
                endMillis = end.timeInMillis,
                isCurrent = isCurrent
            )
        )
    }
    return list
}

// -----------------------------------------------------------------------------
// Main Expense Visualizer Card
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseVisualizer(
    monthlyBudget: Double,
    transactions: List<TransactionWithCategory>,
    categories: List<CategoryEntity>,
    onUpdateBudget: (Double) -> Unit,
    modifier: Modifier = Modifier,
    monthDelta: Delta = Delta.NoData,
    onSelectMonth: ((year: Int, month: Int) -> Unit)? = null
) {
    val months = remember { getRecentMonths() }
    var selectedMonth by remember { mutableStateOf(months.first()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMonth) {
        val parts = selectedMonth.key.split("-")
        if (parts.size == 2) {
            val y = parts[0].toIntOrNull() ?: 2026
            val m = parts[1].toIntOrNull() ?: 9
            onSelectMonth?.invoke(y, m)
        }
    }

    var visualType by rememberSaveable { mutableStateOf(DataVisualType.RING) }
    var showVisualTypePicker by remember { mutableStateOf(false) }
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var showDistributionSheet by remember { mutableStateOf(false) }

    // Filter transactions for the selected month
    val monthTransactions = remember(transactions, selectedMonth) {
        transactions.filter {
            it.transaction.timestamp >= selectedMonth.startMillis &&
                    it.transaction.timestamp < selectedMonth.endMillis
        }
    }

    // Calculate total spent for the selected month
    val monthSpent = remember(monthTransactions) {
        monthTransactions
            .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
            .sumOf { it.transaction.amount }
    }

    // Category breakdown sorted descending
    val categorySpendMap = remember(monthTransactions, categories) {
        val debits = monthTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val map = mutableMapOf<String, Double>()
        for (item in debits) {
            val name = item.category?.name ?: "Other"
            map[name] = (map[name] ?: 0.0) + item.transaction.amount
        }
        map.toList().sortedByDescending { it.second }
    }

    val categorySpends = remember(monthTransactions, categories, monthSpent, categorySpendMap) {
        val catMap = categories.associateBy { it.name }
        categorySpendMap.mapIndexed { index, (catName, amount) ->
            val cat = catMap[catName]
            val color = SubtleCategoryPalette[index % SubtleCategoryPalette.size]
            val pct = if (monthSpent > 0) ((amount / monthSpent) * 100).toFloat() else 0f
            CategorySpendItem(
                categoryId = cat?.id,
                name = catName,
                emoji = "",
                color = color,
                totalAmount = amount,
                percentage = pct
            )
        }
    }

    // Dynamic alert color:
    // Normal: Monochrome onSurface (Off-white in Dark mode, Black in Light mode)
    // Near budget (>= 80% and <= 100%): Mild orange (#F59E0B)
    // Exceeds budget (> 100%): Bright red (#EF4444)
    val budgetRatio = if (monthlyBudget > 0) (monthSpent / monthlyBudget).toFloat() else 0f
    val targetAlertColor = when {
        budgetRatio > 1.0f -> Color(0xFFEF4444) // Bright red (exceeded)
        budgetRatio >= 0.8f -> Color(0xFFF59E0B) // Mild orange (near budget)
        else -> MaterialTheme.colorScheme.onSurface // Monochrome high contrast (off-white in dark, black in light)
    }
    val alertColor by animateColorAsState(
        targetValue = targetAlertColor,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "alertColor"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -----------------------------------------------------------------
            // Header Row: Section Title & Month Dropdown Menu
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPENSES OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Month Dropdown Button (merges with background, no purple accent)
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { monthMenuExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedMonth.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select Month",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    KanriDropdownMenu(
                        expanded = monthMenuExpanded,
                        onDismissRequest = { monthMenuExpanded = false }
                    ) {
                        months.forEach { option ->
                            val isSelected = option.key == selectedMonth.key
                            KanriDropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = option.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedMonth = option
                                    monthMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // -----------------------------------------------------------------
            // Full Width Visualizer Area (EXTRA LARGE)
            // -----------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = visualType,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                    },
                    label = "visualizerTransition"
                ) { targetType ->
                    when (targetType) {
                        DataVisualType.RING -> RingVisualizer(
                            monthSpent = monthSpent,
                            monthlyBudget = monthlyBudget,
                            categorySpendMap = categorySpendMap,
                            alertColor = alertColor,
                            budgetRatio = budgetRatio,
                            monthDelta = monthDelta,
                            onCategoryClick = { showDistributionSheet = true }
                        )
                        DataVisualType.BAR_GRAPH -> BarGraphVisualizer(
                            monthSpent = monthSpent,
                            monthlyBudget = monthlyBudget,
                            monthTransactions = monthTransactions,
                            allTransactions = transactions,
                            selectedMonth = selectedMonth,
                            alertColor = alertColor,
                            monthDelta = monthDelta
                        )
                        DataVisualType.PIE_CHART -> PieChartVisualizer(
                            monthSpent = monthSpent,
                            monthlyBudget = monthlyBudget,
                            categorySpendMap = categorySpendMap,
                            alertColor = alertColor,
                            monthDelta = monthDelta,
                            onCategoryClick = { showDistributionSheet = true }
                        )
                        DataVisualType.DIGITAL -> DigitalVisualizer(
                            monthSpent = monthSpent,
                            monthlyBudget = monthlyBudget,
                            alertColor = alertColor,
                            budgetRatio = budgetRatio,
                            monthDelta = monthDelta
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // -----------------------------------------------------------------
            // Borderless Action Buttons: Just SVG Icons with simple text
            // "edit" and "data type"
            // -----------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Edit
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showEditBudgetDialog = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Button 2: Data Type
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showVisualTypePicker = true }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = visualType.icon,
                        contentDescription = "Data Type",
                        tint = alertColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Data Type",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Edit Budget Dialog (Unified with SetBudgetDialog)
    // -------------------------------------------------------------------------
    if (showEditBudgetDialog) {
        SetBudgetDialog(
            currentBudget = monthlyBudget,
            monthName = selectedMonth.displayName,
            onDismiss = { showEditBudgetDialog = false },
            onSaveBudget = { newBudget ->
                onUpdateBudget(newBudget)
                showEditBudgetDialog = false
            }
        )
    }

    // -------------------------------------------------------------------------
    // Visual Type Picker Bottom Sheet
    // -------------------------------------------------------------------------
    if (showVisualTypePicker) {
        VisualTypePickerSheet(
            currentType = visualType,
            onSelectType = {
                visualType = it
                showVisualTypePicker = false
            },
            onDismiss = { showVisualTypePicker = false }
        )
    }

    if (showDistributionSheet) {
        CategoryDistributionSheet(
            categorySpends = categorySpends,
            totalSpent = monthSpent,
            onDismiss = { showDistributionSheet = false }
        )
    }
}

// =============================================================================
// 1. RING PROGRESS BAR VISUALIZER (EXTRA LARGE: 320dp with category percentages)
// =============================================================================
@Composable
private fun RingVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    categorySpendMap: List<Pair<String, Double>>,
    alertColor: Color,
    budgetRatio: Float,
    monthDelta: Delta = Delta.NoData,
    onCategoryClick: () -> Unit = {}
) {
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val animatedSweep by animateFloatAsState(
        targetValue = if (animationTriggered) budgetRatio.coerceAtMost(1.0f) else 0f,
        animationSpec = tween(durationMillis = 950, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "ringSweep"
    )

    val centerAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "ringCenterAlpha"
    )
    val centerScale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0.93f,
        animationSpec = tween(durationMillis = 650, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "ringCenterScale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "ringPillAlpha"
    )

    val totalSpendSafe = if (monthSpent > 0.0) monthSpent else 1.0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(320.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

            Canvas(modifier = Modifier.size(305.dp)) {
                val strokeWidth = 30.dp.toPx()
                val arcSize = size.width - strokeWidth
                val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                // Background Track
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeftOffset,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth)
                )

                // Progress Arc (only drawn when there is active spending)
                if (animatedSweep > 0f && monthSpent > 0.0) {
                    val sweep = animatedSweep * 360f
                    drawArc(
                        color = alertColor,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                }
            ) {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val amountStr = CurrencyUtils.formatCurrency(monthSpent)
                val amountFontSize = when {
                    amountStr.length > 15 -> 21.sp
                    amountStr.length > 11 -> 26.sp
                    else -> 32.sp
                }
                AnimatedNumberText(
                    text = amountStr,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = amountFontSize,
                    animateFromZero = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                MonthDeltaChip(delta = monthDelta)
                Spacer(modifier = Modifier.height(4.dp))
                val budgetStatusText = when {
                    budgetRatio > 1.0f -> "Over Budget (${(budgetRatio * 100).toInt()}%)"
                    budgetRatio >= 0.8f -> "${(budgetRatio * 100).toInt()}% Used"
                    else -> "Budget: ${CurrencyUtils.formatCurrency(monthlyBudget)}"
                }
                Text(
                    text = budgetStatusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (budgetRatio >= 0.8f) alertColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category Percentage Breakdown
        if (categorySpendMap.isNotEmpty()) {
            Box(modifier = Modifier.graphicsLayer { alpha = pillAlpha }) {
                CategoryPreviewPill(
                    categorySpendMap = categorySpendMap,
                    totalSpendSafe = totalSpendSafe,
                    budgetRatio = budgetRatio,
                    alertColor = alertColor,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}



@Composable
private fun CategoryPreviewPill(
    categorySpendMap: List<Pair<String, Double>>,
    totalSpendSafe: Double,
    budgetRatio: Float,
    alertColor: Color,
    onCategoryClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCategoryClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                categorySpendMap.take(3).forEachIndexed { index, pair ->
                    val percentage = if (totalSpendSafe > 0) ((pair.second / totalSpendSafe) * 100).toInt() else 0
                    val pctStr = if (percentage == 0 && pair.second > 0) "<1%" else "$percentage%"
                    val pillColor = if (index == 0 && budgetRatio >= 0.8f) {
                        alertColor
                    } else {
                        val alpha = (1.0f - index * 0.22f).coerceIn(0.45f, 1f)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(pillColor)
                        )
                        CategoryIcon(
                            categoryName = pair.first,
                            tint = pillColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$pctStr ${pair.first}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "View All Categories",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


// =============================================================================
// 3. BAR GRAPH VISUALIZER (Classic normal bar graph with baseline, grid & day/month toggle)
// =============================================================================
enum class BarTimeMode { DAY, MONTH }

@Composable
private fun BarGraphVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    monthTransactions: List<TransactionWithCategory>,
    allTransactions: List<TransactionWithCategory>,
    selectedMonth: MonthOption,
    alertColor: Color,
    monthDelta: Delta = Delta.NoData
) {
    var timeMode by rememberSaveable { mutableStateOf(BarTimeMode.DAY) }

    // Compute Daily data: Last 7 calendar days
    val dailyBars = remember(monthTransactions, selectedMonth) {
        val debits = monthTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        val dayMap = mutableMapOf<String, Pair<String, Double>>()

        val endCal = Calendar.getInstance()
        if (!selectedMonth.isCurrent) {
            endCal.timeInMillis = selectedMonth.endMillis - 1000
        }
        val days = mutableListOf<String>()
        val tempCal = endCal.clone() as Calendar
        for (i in 0 until 7) {
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)
            days.add(0, key)
            dayMap[key] = Pair(
                "${dayOfWeekFormat.format(tempCal.time)}\n${tempCal.get(Calendar.DAY_OF_MONTH)}",
                0.0
            )
            tempCal.add(Calendar.DAY_OF_MONTH, -1)
        }

        for (item in debits) {
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(item.transaction.timestamp))
            if (dayMap.containsKey(key)) {
                val current = dayMap[key]!!
                dayMap[key] = Pair(current.first, current.second + item.transaction.amount)
            }
        }

        days.map { key ->
            val data = dayMap[key]!!
            Pair(data.first, data.second)
        }
    }

    // Compute Monthly data: Past 6 months
    val monthlyBars = remember(allTransactions) {
        val debits = allTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }
        val cal = Calendar.getInstance()
        val monthList = mutableListOf<Pair<String, Double>>()
        val monthKeyFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthDisplayFormat = SimpleDateFormat("MMM", Locale.getDefault())

        for (i in 5 downTo 0) {
            val temp = cal.clone() as Calendar
            temp.add(Calendar.MONTH, -i)
            val key = monthKeyFormat.format(temp.time)
            val displayName = monthDisplayFormat.format(temp.time)

            val total = debits.filter {
                monthKeyFormat.format(Date(it.transaction.timestamp)) == key
            }.sumOf { it.transaction.amount }

            monthList.add(Pair(displayName, total))
        }
        monthList
    }

    val currentBars = if (timeMode == BarTimeMode.DAY) dailyBars else monthlyBars
    val maxSpend = remember(currentBars) {
        (currentBars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    }

    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(timeMode, selectedMonth) {
        animationTriggered = false
        animationTriggered = true
    }

    val animatedHeightFraction by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "barHeightAnim"
    )
    val topRowAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 60, easing = FastOutSlowInEasing),
        label = "barTopRowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Total spent + Day/Month Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (timeMode == BarTimeMode.DAY) "RECENT DAILY SPENDING" else "MONTHLY SPENDING TREND",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedNumber(
                        value = monthSpent,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 32.sp,
                        animateFromZero = true
                    )
                    MonthDeltaChip(delta = monthDelta)
                }
            }

            // Segmented Toggle Pill [ Day | Month ]
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(modifier = Modifier.padding(3.dp)) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (timeMode == BarTimeMode.DAY) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        modifier = Modifier.clickable { timeMode = BarTimeMode.DAY }
                    ) {
                        Text(
                            text = "Day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (timeMode == BarTimeMode.DAY) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (timeMode == BarTimeMode.DAY) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = if (timeMode == BarTimeMode.MONTH) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        modifier = Modifier.clickable { timeMode = BarTimeMode.MONTH }
                    ) {
                        Text(
                            text = "Month",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (timeMode == BarTimeMode.MONTH) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (timeMode == BarTimeMode.MONTH) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Normal Classic Bar Graph (Bars rising from clean baseline with subtle grid lines)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // Bars Area with Background Horizontal Grid Lines
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Subtle horizontal dashed grid lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lines = 3
                    for (i in 1..lines) {
                        val y = size.height * (i.toFloat() / (lines + 1))
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }
                }

                // Rising Vertical Bars
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    currentBars.forEach { pair ->
                        val hasSpend = pair.second > 0.0
                        val fraction = if (hasSpend) {
                            val linearRatio = (pair.second / maxSpend).toFloat()
                            // Guaranteed 12% minimum visible bar height for days with spending
                            (0.12f + 0.88f * linearRatio) * animatedHeightFraction
                        } else {
                            0f
                        }
                        val isMax = pair.second == maxSpend && hasSpend
                        val barColor = if (isMax) alertColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Amount label above bar (only if spending > 0)
                            if (hasSpend) {
                                Text(
                                    text = formatShortCurrency(pair.second),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isMax) alertColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            } else {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // Normal Solid Bar (Width 32dp, clean rounded top)
                            if (hasSpend) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(barColor)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(0.dp))
                            }
                        }
                    }
                }
            }

            // Clean Horizontal Baseline Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Labels Row below baseline
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                currentBars.forEach { pair ->
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

// =============================================================================
// 4. CATEGORY DONUT VISUALIZER (EXTRA LARGE: 305dp with Donut Center Stats)
// =============================================================================
@Composable
private fun PieChartVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    categorySpendMap: List<Pair<String, Double>>,
    alertColor: Color,
    monthDelta: Delta = Delta.NoData,
    onCategoryClick: () -> Unit = {}
) {
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val animatedSweep by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 950, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "pieSweep"
    )

    val centerAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "pieCenterAlpha"
    )
    val centerScale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0.93f,
        animationSpec = tween(durationMillis = 650, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "pieCenterScale"
    )
    val pillAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "piePillAlpha"
    )

    // Subtle, soft, eye-friendly colors for category pie slices
    val categoryColors = remember {
        SubtleCategoryPalette
    }

    val totalSpendSafe = if (monthSpent > 0.0) monthSpent else 1.0
    val activeCategories = remember(categorySpendMap) {
        categorySpendMap.filter { it.second > 0.0 }.take(6)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Category Donut Chart Canvas (305dp) with Center Content
        Box(
            modifier = Modifier.size(305.dp),
            contentAlignment = Alignment.Center
        ) {
            val emptyColor = MaterialTheme.colorScheme.surfaceContainer
            val sliceDividerColor = MaterialTheme.colorScheme.surface

            Canvas(modifier = Modifier.size(295.dp)) {
                val strokeWidth = 38.dp.toPx()
                val arcSize = size.width - strokeWidth
                val topLeftOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

                if (activeCategories.isNotEmpty() && monthSpent > 0.0) {
                    if (activeCategories.size == 1) {
                        // Single category takes whole circle
                        drawArc(
                            color = categoryColors[0],
                            startAngle = -90f,
                            sweepAngle = 360f * animatedSweep,
                            useCenter = false,
                            topLeft = topLeftOffset,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth)
                        )
                    } else {
                        val minAngle = 14f
                        val allocatedMin = activeCategories.size * minAngle
                        val remainingAngle = 360f - allocatedMin
                        var currentAngle = -90f

                        activeCategories.forEachIndexed { index, pair ->
                            val ratio = (pair.second / totalSpendSafe).toFloat()
                            val sweep = (minAngle + ratio * remainingAngle) * animatedSweep

                            // Donut arc slice
                            drawArc(
                                color = categoryColors[index % categoryColors.size],
                                startAngle = currentAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth)
                            )
                            // Clean slice boundary divider
                            drawArc(
                                color = sliceDividerColor,
                                startAngle = currentAngle,
                                sweepAngle = 2f,
                                useCenter = false,
                                topLeft = topLeftOffset,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth)
                            )
                            currentAngle += sweep
                        }
                    }
                } else {
                    drawArc(
                        color = emptyColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeftOffset,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            // Center Content inside Donut hole
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = centerScale
                    scaleY = centerScale
                }
            ) {
                Text(
                    text = "TOTAL SPENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                val amountStr = CurrencyUtils.formatCurrency(monthSpent)
                val amountFontSize = when {
                    amountStr.length > 15 -> 18.sp
                    amountStr.length > 11 -> 22.sp
                    else -> 26.sp
                }
                AnimatedNumberText(
                    text = amountStr,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = amountFontSize,
                    animateFromZero = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                MonthDeltaChip(delta = monthDelta)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multiple Colorful Category Percentage Legend (Top 3)
        if (activeCategories.isNotEmpty() && monthSpent > 0.0) {
            Box(modifier = Modifier.graphicsLayer { alpha = pillAlpha }) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onCategoryClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            activeCategories.take(3).forEach { pair ->
                                val catIndex = activeCategories.indexOf(pair)
                                val percentage = if (totalSpendSafe > 0) ((pair.second / totalSpendSafe) * 100).toInt() else 0
                                val pctStr = if (percentage == 0 && pair.second > 0) "<1%" else "$percentage%"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(categoryColors[catIndex % categoryColors.size])
                                    )
                                    CategoryIcon(
                                        categoryName = pair.first,
                                        tint = categoryColors[catIndex % categoryColors.size],
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$pctStr ${pair.first}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "View All Categories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. DIGITAL EXPENSE / BUDGET VISUALIZER (Days Remaining, No Boxes, Huge Typography)
// =============================================================================
@Composable
private fun DigitalVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    alertColor: Color,
    budgetRatio: Float,
    monthDelta: Delta = Delta.NoData
) {
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationTriggered) budgetRatio.coerceAtMost(1.0f) else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 80, easing = FastOutSlowInEasing),
        label = "digitalProgress"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "digitalContentAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0.95f,
        animationSpec = tween(durationMillis = 600, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "digitalContentScale"
    )

    val remaining = monthlyBudget - monthSpent
    val isExceeded = remaining < 0
    val daysLeft = remember { BudgetCalculator.getDaysRemainingInMonth() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp)
            .graphicsLayer {
                scaleX = contentScale
                scaleY = contentScale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TOTAL MONEY SPENT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        val amountStr = CurrencyUtils.formatCurrency(monthSpent)
        val amountFontSize = when {
            amountStr.length > 15 -> 28.sp
            amountStr.length > 11 -> 36.sp
            else -> 44.sp
        }
        AnimatedNumberText(
            text = amountStr,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = amountFontSize,
            animateFromZero = true
        )

        Spacer(modifier = Modifier.height(6.dp))
        MonthDeltaChip(delta = monthDelta)

        Spacer(modifier = Modifier.height(18.dp))

        // Progress Bar with Budget and Expense Labels
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(budgetRatio * 100).toInt()}% Used",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = alertColor,
                    fontSize = 13.sp
                )
                Text(
                    text = "Budget: ${CurrencyUtils.formatCurrency(monthlyBudget)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sleek Large Progress Track (16dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (monthSpent > 0.0 && animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(alertColor)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Clean Typography Below the Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isExceeded) "MONEY SPENT ABOVE BUDGET" else "BUDGET REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = if (isExceeded) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyUtils.formatCurrency(abs(remaining)),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isExceeded) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DAYS REMAINING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$daysLeft ${if (daysLeft == 1) "day left" else "days left"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// =============================================================================
// Bottom Sheet: Visual Type Picker
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisualTypePickerSheet(
    currentType: DataVisualType,
    onSelectType: (DataVisualType) -> Unit,
    onDismiss: () -> Unit
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Visualization Style",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Choose your preferred data visualizer for expense tracking",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            DataVisualType.entries.forEach { type ->
                val isSelected = type == currentType
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectType(type) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = type.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = type.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



// -----------------------------------------------------------------------------
// Short Currency Formatter
// -----------------------------------------------------------------------------
private fun formatShortCurrency(amount: Double): String {
    return when {
        amount >= 10000000 -> String.format(Locale.getDefault(), "₹%.1fCr", amount / 10000000)
        amount >= 100000 -> String.format(Locale.getDefault(), "₹%.1fL", amount / 100000)
        amount >= 1000 -> String.format(Locale.getDefault(), "₹%.1fk", amount / 1000)
        else -> String.format(Locale.getDefault(), "₹%.0f", amount)
    }
}
