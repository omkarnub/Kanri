package com.omkarnub.kanri.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassButton
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassPill
import com.omkarnub.kanri.ui.common.KanriModalBottomSheet
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.rememberKanriHaptics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchFilterSheet(
    uiState: SearchUiState,
    onDismiss: () -> Unit,
    onTypeSelect: (TransactionTypeFilter) -> Unit,
    onDatePresetSelect: (DateRangePreset) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onClearCategories: () -> Unit,
    onSourceToggle: (String) -> Unit,
    onAmountPresetSelect: (AmountRangePreset) -> Unit,
    onSortSelect: (SortOption) -> Unit,
    onWalletFilterSelect: (WalletFilter) -> Unit = {},
    onResetAll: () -> Unit,
    initialTab: SearchFilterSheetTab = SearchFilterSheetTab.FILTERS
) {
    val haptics = rememberKanriHaptics()
    val tokens = LocalKanriGlass.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var isCategoriesExpanded by remember { mutableStateOf(false) }

    KanriModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header Row: Icon, Title, and Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (activeTab == SearchFilterSheetTab.SORT) Icons.AutoMirrored.Filled.Sort else Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (activeTab == SearchFilterSheetTab.SORT) "Sort Order" else "Filter & Refine",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (activeTab == SearchFilterSheetTab.SORT) {
                                "Currently: ${uiState.sortOption.displayName}"
                            } else if (uiState.activeFilterCount > 0) {
                                "${uiState.activeFilterCount} active filters applied"
                            } else {
                                "Customize transaction search view"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.activeFilterCount > 0 && activeTab == SearchFilterSheetTab.FILTERS) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }

                if (activeTab == SearchFilterSheetTab.FILTERS && uiState.activeFilterCount > 0) {
                    TextButton(
                        onClick = {
                            haptics.warning()
                            onResetAll()
                        }
                    ) {
                        Text(
                            text = "Reset All",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            onDismiss()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // SEGMENTED TAB SWITCHER: Filters vs Sort
            // =================================================================
            KanriGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                level = GlassLevel.Flat
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Filters Tab
                    val isFiltersSelected = activeTab == SearchFilterSheetTab.FILTERS
                    KanriGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(9.dp),
                        level = if (isFiltersSelected) GlassLevel.Strong else GlassLevel.Flat,
                        onClick = {
                            haptics.tick()
                            activeTab = SearchFilterSheetTab.FILTERS
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isFiltersSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.activeFilterCount > 0) "Filters (${uiState.activeFilterCount})" else "Filters",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isFiltersSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isFiltersSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Sort Tab
                    val isSortSelected = activeTab == SearchFilterSheetTab.SORT
                    KanriGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(9.dp),
                        level = if (isSortSelected) GlassLevel.Strong else GlassLevel.Flat,
                        onClick = {
                            haptics.tick()
                            activeTab = SearchFilterSheetTab.SORT
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isSortSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sort (${uiState.sortOption.displayName})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSortSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSortSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // SCROLLABLE CONTENT BODY
            // =================================================================
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                if (activeTab == SearchFilterSheetTab.SORT) {
                    // =========================================================
                    // DEDICATED SORT TAB
                    // =========================================================
                    SortSectionContent(
                        selectedSort = uiState.sortOption,
                        onSelect = { option ->
                            haptics.click()
                            onSortSelect(option)
                        }
                    )
                } else {
                    // =========================================================
                    // DEDICATED FILTERS TAB
                    // =========================================================
                    FiltersSectionContent(
                        uiState = uiState,
                        isCategoriesExpanded = isCategoriesExpanded,
                        onToggleCategoriesExpanded = { isCategoriesExpanded = !isCategoriesExpanded },
                        onTypeSelect = onTypeSelect,
                        onWalletFilterSelect = onWalletFilterSelect,
                        onDatePresetSelect = onDatePresetSelect,
                        onCategoryToggle = onCategoryToggle,
                        onClearCategories = onClearCategories,
                        onSourceToggle = onSourceToggle,
                        onAmountPresetSelect = onAmountPresetSelect
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // =================================================================
            // BOTTOM DOCKED ACTION BUTTON
            // =================================================================
            KanriGlassButton(
                text = if (activeTab == SearchFilterSheetTab.SORT) "Done" else "Show Results (${uiState.filteredTransactions.size})",
                level = GlassLevel.Strong,
                shape = RoundedCornerShape(14.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    haptics.primaryAction()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * Dedicated Sort View: Shows clear full-width glass selectable cards for each sort order.
 */
@Composable
private fun SortSectionContent(
    selectedSort: SortOption,
    onSelect: (SortOption) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ORDER TRANSACTIONS BY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SortOption.entries.forEach { option ->
            val isSelected = selectedSort == option
            val (icon, subtitle) = when (option) {
                SortOption.DATE_DESC -> Pair(Icons.Default.CalendarToday, "Most recent transactions first (Default)")
                SortOption.DATE_ASC -> Pair(Icons.Default.History, "Oldest transactions first")
                SortOption.AMOUNT_DESC -> Pair(Icons.AutoMirrored.Filled.TrendingUp, "Largest expenses & credits first")
                SortOption.AMOUNT_ASC -> Pair(Icons.AutoMirrored.Filled.TrendingDown, "Smallest transactions first")
            }

            KanriGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat,
                onClick = { onSelect(option) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
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
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = option.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Radio Check Indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Filter Sections Content: Clean card layout with 100% centered button text.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersSectionContent(
    uiState: SearchUiState,
    isCategoriesExpanded: Boolean,
    onToggleCategoriesExpanded: () -> Unit,
    onTypeSelect: (TransactionTypeFilter) -> Unit,
    onWalletFilterSelect: (WalletFilter) -> Unit,
    onDatePresetSelect: (DateRangePreset) -> Unit,
    onCategoryToggle: (Long) -> Unit,
    onClearCategories: () -> Unit,
    onSourceToggle: (String) -> Unit,
    onAmountPresetSelect: (AmountRangePreset) -> Unit
) {
    val haptics = rememberKanriHaptics()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =================================================================
        // 1. TRANSACTION TYPE (3 Buttons in a Row with 100% Centered Text)
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionHeader(title = "TRANSACTION TYPE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionTypeFilter.entries.forEach { type ->
                    val isSelected = uiState.typeFilter == type
                    val (icon, label) = when (type) {
                        TransactionTypeFilter.ALL -> Pair(null, "All")
                        TransactionTypeFilter.DEBIT -> Pair(Icons.Default.ArrowDownward, "Spent")
                        TransactionTypeFilter.CREDIT -> Pair(Icons.Default.ArrowUpward, "Received")
                    }

                    KanriGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(11.dp),
                        level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat,
                        onClick = {
                            haptics.tick()
                            onTypeSelect(type)
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (icon != null) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // =================================================================
        // 2. WALLET FILTER (Clean 2x2 Grid with 100% Centered Text & Icons)
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionHeader(title = "WALLET & ACCOUNTS")
            val wallets = WalletFilter.entries
            // 2 Rows of 2 equal columns
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: All, Cash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.take(2).forEach { filter ->
                        WalletFilterButton(
                            filter = filter,
                            isSelected = uiState.walletFilter == filter,
                            onClick = { onWalletFilterSelect(filter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Row 2: Online, Transfers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    wallets.drop(2).forEach { filter ->
                        WalletFilterButton(
                            filter = filter,
                            isSelected = uiState.walletFilter == filter,
                            onClick = { onWalletFilterSelect(filter) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // =================================================================
        // 3. DATE RANGE PRESETS (Clean Glass Pills with Centered Text)
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionHeader(title = "DATE RANGE")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DateRangePreset.entries.filter { it != DateRangePreset.CUSTOM }.forEach { preset ->
                    val isSelected = uiState.datePreset == preset
                    KanriFilterChipPill(
                        selected = isSelected,
                        onClick = { onDatePresetSelect(preset) },
                        label = preset.displayName
                    )
                }
            }
        }

        // =================================================================
        // 4. CATEGORIES (Organized Pills with Count & Optional Collapse)
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterSectionHeader(title = "CATEGORIES (${uiState.availableCategories.size})")
                if (uiState.selectedCategoryIds.isNotEmpty()) {
                    TextButton(onClick = {
                        haptics.tick()
                        onClearCategories()
                    }) {
                        Text(
                            text = "Clear (${uiState.selectedCategoryIds.size})",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val displayedCategories = if (isCategoriesExpanded || uiState.availableCategories.size <= 8) {
                uiState.availableCategories
            } else {
                uiState.availableCategories.take(8)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayedCategories.forEach { category ->
                    val isSelected = uiState.selectedCategoryIds.contains(category.id)
                    KanriFilterChipPill(
                        selected = isSelected,
                        onClick = { onCategoryToggle(category.id) },
                        label = category.name,
                        leadingIcon = {
                            CategoryIcon(
                                categoryName = category.name,
                                iconName = category.iconName,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    )
                }

                // Show More / Show Less Toggle Pill
                if (uiState.availableCategories.size > 8) {
                    val remaining = uiState.availableCategories.size - 8
                    KanriFilterChipPill(
                        selected = false,
                        onClick = onToggleCategoriesExpanded,
                        label = if (isCategoriesExpanded) "Show Less" else "+$remaining More",
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCategoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    )
                }
            }
        }

        // =================================================================
        // 5. AMOUNT RANGE
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionHeader(title = "AMOUNT RANGE")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AmountRangePreset.entries.filter { it != AmountRangePreset.CUSTOM }.forEach { preset ->
                    val isSelected = uiState.amountPreset == preset
                    KanriFilterChipPill(
                        selected = isSelected,
                        onClick = { onAmountPresetSelect(preset) },
                        label = preset.displayName
                    )
                }
            }
        }

        // =================================================================
        // 6. PAYMENT SOURCE
        // =================================================================
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterSectionHeader(title = "PAYMENT METHOD")
            val sources = listOf("UPI", "Card", "ATM", "Bank Transfer")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEach { source ->
                    val isSelected = uiState.selectedSourceTypes.any { it.equals(source, ignoreCase = true) }
                    KanriFilterChipPill(
                        selected = isSelected,
                        onClick = { onSourceToggle(source) },
                        label = source
                    )
                }
            }
        }
    }
}

/**
 * Clean Wallet Button with Icon and Centered Text
 */
@Composable
private fun WalletFilterButton(
    filter: WalletFilter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    val icon = when (filter) {
        WalletFilter.ALL -> Icons.Default.AccountBalanceWallet
        WalletFilter.CASH -> Icons.Default.Payments
        WalletFilter.ONLINE -> Icons.Default.CreditCard
        WalletFilter.TRANSFERS -> Icons.Default.SwapHoriz
    }

    KanriGlassCard(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(11.dp),
        level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat,
        onClick = {
            haptics.tick()
            onClick()
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = filter.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Reusable Glass Filter Pill with 100% Centered Text and Uniform Height
 */
@Composable
fun KanriFilterChipPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val haptics = rememberKanriHaptics()

    KanriGlassCard(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(10.dp),
        level = if (selected) GlassLevel.Strong else GlassLevel.Flat,
        onClick = {
            haptics.tick()
            onClick()
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = MaterialTheme.colorScheme.outline
    )
}
