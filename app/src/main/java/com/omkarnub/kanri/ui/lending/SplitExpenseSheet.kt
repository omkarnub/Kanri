package com.omkarnub.kanri.ui.lending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.home.formatCurrency

enum class SplitRole {
    I_PAID,         // User paid total -> collects from friends (Money I Lent)
    SOMEONE_ELSE    // Friend paid total -> user owes their share (Money I Borrowed)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSaveLenderSplit: (friendNames: List<String>, perPersonShare: Double, eventDescription: String) -> Unit,
    onSaveBorrowerSplit: (payerName: String, userShare: Double, eventDescription: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var peopleCount by remember { mutableIntStateOf(3) }
    var eventDescription by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(SplitRole.I_PAID) }
    var payerName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dynamic names list for other friends when user paid full bill
    val friendNames = remember { mutableStateListOf("Friend 1", "Friend 2") }

    fun syncFriendNames(newCount: Int) {
        val needed = (newCount - 1).coerceAtLeast(1)
        while (friendNames.size < needed) {
            friendNames.add("Friend ${friendNames.size + 1}")
        }
        while (friendNames.size > needed) {
            friendNames.removeAt(friendNames.size - 1)
        }
    }

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0
    val perPersonShare = if (peopleCount > 0) totalAmount / peopleCount else 0.0
    val othersTotal = perPersonShare * (peopleCount - 1)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "⚡ Split a Group Expense",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "Divide bills with an animated share breakdown and log debts in 1 tap.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Animated Circle Gauge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                SplitCircleGauge(
                    totalAmount = totalAmount,
                    peopleCount = peopleCount
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("Total Bill Amount") },
                placeholder = { Text("e.g. 1200") },
                prefix = { Text("₹ ", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Number of People Stepper & Quick Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Split Between",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$peopleCount People (You + ${peopleCount - 1})",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (peopleCount > 2) {
                                peopleCount--
                                syncFriendNames(peopleCount)
                            }
                        },
                        enabled = peopleCount > 2,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (peopleCount > 2) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease people",
                            tint = if (peopleCount > 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "$peopleCount",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (peopleCount < 12) {
                                peopleCount++
                                syncFriendNames(peopleCount)
                            }
                        },
                        enabled = peopleCount < 12,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (peopleCount < 12) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase people",
                            tint = if (peopleCount < 12) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Chips: 2, 3, 4, 5 people
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(2, 3, 4, 5).forEach { count ->
                    val isSelected = peopleCount == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                peopleCount = count
                                syncFriendNames(count)
                            }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count People",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description / Note
            OutlinedTextField(
                value = eventDescription,
                onValueChange = { eventDescription = it },
                label = { Text("Event / Note (Optional)") },
                placeholder = { Text("e.g. Dinner, Pizza night, Uber trip") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Who Paid Toggle Cards
            Text(
                text = "Who Paid the Bill?",
                color = Color(0xFF8B949E),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Option A: I Paid
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (selectedRole == SplitRole.I_PAID) Color(0xFF2ECC71) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedRole = SplitRole.I_PAID },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == SplitRole.I_PAID) Color(0x222ECC71) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🤝 I Paid Full Bill",
                            color = if (selectedRole == SplitRole.I_PAID) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Others owe me money back",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // Option B: Someone Else Paid
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            1.dp,
                            if (selectedRole == SplitRole.SOMEONE_ELSE) Color(0xFFE74C3C) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedRole = SplitRole.SOMEONE_ELSE },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedRole == SplitRole.SOMEONE_ELSE) Color(0x22E74C3C) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📥 Someone Else Paid",
                            color = if (selectedRole == SplitRole.SOMEONE_ELSE) Color(0xFFE74C3C) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "I owe my single share",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Participant Input
            if (selectedRole == SplitRole.I_PAID) {
                Text(
                    text = "Who is splitting with you? (${friendNames.size} friends)",
                    color = Color(0xFF8B949E),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                friendNames.forEachIndexed { index, name ->
                    OutlinedTextField(
                        value = name,
                        onValueChange = { friendNames[index] = it },
                        label = { Text("Friend ${index + 1} Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }
            } else {
                OutlinedTextField(
                    value = payerName,
                    onValueChange = { payerName = it },
                    label = { Text("Who Paid the Bill?") },
                    placeholder = { Text("e.g. Rahul, Sneha, Amit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFFF6B6B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Action Button (Save to Ledger)
            Button(
                onClick = {
                    if (totalAmount <= 0.0) {
                        errorMessage = "Please enter a valid bill amount"
                        return@Button
                    }
                    if (selectedRole == SplitRole.I_PAID) {
                        onSaveLenderSplit(friendNames.toList(), perPersonShare, eventDescription)
                        onDismiss()
                    } else {
                        val name = payerName.trim().ifBlank { "Friend" }
                        onSaveBorrowerSplit(name, perPersonShare, eventDescription)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == SplitRole.I_PAID) Color(0xFF238636) else Color(0xFFDA3633)
                )
            ) {
                Text(
                    text = if (selectedRole == SplitRole.I_PAID) {
                        "Collect ${formatCurrency(othersTotal)} (${friendNames.size} friends) → Money I Lent"
                    } else {
                        "Owe ${formatCurrency(perPersonShare)} → Money I Borrowed"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action: Don't Add (Just Calculate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Don't Add to Ledger (Just Calculate)",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
