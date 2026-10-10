package com.omkarnub.kanri.ui.home

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.IncomeGreen
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-window transaction detail page:
 * - Clean, monochrome, professional aesthetic matching Kanri's design system.
 * - Displays all metadata: hero amount, type badge, merchant, category, payment source,
 *   bank/account, ref/UTR, timestamp, notes, review status, and raw SMS.
 * - Complete modification options (edit amount, title, type, category, date, bank, ref, notes).
 * - Safe deletion option with confirmation alert.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onBack: () -> Unit,
    onDeleted: () -> Unit = onBack,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptics = rememberKanriHaptics()

    val txWithCategoryFlow = remember(transactionId) {
        viewModel.observeTransactionWithCategory(transactionId)
    }
    val item by txWithCategoryFlow.collectAsState(initial = null)
    val homeUiState by viewModel.uiState.collectAsState()

    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                com.omkarnub.kanri.ui.common.KanriWobbleLoader(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            val tx = item!!.transaction
            val category = item!!.category
            val isTransfer = tx.isTransfer
            val isDebit = tx.type.equals("DEBIT", ignoreCase = true)
            val merchantTitle = when {
                isTransfer -> "Transfer: ${tx.wallet} → ${tx.transferToWallet}"
                else -> tx.counterparty?.takeIf { it.isNotBlank() }
                    ?: tx.displayName?.takeIf { it.isNotBlank() }
                    ?: if (isDebit) "Expense" else "Income"
            }

            val formattedAmount = CurrencyUtils.formatCurrency(tx.amount)
            val signPrefix = when {
                isTransfer -> "⇄ "
                isDebit -> "- "
                else -> "+ "
            }

            var balanceAfter by remember { mutableStateOf<Double?>(null) }
            LaunchedEffect(tx.id, tx.wallet, tx.amount, tx.type, tx.timestamp, homeUiState.walletBalances) {
                balanceAfter = viewModel.getBalanceAfter(tx.wallet, tx)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // =================================================================
                // 1. TOP APP BAR
                // =================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable {
                                haptics.click()
                                onBack()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Title
                    Text(
                        text = "Transaction Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Action Buttons (Edit & Delete)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit / Modify Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable {
                                    haptics.click()
                                    showEditSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modify transaction",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Delete Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable {
                                    haptics.warning()
                                    showDeleteConfirmDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete transaction",
                                modifier = Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // =================================================================
                // 2. SCROLLABLE DETAILS CONTENT
                // =================================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // =============================================================
                    // HERO CARD: Amount, Type, Merchant, Full Date
                    // =============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Type Tag (Monochrome Badge)
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isTransfer) MaterialTheme.colorScheme.onSurfaceVariant
                                                else if (isDebit) ExpenseRed
                                                else IncomeGreen
                                            )
                                    )
                                    Text(
                                        text = if (isTransfer) "TRANSFER • ${tx.wallet} → ${tx.transferToWallet}"
                                            else if (isDebit) "EXPENSE • DEBIT"
                                            else "INCOME • CREDIT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.8.sp,
                                            fontSize = 10.5.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Large Hero Amount Display
                            Text(
                                text = "$signPrefix$formattedAmount",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 38.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Merchant / Payee
                            Text(
                                text = merchantTitle,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 19.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Formatted Timestamp
                            Text(
                                text = formatDetailedDate(tx.timestamp),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp
                                ),
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )

                            if (balanceAfter != null && homeUiState.walletBalances.isConfigured && tx.wallet != "NONE") {
                                Spacer(modifier = Modifier.height(10.dp))
                                val walletLabel = if (tx.wallet.equals("CASH", ignoreCase = true)) "Cash" else "Online"
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = "$walletLabel balance after this payment: ${CurrencyUtils.formatCurrency(balanceAfter!!)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // =============================================================
                    // QUICK ACTION BUTTONS (Modify & Delete)
                    // =============================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Modify Transaction Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptics.click()
                                    showEditSheet = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Modify Details",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Delete Transaction Button
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptics.warning()
                                    showDeleteConfirmDialog = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delete",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // =============================================================
                    // NEEDS REVIEW BANNER (if active)
                    // =============================================================
                    if (tx.needsReview) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Review",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Review Needed",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tx.reviewReason ?: "Verify category and counterparty",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.clickable {
                                        haptics.click()
                                        viewModel.clearReviewFlag(tx.id)
                                        Toast.makeText(context, "Review flag cleared", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(
                                        text = "Confirm",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Convert Action Banner for Cash Deposit or ATM Spending
                    val isCashDepositCandidate = (tx.reviewReason?.contains("cash deposit", ignoreCase = true) == true) ||
                        (!isTransfer && !isDebit && (tx.rawSms.contains("CDM", ignoreCase = true) || tx.rawSms.contains("cash deposit", ignoreCase = true) || tx.rawSms.contains("deposited in cash", ignoreCase = true)))
                    val isAtmWithdrawalCandidate = !isTransfer && tx.sourceType.equals("ATM", ignoreCase = true) && isDebit

                    if (isCashDepositCandidate) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Cash Deposit Detected",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Convert to transfer from Cash to Online to prevent double counting.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        haptics.click()
                                        viewModel.convertToTransfer(tx.id, "CASH", "ONLINE")
                                        Toast.makeText(context, "Converted to cash deposit transfer", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text("Convert", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    } else if (isAtmWithdrawalCandidate) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ATM Cash Withdrawal",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Convert to transfer from Online to Cash wallet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        haptics.click()
                                        viewModel.convertToTransfer(tx.id, "ONLINE", "CASH")
                                        Toast.makeText(context, "Converted to cash withdrawal transfer", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurface,
                                        contentColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Text("Convert", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    // =============================================================
                    // STRUCTURED DETAILS BENTO (All Transaction Metadata)
                    // =============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 1. Wallet Row
                            DetailRowItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Wallet",
                                value = when {
                                    isTransfer -> "Transfer (${tx.wallet} → ${tx.transferToWallet})"
                                    tx.wallet.equals("CASH", ignoreCase = true) -> "Cash"
                                    tx.wallet.equals("ONLINE", ignoreCase = true) -> "Online (Bank / UPI)"
                                    else -> "Not from balances (Excluded)"
                                },
                                trailingAction = if (!isTransfer) {
                                    {
                                        var showWalletMenu by remember { mutableStateOf(false) }
                                        Box {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.clickable {
                                                    haptics.click()
                                                    showWalletMenu = true
                                                }
                                            ) {
                                                Text(
                                                    text = "Change",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }

                                            androidx.compose.material3.DropdownMenu(
                                                expanded = showWalletMenu,
                                                onDismissRequest = { showWalletMenu = false }
                                            ) {
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text("Cash") },
                                                    onClick = {
                                                        haptics.click()
                                                        showWalletMenu = false
                                                        viewModel.updateTransactionWallet(tx.id, "CASH")
                                                    }
                                                )
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text("Online (Bank / UPI)") },
                                                    onClick = {
                                                        haptics.click()
                                                        showWalletMenu = false
                                                        viewModel.updateTransactionWallet(tx.id, "ONLINE")
                                                    }
                                                )
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text("Not from balances") },
                                                    onClick = {
                                                        haptics.click()
                                                        showWalletMenu = false
                                                        viewModel.updateTransactionWallet(tx.id, "NONE")
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else null
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 2. Category Row (Clickable to change category)
                            DetailRowItem(
                                icon = {
                                    if (category != null) {
                                        CategoryIcon(
                                            categoryName = category.name,
                                            iconName = category.iconName,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                label = "Category",
                                value = category?.name ?: "Uncategorized",
                                trailingAction = {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.clickable {
                                            haptics.click()
                                            showCategoryPickerSheet = true
                                        }
                                    ) {
                                        Text(
                                            text = if (category != null) "Change" else "+ Assign",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 2. Payment Source / Method
                            DetailRowItem(
                                icon = {
                                    Icon(
                                        imageVector = getSourceIcon(tx.sourceType),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Payment Method",
                                value = formatSourceType(tx.sourceType)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 3. Bank / Account
                            val accountMask = extractAccountMask(tx.rawSms)
                            val bankDisplay = buildString {
                                append(tx.bank?.takeIf { it.isNotBlank() } ?: "Not Specified")
                                if (!accountMask.isNullOrBlank()) {
                                    append(" ($accountMask)")
                                }
                            }
                            DetailRowItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Bank / Account",
                                value = bankDisplay
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 4. Reference / UTR Number
                            DetailRowItem(
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Reference / UTR",
                                value = tx.refNo?.takeIf { it.isNotBlank() } ?: "—",
                                trailingAction = if (!tx.refNo.isNullOrBlank()) {
                                    {
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(tx.refNo))
                                                haptics.click()
                                                Toast.makeText(context, "Reference number copied", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy Ref No",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                } else null
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            // 5. Creation Mode / Source
                            DetailRowItem(
                                icon = {
                                    Icon(
                                        imageVector = if (tx.isManualEntry) Icons.Default.Edit else Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = "Record Type",
                                value = if (tx.isManualEntry) "Manual Entry" else "SMS Auto-Detected"
                            )
                        }
                    }

                    // =============================================================
                    // USER NOTES SECTION
                    // =============================================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Notes & Remarks",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        haptics.click()
                                        showEditSheet = true
                                    }
                                ) {
                                    Text(
                                        text = if (tx.notes.isNullOrBlank()) "+ Add Note" else "Edit",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (!tx.notes.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = tx.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "No notes added for this transaction.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // =============================================================
                    // ORIGINAL NOTIFICATION / SMS CARD (if parsed from SMS)
                    // =============================================================
                    if (tx.rawSms.isNotBlank() && !tx.isManualEntry) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sms,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Original Bank Notification",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(tx.rawSms))
                                            haptics.click()
                                            Toast.makeText(context, "SMS text copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy SMS",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = tx.rawSms,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.5.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }

            // =================================================================
            // 3. EDIT TRANSACTION MODAL BOTTOM SHEET
            // =================================================================
            if (showEditSheet) {
                EditTransactionSheet(
                    transaction = tx,
                    currentCategory = category,
                    availableCategories = homeUiState.categories,
                    walletBalances = homeUiState.walletBalances,
                    onDismiss = { showEditSheet = false },
                    onSave = { updatedAmount, updatedType, updatedTitle, updatedCatId, updatedSource, updatedBank, updatedRef, updatedNotes, updatedTimestamp, updatedWallet ->
                        viewModel.updateTransaction(
                            transactionId = tx.id,
                            amount = updatedAmount,
                            type = updatedType,
                            counterparty = updatedTitle,
                            displayName = updatedTitle,
                            categoryId = updatedCatId,
                            sourceType = updatedSource,
                            bank = updatedBank,
                            refNo = updatedRef,
                            notes = updatedNotes,
                            timestamp = updatedTimestamp,
                            wallet = updatedWallet
                        )
                        showEditSheet = false
                        haptics.click()
                        Toast.makeText(context, "Transaction updated", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // =================================================================
            // 4. QUICK CATEGORY PICKER SHEET
            // =================================================================
            if (showCategoryPickerSheet) {
                CategoryPickerSheet(
                    targetTransaction = item!!,
                    categories = homeUiState.categories,
                    onDismiss = { showCategoryPickerSheet = false },
                    onCategorySelected = { catId, note ->
                        viewModel.assignCategory(
                            transactionId = tx.id,
                            counterparty = tx.counterparty,
                            categoryId = catId,
                            note = note,
                            bulkUpdate = false
                        )
                        showCategoryPickerSheet = false
                        haptics.click()
                        Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                    },
                    onOpenLendBorrow = {
                        showCategoryPickerSheet = false
                    }
                )
            }

            // =================================================================
            // 5. DELETE CONFIRMATION DIALOG
            // =================================================================
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = {
                        Text(
                            text = "Delete Transaction?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to delete this $formattedAmount transaction for \"$merchantTitle\"? This action cannot be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                haptics.warning()
                                showDeleteConfirmDialog = false
                                viewModel.deleteTransactionById(tx.id)
                                Toast.makeText(context, "Transaction deleted", Toast.LENGTH_SHORT).show()
                                onDeleted()
                            }
                        ) {
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirmDialog = false }
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }
    }
}

/**
 * Reusable row item for transaction properties in the details card.
 */
@Composable
private fun DetailRowItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    trailingAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingAction != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingAction()
        }
    }
}

/**
 * Clean, monochrome modal bottom sheet for modifying all details of a transaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTransactionSheet(
    transaction: TransactionEntity,
    currentCategory: CategoryEntity?,
    availableCategories: List<CategoryEntity>,
    walletBalances: com.omkarnub.kanri.data.wallet.WalletBalances? = null,
    onDismiss: () -> Unit,
    onSave: (
        amount: Double,
        type: String,
        counterparty: String,
        categoryId: Long?,
        sourceType: String,
        bank: String?,
        refNo: String?,
        notes: String?,
        timestamp: Long,
        wallet: String
    ) -> Unit
) {
    val haptics = rememberKanriHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current

    var amountText by remember { mutableStateOf(transaction.amount.toString()) }
    var selectedType by remember { mutableStateOf(transaction.type.uppercase()) }
    var titleText by remember { mutableStateOf(transaction.counterparty ?: transaction.displayName ?: "") }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var selectedSourceType by remember { mutableStateOf(transaction.sourceType.uppercase()) }
    var selectedWallet by remember { mutableStateOf(transaction.wallet.uppercase()) }
    var bankText by remember { mutableStateOf(transaction.bank ?: "") }
    var refNoText by remember { mutableStateOf(transaction.refNo ?: "") }
    var notesText by remember { mutableStateOf(transaction.notes ?: "") }
    var timestampMillis by remember { mutableLongStateOf(transaction.timestamp) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isDebit = selectedType.equals("DEBIT", ignoreCase = true)
    val sourceOptions = listOf("UPI", "CARD", "ATM", "BANK_TRANSFER", "CASH", "OTHER")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(100.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modify Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Expense vs Income Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable {
                            haptics.click()
                            selectedType = "DEBIT"
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isDebit) MaterialTheme.colorScheme.surfaceContainer else androidx.compose.ui.graphics.Color.Transparent,
                    border = if (isDebit) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null
                ) {
                    Text(
                        text = "Expense (Debit)",
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isDebit) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .clickable {
                            haptics.click()
                            selectedType = "CREDIT"
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = if (!isDebit) MaterialTheme.colorScheme.surfaceContainer else androidx.compose.ui.graphics.Color.Transparent,
                    border = if (!isDebit) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null
                ) {
                    Text(
                        text = "Income (Credit)",
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (!isDebit) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 2. Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount") },
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Merchant / Payee
            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                label = { Text("Merchant / Payee / Title") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Category Selector (Horizontal Scrolling Chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // None / Uncategorized chip
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = {
                            haptics.click()
                            selectedCategoryId = null
                        },
                        label = { Text("None") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    availableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryId == cat.id,
                            onClick = {
                                haptics.click()
                                selectedCategoryId = cat.id
                            },
                            label = { Text(cat.name) },
                            leadingIcon = {
                                CategoryIcon(
                                    categoryName = cat.name,
                                    iconName = cat.iconName,
                                    tint = if (selectedCategoryId == cat.id) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 5. Payment Source Type (Chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Payment Source",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sourceOptions.forEach { source ->
                        FilterChip(
                            selected = selectedSourceType.equals(source, ignoreCase = true),
                            onClick = {
                                haptics.click()
                                selectedSourceType = source
                            },
                            label = { Text(formatSourceType(source)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                                selectedLabelColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 5b. Wallet Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wallet",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedWallet == "CASH",
                        onClick = {
                            haptics.click()
                            selectedWallet = "CASH"
                        },
                        label = { Text("Cash") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = selectedWallet == "ONLINE",
                        onClick = {
                            haptics.click()
                            selectedWallet = "ONLINE"
                        },
                        label = { Text("Online (Bank / UPI)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = selectedWallet == "NONE",
                        onClick = {
                            haptics.click()
                            selectedWallet = "NONE"
                        },
                        label = { Text("Not from balances") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // 6. Bank Name & Reference Number (2 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = bankText,
                    onValueChange = { bankText = it },
                    label = { Text("Bank") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = refNoText,
                    onValueChange = { refNoText = it },
                    label = { Text("Ref / UTR") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // 7. Date Selector
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Date & Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = formatDetailedDate(timestampMillis),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            val openingTs = if (selectedWallet == "CASH") walletBalances?.cashOpeningTimestamp else walletBalances?.onlineOpeningTimestamp
            if (selectedWallet != "NONE" && openingTs != null && timestampMillis < openingTs) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Backdated entry: falls before wallet baseline was set; will not move balances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 8. Notes
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it },
                label = { Text("Notes / Remarks") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // 9. Save Changes Button
            Button(
                onClick = {
                    val parsedAmount = amountText.toDoubleOrNull() ?: transaction.amount
                    keyboardController?.hide()
                    onSave(
                        parsedAmount,
                        selectedType,
                        titleText.trim().ifEmpty { if (isDebit) "Expense" else "Income" },
                        selectedCategoryId,
                        selectedSourceType,
                        bankText.trim().ifEmpty { null },
                        refNoText.trim().ifEmpty { null },
                        notesText.trim().ifEmpty { null },
                        timestampMillis,
                        selectedWallet
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = timestampMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            timestampMillis = selected
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outline)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

private fun formatDetailedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("EEEE, d MMMM yyyy • h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatSourceType(sourceType: String): String {
    return when (sourceType.uppercase()) {
        "UPI" -> "UPI"
        "CARD" -> "Debit / Credit Card"
        "ATM" -> "Cash ATM"
        "BANK_TRANSFER" -> "Bank Transfer"
        "CASH" -> "Cash"
        else -> sourceType
    }
}

private fun extractAccountMask(rawSms: String): String? {
    return com.omkarnub.kanri.data.parser.SmsParser.extractAccount(rawSms)
}
