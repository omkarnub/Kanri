package com.omkarnub.kanri.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.input.KeyboardType
import com.omkarnub.kanri.data.db.SavingsGoalContributionEntity
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.ui.savings.GoalIcon
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.TransactionWithCategory
import com.omkarnub.kanri.ui.common.AVAILABLE_CATEGORY_ICONS
import com.omkarnub.kanri.ui.common.CategoryIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val DEFAULT_CATEGORY_NAMES_SET = setOf(
    "auto / taxi fare", "auto", "taxi", "cab",
    "food & dining", "food",
    "train & metro", "train", "metro",
    "other", "others",
    "groceries",
    "shopping",
    "bills & utilities",
    "transport & fuel", "transport",
    "entertainment",
    "health & medical",
    "salary & income",
    "investment & savings",
    "education",
    "cash & atm",
    "rent & maintenance",
    "personal care",
    "gifts & donations",
    "travel & vacation"
)

fun isCustomCategory(category: CategoryEntity): Boolean {
    if (category.isCustom) return true
    val clean = category.name.trim().lowercase()
    return clean !in DEFAULT_CATEGORY_NAMES_SET
}

/**
 * Sorts categories prioritizing:
 * 1. User-created custom categories on TOP (priority 0).
 * 2. Everyday essentials (Auto/Taxi, Food, Train, Other).
 * 3. Standard categories in the middle.
 */
