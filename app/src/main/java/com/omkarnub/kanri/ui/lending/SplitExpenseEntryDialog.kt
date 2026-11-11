package com.omkarnub.kanri.ui.lending

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.applyWindowBlur
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.common.kokonut.DrawerDetailRow
import com.omkarnub.kanri.ui.common.kokonut.KokonutAmountPad
import com.omkarnub.kanri.ui.common.kokonut.KokonutSlideToConfirm
import com.omkarnub.kanri.ui.common.kokonut.KokonutSmoothDrawer
import com.omkarnub.kanri.ui.common.kokonut.SlideConfirmationType
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private val SageGreen = Color(0xFF30A46C)
private val AmberGold = Color(0xFFF59E0B)
private val ExpenseRed = Color(0xFFE54D2E)

enum class SplitExpenseStep {
    AMOUNT_PAD,
    FILL_DETAILS,
    SUMMARY_DRAWER
}

/**
 * Modern unified Pop-up Workflow for Splitting Bills and Group Expenses:
 * 1. KokonutAmountPad: Fluid zero-start amount keypad
 * 2. Bottom Card Fill Details: Role, event description, people count stepper, friend slots
 * 3. KokonutSmoothDrawer: Slide-to-confirm with haptic feedback
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseEntryDialog(
    onDismiss: () -> Unit,
    onSaveLenderSplit: (friendNames: List<String>, perPersonShare: Double, eventDescription: String) -> Unit,
    onSaveBorrowerSplit: (payerName: String, userShare: Double, eventDescription: String) -> Unit,
    existingPeople: List<String> = emptyList(),
    initialAmount: Double? = null,
    initialDescription: String? = null,
    hazeState: HazeState? = LocalHazeState.current
) {
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var currentStep by remember {
        mutableStateOf(if (initialAmount != null && initialAmount > 0.0) SplitExpenseStep.FILL_DETAILS else SplitExpenseStep.AMOUNT_PAD)
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val glassTokens = LocalKanriGlass.current
    val targetScrim = if (!isVisible) 0f else if (glassTokens != null) 0.28f else 0.55f
    val scrimAlpha by animateFloatAsState(
        targetValue = targetScrim,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "splitScrimAlpha"
    )

    fun performDismiss() {
        if (!isVisible) return
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    var totalAmount by remember { mutableDoubleStateOf(initialAmount ?: 0.0) }
    var selectedRole by remember { mutableStateOf(SplitRole.I_PAID) }
    var eventDescription by remember { mutableStateOf(initialDescription?.trim() ?: "") }
    var peopleCount by remember { mutableIntStateOf(3) }
    var payerName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dynamic friend names list (for peopleCount - 1 friends)
    val friendNames = remember { mutableStateListOf<String>() }

    LaunchedEffect(peopleCount) {
        val needed = (peopleCount - 1).coerceAtLeast(1)
        while (friendNames.size < needed) {
            val nextIdx = friendNames.size
            val defaultName = if (nextIdx < existingPeople.size && !friendNames.contains(existingPeople[nextIdx])) {
                existingPeople[nextIdx]
            } else {
                "Friend ${nextIdx + 1}"
            }
            friendNames.add(defaultName)
        }
        while (friendNames.size > needed) {
            friendNames.removeAt(friendNames.size - 1)
        }
    }

    val perPersonShare = if (peopleCount > 0) totalAmount / peopleCount else 0.0
    val othersTotal = perPersonShare * (peopleCount - 1).coerceAtLeast(1)

    // Validation
    val isAmountValid = totalAmount > 0.0
    val isRoleValid = if (selectedRole == SplitRole.I_PAID) {
        friendNames.isNotEmpty() && friendNames.all { it.isNotBlank() }
    } else {
        payerName.isNotBlank()
    }
    val isValid = isAmountValid && isRoleValid

    val tokens = LocalKanriGlass.current
    val sectionCardColor = if (tokens != null) tokens.tintFlat else MaterialTheme.colorScheme.surfaceContainerLow
    val sectionCardBorder = BorderStroke(
        width = 0.8.dp,
        color = if (tokens != null) tokens.borderStart else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )

    BackHandler(onBack = {
        when (currentStep) {
            SplitExpenseStep.AMOUNT_PAD -> performDismiss()
            SplitExpenseStep.FILL_DETAILS -> currentStep = SplitExpenseStep.AMOUNT_PAD
            SplitExpenseStep.SUMMARY_DRAWER -> currentStep = SplitExpenseStep.FILL_DETAILS
        }
    })

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Scrim backdrop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (currentStep == SplitExpenseStep.SUMMARY_DRAWER) {
                                currentStep = SplitExpenseStep.FILL_DETAILS
                            } else if (currentStep == SplitExpenseStep.AMOUNT_PAD) {
                                performDismiss()
                            }
                        }
                    )
            )

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
                    contentAlignment = Alignment.BottomCenter,
                    transitionSpec = {
                        if (targetState == SplitExpenseStep.SUMMARY_DRAWER) {
                            (slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f)
                            ) + fadeIn(tween(180))).togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { it / 4 },
                                    animationSpec = tween(180)
                                ) + fadeOut(tween(140))
                            )
                        } else if (initialState == SplitExpenseStep.SUMMARY_DRAWER) {
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
                    label = "splitExpenseStepTransition",
                    modifier = Modifier.fillMaxSize()
                ) { step ->
                    when (step) {
                        SplitExpenseStep.AMOUNT_PAD -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                KokonutAmountPad(
                                    label = "Split a Bill",
                                    recipientName = "Total Bill Amount",
                                    initialAmount = totalAmount,
                                    currency = CurrencyUtils.getCurrencySymbol(),
                                    showHandle = true,
                                    onClose = { performDismiss() },
                                    hazeState = hazeState,
                                    onContinue = { entered ->
                                        totalAmount = entered
                                        currentStep = SplitExpenseStep.FILL_DETAILS
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .imePadding()
                                )
                            }
                        }

                        SplitExpenseStep.FILL_DETAILS -> {
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
                                    level = GlassLevel.EntrySheet,
                                    hazeState = hazeState
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = 22.dp, vertical = 20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Drag Handle
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
                                        )

                                        // Header
                                        Text(
                                            text = "Split a Bill",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = GoogleSansFlex,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = "Divide an expense evenly among friends & groups.",
                                            fontFamily = GoogleSansFlex,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.5.sp,
                                            textAlign = TextAlign.Center
                                        )

                                        // Sleek Amount Summary Card with Edit Affordance
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = sectionCardColor,
                                            border = sectionCardBorder,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .clickable {
                                                    haptics.tick()
                                                    currentStep = SplitExpenseStep.AMOUNT_PAD
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
                                                        text = "TOTAL BILL AMOUNT",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        letterSpacing = 1.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    val formatted = CurrencyUtils.formatCurrency(totalAmount, includeDecimals = (totalAmount % 1.0 != 0.0))
                                                    Text(
                                                        text = formatted,
                                                        fontSize = 28.sp,
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
                                                            fontWeight = FontWeight.Medium,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Role Selector: I Paid vs Someone Else Paid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val isIPaid = selectedRole == SplitRole.I_PAID

                                            // Option 1: I Paid (User collects from friends)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        haptics.tick()
                                                        selectedRole = SplitRole.I_PAID
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isIPaid) SageGreen.copy(alpha = 0.18f) else sectionCardColor,
                                                border = if (isIPaid) BorderStroke(1.2.dp, SageGreen) else sectionCardBorder
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 11.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "I Paid (I Lent)",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = if (isIPaid) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (isIPaid) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Option 2: Someone Else Paid (User owes their share)
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        haptics.tick()
                                                        selectedRole = SplitRole.SOMEONE_ELSE
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (!isIPaid) AmberGold.copy(alpha = 0.18f) else sectionCardColor,
                                                border = if (!isIPaid) BorderStroke(1.2.dp, AmberGold) else sectionCardBorder
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 11.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "Friend Paid (I Owe)",
                                                        style = MaterialTheme.typography.labelLarge.copy(
                                                            fontWeight = if (!isIPaid) FontWeight.Bold else FontWeight.Medium
                                                        ),
                                                        color = if (!isIPaid) AmberGold else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Event Description Field
                                        OutlinedTextField(
                                            value = eventDescription,
                                            onValueChange = { eventDescription = it },
                                            label = { Text("What was this for? (e.g. Dinner, Groceries)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Next
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                cursorColor = MaterialTheme.colorScheme.onSurface,
                                                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp)
                                        )

                                        // Number of People Stepper & Share Calculation Card
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = sectionCardColor,
                                            border = sectionCardBorder,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "SPLIT BETWEEN",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            letterSpacing = 1.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = "$peopleCount People",
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                if (peopleCount > 2) {
                                                                    haptics.tick()
                                                                    peopleCount -= 1
                                                                }
                                                            },
                                                            enabled = peopleCount > 2,
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Remove,
                                                                contentDescription = "Decrease",
                                                                tint = if (peopleCount > 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                if (peopleCount < 20) {
                                                                    haptics.tick()
                                                                    peopleCount += 1
                                                                }
                                                            },
                                                            enabled = peopleCount < 20,
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Increase",
                                                                tint = if (peopleCount < 20) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Breakdown Summary Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "PER PERSON SHARE",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            letterSpacing = 0.8.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = CurrencyUtils.formatCurrency(perPersonShare),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Text(
                                                        text = if (selectedRole == SplitRole.I_PAID) {
                                                            "Friends Owe: ${CurrencyUtils.formatCurrency(othersTotal)}"
                                                        } else {
                                                            "You Owe: ${CurrencyUtils.formatCurrency(perPersonShare)}"
                                                        },
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (selectedRole == SplitRole.I_PAID) SageGreen else AmberGold
                                                    )
                                                }
                                            }
                                        }

                                        // Role Details: Friend slots vs Payer slot
                                        if (selectedRole == SplitRole.I_PAID) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Friends Splitting with You (${friendNames.size})",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )

                                                friendNames.forEachIndexed { index, name ->
                                                    OutlinedTextField(
                                                        value = name,
                                                        onValueChange = { friendNames[index] = it },
                                                        label = { Text("Friend ${index + 1}") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Default.Person,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        },
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                            cursorColor = MaterialTheme.colorScheme.onSurface,
                                                            focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        ),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                }

                                                // Quick suggest from existing contacts
                                                if (existingPeople.isNotEmpty()) {
                                                    Text(
                                                        text = "Quick Suggest:",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                    )
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        existingPeople.take(8).forEach { person ->
                                                            FilterChip(
                                                                selected = friendNames.contains(person),
                                                                onClick = {
                                                                    haptics.tick()
                                                                    val firstDefaultIdx = friendNames.indexOfFirst { it.startsWith("Friend") }
                                                                    if (firstDefaultIdx != -1) {
                                                                        friendNames[firstDefaultIdx] = person
                                                                    } else if (!friendNames.contains(person)) {
                                                                        friendNames.add(person)
                                                                        peopleCount = friendNames.size + 1
                                                                    }
                                                                },
                                                                label = { Text(person, fontSize = 12.sp) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // Friend Paid: Who paid?
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Who paid the bill?",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )

                                                OutlinedTextField(
                                                    value = payerName,
                                                    onValueChange = { payerName = it },
                                                    label = { Text("Payer Name") },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = null,
                                                            tint = AmberGold,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    },
                                                    singleLine = true,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        cursorColor = MaterialTheme.colorScheme.onSurface,
                                                        focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(12.dp)
                                                )

                                                if (existingPeople.isNotEmpty()) {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        existingPeople.take(6).forEach { person ->
                                                            FilterChip(
                                                                selected = payerName == person,
                                                                onClick = {
                                                                    haptics.tick()
                                                                    payerName = person
                                                                },
                                                                label = { Text(person, fontSize = 12.sp) }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Action Buttons: Back, Cancel & Proceed
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = {
                                                haptics.tick()
                                                currentStep = SplitExpenseStep.AMOUNT_PAD
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
                                                        if (isValid) {
                                                            haptics.primaryAction()
                                                            currentStep = SplitExpenseStep.SUMMARY_DRAWER
                                                        }
                                                    },
                                                    enabled = isValid,
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

                        SplitExpenseStep.SUMMARY_DRAWER -> {
                            val detailRows = listOfNotNull(
                                DrawerDetailRow(title = "Split For", value = eventDescription.ifBlank { "Group Expense" }),
                                DrawerDetailRow(title = "Paid By", value = if (selectedRole == SplitRole.I_PAID) "You" else payerName.trim()),
                                DrawerDetailRow(title = "Total Bill", value = CurrencyUtils.formatCurrency(totalAmount)),
                                DrawerDetailRow(title = "Total People", value = "$peopleCount people"),
                                DrawerDetailRow(title = "Each Share", value = CurrencyUtils.formatCurrency(perPersonShare)),
                                if (selectedRole == SplitRole.I_PAID) {
                                    DrawerDetailRow(title = "Friends Owe You", value = CurrencyUtils.formatCurrency(othersTotal))
                                } else {
                                    DrawerDetailRow(title = "You Owe", value = CurrencyUtils.formatCurrency(perPersonShare))
                                },
                                if (selectedRole == SplitRole.I_PAID && friendNames.isNotEmpty()) {
                                    DrawerDetailRow(title = "Friends", value = friendNames.joinToString(", "))
                                } else null
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                KokonutSmoothDrawer(
                                    title = "Review Split Bill",
                                    description = "${eventDescription.ifBlank { "Group Expense" }} • $peopleCount people",
                                    totalAmount = if (selectedRole == SplitRole.I_PAID) othersTotal else perPersonShare,
                                    slideLabel = if (selectedRole == SplitRole.I_PAID) {
                                        "Slide to split (Collect ${com.omkarnub.kanri.util.CurrencyUtils.formatCurrency(othersTotal, includeDecimals = false)})"
                                    } else {
                                        "Slide to confirm (Owe ${com.omkarnub.kanri.util.CurrencyUtils.formatCurrency(perPersonShare, includeDecimals = false)})"
                                    },
                                    detailRows = detailRows,
                                    accountType = if (selectedRole == SplitRole.I_PAID) "Lent Ledger" else "Borrowed Ledger",
                                    confirmationType = if (selectedRole == SplitRole.I_PAID) SlideConfirmationType.LEND else SlideConfirmationType.BORROW,
                                    hazeState = hazeState,
                                    onConfirm = {
                                        if (selectedRole == SplitRole.I_PAID) {
                                            onSaveLenderSplit(friendNames.toList(), perPersonShare, eventDescription.trim())
                                        } else {
                                            onSaveBorrowerSplit(payerName.trim(), perPersonShare, eventDescription.trim())
                                        }
                                    },
                                    onSuccessComplete = {
                                        performDismiss()
                                    },
                                    onDismiss = {
                                        currentStep = SplitExpenseStep.FILL_DETAILS
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
}
