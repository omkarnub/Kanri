package com.omkarnub.kanri.ui.insights.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.home.CategoryPickerSheet
import com.omkarnub.kanri.ui.home.TransactionItemCard
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.insights.InsightsViewModel
import com.omkarnub.kanri.ui.insights.PayeeDetailData
import java.time.format.DateTimeFormatter

private val ExpenseRed = Color(0xFFE54D2E)
private val IncomeSage = Color(0xFF30A46C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayeeDetailScreen(
    counterparty: String,
    viewModel: InsightsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val payeeDataState: PayeeDetailData? by viewModel.getPayeeDetailFlow(counterparty).collectAsState(initial = null)
    val availableCategories: List<CategoryEntity> by viewModel.allCategoriesFlow.collectAsState(initial = emptyList<CategoryEntity>())
    var isAllTimeScope by remember { mutableStateOf(false) }
    var selectedTxForPicker by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var scrubbedMonthIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payee Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        val data = payeeDataState
        if (data == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                com.omkarnub.kanri.ui.common.KanriWobbleLoader(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val transactionsToDisplay = data.transactions

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Avatar, Display Name, Raw counterparty, Default Category Chip
            item {
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
                            .padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            val initial = (data.displayName.firstOrNull() ?: '?').uppercaseChar().toString()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = data.displayName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = data.rawCounterparty,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    transactionsToDisplay.firstOrNull()?.let { tx ->
                                        selectedTxForPicker = tx
                                    }
                                }
                            ) {
                                Text(
                                    text = data.category?.name ?: "Uncategorized",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stat Tiles (2-column grid)
            item {
                val dtf = DateTimeFormatter.ofPattern("d MMM yyyy")
                val stats = listOfNotNull(
                    "Total paid" to formatCurrency(data.totalPaid),
                    "Payments count" to data.paymentCount.toString(),
                    "Avg per payment" to formatCurrency(data.averagePerPayment),
                    "Largest payment" to formatCurrency(data.largestPayment),
                    "First paid" to (data.firstPaidDate?.format(dtf) ?: "—"),
                    "Last paid" to (data.lastPaidDate?.format(dtf) ?: "—"),
                    "Frequency" to data.frequencyLabel,
                    if (data.receivedFromThem > 0) "Received" to formatCurrency(data.receivedFromThem) else null,
                    if (data.receivedFromThem > 0) "Net" to formatCurrency(data.netTotal) else null
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (label, value) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 12-Month Spend Bars
            if (data.last12MonthsBars.isNotEmpty()) {
                item {
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
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "12-MONTH SPEND HISTORY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val maxSpend = (data.last12MonthsBars.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
                            val onSurface = MaterialTheme.colorScheme.onSurface

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            ) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(data.last12MonthsBars) {
                                            detectTapGestures { offset ->
                                                val barSlot = size.width / data.last12MonthsBars.size
                                                val idx = (offset.x / barSlot).toInt().coerceIn(0, data.last12MonthsBars.size - 1)
                                                scrubbedMonthIndex = idx
                                            }
                                        }
                                ) {
                                    val barSlot = size.width / data.last12MonthsBars.size
                                    val barWidth = barSlot * 0.6f
                                    val h = size.height

                                    for (i in data.last12MonthsBars.indices) {
                                        val bar = data.last12MonthsBars[i]
                                        val barHeight = ((bar.amount / maxSpend).toFloat() * h).coerceAtLeast(4f)
                                        val x = i * barSlot + (barSlot - barWidth) / 2f
                                        val y = h - barHeight

                                        val isScrubbed = scrubbedMonthIndex == i
                                        val color = if (isScrubbed) ExpenseRed else onSurface.copy(alpha = 0.7f)

                                        drawRoundRect(
                                            color = color,
                                            topLeft = Offset(x, y),
                                            size = Size(barWidth, barHeight),
                                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                        )
                                    }
                                }
                            }

                            scrubbedMonthIndex?.let { idx ->
                                val bar = data.last12MonthsBars.getOrNull(idx)
                                if (bar != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${bar.label}: ${formatCurrency(bar.amount)}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transactions Header
            item {
                Text(
                    text = "TRANSACTIONS (${transactionsToDisplay.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // Transaction Rows
            items(
                items = transactionsToDisplay,
                key = { it.transaction.id }
            ) { txModel ->
                TransactionItemCard(
                    item = txModel,
                    onClick = { selectedTxForPicker = txModel }
                )
            }
        }
    }

    selectedTxForPicker?.let { item: TransactionWithCategory ->
        CategoryPickerSheet(
            targetTransaction = item,
            categories = availableCategories,
            onDismiss = { selectedTxForPicker = null },
            onCategorySelected = { newCategoryId: Long, note: String? ->
                viewModel.updateTransactionCategory(
                    transactionId = item.transaction.id,
                    newCategoryId = newCategoryId,
                    counterparty = item.transaction.counterparty,
                    note = note
                )
                selectedTxForPicker = null
            }
        )
    }
}
