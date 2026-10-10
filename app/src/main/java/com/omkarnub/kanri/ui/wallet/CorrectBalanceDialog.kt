package com.omkarnub.kanri.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlin.math.abs

/**
 * Dialog to correct the current balance of a wallet (Cash or Online).
 * Updates opening_amount via WalletBalanceCalculator.correctionToOpening, keeping opening_timestamp.
 */
@Composable
fun CorrectBalanceDialog(
    wallet: String, // "CASH" or "ONLINE"
    currentBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (desiredBalance: Double) -> Unit
) {
    val haptics = rememberKanriHaptics()
    var inputAmountStr by remember {
        mutableStateOf(if (currentBalance >= 0) String.format(java.util.Locale.US, "%.2f", currentBalance) else String.format(java.util.Locale.US, "%.2f", currentBalance))
    }
    var isSubmitting by remember { mutableStateOf(false) }

    val walletDisplayName = if (wallet.equals("CASH", ignoreCase = true)) "Cash" else "Online (Bank/UPI)"

    val parsedAmount = inputAmountStr.toDoubleOrNull()
    val difference = if (parsedAmount != null) WalletBalanceCalculator.roundTo2Decimals(parsedAmount - currentBalance) else null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Correct $walletDisplayName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Current calculated: ${formatCurrency(currentBalance)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.tick()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Amount Text Field
                OutlinedTextField(
                    value = inputAmountStr,
                    onValueChange = { inputAmountStr = it },
                    label = { Text("Actual Current Balance") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stepper Quick Chips (-500, -100, +100, +500)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(-500.0, -100.0, 100.0, 500.0).forEach { delta ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptics.tick()
                                    val cur = inputAmountStr.toDoubleOrNull() ?: currentBalance
                                    val updated = WalletBalanceCalculator.roundTo2Decimals(cur + delta)
                                    inputAmountStr = String.format(java.util.Locale.US, "%.2f", updated)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = if (delta > 0) "+${delta.toInt()}" else "${delta.toInt()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Difference Preview
                if (difference != null) {
                    val isPositive = difference > 0.001
                    val isZero = abs(difference) < 0.005
                    val diffColor = if (isZero) MaterialTheme.colorScheme.onSurfaceVariant else if (isPositive) IncomeGreen else ExpenseRed

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Adjustment needed:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = if (isZero) "No change" else if (isPositive) "+${formatCurrency(difference)}" else "-${formatCurrency(abs(difference))}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = diffColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (parsedAmount != null && !isSubmitting) {
                                isSubmitting = true
                                haptics.success()
                                onConfirm(parsedAmount)
                            }
                        },
                        enabled = parsedAmount != null && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Save Correction", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
