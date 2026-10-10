package com.omkarnub.kanri.ui.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omkarnub.kanri.data.analytics.Delta
import com.omkarnub.kanri.data.analytics.DeltaCalculator
import com.omkarnub.kanri.data.analytics.StreakCalculator
import com.omkarnub.kanri.data.analytics.StreakDataStore
import com.omkarnub.kanri.data.analytics.StreakState
import com.omkarnub.kanri.data.budget.BudgetCalculator
import com.omkarnub.kanri.data.db.BudgetEntity
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.omkarnub.kanri.data.greeting.GreetingDataStore
import com.omkarnub.kanri.ui.greeting.GreetingContext
import com.omkarnub.kanri.ui.greeting.GreetingResolver
import com.omkarnub.kanri.ui.greeting.GreetingResult
import com.omkarnub.kanri.ui.greeting.Tone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Immutable
data class BankAccountBalance(
    val bankName: String,
    val accountMask: String,
    val balance: Double?,
    val lastUpdated: Long
)

@Immutable
data class LendingDueItem(
    val personName: String,
    val amount: Double,
    val isLent: Boolean,
    val dueDate: Long?,
    val isOverdue: Boolean
)

@Immutable
data class GoalWidgetItem(
    val title: String,
    val progressPercent: Float,
    val currentAmount: Double,
    val targetAmount: Double,
    val emoji: String
)

