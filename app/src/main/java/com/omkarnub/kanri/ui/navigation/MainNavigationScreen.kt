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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import com.omkarnub.kanri.ui.home.TransactionDetailScreen
import com.omkarnub.kanri.ui.lending.AddLendingDialog
import com.omkarnub.kanri.ui.lending.LendingScreen
import com.omkarnub.kanri.ui.lending.LendingViewModel
import com.omkarnub.kanri.ui.lending.SplitExpenseSheet
import com.omkarnub.kanri.ui.month.MonthViewScreen
import com.omkarnub.kanri.ui.profile.ProfileScreen
import com.omkarnub.kanri.ui.savings.GoalsScreen
import com.omkarnub.kanri.ui.search.SearchScreen
import com.omkarnub.kanri.ui.settings.SettingsScreen
import com.omkarnub.kanri.ui.theme.KanriMotion

enum class KanriTab(
    val title: String
) {
    HOME("Home"),
    INSIGHTS("Insights"),
    LEND_BORROW("Lend & Borrow"),
    GOALS("Goals")
}

private sealed interface MainNavDestination {
    data object Tabs : MainNavDestination
    data object History : MainNavDestination
    data object Profile : MainNavDestination
    data object Settings : MainNavDestination
    data class TransactionDetail(val txId: Long) : MainNavDestination

