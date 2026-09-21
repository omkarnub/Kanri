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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.SavingsGoalEntity
import com.omkarnub.kanri.ui.theme.Panchang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GoalsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { KanriDatabase.getDatabase(context) }
    val goalsFlow = remember { db.savingsGoalDao().getAllGoals() }
    val goals by goalsFlow.collectAsState(initial = emptyList())

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: In Progress, 1: Completed
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var adjustTargetGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    val stats = remember(goals) {
        SavingsGoalCalculator.calculateOverallStats(goals)
    }

    val filteredGoals = remember(goals, selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> goals.filter { !it.isCompleted && !(it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
            1 -> goals.filter { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
            else -> goals
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
                            letterSpacing = 2.4.sp,
                            fontSize = 19.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Target-based savings tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Add Goal Button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showAddGoalDialog = true },
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
                            contentDescription = "Add Goal",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "New Goal",
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
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(stats.totalSaved),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOTAL TARGET",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currencyFormat.format(stats.totalTarget),
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
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stats.completedGoals} of ${stats.totalGoals} goals reached",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tabs: In Progress & Completed
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val inProgressCount = goals.count { !it.isCompleted && !(it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }
                    val completedCount = goals.count { it.isCompleted || (it.targetAmount > 0 && it.currentAmount >= it.targetAmount) }

                    listOf("In Progress ($inProgressCount)", "Completed ($completedCount)").forEachIndexed { index, label ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.surface
                                    else Color.Transparent
                                )
                                .clickable { selectedTabIndex = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Goals Feed
            if (filteredGoals.isEmpty()) {
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
                        Text(
                            text = "🎯",
                            fontSize = 38.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "No active goals" else "No completed goals yet",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedTabIndex == 0) "Track funds for a new laptop, trip, or emergency fund" else "Goals you achieve will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (selectedTabIndex == 0) {
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
                    items(filteredGoals, key = { it.id }) { goal ->
                        val percent = SavingsGoalCalculator.calculateProgressPercentage(goal.currentAmount, goal.targetAmount)
                        val remaining = SavingsGoalCalculator.calculateRemainingAmount(goal.currentAmount, goal.targetAmount)
                        val isFinished = goal.isCompleted || (goal.targetAmount > 0 && goal.currentAmount >= goal.targetAmount)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isFinished) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Gauge
                                CircularGoalProgressGauge(
                                    progressPercent = percent,
                                    emoji = goal.emoji,
                                    size = 68.dp,
                                    strokeWidth = 6.dp,
                                    primaryColor = MaterialTheme.colorScheme.onSurface,
                                    secondaryColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Goal info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
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
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = currencyFormat.format(goal.currentAmount),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = " / ${currencyFormat.format(goal.targetAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (!isFinished && remaining > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${currencyFormat.format(remaining)} left to save",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                // Actions
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = { adjustTargetGoal = goal },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Deposit / Withdraw",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { goalToDelete = goal },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Goal",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
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
        AddSavingsGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, targetAmount, initialDeposit, emoji, colorHex ->
                scope.launch(Dispatchers.IO) {
                    val isDone = initialDeposit >= targetAmount
                    db.savingsGoalDao().insert(
                        SavingsGoalEntity(
                            title = title,
                            targetAmount = targetAmount,
                            currentAmount = initialDeposit,
                            emoji = emoji,
                            colorHex = colorHex,
                            isCompleted = isDone
                        )
                    )
                }
                showAddGoalDialog = false
            }
        )
    }

    // Adjust Goal Dialog (Deposit / Withdraw)
    adjustTargetGoal?.let { target ->
        AdjustGoalFundsDialog(
            goal = target,
            onDismiss = { adjustTargetGoal = null },
            onConfirm = { newAmount, isCompleted ->
                scope.launch(Dispatchers.IO) {
                    db.savingsGoalDao().updateProgress(
                        id = target.id,
                        newAmount = newAmount,
                        isCompleted = isCompleted
                    )
                }
                adjustTargetGoal = null
            }
        )
    }

    // Delete Confirmation Dialog
    goalToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { goalToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    "Delete Savings Goal",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${target.title}\"? This cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.savingsGoalDao().delete(target)
                        }
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
