package com.omkarnub.kanri.ui.wallet

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDialog
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.ui.theme.kanriPressFeedback
import com.omkarnub.kanri.ui.theme.KanriMotion

enum class TransferDirection {
    WITHDRAW_TO_CASH, // ONLINE -> CASH
    DEPOSIT_TO_BANK   // CASH -> ONLINE
}

/**
 * Move Money Dialog.
 * Enables transferring money between Cash and Online (Bank/UPI) balances.
 * Creates a WALLET_TRANSFER transaction.
 */
@Composable
fun MoveMoneyDialog(
    onDismiss: () -> Unit,
    onConfirm: (fromWallet: String, toWallet: String, amount: Double, timestamp: Long, note: String?) -> Unit
) {
    val haptics = rememberKanriHaptics()
    var direction by remember { mutableStateOf(TransferDirection.WITHDRAW_TO_CASH) }
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull()
    val isValid = amount != null && amount > 0.0

    val fromWallet = if (direction == TransferDirection.WITHDRAW_TO_CASH) "ONLINE" else "CASH"
    val toWallet = if (direction == TransferDirection.WITHDRAW_TO_CASH) "CASH" else "ONLINE"

    KanriGlassDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.92f),
        shape = RoundedCornerShape(24.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Move Money",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
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

                // Direction Toggle (Withdraw Cash vs Deposit to Bank) with fluid Apple-grade color transitions
                val isWithdraw = direction == TransferDirection.WITHDRAW_TO_CASH
                val withdrawBg by animateColorAsState(
                    targetValue = if (isWithdraw) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "withdraw_bg"
                )
                val withdrawBorder by animateColorAsState(
                    targetValue = if (isWithdraw) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "withdraw_border"
                )
                val withdrawText by animateColorAsState(
                    targetValue = if (isWithdraw) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "withdraw_text"
                )
                val withdrawSubtext by animateColorAsState(
                    targetValue = if (isWithdraw) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "withdraw_subtext"
                )

                val isDeposit = direction == TransferDirection.DEPOSIT_TO_BANK
                val depositBg by animateColorAsState(
                    targetValue = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "deposit_bg"
                )
                val depositBorder by animateColorAsState(
                    targetValue = if (isDeposit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "deposit_border"
                )
                val depositText by animateColorAsState(
                    targetValue = if (isDeposit) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "deposit_text"
                )
                val depositSubtext by animateColorAsState(
                    targetValue = if (isDeposit) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    animationSpec = tween(180, easing = KanriMotion.EaseOut),
                    label = "deposit_subtext"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .kanriPressFeedback(
                                pressedScale = 0.96f,
                                onClick = { direction = TransferDirection.WITHDRAW_TO_CASH }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = withdrawBg,
                        border = BorderStroke(1.dp, withdrawBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ATM / Withdraw",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isWithdraw) FontWeight.Bold else FontWeight.Medium,
                                color = withdrawText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bank → Cash",
                                style = MaterialTheme.typography.labelSmall,
                                color = withdrawSubtext
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .kanriPressFeedback(
                                pressedScale = 0.96f,
                                onClick = { direction = TransferDirection.DEPOSIT_TO_BANK }
                            ),
                        shape = RoundedCornerShape(12.dp),
                        color = depositBg,
                        border = BorderStroke(1.dp, depositBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Deposit to Bank",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isDeposit) FontWeight.Bold else FontWeight.Medium,
                                color = depositText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cash → Bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = depositSubtext
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transfer Amount") },
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

                Spacer(modifier = Modifier.height(14.dp))

                // Note Field
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. ATM withdrawal, CDM deposit") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

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
                            if (isValid && !isSubmitting) {
                                isSubmitting = true
                                haptics.success()
                                onConfirm(
                                    fromWallet,
                                    toWallet,
                                    amount!!,
                                    System.currentTimeMillis(),
                                    noteStr.trim().ifBlank { null }
                                )
                            }
                        },
                        enabled = isValid && !isSubmitting,
                        modifier = Modifier.kanriPressFeedback(pressedScale = 0.96f, enabled = isValid && !isSubmitting),
                        shape = RoundedCornerShape(12.dp),
                        colors = kanriPrimaryButtonColors()
                    ) {
                        Text("Move Money", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
}
