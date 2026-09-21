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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.SavingsGoalEntity
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Savings Goals",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Target-based money trackers",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = { showAddGoalDialog = true }
                    ) {
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
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                                text = "Total Saved",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormat.format(stats.totalSaved),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Target",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currencyFormat.format(stats.totalTarget),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { stats.overallPercentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stats.overallPercentage.toInt()}% achieved",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${stats.completedGoals} of ${stats.totalGoals} goals reached",
                            fontSize = 12.sp,
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
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🎯",
                            fontSize = 40.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No savings goals yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Track funds for a new phone, trip, or emergency fund",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAddGoalDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Goal", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        val percent = SavingsGoalCalculator.calculateProgressPercentage(goal.currentAmount, goal.targetAmount)
                        val remaining = SavingsGoalCalculator.calculateRemainingAmount(goal.currentAmount, goal.targetAmount)
                        val isFinished = goal.isCompleted || (goal.targetAmount > 0 && goal.currentAmount >= goal.targetAmount)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            border = BorderStroke(
                                1.dp,
                                if (isFinished) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                            )
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
                                    size = 72.dp,
                                    strokeWidth = 6.dp
                                )

                                Spacer(modifier = Modifier.width(14.dp))

                                // Goal info
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = goal.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (isFinished) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Achieved! 🎉",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = currencyFormat.format(goal.currentAmount),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = " / ${currencyFormat.format(goal.targetAmount)}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    if (!isFinished && remaining > 0) {
                                        Text(
                                            text = "${currencyFormat.format(remaining)} left to save",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Actions
                                Column(horizontalAlignment = Alignment.End) {
                                    IconButton(
                                        onClick = { adjustTargetGoal = goal },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Deposit / Withdraw",
                                            tint = Color(0xFF00B4D8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    IconButton(
                                        onClick = { goalToDelete = goal },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Goal",
                                            tint = Color(0xFFE63946),
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
            title = { Text("Delete Savings Goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${target.title}\"? This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.savingsGoalDao().delete(target)
                        }
                        goalToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946))
                ) {
                    Text("Delete", color = Color.White)
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

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, target: Double, initial: Double, emoji: String, colorHex: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetAmountStr by remember { mutableStateOf("") }
    var initialDepositStr by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🎯") }
    val emojiOptions = listOf("🎯", "💻", "🏖️", "🚗", "🏠", "💍", "📱", "🎓", "🎮", "🛡️")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("New Savings Goal", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Name (e.g. New Laptop)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = targetAmountStr,
                    onValueChange = { targetAmountStr = it },
                    label = { Text("Target Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = initialDepositStr,
                    onValueChange = { initialDepositStr = it },
                    label = { Text("Initial Saved Amount (Optional ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Icon", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojiOptions) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetAmountStr.toDoubleOrNull() ?: 0.0
                    val initial = initialDepositStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && target > 0.0) {
                        onConfirm(title.trim(), target, initial, selectedEmoji, "#06D6A0")
                    }
                },
                enabled = title.isNotBlank() && (targetAmountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun AdjustGoalFundsDialog(
    goal: SavingsGoalEntity,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: Double, isCompleted: Boolean) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Deposit, 1: Withdraw
    var amountStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "${goal.emoji} ${goal.title}",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Deposit (+)") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Withdraw (-)") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Current: ₹${goal.currentAmount.toInt()} / Target: ₹${goal.targetAmount.toInt()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (selectedTab == 0) "Deposit Amount (₹)" else "Withdraw Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0.0) {
                        val newTotal = if (selectedTab == 0) {
                            SavingsGoalCalculator.calculateDeposit(goal.currentAmount, amount)
                        } else {
                            SavingsGoalCalculator.calculateWithdraw(goal.currentAmount, amount)
                        }
                        val isDone = newTotal >= goal.targetAmount
                        onConfirm(newTotal, isDone)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (selectedTab == 0) "Deposit" else "Withdraw",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
