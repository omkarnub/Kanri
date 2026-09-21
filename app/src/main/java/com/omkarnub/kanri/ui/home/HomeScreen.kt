package com.omkarnub.kanri.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import com.omkarnub.kanri.util.CurrencyUtils
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.backup.BackupRestoreSheet
import com.omkarnub.kanri.ui.popup.InstantPopupSheet
import com.omkarnub.kanri.ui.search.SearchScreen
import com.omkarnub.kanri.ui.security.SecuritySettingsSheet
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.platform.LocalContext
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.FlipFadeText
import com.omkarnub.kanri.ui.greeting.GreetingHeader
import com.omkarnub.kanri.ui.health.DiagnosticsSheet
import com.omkarnub.kanri.ui.health.HealthCheckBanner
import com.omkarnub.kanri.ui.health.HealthCheckHelper
import com.omkarnub.kanri.ui.recurring.RecurringPaymentsSheet
import com.omkarnub.kanri.ui.savings.SavingsGoalsSheet
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// -----------------------------------------------------------------------------
// Constants & User Configuration
// -----------------------------------------------------------------------------
// USER_NAME placeholder: Connect to user profile / onboarding preference in future
private const val DEFAULT_USER_NAME = "Alex"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToLending: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val greetingResult by viewModel.greeting.collectAsState()
    var selectedTransactionForCategory by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var showNeedsReviewSheet by remember { mutableStateOf(false) }
    var hasDismissedBudgetPrompt by rememberSaveable { mutableStateOf(false) }

    if (!state.isLoading && !state.hasBudget && !hasDismissedBudgetPrompt) {
        SetBudgetDialog(
            currentBudget = state.monthlyBudget,
            monthName = state.currentMonthName.ifEmpty { "This Month" },
            onDismiss = { hasDismissedBudgetPrompt = true },
            onSaveBudget = { newLimit ->
                viewModel.updateMonthlyBudget(newLimit)
                hasDismissedBudgetPrompt = true
            }
        )
    }

    // App launch entrance animations: staggered fluid fade-in and slide-up
    val headerEntrance = remember { Animatable(0f) }
    val greetingFade = remember { Animatable(0f) }
    val dateFade = remember { Animatable(0f) }
    val needsReviewEntrance = remember { Animatable(0f) }
    val visualizerEntrance = remember { Animatable(0f) }
    val transactionsEntrance = remember { Animatable(0f) }
    val safeSpendEntrance = remember { Animatable(0f) }
    val streakEntrance = remember { Animatable(0f) }
    val footerEntrance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Header (Logo & Profile) - subtle slide down + fade
        launch {
            headerEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 450, delayMillis = 50, easing = FastOutSlowInEasing)
            )
        }
        // 2. Greeting with user name - smooth fade & gentle rise
        launch {
            greetingFade.animateTo(
                1f,
                animationSpec = tween(durationMillis = 500, delayMillis = 120, easing = FastOutSlowInEasing)
            )
        }
        // 3. Small dim day and date - elegant delayed fade
        launch {
            dateFade.animateTo(
                1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 220, easing = FastOutSlowInEasing)
            )
        }
        // 4. Needs-Review badge (if visible)
        launch {
            needsReviewEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 500, delayMillis = 280, easing = FastOutSlowInEasing)
            )
        }
        // 5. Visual Data Card - scale + glide up + fade (immediate for digit rolling)
        launch {
            visualizerEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 200, delayMillis = 50, easing = FastOutSlowInEasing)
            )
        }
        // 6. Recent Transactions List
        launch {
            transactionsEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        // 7. Daily Safe-to-Spend Card
        launch {
            safeSpendEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        // 8. No-Spend Streak Card
        launch {
            streakEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 580, easing = FastOutSlowInEasing)
            )
        }
        // 9. Support Footer
        launch {
            footerEntrance.animateTo(
                1f,
                animationSpec = tween(durationMillis = 500, delayMillis = 660, easing = FastOutSlowInEasing)
            )
        }
    }

    if (showNeedsReviewSheet) {
        NeedsReviewSheet(
            reviewQueue = state.reviewQueue,
            categories = state.categories,
            onDismiss = { showNeedsReviewSheet = false },
            onAssignCategory = { txId, counterparty, catId, note ->
                viewModel.assignCategory(
                    transactionId = txId,
                    counterparty = counterparty,
                    categoryId = catId,
                    note = note,
                    bulkUpdate = false
                )
            }
        )
    }

    selectedTransactionForCategory?.let { target ->
        CategoryPickerSheet(
            targetTransaction = target,
            categories = state.categories,
            onDismiss = { selectedTransactionForCategory = null },
            onCategorySelected = { categoryId, note ->
                viewModel.assignCategory(
                    transactionId = target.transaction.id,
                    counterparty = target.transaction.counterparty,
                    categoryId = categoryId,
                    note = note,
                    bulkUpdate = true
                )
                selectedTransactionForCategory = null
            }
        )
    }

    if (state.isLoading) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                com.omkarnub.kanri.ui.common.KanriWobbleLoader(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    } else {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // =========================================================================
                // 1 & 2. TOP HEADER ROW: KANRI LOGO (Left) & DEMO PROFILE ICON (Right)
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = headerEntrance.value
                            translationY = (1f - headerEntrance.value) * (-8.dp.toPx())
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1 - KANRI LOGO
                    Text(
                        text = "KANRI",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = com.omkarnub.kanri.ui.theme.Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.4.sp,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 2 - DEMO PROFILE ICON (will be customized later)
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                CircleShape
                            )
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile (Demo)",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // =========================================================================
                // 3. GREETINGS WITH USER NAME & FINANCIAL SUBTITLE (3D FLIPFADE ANIMATION)
                // =========================================================================
                GreetingHeader(
                    greetingResult = greetingResult,
                    modifier = Modifier.graphicsLayer {
                        alpha = greetingFade.value
                        translationY = (1f - greetingFade.value) * 16.dp.toPx()
                    },
                    onNavigateToInsights = onNavigateToInsights,
                    onOpenReviewQueue = { showNeedsReviewSheet = true },
                    onGreetingDisplayed = { result -> viewModel.onGreetingDisplayed(result) }
                )

                // =========================================================================
                // 3.5. NEEDS-REVIEW BADGE (Visible only if count > 0)
                // =========================================================================
                if (state.reviewCount > 0) {
                    NeedsReviewCard(
                        reviewCount = state.reviewCount,
                        onClick = { showNeedsReviewSheet = true },
                        modifier = Modifier.graphicsLayer {
                            alpha = needsReviewEntrance.value
                            translationY = (1f - needsReviewEntrance.value) * 16.dp.toPx()
                        }
                    )
                }

                // =========================================================================
                // 4. MULTI-STYLE EXPENSE DATA VISUALIZER (RING, HALF-RING, BARS, PIE, DIGITAL)
                // =========================================================================
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = visualizerEntrance.value
                        translationY = (1f - visualizerEntrance.value) * 20.dp.toPx()
                        scaleX = 0.96f + 0.04f * visualizerEntrance.value
                        scaleY = 0.96f + 0.04f * visualizerEntrance.value
                    }
                ) {
                    ExpenseVisualizer(
                        monthlyBudget = state.monthlyBudget,
                        transactions = state.transactions,
                        categories = state.categories,
                        monthDelta = state.monthDelta,
                        onSelectMonth = { year, month -> viewModel.selectMonth(year, month) },
                        onUpdateBudget = { newLimit -> viewModel.updateMonthlyBudget(newLimit) }
                    )
                }

                // =========================================================================
                // 4.1. MONTHLY CASH FLOW SNAPSHOT (INCOME vs SPENT vs NET)
                // =========================================================================
                CashFlowCard(
                    monthIncome = state.monthIncome,
                    monthSpent = state.monthSpent,
                    monthNet = state.monthNetCashFlow,
                    monthName = state.currentMonthName,
                    modifier = Modifier.graphicsLayer {
                        alpha = visualizerEntrance.value
                        translationY = (1f - visualizerEntrance.value) * 22.dp.toPx()
                    }
                )

                // =========================================================================
                // 4.2. LEND & BORROW QUICK PULSE
                // =========================================================================
                LendingQuickPulse(
                    totalLentPending = state.totalLentPending,
                    totalBorrowedPending = state.totalBorrowedPending,
                    onClick = onNavigateToLending,
                    modifier = Modifier.graphicsLayer {
                        alpha = visualizerEntrance.value
                        translationY = (1f - visualizerEntrance.value) * 26.dp.toPx()
                    }
                )

                // =========================================================================
                // 5. RECENT 3 TRANSACTIONS (INCOME/DEBIT) WITH "SEE MORE"
                // =========================================================================
                RecentTransactionsSection(
                    transactions = state.transactions.take(3),
                    onTransactionClick = { selectedTransactionForCategory = it },
                    onSeeMoreClick = onNavigateToHistory,
                    modifier = Modifier.graphicsLayer {
                        alpha = transactionsEntrance.value
                        translationY = (1f - transactionsEntrance.value) * 28.dp.toPx()
                    }
                )

                // =========================================================================
                // 6. DAILY SAFE-TO-SPEND & FINANCIAL PACE WIDGET
                // =========================================================================
                DailySafeToSpendCard(
                    safeToSpendPerDay = state.safeToSpendPerDay,
                    remainingBudget = state.monthBudgetRemaining,
                    totalBudget = state.monthlyBudget,
                    daysRemaining = state.daysLeftInMonth,
                    currentMonth = state.currentMonthName,
                    modifier = Modifier.graphicsLayer {
                        alpha = safeSpendEntrance.value
                        translationY = (1f - safeSpendEntrance.value) * 32.dp.toPx()
                    }
                )

                // =========================================================================
                // 6.5. NO-SPEND STREAK CARD
                // =========================================================================
                NoSpendStreakCard(
                    streakState = state.streakState,
                    lastCelebratedMilestone = state.lastCelebratedMilestone,
                    onMilestoneCelebrated = { milestone -> viewModel.setLastCelebratedMilestone(milestone) },
                    modifier = Modifier.graphicsLayer {
                        alpha = streakEntrance.value
                        translationY = (1f - streakEntrance.value) * 32.dp.toPx()
                    }
                )

                // =========================================================================
                // 7. FOOTER: SUPPORT / CONTACT US HYPERLINK WITH SUBTLE ANIMATION
                // =========================================================================
                SupportContactFooter(
                    modifier = Modifier.graphicsLayer {
                        alpha = footerEntrance.value
                        translationY = (1f - footerEntrance.value) * 20.dp.toPx()
                    }
                )

                // Bottom spacing so content smoothly scrolls above the floating frosted dock
                Spacer(modifier = Modifier.height(115.dp))
            }
        }
    }
}



