package com.omkarnub.kanri.ui.month

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.data.export.ExportRange
import com.omkarnub.kanri.ui.category.CategoryDetailScreen
import com.omkarnub.kanri.ui.export.ExportStatementSheet
import com.omkarnub.kanri.ui.home.CategoryPickerSheet
import com.omkarnub.kanri.ui.home.SetBudgetDialog
import com.omkarnub.kanri.ui.insights.InsightsMode
import com.omkarnub.kanri.ui.insights.InsightsPeriods
import com.omkarnub.kanri.ui.insights.InsightsRange
import com.omkarnub.kanri.ui.insights.InsightsViewModel
import com.omkarnub.kanri.ui.insights.SectionState
import com.omkarnub.kanri.ui.insights.screens.CompareMonthsScreen
import com.omkarnub.kanri.ui.insights.screens.MonthlyRecapScreen
import com.omkarnub.kanri.ui.insights.screens.PayeeDetailScreen
import com.omkarnub.kanri.ui.insights.sections.BiggestTransactionsSection
import com.omkarnub.kanri.ui.insights.sections.BudgetVsActualSection
import com.omkarnub.kanri.ui.insights.sections.CalendarHeatmapSection
import com.omkarnub.kanri.ui.insights.sections.CategoryMoversSection
import com.omkarnub.kanri.ui.insights.sections.CumulativeSpendLineSection
import com.omkarnub.kanri.ui.insights.sections.IncomeSourcesSection
import com.omkarnub.kanri.ui.insights.sections.InsightsRangeBar
import com.omkarnub.kanri.ui.insights.sections.InsightsToolsRow
import com.omkarnub.kanri.ui.insights.sections.MonthEndProjectionSection
import com.omkarnub.kanri.ui.insights.sections.MonthSummaryStripSection
import com.omkarnub.kanri.ui.insights.sections.PaymentMethodSplitSection
import com.omkarnub.kanri.ui.insights.sections.PersonalRecordsSection
import com.omkarnub.kanri.ui.insights.sections.SmallSpendsSection
import com.omkarnub.kanri.ui.insights.sections.SplitExpensesSummarySection
import com.omkarnub.kanri.ui.insights.sections.SuggestedBudgetSection
import com.omkarnub.kanri.ui.insights.sections.TopPayeesSection
import com.omkarnub.kanri.ui.insights.sections.WeekendVsWeekdaySection
import com.omkarnub.kanri.ui.insights.sections.YearHeatmapSection
import com.omkarnub.kanri.ui.search.AmountRangePreset
import com.omkarnub.kanri.ui.search.DateRangePreset
import com.omkarnub.kanri.ui.search.SearchFilterState
import com.omkarnub.kanri.ui.search.SearchScreen
import com.omkarnub.kanri.ui.search.TransactionTypeFilter
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthViewScreen(
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    monthViewModel: MonthViewModel = viewModel(),
    insightsViewModel: InsightsViewModel = viewModel()
) {
    val monthState by monthViewModel.uiState.collectAsState()
    val allTimeState by monthViewModel.allTimeTrendsUiState.collectAsState()
    val haptics = rememberKanriHaptics()

    val currentRange by insightsViewModel.range.collectAsState()
    val currentMode by insightsViewModel.mode.collectAsState()

    // Section state flows from InsightsViewModel
    val summaryStripState by insightsViewModel.summaryStripFlow.collectAsState()
    val projectionState by insightsViewModel.projectionFlow.collectAsState()
    val cumulativeSpendState by insightsViewModel.cumulativeSpendFlow.collectAsState()
    val budgetVsActualState by insightsViewModel.budgetVsActualFlow.collectAsState()
    val calendarHeatmapState by insightsViewModel.calendarHeatmapFlow.collectAsState()
    val categoryMoversState by insightsViewModel.categoryMoversFlow.collectAsState()
    val topPayeesState by insightsViewModel.topPayeesFlow.collectAsState()
    val biggestTransactionsState by insightsViewModel.biggestTransactionsFlow.collectAsState()
    val paymentMethodState by insightsViewModel.paymentMethodFlow.collectAsState()
    val weekendVsWeekdayState by insightsViewModel.weekendVsWeekdayFlow.collectAsState()
    val smallSpendsState by insightsViewModel.smallSpendsFlow.collectAsState()
    val incomeSourcesState by insightsViewModel.incomeSourcesFlow.collectAsState()
    val splitExpensesState by insightsViewModel.splitExpensesFlow.collectAsState()
    val suggestedBudgetState by insightsViewModel.suggestedBudgetFlow.collectAsState()
    val personalRecordsState by insightsViewModel.personalRecordsFlow.collectAsState()
    val yearHeatmapState by insightsViewModel.yearHeatmapFlow.collectAsState()

    val allCategories: List<CategoryEntity> by insightsViewModel.allCategoriesFlow.collectAsState(initial = emptyList<CategoryEntity>())

    // UI state destinations
    var showAllTimeTrends by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showSearchScreen by remember { mutableStateOf(false) }
    var searchFilterToApply by remember { mutableStateOf<SearchFilterState?>(null) }
    var selectedCategoryDetail by remember { mutableStateOf<CategorySpendItem?>(null) }
    var selectedPayeeDetail by remember { mutableStateOf<String?>(null) }
    var showCompareMonths by remember { mutableStateOf(false) }
    var showMonthlyRecap by remember { mutableStateOf(false) }

    val isSubScreenOpen = showAllTimeTrends || showSearchScreen || selectedCategoryDetail != null ||
            selectedPayeeDetail != null || showCompareMonths || showMonthlyRecap

    BackHandler(enabled = isSubScreenOpen) {
        when {
            showCompareMonths -> showCompareMonths = false
            showMonthlyRecap -> showMonthlyRecap = false
            selectedPayeeDetail != null -> selectedPayeeDetail = null
            selectedCategoryDetail != null -> selectedCategoryDetail = null
            showSearchScreen -> {
                showSearchScreen = false
                searchFilterToApply = null
            }
            showAllTimeTrends -> showAllTimeTrends = false
        }
    }

    // Dialog state
    var setBudgetCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var selectedTxForCategoryPicker by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var lendingEntryForTransaction by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var showFinancialHealthDetailsSheet by remember { mutableStateOf(false) }

    val daysRemaining = if (monthState.isCurrentMonth) {
        (monthState.daysInMonth - monthState.currentDay + 1).coerceAtLeast(1)
    } else 1
    val currentBudget = when (val bState = budgetVsActualState) {
        is SectionState.Data -> bState.data.overallBudget
        else -> com.omkarnub.kanri.data.budget.BudgetCalculator.DEFAULT_MONTHLY_BUDGET
    }
    val financialHealth = remember(
        monthState.totalSpent,
        monthState.totalReceived,
        currentBudget,
        daysRemaining,
        monthState.selectedYear,
        monthState.selectedMonth,
        monthState.currentDay
    ) {
        val cal = Calendar.getInstance().apply {
            if (monthState.selectedYear > 0) set(Calendar.YEAR, monthState.selectedYear)
            if (monthState.selectedMonth > 0) set(Calendar.MONTH, monthState.selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, monthState.currentDay.coerceAtLeast(1))
        }
        com.omkarnub.kanri.data.analytics.FinancialHealthCalculator.calculate(
            monthSpent = monthState.totalSpent,
            monthIncome = monthState.totalReceived,
            monthlyBudget = currentBudget,
            daysRemaining = daysRemaining,
            calendar = cal
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Synchronize month changes between MonthViewModel and InsightsViewModel
    val activeSelectedMonth = when (val r = currentRange) {
        is InsightsRange.Month -> r.yearMonth
        else -> YearMonth.of(monthState.selectedYear, monthState.selectedMonth)
    }

    // Single-entry stagger animation gate for the visit
    var hasPlayedEntranceAnim by remember { mutableStateOf(false) }
    val entranceAlpha = remember { Animatable(0f) }
    val entranceTranslationY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        if (!hasPlayedEntranceAnim) {
            launch {
                entranceAlpha.animateTo(1f, tween(440, easing = FastOutSlowInEasing))
            }
            launch {
                entranceTranslationY.animateTo(0f, tween(440, easing = FastOutSlowInEasing))
            }
            hasPlayedEntranceAnim = true
        } else {
            entranceAlpha.snapTo(1f)
            entranceTranslationY.snapTo(0f)
        }
    }

    // Destination: Full screen Search
    if (showSearchScreen) {
        SearchScreen(
            onBack = {
                showSearchScreen = false
                searchFilterToApply = null
            },
            initialFilterState = searchFilterToApply,
            modifier = modifier
        )
        return
    }

    // Destination: Payee Detail Screen
    if (selectedPayeeDetail != null) {
        PayeeDetailScreen(
            counterparty = selectedPayeeDetail!!,
            viewModel = insightsViewModel,
            onBack = { selectedPayeeDetail = null },
            modifier = modifier
        )
        return
    }

    // Destination: Compare Months Screen
    if (showCompareMonths) {
        CompareMonthsScreen(
            viewModel = insightsViewModel,
            onBack = { showCompareMonths = false },
            modifier = modifier
        )
        return
    }

    // Destination: Monthly Recap Screen
    if (showMonthlyRecap) {
        MonthlyRecapScreen(
            targetMonth = activeSelectedMonth,
            viewModel = insightsViewModel,
            onBack = { showMonthlyRecap = false },
            modifier = modifier
        )
        return
    }

    // Destination: Category Detail Screen
    if (selectedCategoryDetail != null) {
        CategoryDetailScreen(
            categoryItem = selectedCategoryDetail!!,
            selectedYear = monthState.selectedYear,
            selectedMonth = monthState.selectedMonth,
            onBack = { selectedCategoryDetail = null },
            modifier = modifier
        )
        return
    }

    // Destination: All-Time Trends
    if (showAllTimeTrends) {
        AllTimeTrendsView(
            state = allTimeState,
            onMetricSelect = { monthViewModel.selectTrendMetric(it) },
            onScrubbedIndexChange = { monthViewModel.setScrubbedMonth(it) },
            onSelectMonth = { year, month ->
                monthViewModel.selectMonth(year, month)
                insightsViewModel.setRange(InsightsRange.Month(YearMonth.of(year, month)))
                showAllTimeTrends = false
            }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "INSIGHTS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(onClick = {
                        haptics.click()
                        searchFilterToApply = null
                        showSearchScreen = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Transactions",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        haptics.click()
                        showExportSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Financial Statement",
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
                .padding(innerPadding)
                .alpha(entranceAlpha.value)
                .offset { IntOffset(0, entranceTranslationY.value.dp.roundToPx()) },
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 115.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Month Selector (visible for Month range, replaced by range label for others)
            item {
                if (currentRange is InsightsRange.Month) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                haptics.tick()
                                monthViewModel.previousMonth()
                                val prevYm = activeSelectedMonth.minusMonths(1)
                                insightsViewModel.setRange(InsightsRange.Month(prevYm))
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            val dtf = DateTimeFormatter.ofPattern("MMMM yyyy")
                            Text(
                                text = activeSelectedMonth.format(dtf),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            IconButton(onClick = {
                                haptics.tick()
                                monthViewModel.nextMonth()
                                val nextYm = activeSelectedMonth.plusMonths(1)
                                insightsViewModel.setRange(InsightsRange.Month(nextYm))
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        val dtf = DateTimeFormatter.ofPattern("d MMM yyyy")
                        val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                        val label = "${window.startDate.format(dtf)} – ${window.endDate.format(dtf)}"
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // 5.1 Sticky Range Switcher + Expense/Income Toggle
            item {
                InsightsRangeBar(
                    currentRange = currentRange,
                    currentMode = currentMode,
                    onRangeSelected = { newRange ->
                        insightsViewModel.setRange(newRange)
                        if (newRange is InsightsRange.Month) {
                            monthViewModel.selectMonth(newRange.yearMonth.year, newRange.yearMonth.monthValue)
                        }
                    },
                    onModeSelected = { newMode ->
                        insightsViewModel.setMode(newMode)
                    }
                )
            }

            // Global Empty Check: If Summary Strip is Empty, show a single minimal empty state
            if (summaryStripState is SectionState.Empty) {
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
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "No transactions in this period",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                return@LazyColumn
            }

            // ═══════════════════════════════════════════════════════════
            // FINANCIAL HEALTH (CHECK FINANCIAL HEALTH CARD)
            // ═══════════════════════════════════════════════════════════
            item {
                com.omkarnub.kanri.ui.insights.sections.CheckFinancialHealthCard(
                    onClick = { showFinancialHealthDetailsSheet = true }
                )
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 1: SUMMARY STRIP (All ranges, Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            item {
                when (val st = summaryStripState) {
                    is SectionState.Data -> {
                        MonthSummaryStripSection(
                            data = st.data,
                            mode = currentMode,
                            onSpentClick = {
                                val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                searchFilterToApply = SearchFilterState(
                                    typeFilter = TransactionTypeFilter.DEBIT,
                                    datePreset = DateRangePreset.CUSTOM,
                                    customStartDate = window.startMillis,
                                    customEndDate = window.endMillis
                                )
                                showSearchScreen = true
                            },
                            onIncomeClick = {
                                val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                searchFilterToApply = SearchFilterState(
                                    typeFilter = TransactionTypeFilter.CREDIT,
                                    datePreset = DateRangePreset.CUSTOM,
                                    customStartDate = window.startMillis,
                                    customEndDate = window.endMillis
                                )
                                showSearchScreen = true
                            }
                        )
                    }
                    is SectionState.Loading -> {
                        SectionPlaceholder(height = 110.dp)
                    }
                    is SectionState.Empty -> {}
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 2: SPENDING BY CATEGORY (All ranges, Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            if (monthState.categorySpends.isNotEmpty()) {
                item {
                    CategoryDonutChart(
                        categorySpends = monthState.categorySpends,
                        totalSpent = monthState.totalSpent,
                        onCategoryClick = { item ->
                            haptics.click()
                            selectedCategoryDetail = item
                        }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 3: DAILY SPEND (Month, 30 Days; Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month || currentRange is InsightsRange.Days30) {
                if (monthState.dailySpends.isNotEmpty()) {
                    item {
                        DailySpendBarChart(
                            dailySpends = monthState.dailySpends,
                            daysInMonth = monthState.daysInMonth,
                            currentDay = monthState.currentDay,
                            isCurrentMonth = monthState.isCurrentMonth
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 4: CALENDAR HEATMAP (Month only, Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month) {
                item {
                    when (val st = calendarHeatmapState) {
                        is SectionState.Data -> {
                            CalendarHeatmapSection(
                                data = st.data,
                                mode = currentMode,
                                onViewDayInHistory = { date ->
                                    val start = InsightsPeriods.toStartOfDayMillis(date, ZoneId.systemDefault())
                                    val end = InsightsPeriods.toEndOfDayMillis(date, ZoneId.systemDefault())
                                    searchFilterToApply = SearchFilterState(
                                        datePreset = DateRangePreset.CUSTOM,
                                        customStartDate = start,
                                        customEndDate = end
                                    )
                                    showSearchScreen = true
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 240.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 5: CATEGORY LEADERBOARD / MOVERS (All ranges, Expense only) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = categoryMoversState) {
                        is SectionState.Data -> {
                            CategoryMoversSection(
                                data = st.data,
                                onCategoryClick = { catId ->
                                    val item = monthState.categorySpends.find { it.categoryId == catId }
                                    if (item != null) {
                                        selectedCategoryDetail = item
                                    } else {
                                        val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                        searchFilterToApply = SearchFilterState(
                                            typeFilter = TransactionTypeFilter.DEBIT,
                                            datePreset = DateRangePreset.CUSTOM,
                                            customStartDate = window.startMillis,
                                            customEndDate = window.endMillis,
                                            selectedCategoryIds = if (catId != null) setOf(catId) else emptySet()
                                        )
                                        showSearchScreen = true
                                    }
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 160.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 6A: PAYMENT METHOD SPLIT (All ranges, Both modes) — No Card
            // ═══════════════════════════════════════════════════════════
            item {
                when (val st = paymentMethodState) {
                    is SectionState.Data -> {
                        PaymentMethodSplitSection(
                            data = st.data,
                            mode = currentMode,
                            onMethodClick = { method ->
                                val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                searchFilterToApply = SearchFilterState(
                                    typeFilter = if (currentMode == InsightsMode.EXPENSE) TransactionTypeFilter.DEBIT else TransactionTypeFilter.CREDIT,
                                    datePreset = DateRangePreset.CUSTOM,
                                    customStartDate = window.startMillis,
                                    customEndDate = window.endMillis,
                                    selectedSourceTypes = setOf(method)
                                )
                                showSearchScreen = true
                            }
                        )
                    }
                    is SectionState.Loading -> {
                        SectionPlaceholder(height = 160.dp)
                    }
                    is SectionState.Empty -> {}
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 6B: TOP PAYEES (Expense) / INCOME SOURCES (Income) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = topPayeesState) {
                        is SectionState.Data -> {
                            TopPayeesSection(
                                data = st.data,
                                onPayeeClick = { payee ->
                                    selectedPayeeDetail = payee
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 180.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            } else {
                item {
                    when (val st = incomeSourcesState) {
                        is SectionState.Data -> {
                            IncomeSourcesSection(
                                data = st.data,
                                onSourceClick = { source ->
                                    selectedPayeeDetail = source
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 180.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 7: BUDGET VS ACTUAL (Month only, Expense only) — Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month && currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = budgetVsActualState) {
                        is SectionState.Data -> {
                            BudgetVsActualSection(
                                data = st.data,
                                onCategoryClick = { catId ->
                                    val item = monthState.categorySpends.find { it.categoryId == catId }
                                    if (item != null) {
                                        selectedCategoryDetail = item
                                    } else {
                                        val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                        searchFilterToApply = SearchFilterState(
                                            typeFilter = TransactionTypeFilter.DEBIT,
                                            datePreset = DateRangePreset.CUSTOM,
                                            customStartDate = window.startMillis,
                                            customEndDate = window.endMillis,
                                            selectedCategoryIds = if (catId != null) setOf(catId) else emptySet()
                                        )
                                        showSearchScreen = true
                                    }
                                },
                                onEditBudgetClick = { catId ->
                                    setBudgetCategory = allCategories.find { it.id == catId }
                                    showSetBudgetDialog = true
                                },
                                onSetOverallBudgetClick = {
                                    setBudgetCategory = null
                                    showSetBudgetDialog = true
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 180.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 8: CUMULATIVE SPEND (Month, 30D; Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month || currentRange is InsightsRange.Days30) {
                item {
                    when (val st = cumulativeSpendState) {
                        is SectionState.Data -> {
                            CumulativeSpendLineSection(data = st.data)
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 220.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 9: MONTH-END PROJECTION (Month, current month, Expense) — Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month && currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = projectionState) {
                        is SectionState.Data -> {
                            MonthEndProjectionSection(
                                data = st.data,
                                onSetBudgetClick = {
                                    setBudgetCategory = null
                                    showSetBudgetDialog = true
                                }
                            )
                        }
                        is SectionState.Empty -> {
                            Text(
                                text = st.reason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 140.dp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 10: BIGGEST TRANSACTIONS (All ranges, Both modes) — No Card
            // ═══════════════════════════════════════════════════════════
            item {
                when (val st = biggestTransactionsState) {
                    is SectionState.Data -> {
                        BiggestTransactionsSection(
                            data = st.data,
                            mode = currentMode,
                            onTransactionClick = { tx ->
                                val cat = allCategories.find { it.id == tx.categoryId }
                                selectedTxForCategoryPicker = TransactionWithCategory(transaction = tx, category = cat)
                            }
                        )
                    }
                    is SectionState.Loading -> {
                        SectionPlaceholder(height = 180.dp)
                    }
                    is SectionState.Empty -> {}
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 11: WEEKEND VS WEEKDAY (All ranges, Expense only) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = weekendVsWeekdayState) {
                        is SectionState.Data -> {
                            WeekendVsWeekdaySection(data = st.data)
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 150.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 12: SMALL SPENDS (All ranges, Expense only) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = smallSpendsState) {
                        is SectionState.Data -> {
                            SmallSpendsSection(
                                data = st.data,
                                onThresholdSelected = { th ->
                                    insightsViewModel.setSmallSpendThreshold(th)
                                },
                                onCardClick = {
                                    val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                    searchFilterToApply = SearchFilterState(
                                        typeFilter = TransactionTypeFilter.DEBIT,
                                        datePreset = DateRangePreset.CUSTOM,
                                        customStartDate = window.startMillis,
                                        customEndDate = window.endMillis,
                                        amountPreset = AmountRangePreset.CUSTOM,
                                        customMaxAmount = st.data.threshold
                                    )
                                    showSearchScreen = true
                                },
                                onPayeeClick = { payee ->
                                    selectedPayeeDetail = payee
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 140.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 13: INCOME SOURCES (Expense mode only — already shown for Income above)
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = incomeSourcesState) {
                        is SectionState.Data -> {
                            IncomeSourcesSection(
                                data = st.data,
                                onSourceClick = { source ->
                                    selectedPayeeDetail = source
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 180.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 14: SPLIT EXPENSES SUMMARY (All ranges, Expense only) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = splitExpensesState) {
                        is SectionState.Data -> {
                            SplitExpensesSummarySection(
                                data = st.data,
                                onOpenLending = {
                                    val window = InsightsPeriods.calculateWindow(currentRange, ZoneId.systemDefault())
                                    searchFilterToApply = SearchFilterState(
                                        query = "Split",
                                        datePreset = DateRangePreset.CUSTOM,
                                        customStartDate = window.startMillis,
                                        customEndDate = window.endMillis
                                    )
                                    showSearchScreen = true
                                }
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 140.dp)
                        }
                        is SectionState.Empty -> {}
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 15: SUGGESTED BUDGET (Month, Expense only) — No Card
            // ═══════════════════════════════════════════════════════════
            if (currentRange is InsightsRange.Month && currentMode == InsightsMode.EXPENSE) {
                item {
                    when (val st = suggestedBudgetState) {
                        is SectionState.Data -> {
                            SuggestedBudgetSection(
                                data = st.data,
                                onApplyCategory = { catRow ->
                                    haptics.success()
                                    insightsViewModel.applyCategoryBudget(
                                        categoryId = catRow.categoryId,
                                        amount = catRow.suggestedBudget,
                                        yearMonth = activeSelectedMonth
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Budget applied for ${catRow.categoryName}")
                                    }
                                },
                                onApplyAll = { suggestedData ->
                                    haptics.success()
                                    insightsViewModel.applyAllSuggestedBudgets(
                                        suggested = suggestedData,
                                        yearMonth = activeSelectedMonth
                                    ) { previousBudgets ->
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Applied suggested budgets",
                                                actionLabel = "Undo"
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                insightsViewModel.restorePreviousBudgets(previousBudgets)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        is SectionState.Empty -> {
                            Text(
                                text = st.reason,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        is SectionState.Loading -> {
                            SectionPlaceholder(height = 160.dp)
                        }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 16: PERSONAL RECORDS (Always, Both modes) — No Card
            // ═══════════════════════════════════════════════════════════
            item {
                when (val st = personalRecordsState) {
                    is SectionState.Data -> {
                        PersonalRecordsSection(
                            data = st.data,
                            onSelectMonth = { ym ->
                                insightsViewModel.setRange(InsightsRange.Month(ym))
                                monthViewModel.selectMonth(ym.year, ym.monthValue)
                            },
                            onSelectDay = { date ->
                                val start = InsightsPeriods.toStartOfDayMillis(date, ZoneId.systemDefault())
                                val end = InsightsPeriods.toEndOfDayMillis(date, ZoneId.systemDefault())
                                searchFilterToApply = SearchFilterState(
                                    datePreset = DateRangePreset.CUSTOM,
                                    customStartDate = start,
                                    customEndDate = end
                                )
                                showSearchScreen = true
                            }
                        )
                    }
                    is SectionState.Loading -> {
                        SectionPlaceholder(height = 200.dp)
                    }
                    is SectionState.Empty -> {
                        Text(
                            text = st.reason,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════
            // SECTION 17: YEAR HEATMAP (Always, Both modes) — Card
            // ═══════════════════════════════════════════════════════════
            item {
                when (val st = yearHeatmapState) {
                    is SectionState.Data -> {
                        YearHeatmapSection(
                            data = st.data,
                            mode = currentMode,
                            onYearChange = { y -> insightsViewModel.setYearForHeatmap(y) },
                            onViewDayInHistory = { date ->
                                val start = InsightsPeriods.toStartOfDayMillis(date, ZoneId.systemDefault())
                                val end = InsightsPeriods.toEndOfDayMillis(date, ZoneId.systemDefault())
                                searchFilterToApply = SearchFilterState(
                                    datePreset = DateRangePreset.CUSTOM,
                                    customStartDate = start,
                                    customEndDate = end
                                )
                                showSearchScreen = true
                            }
                        )
                    }
                    is SectionState.Loading -> {
                        SectionPlaceholder(height = 220.dp)
                    }
                    is SectionState.Empty -> {}
                }
            }

            // ═══════════════════════════════════════════════════════════
            // TOOLS ROW: Compare Months, Monthly Recap, All-Time Trends
            // ═══════════════════════════════════════════════════════════
            item {
                InsightsToolsRow(
                    onCompareMonthsClick = {
                        haptics.click()
                        showCompareMonths = true
                    },
                    onMonthlyRecapClick = {
                        haptics.click()
                        showMonthlyRecap = true
                    },
                    onAllTimeTrendsClick = {
                        haptics.click()
                        showAllTimeTrends = true
                    }
                )
            }

            // Bottom spacer so content clears floating dock
            item {
                Spacer(modifier = Modifier.height(115.dp))
            }
        }
    }

    // Export Statement Sheet
    if (showExportSheet) {
        ExportStatementSheet(
            initialRange = if (monthState.isCurrentMonth) ExportRange.CurrentMonth else ExportRange.SpecificMonth(monthState.selectedYear, monthState.selectedMonth),
            onDismiss = { showExportSheet = false }
        )
    }

    // Set Budget Dialog (overall or category)
    if (showSetBudgetDialog) {
        val monthDtf = DateTimeFormatter.ofPattern("MMMM yyyy")
        val currentBudgetVal = if (setBudgetCategory == null) {
            monthViewModel.uiState.value.totalSpent // or current overall budget
        } else 0.0

        SetBudgetDialog(
            currentBudget = currentBudgetVal,
            monthName = activeSelectedMonth.format(monthDtf),
            category = setBudgetCategory,
            onDismiss = {
                showSetBudgetDialog = false
                setBudgetCategory = null
            },
            onSaveBudget = { newLimit ->
                val cat = setBudgetCategory
                if (cat == null) {
                    insightsViewModel.applyOverallBudget(newLimit, activeSelectedMonth)
                } else {
                    insightsViewModel.applyCategoryBudget(cat.id, newLimit, activeSelectedMonth)
                }
                showSetBudgetDialog = false
                setBudgetCategory = null
            }
        )
    }

    // Category Picker Sheet
    selectedTxForCategoryPicker?.let { item: TransactionWithCategory ->
        CategoryPickerSheet(
            targetTransaction = item,
            categories = allCategories,
            onDismiss = { selectedTxForCategoryPicker = null },
            onCategorySelected = { newCatId: Long, note: String? ->
                insightsViewModel.updateTransactionCategory(
                    transactionId = item.transaction.id,
                    newCategoryId = newCatId,
                    counterparty = item.transaction.counterparty,
                    note = note
                )
                selectedTxForCategoryPicker = null
            },
            onOpenLendBorrow = {
                lendingEntryForTransaction = item
                selectedTxForCategoryPicker = null
            }
        )
    }

    lendingEntryForTransaction?.let { target ->
        com.omkarnub.kanri.ui.lending.LendingTransactionBridgeDialog(
            targetTransaction = target,
            onDismiss = { lendingEntryForTransaction = null }
        )
    }

    if (showFinancialHealthDetailsSheet) {
        com.omkarnub.kanri.ui.home.FinancialHealthDetailsSheet(
            financialHealth = financialHealth,
            monthName = monthState.monthTitle,
            onDismiss = { showFinancialHealthDetailsSheet = false }
        )
    }
}

@Composable
private fun SectionPlaceholder(height: androidx.compose.ui.unit.Dp) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {}
}
