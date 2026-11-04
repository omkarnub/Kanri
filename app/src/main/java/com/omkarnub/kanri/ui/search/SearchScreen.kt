package com.omkarnub.kanri.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.home.CategoryPickerSheet
import com.omkarnub.kanri.ui.home.TransactionItemCard
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
    autoFocus: Boolean = true,
    initialFilterState: SearchFilterState? = null,
    onTransactionClick: ((TransactionWithCategory) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterSheetInitialTab by remember { mutableStateOf(SearchFilterSheetTab.FILTERS) }
    var selectedTransactionForCategory by remember { mutableStateOf<TransactionWithCategory?>(null) }
    var lendingEntryForTransaction by remember { mutableStateOf<TransactionWithCategory?>(null) }
    val haptics = rememberKanriHaptics()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialFilterState) {
        initialFilterState?.let { filter ->
            viewModel.applyFilterState(filter)
        }
    }

    LaunchedEffect(autoFocus) {
        if (autoFocus && initialFilterState == null) {
            // Automatically request focus on search input upon entering
            focusRequester.requestFocus()
        }
    }

    val tokens = LocalKanriGlass.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = uiState.query,
                        onValueChange = { viewModel.setQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = {
                            Text(
                                text = "Search payees, banks, notes...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = {
                                    haptics.tick()
                                    viewModel.setQuery("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.tick()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptics.click()
                        filterSheetInitialTab = SearchFilterSheetTab.FILTERS
                        showFilterSheet = true
                    }) {
                        if (uiState.activeFilterCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text(uiState.activeFilterCount.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filters",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filters",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Filter Chips Bar
            QuickFilterChipsRow(
                uiState = uiState,
                onOpenFilterSheet = { tab ->
                    haptics.click()
                    filterSheetInitialTab = tab
                    showFilterSheet = true
                },
                onToggleType = {
                    haptics.tick()
                    val nextType = when (uiState.typeFilter) {
                        TransactionTypeFilter.ALL -> TransactionTypeFilter.DEBIT
                        TransactionTypeFilter.DEBIT -> TransactionTypeFilter.CREDIT
                        TransactionTypeFilter.CREDIT -> TransactionTypeFilter.ALL
                    }
                    viewModel.setTypeFilter(nextType)
                },
                onToggleWallet = {
                    haptics.tick()
                    val nextWallet = when (uiState.walletFilter) {
                        WalletFilter.ALL -> WalletFilter.CASH
                        WalletFilter.CASH -> WalletFilter.ONLINE
                        WalletFilter.ONLINE -> WalletFilter.TRANSFERS
                        WalletFilter.TRANSFERS -> WalletFilter.ALL
                    }
                    viewModel.setWalletFilter(nextWallet)
                },
                onClearAll = {
                    haptics.tick()
                    viewModel.clearAllFilters()
                }
            )

            // Dynamic Results & Summary Header
            if (uiState.filteredTransactions.isNotEmpty()) {
                SearchSummaryHeader(
                    count = uiState.filteredTransactions.size,
                    spent = uiState.totalSpent,
                    received = uiState.totalReceived,
                    net = uiState.netAmount,
                    transfers = uiState.totalTransfers,
                    isTransfersFilterActive = uiState.walletFilter == WalletFilter.TRANSFERS
                )
            }

            // Results Content
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.omkarnub.kanri.ui.common.KanriWobbleLoader(color = MaterialTheme.colorScheme.primary)
                }
            } else if (uiState.filteredTransactions.isEmpty()) {
                EmptySearchResults(
                    hasFilters = uiState.query.isNotBlank() || uiState.activeFilterCount > 0,
                    onClearFilters = { viewModel.clearAllFilters() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.filteredTransactions,
                        key = { it.transaction.id }
                    ) { item ->
                        if (tokens != null) {
                            KanriGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                level = GlassLevel.Flat,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)) {
                                    TransactionItemCard(
                                        item = item,
                                        onClick = {
                                            haptics.click()
                                            if (onTransactionClick != null) {
                                                onTransactionClick(item)
                                            } else {
                                                selectedTransactionForCategory = item
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            TransactionItemCard(
                                item = item,
                                onClick = {
                                    haptics.click()
                                    if (onTransactionClick != null) {
                                        onTransactionClick(item)
                                    } else {
                                        selectedTransactionForCategory = item
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        SearchFilterSheet(
            uiState = uiState,
            initialTab = filterSheetInitialTab,
            onDismiss = { showFilterSheet = false },
            onTypeSelect = { viewModel.setTypeFilter(it) },
            onDatePresetSelect = { viewModel.setDatePreset(it) },
            onCategoryToggle = { viewModel.toggleCategory(it) },
            onClearCategories = { viewModel.clearCategories() },
            onSourceToggle = { viewModel.toggleSourceType(it) },
            onAmountPresetSelect = { viewModel.setAmountPreset(it) },
            onSortSelect = { viewModel.setSortOption(it) },
            onWalletFilterSelect = { viewModel.setWalletFilter(it) },
            onResetAll = { viewModel.clearAllFilters() }
        )
    }

    // Category Re-labeling Sheet
    selectedTransactionForCategory?.let { item ->
        CategoryPickerSheet(
            targetTransaction = item,
            categories = uiState.availableCategories,
            onDismiss = { selectedTransactionForCategory = null },
            onCategorySelected = { catId, note ->
                viewModel.updateTransactionCategory(item.transaction.id, catId, note)
                selectedTransactionForCategory = null
            },
            onOpenLendBorrow = {
                lendingEntryForTransaction = item
                selectedTransactionForCategory = null
            }
        )
    }

    lendingEntryForTransaction?.let { target ->
        com.omkarnub.kanri.ui.lending.LendingTransactionBridgeDialog(
            targetTransaction = target,
            onDismiss = { lendingEntryForTransaction = null }
        )
    }
}

@Composable
private fun QuickFilterChipsRow(
    uiState: SearchUiState,
    onOpenFilterSheet: (SearchFilterSheetTab) -> Unit,
    onToggleType: () -> Unit,
    onToggleWallet: () -> Unit,
    onClearAll: () -> Unit
) {
    val haptics = rememberKanriHaptics()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Quick Filters Button
        val isFilterActive = uiState.activeFilterCount > 0
        KanriGlassCard(
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            level = if (isFilterActive) GlassLevel.Strong else GlassLevel.Flat,
            onClick = {
                onOpenFilterSheet(SearchFilterSheetTab.FILTERS)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFilterActive) "Filters (${uiState.activeFilterCount})" else "Filters",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Direct Sort Order Launcher (1-tap fast access to sort!)
        val isSortCustom = uiState.sortOption != SortOption.DATE_DESC
        KanriGlassCard(
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            level = if (isSortCustom) GlassLevel.Strong else GlassLevel.Flat,
            onClick = {
                onOpenFilterSheet(SearchFilterSheetTab.SORT)
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = if (isSortCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (uiState.sortOption) {
                            SortOption.DATE_DESC -> "Sort: Newest"
                            SortOption.DATE_ASC -> "Sort: Oldest"
                            SortOption.AMOUNT_DESC -> "Sort: Highest"
                            SortOption.AMOUNT_ASC -> "Sort: Lowest"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSortCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 3. Type Filter Chip
        val isTypeActive = uiState.typeFilter != TransactionTypeFilter.ALL
        KanriGlassCard(
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            level = if (isTypeActive) GlassLevel.Strong else GlassLevel.Flat,
            onClick = onToggleType
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (uiState.typeFilter) {
                        TransactionTypeFilter.ALL -> "Type: All"
                        TransactionTypeFilter.DEBIT -> "Spent Only"
                        TransactionTypeFilter.CREDIT -> "Received Only"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isTypeActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isTypeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 4. Wallet Filter Chip
        val isWalletActive = uiState.walletFilter != WalletFilter.ALL
        KanriGlassCard(
            modifier = Modifier.height(36.dp),
            shape = RoundedCornerShape(10.dp),
            level = if (isWalletActive) GlassLevel.Strong else GlassLevel.Flat,
            onClick = onToggleWallet
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (uiState.walletFilter) {
                        WalletFilter.ALL -> "Wallet: All"
                        WalletFilter.CASH -> "Cash"
                        WalletFilter.ONLINE -> "Online"
                        WalletFilter.TRANSFERS -> "Transfers"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isWalletActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isWalletActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 5. Date Preset Chip
        if (uiState.datePreset != DateRangePreset.ALL_TIME) {
            KanriGlassCard(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                level = GlassLevel.Strong,
                onClick = { onOpenFilterSheet(SearchFilterSheetTab.FILTERS) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.datePreset.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 6. Category Filter Chip
        if (uiState.selectedCategoryIds.isNotEmpty()) {
            KanriGlassCard(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                level = GlassLevel.Strong,
                onClick = { onOpenFilterSheet(SearchFilterSheetTab.FILTERS) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Categories (${uiState.selectedCategoryIds.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 7. Amount Filter Chip
        if (uiState.amountPreset != AmountRangePreset.ALL) {
            KanriGlassCard(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                level = GlassLevel.Strong,
                onClick = { onOpenFilterSheet(SearchFilterSheetTab.FILTERS) }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.amountPreset.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 8. Clear / Reset All Chip
        if (uiState.activeFilterCount > 0 || uiState.query.isNotEmpty() || isSortCustom) {
            KanriGlassCard(
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(10.dp),
                level = GlassLevel.Flat,
                onClick = onClearAll
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear all",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSummaryHeader(
    count: Int,
    spent: Double,
    received: Double,
    net: Double,
    transfers: Double = 0.0,
    isTransfersFilterActive: Boolean = false
) {
    val tokens = LocalKanriGlass.current
    val content = @Composable {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$count ${if (count == 1) "transaction" else "transactions"} found",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!isTransfersFilterActive) {
                    Text(
                        text = "Net: ${if (net >= 0) "+" else ""}${formatCurrency(net)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isTransfersFilterActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Transfers: ${formatCurrency(transfers)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spent: ${formatCurrency(spent)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Received: ${formatCurrency(received)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (tokens != null) {
        KanriGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            level = GlassLevel.Flat,
            shape = RoundedCornerShape(14.dp)
        ) {
            content()
        }
    } else {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            content()
        }
    }
}

@Composable
private fun EmptySearchResults(
    hasFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No matching transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (hasFilters) {
                    "Try adjusting your search query, dates, or clearing active filters."
                } else {
                    "No transactions have been recorded yet."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (hasFilters) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Reset All Filters",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
