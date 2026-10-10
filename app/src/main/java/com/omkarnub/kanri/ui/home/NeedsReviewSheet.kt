package com.omkarnub.kanri.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.input.KeyboardType
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.SavingsGoalContributionEntity
import com.omkarnub.kanri.data.db.SavingsGoalEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.savings.GoalIcon
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColorIncomeGreen = Color(0xFF34C759)
private val ColorExpenseRed = Color(0xFFE5484D)
private val ColorAmberBadge = Color(0xFFF5A524)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsReviewSheet(
    reviewQueue: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAssignCategory: (transactionId: Long, counterparty: String?, categoryId: Long, note: String?) -> Unit,
    onOpenLendBorrow: ((TransactionEntity) -> Unit)? = null,
    onConvertToTransfer: ((transactionId: Long, wallet: String, transferToWallet: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val db = remember { KanriDatabase.getDatabase(context) }
    val haptics = rememberKanriHaptics()

    var activeGoals by remember { mutableStateOf<List<SavingsGoalEntity>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            activeGoals = db.savingsGoalDao().getActiveGoalsSync()
        }
    }

    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var allCategories by remember(categories) {
        mutableStateOf(sortCategoriesWithPriority(categories))
    }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingCategory by remember { mutableStateOf(false) }

    // Intercept hardware back button step-by-step
    BackHandler(enabled = selectedTransaction != null) {
        when {
            isCreatingCategory -> isCreatingCategory = false
            isSearchOpen || searchQuery.isNotEmpty() -> {
                isSearchOpen = false
                searchQuery = ""
            }
            else -> selectedTransaction = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                    .clickable { onDismiss() }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            AnimatedContent(
                targetState = selectedTransaction,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { -it } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally { -it } + fadeIn(tween(250))) togetherWith
                                (slideOutHorizontally { it } + fadeOut(tween(200)))
                    }
                },
                label = "ReviewSheetContentTransition"
            ) { targetTx ->
                if (targetTx == null) {
                    // ==========================================
                    // VIEW 1: TRANSACTIONS LIST
                    // ==========================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Needs Categorization",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap a transaction to select its category",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (reviewQueue.isEmpty()) {
                            // Empty state: "All caught up"
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ColorIncomeGreen,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "All caught up",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "All transactions are categorized.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val dateFormat = remember {
                                SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(maxHeight = 460.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = reviewQueue,
                                    key = { it.id }
                                ) { tx ->
                                    ReviewQueueItem(
                                        transaction = tx,
                                        formattedDate = dateFormat.format(Date(tx.timestamp)),
                                        onClick = {
                                            haptics.click()
                                            selectedTransaction = tx
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // VIEW 2: CATEGORY PICKER FOR SELECTED ITEM
                    // ==========================================
                    var chosenCategoryId by remember(targetTx) {
                        mutableStateOf<Long?>(targetTx.categoryId)
                    }
                    var noteText by remember(targetTx) {
                        mutableStateOf(targetTx.notes ?: "")
                    }
                    var isAllocateToGoalEnabled by remember(targetTx) { mutableStateOf(false) }
                    var selectedGoalId by remember(targetTx, activeGoals) {
                        mutableStateOf(activeGoals.firstOrNull()?.id)
                    }
                    var allocatedGoalAmountStr by remember(targetTx) {
                        mutableStateOf(targetTx.amount.toInt().toString())
                    }

                    val filteredCategories = remember(allCategories, searchQuery) {
                        if (searchQuery.isBlank()) {
                            allCategories
                        } else {
                            val q = searchQuery.trim().lowercase()
                            allCategories.filter { it.name.lowercase().contains(q) }
                        }
                    }

                    val selectedCategory = allCategories.find { it.id == chosenCategoryId }
                    val isOthersSelected = selectedCategory?.name?.equals("Other", ignoreCase = true) == true ||
                            selectedCategory?.name?.equals("Others", ignoreCase = true) == true

                    val isNoteValid = !isOthersSelected || noteText.trim().isNotBlank()
                    val isConfirmEnabled = chosenCategoryId != null && isNoteValid

                    if (isCreatingCategory) {
                        CreateCustomCategoryView(
                            onBack = { isCreatingCategory = false },
                            onCategoryCreated = { newCat ->
                                coroutineScope.launch {
                                    val db = KanriDatabase.getDatabase(context)
                                    val newId = withContext(Dispatchers.IO) {
                                        db.categoryDao().insertCategory(newCat)
                                    }
                                    val createdCat = newCat.copy(id = newId)
                                    allCategories = sortCategoriesWithPriority(allCategories + createdCat)
                                    chosenCategoryId = newId
                                    isCreatingCategory = false
                                }
                            }
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.tick()
                                        if (isSearchOpen || searchQuery.isNotEmpty()) {
                                            isSearchOpen = false
                                            searchQuery = ""
                                        } else {
                                            selectedTransaction = null
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to list",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Select Category",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val payee = targetTx.counterparty?.ifBlank { "Transaction" } ?: "Transaction"
                                    Text(
                                        text = "Assign category for '$payee'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            haptics.tick()
                                            isSearchOpen = !isSearchOpen
                                            if (!isSearchOpen) searchQuery = ""
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                                            contentDescription = if (isSearchOpen) "Close search" else "Search category",
                                            tint = if (isSearchOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            haptics.click()
                                            isCreatingCategory = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Create custom category",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            val isCashDeposit = targetTx.reviewReason?.contains("cash deposit", ignoreCase = true) == true ||
                                (!targetTx.isTransfer && targetTx.type.equals("CREDIT", ignoreCase = true) &&
                                 (targetTx.rawSms.contains("CDM", ignoreCase = true) || targetTx.rawSms.contains("cash deposit", ignoreCase = true)))
                            val isAtmSpend = !targetTx.isTransfer && targetTx.sourceType.equals("ATM", ignoreCase = true) && targetTx.type.equals("DEBIT", ignoreCase = true)

                            if (isCashDeposit || isAtmSpend) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isCashDeposit) "Convert to Cash Deposit" else "Convert to Cash Withdrawal",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isCashDeposit) "Move money: Cash → Online" else "Move money: Online → Cash",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                haptics.click()
                                                if (isCashDeposit) {
                                                    onConvertToTransfer?.invoke(targetTx.id, "CASH", "ONLINE")
                                                } else {
                                                    onConvertToTransfer?.invoke(targetTx.id, "ONLINE", "CASH")
                                                }
                                                selectedTransaction = null
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

                            // Search Bar
                            androidx.compose.animation.AnimatedVisibility(visible = isSearchOpen) {
                                Column {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("Search categories (e.g. food, cab, train)...") },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(maxHeight = 300.dp)
                            ) {
                                items(filteredCategories, key = { it.id }) { cat ->
                                    val isSelected = chosenCategoryId == cat.id
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.8.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                haptics.click()
                                                chosenCategoryId = cat.id
                                                val isLendBorrow = cat.name.contains("lend", ignoreCase = true) || cat.name.contains("borrow", ignoreCase = true)
                                                if (isLendBorrow && onOpenLendBorrow != null) {
                                                    onOpenLendBorrow(targetTx)
                                                    selectedTransaction = null
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CategoryIcon(
                                                    categoryName = cat.name,
                                                    iconName = cat.iconName,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // ==========================================
                            // OPTIONAL: ADD INCOME AS SAVINGS FOR A GOAL
                            // ==========================================
                            val isTxCredit = targetTx.type.equals("CREDIT", ignoreCase = true)
                            if (isTxCredit && activeGoals.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GoalIcon(
                                                    iconKey = "savings",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Add to Savings Goal",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Allocate this income directly to a target",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            Switch(
                                                checked = isAllocateToGoalEnabled,
                                                onCheckedChange = { isAllocateToGoalEnabled = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                                    checkedTrackColor = MaterialTheme.colorScheme.onSurface,
                                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            )
                                        }

                                        if (isAllocateToGoalEnabled) {
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = "SELECT GOAL:",
                                                fontFamily = GoogleSansFlex,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                activeGoals.forEach { g ->
                                                    val isGoalSelected = selectedGoalId == g.id
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = if (isGoalSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isGoalSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                                        ),
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable { selectedGoalId = g.id }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            GoalIcon(
                                                                iconKey = g.emoji,
                                                                tint = if (isGoalSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Text(
                                                                text = g.title,
                                                                fontFamily = GoogleSansFlex,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isGoalSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isGoalSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = allocatedGoalAmountStr,
                                                    onValueChange = { allocatedGoalAmountStr = it },
                                                    label = { Text("Amount to Save (₹)", fontFamily = GoogleSansFlex, fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                                    ),
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlex, fontSize = 13.sp),
                                                    modifier = Modifier.weight(1f)
                                                )

                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            allocatedGoalAmountStr = targetTx.amount.toInt().toString()
                                                        }
                                                ) {
                                                    Text(
                                                        text = "Full ₹${targetTx.amount.toInt()}",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Text input box above Confirm button (smaller, normal appearance)
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                placeholder = {
                                    Text(
                                        text = if (isOthersSelected) {
                                            "Specify details for 'Other' (Required)..."
                                        } else {
                                            "Add a note to remember (Optional)..."
                                        },
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Notes,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSansFlex, fontSize = 13.5.sp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    haptics.success()
                                    chosenCategoryId?.let { catId ->
                                        val chosenCat = allCategories.firstOrNull { it.id == catId }
                                        val isLendBorrow = chosenCat?.name?.contains("lend", ignoreCase = true) == true ||
                                                chosenCat?.name?.contains("borrow", ignoreCase = true) == true
                                        if (isLendBorrow && onOpenLendBorrow != null) {
                                            onOpenLendBorrow(targetTx)
                                            selectedTransaction = null
                                        } else {
                                            var finalNote = noteText.trim().ifBlank { null }
                                            if (isAllocateToGoalEnabled && selectedGoalId != null) {
                                                val allocAmount = allocatedGoalAmountStr.toDoubleOrNull() ?: 0.0
                                                val chosenGoal = activeGoals.find { it.id == selectedGoalId }
                                                if (allocAmount > 0.0 && chosenGoal != null) {
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        val newTotal = chosenGoal.currentAmount + allocAmount
                                                        val isDone = chosenGoal.targetAmount > 0 && newTotal >= chosenGoal.targetAmount
                                                        db.savingsGoalDao().updateProgress(chosenGoal.id, newTotal, isDone)
                                                        val payeeName = targetTx.counterparty?.ifBlank { "Income" } ?: "Income"
                                                        db.savingsGoalDao().insertContribution(
                                                            SavingsGoalContributionEntity(
                                                                goalId = chosenGoal.id,
                                                                amount = allocAmount,
                                                                timestamp = System.currentTimeMillis(),
                                                                note = "Income allocation ($payeeName)",
                                                                sourceTransactionId = targetTx.id
                                                            )
                                                        )
                                                    }
                                                    val goalTag = "[Saved ₹${allocAmount.toInt()} for ${chosenGoal.title}]"
                                                    finalNote = if (finalNote != null) "$goalTag $finalNote" else goalTag
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "₹${allocAmount.toInt()} allocated to ${chosenGoal.title}",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                            onAssignCategory(targetTx.id, targetTx.counterparty, catId, finalNote)
                                            selectedTransaction = null
                                        }
                                    }
                                },
                                enabled = isConfirmEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface,
                                    contentColor = MaterialTheme.colorScheme.surface,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            ) {
                                Text(
                                    text = "Confirm",
                                    fontFamily = GoogleSansFlex,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQueueItem(
    transaction: TransactionEntity,
    formattedDate: String,
    onClick: () -> Unit
) {
    val isCredit = transaction.type.equals("CREDIT", ignoreCase = true)
    val amountColor = if (isCredit) ColorIncomeGreen else ColorExpenseRed
    val amountPrefix = if (isCredit) "+ " else "- "
    val merchant = transaction.counterparty?.ifBlank { "Unknown Merchant" } ?: "Unknown Merchant"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Transaction $merchant for $amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}. Tap to categorize."
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = merchant,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Plain yellow text without background pill
                Text(
                    text = if (!transaction.reviewReason.isNullOrBlank()) {
                        transaction.reviewReason
                    } else {
                        "+ Select category"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = ColorAmberBadge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$amountPrefix${CurrencyUtils.formatCurrency(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp
                    ),
                    color = amountColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
