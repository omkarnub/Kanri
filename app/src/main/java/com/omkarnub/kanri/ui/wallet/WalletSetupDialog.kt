package com.omkarnub.kanri.ui.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.omkarnub.kanri.data.wallet.AtmWithdrawalMode
import com.omkarnub.kanri.data.wallet.WalletBalanceCalculator
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics

private enum class WalletSetupTab {
    CASH, ONLINE
}

/**
 * Wallet Setup Dialog matching the exact aesthetics and animations of SetBudgetDialog:
 * 1. Frosted glass appearance matching luxury theme.
 * 2. Spacious 92% screen width with zero clipping.
 * 3. Rolling animated number with [-] and [+] stepping buttons.
 * 4. Tapping on the number allows manual typing with autofocus.
 * 5. Small uniformly arranged quick increment buttons: +500, +1000, +2000, +5000.
 * 6. Dual Cash & Online selector pills with smooth switching.
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
    val glassTheme = rememberKanriGlassTheme()
    val haptics = rememberKanriHaptics()

    var currentTab by remember { mutableStateOf(WalletSetupTab.CASH) }
    var cashAmount by remember(initialCash) {
        mutableDoubleStateOf(if (initialCash > 0) initialCash else 2000.0)
    }
    var onlineAmount by remember(initialOnline) {
        mutableDoubleStateOf(if (initialOnline > 0) initialOnline else 25000.0)
    }
    var atmMode by remember(initialAtmMode) { mutableStateOf(initialAtmMode) }

    val currentAmount = if (currentTab == WalletSetupTab.CASH) cashAmount else onlineAmount
    var isManualInput by remember { mutableStateOf(false) }
    var customText by remember(currentTab, currentAmount) {
        mutableStateOf(currentAmount.toLong().toString())
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
        }
    }

    fun updateCurrentAmount(newAmount: Double) {
        val safe = newAmount.coerceAtLeast(0.0)
        if (currentTab == WalletSetupTab.CASH) {
            cashAmount = safe
        } else {
            onlineAmount = safe
        }
        customText = safe.toLong().toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = glassTheme.fallbackBackgroundColor.copy(alpha = 0.93f),
            border = glassTheme.glassBorder,
            shadowElevation = glassTheme.shadowElevation + 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = if (isInitialSetup) {
                        if (currentTab == WalletSetupTab.CASH) "Set opening Cash balance" else "Set opening Online balance"
                    } else {
                        if (currentTab == WalletSetupTab.CASH) "Edit Cash Balance" else "Edit Bank Balance"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (currentTab == WalletSetupTab.CASH) {
                        "How much physical cash do you have in hand right now? Kanri tracks paper cash spent live."
                    } else {
                        "How much money do you currently have in bank accounts & UPI? Kanri deducts this from SMS."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                )

                // -------------------------------------------------------------
                // Segmented Step Tabs: Cash in Hand vs Online / Bank
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Cash Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptics.tick()
                                currentTab = WalletSetupTab.CASH
                                customText = cashAmount.toLong().toString()
                                isManualInput = false
                            },
                        color = if (currentTab == WalletSetupTab.CASH)
                            MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        border = if (currentTab == WalletSetupTab.CASH)
                            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "💵 1. Cash in Hand",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (currentTab == WalletSetupTab.CASH) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (currentTab == WalletSetupTab.CASH)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(cashAmount, includeDecimals = false),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTab == WalletSetupTab.CASH)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Online Tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptics.tick()
                                currentTab = WalletSetupTab.ONLINE
                                customText = onlineAmount.toLong().toString()
                                isManualInput = false
                            },
                        color = if (currentTab == WalletSetupTab.ONLINE)
                            MaterialTheme.colorScheme.surface
                        else Color.Transparent,
                        shape = RoundedCornerShape(10.dp),
                        border = if (currentTab == WalletSetupTab.ONLINE)
                            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🏦 2. Online / Bank",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (currentTab == WalletSetupTab.ONLINE) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (currentTab == WalletSetupTab.ONLINE)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(onlineAmount, includeDecimals = false),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = if (currentTab == WalletSetupTab.ONLINE)
                                    MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // -------------------------------------------------------------
                // Central Animated Digit with [-] and [+] Stepping Buttons
                // -------------------------------------------------------------
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Decrease Button [-]
                        IconButton(
                            onClick = {
                                haptics.tick()
                                val step = if (currentTab == WalletSetupTab.CASH) 500.0 else 1000.0
                                updateCurrentAmount(currentAmount - step)
                                isManualInput = false
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Amount",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Central Number / Tap to Edit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isManualInput) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = {
                                        val filtered = it.filter { ch -> ch.isDigit() }
                                        customText = filtered
                                        val parsed = filtered.toDoubleOrNull()
                                        if (parsed != null && parsed >= 0) {
                                            if (currentTab == WalletSetupTab.CASH) cashAmount = parsed else onlineAmount = parsed
                                        }
                                    },
                                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val parsed = customText.toDoubleOrNull()
                                            if (parsed != null && parsed >= 0) {
                                                if (currentTab == WalletSetupTab.CASH) cashAmount = parsed else onlineAmount = parsed
                                            }
                                            isManualInput = false
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            customText = currentAmount.toLong().toString()
                                            isManualInput = true
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    val formattedStr = CurrencyUtils.formatCurrency(currentAmount, includeDecimals = false)
                                    val fontSize = when {
                                        formattedStr.length > 10 -> 20.sp
                                        formattedStr.length > 7 -> 24.sp
                                        else -> 28.sp
                                    }
                                    AnimatedNumberText(
                                        text = formattedStr,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = fontSize,
                                        animateFromZero = false
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Tap to edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Tap to type",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        // Increase Button [+]
                        IconButton(
                            onClick = {
                                haptics.tick()
                                val step = if (currentTab == WalletSetupTab.CASH) 500.0 else 1000.0
                                updateCurrentAmount(currentAmount + step)
                                isManualInput = false
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Amount",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // -------------------------------------------------------------
                // Quick Increment Buttons: +500, +1000, +2000, +5000
                // -------------------------------------------------------------
                val quickIncrements = if (currentTab == WalletSetupTab.CASH) {
                    listOf(100.0, 500.0, 1000.0, 2000.0)
                } else {
                    listOf(500.0, 1000.0, 5000.0, 10000.0)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickIncrements.forEach { inc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    haptics.tick()
                                    updateCurrentAmount(currentAmount + inc)
                                    isManualInput = false
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+₹%,d".format(inc.toInt()),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // -------------------------------------------------------------
                // ATM Withdrawals Option (Minimalist)
                // -------------------------------------------------------------
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.tick()
                            atmMode = if (atmMode == AtmWithdrawalMode.TRANSFER) AtmWithdrawalMode.SPENDING else AtmWithdrawalMode.TRANSFER
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ATM Withdrawals",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (atmMode == AtmWithdrawalMode.TRANSFER) "Auto-transfers bank withdrawal to Cash" else "Counts withdrawal as expense",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = atmMode == AtmWithdrawalMode.TRANSFER,
                            onCheckedChange = {
                                haptics.tick()
                                atmMode = if (it) AtmWithdrawalMode.TRANSFER else AtmWithdrawalMode.SPENDING
                            },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // -------------------------------------------------------------
                // Action Buttons: Maybe Later / Next & Save Balances
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentTab == WalletSetupTab.CASH) {
                        TextButton(onClick = {
                            haptics.tick()
                            onDismiss()
                        }) {
                            Text(
                                text = "Maybe Later",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                haptics.tick()
                                currentTab = WalletSetupTab.ONLINE
                                customText = onlineAmount.toLong().toString()
                                isManualInput = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Next: Online →",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    } else {
                        TextButton(onClick = {
                            haptics.tick()
                            currentTab = WalletSetupTab.CASH
                            customText = cashAmount.toLong().toString()
                            isManualInput = false
                        }) {
                            Text(
                                text = "← Back to Cash",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                haptics.success()
                                val roundedCash = WalletBalanceCalculator.roundTo2Decimals(cashAmount)
                                val roundedOnline = WalletBalanceCalculator.roundTo2Decimals(onlineAmount)
                                onConfirm(roundedCash, roundedOnline, atmMode)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Save Balances",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
