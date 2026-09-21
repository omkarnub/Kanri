package com.omkarnub.kanri.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * Pure Monochrome Frosted Glass Transaction Dialog (Expense & Income):
 * 1. In-window frosted glass overlay with real hardware-accelerated Haze blur across all themes (Dark, AMOLED, Light).
 * 2. Strict monochrome styling matching the app theme (no red/green).
 * 3. Central animated rolling digits with [-] and [+] steppers.
 * 4. Tap-to-type with numeric keyboard and autofocus.
 * 5. Small uniform buttons: +10, +50, +100, +500.
 * 6. In-popup category selection with real vector icons and note input.
 */
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, counterparty: String, sourceType: String, categoryId: Long?) -> Unit,
    initialType: String = "DEBIT",
    categories: List<CategoryEntity> = emptyList(),
    hazeState: HazeState? = LocalHazeState.current
) {
    BackHandler(onBack = onDismiss)

    val glassTheme = rememberKanriGlassTheme()
    val isDebit = initialType.equals("DEBIT", ignoreCase = true)
    val titleText = if (isDebit) "Add Expense" else "Add Income"

    var amount by remember { mutableDoubleStateOf(100.0) }
    var isManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf(amount.toLong().toString()) }
    var payeeText by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("UPI") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val sortedCategories = remember(categories) {
        sortCategoriesWithPriority(categories)
    }

    // Default category selection
    var selectedCategoryId by remember(sortedCategories, isDebit) {
        val defaultCat = if (isDebit) {
            sortedCategories.firstOrNull {
                it.name.contains("food", ignoreCase = true) ||
                        it.name.contains("dining", ignoreCase = true) ||
                        it.name.contains("grocer", ignoreCase = true)
            } ?: sortedCategories.firstOrNull()
        } else {
            sortedCategories.firstOrNull {
                it.name.contains("salary", ignoreCase = true) ||
                        it.name.contains("income", ignoreCase = true)
            } ?: sortedCategories.firstOrNull()
        }
        mutableStateOf<Long?>(defaultCat?.id)
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 1. Semi-transparent backdrop scrim with click-outside dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 2. Centered Frosted Glass Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = glassTheme.shadowElevation + 8.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = glassTheme.shadowColor,
                        ambientColor = glassTheme.shadowColor.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = glassTheme.popupHazeStyle
                            )
                        } else {
                            Modifier
                        }
                    )
                    .border(
                        glassTheme.glassBorder,
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                color = if (hazeState != null) Color.Transparent else glassTheme.fallbackBackgroundColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Title (Pure Monochrome according to theme)
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isDebit) "Log your spent amount, category, and note." else "Log your received amount, category, and note.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // -------------------------------------------------------------
                    // Central Animated Digit with [-] and [+] Stepping Buttons (Frosted)
                    // -------------------------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Decrease Button [-] (Frosted glass button)
                            IconButton(
                                onClick = {
                                    val step = if (amount <= 50) 10.0 else 50.0
                                    val next = (amount - step).coerceAtLeast(0.0)
                                    amount = next
                                    manualText = next.toLong().toString()
                                    isManualInput = false
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .border(
                                        BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Amount",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Central Number / Tap to Edit
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isManualInput) {
                                    OutlinedTextField(
                                        value = manualText,
                                        onValueChange = {
                                            val filtered = it.filter { ch -> ch.isDigit() }
                                            manualText = filtered
                                            val parsed = filtered.toDoubleOrNull()
                                            if (parsed != null) {
                                                amount = parsed
                                                errorMessage = null
                                            }
                                        },
                                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                val parsed = manualText.toDoubleOrNull()
                                                if (parsed != null) {
                                                    amount = parsed
                                                }
                                                isManualInput = false
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                manualText = amount.toLong().toString()
                                                isManualInput = true
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val formattedStr = CurrencyUtils.formatCurrency(amount, includeDecimals = false)
                                        val fontSize = when {
                                            formattedStr.length > 10 -> 20.sp
                                            formattedStr.length > 7 -> 24.sp
                                            else -> 28.sp
                                        }
                                        AnimatedNumberText(
                                            text = formattedStr,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = fontSize,
                                            animateFromZero = false
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Tap to edit",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Tap to type",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Increase Button [+] (Frosted glass button)
                            IconButton(
                                onClick = {
                                    val step = if (amount < 50) 10.0 else 50.0
                                    val next = amount + step
                                    amount = next
                                    manualText = next.toLong().toString()
                                    isManualInput = false
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    .border(
                                        BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Amount",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // Small Uniformly Arranged Quick Increment Buttons (Frosted Glass)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickIncrements = listOf(10.0, 50.0, 100.0, 500.0)
                        quickIncrements.forEach { inc ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        amount += inc
                                        manualText = amount.toLong().toString()
                                        isManualInput = false
                                        errorMessage = null
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "+₹%,d".format(inc.toInt()),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // -------------------------------------------------------------
                    // In-Popup Category Selection (Frosted glass & Highlighted selection)
                    // -------------------------------------------------------------
                    Text(
                        text = "Category",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedCategories, key = { it.id }) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                    }
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedCategoryId = cat.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CategoryIcon(
                                        categoryName = cat.name,
                                        iconName = cat.iconName,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // Payee / Note Field
                    // -------------------------------------------------------------
                    OutlinedTextField(
                        value = payeeText,
                        onValueChange = { payeeText = it },
                        label = { Text(if (isDebit) "Paid to / Note" else "Received from / Note") },
                        placeholder = {
                            Text(
                                if (isDebit) "e.g. Coffee, Grocery, Dinner" else "e.g. Salary, Freelance, Refund",
                                fontSize = 12.5.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // -------------------------------------------------------------
                    // Payment Method / Source Selector (Frosted glass & Highlighted selection)
                    // -------------------------------------------------------------
                    Text(
                        text = "Payment Method",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sourceOptions = listOf("UPI", "Cash", "Card", "Bank")
                        sourceOptions.forEach { source ->
                            val sourceKey = when (source) {
                                "Bank" -> "BANK_TRANSFER"
                                else -> source.uppercase()
                            }
                            val isSelected = selectedSource == sourceKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSource = sourceKey },
                                label = {
                                    Text(
                                        text = source,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = MaterialTheme.colorScheme.onSurface,
                                    selectedBorderWidth = 1.5.dp,
                                    borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                    borderWidth = 0.8.dp
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // -------------------------------------------------------------
                    // Action Buttons: Cancel & Confirm (Monochrome)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (amount <= 0) {
                                    errorMessage = "Please enter an amount greater than 0"
                                    return@Button
                                }
                                val defaultNote = if (isDebit) "Expense" else "Income"
                                val resolvedPayee = payeeText.ifBlank { defaultNote }
                                onConfirm(amount, initialType, resolvedPayee, selectedSource, selectedCategoryId)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isDebit) "Save Expense" else "Save Income",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
