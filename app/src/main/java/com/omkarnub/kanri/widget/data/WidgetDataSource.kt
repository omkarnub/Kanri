package com.omkarnub.kanri.widget.data

import android.content.Context
import com.omkarnub.kanri.data.analytics.StreakCalculator
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.widget.core.ClimateStatus
import com.omkarnub.kanri.widget.core.PacingStatus
import com.omkarnub.kanri.widget.core.TodayWidgetState
import com.omkarnub.kanri.widget.core.BudgetWidgetState
import com.omkarnub.kanri.widget.core.WalletsWidgetState
import com.omkarnub.kanri.widget.core.GoalsWidgetState
import com.omkarnub.kanri.widget.core.DuesWidgetState
import com.omkarnub.kanri.widget.core.SkyWidgetState
import com.omkarnub.kanri.widget.core.HeatmapWidgetState
import com.omkarnub.kanri.widget.core.StreakWidgetState
import com.omkarnub.kanri.widget.core.SafeToSpendWidgetState
import com.omkarnub.kanri.widget.core.ReviewInboxWidgetState
import com.omkarnub.kanri.widget.core.MonthDeltaWidgetState
import com.omkarnub.kanri.widget.core.QuickActionsWidgetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Consolidated data source powering all 12 Kanri Pixel DotTiles widgets via Room queries.
 * Single source of truth for widget data computation.
 */
object WidgetDataSource {

    // Reference epoch for astronomical new moon: Jan 6, 2000, 18:14 UTC
    const val NEW_MOON_REF_MILLIS = 947182440000L
    const val SYNODIC_MONTH_DAYS = 29.53058867

