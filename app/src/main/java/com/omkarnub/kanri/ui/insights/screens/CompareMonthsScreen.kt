package com.omkarnub.kanri.ui.insights.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.CompareCategoryItem
import com.omkarnub.kanri.ui.insights.CompareMonthsData
import com.omkarnub.kanri.ui.insights.DaySpendPoint
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.InsightsViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareMonthsScreen(
    viewModel: InsightsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var monthA by remember { mutableStateOf(YearMonth.now()) }
    var monthB by remember { mutableStateOf(YearMonth.now().minusMonths(1)) }
    val globalMode by viewModel.mode.collectAsState()
    var compareMode by remember { mutableStateOf(globalMode) }
    var sameDaysOnly by remember { mutableStateOf(true) }

    val compareDataState: CompareMonthsData? by viewModel.getCompareMonthsFlow(monthA, monthB, compareMode, sameDaysOnly)
        .collectAsState(initial = null)
    val availableMonths: List<YearMonth> by viewModel.availableMonthsFlow.collectAsState(initial = emptyList<YearMonth>())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compare Months",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Pickers & Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MonthDropdownSelector(
                                selectedMonth = monthA,
                                label = "Month A",
                                availableMonths = availableMonths,
                                onSelect = { monthA = it },
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(
                                onClick = {
                                    val tmp = monthA
                                    monthA = monthB
                                    monthB = tmp
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Swap months",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            MonthDropdownSelector(
                                selectedMonth = monthB,
                                label = "Month B",
                                availableMonths = availableMonths,
                                onSelect = { monthB = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (compareMode == InsightsMode.EXPENSE) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { compareMode = InsightsMode.EXPENSE }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Spent",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (compareMode == InsightsMode.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (compareMode == InsightsMode.INCOME) IncomeSage.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { compareMode = InsightsMode.INCOME }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Received",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (compareMode == InsightsMode.INCOME) IncomeSage else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Same days only",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                                Switch(
                                    checked = sameDaysOnly,
                                    onCheckedChange = { sameDaysOnly = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (monthA == monthB) {
                item {
                    Text(
                        text = "Please select two different months to compare.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                return@LazyColumn
            }

            val data = compareDataState
            if (data == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        com.omkarnub.kanri.ui.common.KanriWobbleLoader(color = MaterialTheme.colorScheme.primary)
                    }
                }
                return@LazyColumn
            }

            // Summary sentence
            if (data.summarySentence.isNotEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = data.summarySentence,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            // 1. Headline Table Card
            item {
                val mA = data.metricsA
                val mB = data.metricsB
                val dtf = DateTimeFormatter.ofPattern("MMM yyyy")

                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "METRICS COMPARISON",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("METRIC", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1.2f))
                            Text(monthA.format(dtf), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            Text(monthB.format(dtf), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                            Text("Δ", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricCompareRow("Spent", formatCurrency(mA.spent), formatCurrency(mB.spent), mA.spent, mB.spent, isExpense = true)
                            MetricCompareRow("Income", formatCurrency(mA.income), formatCurrency(mB.income), mA.income, mB.income, isExpense = false)
                            MetricCompareRow("Net", formatCurrency(mA.net), formatCurrency(mB.net), mA.net, mB.net, isExpense = false)
                            MetricCompareRow("Tx Count", mA.transactionCount.toString(), mB.transactionCount.toString(), mA.transactionCount.toDouble(), mB.transactionCount.toDouble(), isExpense = true)
                            MetricCompareRow("Avg / Day", formatCurrency(mA.averagePerDay), formatCurrency(mB.averagePerDay), mA.averagePerDay, mB.averagePerDay, isExpense = true)
                            MetricCompareRow("Largest", formatCurrency(mA.largestExpense), formatCurrency(mB.largestExpense), mA.largestExpense, mB.largestExpense, isExpense = true)
                        }
                    }
                }
            }

            // 2. Cumulative Overlay Chart
            if (data.dailyCumulativeA.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CUMULATIVE OVERLAY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                val dtf = DateTimeFormatter.ofPattern("MMM")
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "${monthA.format(dtf)} (solid)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${monthB.format(dtf)} (dashed)",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CumulativeCompareChart(
                                pointsA = data.dailyCumulativeA,
                                pointsB = data.dailyCumulativeB,
                                totalDays = data.clampedDayCount
                            )
                        }
                    }
                }
            }

            // 3. Category Comparison Bars
            if (data.categoryItems.isNotEmpty()) {
                item {
                    CategoryComparisonSection(categories = data.categoryItems)
                }
            }
        }
    }
}