    val depth: Int get() = when (this) {
        Tabs -> 0
        History, Profile -> 1
        Settings, is TransactionDetail -> 2
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigationScreen(
    onReplayOnboarding: () -> Unit = {},
    onTestLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(KanriTab.HOME) }
    var showProfileScreen by remember { mutableStateOf(false) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    var viewingTransactionId by remember { mutableStateOf<Long?>(null) }
    var isInsightsSubScreenOpen by remember { mutableStateOf(false) }

    val homeViewModel: HomeViewModel = viewModel()
    val lendingViewModel: LendingViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    val lendingState by lendingViewModel.uiState.collectAsState()

    var showAddExpenseIncomeSheet by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionInitialType by remember { mutableStateOf("DEBIT") }

    var showAddLendingDialog by remember { mutableStateOf(false) }
    var addLendingInitialType by remember { mutableStateOf("LENT") }
    var addLendingInitialAmount by remember { androidx.compose.runtime.mutableDoubleStateOf(100.0) }
    var addLendingInitialPerson by remember { mutableStateOf("") }
    var addLendingInitialNotes by remember { mutableStateOf<String?>("") }
    var showSplitSheet by remember { mutableStateOf(false) }
    var splitInitialAmount by remember { mutableStateOf<Double?>(null) }
    var splitInitialDescription by remember { mutableStateOf<String?>(null) }
    var openWalletSheetFromWidget by remember { mutableStateOf(false) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    val splitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val context = androidx.compose.ui.platform.LocalContext.current
    val updateManager = remember { com.omkarnub.kanri.data.update.AppUpdateManager.getInstance(context) }
    val updatePrefs = remember { com.omkarnub.kanri.data.update.AppUpdatePreferences.getInstance(context) }
    val showUpdateTrigger by updateManager.showUpdatePopupTrigger.collectAsState()
    val pendingUpdate by updatePrefs.pendingUpdateFlow.collectAsState()
    var activeUpdateDialogInfo by remember { mutableStateOf<com.omkarnub.kanri.data.update.AppUpdateInfo?>(null) }

    LaunchedEffect(showUpdateTrigger, pendingUpdate) {
        if (showUpdateTrigger) {
            updateManager.dismissUpdatePopupTrigger()
            val info = pendingUpdate
            if (info != null) {
                activeUpdateDialogInfo = info
            } else {
                updateManager.checkForUpdate().onSuccess { freshInfo ->
                    if (freshInfo != null) {
                        activeUpdateDialogInfo = freshInfo
                    }
                }
            }
        }
    }

    val widgetActionEvent by com.omkarnub.kanri.widget.KanriWidgetActions.pendingActionEvent.collectAsState()
    LaunchedEffect(widgetActionEvent) {
        val event = widgetActionEvent ?: return@LaunchedEffect
        when (event.action) {
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_SPLIT_BILL -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                val (amt, desc) = com.omkarnub.kanri.widget.KanriWidgetActions.consumeSplitPayload()
                splitInitialAmount = amt
                splitInitialDescription = desc
                showSplitSheet = true
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_ADD_EXPENSE -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                addTransactionInitialType = "DEBIT"
                showAddTransactionDialog = true
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_ADD_INCOME -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                addTransactionInitialType = "CREDIT"
                showAddTransactionDialog = true
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_OPEN_GOALS -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                selectedTab = KanriTab.GOALS
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_OPEN_LENDING -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                selectedTab = KanriTab.LEND_BORROW
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_OPEN_HOME -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                selectedTab = KanriTab.HOME
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
            com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_OPEN_WALLET -> {
                showSettingsScreen = false
                showProfileScreen = false
                showHistoryScreen = false
                selectedTab = KanriTab.HOME
                openWalletSheetFromWidget = true
                com.omkarnub.kanri.widget.KanriWidgetActions.consumeAction()
            }
        }
    }

    val currentDestination: MainNavDestination = when {
        viewingTransactionId != null -> MainNavDestination.TransactionDetail(viewingTransactionId!!)
        showSettingsScreen -> MainNavDestination.Settings
        showProfileScreen -> MainNavDestination.Profile
        showHistoryScreen -> MainNavDestination.History
        else -> MainNavDestination.Tabs
    }

    BackHandler(enabled = viewingTransactionId != null) {
        viewingTransactionId = null
    }

    BackHandler(enabled = showSettingsScreen && viewingTransactionId == null) {
        showSettingsScreen = false
    }

    BackHandler(enabled = showProfileScreen && !showSettingsScreen && viewingTransactionId == null) {
        showProfileScreen = false
    }

    BackHandler(enabled = showHistoryScreen && viewingTransactionId == null && !showSettingsScreen && !showProfileScreen) {
        showHistoryScreen = false
    }

    val parentHazeState = LocalHazeState.current
    val localHazeState = remember { dev.chrisbanes.haze.HazeState() }
    val effectiveHazeState = parentHazeState ?: localHazeState

    CompositionLocalProvider(LocalHazeState provides effectiveHazeState) {
        // Apple HIG horizontal push & pop transitions between Main App Tabs, History, Profile, Settings, and TransactionDetail
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val isPush = targetState.depth > initialState.depth ||
                    (targetState.depth == initialState.depth && targetState != initialState && targetState != MainNavDestination.Tabs)
                if (isPush) {
                    KanriMotion.screenPushEnter().togetherWith(KanriMotion.screenPushExit())
                } else {
                    KanriMotion.screenPopEnter().togetherWith(KanriMotion.screenPopExit())
                }
            },
            label = "mainNavDestinationTransition",
            modifier = modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                MainNavDestination.Tabs -> {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Screen Content (scrolls underneath the translucent frosted glass dock)
                        // Directional tab slide transition with fluid momentum
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val isForward = targetState.ordinal >= initialState.ordinal
                                KanriMotion.tabTransition(isForward)
                            },
                            label = "tabTransition",
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (parentHazeState == null) {
                                        Modifier.haze(effectiveHazeState)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) { targetTab ->
                            when (targetTab) {
                                KanriTab.HOME -> HomeScreen(
                                    onOpenProfile = { showProfileScreen = true },
                                    onOpenSettings = { showSettingsScreen = true },
                                    onNavigateToHistory = { showHistoryScreen = true },
                                    onNavigateToLending = { selectedTab = KanriTab.LEND_BORROW },
                                    onNavigateToInsights = { selectedTab = KanriTab.INSIGHTS },
                                    onTransactionClick = { item -> viewingTransactionId = item.transaction.id },
                                    openWalletSheet = openWalletSheetFromWidget,
                                    onWalletSheetOpened = { openWalletSheetFromWidget = false },
                                    viewModel = homeViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                KanriTab.INSIGHTS -> MonthViewScreen(
                                    onOpenSettings = { showProfileScreen = true },
                                    onSubScreenStateChanged = { isInsightsSubScreenOpen = it },
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
                            hazeState = effectiveHazeState
                        )

                        // Floating Translucent Glass Dock Overlay (slides down when detail sub-screen is active)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isInsightsSubScreenOpen,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            ) + fadeIn(tween(180)),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(200, easing = KanriMotion.EaseOut)
                            ) + fadeOut(tween(140)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            KanriFloatingDock(
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                onAddClick = { showAddExpenseIncomeSheet = !showAddExpenseIncomeSheet },
                                isAddMenuOpen = showAddExpenseIncomeSheet,
                                hazeState = effectiveHazeState
                            )
                        }
                    }
                }
                MainNavDestination.History -> {
                    SearchScreen(
                        onBack = { showHistoryScreen = false },
                        autoFocus = false,
                        onTransactionClick = { item -> viewingTransactionId = item.transaction.id },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainNavDestination.Profile -> {
                    ProfileScreen(
                        onClose = { showProfileScreen = false },
                        onOpenSettings = {
                            showProfileScreen = false
                            showSettingsScreen = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                MainNavDestination.Settings -> {
                    SettingsScreen(
                        onBack = { showSettingsScreen = false },
                        onReplayOnboarding = onReplayOnboarding,
                        onTestLockScreen = onTestLockScreen,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is MainNavDestination.TransactionDetail -> {
                    TransactionDetailScreen(
                        transactionId = destination.txId,
                        onBack = { viewingTransactionId = null },
                        onDeleted = { viewingTransactionId = null },
                        viewModel = homeViewModel,
                        modifier = Modifier.fillMaxSize()
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
                onConfirm = { amount, type, counterparty, sourceType, categoryId, wallet ->
                    homeViewModel.addManualTransaction(amount, type, counterparty, sourceType, categoryId, wallet)
                    showAddTransactionDialog = false
                },
                onOpenLendBorrow = { amount, type, counterparty, note ->
                    showAddTransactionDialog = false
                    addLendingInitialType = type
                    addLendingInitialAmount = amount
                    addLendingInitialPerson = counterparty
                    addLendingInitialNotes = note
                    showAddLendingDialog = true
                },
                hazeState = effectiveHazeState
            )
        }

        // Add Lending Dialog
        if (showAddLendingDialog) {
            com.omkarnub.kanri.ui.lending.LendingEntryDialog(
                onDismiss = { showAddLendingDialog = false },
                onSave = { personName, amount, type, date, dueDate, notes, wallet ->
                    lendingViewModel.addRecord(personName, amount, type, dueDate, notes, wallet)
                    showAddLendingDialog = false
                },
                initialType = addLendingInitialType,
                initialPersonName = addLendingInitialPerson,
                initialAmount = addLendingInitialAmount,
                initialNotes = addLendingInitialNotes,
                hazeState = effectiveHazeState
            )
        }

        // Split Bill Full Screen Overlay with Apple Drawer curve
        androidx.compose.animation.AnimatedVisibility(
            visible = showSplitSheet,
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(340, easing = KanriMotion.EaseDrawer)
            ) + androidx.compose.animation.fadeIn(tween(240, easing = KanriMotion.EaseOut)),
            exit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(280, easing = KanriMotion.EaseDrawer)
            ) + androidx.compose.animation.fadeOut(tween(180, easing = KanriMotion.EaseOut))
        ) {
            val existingPeople = remember(lendingState.records) {
                lendingState.records.map { it.personName.trim() }.distinct().filter { it.isNotBlank() }
            }
            SplitExpenseSheet(
                sheetState = splitSheetState,
                onDismiss = {
                    showSplitSheet = false
                    splitInitialAmount = null
                    splitInitialDescription = null
                },
                onSaveLenderSplit = { friendNames, perPersonShare, eventDescription ->
                    lendingViewModel.addSplitAsLender(friendNames, perPersonShare, eventDescription)
                    showSplitSheet = false
                    splitInitialAmount = null
                    splitInitialDescription = null
                },
                onSaveBorrowerSplit = { payerName, userShare, eventDescription ->
                    lendingViewModel.addSplitAsBorrower(payerName, userShare, eventDescription)
                    showSplitSheet = false
                    splitInitialAmount = null
                    splitInitialDescription = null
                },
                existingPeople = existingPeople,
                initialAmount = splitInitialAmount,
                initialDescription = splitInitialDescription
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

        // App Update Popup Dialog
        if (activeUpdateDialogInfo != null) {
            com.omkarnub.kanri.ui.update.AppUpdateDialog(
                updateInfo = activeUpdateDialogInfo!!,
                onDismiss = { activeUpdateDialogInfo = null }
            )
        }
    }
}

