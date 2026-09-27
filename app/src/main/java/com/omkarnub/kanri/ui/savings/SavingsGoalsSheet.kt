package com.omkarnub.kanri.ui.savings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.SavingsGoalContributionEntity
import com.omkarnub.kanri.data.db.SavingsGoalEntity
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { KanriDatabase.getDatabase(context) }
    val goalsFlow = remember { db.savingsGoalDao().getAllGoals() }
    val goals by goalsFlow.collectAsState(initial = emptyList())

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    val stats = remember(goals) {
        SavingsGoalCalculator.calculateOverallStats(goals)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            GoalIcon(
                                iconKey = "savings",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Savings Goals",
                            fontFamily = GoogleSansFlex,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target-based money trackers",
                            fontFamily = GoogleSansFlex,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { showAddGoalDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Goal",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL SAVED",
                                fontFamily = GoogleSansFlex,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(stats.totalSaved),
                                fontFamily = GoogleSansFlex,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL TARGET",
                                fontFamily = GoogleSansFlex,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = currencyFormat.format(stats.totalTarget),
                                fontFamily = GoogleSansFlex,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (stats.overallPercentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.onSurface,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stats.overallPercentage.toInt()}% achieved",
                            fontFamily = GoogleSansFlex,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stats.completedGoals} of ${stats.totalGoals} goals reached",
                            fontFamily = GoogleSansFlex,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Goals Feed
            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GoalIcon(
                                    iconKey = "target",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No savings goals yet",
                            fontFamily = GoogleSansFlex,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track funds for a new laptop, trip, or emergency fund",
                            fontFamily = GoogleSansFlex,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAddGoalDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Create Goal",
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        val percent = SavingsGoalCalculator.calculateProgressPercentage(goal.currentAmount, goal.targetAmount)
                        val remaining = SavingsGoalCalculator.calculateRemainingAmount(goal.currentAmount, goal.targetAmount)
                        val isFinished = goal.isCompleted || (goal.targetAmount > 0 && goal.currentAmount >= goal.targetAmount)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { detailGoal = goal },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (isFinished) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // SVG Circular Gauge
                                CircularGoalProgressGauge(
                                    progressPercent = percent,
                                    iconKey = goal.emoji,
                                    size = 64.dp,
                                    strokeWidth = 6.dp
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Goal info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = goal.title,
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
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
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = currencyFormat.format(goal.currentAmount),
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = " / ${currencyFormat.format(goal.targetAmount)}",
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (!isFinished && remaining > 0) {
                                        Text(
                                            text = "${currencyFormat.format(remaining)} left to save",
                                            fontFamily = GoogleSansFlex,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { adjustFundsGoal = goal },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Deposit / Withdraw",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { goalToEdit = goal },
                                        modifier = Modifier.size(32.dp)
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
                    val newId = db.savingsGoalDao().insert(
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
                    if (initialDeposit > 0) {
                        db.savingsGoalDao().insertContribution(
                            SavingsGoalContributionEntity(
                                goalId = newId,
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

    // Adjust Goal Funds Dialog
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

    // Details Sheet
    detailGoal?.let { target ->
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
