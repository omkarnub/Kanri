package com.omkarnub.kanri.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.budget.BudgetCalculator
import com.omkarnub.kanri.data.db.BudgetEntity
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.home.formatCurrency
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val budgetDao = db.budgetDao()
    private val lendingDao = db.lendingDao()
    private val recurringPaymentDao = db.recurringPaymentDao()

    // -------------------------------------------------------------------------
    // Global Filter States
    // -------------------------------------------------------------------------
    private val _range = MutableStateFlow<InsightsRange>(InsightsRange.Month(YearMonth.now()))
    val range: StateFlow<InsightsRange> = _range.asStateFlow()

    private val _mode = MutableStateFlow(InsightsMode.EXPENSE)
    val mode: StateFlow<InsightsMode> = _mode.asStateFlow()

    private val _smallSpendsThreshold = MutableStateFlow(100.0)
    val smallSpendsThreshold: StateFlow<Double> = _smallSpendsThreshold.asStateFlow()

    private val _yearHeatmapYear = MutableStateFlow(LocalDate.now().year)
    val yearHeatmapYear: StateFlow<Int> = _yearHeatmapYear.asStateFlow()

    fun setRange(newRange: InsightsRange) {
        _range.value = newRange
    }

    fun setMode(newMode: InsightsMode) {
        _mode.value = newMode
    }

    fun selectMonth(yearMonth: YearMonth) {
        _range.value = InsightsRange.Month(yearMonth)
    }

    fun previousMonth() {
        val current = _range.value
        if (current is InsightsRange.Month) {
            _range.value = InsightsRange.Month(current.yearMonth.minusMonths(1))
        }
    }

    fun nextMonth() {
        val current = _range.value
        if (current is InsightsRange.Month) {
            _range.value = InsightsRange.Month(current.yearMonth.plusMonths(1))
        }
    }

    fun setSmallSpendsThreshold(threshold: Double) {
        _smallSpendsThreshold.value = threshold
    }

    fun setSmallSpendThreshold(threshold: Double) = setSmallSpendsThreshold(threshold)

    fun setYearHeatmapYear(year: Int) {
        _yearHeatmapYear.value = year
    }

    fun setYearForHeatmap(year: Int) = setYearHeatmapYear(year)
    fun setHeatmapYear(year: Int) = setYearHeatmapYear(year)

    // -------------------------------------------------------------------------
    // 5.2 Month Summary Strip State
    // -------------------------------------------------------------------------
    val summaryStripState: StateFlow<SectionState<SummaryStripData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val prevWindow = InsightsPeriods.calculatePreviousPeriod(range)

            val currentTx = transactionDao.getTransactionsBetweenSync(window.startMillis, window.endMillis)
            val prevTx = transactionDao.getTransactionsBetweenSync(prevWindow.startMillis, prevWindow.endMillis)

            if (currentTx.isEmpty() && prevTx.isEmpty()) {
                emit(SectionState.Empty("No transactions in this period"))
                return@flow
            }

            var spent = 0.0
            var income = 0.0
            var count = 0
            for (tx in currentTx) {
                if (tx.type.equals("DEBIT", ignoreCase = true)) {
                    spent += tx.amount
                    count++
                } else if (tx.type.equals("CREDIT", ignoreCase = true)) {
                    income += tx.amount
                    count++
                }
            }

            var prevSpent = 0.0
            var prevIncome = 0.0
            for (tx in prevTx) {
                if (tx.type.equals("DEBIT", ignoreCase = true)) prevSpent += tx.amount
                else if (tx.type.equals("CREDIT", ignoreCase = true)) prevIncome += tx.amount
            }

            val net = income - spent
            val prevNet = prevIncome - prevSpent

            val daysElapsed = window.daysCount.coerceAtLeast(1)
            val avgPerDay = (if (mode == InsightsMode.EXPENSE) spent else income) / daysElapsed

            val (deltaSpentPct, deltaSpentAmt) = InsightsCalculators.calculateDelta(spent, prevSpent)
            val (deltaIncomePct, deltaIncomeAmt) = InsightsCalculators.calculateDelta(income, prevIncome)
            val (deltaNetPct, deltaNetAmt) = InsightsCalculators.calculateDelta(net, prevNet)

            val prevLabel = if (range is InsightsRange.Month) "vs last month" else "vs previous ${window.daysCount} days"

            emit(
                SectionState.Data(
                    SummaryStripData(
                        spent = spent,
                        income = income,
                        net = net,
                        transactionCount = count,
                        averagePerDay = avgPerDay,
                        deltaSpentPercent = deltaSpentPct,
                        deltaSpentAmount = deltaSpentAmt,
                        deltaIncomePercent = deltaIncomePct,
                        deltaIncomeAmount = deltaIncomeAmt,
                        deltaNetPercent = deltaNetPct,
                        deltaNetAmount = deltaNetAmt,
                        previousPeriodLabel = prevLabel
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.3 Month-End Projection State
    // -------------------------------------------------------------------------
    val projectionState: StateFlow<SectionState<MonthEndProjectionData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (range !is InsightsRange.Month || mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Projection only available for Month Expense view"))
                return@flow
            }

            val now = LocalDate.now()
            val currentYm = YearMonth.from(now)
            if (range.yearMonth != currentYm) {
                emit(SectionState.Empty("Projection applies to the current active month only"))
                return@flow
            }

            val daysElapsed = now.dayOfMonth
            if (daysElapsed < 4) {
                emit(SectionState.Empty("Projection improves after a few days of data"))
                return@flow
            }

            emit(SectionState.Loading)

            val window = InsightsPeriods.calculateRangeWindow(range)
            val transactions = transactionDao.getTransactionsByTypeBetweenSync("DEBIT", window.startMillis, window.endMillis)
            val spentSoFar = transactions.sumOf { it.amount }
            val txAmounts = transactions.map { it.amount }

            val monthKey = "%04d-%02d".format(range.yearMonth.year, range.yearMonth.monthValue)
            val budgetEntity = budgetDao.getBudgetForMonthSync(monthKey)
            val monthlyBudget = budgetEntity?.monthlyLimit

            // Unpaid recurring payments due between tomorrow and end of month
            val tomorrowStart = InsightsPeriods.toStartOfDayMillis(now.plusDays(1))
            val monthEnd = InsightsPeriods.toEndOfDayMillis(range.yearMonth.atEndOfMonth())
            val allRecurring = recurringPaymentDao.getAllRecurringPaymentsSync()
            val unpaidRecurring = allRecurring
                .filter { it.isActive && it.nextDueTimestamp in tomorrowStart..monthEnd }
                .sumOf { it.amount }

            // Last month spend
            val prevMonthWindow = InsightsPeriods.calculateRangeWindow(InsightsRange.Month(range.yearMonth.minusMonths(1)))
            val lastMonthTx = transactionDao.getTransactionsByTypeBetweenSync("DEBIT", prevMonthWindow.startMillis, prevMonthWindow.endMillis)
            val lastMonthTotal = if (lastMonthTx.isNotEmpty()) lastMonthTx.sumOf { it.amount } else null

            val daysRemaining = (range.yearMonth.lengthOfMonth() - daysElapsed).coerceAtLeast(0)

            val projection = InsightsCalculators.calculateProjection(
                InsightsCalculators.ProjectionInput(
                    spentSoFar = spentSoFar,
                    daysElapsed = daysElapsed,
                    daysRemaining = daysRemaining,
                    monthlyBudget = monthlyBudget,
                    transactionAmounts = txAmounts,
                    recurringAlreadyPaid = 0.0,
                    unpaidRecurringTotal = unpaidRecurring,
                    lastMonthTotal = lastMonthTotal
                )
            )

            if (projection != null) {
                emit(SectionState.Data(projection))
            } else {
                emit(SectionState.Empty("Needs a few more days of data"))
            }
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.4 Cumulative Spend Line State
    // -------------------------------------------------------------------------
    val cumulativeLineState: StateFlow<SectionState<CumulativeLineData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (range !is InsightsRange.Month && range !is InsightsRange.Days30) {
                emit(SectionState.Empty("Cumulative line available for Month and 30 Days"))
                return@flow
            }

            emit(SectionState.Loading)
            val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"
            val window = InsightsPeriods.calculateRangeWindow(range)
            val prevWindow = InsightsPeriods.calculatePreviousPeriod(range)

            val currentAggregates = transactionDao.getDailySpendAggregates(txType, window.startMillis, window.endMillis)
            val prevAggregates = transactionDao.getDailySpendAggregates(txType, prevWindow.startMillis, prevWindow.endMillis)

            val currentMap = currentAggregates.associate { it.dayString to it.totalAmount }
            val prevMap = prevAggregates.associate { it.dayString to it.totalAmount }

            val currentPoints = mutableListOf<CumulativePoint>()
            var currentCum = 0.0
            var currDate = window.startDate
            var dayIdx = 1

            while (!currDate.isAfter(window.endDate)) {
                val dayStr = currDate.toString()
                val daily = currentMap[dayStr] ?: 0.0
                currentCum += daily
                currentPoints.add(CumulativePoint(dayIdx, "${currDate.dayOfMonth} ${currDate.month.name.take(3)}", currentCum, daily))
                currDate = currDate.plusDays(1)
                dayIdx++
            }

            val ghostPoints = mutableListOf<CumulativePoint>()
            var ghostCum = 0.0
            var ghostDate = prevWindow.startDate
            var ghostIdx = 1

            while (!ghostDate.isAfter(prevWindow.endDate)) {
                val dayStr = ghostDate.toString()
                val daily = prevMap[dayStr] ?: 0.0
                ghostCum += daily
                ghostPoints.add(CumulativePoint(ghostIdx, "${ghostDate.dayOfMonth} ${ghostDate.month.name.take(3)}", ghostCum, daily))
                ghostDate = ghostDate.plusDays(1)
                ghostIdx++
            }

            // Projected points extension if in current month
            val projectedPoints = mutableListOf<CumulativePoint>()
            val now = LocalDate.now()
            if (range is InsightsRange.Month && range.yearMonth == YearMonth.from(now) && mode == InsightsMode.EXPENSE) {
                val daysElapsed = now.dayOfMonth
                val totalDays = range.yearMonth.lengthOfMonth()
                if (daysElapsed in 4 until totalDays && currentPoints.isNotEmpty()) {
                    val lastPoint = currentPoints.last()
                    val daysRemaining = totalDays - daysElapsed
                    val runRate = (lastPoint.cumulativeAmount / daysElapsed).coerceAtLeast(0.0)
                    var runCum = lastPoint.cumulativeAmount

                    for (d in (daysElapsed + 1)..totalDays) {
                        runCum += runRate
                        val date = range.yearMonth.atDay(d)
                        projectedPoints.add(CumulativePoint(d, "${date.dayOfMonth} ${date.month.name.take(3)}", runCum, runRate))
                    }
                }
            }

            val budgetLimit = if (range is InsightsRange.Month && mode == InsightsMode.EXPENSE) {
                val monthKey = "%04d-%02d".format(range.yearMonth.year, range.yearMonth.monthValue)
                budgetDao.getBudgetForMonthSync(monthKey)?.monthlyLimit
            } else null

            val maxVal = maxOf(
                currentPoints.maxOfOrNull { it.cumulativeAmount } ?: 0.0,
                ghostPoints.maxOfOrNull { it.cumulativeAmount } ?: 0.0,
                projectedPoints.maxOfOrNull { it.cumulativeAmount } ?: 0.0,
                budgetLimit ?: 0.0,
                100.0
            )

            val prevLabel = if (range is InsightsRange.Month) "last month" else "previous 30d"

            emit(
                SectionState.Data(
                    CumulativeLineData(
                        currentPoints = currentPoints,
                        ghostPoints = ghostPoints,
                        projectedPoints = projectedPoints,
                        budgetLimit = budgetLimit,
                        maxAmount = maxVal,
                        previousPeriodLabel = prevLabel
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.5 Budget vs Actual per Category State
    // -------------------------------------------------------------------------
    val budgetVsActualState: StateFlow<SectionState<BudgetVsActualData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (range !is InsightsRange.Month || mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Budget vs Actual appears in Month view"))
                return@flow
            }

            emit(SectionState.Loading)

            val monthKey = "%04d-%02d".format(range.yearMonth.year, range.yearMonth.monthValue)
            val overallBudget = budgetDao.getBudgetForMonthSync(monthKey)?.monthlyLimit ?: 0.0

            val window = InsightsPeriods.calculateRangeWindow(range)
            val categorySpends = transactionDao.getCategorySpendAggregates("DEBIT", window.startMillis, window.endMillis)
            val categoryMap = categoryDao.getAllCategoriesSync().associateBy { it.id }

            val catBudgetPrefix = "$monthKey:cat:%"
            val categoryBudgets = budgetDao.getCategoryBudgetsForMonthSync(catBudgetPrefix)
            val catBudgetMap = categoryBudgets.associate {
                val catId = it.monthKey.substringAfterLast(":").toLongOrNull()
                catId to it.monthlyLimit
            }

            val totalSpent = categorySpends.sumOf { it.totalAmount }
            val overallRemaining = (overallBudget - totalSpent).coerceAtLeast(0.0)
            val overallPercentUsed = if (overallBudget > 0) (totalSpent / overallBudget).toFloat().coerceIn(0f, 2f) else 1f

            val categoryRows = mutableListOf<CategoryBudgetRow>()
            var budgetedSpendSum = 0.0

            for ((catId, budgetLimit) in catBudgetMap) {
                if (catId == null) continue
                val cat = categoryMap[catId]
                val spent = categorySpends.find { it.categoryId == catId }?.totalAmount ?: 0.0
                budgetedSpendSum += spent
                val pct = if (budgetLimit > 0) (spent / budgetLimit).toFloat() else 0f
                val remaining = budgetLimit - spent

                categoryRows.add(
                    CategoryBudgetRow(
                        categoryId = catId,
                        categoryName = cat?.name ?: "Category $catId",
                        categoryIconName = cat?.iconName ?: "category",
                        categoryColorHex = cat?.colorHex ?: "#8D6E63",
                        budgetedAmount = budgetLimit,
                        spentAmount = spent,
                        percentUsed = pct,
                        remainingAmount = abs(remaining),
                        isOverBudget = remaining < 0
                    )
                )
            }

            categoryRows.sortByDescending { it.percentUsed }

            val unbudgetedSpend = (totalSpent - budgetedSpendSum).coerceAtLeast(0.0)
            val unbudgetedCats = categorySpends
                .filter { it.categoryId !in catBudgetMap.keys }
                .map {
                    val cat = categoryMap[it.categoryId]
                    UnbudgetedCategoryRow(
                        categoryId = it.categoryId,
                        categoryName = cat?.name ?: "Uncategorized",
                        categoryIconName = cat?.iconName ?: "category",
                        spentAmount = it.totalAmount
                    )
                }

            val sumOfCatBudgets = catBudgetMap.values.sum()
            val exceedsOverall = overallBudget > 0 && sumOfCatBudgets > overallBudget

            emit(
                SectionState.Data(
                    BudgetVsActualData(
                        overallBudget = overallBudget,
                        overallSpent = totalSpent,
                        overallPercentUsed = overallPercentUsed,
                        overallRemaining = overallRemaining,
                        isOverallOverBudget = totalSpent > overallBudget && overallBudget > 0,
                        categoryRows = categoryRows,
                        unbudgetedSpendTotal = unbudgetedSpend,
                        unbudgetedCategories = unbudgetedCats,
                        sumOfCategoriesExceedsOverall = exceedsOverall
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.6 Calendar Heatmap State
    // -------------------------------------------------------------------------
    val calendarHeatmapState: StateFlow<SectionState<CalendarHeatmapData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (range !is InsightsRange.Month) {
                emit(SectionState.Empty("Calendar Heatmap is available in Month view"))
                return@flow
            }

            emit(SectionState.Loading)
            val ym = range.yearMonth
            val window = InsightsPeriods.calculateRangeWindow(range)
            val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"
            val aggregates = transactionDao.getDailySpendAggregates(txType, window.startMillis, window.endMillis)
            val dayMap = aggregates.associate { it.dayString to it.totalAmount }

            val totalDaysInMonth = ym.lengthOfMonth()
            val now = LocalDate.now()
            val daysList = mutableListOf<CalendarDayData>()
            val dailyAmounts = mutableListOf<Double>()

            for (d in 1..totalDaysInMonth) {
                val date = ym.atDay(d)
                val amount = dayMap[date.toString()] ?: 0.0
                dailyAmounts.add(amount)
            }

            val thresholds = InsightsCalculators.calculateQuantileThresholds(dailyAmounts)
            var noSpendDays = 0
            var highestDay: CalendarDayData? = null
            var maxAmount = 0.0

            for (d in 1..totalDaysInMonth) {
                val date = ym.atDay(d)
                val amount = dayMap[date.toString()] ?: 0.0
                val isFuture = date.isAfter(now)
                val isToday = date.isEqual(now)
                val hasSpend = amount > 0.0
                if (!isFuture && !hasSpend) noSpendDays++

                val level = if (isFuture) 0 else InsightsCalculators.getQuantileLevel(amount, thresholds)

                val dayData = CalendarDayData(
                    date = date,
                    dayOfMonth = d,
                    dayOfWeek = date.dayOfWeek.value, // 1 = Monday .. 7 = Sunday
                    amount = amount,
                    quantileLevel = level,
                    isFuture = isFuture,
                    isToday = isToday,
                    hasSpend = hasSpend
                )
                daysList.add(dayData)

                if (amount > maxAmount) {
                    maxAmount = amount
                    highestDay = dayData
                }
            }

            // Monday-first offset
            val firstDayOfWeek = ym.atDay(1).dayOfWeek.value // 1=Mon .. 7=Sun
            val emptyLeadingDays = firstDayOfWeek - 1

            emit(
                SectionState.Data(
                    CalendarHeatmapData(
                        yearMonth = ym,
                        days = daysList,
                        emptyLeadingDays = emptyLeadingDays,
                        noSpendDaysCount = noSpendDays,
                        highestDayAmount = maxAmount,
                        highestDayDate = highestDay?.date,
                        quantileThresholds = thresholds
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.7 Category Movers State
    // -------------------------------------------------------------------------
    val categoryMoversState: StateFlow<SectionState<CategoryMoversData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Category movers are available in Expense mode"))
                return@flow
            }

            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val prevWindow = InsightsPeriods.calculatePreviousPeriod(range)

            val currentAggs = transactionDao.getCategorySpendAggregates("DEBIT", window.startMillis, window.endMillis)
            val prevAggs = transactionDao.getCategorySpendAggregates("DEBIT", prevWindow.startMillis, prevWindow.endMillis)

            if (currentAggs.isEmpty() && prevAggs.isEmpty()) {
                emit(SectionState.Empty("No categories with sufficient spend"))
                return@flow
            }

            val currentMap = currentAggs.associate { it.categoryId to it.totalAmount }
            val prevMap = prevAggs.associate { it.categoryId to it.totalAmount }

            val categories = categoryDao.getAllCategoriesSync()
            val categoryMeta: Map<Long?, Triple<String, String, String>> = categories.associate { (it.id as Long?) to Triple(it.name, it.iconName, it.colorHex) }

            val (upMovers, downMovers) = InsightsCalculators.calculateMovers(currentMap, prevMap, categoryMeta)

            if (upMovers.isEmpty() && downMovers.isEmpty()) {
                emit(SectionState.Empty("No significant category changes in this period"))
                return@flow
            }

            val periodLabel = if (range is InsightsRange.Month) "vs last month" else "vs previous ${window.daysCount} days"

            emit(
                SectionState.Data(
                    CategoryMoversData(
                        upMovers = upMovers,
                        downMovers = downMovers,
                        periodComparisonLabel = periodLabel
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.8 Top Payees State
    // -------------------------------------------------------------------------
    val topPayeesState: StateFlow<SectionState<TopPayeesData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Top payees are replaced by Income Sources in Income mode"))
                return@flow
            }

            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val payeesAgg = transactionDao.getTopPayeeAggregates("DEBIT", window.startMillis, window.endMillis, limit = 15)

            if (payeesAgg.isEmpty()) {
                emit(SectionState.Empty("No payee data in this range"))
                return@flow
            }

            val totalExpense = payeesAgg.sumOf { it.totalAmount }
            val maxAmount = payeesAgg.firstOrNull()?.totalAmount ?: 1.0

            val items = payeesAgg.map { agg ->
                val raw = agg.counterparty ?: "Merchant"
                val display = agg.displayName?.takeIf { it.isNotBlank() } ?: raw
                TopPayeeItem(
                    rawCounterparty = raw,
                    displayName = display,
                    totalAmount = agg.totalAmount,
                    transactionCount = agg.txCount,
                    percentOfTotal = if (totalExpense > 0) (agg.totalAmount / totalExpense) * 100.0 else 0.0,
                    relativeShare = if (maxAmount > 0) (agg.totalAmount / maxAmount).toFloat() else 0f
                )
            }

            emit(SectionState.Data(TopPayeesData(items, totalExpense)))
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.10 Biggest Transactions State
    // -------------------------------------------------------------------------
    val biggestTransactionsState: StateFlow<SectionState<BiggestTransactionsData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            emit(SectionState.Loading)
            val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"
            val window = InsightsPeriods.calculateRangeWindow(range)
            val biggest = transactionDao.getBiggestTransactionsWithCategory(txType, window.startMillis, window.endMillis, limit = 5)

            if (biggest.isEmpty()) {
                emit(SectionState.Empty("No transactions found in this period"))
                return@flow
            }

            val totalForPeriod = transactionDao.getTransactionsByTypeBetweenSync(txType, window.startMillis, window.endMillis)
                .sumOf { it.amount }

            val topItemAmount = biggest.firstOrNull()?.transaction?.amount ?: 0.0
            val topItemShare = if (totalForPeriod > 0) (topItemAmount / totalForPeriod) * 100.0 else 0.0

            val items = biggest.mapIndexed { index, item ->
                val tx = item.transaction
                val cat = item.category
                val payee = tx.displayName?.takeIf { it.isNotBlank() } ?: tx.counterparty ?: "Transaction"
                val pct = if (totalForPeriod > 0) (tx.amount / totalForPeriod) * 100.0 else 0.0

                BiggestTransactionItem(
                    id = tx.id,
                    rank = index + 1,
                    amount = tx.amount,
                    payeeName = payee,
                    categoryName = cat?.name ?: "Uncategorized",
                    categoryIconName = cat?.iconName ?: "category",
                    sourceType = tx.sourceType,
                    timestamp = tx.timestamp,
                    percentOfTotal = pct,
                    rawTransaction = tx
                )
            }

            emit(
                SectionState.Data(
                    BiggestTransactionsData(
                        items = items,
                        topItemShare = topItemShare,
                        hasDominantItem = topItemShare >= 30.0
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.11 Payment Method Split State
    // -------------------------------------------------------------------------
    val paymentMethodSplitState: StateFlow<SectionState<PaymentMethodSplitData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            emit(SectionState.Loading)
            val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"
            val window = InsightsPeriods.calculateRangeWindow(range)
            val aggregates = transactionDao.getSourceTypeAggregates(txType, window.startMillis, window.endMillis)

            if (aggregates.isEmpty()) {
                emit(SectionState.Empty("No payment method data"))
                return@flow
            }

            val total = aggregates.sumOf { it.totalAmount }
            val tones = listOf(1.0f, 0.75f, 0.55f, 0.38f, 0.22f)

            val items = aggregates.mapIndexed { idx, agg ->
                val display = when (agg.sourceType.uppercase()) {
                    "UPI" -> "UPI"
                    "CARD" -> "Card"
                    "ATM" -> "ATM Cash"
                    "BANK_TRANSFER" -> "Bank Transfer"
                    else -> agg.sourceType.replaceFirstChar { it.uppercase() }
                }
                val pct = if (total > 0) (agg.totalAmount / total) * 100.0 else 0.0
                PaymentMethodItem(
                    sourceType = agg.sourceType,
                    displayName = display,
                    totalAmount = agg.totalAmount,
                    count = agg.txCount,
                    percentage = pct,
                    alphaTone = tones.getOrElse(idx) { 0.2f }
                )
            }

            emit(SectionState.Data(PaymentMethodSplitData(items, total)))
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.12 Weekend vs Weekday Split State
    // -------------------------------------------------------------------------
    val weekendVsWeekdayState: StateFlow<SectionState<WeekendVsWeekdayData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Weekend vs Weekday split applies to Expense mode"))
                return@flow
            }

            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val transactions = transactionDao.getTransactionsWithCategoryBetweenSync(window.startMillis, window.endMillis)
                .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) }

            if (transactions.isEmpty()) {
                emit(SectionState.Empty("No expenses in this period"))
                return@flow
            }

            var weekdayTotal = 0.0
            var weekendTotal = 0.0
            val weekdayCatSpends = mutableMapOf<String, Double>()
            val weekendCatSpends = mutableMapOf<String, Double>()

            val zone = ZoneId.systemDefault()
            val weekdayDates = mutableSetOf<LocalDate>()
            val weekendDates = mutableSetOf<LocalDate>()

            for (item in transactions) {
                val date = Instant.ofEpochMilli(item.transaction.timestamp).atZone(zone).toLocalDate()
                val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
                val catName = item.category?.name ?: "Uncategorized"

                if (isWeekend) {
                    weekendTotal += item.transaction.amount
                    weekendDates.add(date)
                    weekendCatSpends[catName] = (weekendCatSpends[catName] ?: 0.0) + item.transaction.amount
                } else {
                    weekdayTotal += item.transaction.amount
                    weekdayDates.add(date)
                    weekdayCatSpends[catName] = (weekdayCatSpends[catName] ?: 0.0) + item.transaction.amount
                }
            }

            // Count total calendar weekdays and weekend days in range
            var cur = window.startDate
            var totalWeekdays = 0
            var totalWeekendDays = 0
            while (!cur.isAfter(window.endDate)) {
                if (cur.dayOfWeek == DayOfWeek.SATURDAY || cur.dayOfWeek == DayOfWeek.SUNDAY) {
                    totalWeekendDays++
                } else {
                    totalWeekdays++
                }
                cur = cur.plusDays(1)
            }

            if (totalWeekdays == 0 || totalWeekendDays == 0) {
                emit(SectionState.Empty("Requires at least one weekday and one weekend day"))
                return@flow
            }

            val topWeekdayCat = weekdayCatSpends.maxByOrNull { it.value }?.key
            val topWeekendCat = weekendCatSpends.maxByOrNull { it.value }?.key

            val result = InsightsCalculators.calculateWeekendWeekday(
                weekdayTotal = weekdayTotal,
                weekdayDays = totalWeekdays,
                weekendTotal = weekendTotal,
                weekendDays = totalWeekendDays,
                weekdayTopCategory = topWeekdayCat,
                weekendTopCategory = topWeekendCat
            )

            if (result != null) {
                emit(SectionState.Data(result))
            } else {
                emit(SectionState.Empty("Insufficient data"))
            }
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.13 Small Spends Total State
    // -------------------------------------------------------------------------
    val smallSpendsState: StateFlow<SectionState<SmallSpendsData>> = combine(_range, _mode, _smallSpendsThreshold) { range, mode, threshold ->
        Triple(range, mode, threshold)
    }.flatMapLatest { (range, mode, threshold) ->
        flow {
            if (mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Small spends apply to Expense mode"))
                return@flow
            }

            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val debits = transactionDao.getTransactionsByTypeBetweenSync("DEBIT", window.startMillis, window.endMillis)
            val totalSpend = debits.sumOf { it.amount }

            val smallSpends = debits.filter { it.amount < threshold }
            if (smallSpends.isEmpty()) {
                emit(SectionState.Empty("No small payments in this range"))
                return@flow
            }

            val smallTotal = smallSpends.sumOf { it.amount }
            val count = smallSpends.size
            val avg = smallTotal / count
            val pct = if (totalSpend > 0) (smallTotal / totalSpend) * 100.0 else 0.0

            val topPayees = smallSpends
                .groupBy { it.displayName?.takeIf { n -> n.isNotBlank() } ?: it.counterparty ?: "Other" }
                .map { (name, list) -> SmallSpendPayee(name, list.size, list.sumOf { it.amount }) }
                .sortedByDescending { it.total }
                .take(3)

            val yearlyPace = InsightsCalculators.calculateSmallSpendsPace(smallTotal, window.daysCount)

            emit(
                SectionState.Data(
                    SmallSpendsData(
                        threshold = threshold,
                        totalAmount = smallTotal,
                        count = count,
                        averageAmount = avg,
                        percentOfTotalSpend = pct,
                        topPayees = topPayees,
                        yearlyPaceAmount = yearlyPace
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.14 Income Sources State
    // -------------------------------------------------------------------------
    val incomeSourcesState: StateFlow<SectionState<IncomeSourcesData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val creditsInRange = transactionDao.getTransactionsByTypeBetweenSync("CREDIT", window.startMillis, window.endMillis)

            if (creditsInRange.isEmpty()) {
                emit(SectionState.Empty("No income received in this range"))
                return@flow
            }

            // Group by normalized counterparty in range
            val groupedInRange = creditsInRange.groupBy {
                it.counterparty?.trim()?.lowercase() ?: "income"
            }

            // Last 6 months credits for regularity classifier
            val sixMonthsAgoMillis = InsightsPeriods.toStartOfDayMillis(LocalDate.now().minusMonths(6))
            val pastCredits = transactionDao.getTransactionsByTypeBetweenSync("CREDIT", sixMonthsAgoMillis, System.currentTimeMillis())
            val zone = ZoneId.systemDefault()

            val sources = mutableListOf<IncomeSourceItem>()

            for ((normKey, txs) in groupedInRange) {
                val total = txs.sumOf { it.amount }
                val count = txs.size
                val raw = txs.firstOrNull()?.counterparty ?: "Source"
                val display = txs.firstNotNullOfOrNull { it.displayName?.takeIf { n -> n.isNotBlank() } } ?: raw
                val lastTxDate = txs.maxByOrNull { it.timestamp }?.let {
                    Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
                }

                // Collect monthly amounts for this counterparty over last 6 months
                val sourcePastTxs = pastCredits.filter { (it.counterparty?.trim()?.lowercase() ?: "") == normKey }
                val monthlyMap = sourcePastTxs.groupBy {
                    val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
                    YearMonth.from(dt)
                }.mapValues { (_, list) -> list.map { it.amount } }

                val (tag, _) = InsightsCalculators.classifyIncomeSource(monthlyMap)

                // Median arrival day of month
                val daysOfMonth = sourcePastTxs.map {
                    Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate().dayOfMonth
                }
                val typicalDay = if (tag != IncomeRegularityTag.IRREGULAR && daysOfMonth.isNotEmpty()) {
                    daysOfMonth.sorted()[daysOfMonth.size / 2]
                } else null

                sources.add(
                    IncomeSourceItem(
                        name = display,
                        rawCounterparty = raw,
                        totalReceived = total,
                        transactionCount = count,
                        tag = tag,
                        lastReceivedDate = lastTxDate,
                        typicalArrivalDayOfMonth = typicalDay
                    )
                )
            }

            sources.sortByDescending { it.totalReceived }

            val regularTotal = sources.filter { it.tag != IncomeRegularityTag.IRREGULAR }.sumOf { it.totalReceived }
            val irregularTotal = sources.filter { it.tag == IncomeRegularityTag.IRREGULAR }.sumOf { it.totalReceived }
            val grandTotal = regularTotal + irregularTotal

            val regPct = if (grandTotal > 0) (regularTotal / grandTotal) * 100.0 else 0.0
            val irregPct = if (grandTotal > 0) (irregularTotal / grandTotal) * 100.0 else 0.0

            emit(
                SectionState.Data(
                    IncomeSourcesData(
                        regularTotal = regularTotal,
                        irregularTotal = irregularTotal,
                        regularPercent = regPct,
                        irregularPercent = irregPct,
                        sources = sources
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.15 Suggested Budget State
    // -------------------------------------------------------------------------
    val suggestedBudgetState: StateFlow<SectionState<SuggestedBudgetData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (range !is InsightsRange.Month || mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Suggested budget appears in Month Expense view"))
                return@flow
            }

            val now = LocalDate.now()
            if (range.yearMonth != YearMonth.from(now)) {
                emit(SectionState.Empty("Suggestions apply to the current active month"))
                return@flow
            }

            emit(SectionState.Loading)

            // Needs at least 2 completed months of history
            val m1 = range.yearMonth.minusMonths(1)
            val m2 = range.yearMonth.minusMonths(2)
            val m3 = range.yearMonth.minusMonths(3)

            val w1 = InsightsPeriods.calculateRangeWindow(InsightsRange.Month(m1))
            val w2 = InsightsPeriods.calculateRangeWindow(InsightsRange.Month(m2))
            val w3 = InsightsPeriods.calculateRangeWindow(InsightsRange.Month(m3))

            val sp1 = transactionDao.getCategorySpendAggregates("DEBIT", w1.startMillis, w1.endMillis)
            val sp2 = transactionDao.getCategorySpendAggregates("DEBIT", w2.startMillis, w2.endMillis)
            val sp3 = transactionDao.getCategorySpendAggregates("DEBIT", w3.startMillis, w3.endMillis)

            val monthsCount = (if (sp1.isNotEmpty()) 1 else 0) + (if (sp2.isNotEmpty()) 1 else 0) + (if (sp3.isNotEmpty()) 1 else 0)
            if (monthsCount < 2) {
                emit(SectionState.Empty("Needs 2 full months of history"))
                return@flow
            }

            val categories = categoryDao.getAllCategoriesSync().associateBy { it.id }
            val currentMonthKey = "%04d-%02d".format(range.yearMonth.year, range.yearMonth.monthValue)
            val currentCatBudgets = budgetDao.getCategoryBudgetsForMonthSync("$currentMonthKey:cat:%")
                .associate {
                    val cid = it.monthKey.substringAfterLast(":").toLongOrNull()
                    cid to it.monthlyLimit
                }

            val allCatIds = (sp1.mapNotNull { it.categoryId } + sp2.mapNotNull { it.categoryId } + sp3.mapNotNull { it.categoryId }).distinct()

            val rows = mutableListOf<SuggestedCategoryRow>()
            var overallSuggested = 0.0

            for (catId in allCatIds) {
                val cat = categories[catId] ?: continue
                val s1 = sp1.find { it.categoryId == catId }?.totalAmount ?: 0.0
                val s2 = sp2.find { it.categoryId == catId }?.totalAmount ?: 0.0
                val s3 = sp3.find { it.categoryId == catId }?.totalAmount ?: 0.0

                val history = listOf(s3, s2, s1).filter { it > 0.0 }
                if (history.isEmpty()) continue

                val suggested = InsightsCalculators.calculateSuggestedCategoryBudget(history)
                val avg = history.average()
                val current = currentCatBudgets[catId]

                overallSuggested += suggested
                rows.add(
                    SuggestedCategoryRow(
                        categoryId = catId,
                        categoryName = cat.name,
                        categoryIconName = cat.iconName,
                        threeMonthAverage = avg,
                        currentBudget = current,
                        suggestedBudget = suggested
                    )
                )
            }

            val overallCurrent = budgetDao.getBudgetForMonthSync(currentMonthKey)?.monthlyLimit

            emit(
                SectionState.Data(
                    SuggestedBudgetData(
                        categories = rows.sortedByDescending { it.suggestedBudget },
                        overallCurrentBudget = overallCurrent,
                        overallSuggestedBudget = overallSuggested,
                        completedMonthsAvailable = monthsCount
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.16 Personal Records State
    // -------------------------------------------------------------------------
    val personalRecordsState: StateFlow<SectionState<PersonalRecordsData>> = flow {
        emit(SectionState.Loading)
        val allTx = transactionDao.getAllTransactionsSync().filter { !it.isDuplicate }

        if (allTx.isEmpty()) {
            emit(SectionState.Empty("Records appear after your first full month"))
            return@flow
        }

        val zone = ZoneId.systemDefault()
        val debits = allTx.filter { it.type.equals("DEBIT", ignoreCase = true) }
        val credits = allTx.filter { it.type.equals("CREDIT", ignoreCase = true) }

        // Monthly debits & credits
        val monthlyDebits = debits.groupBy {
            val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            YearMonth.from(dt)
        }.mapValues { it.value.sumOf { tx -> tx.amount } }

        val monthlyCredits = credits.groupBy {
            val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            YearMonth.from(dt)
        }.mapValues { it.value.sumOf { tx -> tx.amount } }

        val currentYm = YearMonth.now()
        // Complete months only
        val completeMonths = monthlyDebits.keys.filter { it.isBefore(currentYm) }

        if (completeMonths.size < 1) {
            emit(SectionState.Empty("Records appear after your first full month"))
            return@flow
        }

        val highestSpendMonth = completeMonths.maxByOrNull { monthlyDebits[it] ?: 0.0 }?.let {
            it to (monthlyDebits[it] ?: 0.0)
        }
        val lowestSpendMonth = completeMonths.minByOrNull { monthlyDebits[it] ?: 0.0 }?.let {
            it to (monthlyDebits[it] ?: 0.0)
        }

        val highestIncomeMonth = monthlyCredits.keys.filter { it.isBefore(currentYm) }
            .maxByOrNull { monthlyCredits[it] ?: 0.0 }?.let {
                it to (monthlyCredits[it] ?: 0.0)
            }

        val allCompleteMonthsUnion = (completeMonths + monthlyCredits.keys.filter { it.isBefore(currentYm) }).distinct()
        val bestSavingsMonth = allCompleteMonthsUnion.maxByOrNull {
            (monthlyCredits[it] ?: 0.0) - (monthlyDebits[it] ?: 0.0)
        }?.let {
            it to ((monthlyCredits[it] ?: 0.0) - (monthlyDebits[it] ?: 0.0))
        }

        // Biggest single expense
        val biggestExpenseTx = transactionDao.getBiggestSingleExpenseSync()
        val biggestExpense = biggestExpenseTx?.let {
            val dt = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            val payee = it.displayName?.takeIf { n -> n.isNotBlank() } ?: it.counterparty ?: "Expense"
            Triple(it.amount, payee, dt)
        }

        // Daily aggregates for highest spend day & most tx day
        val dailyDebits = debits.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }
        val highestSpendDay = dailyDebits.maxByOrNull { it.value.sumOf { tx -> tx.amount } }?.let {
            it.key to it.value.sumOf { tx -> tx.amount }
        }

        val dailyAllTx = allTx.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }
        val mostTxDay = dailyAllTx.maxByOrNull { it.value.size }?.let {
            it.key to it.value.size
        }

        // No-spend streak calculation
        val streak = com.omkarnub.kanri.data.analytics.StreakCalculator.calculateStreak(allTx)

        emit(
            SectionState.Data(
                PersonalRecordsData(
                    highestSpendMonth = highestSpendMonth,
                    lowestSpendMonth = lowestSpendMonth,
                    highestIncomeMonth = highestIncomeMonth,
                    bestSavingsMonth = bestSavingsMonth,
                    longestNoSpendStreakDays = streak.bestStreak,
                    longestStreakDateRange = "All-time personal best",
                    currentNoSpendStreakDays = streak.currentStreak,
                    biggestSingleExpense = biggestExpense,
                    highestSpendDay = highestSpendDay,
                    mostTransactionsInOneDay = mostTxDay,
                    newRecordsBroken = emptySet()
                )
            )
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.17 Year Heatmap State
    // -------------------------------------------------------------------------
    val yearHeatmapState: StateFlow<SectionState<YearHeatmapData>> = combine(_yearHeatmapYear, _mode) { year, mode ->
        year to mode
    }.flatMapLatest { (year, mode) ->
        flow {
            emit(SectionState.Loading)
            val zone = ZoneId.systemDefault()
            val startYearDate = LocalDate.of(year, 1, 1)
            val endYearDate = LocalDate.of(year, 12, 31)

            val startMillis = InsightsPeriods.toStartOfDayMillis(startYearDate, zone)
            val endMillis = InsightsPeriods.toEndOfDayMillis(endYearDate, zone)

            val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"
            val dailyAggs = transactionDao.getDailySpendAggregates(txType, startMillis, endMillis)
            val dailyMap = dailyAggs.associate { it.dayString to it.totalAmount }
            val dailyCountMap = dailyAggs.associate { it.dayString to it.txCount }

            val allTx = transactionDao.getAllTransactionsSync().filter { !it.isDuplicate }
            val firstTxDate = allTx.minOfOrNull { it.timestamp }?.let { earliestTimestamp ->
                Instant.ofEpochMilli(earliestTimestamp).atZone(zone).toLocalDate()
            }

            val availableYears = if (allTx.isNotEmpty()) {
                val minYear = Instant.ofEpochMilli(allTx.minOf { it.timestamp }).atZone(zone).year
                val maxYear = LocalDate.now().year
                (minYear..maxYear).toList().reversed()
            } else listOf(LocalDate.now().year)

            val allAmounts = dailyAggs.map { it.totalAmount }
            val thresholds = InsightsCalculators.calculateQuantileThresholds(allAmounts)

            val cells = mutableListOf<YearHeatmapCell>()
            var curDate = startYearDate
            var spendingDays = 0
            var noSpendDays = 0
            val today = LocalDate.now()

            while (!curDate.isAfter(endYearDate)) {
                val dayStr = curDate.toString()
                val amt = dailyMap[dayStr] ?: 0.0
                val count = dailyCountMap[dayStr] ?: 0
                val isSpend = amt > 0.0
                val isBeforeFirst = firstTxDate != null && curDate.isBefore(firstTxDate)

                if (!curDate.isAfter(today)) {
                    if (isSpend) spendingDays++ else noSpendDays++
                }

                val level = if (isBeforeFirst) 0 else InsightsCalculators.getQuantileLevel(amt, thresholds)

                cells.add(
                    YearHeatmapCell(
                        date = curDate,
                        amount = amt,
                        quantileLevel = level,
                        isSpendDay = isSpend,
                        isBeforeFirstTx = isBeforeFirst,
                        txCount = count,
                        topCategory = null
                    )
                )
                curDate = curDate.plusDays(1)
            }

            emit(
                SectionState.Data(
                    YearHeatmapData(
                        year = year,
                        availableYears = availableYears,
                        cells = cells,
                        spendingDaysCount = spendingDays,
                        noSpendDaysCount = noSpendDays,
                        quantileThresholds = thresholds
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // 5.19 Split Expenses Summary State
    // -------------------------------------------------------------------------
    val splitExpensesState: StateFlow<SectionState<SplitExpensesSummaryData>> = combine(_range, _mode) { range, mode ->
        range to mode
    }.flatMapLatest { (range, mode) ->
        flow {
            if (mode != InsightsMode.EXPENSE) {
                emit(SectionState.Empty("Split expenses summary applies to Expense mode"))
                return@flow
            }

            emit(SectionState.Loading)
            val window = InsightsPeriods.calculateRangeWindow(range)
            val splits = lendingDao.getSplitRecordsBetweenSync(window.startMillis, window.endMillis)

            if (splits.isEmpty()) {
                emit(SectionState.Empty("No split expense records in this range"))
                return@flow
            }

            // Lent splits (I paid the bill, friends owe me)
            val lentSplits = splits.filter { it.type.equals("LENT", ignoreCase = true) }
            val borrowedSplits = splits.filter { it.type.equals("BORROWED", ignoreCase = true) }

            // Group lent records by date / note to identify distinct split events
            val groupedEvents = lentSplits.groupBy { "${it.date}_${it.notes}" }
            val splitEventsCount = groupedEvents.size

            var totalLentPending = 0.0
            var totalLentRecovered = 0.0
            var myOwnShareTotal = 0.0
            var billsPaidByMeTotal = 0.0

            for ((_, records) in groupedEvents) {
                val perPersonShare = records.firstOrNull()?.amount ?: 0.0
                myOwnShareTotal += perPersonShare // 1 share for me
                val friendsShareTotal = records.sumOf { it.amount }
                billsPaidByMeTotal += (perPersonShare + friendsShareTotal)

                for (r in records) {
                    if (r.isSettled) {
                        totalLentRecovered += r.amount
                    } else {
                        totalLentPending += r.amount
                    }
                }
            }

            val avgParticipants = if (splitEventsCount > 0) {
                (lentSplits.size.toDouble() / splitEventsCount) + 1.0 // friends + me
            } else 0.0

            val billsPaidByOthers = borrowedSplits.sumOf { it.amount }
            val myBorrowedPending = borrowedSplits.filter { !it.isSettled }.sumOf { it.amount }

            // Top pending people
            val topPending = lentSplits.filter { !it.isSettled }
                .groupBy { it.personName }
                .map { (name, list) -> SplitPendingPerson(name, list.sumOf { it.amount }, list.size) }
                .sortedByDescending { it.pendingAmount }
                .take(3)

            emit(
                SectionState.Data(
                    SplitExpensesSummaryData(
                        billsPaidByMeTotal = billsPaidByMeTotal,
                        myOwnShareTotal = myOwnShareTotal,
                        othersOweMeTotal = totalLentPending + totalLentRecovered,
                        recoveredTotal = totalLentRecovered,
                        pendingTotal = totalLentPending,
                        splitEventsCount = splitEventsCount,
                        averageParticipants = avgParticipants,
                        billsPaidByOthersTotal = billsPaidByOthers,
                        myBorrowedSharePending = myBorrowedPending,
                        topPendingPeople = topPending
                    )
                )
            )
        }.flowOn(Dispatchers.IO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SectionState.Loading)

    // -------------------------------------------------------------------------
    // Actions: Budget Updates
    // -------------------------------------------------------------------------
    fun applyOverallBudget(amount: Double, yearMonth: YearMonth = YearMonth.now()) {
        viewModelScope.launch(Dispatchers.IO) {
            val monthKey = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
            budgetDao.setBudget(BudgetEntity(monthKey = monthKey, monthlyLimit = amount))
        }
    }

    fun applyCategoryBudget(categoryId: Long, amount: Double, yearMonth: YearMonth = YearMonth.now()) {
        viewModelScope.launch(Dispatchers.IO) {
            val monthKey = "%04d-%02d:cat:%d".format(yearMonth.year, yearMonth.monthValue, categoryId)
            budgetDao.setBudget(BudgetEntity(monthKey = monthKey, monthlyLimit = amount))
        }
    }

    fun applyAllSuggestedBudgets(suggested: SuggestedBudgetData, yearMonth: YearMonth = YearMonth.now(), onDone: (List<BudgetEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val monthKey = "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)
            val oldBudgets = budgetDao.getAllBudgetsSync().filter { it.monthKey.startsWith(monthKey) }

            // 1. Overall budget
            budgetDao.setBudget(BudgetEntity(monthKey = monthKey, monthlyLimit = suggested.overallSuggestedBudget))

            // 2. Category budgets
            for (cat in suggested.categories) {
                val catKey = "$monthKey:cat:${cat.categoryId}"
                budgetDao.setBudget(BudgetEntity(monthKey = catKey, monthlyLimit = cat.suggestedBudget))
            }

            withContext(Dispatchers.Main) {
                onDone(oldBudgets)
            }
        }
    }

    fun restorePreviousBudgets(previousBudgets: List<BudgetEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetDao.insertAllBudgets(previousBudgets)
        }
    }

    // Flow aliases for MonthViewScreen
    val summaryStripFlow: StateFlow<SectionState<SummaryStripData>> get() = summaryStripState
    val projectionFlow: StateFlow<SectionState<MonthEndProjectionData>> get() = projectionState
    val cumulativeSpendFlow: StateFlow<SectionState<CumulativeLineData>> get() = cumulativeLineState
    val cumulativeSpendState: StateFlow<SectionState<CumulativeLineData>> get() = cumulativeLineState
    val budgetVsActualFlow: StateFlow<SectionState<BudgetVsActualData>> get() = budgetVsActualState
    val calendarHeatmapFlow: StateFlow<SectionState<CalendarHeatmapData>> get() = calendarHeatmapState
    val categoryMoversFlow: StateFlow<SectionState<CategoryMoversData>> get() = categoryMoversState
    val topPayeesFlow: StateFlow<SectionState<TopPayeesData>> get() = topPayeesState
    val biggestTransactionsFlow: StateFlow<SectionState<BiggestTransactionsData>> get() = biggestTransactionsState
    val paymentMethodFlow: StateFlow<SectionState<PaymentMethodSplitData>> get() = paymentMethodSplitState
    val paymentMethodState: StateFlow<SectionState<PaymentMethodSplitData>> get() = paymentMethodSplitState
    val weekendVsWeekdayFlow: StateFlow<SectionState<WeekendVsWeekdayData>> get() = weekendVsWeekdayState
    val smallSpendsFlow: StateFlow<SectionState<SmallSpendsData>> get() = smallSpendsState
    val incomeSourcesFlow: StateFlow<SectionState<IncomeSourcesData>> get() = incomeSourcesState
    val splitExpensesFlow: StateFlow<SectionState<SplitExpensesSummaryData>> get() = splitExpensesState
    val suggestedBudgetFlow: StateFlow<SectionState<SuggestedBudgetData>> get() = suggestedBudgetState
    val personalRecordsFlow: StateFlow<SectionState<PersonalRecordsData>> get() = personalRecordsState
    val yearHeatmapFlow: StateFlow<SectionState<YearHeatmapData>> get() = yearHeatmapState

    val allCategoriesFlow: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    val availableMonthsFlow: Flow<List<YearMonth>> = flow {
        val txs = transactionDao.getAllTransactionsSync().filter { !it.isDuplicate }
        val zone = ZoneId.systemDefault()
        val months = txs.map {
            val date = Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
            YearMonth.of(date.year, date.month)
        }.distinct().sortedDescending()
        emit(if (months.isNotEmpty()) months else listOf(YearMonth.now()))
    }.flowOn(Dispatchers.IO)

    fun updateTransactionCategory(
        transactionId: Long,
        newCategoryId: Long,
        counterparty: String?,
        note: String? = null,
        applyToAll: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            transactionDao.updateCategoryAndNotes(transactionId, newCategoryId, note)
            if (applyToAll && !counterparty.isNullOrBlank()) {
                transactionDao.updateCategoryForCounterparty(counterparty, newCategoryId)
                categoryDao.setMapping(
                    CounterpartyCategoryMapEntity(
                        counterparty = counterparty.trim().lowercase(),
                        categoryId = newCategoryId
                    )
                )
            }
        }
    }

    fun getPayeeDetailFlow(counterparty: String): Flow<PayeeDetailData> = flow {
        val zone = ZoneId.systemDefault()
        val allTx = transactionDao.getAllTransactionsSync().filter {
            !it.isDuplicate && (it.counterparty.equals(counterparty, ignoreCase = true) || it.displayName.equals(counterparty, ignoreCase = true))
        }

        val allWithCat = transactionDao.getAllTransactionsWithCategorySync().filter {
            !it.transaction.isDuplicate && (it.transaction.counterparty.equals(counterparty, ignoreCase = true) || it.transaction.displayName.equals(counterparty, ignoreCase = true))
        }

        val expenses = allTx.filter { it.type == "DEBIT" }
        val incomes = allTx.filter { it.type == "CREDIT" }

        val totalPaid = expenses.sumOf { it.amount }
        val paymentCount = expenses.size
        val avgAmount = if (paymentCount > 0) totalPaid / paymentCount else 0.0
        val largestPayment = expenses.maxOfOrNull { it.amount } ?: 0.0

        val firstPaid = expenses.minOfOrNull { it.timestamp }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val lastPaid = expenses.maxOfOrNull { it.timestamp }?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

        val frequencyLabel = if (expenses.size >= 2) {
            val timestamps = expenses.map { it.timestamp }.sorted()
            val gapsDays = timestamps.zipWithNext { a, b -> (b - a) / 86_400_000.0 }
            val medianGap = gapsDays.sorted()[gapsDays.size / 2]
            when {
                medianGap <= 1.5 -> "about every day"
                medianGap <= 7.0 -> "about every ${medianGap.toInt()} days"
                medianGap <= 14.0 -> "about every week"
                medianGap <= 35.0 -> "about once a month"
                else -> "about every ${(medianGap / 30.0).toInt()} months"
            }
        } else "—"

        val receivedFromThem = incomes.sumOf { it.amount }
        val netTotal = receivedFromThem - totalPaid

        val now = YearMonth.now()
        val monthlyBars = mutableListOf<PayeeMonthlyBar>()
        val dtf = DateTimeFormatter.ofPattern("MMM")
        for (i in 11 downTo 0) {
            val ym = now.minusMonths(i.toLong())
            val startMillis = InsightsPeriods.toStartOfDayMillis(ym.atDay(1), zone)
            val endMillis = InsightsPeriods.toEndOfDayMillis(ym.atEndOfMonth(), zone)
            val monthTotal = expenses.filter { it.timestamp in startMillis..endMillis }.sumOf { it.amount }
            monthlyBars.add(PayeeMonthlyBar(yearMonth = ym, label = ym.format(dtf), amount = monthTotal))
        }

        val displayName = allTx.firstOrNull()?.displayName ?: counterparty
        val category = allWithCat.firstOrNull()?.category

        emit(
            PayeeDetailData(
                displayName = displayName,
                rawCounterparty = counterparty,
                category = category,
                totalPaid = totalPaid,
                paymentCount = paymentCount,
                averagePerPayment = avgAmount,
                largestPayment = largestPayment,
                firstPaidDate = firstPaid,
                lastPaidDate = lastPaid,
                frequencyLabel = frequencyLabel,
                receivedFromThem = receivedFromThem,
                netTotal = netTotal,
                last12MonthsBars = monthlyBars,
                transactions = allWithCat.sortedByDescending { it.transaction.timestamp }
            )
        )
    }.flowOn(Dispatchers.IO)

    fun getCompareMonthsFlow(
        monthA: YearMonth,
        monthB: YearMonth,
        mode: InsightsMode,
        sameDaysOnly: Boolean
    ): Flow<CompareMonthsData> = flow {
        val zone = ZoneId.systemDefault()
        val txType = if (mode == InsightsMode.EXPENSE) "DEBIT" else "CREDIT"

        val today = LocalDate.now()
        val isAInProgress = (monthA == YearMonth.from(today))
        val isBInProgress = (monthB == YearMonth.from(today))

        val daysCountA = if (sameDaysOnly && isAInProgress) today.dayOfMonth else monthA.lengthOfMonth()
        val daysCountB = if (sameDaysOnly && isBInProgress) today.dayOfMonth else monthB.lengthOfMonth()
        val clampedDays = if (sameDaysOnly) minOf(daysCountA, daysCountB) else maxOf(daysCountA, daysCountB)

        val startA = InsightsPeriods.toStartOfDayMillis(monthA.atDay(1), zone)
        val endA = InsightsPeriods.toEndOfDayMillis(monthA.atDay(if (sameDaysOnly) clampedDays else monthA.lengthOfMonth()), zone)

        val startB = InsightsPeriods.toStartOfDayMillis(monthB.atDay(1), zone)
        val endB = InsightsPeriods.toEndOfDayMillis(monthB.atDay(if (sameDaysOnly) clampedDays else monthB.lengthOfMonth()), zone)

        val txA = transactionDao.getTransactionsBetweenSync(startA, endA).filter { !it.isDuplicate }
        val txB = transactionDao.getTransactionsBetweenSync(startB, endB).filter { !it.isDuplicate }

        val spentA = txA.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val incomeA = txA.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val netA = incomeA - spentA
        val countA = txA.size
        val avgA = if (clampedDays > 0) spentA / clampedDays else 0.0
        val largestA = txA.filter { it.type == "DEBIT" }.maxOfOrNull { it.amount } ?: 0.0

        val spentB = txB.filter { it.type == "DEBIT" }.sumOf { it.amount }
        val incomeB = txB.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val netB = incomeB - spentB
        val countB = txB.size
        val avgB = if (clampedDays > 0) spentB / clampedDays else 0.0
        val largestB = txB.filter { it.type == "DEBIT" }.maxOfOrNull { it.amount } ?: 0.0

        val metricsA = CompareMonthMetrics(spentA, incomeA, netA, countA, avgA, largestA)
        val metricsB = CompareMonthMetrics(spentB, incomeB, netB, countB, avgB, largestB)

        val cumA = mutableListOf<DaySpendPoint>()
        var accA = 0.0
        val targetTxA = txA.filter { it.type == txType }
        for (d in 1..clampedDays) {
            val dayStart = InsightsPeriods.toStartOfDayMillis(monthA.atDay(d), zone)
            val dayEnd = InsightsPeriods.toEndOfDayMillis(monthA.atDay(d), zone)
            val dayAmt = targetTxA.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.amount }
            accA += dayAmt
            cumA.add(DaySpendPoint(dayOfMonth = d, cumulative = accA, daily = dayAmt))
        }

        val cumB = mutableListOf<DaySpendPoint>()
        var accB = 0.0
        val targetTxB = txB.filter { it.type == txType }
        for (d in 1..clampedDays) {
            val dayStart = InsightsPeriods.toStartOfDayMillis(monthB.atDay(d), zone)
            val dayEnd = InsightsPeriods.toEndOfDayMillis(monthB.atDay(d), zone)
            val dayAmt = targetTxB.filter { it.timestamp in dayStart..dayEnd }.sumOf { it.amount }
            accB += dayAmt
            cumB.add(DaySpendPoint(dayOfMonth = d, cumulative = accB, daily = dayAmt))
        }

        val aggsA = transactionDao.getCategorySpendAggregates(txType, startA, endA)
        val aggsB = transactionDao.getCategorySpendAggregates(txType, startB, endB)
        val mapA = aggsA.associate { it.categoryId to it.totalAmount }
        val mapB = aggsB.associate { it.categoryId to it.totalAmount }
        val allCats = (mapA.keys + mapB.keys).distinct()

        val catEntities = categoryDao.getAllCategoriesSync().associateBy { it.id }

        val categoryItems = allCats.map { catId ->
            val a = mapA[catId] ?: 0.0
            val b = mapB[catId] ?: 0.0
            val delta = a - b
            val pct = if (b > 0) (delta / b) * 100.0 else null
            val entity = catEntities[catId]
            CompareCategoryItem(
                categoryId = catId,
                categoryName = entity?.name ?: "Uncategorized",
                categoryIconName = entity?.iconName ?: "category",
                amountA = a,
                amountB = b,
                deltaAmount = delta,
                deltaPercent = pct
            )
        }.sortedByDescending { abs(it.deltaAmount) }

        val totalDiff: Double = if (mode == InsightsMode.EXPENSE) spentA - spentB else incomeA - incomeB
        val topDiffCats = categoryItems.take(2).map { it.categoryName }
        val compWord = if (totalDiff >= 0) "higher" else "lower"
        val topCatsClause = if (topDiffCats.isNotEmpty()) ", mostly ${topDiffCats.joinToString(" and ")}" else ""
        val nameA = monthA.month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
        val nameB = monthB.month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
        val summarySentence = "$nameA is ${formatCurrency(abs(totalDiff))} $compWord than $nameB$topCatsClause."

        val allTx: List<TransactionEntity> = transactionDao.getAllTransactionsSync().filter { tx: TransactionEntity -> !tx.isDuplicate }
        val availableMonths: List<YearMonth> = allTx.map { tx: TransactionEntity ->
            val date = Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate()
            YearMonth.of(date.year, date.month)
        }.distinct().sortedDescending()

        emit(
            CompareMonthsData(
                monthA = monthA,
                monthB = monthB,
                availableMonths = if (availableMonths.isNotEmpty()) availableMonths else listOf(monthA, monthB),
                isSameDaysOnly = sameDaysOnly,
                clampedDayCount = clampedDays,
                metricsA = metricsA,
                metricsB = metricsB,
                dailyCumulativeA = cumA,
                dailyCumulativeB = cumB,
                categoryItems = categoryItems,
                summarySentence = summarySentence
            )
        )
    }.flowOn(Dispatchers.IO)

    fun getMonthlyRecapFlow(targetMonth: YearMonth): Flow<MonthlyRecapData> = flow {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val isSoFar = (targetMonth == YearMonth.from(today))

        val startMillis = InsightsPeriods.toStartOfDayMillis(targetMonth.atDay(1), zone)
        val endDay = if (isSoFar) today.dayOfMonth else targetMonth.lengthOfMonth()
        val endMillis = InsightsPeriods.toEndOfDayMillis(targetMonth.atDay(endDay), zone)

        val txs = transactionDao.getTransactionsBetweenSync(startMillis, endMillis).filter { !it.isDuplicate }
        val expenses = txs.filter { it.type == "DEBIT" }
        val incomes = txs.filter { it.type == "CREDIT" }

        val totalSpent = expenses.sumOf { it.amount }
        val totalIncome = incomes.sumOf { it.amount }
        val netAmount = totalIncome - totalSpent
        val savingsRate = if (totalIncome > 0) ((netAmount / totalIncome) * 100.0).coerceAtLeast(0.0) else null

        val prevMonth = targetMonth.minusMonths(1)
        val prevDays = if (isSoFar) minOf(today.dayOfMonth, prevMonth.lengthOfMonth()) else prevMonth.lengthOfMonth()
        val prevStart = InsightsPeriods.toStartOfDayMillis(prevMonth.atDay(1), zone)
        val prevEnd = InsightsPeriods.toEndOfDayMillis(prevMonth.atDay(prevDays), zone)
        val prevTxs = transactionDao.getTransactionsBetweenSync(prevStart, prevEnd).filter { !it.isDuplicate && it.type == "DEBIT" }
        val prevSpent = prevTxs.sumOf { it.amount }

        val deltaAmount: Double = totalSpent - prevSpent
        val deltaPercent: Double? = if (prevSpent > 0) (deltaAmount / prevSpent) * 100.0 else null

        val catAggs = transactionDao.getCategorySpendAggregates("DEBIT", startMillis, endMillis)
        val catEntities = categoryDao.getAllCategoriesSync().associateBy { it.id }
        val topCategories = catAggs.take(3).map { agg ->
            val entity = catEntities[agg.categoryId]
            val pct = if (totalSpent > 0) (agg.totalAmount / totalSpent) * 100.0 else 0.0
            RecapTopCategory(
                name = entity?.name ?: "Uncategorized",
                iconName = entity?.iconName ?: "category",
                percent = pct,
                amount = agg.totalAmount
            )
        }

        val payeeAggs = transactionDao.getTopPayeeAggregates("DEBIT", startMillis, endMillis, 1)
        val topPayee: String? = payeeAggs.firstOrNull()?.let { it.displayName ?: it.counterparty }

        val biggestExpense = expenses.maxByOrNull { it.amount }
        val biggestExpenseAmount: Double? = biggestExpense?.amount
        val biggestExpensePayee: String? = biggestExpense?.counterparty

        val spendDates: Set<LocalDate> = expenses.map { tx: TransactionEntity -> Instant.ofEpochMilli(tx.timestamp).atZone(zone).toLocalDate() }.toSet()
        val noSpendCount: Int = (1..endDay).count { d: Int -> !spendDates.contains(targetMonth.atDay(d)) }

        val monthKey = "%04d-%02d".format(targetMonth.year, targetMonth.monthValue)
        val budgetEntity = budgetDao.getBudgetForMonthSync(monthKey)
        val budgetLimit: Double? = budgetEntity?.monthlyLimit
        val isUnder: Boolean? = if (budgetLimit != null) (totalSpent <= budgetLimit) else null
        val budgetResultLabel: String? = if (budgetLimit != null) {
            val diff = abs(budgetLimit - totalSpent)
            if (isUnder == true) "Under budget by ${formatCurrency(diff)}" else "Over budget by ${formatCurrency(diff)}"
        } else null

        val dailyAggs = transactionDao.getDailySpendAggregates("DEBIT", startMillis, endMillis)
        val dailyMap: Map<String, Double> = dailyAggs.associate { it.dayString to it.totalAmount }
        val dailySpends: List<Double> = (1..endDay).map { d: Int ->
            val dateStr = "%04d-%02d-%02d".format(targetMonth.year, targetMonth.monthValue, d)
            dailyMap[dateStr] ?: 0.0
        }

        emit(
            MonthlyRecapData(
                yearMonth = targetMonth,
                isSoFar = isSoFar,
                totalSpent = totalSpent,
                deltaVsPreviousAmount = deltaAmount,
                deltaVsPreviousPercent = deltaPercent,
                topCategories = topCategories,
                topPayee = topPayee,
                biggestExpenseAmount = biggestExpenseAmount,
                biggestExpensePayee = biggestExpensePayee,
                noSpendDaysCount = noSpendCount,
                totalIncome = totalIncome,
                netAmount = netAmount,
                savingsRatePercent = savingsRate,
                budgetAmount = budgetLimit,
                budgetResultLabel = budgetResultLabel,
                isUnderBudget = isUnder,
                dailySpends = dailySpends
            )
        )
    }.flowOn(Dispatchers.IO)
}
