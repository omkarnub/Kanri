package com.omkarnub.kanri.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.data.wallet.WalletBalances
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.widget.BudgetProgressWidgetPreview
import com.omkarnub.kanri.ui.widget.BudgetRingWidgetPreview
import com.omkarnub.kanri.ui.widget.FinancialDayRecord
import com.omkarnub.kanri.ui.widget.FinancialWeatherWidgetPreview
import com.omkarnub.kanri.ui.widget.GoalsRingWidgetPreview
import com.omkarnub.kanri.ui.widget.LendBorrowWidgetPreview
import com.omkarnub.kanri.ui.widget.QuickAddWidgetPreview
import com.omkarnub.kanri.ui.widget.SpentTodayWidgetPreview
import com.omkarnub.kanri.ui.widget.SplitBillWidgetPreview
import com.omkarnub.kanri.ui.widget.WalletBalanceWidgetPreview
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import com.omkarnub.kanri.widget.WidgetPreferences
import com.omkarnub.kanri.widget.WidgetStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val widgetPrefs = remember { WidgetPreferences.getInstance(context) }
    val savedStyle by widgetPrefs.widgetStyleFlow.collectAsState()
    var selectedStyle by remember(savedStyle) { mutableStateOf(savedStyle) }

    var showAppliedBanner by remember { mutableStateOf(false) }

    // Live financial data states for 1:1 preview fidelity
    var liveTodaySpent by remember { mutableDoubleStateOf(0.0) }
    var liveTodayCount by remember { mutableIntStateOf(0) }
    var liveCash by remember { mutableDoubleStateOf(0.0) }
    var liveOnline by remember { mutableDoubleStateOf(0.0) }
    var liveBudgetLimit by remember { mutableDoubleStateOf(50000.0) }
    var liveMonthSpent by remember { mutableDoubleStateOf(0.0) }
    var liveGoalTitle by remember { mutableStateOf("Savings Target") }
    var liveGoalEmoji by remember { mutableStateOf("🎯") }
    var liveGoalSaved by remember { mutableDoubleStateOf(0.0) }
    var liveGoalTarget by remember { mutableDoubleStateOf(100000.0) }
    var liveDaysLeft by remember { mutableIntStateOf(15) }
    var liveWeekRecords by remember { mutableStateOf<List<FinancialDayRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = KanriDatabase.getDatabase(context)

                // 1. Today's spends
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStart = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val todayEnd = cal.timeInMillis

                val todayTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(todayStart, todayEnd)
                val debitTx = todayTx.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                liveTodaySpent = debitTx.sumOf { it.transaction.amount }
                liveTodayCount = debitTx.size

                // 2. Wallets
                val balanceEntities = db.walletDao().getBalancesSync()
                if (balanceEntities.isNotEmpty()) {
                    val map = balanceEntities.associateBy { it.walletId.uppercase() }
                    val txRows = db.transactionDao().getWalletTransactionsSync()
                    val balances = WalletBalanceCalculator.balances(map, txRows)
                    liveCash = balances.cash
                    liveOnline = balances.online
                } else {
                    liveCash = 4250.0
                    liveOnline = 38500.0
                }

                // 3. Month budget
                val now = Calendar.getInstance()
                val monthKey = SimpleDateFormat("yyyy-MM", Locale.US).format(now.time)
                val budget = db.budgetDao().getBudgetForMonthSync(monthKey)
                liveBudgetLimit = budget?.monthlyLimit ?: 50000.0

                val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                liveDaysLeft = maxOf(1, maxDays - now.get(Calendar.DAY_OF_MONTH))

                val monthCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val monthTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(monthCal.timeInMillis, now.timeInMillis)
                liveMonthSpent = monthTx
                    .filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                    .sumOf { it.transaction.amount }

                // 4. Savings Goal
                val goals = db.savingsGoalDao().getAllGoalsSync()
                val activeGoal = goals.firstOrNull { it.currentAmount < it.targetAmount } ?: goals.firstOrNull()
                if (activeGoal != null) {
                    liveGoalTitle = activeGoal.title
                    liveGoalEmoji = activeGoal.emoji
                    liveGoalSaved = activeGoal.currentAmount
                    liveGoalTarget = activeGoal.targetAmount
                }

                // 5. Past 7 days records
                val dayFormat = SimpleDateFormat("EEE", Locale.US)
                val dayRecords = mutableListOf<FinancialDayRecord>()
                val safeDaily = (liveBudgetLimit - liveMonthSpent).coerceAtLeast(0.0) / liveDaysLeft
                for (offset in 6 downTo 1) {
                    val dCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -offset)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val sStart = dCal.timeInMillis
                    dCal.set(Calendar.HOUR_OF_DAY, 23)
                    dCal.set(Calendar.MINUTE, 59)
                    val sEnd = dCal.timeInMillis
                    val dTx = db.transactionDao().getTransactionsWithCategoryBetweenSync(sStart, sEnd)
                    val dSpend = dTx.filter { it.transaction.type.equals("DEBIT", ignoreCase = true) && !it.transaction.isTransfer }
                        .sumOf { it.transaction.amount }
                    dayRecords.add(FinancialDayRecord(dayFormat.format(dCal.time), dSpend, safeDaily))
                }
                dayRecords.add(FinancialDayRecord("Today", liveTodaySpent, safeDaily, isToday = true))
                liveWeekRecords = dayRecords
            } catch (_: Exception) {}
        }
    }

    BackHandler {
        onBack()
    }

    val tokens = LocalKanriGlass.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "WIDGETS & PREVIEWS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp
                        ),
                        color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.click()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. STYLE SELECTOR TABS
            // =================================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CHOOSE WIDGET AESTHETIC",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.sp,
                        fontSize = 11.sp,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x18FFFFFF))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WidgetStyle.entries.forEach { style ->
                            val isSelected = selectedStyle == style
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) {
                                            when (style) {
                                                WidgetStyle.IPHONE -> Color(0xFF0A84FF)
                                                WidgetStyle.GLASS_100X -> Color(0xFF00E5FF)
                                                WidgetStyle.PIXEL_MATRIX -> Color(0xFFFFFFFF)
                                            }
                                        } else Color.Transparent
                                    )
                                    .clickable {
                                        haptics.click()
                                        selectedStyle = style
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = style.displayName,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) {
                                        if (style == WidgetStyle.PIXEL_MATRIX) Color.Black else Color.White
                                    } else Color(0x99FFFFFF),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Style Ethos & Subtitle Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x12FFFFFF))
                            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (selectedStyle) {
                                                WidgetStyle.IPHONE -> Color(0xFF0A84FF)
                                                WidgetStyle.GLASS_100X -> Color(0xFF00E5FF)
                                                WidgetStyle.PIXEL_MATRIX -> Color.White
                                            }
                                        )
                                )
                                Text(
                                    text = selectedStyle.tag,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = selectedStyle.subtitle,
                                fontFamily = GoogleSansFlex,
                                fontSize = 12.sp,
                                color = Color(0xB3FFFFFF),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // =================================================================
            // 2. APPLY AS DEFAULT THEME BUTTON
            // =================================================================
            item {
                val isCurrentlyActive = savedStyle == selectedStyle
                Button(
                    onClick = {
                        haptics.success()
                        widgetPrefs.widgetStyle = selectedStyle
                        KanriWidgetsUpdater.updateAllWidgets(context)
                        showAppliedBanner = true
                        Toast.makeText(
                            context,
                            "Applied ${selectedStyle.displayName} to home screen widgets!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentlyActive) Color(0x22FFFFFF) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isCurrentlyActive) Color.White else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyActive) Icons.Default.Check else Icons.Default.Palette,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isCurrentlyActive) {
                                "Active Home Screen Style"
                            } else {
                                "Set as Home Screen Widget Style"
                            },
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // =================================================================
            // 3. 1:1 PREVIEWS SECTION HEADER
            // =================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1:1 LIVE INTERACTIVE PREVIEWS",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.sp,
                        fontSize = 11.sp,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap widget to replay",
                        fontFamily = GoogleSansFlex,
                        fontSize = 10.sp,
                        color = Color(0x88FFFFFF)
                    )
                }
            }

            // 1. Financial Sky Weather Widget (Dot Matrix Celestial Horizon & 7-Day Pulse)
            item {
                val safeDaily = (liveBudgetLimit - liveMonthSpent).coerceAtLeast(0.0) / liveDaysLeft
                FinancialWeatherWidgetPreview(
                    style = selectedStyle,
                    todaySpent = if (liveTodaySpent > 0) liveTodaySpent else 1450.0,
                    monthBudget = liveBudgetLimit,
                    monthSpent = if (liveMonthSpent > 0) liveMonthSpent else 18500.0,
                    safeToSpendDaily = if (safeDaily > 0) safeDaily else 1250.0,
                    weekRecords = liveWeekRecords
                )
            }

            // 2. Spent Today Widget
            item {
                SpentTodayWidgetPreview(
                    style = selectedStyle,
                    amount = if (liveTodaySpent > 0) liveTodaySpent else 1450.0,
                    txCount = if (liveTodayCount > 0) liveTodayCount else 3
                )
            }

            // 2. Wallet Balance Widget
            item {
                WalletBalanceWidgetPreview(
                    style = selectedStyle,
                    cash = liveCash,
                    online = liveOnline
                )
            }

            // 3. Budget Ring Widget
            item {
                BudgetRingWidgetPreview(
                    style = selectedStyle,
                    spent = if (liveMonthSpent > 0) liveMonthSpent else 18500.0,
                    budget = liveBudgetLimit
                )
            }

            // 4. Budget Progress Bar Widget
            item {
                BudgetProgressWidgetPreview(
                    style = selectedStyle,
                    spent = if (liveMonthSpent > 0) liveMonthSpent else 18500.0,
                    budget = liveBudgetLimit,
                    daysLeft = liveDaysLeft
                )
            }

            // 5. Savings Goals Ring Widget
            item {
                GoalsRingWidgetPreview(
                    style = selectedStyle,
                    goalTitle = liveGoalTitle,
                    emoji = liveGoalEmoji,
                    saved = if (liveGoalSaved > 0) liveGoalSaved else 65000.0,
                    target = liveGoalTarget
                )
            }

            // 6. Lend & Borrow Dues Widget
            item {
                LendBorrowWidgetPreview(
                    style = selectedStyle,
                    toReceive = 5200.0,
                    toPay = 1800.0
                )
            }

            // 7. Quick Record Widget
            item {
                QuickAddWidgetPreview(style = selectedStyle)
            }

            // 8. Split The Bill Widget
            item {
                SplitBillWidgetPreview(
                    style = selectedStyle,
                    billTotal = 3200.0,
                    peopleCount = 4
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

