package com.omkarnub.kanri.ui.lending

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.util.CurrencyUtils
import java.util.Locale
import kotlin.math.abs

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

/**
 * GPay-style Person Ledger & Detail Stream:
 * 1. Header with back button, avatar, tap-to-rename, and net rolling balance.
 * 2. Quick action toolbar: Lent, Borrowed, Repay, Remind, Settle All.
 * 3. Compact GPay send & receive transaction bubbles (Right-aligned green for Lent, Left-aligned red for Borrowed).
 * 4. Tapping any transaction opens the full transaction detail window.
 */
@Composable
fun LendingPersonDetailView(
    person: PersonSummary,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onAddLentClick: () -> Unit,
    onAddBorrowedClick: () -> Unit,
    onRecordRepaymentClick: () -> Unit,
    onSettleAllClick: () -> Unit,
    onPayUpiClick: () -> Unit = {},
    onTransactionClick: (LendingWithRepayments) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val allEntries = remember(person) {
        (person.openEntries + person.settledEntries).sortedBy { it.lending.date }
    }

    val listState = rememberLazyListState()

    // Auto-scroll to the latest transactions at the bottom like GPay/chat profile
    LaunchedEffect(person.key, allEntries.size) {
        if (allEntries.isNotEmpty()) {
            listState.scrollToItem(allEntries.size)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Pinned Top Header Bar: Back arrow, Person avatar & name, net amount
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Avatar
                    val initialLetter = person.displayName.trim().take(1).uppercase(Locale.ROOT).ifBlank { "?" }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetter,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Name + Tap to Rename
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onRenameClick)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = person.displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename person",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Net Balance Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    val isPositive = person.net > 0
                    val isNegative = person.net < 0
                    val amountColor = if (isPositive) SageGreen else if (isNegative) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                    val sign = if (isPositive) "+" else if (isNegative) "−" else ""
                    val caption = if (isPositive) "You'll get" else if (isNegative) "You owe" else "Settled"

                    Text(
                        text = caption,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sign.isNotEmpty()) {
                            Text(
                                text = sign,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = amountColor
                            )
                        }
                        AnimatedNumberText(
                            text = CurrencyUtils.formatCurrency(abs(person.net)),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = amountColor,
                            animateFromZero = false
                        )
                    }
                }
            }

        // 2. Pinned Action Buttons Row: Lent, Borrowed, Repay, Remind, Settle All
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // Lent CTA
                ActionButton(
                    icon = Icons.Default.Add,
                    label = "Lent",
                    onClick = onAddLentClick,
                    modifier = Modifier.weight(1f)
                )

                // Borrowed CTA
                ActionButton(
                    icon = Icons.Default.Add,
                    label = "Borrowed",
                    onClick = onAddBorrowedClick,
                    modifier = Modifier.weight(1f)
                )

                // Record Repayment
                if (person.openCount > 0) {
                    ActionButton(
                        icon = Icons.Default.Payments,
                        label = "Repay",
                        onClick = onRecordRepaymentClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Send Reminder (only when person owes user)
                if (person.toReceive > 0) {
                    ActionButton(
                        icon = Icons.Default.Notifications,
                        label = "Remind",
                        onClick = {
                            val msg = LendingReminderUtils.buildReminderMessage(
                                personName = person.displayName,
                                outstandingAmount = person.toReceive,
                                originalDateMillis = person.openEntries.firstOrNull { it.lending.type.equals("LENT", true) }?.lending?.date ?: System.currentTimeMillis(),
                                dueDateMillis = person.nextDueDate
                            )
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send Reminder"))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Settle All
                if (person.openCount > 0) {
                    ActionButton(
                        icon = Icons.Default.Check,
                        label = "Settle All",
                        onClick = onSettleAllClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Pay via UPI (when user owes this person money)
                if (person.toPay > 0) {
                    ActionButton(
                        icon = Icons.Default.Payments,
                        label = "Pay UPI",
                        onClick = onPayUpiClick,
                        modifier = Modifier.weight(1f)
                    )
                }
        }

        // 3. GPay/Chat-style Send & Receive Transaction Stream (Latest at very bottom)
        if (allEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No transactions with ${person.displayName} yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom)
            ) {
                item(key = "history_header") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TRANSACTIONS HISTORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp,
                                fontSize = 10.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                items(allEntries, key = { it.lending.id }) { entryWithRepayments ->
                    GPayTransactionItem(
                        entryWithRepayments = entryWithRepayments,
                        onClick = { onTransactionClick(entryWithRepayments) }
                    )
                }

                item(key = "bottom_dock_clearance") {
                    Spacer(modifier = Modifier.height(115.dp)) // Dock clearance
                }
            }
        }
    }
}

/**
 * GPay-style transaction bubble / card:
 * - If Lent: Aligned right, subtle SageGreen tint, outgoing arrow, "+₹X.XX"
 * - If Borrowed: Aligned left, subtle ExpenseRed tint, incoming arrow, "-₹X.XX"
 * - Tapping opens full window with all details and actions.
 */
@Composable
private fun GPayTransactionItem(
    entryWithRepayments: LendingWithRepayments,
    onClick: () -> Unit
) {
    val entry = entryWithRepayments.lending
    val isLent = entry.type.equals("LENT", ignoreCase = true)
    val now = System.currentTimeMillis()
    val isOverdue = !entry.isSettled && entry.dueDate != null && entry.dueDate < now
    val effectiveOriginal = entryWithRepayments.effectiveOriginal
    val outstanding = entryWithRepayments.outstanding
    val totalRepaid = entryWithRepayments.totalRepaid

    // Alignment: Lent is sent (right), Borrowed is received (left)
    val alignment = if (isLent) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = if (isLent) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
    }

    // Monochrome card container & border as per theme
    val bubbleBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val bubbleBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 220.dp, max = 320.dp)
                .clip(bubbleShape)
                .clickable(onClick = onClick),
            shape = bubbleShape,
            colors = CardDefaults.cardColors(containerColor = bubbleBg),
            border = BorderStroke(1.dp, bubbleBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Top Row: Type & Direction Icon, Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isLent) Icons.AutoMirrored.Filled.CallMade else Icons.AutoMirrored.Filled.CallReceived,
                            contentDescription = null,
                            tint = if (isLent) SageGreen else ExpenseRed,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isLent) "You lent" else "You borrowed",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = if (isLent) SageGreen else ExpenseRed
                        )
                    }

                    // Status Pill (Monochrome as per theme)
                    val statusText = when {
                        entry.isSettled -> "Settled"
                        isOverdue -> "Overdue"
                        totalRepaid > 0 -> "Part-paid"
                        else -> "Pending"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Center Amount (Colored respectively: SageGreen for Lent, ExpenseRed for Borrowed)
                val sign = if (isLent) "+" else "−"
                val displayAmount = if (entry.isSettled) effectiveOriginal else outstanding
                Text(
                    text = "$sign${CurrencyUtils.formatCurrency(displayAmount)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = if (isLent) SageGreen else ExpenseRed
                )

                // Subcaption for partial repayment (Monochrome)
                if (!entry.isSettled && totalRepaid > 0) {
                    Text(
                        text = "${CurrencyUtils.formatCurrency(totalRepaid)} repaid of ${CurrencyUtils.formatCurrency(effectiveOriginal)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Note snippet (if any)
                if (!entry.notes.isNullOrBlank()) {
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                // Bottom Row: Date & Due Date indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = LendingDateFormatters.formatShortDateTime(entry.date),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )

                    if (entry.dueDate != null && !entry.isSettled) {
                        val dueStr = LendingDateFormatters.formatShort(entry.dueDate)
                        Text(
                            text = "Due: $dueStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
