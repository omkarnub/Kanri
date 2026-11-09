package com.omkarnub.kanri.ui.home

import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import com.omkarnub.kanri.ui.theme.KanriMotion
import com.omkarnub.kanri.ui.theme.kanriPressFeedback
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Tune
import com.omkarnub.kanri.ui.theme.ExpenseRed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriModalBottomSheet
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
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
import com.omkarnub.kanri.data.wallet.WalletBalances
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
    walletBalances: WalletBalances? = null,
    onSelectMonth: ((year: Int, month: Int) -> Unit)? = null
) {
    val months = remember { getRecentMonths() }
    var selectedMonth by remember { mutableStateOf(months.first()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    val tokens = LocalKanriGlass.current
    val actionContrastColor = tokens?.textPrimary?.copy(alpha = 0.85f)
        ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)

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
            .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
            .sumOf { it.transaction.amount }
    }

    // Category breakdown sorted descending
    val categorySpendMap = remember(monthTransactions, categories) {
        val debits = monthTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
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

    KanriGlassCard(
        modifier = modifier.fillMaxWidth(),
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(24.dp)
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
                            .kanriPressFeedback(pressedScale = 0.95f, onClick = { monthMenuExpanded = true })
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
                        (fadeIn(animationSpec = tween(220, easing = KanriMotion.EaseOut)) +
                                scaleIn(initialScale = 0.96f, animationSpec = KanriMotion.SnappySpring))
                            .togetherWith(
                                fadeOut(animationSpec = tween(160, easing = KanriMotion.EaseOut)) +
                                scaleOut(targetScale = 0.98f, animationSpec = tween(160, easing = KanriMotion.EaseOut))
                            )
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
                            walletBalances = walletBalances,
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
                        .kanriPressFeedback(pressedScale = 0.95f, onClick = { showEditBudgetDialog = true })
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = actionContrastColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = actionContrastColor
                    )
                }

                // Button 2: View by
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .kanriPressFeedback(pressedScale = 0.95f, onClick = { showVisualTypePicker = true })
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = visualType.icon,
                        contentDescription = "View by",
                        tint = if (alertColor != MaterialTheme.colorScheme.onSurface) alertColor else actionContrastColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "View by",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = actionContrastColor
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
    walletBalances: WalletBalances? = null,
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
            val tokens = LocalKanriGlass.current
            val trackColor = tokens?.track ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

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
                    val arcColor = if (tokens != null) {
                        if (budgetRatio > 1.0f) tokens.debit else tokens.accent
                    } else {
                        alertColor
                    }
                    drawArc(
                        color = arcColor,
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

                if (walletBalances != null && walletBalances.isConfigured) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cash Balance with Payments SVG
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = "Cash Balance",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(walletBalances.cash),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                ),
                                color = if (walletBalances.cash < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        // Online Balance with AccountBalance SVG
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Online Balance",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(walletBalances.online),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.5.sp
                                ),
                                color = if (walletBalances.online < 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
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
        val debits = monthTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
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
        val debits = allTransactions.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
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
// 5. DIGITAL EXPENSE / BUDGET VISUALIZER (Liquid Fill Blob)
// =============================================================================

/**
 * Custom wave shape outlining the liquid area beneath a sine-wave surface.
 * Clipped by parent's CircleShape to form a circular liquid-fill blob.
 */
private class LiquidWaveShape(
    private val level: Float,
    private val phase: Float,
    private val amplitudePx: Float,
    private val wavelengthFactor: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        if (level <= 0f) {
            return Outline.Generic(path)
        }
        if (level >= 1f) {
            path.addRect(Rect(0f, 0f, size.width, size.height))
            return Outline.Generic(path)
        }
        val width = size.width
        val height = size.height
        val waterY = height * (1f - level)
        val wavelength = width * wavelengthFactor

        path.moveTo(0f, height)
        val startY = (waterY + amplitudePx * sin(phase.toDouble())).toFloat().coerceIn(0f, height)
        path.lineTo(0f, startY)

        val step = 4f
        var x = 0f
        while (x <= width) {
            val y = (waterY + amplitudePx * sin(2.0 * Math.PI * (x / wavelength) + phase)).toFloat().coerceIn(0f, height)
            path.lineTo(x, y)
            x += step
        }
        path.lineTo(width, height)
        path.close()

        return Outline.Generic(path)
    }
}

/**
 * Liquid-fill blob visualizer:
 * - 224dp circle, centered, with subtle outline and base shade.
 * - Inside, darker tinted liquid rising with budget spent (level = spent / budget clamped to 0..1).
 * - Liquid surface: two sine-wave layers with different speeds, amplitudes, and opacities.
 * - When spent changes, animates level with spring (slight settle).
 * - Minimum visible level at low spend (e.g. 2%) so it never looks empty or broken.
 * - Total spent text sits in center, dual-drawn and clipped for crisp legibility across both empty and liquid regions.
 * - State colors: on pace = neutral/green tint, above pace = amber, near/over budget = red.
 * - Over 100%: circle stays full, wave turns red and moves faster, with subtle overflow ripple.
 * - Faint dashed line across circle marking where spending should be by today (budget * day of month / days in month).
 * - Lifecycle-aware: pauses animations in background.
 * - Respects system "reduce motion" setting.
 */
@Composable
private fun LiquidBlobVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    budgetRatio: Float,
    alertColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Pause animation when screen is not visible or app in background
    var isAppForeground by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppForeground = event.targetState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 2. Respect system "reduce motion" setting
    val reduceMotion = remember(context) {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            val transitionScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                1.0f
            )
            durationScale == 0f || transitionScale == 0f
        } catch (_: Exception) {
            false
        }
    }

    // 3. Today's pace calculation
    val calendar = remember { Calendar.getInstance() }
    val dayOfMonth = remember { calendar.get(Calendar.DAY_OF_MONTH) }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val monthProgress = remember(dayOfMonth, daysInMonth) {
        (dayOfMonth.toFloat() / daysInMonth.toFloat()).coerceIn(0f, 1f)
    }

    val isOverBudget = budgetRatio > 1.0f

    // 4. Fill target level with spring animation
    var animationTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animationTriggered = true }

    val targetLevel = when {
        monthlyBudget <= 0.0 || monthSpent <= 0.0 -> 0f
        isOverBudget -> 1.0f
        else -> (0.18f + budgetRatio.coerceIn(0f, 1f) * 0.72f)
    }

    val animatedLevel by animateFloatAsState(
        targetValue = if (animationTriggered) targetLevel else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "blobFillLevel"
    )

    // 5. Phase animations for the two sine wave layers & organic reshape movements
    val infiniteTransition = rememberInfiniteTransition(label = "blobWaveTransition")
    val waveSpeedMultiplier = if (isOverBudget) 0.5f else 1.0f
    val canAnimate = isAppForeground && !reduceMotion && monthSpent > 0.0

    val reshapeScaleX by if (canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0.975f,
            targetValue = 1.025f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "reshapeScaleX"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val reshapeScaleY by if (canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 1.025f,
            targetValue = 0.975f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "reshapeScaleY"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val reshapeRotation by if (canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = -1.8f,
            targetValue = 1.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "reshapeRotation"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val wavePhase1 by if (canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (3200 * waveSpeedMultiplier).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase1"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val wavePhase2 by if (canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = (2200 * waveSpeedMultiplier).toInt(), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "wavePhase2"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // 6. Overflow ripple animation when budget is exceeded (> 100%)
    val rippleRadiusOffset by if (isOverBudget && canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 9.dp.value,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "overflowRippleRadius"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val rippleAlpha by if (isOverBudget && canAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "overflowRippleAlpha"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // 7. State colors: on pace = neutral/green tint, above pace = amber, near or over budget = red
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val (targetFrontColor, targetBackColor, targetCrestColor) = when {
        isOverBudget -> {
            if (isDark) {
                Triple(Color(0xFF5C1C20), Color(0xFF3F1316), Color(0xFFEF4444).copy(alpha = 0.85f))
            } else {
                Triple(Color(0xFFF9B4B7), Color(0xFFF28B90), Color(0xFFE5484D))
            }
        }
        budgetRatio >= 0.85f -> {
            // Near budget: Red tint
            if (isDark) {
                Triple(Color(0xFF45171A), Color(0xFF2E0F11), Color(0xFFEF4444).copy(alpha = 0.65f))
            } else {
                Triple(Color(0xFFFCD0D2), Color(0xFFF8ACB0), Color(0xFFE5484D).copy(alpha = 0.8f))
            }
        }
        budgetRatio > monthProgress -> {
            // Above pace: Amber tint
            if (isDark) {
                Triple(Color(0xFF3E2D13), Color(0xFF2A1E0C), Color(0xFFF59E0B).copy(alpha = 0.65f))
            } else {
                Triple(Color(0xFFFDE8C5), Color(0xFFF8D497), Color(0xFFD97706))
            }
        }
        else -> {
            // On pace: Neutral / Green tint
            if (isDark) {
                Triple(Color(0xFF1E3D2A), Color(0xFF13281B), Color(0xFF34C759).copy(alpha = 0.75f))
            } else {
                Triple(Color(0xFFD6EBD9), Color(0xFFBCE0C1), Color(0xFF2E7D4E))
            }
        }
    }

    val frontLiquidColor by animateColorAsState(targetFrontColor, tween(600), label = "frontLiquidColor")
    val backLiquidColor by animateColorAsState(targetBackColor, tween(600), label = "backLiquidColor")
    val crestColor by animateColorAsState(targetCrestColor, tween(600), label = "crestColor")

    val baseShade = if (isDark) {
        Color(0xFF16161A)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val paceLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)

    val baseTextColor = MaterialTheme.colorScheme.onSurface
    val liquidTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF121212)

    val pctUsed = if (monthlyBudget > 0) (budgetRatio * 100).toInt() else 0
    val accessibilityText = if (monthlyBudget > 0) {
        "${CurrencyUtils.formatCurrency(monthSpent)} spent of ${CurrencyUtils.formatCurrency(monthlyBudget)} budget, $pctUsed percent used"
    } else {
        "${CurrencyUtils.formatCurrency(monthSpent)} spent, no budget set"
    }

    val blobDiameter = 224.dp

    // Spent Amount Typography (same font and style as before)
    val amountStr = CurrencyUtils.formatCurrency(monthSpent)
    val amountFontSize = when {
        amountStr.length > 15 -> 20.sp
        amountStr.length > 12 -> 24.sp
        amountStr.length > 9 -> 28.sp
        else -> 34.sp
    }
    val textShadow = Shadow(
        color = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.45f),
        blurRadius = 6f
    )
    val baseStyle = MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        shadow = textShadow
    )
    val liquidStyle = MaterialTheme.typography.displaySmall.copy(
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
        shadow = textShadow
    )

    Box(
        modifier = modifier
            .size(blobDiameter + 16.dp)
            .aspectRatio(1f)
            .semantics { contentDescription = accessibilityText },
        contentAlignment = Alignment.Center
    ) {
        // Subtle overflow ripple pulse when budget is exceeded
        if (isOverBudget && canAnimate) {
            Canvas(modifier = Modifier.size(blobDiameter + 16.dp).aspectRatio(1f)) {
                val radius = (blobDiameter.toPx() / 2f) + rippleRadiusOffset.dp.toPx()
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = rippleAlpha),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Circular blob container with gentle organic movement
        Box(
            modifier = Modifier
                .size(blobDiameter)
                .aspectRatio(1f)
                .graphicsLayer {
                    if (canAnimate) {
                        scaleX = reshapeScaleX
                        scaleY = reshapeScaleY
                        rotationZ = reshapeRotation
                    }
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            baseShade,
                            if (isDark) Color(0xFF0D0D10) else Color(0xFFE5E3DC)
                        ),
                        radius = with(density) { (blobDiameter / 2f).toPx() }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Specular top highlight / inner orb reflection
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.07f else 0.2f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.22f),
                        radius = size.width * 0.48f
                    )
                )
            }

            // Layer 1: Today's pace line & Back wave layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Faint dashed line marking "where you should be by today"
                if (monthlyBudget > 0.0) {
                    val paceY = height * (1f - monthProgress)
                    val radius = width / 2f
                    val dy = abs(paceY - radius)
                    if (dy < radius) {
                        val dx = sqrt(radius * radius - dy * dy)
                        drawLine(
                            color = paceLineColor,
                            start = Offset(radius - dx, paceY),
                            end = Offset(radius + dx, paceY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }
                }

                // Back wave layer with different speed, amplitude, and opacity
                if (animatedLevel > 0f) {
                    val backPath = Path()
                    val waterY = height * (1f - animatedLevel)
                    val amp2 = 4.dp.toPx()
                    val wl2 = width * 0.85f

                    backPath.moveTo(0f, height)
                    val startY = (waterY + amp2 * sin(wavePhase2.toDouble())).toFloat().coerceIn(0f, height)
                    backPath.lineTo(0f, startY)

                    val step = 4f
                    var x = 0f
                    while (x <= width) {
                        val y = (waterY + amp2 * sin(2.0 * Math.PI * (x / wl2) + wavePhase2)).toFloat().coerceIn(0f, height)
                        backPath.lineTo(x, y)
                        x += step
                    }
                    backPath.lineTo(width, height)
                    backPath.close()

                    drawPath(backPath, color = backLiquidColor)
                }
            }

            // Layer 2: Base copy of spent text (visible in the unfilled region above the wave)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedNumberText(
                    text = amountStr,
                    style = baseStyle,
                    color = baseTextColor,
                    fontSize = amountFontSize,
                    animateFromZero = true
                )
            }

            // Layer 3: Front liquid wave layer + foreground copy of spent text
            if (animatedLevel > 0f) {
                val waveShape = remember(animatedLevel, wavePhase1, density) {
                    LiquidWaveShape(
                        level = animatedLevel,
                        phase = wavePhase1,
                        amplitudePx = with(density) { 5.5.dp.toPx() },
                        wavelengthFactor = 1.15f
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(waveShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    frontLiquidColor.copy(alpha = 0.88f),
                                    frontLiquidColor
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Foreground copy of spent text (visible where liquid is present)
                    AnimatedNumberText(
                        text = amountStr,
                        style = liquidStyle,
                        color = liquidTextColor,
                        fontSize = amountFontSize,
                        animateFromZero = true
                    )
                }

                // Wave surface crest highlight
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val waterY = height * (1f - animatedLevel)
                    val amp1 = 5.5.dp.toPx()
                    val wl1 = width * 1.15f

                    val crestPath = Path()
                    var started = false
                    val step = 3f
                    var x = 0f
                    while (x <= width) {
                        val y = (waterY + amp1 * sin(2.0 * Math.PI * (x / wl1) + wavePhase1)).toFloat().coerceIn(0f, height)
                        if (!started) {
                            crestPath.moveTo(x, y)
                            started = true
                        } else {
                            crestPath.lineTo(x, y)
                        }
                        x += step
                    }
                    drawPath(
                        path = crestPath,
                        color = crestColor,
                        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Layer 4: Light outline stroke
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = outlineColor,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun DigitalVisualizer(
    monthSpent: Double,
    monthlyBudget: Double,
    alertColor: Color,
    budgetRatio: Float,
    monthDelta: Delta = Delta.NoData
) {
    val remaining = monthlyBudget - monthSpent
    val isExceeded = remaining < 0
    val daysLeft = remember { BudgetCalculator.getDaysRemainingInMonth() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row: % Used & Budget amount
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (monthlyBudget > 0) "${(budgetRatio * 100).toInt()}% Used" else "0% Used",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = alertColor,
                fontSize = 13.sp
            )
            Text(
                text = if (monthlyBudget > 0) "Budget: ${CurrencyUtils.formatCurrency(monthlyBudget)}" else "No budget set",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Center visual: Liquid Fill Blob
        LiquidBlobVisualizer(
            monthSpent = monthSpent,
            monthlyBudget = monthlyBudget,
            budgetRatio = budgetRatio,
            alertColor = alertColor
        )

        // Month Delta Chip (if comparison data exists)
        if (monthDelta !is Delta.NoData) {
            Spacer(modifier = Modifier.height(10.dp))
            MonthDeltaChip(delta = monthDelta)
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Bottom row: Budget Remaining / Money Spent Above Budget (left) & Days Remaining (right)
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
    val tokens = LocalKanriGlass.current

    KanriModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Visualization Style",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose your preferred data visualizer for expense tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

            Spacer(modifier = Modifier.height(6.dp))

            DataVisualType.entries.forEach { type ->
                val isSelected = type == currentType
                KanriGlassCard(
                    level = if (isSelected) GlassLevel.Blur else GlassLevel.Flat,
                    shape = RoundedCornerShape(16.dp),
                    onClick = { onSelectType(type) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val iconBg = if (isSelected) {
                            tokens?.accent?.copy(alpha = 0.20f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        } else {
                            tokens?.tintFlat ?: MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val iconTint = if (isSelected) {
                            tokens?.accent ?: MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = type.icon,
                                contentDescription = type.title,
                                tint = iconTint,
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
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tokens?.accent?.copy(alpha = 0.20f) ?: MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = tokens?.accent ?: MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
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
    return CurrencyUtils.formatCompactCurrency(amount)
}