fun sortCategoriesWithPriority(categories: List<CategoryEntity>): List<CategoryEntity> {
    fun priorityScore(cat: CategoryEntity): Int {
        if (isCustomCategory(cat)) {
            return 0 // Custom categories created by the user ALWAYS on top!
        }
        val lower = cat.name.lowercase().trim()
        return when {
            lower.contains("taxi") || lower.contains("auto") || lower.contains("cab") -> 1
            lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") -> 2
            lower.contains("train") || lower.contains("metro") || lower.contains("railway") -> 3
            lower == "other" || lower == "others" -> 4 // Top side like other categories
            lower.contains("grocer") -> 5
            lower.contains("shopping") || lower.contains("shop") -> 6
            lower.contains("bill") || lower.contains("utilit") -> 7
            lower.contains("transport") || lower.contains("fuel") -> 8
            lower.contains("entertainment") || lower.contains("game") -> 9
            lower.contains("health") || lower.contains("medic") -> 10
            lower.contains("salary") || lower.contains("income") -> 11
            lower.contains("invest") -> 12
            lower.contains("educat") -> 13
            lower.contains("cash") || lower.contains("atm") -> 14
            lower.contains("rent") || lower.contains("home") -> 15
            lower.contains("personal") -> 16
            lower.contains("gift") -> 17
            lower.contains("travel") || lower.contains("flight") -> 18
            else -> 0 // Any unrecognized category also goes to top!
        }
    }
    // Deduplicate categories and normalize "Others" to "Other"
    val deduplicated = categories.map {
        if (it.name.equals("Others", ignoreCase = true)) it.copy(name = "Other") else it
    }.distinctBy {
        val n = it.name.lowercase().trim()
        if (n == "transport") "transport & fuel" else n
    }
    return deduplicated.sortedWith(
        compareBy<CategoryEntity>({ priorityScore(it) })
            .thenByDescending { it.id } // Newest custom categories first
            .thenBy { it.name }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    targetTransaction: TransactionWithCategory,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onCategorySelected: (categoryId: Long, note: String?) -> Unit,
    onOpenLendBorrow: ((TransactionWithCategory) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val haptics = rememberKanriHaptics()

    var chosenCategoryId by remember(targetTransaction) {
        mutableStateOf<Long?>(targetTransaction.category?.id)
    }
    var noteText by remember(targetTransaction) {
        mutableStateOf(targetTransaction.transaction.notes ?: "")
    }

    val isIncome = targetTransaction.transaction.type.equals("CREDIT", ignoreCase = true)
    val db = remember { KanriDatabase.getDatabase(context) }
    val activeGoalsFlow = remember { db.savingsGoalDao().getActiveGoals() }
    val activeGoals by activeGoalsFlow.collectAsState(initial = emptyList())

    var isAllocateToGoalEnabled by remember { mutableStateOf(false) }
    var selectedGoalId by remember(activeGoals) {
        mutableStateOf<Long?>(activeGoals.firstOrNull()?.id)
    }
    var allocatedGoalAmountStr by remember(targetTransaction) {
        mutableStateOf(targetTransaction.transaction.amount.toInt().toString())
    }

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingCategory by remember { mutableStateOf(false) }

    // Dynamic category list that updates when custom category is created
    var allCategories by remember(categories) {
        mutableStateOf(sortCategoriesWithPriority(categories))
    }

    // Step-by-step BackHandler: One back press only goes back one step!
    // Sheet does not dismiss on single back press at root; top notch is used to dismiss.
    BackHandler(enabled = true) {
        when {
            isCreatingCategory -> {
                isCreatingCategory = false
            }
            isSearchOpen || searchQuery.isNotEmpty() -> {
                isSearchOpen = false
                searchQuery = ""
            }
            chosenCategoryId != null && chosenCategoryId != targetTransaction.category?.id -> {
                chosenCategoryId = targetTransaction.category?.id
            }
            else -> {
                // At root level: Do not close the entire menu on a single back press.
                // The top notch of the popup is there to fully close it.
            }
        }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // Prominent, interactive top notch: click or drag to close the popup
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            AnimatedContent(
                targetState = isCreatingCategory,
                transitionSpec = {
                    if (targetState) {
                        (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                    } else {
                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith (slideOutVertically { it / 2 } + fadeOut())
                    }
                },
                label = "CategoryPickerContentTransition"
            ) { creating ->
                if (creating) {
                    // ==========================================
                    // VIEW 1: CREATE CUSTOM CATEGORY
                    // ==========================================
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
                    // ==========================================
                    // VIEW 2: FULL LIST + SEARCH + NOTE + CONFIRM
                    // ==========================================
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with Title and Search / Add Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Select Category",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val payee = targetTransaction.transaction.counterparty?.ifBlank { "Transaction" } ?: "Transaction"
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
                                // Search Icon Button
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

                                // + Button: Create Custom Category
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

                        // Search Input Bar (smoothly expands when search icon clicked)
                        AnimatedVisibility(visible = isSearchOpen) {
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

                        // Category Grid
                        if (filteredCategories.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No category found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Tap '+' above to create a custom category",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .sizeIn(maxHeight = 310.dp)
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
                                                    onOpenLendBorrow(targetTransaction)
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
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CategoryIcon(
                                                    categoryName = cat.name,
                                                    iconName = cat.iconName,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(17.dp)
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
                        }

                        // ==========================================
                        // OPTION: ADD INCOME TO SAVINGS GOAL
                        // ==========================================
                        if (isIncome && activeGoals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllocateToGoalEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isAllocateToGoalEnabled = !isAllocateToGoalEnabled },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isAllocateToGoalEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    GoalIcon(
                                                        iconKey = "savings",
                                                        tint = if (isAllocateToGoalEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = "Add to Savings Goal",
                                                    fontFamily = GoogleSansFlex,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Allocate this income as savings for a goal",
                                                    fontFamily = GoogleSansFlex,
                                                    style = MaterialTheme.typography.bodySmall,
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
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }

                                    if (isAllocateToGoalEnabled) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Goal Chips
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
                                                        .clickable {
                                                            haptics.tick()
                                                            selectedGoalId = g.id
                                                        }
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

                                        // Amount to allocate input
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
                                                        haptics.tick()
                                                        allocatedGoalAmountStr = targetTransaction.transaction.amount.toInt().toString()
                                                    }
                                            ) {
                                                Text(
                                                    text = "Full ₹${targetTransaction.transaction.amount.toInt()}",
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // ==========================================
                        // TEXT INPUT BOX ABOVE CONFIRM BUTTON
                        // Smaller, sleek, normal appearance (no red error color)
                        // ==========================================
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

                        // Confirm Button
                        Button(
                            onClick = {
                                haptics.success()
                                chosenCategoryId?.let { catId ->
                                    val chosenCat = allCategories.firstOrNull { it.id == catId }
                                    val isLendBorrow = chosenCat?.name?.contains("lend", ignoreCase = true) == true ||
                                            chosenCat?.name?.contains("borrow", ignoreCase = true) == true
                                    if (isLendBorrow && onOpenLendBorrow != null) {
                                        onOpenLendBorrow(targetTransaction)
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
                                                    val payeeName = targetTransaction.transaction.counterparty?.ifBlank { "Income" } ?: "Income"
                                                    db.savingsGoalDao().insertContribution(
                                                        SavingsGoalContributionEntity(
                                                            goalId = chosenGoal.id,
                                                            amount = allocAmount,
                                                            timestamp = System.currentTimeMillis(),
                                                            note = "Income allocation ($payeeName)",
                                                            sourceTransactionId = targetTransaction.transaction.id
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
                                        onCategorySelected(catId, finalNote)
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

/**
 * Clean sub-view for creating a custom category with SVG icons.
 */
@Composable
fun CreateCustomCategoryView(
    onBack: () -> Unit,
    onCategoryCreated: (CategoryEntity) -> Unit
) {
    val haptics = rememberKanriHaptics()
    var name by remember { mutableStateOf("") }
    var selectedIconName by remember { mutableStateOf(AVAILABLE_CATEGORY_ICONS.first().iconName) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top row with Back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = {
                    haptics.tick()
                    onBack()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Custom Category",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Category Name
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Category Name") },
            placeholder = { Text("e.g. Gym, Coffee, Pet Care") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Choose Icon (SVG Icon Picker)
        Text(
            text = "Select Icon",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxHeight = 180.dp)
        ) {
            items(AVAILABLE_CATEGORY_ICONS, key = { it.iconName }) { iconOpt ->
                val isSelected = selectedIconName == iconOpt.iconName
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            haptics.tick()
                            selectedIconName = iconOpt.iconName
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIcon(
                            categoryName = null,
                            iconName = iconOpt.iconName,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Save Button
        Button(
            onClick = {
                if (name.trim().isNotBlank()) {
                    haptics.success()
                    onCategoryCreated(
                        CategoryEntity(
                            name = name.trim(),
                            colorHex = "#6366F1",
                            iconName = selectedIconName,
                            isCustom = true
                        )
                    )
                }
            },
            enabled = name.trim().isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Text("Save Category", fontWeight = FontWeight.SemiBold)
        }
    }
}

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(colorInt or 0x00000000FF000000)
        } else {
            Color(colorInt)
        }
    } catch (_: Exception) {
        Color(0xFF8D6E63)
    }
}
