package com.omkarnub.kanri.ui.savings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.data.db.SavingsGoalContributionEntity
import com.omkarnub.kanri.data.db.SavingsGoalEntity
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.Panchang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GoalSortOption(val label: String) {
    RECENT("Recently Added"),
    PROGRESS_DESC("Highest Progress"),
    DEADLINE("Closest Deadline"),
    TARGET_DESC("Target: High to Low")
}

@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { KanriDatabase.getDatabase(context) }
    val goalsFlow = remember { db.savingsGoalDao().getAllGoals() }
    val goals by goalsFlow.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: In Progress, 1: Completed, 2: All
    var selectedSort by remember { mutableStateOf(GoalSortOption.RECENT) }
    var isSortMenuOpen by remember { mutableStateOf(false) }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var adjustFundsGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var detailGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }
    val deadlineFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    val stats = remember(goals) {
        SavingsGoalCalculator.calculateOverallStats(goals)
    }

    // Filter by Tab: 0: In Progress, 1: Completed, 2: All
    val tabFilteredGoals = remember(goals, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> goals.filter { !it.isCompleted && !(it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
            1 -> goals.filter { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
            else -> goals
        }
    }

    // Sort according to selectedSort
    val sortedGoals = remember(tabFilteredGoals, selectedSort) {
        when (selectedSort) {
            GoalSortOption.RECENT -> tabFilteredGoals.sortedByDescending { it.id }
            GoalSortOption.PROGRESS_DESC -> tabFilteredGoals.sortedByDescending {
                SavingsGoalCalculator.calculateProgressPercentage(it.currentAmount, it.targetAmount)
            }
            GoalSortOption.DEADLINE -> tabFilteredGoals.sortedWith(
                compareBy<SavingsGoalEntity> { it.targetDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.id }
            )
            GoalSortOption.TARGET_DESC -> tabFilteredGoals.sortedByDescending { it.targetAmount }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOALS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target-based money trackers",
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Add Goal Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.primaryAction()
                            showAddGoalDialog = true
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Goal",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "New Goal",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SAVED",
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(stats.totalSaved),
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL TARGET",
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(stats.totalTarget),
                                fontFamily = GoogleSansFlex,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LinearProgressIndicator(
                        progress = { (stats.overallPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stats.overallPercentage.toInt()}% achieved",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stats.completedGoals} of ${stats.totalGoals} goals reached",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs & Sort Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Tabs: In Progress, Completed, All
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(3.dp)
                    ) {
                        val inProgressCount = goals.count { !it.isCompleted && !(it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
                        val completedCount = goals.count { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }

                        listOf(
                            "Active ($inProgressCount)" to 0,
                            "Done ($completedCount)" to 1,
                            "All (${goals.size})" to 2
                        ).forEach { (label, index) ->
                            val isSelected = selectedTabIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.surface
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (selectedTabIndex != index) {
                                            haptics.tick()
                                            selectedTabIndex = index
                                        }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Sort Dropdown Button
                Box {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                haptics.tick()
                                isSortMenuOpen = true
                            }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort goals",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isSortMenuOpen,
                        onDismissRequest = { isSortMenuOpen = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        GoalSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = if (selectedSort == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSort == option) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    haptics.tick()
                                    selectedSort = option
                                    isSortMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Goals Feed
            if (sortedGoals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GoalIcon(
                                    iconKey = "target",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "No active goals" else if (selectedTabIndex == 1) "No completed goals yet" else "No goals created yet",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "Set targets for a new laptop, emergency fund, or dream trip" else "Goals you achieve will appear here",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (selectedTabIndex != 1) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showAddGoalDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "+ Create Goal",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.surface
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(sortedGoals, key = { it.id }) { goal ->
                        val percent = SavingsGoalCalculator.calculateProgressPercentage(goal.currentAmount, goal.targetAmount)
                        val remaining = SavingsGoalCalculator.calculateRemainingAmount(goal.currentAmount, goal.targetAmount)
                        val isFinished = goal.isCompleted || (goal.targetAmount > 0 && goal.currentAmount >= goal.targetAmount)
                        val daysRemaining = remember(goal.targetDate) { SavingsGoalCalculator.calculateDaysRemaining(goal.targetDate) }
                        val monthlyPace = remember(goal) { SavingsGoalCalculator.calculateRequiredMonthlySavings(goal.currentAmount, goal.targetAmount, goal.targetDate) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    haptics.click()
                                    detailGoal = goal
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isFinished) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Crisp SVG circular gauge
                                CircularGoalProgressGauge(
                                    progressPercent = percent,
                                    iconKey = goal.emoji,
                                    size = 68.dp,
                                    strokeWidth = 6.dp,
                                    primaryColor = MaterialTheme.colorScheme.onSurface,
                                    secondaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Goal information
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.title,
                                            fontFamily = GoogleSansFlex,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isFinished) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "Achieved! 🎉",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontFamily = GoogleSansFlex,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    // Amount saved / target
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = currencyFormat.format(goal.currentAmount),
                                            fontFamily = GoogleSansFlex,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = " / ${currencyFormat.format(goal.targetAmount)}",
                                            fontFamily = GoogleSansFlex,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Target date or remaining pace
                                    if (!isFinished) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val subline = buildString {
                                            if (remaining > 0) append("${currencyFormat.format(remaining)} left")
                                            if (monthlyPace != null && monthlyPace > 0) {
                                                if (isNotEmpty()) append(" · ")
                                                append("₹${monthlyPace.toInt()}/mo")
                                            } else if (daysRemaining != null) {
                                                if (isNotEmpty()) append(" · ")
                                                append(if (daysRemaining > 0) "$daysRemaining days left" else "Due")
                                            }
                                        }
                                        if (subline.isNotBlank()) {
                                            Text(
                                                text = subline,
                                                fontFamily = GoogleSansFlex,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }

                                // Quick Actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Deposit / Withdraw Button
                                    IconButton(
                                        onClick = {
                                            haptics.click()
                                            adjustFundsGoal = goal
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Deposit / Withdraw",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    // Edit Goal Button
                                    IconButton(
                                        onClick = {
                                            haptics.click()
                                            goalToEdit = goal
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Goal",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddEditSavingsGoalDialog(
            goalToEdit = null,
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, targetAmount, initialDeposit, iconKey, targetDate ->
                scope.launch(Dispatchers.IO) {
                    val isDone = targetAmount > 0 && initialDeposit >= targetAmount
                    val newGoalId = db.savingsGoalDao().insert(
                        SavingsGoalEntity(
                            title = title,
                            targetAmount = targetAmount,
                            currentAmount = initialDeposit,
                            emoji = iconKey,
                            colorHex = "#06D6A0",
                            targetDate = targetDate,
                            isCompleted = isDone
                        )
                    )
                    // If initial deposit > 0, log it in contributions
                    if (initialDeposit > 0) {
                        db.savingsGoalDao().insertContribution(
                            SavingsGoalContributionEntity(
                                goalId = newGoalId,
                                amount = initialDeposit,
                                timestamp = System.currentTimeMillis(),
                                note = "Initial Deposit"
                            )
                        )
                    }
                }
                showAddGoalDialog = false
            }
        )
    }

    // Edit Goal Dialog
    goalToEdit?.let { target ->
        AddEditSavingsGoalDialog(
            goalToEdit = target,
            onDismiss = { goalToEdit = null },
            onConfirm = { title, targetAmount, currentSaved, iconKey, targetDate ->
                scope.launch(Dispatchers.IO) {
                    val isDone = targetAmount > 0 && currentSaved >= targetAmount
                    db.savingsGoalDao().update(
                        target.copy(
                            title = title,
                            targetAmount = targetAmount,
                            currentAmount = currentSaved,
                            emoji = iconKey,
                            targetDate = targetDate,
                            isCompleted = isDone
                        )
                    )
                }
                goalToEdit = null
            }
        )
    }

    // Adjust Goal Funds Dialog (Deposit / Withdraw with note & history logging)
    adjustFundsGoal?.let { target ->
        AdjustGoalFundsDialog(
            goal = target,
            onDismiss = { adjustFundsGoal = null },
            onConfirm = { amount, isDeposit, note ->
                scope.launch(Dispatchers.IO) {
                    val newTotal = if (isDeposit) {
                        SavingsGoalCalculator.calculateDeposit(target.currentAmount, amount)
                    } else {
                        SavingsGoalCalculator.calculateWithdraw(target.currentAmount, amount)
                    }
                    val isDone = target.targetAmount > 0 && newTotal >= target.targetAmount

                    db.savingsGoalDao().updateProgress(
                        id = target.id,
                        newAmount = newTotal,
                        isCompleted = isDone
                    )

                    // Record contribution entry
                    db.savingsGoalDao().insertContribution(
                        SavingsGoalContributionEntity(
                            goalId = target.id,
                            amount = if (isDeposit) amount else -amount,
                            timestamp = System.currentTimeMillis(),
                            note = note ?: if (isDeposit) "Deposit" else "Withdrawal"
                        )
                    )
                }
                adjustFundsGoal = null
            }
        )
    }

    // Goal Details & Deep Insights Sheet
    detailGoal?.let { target ->
        // Refresh goal state live from goals list if it was updated
        val liveTarget = goals.find { it.id == target.id } ?: target
        GoalDetailsSheet(
            goal = liveTarget,
            onDismiss = { detailGoal = null },
            onAdjustFunds = {
                detailGoal = null
                adjustFundsGoal = liveTarget
            },
            onEditGoal = {
                detailGoal = null
                goalToEdit = liveTarget
            },
            onDeleteGoal = {
                detailGoal = null
                goalToDelete = liveTarget
            }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { target ->
        DeleteGoalConfirmDialog(
            goal = target,
            onDismiss = { goalToDelete = null },
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    db.savingsGoalDao().delete(target)
                }
                goalToDelete = null
            }
        )
    }
}
