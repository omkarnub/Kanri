package com.omkarnub.kanri.ui.lending

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)
private val WarningAmber = Color(0xFFF5A524)

@Composable
fun LendingPersonDetailView(
    person: PersonSummary,
    onBack: () -> Unit,
    onRenameClick: () -> Unit,
    onAddLentClick: () -> Unit,
    onAddBorrowedClick: () -> Unit,
    onRecordRepaymentClick: () -> Unit,
    onSettleAllClick: () -> Unit,
    onEntryRepay: (LendingWithRepayments) -> Unit,
    onEntryEdit: (LendingWithRepayments) -> Unit,
    onEntryDelete: (LendingWithRepayments) -> Unit,
    onEntryReopen: (LendingWithRepayments) -> Unit,
    onUndoRepayment: (LendingWithRepayments) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allEntries = remember(person) {
        (person.openEntries + person.settledEntries).sortedByDescending { it.lending.date }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Bar: Back arrow, Person avatar & name, net amount
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetter,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Tap-to-rename Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onRenameClick)
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = person.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
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
                if (person.openCount == 0) {
                    Text(
                        text = "Settled",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                } else {
                    val isPositive = person.net > 0
                    val isNegative = person.net < 0
                    val amountColor = if (isPositive) SageGreen else if (isNegative) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    val sign = if (isPositive) "+" else if (isNegative) "−" else ""

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = amountColor
                        )
                        AnimatedNumberText(
                            text = CurrencyUtils.formatCurrency(abs(person.net)),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = amountColor,
                            animateFromZero = false
                        )
                    }
                }
            }
        }

        // 2. Action Buttons Row: Lent, Borrowed, Repay, Reminder, Settle All
        item {
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
            }
        }

        // 3. Entries List Section
        item {
            Text(
                text = "ENTRIES (${allEntries.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
            )
        }

        items(allEntries, key = { it.lending.id }) { entryWithRepayments ->
            SwipeablePersonEntryCard(
                entryWithRepayments = entryWithRepayments,
                onRepay = { onEntryRepay(entryWithRepayments) },
                onEdit = { onEntryEdit(entryWithRepayments) },
                onDelete = { onEntryDelete(entryWithRepayments) },
                onReopen = { onEntryReopen(entryWithRepayments) },
                onUndoRepayment = { onUndoRepayment(entryWithRepayments) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(115.dp)) // Dock clearance
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeablePersonEntryCard(
    entryWithRepayments: LendingWithRepayments,
    onRepay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReopen: () -> Unit,
    onUndoRepayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe start -> end = Record repayment
                    if (!entryWithRepayments.lending.isSettled) {
                        onRepay()
                    }
                    false // Return false so card snaps back
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe end -> start = Delete with undo
                    onDelete()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
            val bgColor = if (isStartToEnd) SageGreen.copy(alpha = 0.25f) else ExpenseRed.copy(alpha = 0.25f)
            val icon = if (isStartToEnd) Icons.Default.Payments else Icons.Default.Delete
            val label = if (isStartToEnd) "Repay" else "Delete"

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isStartToEnd) SageGreen else ExpenseRed
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isStartToEnd) SageGreen else ExpenseRed
                    )
                }
            }
        }
    ) {
        PersonEntryCardContent(
            entryWithRepayments = entryWithRepayments,
            onRepay = onRepay,
            onEdit = onEdit,
            onDelete = onDelete,
            onReopen = onReopen,
            onUndoRepayment = onUndoRepayment
        )
    }
}

@Composable
fun PersonEntryCardContent(
    entryWithRepayments: LendingWithRepayments,
    onRepay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReopen: () -> Unit,
    onUndoRepayment: () -> Unit
) {
    val entry = entryWithRepayments.lending
    val isLent = entry.type.equals("LENT", ignoreCase = true)
    val now = System.currentTimeMillis()
    val isOverdue = !entry.isSettled && entry.dueDate != null && entry.dueDate < now
    val effectiveOriginal = entryWithRepayments.effectiveOriginal
    val outstanding = entryWithRepayments.outstanding
    val totalRepaid = entryWithRepayments.totalRepaid
    var isHistoryExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Type badge, Settled/Overdue tag, Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val badgeBg = if (isLent) SageGreen.copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)
                    val badgeColor = if (isLent) SageGreen else ExpenseRed
                    val badgeText = if (isLent) "I Lent" else "I Borrowed"

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            ),
                            color = badgeColor
                        )
                    }

                    if (entry.isSettled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (isOverdue) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WarningAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Overdue",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = WarningAmber
                            )
                        }
                    }
                }

                // Amount
                Text(
                    text = CurrencyUtils.formatCurrency(if (entry.isSettled) effectiveOriginal else outstanding),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (entry.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else if (isLent) SageGreen else ExpenseRed
                )
            }

            // Repaid progress bar (if partially repaid)
            if (effectiveOriginal > 0 && !entry.isSettled && entryWithRepayments.repayments.isNotEmpty()) {
                val progress = (totalRepaid / effectiveOriginal).coerceIn(0.0, 1.0).toFloat()
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                    label = "repaidProgress"
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${CurrencyUtils.formatCurrency(totalRepaid)} repaid",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = SageGreen
                        )
                        Text(
                            text = "of ${CurrencyUtils.formatCurrency(effectiveOriginal)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SageGreen,
                        trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }

            // Dates: Entry date and due date
            val entryDateStr = LendingDateFormatters.formatMedium(entry.date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Given on $entryDateStr",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                if (entry.dueDate != null) {
                    val dueDateStr = LendingDateFormatters.formatMedium(entry.dueDate)
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Due $dueDateStr",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isOverdue) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isOverdue) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            // Note (if any)
            if (!entry.notes.isNullOrBlank()) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable Repayment History
            if (entryWithRepayments.repayments.isNotEmpty()) {
                val chevronRot by animateFloatAsState(
                    targetValue = if (isHistoryExpanded) 180f else 0f,
                    label = "historyChevron"
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { isHistoryExpanded = !isHistoryExpanded }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Repayment history (${entryWithRepayments.repayments.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle history",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(chevronRot)
                    )
                }

                AnimatedVisibility(
                    visible = isHistoryExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .border(0.8.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        entryWithRepayments.repayments.forEach { rep ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    val repDate = LendingDateFormatters.formatMedium(rep.paidAt)
                                    Text(
                                        text = repDate,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!rep.note.isNullOrBlank()) {
                                        Text(
                                            text = rep.note,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Text(
                                    text = CurrencyUtils.formatCurrency(rep.amount),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = SageGreen
                                )
                            }
                        }

                        // Undo latest repayment button
                        Text(
                            text = "Undo latest repayment",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = ExpenseRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onUndoRepayment)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 2.dp)
            )

            // Bottom Actions: Repay, Edit, Delete, Reopen
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!entry.isSettled) {
                        Text(
                            text = "Record repayment",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = SageGreen,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onRepay)
                                .padding(vertical = 2.dp)
                        )
                    } else {
                        Text(
                            text = "Reopen",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable(onClick = onReopen)
                                .padding(vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onEdit)
                            .padding(vertical = 2.dp)
                    )
                }

                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = ExpenseRed.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onDelete)
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}
