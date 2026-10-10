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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.omkarnub.kanri.data.wallet.AtmWithdrawalMode
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.util.rememberKanriHaptics

/**
 * Dialog for initial balance configuration or editing opening balances.
 */
@Composable
fun WalletSetupDialog(
    initialCash: Double = 0.0,
    initialOnline: Double = 0.0,
    initialAtmMode: AtmWithdrawalMode = AtmWithdrawalMode.TRANSFER,
    isInitialSetup: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (cash: Double, online: Double, atmMode: AtmWithdrawalMode) -> Unit
) {
    val haptics = rememberKanriHaptics()
    var cashStr by remember {
        mutableStateOf(if (initialCash > 0) String.format(java.util.Locale.US, "%.2f", initialCash) else "")
    }
    var onlineStr by remember {
        mutableStateOf(if (initialOnline > 0) String.format(java.util.Locale.US, "%.2f", initialOnline) else "")
    }
    var atmMode by remember { mutableStateOf(initialAtmMode) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun parseAmount(str: String): Double? {
        val clean = str.trim().replace("₹", "").replace(",", "")
        if (clean.isBlank()) return 0.0
        return clean.toDoubleOrNull()
    }

    val cashParsed = parseAmount(cashStr)
    val onlineParsed = parseAmount(onlineStr)
    val isValid = cashParsed != null && cashParsed >= 0 && onlineParsed != null && onlineParsed >= 0

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
                            text = if (isInitialSetup) "Set Up Balances" else "Edit Opening Balances",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isInitialSetup) "Enter what you currently have" else "Reset your starting baseline",
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

                Spacer(modifier = Modifier.height(18.dp))

                // Cash Balance
                OutlinedTextField(
                    value = cashStr,
                    onValueChange = { cashStr = it },
                    label = { Text("Cash in hand") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0.00") },
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

                // Online Balance
                OutlinedTextField(
                    value = onlineStr,
                    onValueChange = { onlineStr = it },
                    label = { Text("Online / Bank / UPI Balance") },
                    prefix = { Text("₹", fontWeight = FontWeight.Bold) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ATM Mode Choice
                Text(
                    text = "ATM WITHDRAWALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tick()
                                atmMode = AtmWithdrawalMode.TRANSFER
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = atmMode == AtmWithdrawalMode.TRANSFER,
                            onClick = {
                                haptics.tick()
                                atmMode = AtmWithdrawalMode.TRANSFER
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Move money to Cash (recommended)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Debits bank and credits Cash balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.tick()
                                atmMode = AtmWithdrawalMode.SPENDING
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = atmMode == AtmWithdrawalMode.SPENDING,
                            onClick = {
                                haptics.tick()
                                atmMode = AtmWithdrawalMode.SPENDING
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Count as spending",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Treated as a regular expense from Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions
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
                            if (isValid && !isSubmitting) {
                                isSubmitting = true
                                haptics.success()
                                val roundedCash = WalletBalanceCalculator.roundTo2Decimals(cashParsed!!)
                                val roundedOnline = WalletBalanceCalculator.roundTo2Decimals(onlineParsed!!)
                                onConfirm(roundedCash, roundedOnline, atmMode)
                            }
                        },
                        enabled = isValid && !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text("Save Balances", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
