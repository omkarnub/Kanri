package com.omkarnub.kanri.ui.lending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.ui.home.formatCurrency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendingScreen(
    modifier: Modifier = Modifier,
    viewModel: LendingViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSplitSheet by remember { mutableStateOf(false) }
    val splitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lent & Borrowed",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { showSplitSheet = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = "Split a Bill",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (showAddDialog) {
            AddLendingDialog(
                onDismiss = { showAddDialog = false },
                onSave = { personName, amount, type, dueDate, notes ->
                    viewModel.addRecord(personName, amount, type, dueDate, notes)
                    showAddDialog = false
                }
            )
        }

        if (showSplitSheet) {
            SplitExpenseSheet(
                sheetState = splitSheetState,
                onDismiss = { showSplitSheet = false },
                onSaveLenderSplit = { friendNames, perPersonShare, eventDescription ->
                    viewModel.addSplitAsLender(friendNames, perPersonShare, eventDescription)
                    showSplitSheet = false
                },
                onSaveBorrowerSplit = { payerName, userShare, eventDescription ->
                    viewModel.addSplitAsBorrower(payerName, userShare, eventDescription)
                    showSplitSheet = false
                }
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                com.omkarnub.kanri.ui.common.KanriWobbleLoader(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Cards
                item {
                    LendingSummaryCard(
                        totalLent = state.totalLentPending,
                        totalBorrowed = state.totalBorrowedPending,
                        netBalance = state.netBalance
                    )
                }

                // Split a Bill Quick Action Card
                item {
                    SplitBillBannerCard(onClick = { showSplitSheet = true })
                }

                // Filter Chips Row
                item {
                    LendingFilterRow(
                        currentFilter = state.activeFilter,
                        onFilterSelected = { viewModel.setFilter(it) }
                    )
                }

                if (state.records.isEmpty()) {
                    item {
                        EmptyLendingView(onAddClick = { showAddDialog = true })
                    }
                } else {
                    items(state.records, key = { it.id }) { record ->
                        LendingItemCard(
                            record = record,
                            onToggleSettled = { viewModel.toggleSettled(record) },
                            onDelete = { viewModel.deleteRecord(record) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun LendingSummaryCard(
    totalLent: Double,
    totalBorrowed: Double,
    netBalance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEBT LEDGER OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (netBalance >= 0) Color(0x222ECC71) else Color(0x22E74C3C))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (netBalance >= 0) "Net: +${formatCurrency(netBalance)}" else "Net: ${formatCurrency(netBalance)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lent Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Money I Lent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(totalLent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Awaiting return",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Borrowed Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Money I Borrowed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(totalBorrowed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "To pay back",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun LendingFilterRow(
    currentFilter: LendingFilter,
    onFilterSelected: (LendingFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LendingFilter.entries.forEach { filter ->
            val isSelected = currentFilter == filter
            val label = when (filter) {
                LendingFilter.ALL -> "All"
                LendingFilter.LENT -> "Lent"
                LendingFilter.BORROWED -> "Borrowed"
                LendingFilter.PENDING_ONLY -> "Pending"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (!isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        else Modifier
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun LendingItemCard(
    record: LendingEntity,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit
) {
    val isLent = record.type.equals("LENT", ignoreCase = true)
    val accentColor = MaterialTheme.colorScheme.onSurface
    val isSettled = record.isSettled

    val now = System.currentTimeMillis()
    val isOverdue = !isSettled && record.dueDate != null && record.dueDate < now

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isSettled) 0.1f else 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.personName.firstOrNull()?.uppercase() ?: "?",
                    color = accentColor.copy(alpha = if (isSettled) 0.5f else 1.0f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = record.personName,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isSettled) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isLent) "Lent" else "Borrowed",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!record.notes.isNullOrBlank()) {
                    Text(
                        text = record.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date & Due Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatDate(record.date),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (record.dueDate != null) {
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = if (isOverdue) "Overdue (${formatDate(record.dueDate)})" else "Due ${formatDate(record.dueDate)}",
                            fontSize = 11.sp,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount & Settle Button Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isLent) "+" else "-"}${formatCurrency(record.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSettled) MaterialTheme.colorScheme.onSurfaceVariant else accentColor,
                        textDecoration = if (isSettled) TextDecoration.LineThrough else TextDecoration.None
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Settle Chip Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSettled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                0.5.dp,
                                if (isSettled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onToggleSettled() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSettled) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = if (isSettled) "Settled" else "Mark Settled",
                                color = if (isSettled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Delete Icon
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLendingView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Handshake,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No lending records yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Track money given to friends, split bills, or money you borrowed in one place.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 18.sp
        )
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun SplitBillBannerCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "⚡ Split a Group Bill",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Divide costs with animated circle & auto-log debts",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Split Now →",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

