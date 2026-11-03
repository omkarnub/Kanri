package com.omkarnub.kanri.ui.home

import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.SavingsGoalContributionEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDatePickerDialog
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.common.kokonut.DrawerDetailRow
import com.omkarnub.kanri.ui.common.kokonut.KokonutAmountPad
import com.omkarnub.kanri.ui.common.kokonut.KokonutSlideToConfirm
import com.omkarnub.kanri.ui.common.kokonut.KokonutSmoothDrawer
import com.omkarnub.kanri.ui.common.kokonut.SlideConfirmationType
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.savings.GoalIcon
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private var lastSelectedWalletSession: String? = null

enum class AddTransactionStep {
    AMOUNT_PAD,
    FILL_DETAILS,
    SUMMARY_DRAWER
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, counterparty: String, sourceType: String, categoryId: Long?, wallet: String?) -> Unit,
    initialType: String = "DEBIT",
    categories: List<CategoryEntity> = emptyList(),
    onOpenLendBorrow: ((amount: Double, type: String, counterparty: String, note: String) -> Unit)? = null,
    hazeState: HazeState? = LocalHazeState.current
) {
    AddTransactionDialog(
        onDismiss = onDismiss,
        onConfirm = { amount, type, counterparty, sourceType, categoryId, wallet, _ ->
            onConfirm(amount, type, counterparty, sourceType, categoryId, wallet)
        },
        initialType = initialType,
        categories = categories,
        onOpenLendBorrow = onOpenLendBorrow,
        hazeState = hazeState
    )
}