    suspend fun getTodayState(context: Context): TodayWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)

        val startOfDay = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val todayTxs = db.transactionDao().getTransactionsBetweenSync(startOfDay, endOfDay)
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }

        val spentToday = todayTxs.sumOf { it.amount }
        val expenseCount = todayTxs.size

        // Hourly spend breakdown across 24 hours
        val hourlyBuckets = DoubleArray(24) { 0.0 }
        val itemCal = Calendar.getInstance()
        for (tx in todayTxs) {
            itemCal.timeInMillis = tx.timestamp
            val hour = itemCal.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
            hourlyBuckets[hour] += tx.amount
        }

        val maxHourly = hourlyBuckets.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val hourlySpend = hourlyBuckets.map { amt ->
            if (amt <= 0.0) null else (amt / maxHourly).toFloat().coerceIn(0.1f, 1.0f)
        }

        // Pacing: compare with daily average
        val thirtyDaysAgo = startOfDay - (30L * 86400000L)
        val pastTxs = db.transactionDao().getTransactionsBetweenSync(thirtyDaysAgo, startOfDay)
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }
        val avgDailySpend = if (pastTxs.isNotEmpty()) pastTxs.sumOf { it.amount } / 30.0 else 1000.0
        val pacingRatio = spentToday / max(avgDailySpend, 1.0)

        val pacing = when {
            pacingRatio > 1.35 -> PacingStatus.OVER
            pacingRatio > 0.95 -> PacingStatus.WARNING
            else -> PacingStatus.HEALTHY
        }

        TodayWidgetState(
            spentToday = spentToday,
            expenseCount = expenseCount,
            hourlySpend = hourlySpend,
            currentHour = currentHour,
            pacingStatus = pacing,
            accessibleDescription = "Spent today: ${spentToday.toInt()} rupees across $expenseCount expenses"
        )
    }

    suspend fun getBudgetState(context: Context): BudgetWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val cal = Calendar.getInstance()
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val budgetEntity = db.budgetDao().getBudgetForMonthSync(monthKey)

        if (budgetEntity == null || budgetEntity.monthlyLimit <= 0.0) {
            return@withContext BudgetWidgetState(
                hasBudget = false,
                accessibleDescription = "No budget configured for this month"
            )
        }

        val totalBudget = budgetEntity.monthlyLimit
        val startOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val monthTxs = db.transactionDao().getTransactionsBetweenSync(startOfMonth, endOfMonth)
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }

        val spentAmount = monthTxs.sumOf { it.amount }
        val remainingAmount = totalBudget - spentAmount
        val percentUsed = (spentAmount / totalBudget).toFloat()

        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysLeft = max(0, maxDays - currentDay)
        val pacingExpectedRatio = (currentDay.toFloat() / maxDays).coerceIn(0f, 1f)

        BudgetWidgetState(
            hasBudget = true,
            remainingAmount = remainingAmount,
            spentAmount = spentAmount,
            totalBudget = totalBudget,
            percentUsed = percentUsed,
            daysLeft = daysLeft,
            pacingExpectedRatio = pacingExpectedRatio,
            accessibleDescription = "Budget: ${remainingAmount.toInt()} rupees remaining, ${(percentUsed * 100).toInt()}% used, $daysLeft days left"
        )
    }

    suspend fun getWalletsState(context: Context): WalletsWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val openings = db.walletDao().getBalancesSync().associateBy { it.walletId.uppercase() }
        val txs = db.transactionDao().getWalletTransactionsSync()
        val balances = WalletBalanceCalculator.balances(openings, txs)

        val cash = balances.cash
        val bank = balances.online
        val total = balances.total
        val totalPositive = max(1.0, (if (cash > 0) cash else 0.0) + (if (bank > 0) bank else 0.0))
        val cashRatio = ((if (cash > 0) cash else 0.0) / totalPositive).toFloat().coerceIn(0f, 1f)

        WalletsWidgetState(
            cashBalance = cash,
            bankBalance = bank,
            netWorth = total,
            cashProportion = cashRatio,
            accessibleDescription = "Wallets: Cash ${cash.toInt()} rupees, Bank ${bank.toInt()} rupees, Net Worth ${total.toInt()} rupees"
        )
    }

    suspend fun getGoalsState(context: Context): GoalsWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val goals = db.savingsGoalDao().getActiveGoalsSync()

        if (goals.isEmpty()) {
            return@withContext GoalsWidgetState(
                hasGoal = false,
                accessibleDescription = "No active savings goal set"
            )
        }

        val primary = goals.first()
        val saved = primary.currentAmount
        val target = max(1.0, primary.targetAmount)
        val ratio = (saved / target).toFloat()
        val isDone = ratio >= 1f

        GoalsWidgetState(
            hasGoal = true,
            goalName = primary.title,
            savedAmount = saved,
            targetAmount = target,
            percentSaved = ratio.coerceIn(0f, 1f),
            isCompleted = isDone,
            accessibleDescription = "Goal ${primary.title}: ${saved.toInt()} of ${target.toInt()} saved (${(ratio * 100).toInt()}%)"
        )
    }

    suspend fun getDuesState(context: Context): DuesWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val records = db.lendingDao().getAllRecordsSync().filter { !it.isSettled }

        var receivable = 0.0
        var payable = 0.0
        var overdue = 0
        val now = System.currentTimeMillis()

        for (item in records) {
            val rem = item.amount
            if (rem <= 0) continue

            if (item.type.equals("LENT", ignoreCase = true)) {
                receivable += rem
            } else {
                payable += rem
            }

            if (item.dueDate != null && item.dueDate < now) {
                overdue++
            }
        }

        val totalDues = max(1.0, receivable + payable)
        val recRatio = (receivable / totalDues).toFloat().coerceIn(0f, 1f)
        val payRatio = (payable / totalDues).toFloat().coerceIn(0f, 1f)
        val net = receivable - payable

        DuesWidgetState(
            receivableAmount = receivable,
            payableAmount = payable,
            netAmount = net,
            overdueCount = overdue,
            receivableRatio = recRatio,
            payableRatio = payRatio,
            accessibleDescription = "Dues: Receivable ${receivable.toInt()}, Payable ${payable.toInt()}, Net ${net.toInt()}, $overdue overdue"
        )
    }

    suspend fun getSkyState(context: Context): SkyWidgetState = withContext(Dispatchers.IO) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeFrac = (hour * 60 + minute) / 1440f

        // Day is approx 6:00 (0.25) to 18:00 (0.75)
        val isDay = timeFrac in 0.25f..0.75f
        val sunProgress = if (isDay) {
            (timeFrac - 0.25f) / 0.50f
        } else {
            if (timeFrac > 0.75f) (timeFrac - 0.75f) / 0.50f else (timeFrac + 0.25f) / 0.50f
        }

        // Astronomical moon phase (synodic calculation)
        val daysSinceRef = (System.currentTimeMillis() - NEW_MOON_REF_MILLIS) / 86400000.0
        val moonPhase = (((daysSinceRef % SYNODIC_MONTH_DAYS) / SYNODIC_MONTH_DAYS + 1.0) % 1.0).toFloat()

        // Pacing & Climate
        val todayState = getTodayState(context)
        val safeState = getSafeToSpendState(context)
        val todaySpent = todayState.spentToday
        val safeToSpend = safeState.dailyAllowance

        val climate = when {
            todaySpent > safeToSpend * 1.25 -> ClimateStatus.STORM
            todaySpent > safeToSpend * 0.95 -> ClimateStatus.CLOUDY
            cal.get(Calendar.DAY_OF_MONTH) <= 3 -> ClimateStatus.SUNRISE
            !isDay -> ClimateStatus.STARRY
            else -> ClimateStatus.CLEAR
        }

        // 7-day pulse
        val pulse = FloatArray(7)
        val db = KanriDatabase.getDatabase(context)
        val todayStart = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (i in 0 until 7) {
            val dStart = todayStart - ((6 - i) * 86400000L)
            val dEnd = dStart + 86399999L
            val dayTxs = db.transactionDao().getTransactionsBetweenSync(dStart, dEnd)
                .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }
            val dayTotal = dayTxs.sumOf { it.amount }
            pulse[i] = (dayTotal / max(safeToSpend * 1.5, 100.0)).toFloat().coerceIn(0.05f, 1f)
        }

        SkyWidgetState(
            sunProgress = sunProgress.coerceIn(0f, 1f),
            isDay = isDay,
            moonPhaseFrac = moonPhase,
            climateStatus = climate,
            todaySpent = todaySpent,
            safeToSpend = safeToSpend,
            sevenDayPulse = pulse,
            accessibleDescription = "Financial Sky: ${climate.name}, spent ${todaySpent.toInt()}, safe ${safeToSpend.toInt()}"
        )
    }

    suspend fun getHeatmapState(context: Context): HeatmapWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val cal = Calendar.getInstance()
        val monthName = SimpleDateFormat("MMM", Locale.US).format(cal.time)

        val firstDayCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonth = firstDayCal.timeInMillis
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // Day of week offset (Mon = 0 ... Sun = 6)
        val dow = firstDayCal.get(Calendar.DAY_OF_WEEK)
        val startOffset = (dow - Calendar.MONDAY + 7) % 7

        val endOfMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, daysInMonth)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val monthTxs = db.transactionDao().getTransactionsBetweenSync(startOfMonth, endOfMonth)
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }

        val monthTotal = monthTxs.sumOf { it.amount }

        // Day spend map
        val dailyMap = DoubleArray(daysInMonth + 1) { 0.0 }
        val itemCal = Calendar.getInstance()
        for (tx in monthTxs) {
            itemCal.timeInMillis = tx.timestamp
            val day = itemCal.get(Calendar.DAY_OF_MONTH).coerceIn(1, daysInMonth)
            dailyMap[day] += tx.amount
        }

        // Percentile quantization into 5 levels (0.0, 0.25, 0.5, 0.75, 1.0)
        val nonZeroSpends = dailyMap.filter { it > 0.0 }.sorted()
        val p25 = if (nonZeroSpends.isNotEmpty()) nonZeroSpends[(nonZeroSpends.size * 0.25).toInt()] else 100.0
        val p50 = if (nonZeroSpends.isNotEmpty()) nonZeroSpends[(nonZeroSpends.size * 0.50).toInt()] else 300.0
        val p75 = if (nonZeroSpends.isNotEmpty()) nonZeroSpends[(nonZeroSpends.size * 0.75).toInt()] else 800.0

        val grid = FloatArray(35) { 0f }
        for (d in 1..daysInMonth) {
            val cellIdx = startOffset + d - 1
            if (cellIdx in 0..34) {
                val amt = dailyMap[d]
                grid[cellIdx] = when {
                    amt <= 0.0 -> 0f
                    amt < p25 -> 0.25f
                    amt < p50 -> 0.50f
                    amt < p75 -> 0.75f
                    else -> 1.0f
                }
            }
        }

        val todayIdx = startOffset + currentDay - 1
        val maxActive = startOffset + daysInMonth - 1

        HeatmapWidgetState(
            monthName = monthName,
            monthTotal = monthTotal,
            dailyIntensities = grid,
            todayCellIndex = if (todayIdx in 0..34) todayIdx else -1,
            maxActiveCellIndex = if (maxActive in 0..34) maxActive else 34,
            accessibleDescription = "Heatmap for $monthName: Total spend ${monthTotal.toInt()} rupees"
        )
    }

    suspend fun getStreakState(context: Context): StreakWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val allTxs = db.transactionDao().getAllTransactionsSync()
        val streakState = StreakCalculator.calculateStreak(allTxs)

        // Generate last 30 days boolean array (true = no spend)
        val cal = Calendar.getInstance()
        val startOfToday = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val thirtyDaysAgo = startOfToday - (29L * 86400000L)
        val debitTxs = allTxs.filter {
            it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate && it.timestamp >= thirtyDaysAgo
        }

        val spendDays = HashSet<Int>()
        val itemCal = Calendar.getInstance()
        for (tx in debitTxs) {
            val diffDays = ((tx.timestamp - thirtyDaysAgo) / 86400000L).toInt()
            if (diffDays in 0..29) {
                spendDays.add(diffDays)
            }
        }

        val last30 = BooleanArray(30) { dayIdx ->
            !spendDays.contains(dayIdx)
        }

        StreakWidgetState(
            currentStreak = streakState.currentStreak,
            bestStreak = streakState.bestStreak,
            last30Days = last30,
            todayIndex = 29,
            accessibleDescription = "No-spend streak: ${streakState.currentStreak} days, best: ${streakState.bestStreak} days"
        )
    }

    suspend fun getSafeToSpendState(context: Context): SafeToSpendWidgetState = withContext(Dispatchers.IO) {
        val budgetState = getBudgetState(context)
        val todayState = getTodayState(context)
        val cal = Calendar.getInstance()
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysRemainingInclToday = max(1, daysInMonth - currentDay + 1)

        val allowance = if (budgetState.hasBudget && budgetState.remainingAmount > 0) {
            budgetState.remainingAmount / daysRemainingInclToday
        } else {
            // Fallback: 30-day daily average
            val db = KanriDatabase.getDatabase(context)
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 86400000L)
            val txs = db.transactionDao().getTransactionsBetweenSync(thirtyDaysAgo, System.currentTimeMillis())
                .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }
            if (txs.isNotEmpty()) txs.sumOf { it.amount } / 30.0 else 1000.0
        }

        val ratio = (todayState.spentToday / max(allowance, 1.0)).toFloat()
        val isOver = todayState.spentToday > allowance

        SafeToSpendWidgetState(
            dailyAllowance = allowance,
            todaySpent = todayState.spentToday,
            spentRatio = ratio,
            isOverAllowance = isOver,
            hasBudget = budgetState.hasBudget,
            accessibleDescription = "Safe to spend: ${allowance.toInt()} rupees per day. Today spent: ${todayState.spentToday.toInt()}"
        )
    }

    suspend fun getReviewInboxState(context: Context): ReviewInboxWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val unreviewedTxs = db.transactionDao().getAllTransactionsSync().filter {
            (it.needsReview || it.categoryId == null) && it.transferToWallet == null && !it.isDuplicate
        }

        val count = unreviewedTxs.size
        val totalAmount = unreviewedTxs.sumOf { it.amount }

        ReviewInboxWidgetState(
            pendingCount = count,
            pendingTotalAmount = totalAmount,
            accessibleDescription = "Review Inbox: $count transactions awaiting review"
        )
    }

    suspend fun getMonthDeltaState(context: Context): MonthDeltaWidgetState = withContext(Dispatchers.IO) {
        val db = KanriDatabase.getDatabase(context)
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // This month: 1st to today
        val startOfThisMonth = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val curTxs = db.transactionDao().getTransactionsBetweenSync(startOfThisMonth, System.currentTimeMillis())
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }
        val curSpend = curTxs.sumOf { it.amount }

        // Last month: 1st to same day (clamped to max days of last month)
        val lastMonthCal = (cal.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        val maxDaysLastMonth = lastMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val compareDay = currentDay.coerceAtMost(maxDaysLastMonth)

        val startOfLastMonth = (lastMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfLastMonthCompare = (lastMonthCal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, compareDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val prevTxs = db.transactionDao().getTransactionsBetweenSync(startOfLastMonth, endOfLastMonthCompare)
            .filter { it.type.equals("DEBIT", ignoreCase = true) && !it.isTransfer && !it.isDuplicate }
        val prevSpend = prevTxs.sumOf { it.amount }

        val diff = curSpend - prevSpend
        val isSpendingMore = diff > 0
        val base = max(prevSpend, 1.0)
        val pct = (abs(diff) / base * 100.0).toFloat().coerceIn(0f, 999f)

        MonthDeltaWidgetState(
            deltaPercent = pct,
            isSpendingMore = isSpendingMore,
            currentMonthSpendToDate = curSpend,
            previousMonthSpendToDate = prevSpend,
            dayOfMonth = currentDay,
            accessibleDescription = "Vs last month: ${if (isSpendingMore) "Up" else "Down"} ${pct.toInt()}% through day $currentDay"
        )
    }

    fun getQuickActionsState(): QuickActionsWidgetState {
        return QuickActionsWidgetState()
    }
}