@Composable
private fun MonthDropdownSelector(
    selectedMonth: YearMonth,
    label: String,
    availableMonths: List<YearMonth>,
    onSelect: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dtf = DateTimeFormatter.ofPattern("MMM yyyy")

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { expanded = true }
        ) {
            Text(
                text = selectedMonth.format(dtf),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableMonths.forEach { ym ->
                    DropdownMenuItem(
                        text = { Text(ym.format(dtf)) },
                        onClick = {
                            onSelect(ym)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCompareRow(
    label: String,
    formattedA: String,
    formattedB: String,
    valA: Double,
    valB: Double,
    isExpense: Boolean
) {
    val delta = valA - valB
    val pct = if (valB != 0.0) (delta / abs(valB)) * 100.0 else null

    val deltaColor = when {
        delta == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        isExpense -> if (delta > 0) ExpenseRed else IncomeSage
        else -> if (delta > 0) IncomeSage else ExpenseRed
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium), modifier = Modifier.weight(1.2f))
        Text(text = formattedA, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        Text(text = formattedB, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)

        val pctStr = if (pct != null) {
            val arrow = if (pct >= 0) "↑" else "↓"
            "$arrow${abs(pct).toInt()}%"
        } else "—"

        Text(
            text = pctStr,
            style = MaterialTheme.typography.bodySmall.copy(color = deltaColor, fontWeight = FontWeight.Bold, fontSize = 11.sp),
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun CumulativeCompareChart(
    pointsA: List<DaySpendPoint>,
    pointsB: List<DaySpendPoint>,
    totalDays: Int
) {
    var scrubbedDay by remember { mutableStateOf<Int?>(null) }
    val maxAmount = maxOf(pointsA.maxOfOrNull { it.cumulative } ?: 0.0, pointsB.maxOfOrNull { it.cumulative } ?: 0.0, 100.0) * 1.1
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(pointsA, pointsB) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val day = ((offset.x / size.width) * totalDays).toInt() + 1
                            scrubbedDay = day.coerceIn(1, totalDays)
                        },
                        onDrag = { change, _ ->
                            val day = ((change.position.x / size.width) * totalDays).toInt() + 1
                            scrubbedDay = day.coerceIn(1, totalDays)
                        },
                        onDragEnd = { scrubbedDay = null },
                        onDragCancel = { scrubbedDay = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val daysCount = totalDays.coerceAtLeast(1)

            fun xForDay(day: Int): Float = ((day - 1).toFloat() / (daysCount - 1).coerceAtLeast(1)) * w
            fun yForAmount(amt: Double): Float = h - ((amt / maxAmount).toFloat() * h).coerceIn(0f, h)

            // Month B (dashed)
            if (pointsB.isNotEmpty()) {
                val pathB = Path()
                pointsB.forEachIndexed { idx, pt ->
                    val x = xForDay(pt.dayOfMonth)
                    val y = yForAmount(pt.cumulative)
                    if (idx == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                }
                drawPath(
                    path = pathB,
                    color = onSurfaceVariant.copy(alpha = 0.5f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                )
            }

            // Month A (solid)
            if (pointsA.isNotEmpty()) {
                val pathA = Path()
                pointsA.forEachIndexed { idx, pt ->
                    val x = xForDay(pt.dayOfMonth)
                    val y = yForAmount(pt.cumulative)
                    if (idx == 0) pathA.moveTo(x, y) else pathA.lineTo(x, y)
                }
                drawPath(
                    path = pathA,
                    color = onSurface,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            scrubbedDay?.let { day ->
                val sx = xForDay(day)
                drawLine(
                    color = onSurface.copy(alpha = 0.6f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }

    scrubbedDay?.let { day ->
        val ptA = pointsA.find { it.dayOfMonth == day }?.cumulative
        val ptB = pointsB.find { it.dayOfMonth == day }?.cumulative
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Day $day", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            Text(
                text = "A: ${formatCurrency(ptA ?: 0.0)}  ·  B: ${formatCurrency(ptB ?: 0.0)}",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun CategoryComparisonSection(categories: List<CompareCategoryItem>) {
    var showAll by remember { mutableStateOf(false) }
    val displayList = if (showAll) categories else categories.take(8)
    val maxCategorySpend = (categories.maxOfOrNull { maxOf(it.amountA, it.amountB) } ?: 1.0).coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATEGORY DELTAS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                if (categories.size > 8) {
                    TextButton(
                        onClick = { showAll = !showAll },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (showAll) "Show top 8" else "Show all (${categories.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                displayList.forEach { item ->
                    CategoryDeltaRow(item = item, maxSpend = maxCategorySpend)
                }
            }
        }
    }
}

@Composable
private fun CategoryDeltaRow(
    item: CompareCategoryItem,
    maxSpend: Double
) {
    val ratioA = (item.amountA / maxSpend).toFloat().coerceIn(0.02f, 1f)
    val ratioB = (item.amountB / maxSpend).toFloat().coerceIn(0.02f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1.2f)
        ) {
            CategoryIcon(
                categoryName = item.categoryName,
                iconName = item.categoryIconName,
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width((80 * ratioA).dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface)
                    )
                    Box(
                        modifier = Modifier
                            .width((80 * ratioB).dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${formatCurrency(item.amountA)} vs ${formatCurrency(item.amountB)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )

            val deltaColor = if (item.deltaAmount > 0) ExpenseRed else IncomeSage
            val arrow = if (item.deltaAmount >= 0) "+ " else "- "
            Text(
                text = "$arrow${formatCurrency(abs(item.deltaAmount))}",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = deltaColor,
                    fontSize = 12.sp
                )
            )
        }
    }
}
