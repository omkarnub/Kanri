package com.omkarnub.kanri.ui.settings

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.ui.common.getCategoryIconRes
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Smart Merchant Auto-Categorization Rule Editor.
 * Allows users to view, test, create, edit, and delete keyword auto-categorization rules.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartMerchantRulesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { KanriDatabase.getDatabase(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val rulesFlow = remember { db.categoryDao().getAllMappingsFlow() }
    val rules by rulesFlow.collectAsState(initial = emptyList())

    val categoriesFlow = remember { db.categoryDao().getAllCategories() }
    val categories by categoriesFlow.collectAsState(initial = emptyList())

    val categoryMap = remember(categories) {
        categories.associateBy { it.id }
    }

    var testQuery by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CounterpartyCategoryMapEntity?>(null) }
    var ruleToDelete by remember { mutableStateOf<CounterpartyCategoryMapEntity?>(null) }

    // Initial pre-fill for new rule
    var prefillKeyword by remember { mutableStateOf("") }
    var prefillCategoryId by remember { mutableStateOf<Long?>(null) }

    // Popular starter templates
    val starterTemplates = remember {
        listOf(
            "Swiggy" to "Food & Dining",
            "Zomato" to "Food & Dining",
            "Starbucks" to "Food & Dining",
            "Uber" to "Transport & Fuel",
            "Ola" to "Transport & Fuel",
            "Netflix" to "Entertainment",
            "Amazon" to "Shopping",
            "Flipkart" to "Shopping",
            "Blinkit" to "Groceries",
            "Zepto" to "Groceries"
        )
    }

    // Filtered rules
    val filteredRules = remember(rules, filterText) {
        if (filterText.isBlank()) rules
        else rules.filter { it.counterparty.contains(filterText.trim(), ignoreCase = true) }
    }

    // Real-time matching simulator
    val simulatedMatch = remember(testQuery, rules) {
        val trimmed = testQuery.trim()
        if (trimmed.length >= 2) {
            rules.filter { trimmed.contains(it.counterparty, ignoreCase = true) }
                .maxByOrNull { it.counterparty.length }
        } else null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.tick()
                            onBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Merchant Rules",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = GoogleSansFlex
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${rules.size} automated categorization rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Add Rule Action Button
                    IconButton(
                        onClick = {
                            haptics.tick()
                            prefillKeyword = ""
                            prefillCategoryId = null
                            editingRule = null
                            showAddDialog = true
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Rule",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. Interactive Rule Simulator Card
            // -----------------------------------------------------------------
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Rule Simulator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = GoogleSansFlex,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Test how any merchant name or SMS alert will be classified.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = testQuery,
                            onValueChange = { testQuery = it },
                            placeholder = { Text("e.g. Starbucks, Swiggy, Uber Eats...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (testQuery.isNotEmpty()) {
                                    IconButton(onClick = { testQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Simulator Result Display
                        AnimatedVisibility(
                            visible = testQuery.trim().length >= 2,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Spacer(modifier = Modifier.height(10.dp))
                            if (simulatedMatch != null) {
                                val matchedCat = categoryMap[simulatedMatch.categoryId]
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF30A46C).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFF30A46C).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF30A46C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Matches rule '${simulatedMatch.counterparty}'",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Auto-assigns to: ${matchedCat?.name ?: "Unknown"}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF30A46C)
                                            )
                                        }
                                        if (matchedCat != null) {
                                            Icon(
                                                painter = painterResource(id = getCategoryIconRes(matchedCat.name, matchedCat.iconName)),
                                                contentDescription = null,
                                                tint = Color(0xFF30A46C),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "No rule matched",
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Transactions will be marked 'Other' or 'Needs Review'",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                haptics.tick()
                                                prefillKeyword = testQuery.trim()
                                                prefillCategoryId = null
                                                editingRule = null
                                                showAddDialog = true
                                            }
                                        ) {
                                            Text(
                                                text = "+ Add Rule",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. Starter Suggestions (Chips)
            // -----------------------------------------------------------------
            item {
                Column {
                    Text(
                        text = "POPULAR SUGGESTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        starterTemplates.forEach { (keyword, catName) ->
                            val alreadyHas = rules.any { it.counterparty.equals(keyword, ignoreCase = true) }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (alreadyHas) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    0.8.dp,
                                    if (alreadyHas) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !alreadyHas) {
                                        haptics.tick()
                                        prefillKeyword = keyword
                                        prefillCategoryId = categories.firstOrNull { it.name.equals(catName, ignoreCase = true) }?.id
                                        editingRule = null
                                        showAddDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (alreadyHas) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF30A46C),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    Text(
                                        text = "$keyword → $catName",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (alreadyHas) FontWeight.Normal else FontWeight.Medium,
                                        color = if (alreadyHas) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. Search & Rules Header
            // -----------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE RULES (${filteredRules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (rules.size > 5) {
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            placeholder = { Text("Filter...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .width(160.dp)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 4. Rules List
            // -----------------------------------------------------------------
            if (filteredRules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (filterText.isNotBlank()) "No rules match \"$filterText\""
                                    else "No merchant rules configured yet",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap '+ Add Rule' or pick from popular suggestions above to auto-classify your spends.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredRules, key = { it.counterparty }) { rule ->
                    val targetCategory = categoryMap[rule.categoryId]
                    RuleItemCard(
                        rule = rule,
                        category = targetCategory,
                        onEdit = {
                            haptics.tick()
                            editingRule = rule
                            prefillKeyword = rule.counterparty
                            prefillCategoryId = rule.categoryId
                            showAddDialog = true
                        },
                        onDelete = {
                            haptics.tick()
                            ruleToDelete = rule
                        },
                        onApplyToPast = {
                            haptics.tick()
                            scope.launch {
                                val count = withContext(Dispatchers.IO) {
                                    db.transactionDao().applyCategoryToMatchingTransactions(
                                        keyword = rule.counterparty,
                                        categoryId = rule.categoryId
                                    )
                                }
                                KanriWidgetsUpdater.updateAllWidgets(context)
                                snackbarHostState.showSnackbar(
                                    message = "Applied '${targetCategory?.name ?: "Category"}' to $count past transactions"
                                )
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // -------------------------------------------------------------------------
    // Add / Edit Rule Dialog
    // -------------------------------------------------------------------------
    if (showAddDialog) {
        AddEditRuleDialog(
            isEdit = editingRule != null,
            initialKeyword = prefillKeyword,
            initialCategoryId = prefillCategoryId,
            categories = categories,
            onDismiss = { showAddDialog = false },
            onSave = { keyword, categoryId, applyToPast ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        // If editing and keyword changed, remove old mapping
                        if (editingRule != null && !editingRule!!.counterparty.equals(keyword, ignoreCase = true)) {
                            db.categoryDao().deleteMapping(editingRule!!.counterparty)
                        }
                        db.categoryDao().setMapping(
                            CounterpartyCategoryMapEntity(
                                counterparty = keyword.trim(),
                                categoryId = categoryId
                            )
                        )
                        if (applyToPast) {
                            val affected = db.transactionDao().applyCategoryToMatchingTransactions(
                                keyword = keyword.trim(),
                                categoryId = categoryId
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Saved rule & updated $affected transactions", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    KanriWidgetsUpdater.updateAllWidgets(context)
                    showAddDialog = false
                }
            }
        )
    }

    // -------------------------------------------------------------------------
    // Delete Confirmation Dialog
    // -------------------------------------------------------------------------
    if (ruleToDelete != null) {
        val rule = ruleToDelete!!
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Rule?",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Remove auto-categorization rule for \"${rule.counterparty}\"? Existing transactions will remain untouched.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.tick()
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                db.categoryDao().deleteMapping(rule.counterparty)
                            }
                            KanriWidgetsUpdater.updateAllWidgets(context)
                            ruleToDelete = null
                            snackbarHostState.showSnackbar("Deleted rule for '${rule.counterparty}'")
                        }
                    }
                ) {
                    Text(text = "Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Individual Rule Item Card in the list.
 */
@Composable
private fun RuleItemCard(
    rule: CounterpartyCategoryMapEntity,
    category: CategoryEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApplyToPast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Keyword Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = rule.counterparty,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "→",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Category Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (category != null) {
                        Icon(
                            painter = painterResource(id = getCategoryIconRes(category.name, category.iconName)),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = category?.name ?: "Unknown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Apply To Past Button
            IconButton(
                onClick = onApplyToPast,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = "Apply to past transactions",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Edit Button
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Modal dialog to add or edit an auto-categorization rule.
 */
@Composable
private fun AddEditRuleDialog(
    isEdit: Boolean,
    initialKeyword: String,
    initialCategoryId: Long?,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (keyword: String, categoryId: Long, applyToPast: Boolean) -> Unit
) {
    val haptics = rememberKanriHaptics()
    var keyword by remember { mutableStateOf(initialKeyword) }
    var selectedCategoryId by remember {
        mutableStateOf<Long?>(initialCategoryId ?: categories.firstOrNull()?.id)
    }
    var applyToPast by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isEdit) "Edit Merchant Rule" else "Add Merchant Rule",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Transactions whose merchant name contains this keyword will be categorized automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Keyword input
                OutlinedTextField(
                    value = keyword,
                    onValueChange = {
                        keyword = it
                        errorMessage = null
                    },
                    label = { Text("Merchant Keyword") },
                    placeholder = { Text("e.g. Starbucks, Uber, Swiggy") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Target Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category selector chips (horizontal scroll)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.8.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.tick()
                                    selectedCategoryId = cat.id
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = getCategoryIconRes(cat.name, cat.iconName)),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Retroactive checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { applyToPast = !applyToPast }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = applyToPast,
                        onCheckedChange = { applyToPast = it },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply to existing matching transactions",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        onClick = {
                            val trimmed = keyword.trim()
                            if (trimmed.isBlank()) {
                                errorMessage = "Keyword cannot be empty"
                                return@TextButton
                            }
                            val targetCatId = selectedCategoryId
                            if (targetCatId == null) {
                                errorMessage = "Please select a category"
                                return@TextButton
                            }
                            haptics.tick()
                            onSave(trimmed, targetCatId, applyToPast)
                        }
                    ) {
                        Text(
                            text = if (isEdit) "Save Changes" else "Add Rule",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
