package com.omkarnub.kanri.ui.lending

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingScreen(
    modifier: Modifier = Modifier,
    viewModel: LendingViewModel = viewModel(),
    hubViewModel: LendingHubViewModel = viewModel()
) {
    val hubState by hubViewModel.uiState.collectAsState()
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog states
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var addEntryInitialType by remember { mutableStateOf("LENT") }
    var addEntryInitialPerson by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<LendingWithRepayments?>(null) }

    var repaymentDialogPerson by remember { mutableStateOf<PersonSummary?>(null) }
    var repaymentDialogEntry by remember { mutableStateOf<LendingWithRepayments?>(null) }

    var renameDialogPerson by remember { mutableStateOf<PersonSummary?>(null) }
    var settleAllDialogPerson by remember { mutableStateOf<PersonSummary?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<LendingWithRepayments?>(null) }
    var upiDialogPerson by remember { mutableStateOf<PersonSummary?>(null) }

    // Split Expense Sheet
    var showSplitSheet by remember { mutableStateOf(false) }
    val splitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Local HazeState to blur screen content behind dialog cards
    val lendingHazeState = remember { HazeState() }

    // Entrance animation animatables (played once on tab entrance)
    val headerAlpha = remember { Animatable(0f) }
    val heroAlpha = remember { Animatable(0f) }
    val controlAlpha = remember { Animatable(0f) }
    val listAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            headerAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, delayMillis = 0, easing = FastOutSlowInEasing)
            )
        }
        launch {
            heroAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, delayMillis = 60, easing = FastOutSlowInEasing)
            )
        }
        launch {
            controlAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 440, delayMillis = 130, easing = FastOutSlowInEasing)
            )
        }
        launch {
            listAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 460, delayMillis = 180, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .haze(lendingHazeState),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (hubState.selectedPersonSummary == null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "LEND & BORROW",
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
                        // Pill-shaped Add Record Action Button
                        Surface(
                            onClick = {
                                haptics.primaryAction()
                                addEntryInitialType = "LENT"
                                addEntryInitialPerson = ""
                                editingEntry = null
                                showAddEntryDialog = true
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Record",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Add Record",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.graphicsLayer {
                        alpha = headerAlpha.value
                        translationY = (1f - headerAlpha.value) * (-10.dp.toPx())
                    }
                )
            }
        }
    ) { innerPadding ->
        // Main Screen Transition: Main List <-> Person Detail
        AnimatedContent(
            targetState = hubState.selectedPersonSummary,
            transitionSpec = {
                if (targetState != null) {
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
            label = "personDetailTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { selectedPerson ->
            if (selectedPerson != null) {
                // Intercept back button to return to Main list
                BackHandler {
                    hubViewModel.clearSelectedPerson()
                }

                LendingPersonDetailView(
                    person = selectedPerson,
                    onBack = {
                        haptics.tick()
                        hubViewModel.clearSelectedPerson()
                    },
                    onRenameClick = {
                        haptics.click()
                        renameDialogPerson = selectedPerson
                    },
                    onAddLentClick = {
                        haptics.click()
                        addEntryInitialType = "LENT"
                        addEntryInitialPerson = selectedPerson.displayName
                        editingEntry = null
                        showAddEntryDialog = true
                    },
                    onAddBorrowedClick = {
                        haptics.click()
                        addEntryInitialType = "BORROWED"
                        addEntryInitialPerson = selectedPerson.displayName
                        editingEntry = null
                        showAddEntryDialog = true
                    },
                    onRecordRepaymentClick = {
                        haptics.click()
                        repaymentDialogPerson = selectedPerson
                        repaymentDialogEntry = null
                    },
                    onSettleAllClick = {
                        haptics.click()
                        settleAllDialogPerson = selectedPerson
                    },
                    onPayUpiClick = {
                        haptics.click()
                        upiDialogPerson = selectedPerson
                    },
                    onTransactionClick = { entry ->
                        haptics.click()
                        selectedTransactionForDetail = entry
                    },
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            } else {
                // Main Tab View (People & Timeline)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // 1. Hero Card
                    item {
                        LendingHeroCard(
                            heroTotals = hubState.heroTotals,
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .graphicsLayer {
                                    alpha = heroAlpha.value
                                    translationY = (1f - heroAlpha.value) * 16.dp.toPx()
                                }
                        )
                    }

                    // 2. Borderless Card: Split a Group Expense
                    item {
                        LendingSplitGroupCard(
                            onClick = {
                                haptics.click()
                                showSplitSheet = true
                            },
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .graphicsLayer {
                                    alpha = heroAlpha.value
                                    translationY = (1f - heroAlpha.value) * 16.dp.toPx()
                                }
                        )
                    }

                    // 3. Segmented Control: People | Timeline
                    item {
                        LendingSegmentedControl(
                            selectedTab = hubState.selectedTab,
                            onTabSelected = {
                                haptics.tick()
                                hubViewModel.selectTab(it)
                            },
                            modifier = Modifier
                                .padding(horizontal = 18.dp)
                                .graphicsLayer {
                                    alpha = controlAlpha.value
                                    translationY = (1f - controlAlpha.value) * 16.dp.toPx()
                                }
                        )
                    }

                    // 3. Tab Content: People or Timeline
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                                .graphicsLayer {
                                    alpha = listAlpha.value
                                    translationY = (1f - listAlpha.value) * 16.dp.toPx()
                                }
                        ) {
                            if (hubState.allPeopleSummaries.isEmpty()) {
                                // Clean Empty State with Add CTA
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "No lending or borrowed records yet",
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Keep track of money lent to or borrowed from friends.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    Button(
                                        onClick = {
                                            addEntryInitialType = "LENT"
                                            addEntryInitialPerson = ""
                                            editingEntry = null
                                            showAddEntryDialog = true
                                        },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSurface,
                                            contentColor = MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Entry", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                when (hubState.selectedTab) {
                                    LendingTabOption.PEOPLE -> {
                                        LendingPeopleView(
                                            needsAttentionPeople = hubState.needsAttentionPeople,
                                            activePeople = hubState.activePeople,
                                            settledPeople = hubState.settledPeople,
                                            isSettledExpanded = hubState.isSettledSectionExpanded,
                                            onToggleSettled = {
                                                haptics.tick()
                                                hubViewModel.toggleSettledSection()
                                            },
                                            onPersonClick = { person ->
                                                haptics.click()
                                                hubViewModel.selectPerson(person.key)
                                            }
                                        )
                                    }
                                    LendingTabOption.TIMELINE -> {
                                        LendingTimelineView(
                                            timelineGroups = hubState.timelineGroups,
                                            activeFilter = hubState.timelineFilter,
                                            onFilterSelected = {
                                                haptics.tick()
                                                hubViewModel.setTimelineFilter(it)
                                            },
                                            onEntryClick = { entry ->
                                                haptics.click()
                                                selectedTransactionForDetail = entry
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Spacer for 115.dp dock clearance
                    item {
                        Spacer(modifier = Modifier.height(115.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    CompositionLocalProvider(LocalHazeState provides lendingHazeState) {
        // 1. Add / Edit Entry Dialog
        if (showAddEntryDialog) {
            val recentPeople = remember(hubState.allPeopleSummaries) {
                hubState.allPeopleSummaries.map { it.displayName }
            }
            LendingEntryDialog(
                initialType = addEntryInitialType,
                initialPersonName = addEntryInitialPerson,
                existingEntry = editingEntry,
                recentPeople = recentPeople,
                allPeople = hubState.allPeopleSummaries,
                onDismiss = {
                    showAddEntryDialog = false
                    editingEntry = null
                },
                onSave = { personName, amount, type, date, dueDate, notes ->
                    haptics.success()
                    if (editingEntry != null) {
                        hubViewModel.editEntry(
                            id = editingEntry!!.lending.id,
                            personName = personName,
                            amount = amount,
                            type = type,
                            date = date,
                            dueDate = dueDate,
                            notes = notes
                        )
                    } else {
                        hubViewModel.addEntry(
                            personName = personName,
                            amount = amount,
                            type = type,
                            date = date,
                            dueDate = dueDate,
                            notes = notes
                        )
                    }
                    showAddEntryDialog = false
                    editingEntry = null
                }
            )
        }

        // 2. Record Repayment Dialog
        if (repaymentDialogPerson != null) {
            val p = repaymentDialogPerson!!
            val specificEntry = repaymentDialogEntry
            val initialDir = specificEntry?.lending?.type ?: if (p.toReceive > 0) "LENT" else "BORROWED"
            val maxAllowed = specificEntry?.outstanding

            LendingRepaymentDialog(
                personName = p.displayName,
                toReceive = p.toReceive,
                toPay = p.toPay,
                initialDirection = initialDir,
                maxAllowed = maxAllowed,
                onDismiss = {
                    repaymentDialogPerson = null
                    repaymentDialogEntry = null
                },
                onConfirm = { direction, amount, date, note ->
                    haptics.success()
                    if (specificEntry != null) {
                        hubViewModel.repayEntry(specificEntry.lending.id, amount, date, note)
                    } else {
                        hubViewModel.repayPersonDues(p.displayName, direction, amount, date, note)
                    }
                    repaymentDialogPerson = null
                    repaymentDialogEntry = null
                }
            )
        }

        // 3. Rename Person Dialog
        if (renameDialogPerson != null) {
            LendingRenameDialog(
                currentName = renameDialogPerson!!.displayName,
                onDismiss = { renameDialogPerson = null },
                onConfirm = { newName ->
                    hubViewModel.requestRenamePerson(renameDialogPerson!!.displayName, newName)
                    renameDialogPerson = null
                }
            )
        }

        // 4. Settle All Confirmation Dialog
        if (settleAllDialogPerson != null) {
            LendingSettleAllDialog(
                person = settleAllDialogPerson!!,
                onDismiss = { settleAllDialogPerson = null },
                onConfirm = {
                    haptics.success()
                    hubViewModel.settleAllForPerson(settleAllDialogPerson!!.key)
                    settleAllDialogPerson = null
                }
            )
        }

        // 5. Merge Confirmation Dialog (from rename)
        hubState.pendingMergePrompt?.let { prompt ->
            LendingMergeConfirmationDialog(
                prompt = prompt,
                onDismiss = { hubViewModel.dismissMergePrompt() },
                onConfirm = { hubViewModel.confirmMergePerson() }
            )
        }

        // 6. Transaction Detail Window Dialog
        selectedTransactionForDetail?.let { entry ->
            LendingTransactionDetailDialog(
                entryWithRepayments = entry,
                onDismiss = { selectedTransactionForDetail = null },
                onRecordRepayment = {
                    val p = hubState.allPeopleSummaries.firstOrNull {
                        it.key == LendingHubViewModel.normalizePersonKey(entry.lending.personName)
                    }
                    selectedTransactionForDetail = null
                    repaymentDialogPerson = p
                    repaymentDialogEntry = entry
                },
                onSettleInFull = {
                    selectedTransactionForDetail = null
                    hubViewModel.repayEntry(entry.lending.id, entry.outstanding)
                },
                onReopen = {
                    selectedTransactionForDetail = null
                    hubViewModel.reopenLegacyEntry(entry.lending.id)
                },
                onEdit = {
                    selectedTransactionForDetail = null
                    editingEntry = entry
                    showAddEntryDialog = true
                },
                onDelete = {
                    selectedTransactionForDetail = null
                    hubViewModel.deleteEntry(entry)
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = "Entry deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            hubViewModel.undoDelete()
                        }
                    }
                },
                onUndoRepayment = {
                    selectedTransactionForDetail = null
                    hubViewModel.undoLastRepayment(entry.lending.id)
                },
                onForgiveDebt = {
                    selectedTransactionForDetail = null
                    hubViewModel.forgiveEntry(entry.lending.id)
                }
            )
        }

        // 7. UPI Settle Up Dialog
        if (upiDialogPerson != null) {
            val p = upiDialogPerson!!
            LendingUpiPayDialog(
                personName = p.displayName,
                amount = p.toPay,
                onDismiss = { upiDialogPerson = null },
                onMarkAsRepaid = {
                    hubViewModel.repayPersonDues(p.displayName, "BORROWED", p.toPay, System.currentTimeMillis(), "UPI payment")
                    upiDialogPerson = null
                },
                hazeState = lendingHazeState
            )
        }
    }

    // 6. Split a Bill Full Screen Overlay
    androidx.compose.animation.AnimatedVisibility(
        visible = showSplitSheet,
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) + androidx.compose.animation.fadeIn(tween(280)),
        exit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(320, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) + androidx.compose.animation.fadeOut(tween(220))
    ) {
        val existingPeople = remember(hubState.allPeopleSummaries) {
            hubState.allPeopleSummaries.map { it.displayName }
        }
        SplitExpenseScreen(
            onDismiss = { showSplitSheet = false },
            onSaveLenderSplit = { friendNames, perPersonShare, eventDescription ->
                viewModel.addSplitAsLender(friendNames, perPersonShare, eventDescription)
                showSplitSheet = false
            },
            onSaveBorrowerSplit = { payerName, userShare, eventDescription ->
                viewModel.addSplitAsBorrower(payerName, userShare, eventDescription)
                showSplitSheet = false
            },
            existingPeople = existingPeople
        )
    }
    }
}