/**
 * 5. Recent 3 Transactions with "See More" button that opens History
 */
@Composable
private fun RecentTransactionsSection(
    transactions: List<TransactionWithCategory>,
    onTransactionClick: (TransactionWithCategory) -> Unit,
    onSeeMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Title & See More Button (styled like the September dropdown menu)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // See More Button (merges with background, text + arrow)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSeeMoreClick)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "See more",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "See more transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            EmptyTransactionsCard(onSeeMoreClick = onSeeMoreClick)
        } else {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                transactions.forEachIndexed { index, item ->
                    TransactionListItem(
                        item = item,
                        onClick = { onTransactionClick(item) },
                        showDivider = index < transactions.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsCard(onSeeMoreClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No recent transactions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Transactions from bank SMS or manual entries will show here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.clickable(onClick = onSeeMoreClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = "Open History →",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getGreetingForCurrentTime(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}


@Composable
fun TransactionListItem(
    item: TransactionWithCategory,
    onClick: () -> Unit,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transaction = item.transaction
    val category = item.category
    val isDebit = transaction.type.equals("DEBIT", ignoreCase = true)
    val amountColor = if (isDebit) ExpenseRed else IncomeGreen
    val amountPrefix = if (isDebit) "- " else "+ "

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Source Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                if (category != null) {
                    CategoryIcon(
                        categoryName = category.name,
                        iconName = category.iconName,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = getSourceIcon(transaction.sourceType),
                        contentDescription = transaction.sourceType,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                val title = transaction.counterparty?.takeIf { it.isNotBlank() } ?: (if (isDebit) "Expense" else "Income")
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Badge for Source
                    Text(
                        text = transaction.sourceType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )

                    if (!transaction.bank.isNullOrBlank()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Text(
                            text = transaction.bank,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTimestamp(transaction.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Category Tag (Monochrome theme)
                if (category != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "+ Categorize",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount
            Text(
                text = "$amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = amountColor
            )
        }

        if (showDivider) {
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    item: TransactionWithCategory,
    onClick: () -> Unit
) {
    TransactionListItem(
        item = item,
        onClick = onClick,
        showDivider = true
    )
}

/**
 * 6. Daily Safe-to-Spend & Financial Pace Card:
 * Answers: "How much can I safely spend today?" maintaining Kanri's minimal, monochrome aesthetic.
 */
@Composable
private fun DailySafeToSpendCard(
    safeToSpendPerDay: Double,
    remainingBudget: Double,
    totalBudget: Double,
    daysRemaining: Int,
    currentMonth: String,
    modifier: Modifier = Modifier
) {
    val isOverBudget = remainingBudget <= 0.0
    val budgetRatio = if (totalBudget > 0.0) ((totalBudget - remainingBudget) / totalBudget).coerceIn(0.0, 1.0) else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Header: Section Title
            Text(
                text = "DAILY SAFE-TO-SPEND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Value: Safe amount per day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatCurrency(safeToSpendPerDay),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isOverBudget) "Budget exhausted for this month" else "per day for the rest of $currentMonth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }

                Text(
                    text = "$daysRemaining days left",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { budgetRatio.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurface,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Sub-row: Remaining & Monthly Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining: ${formatCurrency(remainingBudget.coerceAtLeast(0.0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Budget: ${formatCurrency(totalBudget)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * 7. Support & Feedback Footer with subtle breathing animation and hyperlink
 */
@Composable
private fun SupportContactFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "supportFooterPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "supportPulseAlpha"
    )
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "supportArrowOffset"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Found an issue? ",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable {
                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support.kanri.app@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Kanri Support / Issue Report")
                    }
                    try {
                        context.startActivity(emailIntent)
                    } catch (_: Exception) {
                        try {
                            val fallbackIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("mailto:support.kanri.app@gmail.com")
                            )
                            context.startActivity(fallbackIntent)
                        } catch (_: Exception) {}
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Contact us",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = pulseAlpha)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Contact us",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = pulseAlpha),
                modifier = Modifier
                    .size(11.dp)
                    .offset(x = arrowOffset.dp)
            )
        }
    }
}




fun getSourceIcon(sourceType: String): ImageVector {
    return when (sourceType.uppercase()) {
        "ATM" -> Icons.Default.LocalAtm
        "CARD" -> Icons.Default.CreditCard
        "BANK_TRANSFER" -> Icons.Default.AccountBalance
        "CASH" -> Icons.Default.Payments
        else -> Icons.Default.Payments // UPI or default
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "₹${formatter.format(amount)}"
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