@Immutable
data class HomeUiState(
    val transactions: List<TransactionWithCategory> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val todaySpent: Double = 0.0,
    val todayReceived: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBudget: Double = BudgetCalculator.DEFAULT_MONTHLY_BUDGET,
    val monthSpent: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthNetCashFlow: Double = 0.0,
    val monthBudgetRemaining: Double = BudgetCalculator.DEFAULT_MONTHLY_BUDGET,
    val safeToSpendPerDay: Double = 0.0,
    val budgetPercentRemaining: Float = 1.0f,
    val daysLeftInMonth: Int = 1,
    val currentMonthName: String = "",
    val isLoading: Boolean = false,
    // Feature 1: Needs-Review Badge & Queue
    val reviewCount: Int = 0,
    val reviewQueue: List<TransactionEntity> = emptyList(),
    // Feature 2: Month-over-Month Delta
    val monthDelta: Delta = Delta.NoData,
    // Feature 3: No-Spend Streak
    val streakState: StreakState = StreakState(),
    val lastCelebratedMilestone: Int = 0,
    val hasBudget: Boolean = false,
    // New: Accounts & Balances Carousel
    val bankAccounts: List<BankAccountBalance> = emptyList(),
    val totalLiquidBalance: Double = 0.0,
    // New: Lend & Borrow Quick Pulse
    val totalLentPending: Double = 0.0,
    val totalBorrowedPending: Double = 0.0,
    // Lending due alerts for widget
    val lendingDueItems: List<LendingDueItem> = emptyList(),
    // Active savings goals for widget
    val activeGoals: List<GoalWidgetItem> = emptyList(),
    // Feature: Financial Health Score & Statistics
    val financialHealth: com.omkarnub.kanri.data.analytics.FinancialHealthData =
        com.omkarnub.kanri.data.analytics.FinancialHealthCalculator.calculate(
            monthSpent = 0.0,
            monthIncome = 0.0,
            monthlyBudget = BudgetCalculator.DEFAULT_MONTHLY_BUDGET,
            daysRemaining = 1
        ),
    // Feature: Wallet Balances
    val walletBalances: com.omkarnub.kanri.data.wallet.WalletBalances = com.omkarnub.kanri.data.wallet.WalletBalances()
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = KanriDatabase.getDatabase(application)
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    private val budgetDao = db.budgetDao()
    private val lendingDao = db.lendingDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val streakDataStore = StreakDataStore(application)
    private val greetingDataStore = GreetingDataStore(application)
    val walletRepository = com.omkarnub.kanri.data.wallet.WalletRepository(application)

    private val currentMonthKey = BudgetCalculator.getCurrentMonthKey()

    // Feature 2: Selected month (Year, Month 1-indexed)
    private val _selectedMonth = MutableStateFlow<Pair<Int, Int>>(
        Calendar.getInstance().let { it.get(Calendar.YEAR) to (it.get(Calendar.MONTH) + 1) }
    )
    val selectedMonth: StateFlow<Pair<Int, Int>> = _selectedMonth.asStateFlow()

    fun selectMonth(year: Int, month: Int) {
        _selectedMonth.value = Pair(year, month)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthDelta: StateFlow<Delta> = _selectedMonth.flatMapLatest { (year, month) ->
        transactionDao.getTransactionsWithCategory().map { txWithCatList ->
            val txList = txWithCatList.map { it.transaction }
            DeltaCalculator.calculateDelta(year, month, txList)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Delta.NoData
    )

    // Feature 3: Midnight rollover flow
    private fun createMidnightRolloverFlow(): Flow<Long> = callbackFlow {
        trySend(System.currentTimeMillis())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(System.currentTimeMillis())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        getApplication<Application>().registerReceiver(receiver, filter)
        awaitClose {
            getApplication<Application>().unregisterReceiver(receiver)
        }
    }

    val streakState: StateFlow<StreakState> = combine(
        transactionDao.getTransactionsWithCategory(),
        createMidnightRolloverFlow()
    ) { txWithCatList, _ ->
        val txList = txWithCatList.map { it.transaction }
        StreakCalculator.calculateStreak(txList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StreakState()
    )

    val reviewCount: StateFlow<Int> = transactionDao.observeReviewCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val reviewQueue: StateFlow<List<TransactionEntity>> = transactionDao.observeReviewQueue()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<HomeUiState> = combine(
        transactionDao.getTransactionsWithCategory(),
        categoryDao.getAllCategories(),
        budgetDao.getBudgetForMonth(currentMonthKey),
        reviewCount,
        reviewQueue,
        monthDelta,
        streakState,
        streakDataStore.lastCelebratedMilestone,
        lendingDao.getTotalLentPending(),
        lendingDao.getTotalBorrowedPending(),
        walletRepository.observeBalances()
    ) { args: Array<Any?> ->
        val txList = args[0] as List<TransactionWithCategory>
        val catList = args[1] as List<CategoryEntity>
        val budgetEntity = args[2] as BudgetEntity?
        val count = args[3] as Int
        val queue = args[4] as List<TransactionEntity>
        val delta = args[5] as Delta
        val streak = args[6] as StreakState
        val lastCelebrated = args[7] as Int
        val lentPending = (args[8] as? Double) ?: 0.0
        val borrowedPending = (args[9] as? Double) ?: 0.0
        val balances = args[10] as? com.omkarnub.kanri.data.wallet.WalletBalances ?: com.omkarnub.kanri.data.wallet.WalletBalances()

        val startOfToday = getStartOfTodayMillis()
        val startOfMonth = BudgetCalculator.getStartOfMonthMillis()
        val daysLeft = BudgetCalculator.getDaysRemainingInMonth()
        val monthName = BudgetCalculator.getCurrentMonthDisplayName()

        var spentToday = 0.0
        var receivedToday = 0.0
        var spentMonth = 0.0
        var incomeMonth = 0.0

        for (item in txList) {
            val tx = item.transaction
            // Transfers do not affect spending or income
            if (tx.isTransfer) continue

            if (tx.timestamp >= startOfToday) {
                if (tx.type.equals("DEBIT", ignoreCase = true)) {
                    spentToday += tx.amount
                } else if (tx.type.equals("CREDIT", ignoreCase = true)) {
                    receivedToday += tx.amount
                }
            }
            if (tx.timestamp >= startOfMonth) {
                if (tx.type.equals("DEBIT", ignoreCase = true)) {
                    spentMonth += tx.amount
                } else if (tx.type.equals("CREDIT", ignoreCase = true)) {
                    incomeMonth += tx.amount
                }
            }
        }

        val budgetLimit = budgetEntity?.monthlyLimit ?: BudgetCalculator.DEFAULT_MONTHLY_BUDGET
        val remaining = BudgetCalculator.calculateRemaining(budgetLimit, spentMonth)
        val safePerDay = BudgetCalculator.calculateSafeToSpendPerDay(budgetLimit, spentMonth, daysLeft)
        val percentRemaining = BudgetCalculator.calculatePercentRemaining(budgetLimit, spentMonth)

        // Parse unique bank accounts & latest balances
        val bankMap = mutableMapOf<String, BankAccountBalance>()
        val sortedTx = txList.sortedByDescending { it.transaction.timestamp }
        for (item in sortedTx) {
            val tx = item.transaction
            val bankName = tx.bank ?: continue
            val accountMask = extractAccountMask(tx.rawSms) ?: "••Acct"
            val key = "$bankName-$accountMask"
            if (!bankMap.containsKey(key)) {
                val balance = extractBalance(tx.rawSms)
                bankMap[key] = BankAccountBalance(
                    bankName = bankName,
                    accountMask = accountMask,
                    balance = balance,
                    lastUpdated = tx.timestamp
                )
            } else if (bankMap[key]?.balance == null) {
                val balance = extractBalance(tx.rawSms)
                if (balance != null) {
                    bankMap[key] = bankMap[key]!!.copy(balance = balance)
                }
            }
        }
        val bankAccounts = bankMap.values.toList()
        val totalLiquid = bankAccounts.mapNotNull { it.balance }.sum()

        // Lending due items for widget (overdue + due within 3 days)
        val lendingDueItems = try {
            val allLending = db.lendingDao().getAllRecordsWithRepaymentsSync()
            val threeDaysLater = startOfToday + 3 * 86_400_000L
            allLending
                .filter { !it.lending.isSettled && it.lending.dueDate != null }
                .filter { it.lending.dueDate!! < threeDaysLater }
                .sortedBy { it.lending.dueDate }
                .map { entry ->
                    LendingDueItem(
                        personName = entry.lending.personName,
                        amount = entry.outstanding,
                        isLent = entry.lending.type.equals("LENT", ignoreCase = true),
                        dueDate = entry.lending.dueDate,
                        isOverdue = entry.lending.dueDate!! < startOfToday
                    )
                }
        } catch (_: Exception) { emptyList() }

        // Active savings goals for widget
        val activeGoals = try {
            savingsGoalDao.getActiveGoalsSync().map { goal ->
                val percent = if (goal.targetAmount > 0) {
                    ((goal.currentAmount / goal.targetAmount) * 100).toFloat().coerceIn(0f, 100f)
                } else 0f
                GoalWidgetItem(
                    title = goal.title,
                    progressPercent = percent,
                    currentAmount = goal.currentAmount,
                    targetAmount = goal.targetAmount,
                    emoji = goal.emoji
                )
            }
        } catch (_: Exception) { emptyList() }

        HomeUiState(
            transactions = txList,
            categories = catList,
            todaySpent = spentToday,
            todayReceived = receivedToday,
            netBalance = receivedToday - spentToday,
            monthlyBudget = budgetLimit,
            monthSpent = spentMonth,
            monthIncome = incomeMonth,
            monthNetCashFlow = incomeMonth - spentMonth,
            monthBudgetRemaining = remaining,
            safeToSpendPerDay = safePerDay,
            budgetPercentRemaining = percentRemaining,
            daysLeftInMonth = daysLeft,
            currentMonthName = monthName,
            isLoading = false,
            reviewCount = count,
            reviewQueue = queue,
            monthDelta = delta,
            streakState = streak,
            lastCelebratedMilestone = lastCelebrated,
            hasBudget = budgetEntity != null,
            bankAccounts = bankAccounts,
            totalLiquidBalance = totalLiquid,
            totalLentPending = lentPending,
            totalBorrowedPending = borrowedPending,
            lendingDueItems = lendingDueItems,
            activeGoals = activeGoals,
            financialHealth = com.omkarnub.kanri.data.analytics.FinancialHealthCalculator.calculate(
                monthSpent = spentMonth,
                monthIncome = incomeMonth,
                monthlyBudget = budgetLimit,
                daysRemaining = daysLeft,
                totalBorrowed = borrowedPending,
                totalLent = lentPending
            ),
            walletBalances = balances
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    private fun calculateLastMonthSpentSameDay(
        transactions: List<TransactionEntity>,
        nowCal: Calendar = Calendar.getInstance()
    ): Double? {
        val currentYear = nowCal.get(Calendar.YEAR)
        val currentMonth = nowCal.get(Calendar.MONTH) // 0-based
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)

        val prevCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val prevYear = prevCal.get(Calendar.YEAR)
        val prevMonth = prevCal.get(Calendar.MONTH)
        val maxDayPrev = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val clampedDay = currentDay.coerceAtMost(maxDayPrev)

        val startPrev = Calendar.getInstance().apply {
            set(Calendar.YEAR, prevYear)
            set(Calendar.MONTH, prevMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endPrev = Calendar.getInstance().apply {
            set(Calendar.YEAR, prevYear)
            set(Calendar.MONTH, prevMonth)
            set(Calendar.DAY_OF_MONTH, clampedDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val prevSpent = transactions
            .filter { !it.isTransfer && it.type.equals("DEBIT", ignoreCase = true) && it.timestamp in startPrev..endPrev }
            .sumOf { it.amount }

        return if (prevSpent > 0.0) prevSpent else null
    }

    private fun createTimeBucketFlow(): Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(30_000L) // Recomputes on bucket change and at midnight
        }
    }

    val greeting: StateFlow<GreetingResult> = combine(
        uiState,
        greetingDataStore.antiRepetitionState,
        createTimeBucketFlow(),
        createMidnightRolloverFlow(),
        com.omkarnub.kanri.data.profile.UserProfilePreferences.getInstance(getApplication()).userNameFlow
    ) { state, antiRep, now, _, currentUserName ->
        val localDate = now.toLocalDate()
        val epochDay = localDate.toEpochDay()
        val bucket = GreetingResolver.getTimeBucket(now.toLocalTime())
        val seed = GreetingResolver.computeSeed(epochDay, bucket)

        val lastMonthSpent = calculateLastMonthSpentSameDay(state.transactions.map { it.transaction })

        val context = GreetingContext(
            userName = currentUserName,
            now = now,
            dayOfWeek = now.dayOfWeek,
            dayOfMonth = now.dayOfMonth,
            daysLeftInMonth = state.daysLeftInMonth,
            monthlyBudget = if (state.hasBudget) state.monthlyBudget else null,
            spentThisMonth = state.monthSpent,
            spentToday = state.todaySpent,
            safeToSpendPerDay = if (state.safeToSpendPerDay.isFinite() && state.safeToSpendPerDay > 0) state.safeToSpendPerDay else null,
            lastMonthSpentSameDay = lastMonthSpent,
            streakDays = state.streakState.currentStreak,
            needsReviewCount = state.reviewCount,
            isFirstLaunchToday = antiRep.lastShownEpochDay != epochDay,
            seed = seed,
            lastSubtitleRuleId = antiRep.lastSubtitleRuleId,
            lastShownEpochDay = antiRep.lastShownEpochDay,
            lastWarningBucket = antiRep.lastWarningBucket,
            lastWarningEpochDay = antiRep.lastWarningEpochDay
        )

        GreetingResolver.resolve(context)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GreetingResult(
            greeting = "Good day, Alex",
            subtitle = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
            tone = Tone.Neutral,
            ruleId = 11
        )
    )

    fun onGreetingDisplayed(result: GreetingResult) {
        viewModelScope.launch {
            val now = LocalDateTime.now()
            val epochDay = now.toLocalDate().toEpochDay()
            val bucket = GreetingResolver.getTimeBucket(now.toLocalTime())
            greetingDataStore.recordShownRule(
                ruleId = result.ruleId,
                epochDay = epochDay,
                isWarning = result.tone == Tone.Warning,
                bucket = bucket.name
            )
        }
    }



    fun confirmReview(transactionId: Long) {
        viewModelScope.launch {
            transactionDao.clearReviewFlag(transactionId)
        }
    }

    fun confirmAllReviews() {
        viewModelScope.launch {
            transactionDao.clearAllReviewFlags()
        }
    }

    fun setLastCelebratedMilestone(milestone: Int) {
        viewModelScope.launch {
            streakDataStore.setLastCelebratedMilestone(milestone)
        }
    }

    fun assignCategory(
        transactionId: Long,
        counterparty: String?,
        categoryId: Long,
        note: String? = null,
        bulkUpdate: Boolean = false
    ) {
        viewModelScope.launch {
            // 1. Update this specific transaction
            transactionDao.updateCategoryAndNotes(transactionId, categoryId, note)
            com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper.onTransactionCategoryChanged(transactionId, categoryId, db, getApplication())

            // 2. Only bulk-update if explicitly requested AND it's a specific, non-generic merchant
            if (bulkUpdate && isSpecificMerchant(counterparty)) {
                val clean = counterparty!!.trim()
                categoryDao.setMapping(CounterpartyCategoryMapEntity(clean, categoryId))
                transactionDao.updateCategoryForCounterparty(clean, categoryId)
            }
        }
    }

    private fun isSpecificMerchant(counterparty: String?): Boolean {
        if (counterparty.isNullOrBlank()) return false
        val genericNames = setOf(
            "unknown", "unknown_merchant", "unknown merchant", "unknown payee",
            "upi", "card", "atm", "bank_transfer", "bank transfer",
            "cash", "transaction", "debit", "credit", "other", "payment"
        )
        return counterparty.trim().lowercase() !in genericNames
    }

    fun updateMonthlyBudget(newLimit: Double) {
        viewModelScope.launch {
            budgetDao.setBudget(
                BudgetEntity(
                    monthKey = currentMonthKey,
                    monthlyLimit = newLimit
                )
            )
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun addManualTransaction(
        amount: Double,
        type: String,
        counterparty: String,
        sourceType: String,
        categoryId: Long? = null,
        walletChoice: String? = null
    ) {
        viewModelScope.launch {
            val resolvedCategoryId = categoryId ?: if (!counterparty.isBlank()) {
                (categoryDao.findSmartRuleForCounterparty(counterparty.trim())
                    ?: categoryDao.getMappingForCounterparty(counterparty.trim()))?.categoryId
            } else null

            val atmMode = com.omkarnub.kanri.data.wallet.WalletPreferences.getInstance(getApplication()).atmWithdrawalMode
            val resolvedWallet = com.omkarnub.kanri.data.wallet.WalletResolver.resolve(
                sourceType = sourceType,
                type = type,
                userChoice = walletChoice,
                atmMode = atmMode
            )

            val entity = TransactionEntity(
                type = type.uppercase(),
                amount = amount,
                sourceType = sourceType.uppercase(),
                counterparty = counterparty.trim(),
                displayName = counterparty.trim(),
                bank = if (sourceType.equals("CASH", ignoreCase = true)) "Cash" else null,
                refNo = null,
                timestamp = System.currentTimeMillis(),
                categoryId = resolvedCategoryId,
                rawSms = "Manual entry: $counterparty",
                isManualEntry = true,
                wallet = resolvedWallet.wallet,
                transferToWallet = resolvedWallet.transferToWallet
            )
            transactionDao.insert(entity)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun setupWallets(
        cash: Double,
        online: Double,
        atmMode: com.omkarnub.kanri.data.wallet.AtmWithdrawalMode = com.omkarnub.kanri.data.wallet.AtmWithdrawalMode.TRANSFER
    ) {
        viewModelScope.launch {
            walletRepository.setup(cash, online, atmMode)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun correctWalletBalance(wallet: String, desiredBalance: Double) {
        viewModelScope.launch {
            walletRepository.correct(wallet, desiredBalance)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun moveMoney(
        fromWallet: String,
        toWallet: String,
        amount: Double,
        timestamp: Long = System.currentTimeMillis(),
        note: String? = null
    ) {
        viewModelScope.launch {
            walletRepository.moveMoney(fromWallet, toWallet, amount, timestamp, note)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun editOpeningBalances(cash: Double, online: Double) {
        viewModelScope.launch {
            walletRepository.setup(cash, online)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun convertToTransfer(transactionId: Long, wallet: String, transferToWallet: String) {
        viewModelScope.launch {
            walletRepository.convertToTransfer(transactionId, wallet, transferToWallet)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun updateTransactionWallet(transactionId: Long, wallet: String) {
        viewModelScope.launch {
            transactionDao.updateWallet(transactionId, wallet.uppercase().trim())
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    suspend fun getBalanceAfter(wallet: String, transaction: TransactionEntity): Double? {
        return walletRepository.getBalanceAfter(wallet, transaction)
    }

    fun observeTransactionWithCategory(id: Long): Flow<TransactionWithCategory?> {
        return transactionDao.observeTransactionWithCategory(id)
    }

    fun updateTransaction(
        transactionId: Long,
        amount: Double,
        type: String,
        counterparty: String?,
        displayName: String?,
        categoryId: Long?,
        sourceType: String,
        bank: String?,
        refNo: String?,
        notes: String?,
        timestamp: Long,
        wallet: String? = null,
        transferToWallet: String? = null
    ) {
        viewModelScope.launch {
            val existing = transactionDao.getTransactionById(transactionId) ?: return@launch
            val updated = existing.copy(
                amount = amount,
                type = type.uppercase(),
                counterparty = counterparty?.trim(),
                displayName = displayName?.trim() ?: counterparty?.trim(),
                categoryId = categoryId,
                sourceType = sourceType.uppercase(),
                bank = bank?.trim(),
                refNo = refNo?.trim(),
                notes = notes?.trim(),
                timestamp = timestamp,
                wallet = wallet ?: existing.wallet,
                transferToWallet = transferToWallet ?: existing.transferToWallet
            )
            transactionDao.insert(updated)

            if (categoryId != null && categoryId != existing.categoryId) {
                com.omkarnub.kanri.data.lending.LendingTransactionSyncHelper.onTransactionCategoryChanged(
                    transactionId, categoryId, db, getApplication()
                )
            }
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteTransactionById(transactionId: Long) {
        viewModelScope.launch {
            transactionDao.deleteById(transactionId)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    fun clearReviewFlag(transactionId: Long) {
        viewModelScope.launch {
            transactionDao.clearReviewFlag(transactionId)
            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(getApplication())
        }
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    companion object {
        fun extractBalance(rawSms: String): Double? {
            return com.omkarnub.kanri.data.parser.SmsParser.extractBalance(rawSms)
        }

        fun extractAccountMask(rawSms: String): String? {
            return com.omkarnub.kanri.data.parser.SmsParser.extractAccount(rawSms)
        }
    }
}

