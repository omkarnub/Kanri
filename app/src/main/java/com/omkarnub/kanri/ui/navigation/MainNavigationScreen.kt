package com.omkarnub.kanri.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.home.AddTransactionDialog
import com.omkarnub.kanri.ui.home.HomeScreen
import com.omkarnub.kanri.ui.home.HomeViewModel
import com.omkarnub.kanri.ui.home.SetBudgetDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.omkarnub.kanri.ui.lending.AddLendingDialog
import com.omkarnub.kanri.ui.lending.LendingScreen
import com.omkarnub.kanri.ui.lending.LendingViewModel
import com.omkarnub.kanri.ui.lending.SplitExpenseSheet
import com.omkarnub.kanri.ui.month.MonthViewScreen
import com.omkarnub.kanri.ui.savings.GoalsScreen
import com.omkarnub.kanri.ui.search.SearchScreen
import com.omkarnub.kanri.ui.settings.SettingsScreen

enum class KanriTab(
    val title: String
) {
    HOME("Home"),
    INSIGHTS("Insights"),
    LEND_BORROW("Lend/Borrow"),
    GOALS("Goals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    onReplayOnboarding: () -> Unit = {},
    onTestLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(KanriTab.HOME) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel()
    val lendingViewModel: LendingViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectAsState()

    var showAddExpenseIncomeSheet by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionInitialType by remember { mutableStateOf("DEBIT") }

    var showAddLendingDialog by remember { mutableStateOf(false) }
    var showSplitSheet by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    val splitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSettingsScreen) {
        SettingsScreen(
            onBack = { showSettingsScreen = false },
            onReplayOnboarding = {
                showSettingsScreen = false
                onReplayOnboarding()
            },
            onTestLockScreen = onTestLockScreen,
            modifier = modifier
        )
        return
    }

    BackHandler(enabled = showHistoryScreen) {
        showHistoryScreen = false
    }

    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        // Smooth slide & fade transition between Main App Tabs and History (SearchScreen)
        AnimatedContent(
            targetState = showHistoryScreen,
            transitionSpec = {
                if (targetState) {
                    // Transition to History: smooth slide in from right + fade in
                    (slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { -it / 5 },
                            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                        )
                    )
                } else {
                    // Transition back to Home: smooth slide in from left + fade in
                    (slideInHorizontally(
                        initialOffsetX = { -it / 5 },
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )).togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                        ) + fadeOut(
                            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                        )
                    )
                }
            },
            label = "historyTransition",
            modifier = modifier.fillMaxSize()
        ) { isHistory ->
            if (isHistory) {
                SearchScreen(
                    onBack = { showHistoryScreen = false },
                    autoFocus = false,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Screen Content (scrolls underneath the translucent frosted glass dock)
                    // Pure fade transition when switching menus
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn(
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                            ).togetherWith(
                                fadeOut(
                                    animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                )
                            )
                        },
                        label = "tabFadeTransition",
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(hazeState)
                    ) { targetTab ->
                        when (targetTab) {
                            KanriTab.HOME -> HomeScreen(
                                onOpenSettings = { showSettingsScreen = true },
                                onNavigateToHistory = { showHistoryScreen = true },
                                onNavigateToLending = { selectedTab = KanriTab.LEND_BORROW },
                                onNavigateToInsights = { selectedTab = KanriTab.INSIGHTS },
                                viewModel = homeViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            KanriTab.INSIGHTS -> MonthViewScreen(
                                onOpenSettings = { showSettingsScreen = true },
                                modifier = Modifier.fillMaxSize()
                            )
                            KanriTab.LEND_BORROW -> LendingScreen(
                                viewModel = lendingViewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            KanriTab.GOALS -> GoalsScreen(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Floating Frosted Glass Menu Popup for '+' button
                    AddExpenseIncomePopup(
                        isOpen = showAddExpenseIncomeSheet,
                        onDismiss = { showAddExpenseIncomeSheet = false },
                        onAddExpense = {
                            addTransactionInitialType = "DEBIT"
                            showAddTransactionDialog = true
                        },
                        onAddIncome = {
                            addTransactionInitialType = "CREDIT"
                            showAddTransactionDialog = true
                        },
                        hazeState = hazeState
                    )

                    // Floating Translucent Glass Dock Overlay
                    KanriFloatingDock(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        onAddClick = { showAddExpenseIncomeSheet = !showAddExpenseIncomeSheet },
                        isAddMenuOpen = showAddExpenseIncomeSheet,
                        hazeState = hazeState,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }

        // Add Transaction Dialog (Expense or Income)
        if (showAddTransactionDialog) {
            AddTransactionDialog(
                initialType = addTransactionInitialType,
                categories = homeState.categories,
                onDismiss = { showAddTransactionDialog = false },
                onConfirm = { amount, type, counterparty, sourceType, categoryId ->
                    homeViewModel.addManualTransaction(amount, type, counterparty, sourceType, categoryId)
                    showAddTransactionDialog = false
                },
                hazeState = hazeState
            )
        }

        // Add Lending Dialog
        if (showAddLendingDialog) {
            AddLendingDialog(
                onDismiss = { showAddLendingDialog = false },
                onSave = { personName, amount, type, dueDate, notes ->
                    lendingViewModel.addRecord(personName, amount, type, dueDate, notes)
                    showAddLendingDialog = false
                }
            )
        }

        // Split Bill Sheet
        if (showSplitSheet) {
            SplitExpenseSheet(
                sheetState = splitSheetState,
                onDismiss = { showSplitSheet = false },
                onSaveLenderSplit = { friendNames, perPersonShare, eventDescription ->
                    lendingViewModel.addSplitAsLender(friendNames, perPersonShare, eventDescription)
                    showSplitSheet = false
                },
                onSaveBorrowerSplit = { payerName, userShare, eventDescription ->
                    lendingViewModel.addSplitAsBorrower(payerName, userShare, eventDescription)
                    showSplitSheet = false
                }
            )
        }

        // Set Budget Dialog
        if (showSetBudgetDialog) {
            SetBudgetDialog(
                currentBudget = homeState.monthlyBudget,
                monthName = homeState.currentMonthName,
                onDismiss = { showSetBudgetDialog = false },
                onSaveBudget = { newLimit ->
                    homeViewModel.updateMonthlyBudget(newLimit)
                    showSetBudgetDialog = false
                }
            )
        }
    }
}