@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, counterparty: String, sourceType: String, categoryId: Long?, wallet: String?, timestamp: Long) -> Unit,
    initialType: String = "DEBIT",
    categories: List<CategoryEntity> = emptyList(),
    onOpenLendBorrow: ((amount: Double, type: String, counterparty: String, note: String) -> Unit)? = null,
    hazeState: HazeState? = LocalHazeState.current
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val db = remember { KanriDatabase.getDatabase(context) }
    val activeGoalsFlow = remember { db.savingsGoalDao().getActiveGoals() }
    val activeGoals by activeGoalsFlow.collectAsState(initial = emptyList())

    val glassTheme = rememberKanriGlassTheme()
    val isDebit = initialType.equals("DEBIT", ignoreCase = true)
    val titleText = if (isDebit) "Add Expense" else "Add Income"
    var currentStep by remember { mutableStateOf(AddTransactionStep.AMOUNT_PAD) }

    // Starts from ZERO (0.0)
    var amount by remember { mutableDoubleStateOf(0.0) }
    var payeeText by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("UPI") }
    var selectedWallet by remember {
        mutableStateOf(
            lastSelectedWalletSession ?: if (selectedSource.equals("CASH", ignoreCase = true)) "CASH" else "ONLINE"
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var isAllocateToGoalEnabled by remember { mutableStateOf(false) }
    var selectedGoalId by remember(activeGoals) {
        mutableStateOf<Long?>(activeGoals.firstOrNull()?.id)
    }

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

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    var transactionTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun performDismiss() {
        if (!isVisible) return
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    BackHandler(onBack = {
        when (currentStep) {
            AddTransactionStep.AMOUNT_PAD -> performDismiss()
            AddTransactionStep.FILL_DETAILS -> currentStep = AddTransactionStep.AMOUNT_PAD
            AddTransactionStep.SUMMARY_DRAWER -> currentStep = AddTransactionStep.FILL_DETAILS
        }
    })

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.55f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "dialogScrimAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Semi-transparent backdrop scrim with click-outside dismiss
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (currentStep == AddTransactionStep.SUMMARY_DRAWER) {
                            currentStep = AddTransactionStep.FILL_DETAILS
                        } else if (currentStep == AddTransactionStep.AMOUNT_PAD) {
                            performDismiss()
                        }
                    }
                )
        )

        // 2. Animated popup entrance and step transitions: AMOUNT_PAD -> FILL_DETAILS -> SUMMARY_DRAWER
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(150)),
            modifier = Modifier.fillMaxSize()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState == AddTransactionStep.SUMMARY_DRAWER) {
                        (slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f)
                        ) + fadeIn(tween(180))).togetherWith(
                            slideOutVertically(
                                targetOffsetY = { it / 4 },
                                animationSpec = tween(180)
                            ) + fadeOut(tween(140))
                        )
                    } else if (initialState == AddTransactionStep.SUMMARY_DRAWER) {
                        (slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(180)
                        ) + fadeIn(tween(180))).togetherWith(
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(160))
                        )
                    } else if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                        ) + fadeIn(tween(200))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { -it / 3 },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(150))
                        )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                        ) + fadeIn(tween(200))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ) + fadeOut(tween(150))
                        )
                    }
                },
                label = "addTransactionStepTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    AddTransactionStep.AMOUNT_PAD -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            KokonutAmountPad(
                                label = if (isDebit) "Add Expense" else "Add Income",
                                recipientName = if (isDebit) "Expense" else "Income",
                                initialAmount = amount,
                                currency = CurrencyUtils.getCurrencySymbol(),
                                showHandle = true,
                                onClose = { performDismiss() },
                                hazeState = hazeState,
                                onContinue = { entered ->
                                    amount = entered
                                    errorMessage = null
                                    currentStep = AddTransactionStep.FILL_DETAILS
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding()
                            )
                        }
                    }

                    AddTransactionStep.FILL_DETAILS -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .imePadding()
                                .navigationBarsPadding(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            KanriGlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                                level = GlassLevel.Strong,
                                hazeState = hazeState
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 22.dp, vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Header Title
                                    Text(
                                        text = titleText,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = if (isDebit) "Log your spent details, category, and payment info." else "Log your received income details and payment info.",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )

                                    // Sleek Amount Summary Card with Edit Affordance (replaces duplicate steppers)
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                haptics.tick()
                                                currentStep = AddTransactionStep.AMOUNT_PAD
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = if (isDebit) "SPENT AMOUNT" else "RECEIVED AMOUNT",
                                                    fontSize = 11.sp,
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.SemiBold,
                                                    letterSpacing = 1.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val formatted = CurrencyUtils.formatCurrency(amount, includeDecimals = (amount % 1.0 != 0.0))
                                                Text(
                                                    text = formatted,
                                                    fontSize = 28.sp,
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    letterSpacing = (-0.02).sp
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Amount",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "Edit",
                                                        fontSize = 12.sp,
                                                        fontFamily = GoogleSansFlex,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Date & Time Selection (Apple Glass Chips)
                                    Text(
                                        text = "Date & Time",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Date Chip
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptics.tick()
                                                    showDatePicker = true
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = "Date",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(transactionTimestamp)),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        }

                                        // Time Chip
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    haptics.tick()
                                                    val cal = Calendar.getInstance().apply { timeInMillis = transactionTimestamp }
                                                    val is24Hr = android.text.format.DateFormat.is24HourFormat(context)
                                                    TimePickerDialog(
                                                        context,
                                                        { _, h, m ->
                                                            val updatedCal = Calendar.getInstance().apply {
                                                                timeInMillis = transactionTimestamp
                                                                set(Calendar.HOUR_OF_DAY, h)
                                                                set(Calendar.MINUTE, m)
                                                                set(Calendar.SECOND, 0)
                                                            }
                                                            transactionTimestamp = updatedCal.timeInMillis
                                                        },
                                                        cal.get(Calendar.HOUR_OF_DAY),
                                                        cal.get(Calendar.MINUTE),
                                                        is24Hr
                                                    ).show()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AccessTime,
                                                    contentDescription = "Time",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transactionTimestamp)),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // In-Popup Category Selection
                                    Text(
                                        text = "Category",
                                        fontFamily = GoogleSansFlex,
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
                                                    .clickable {
                                                        haptics.tick()
                                                        selectedCategoryId = cat.id
                                                        if (cat.name.contains("lend", ignoreCase = true) && onOpenLendBorrow != null) {
                                                            haptics.click()
                                                            onOpenLendBorrow(
                                                                amount,
                                                                if (isDebit) "LENT" else "BORROWED",
                                                                payeeText.trim(),
                                                                ""
                                                            )
                                                        }
                                                    }
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
                                                        fontFamily = GoogleSansFlex,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Payee / Note Field
                                    OutlinedTextField(
                                        value = payeeText,
                                        onValueChange = { payeeText = it },
                                        label = { Text(if (isDebit) "Paid to / Note" else "Received from / Note", fontFamily = GoogleSansFlex) },
                                        placeholder = {
                                            Text(
                                                if (isDebit) "e.g. Coffee, Grocery, Dinner" else "e.g. Salary, Freelance, Refund",
                                                fontFamily = GoogleSansFlex,
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

                                    // Payment Method / Source Selector
                                    Text(
                                        text = "Payment Method",
                                        fontFamily = GoogleSansFlex,
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
                                                onClick = {
                                                    haptics.tick()
                                                    selectedSource = sourceKey
                                                    if (sourceKey.equals("CASH", ignoreCase = true)) {
                                                        selectedWallet = "CASH"
                                                    } else if (selectedWallet == "CASH") {
                                                        selectedWallet = "ONLINE"
                                                    }
                                                },
                                                label = {
                                                    Text(
                                                        text = source,
                                                        fontSize = 11.sp,
                                                        fontFamily = GoogleSansFlex,
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

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Wallet Impact Selector (Cash | Online | Not from balances)
                                    Text(
                                        text = "Wallet Impact",
                                        fontFamily = GoogleSansFlex,
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
                                        listOf("CASH" to "Cash", "ONLINE" to "Online", "NONE" to "Not from balances").forEach { (key, label) ->
                                            val isSelected = selectedWallet == key
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    haptics.tick()
                                                    selectedWallet = key
                                                },
                                                label = {
                                                    Text(
                                                        text = label,
                                                        fontSize = 11.sp,
                                                        fontFamily = GoogleSansFlex,
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

                                    // Allocate Income to Savings Goal
                                    if (!isDebit && activeGoals.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
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
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        GoalIcon(
                                                            iconKey = "savings",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "Add to Savings Goal",
                                                                fontFamily = GoogleSansFlex,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Save this income towards your goal",
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
                                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                    )
                                                }

                                                if (isAllocateToGoalEnabled) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text = "CHOOSE GOAL:",
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
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        activeGoals.forEach { g ->
                                                            val isGoalSelected = selectedGoalId == g.id
                                                            Surface(
                                                                shape = RoundedCornerShape(10.dp),
                                                                color = if (isGoalSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (isGoalSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.outlineVariant
                                                                ),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .clickable { selectedGoalId = g.id }
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                                ) {
                                                                    GoalIcon(
                                                                        iconKey = g.emoji,
                                                                        tint = if (isGoalSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
                                                                        modifier = Modifier.size(15.dp)
                                                                    )
                                                                    Text(
                                                                        text = g.title,
                                                                        fontFamily = GoogleSansFlex,
                                                                        fontSize = 11.5.sp,
                                                                        fontWeight = if (isGoalSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = if (isGoalSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (errorMessage != null) {
                                        Text(
                                            text = errorMessage ?: "",
                                            color = MaterialTheme.colorScheme.error,
                                            fontFamily = GoogleSansFlex,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Action Buttons: Back & Proceed
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = {
                                            haptics.tick()
                                            currentStep = AddTransactionStep.AMOUNT_PAD
                                        }) {
                                            Text(
                                                text = "Back",
                                                fontFamily = GoogleSansFlex,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            TextButton(onClick = {
                                                haptics.tick()
                                                performDismiss()
                                            }) {
                                                Text(
                                                    text = "Cancel",
                                                    fontFamily = GoogleSansFlex,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    if (amount <= 0) {
                                                        errorMessage = "Please enter an amount greater than 0"
                                                        return@Button
                                                    }
                                                    val selectedCat = sortedCategories.firstOrNull { it.id == selectedCategoryId }
                                                    if (selectedCat?.name?.contains("lend", ignoreCase = true) == true && onOpenLendBorrow != null) {
                                                        onOpenLendBorrow(
                                                            amount,
                                                            if (isDebit) "LENT" else "BORROWED",
                                                            payeeText.trim(),
                                                            ""
                                                        )
                                                        return@Button
                                                    }
                                                    currentStep = AddTransactionStep.SUMMARY_DRAWER
                                                },
                                                colors = kanriPrimaryButtonColors(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = "Proceed",
                                                    fontFamily = GoogleSansFlex,
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

                    AddTransactionStep.SUMMARY_DRAWER -> {
                        val selectedCategory = sortedCategories.firstOrNull { it.id == selectedCategoryId }
                        val defaultNote = if (isDebit) "Expense" else "Income"
                        var resolvedPayee = payeeText.ifBlank { defaultNote }

                        val detailRows = listOfNotNull(
                            DrawerDetailRow(title = "Type", value = if (isDebit) "Expense" else "Income"),
                            DrawerDetailRow(title = "Category", value = selectedCategory?.name ?: "General"),
                            DrawerDetailRow(title = "Date & Time", value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transactionTimestamp))),
                            if (payeeText.isNotBlank()) DrawerDetailRow(title = "Note / Payee", value = payeeText) else null,
                            DrawerDetailRow(title = "Payment", value = selectedSource),
                            DrawerDetailRow(title = "Account", value = if (selectedWallet == "ONLINE") "Online" else if (selectedWallet == "CASH") "Cash" else "Unlinked")
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            KokonutSmoothDrawer(
                                title = if (isDebit) "Review expense" else "Review income",
                                description = "${selectedCategory?.name ?: "General"} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transactionTimestamp))}",
                                totalAmount = amount,
                                slideLabel = if (isDebit) "Slide to save expense" else "Slide to save income",
                                detailRows = detailRows,
                                accountType = if (selectedWallet == "ONLINE") "Online" else if (selectedWallet == "CASH") "Cash" else "Unlinked",
                                confirmationType = if (isDebit) SlideConfirmationType.EXPENSE else SlideConfirmationType.INCOME,
                                hazeState = hazeState,
                                onConfirm = {
                                    if (!isDebit && isAllocateToGoalEnabled && selectedGoalId != null) {
                                        val chosenGoal = activeGoals.find { it.id == selectedGoalId }
                                        if (chosenGoal != null) {
                                            scope.launch(Dispatchers.IO) {
                                                val newTotal = chosenGoal.currentAmount + amount
                                                val isDone = chosenGoal.targetAmount > 0 && newTotal >= chosenGoal.targetAmount
                                                db.savingsGoalDao().updateProgress(chosenGoal.id, newTotal, isDone)
                                                db.savingsGoalDao().insertContribution(
                                                    SavingsGoalContributionEntity(
                                                        goalId = chosenGoal.id,
                                                        amount = amount,
                                                        timestamp = transactionTimestamp,
                                                        note = "Income: $resolvedPayee"
                                                    )
                                                )
                                            }
                                            resolvedPayee = "[Saved ₹${amount.toInt()} for ${chosenGoal.title}] $resolvedPayee"
                                            android.widget.Toast.makeText(
                                                context,
                                                "₹${amount.toInt()} allocated to ${chosenGoal.title}",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    lastSelectedWalletSession = selectedWallet
                                    onConfirm(amount, initialType, resolvedPayee, selectedSource, selectedCategoryId, selectedWallet, transactionTimestamp)
                                },
                                onSuccessComplete = {
                                    performDismiss()
                                },
                                onDismiss = {
                                    currentStep = AddTransactionStep.FILL_DETAILS
                                }
                            )
                        }
                    }
                }
            }
        }

        // Material Date Picker Dialog for Transaction Date
        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = transactionTimestamp
            )
            KanriGlassDatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selected = datePickerState.selectedDateMillis
                            if (selected != null) {
                                val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = selected
                                }
                                val calOrig = Calendar.getInstance().apply {
                                    timeInMillis = transactionTimestamp
                                }
                                val calTarget = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, calUtc.get(Calendar.YEAR))
                                    set(Calendar.MONTH, calUtc.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, calUtc.get(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, calOrig.get(Calendar.HOUR_OF_DAY))
                                    set(Calendar.MINUTE, calOrig.get(Calendar.MINUTE))
                                    set(Calendar.SECOND, calOrig.get(Calendar.SECOND))
                                }
                                transactionTimestamp = calTarget.timeInMillis
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
