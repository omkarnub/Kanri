package com.omkarnub.kanri.ui.lending

import android.app.TimePickerDialog
import android.net.Uri
import android.provider.ContactsContract
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
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
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

enum class LendingEntryStep {
    AMOUNT_PAD,
    FILL_DETAILS,
    SUMMARY_DRAWER
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LendingEntryDialog(
    initialType: String = "LENT",
    initialPersonName: String = "",
    initialAmount: Double = 0.0,
    initialDate: Long = System.currentTimeMillis(),
    initialDueDate: Long? = null,
    initialNotes: String? = null,
    existingEntry: LendingWithRepayments? = null,
    recentPeople: List<String> = emptyList(),
    allPeople: List<PersonSummary> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (personName: String, amount: Double, type: String, date: Long, dueDate: Long?, notes: String?, wallet: String) -> Unit,
    hazeState: HazeState? = LocalHazeState.current
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val isEditing = existingEntry != null
    val hasRepayments = existingEntry?.repayments?.isNotEmpty() == true
    var currentStep by remember {
        mutableStateOf(if (isEditing) LendingEntryStep.FILL_DETAILS else LendingEntryStep.AMOUNT_PAD)
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isVisible) 0.55f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "lendingScrimAlpha"
    )

    fun performDismiss() {
        if (!isVisible) return
        scope.launch {
            isVisible = false
            delay(220)
            onDismiss()
        }
    }

    var type by remember { mutableStateOf(existingEntry?.lending?.type ?: initialType) }
    var personName by remember { mutableStateOf(existingEntry?.lending?.personName ?: initialPersonName) }
    var amount by remember { mutableDoubleStateOf(existingEntry?.lending?.amount ?: initialAmount) }
    var notes by remember { mutableStateOf(existingEntry?.lending?.notes ?: (initialNotes ?: "")) }
    var entryDate by remember { mutableLongStateOf(existingEntry?.lending?.date ?: initialDate) }
    var dueDate by remember { mutableStateOf<Long?>(existingEntry?.lending?.dueDate ?: initialDueDate) }
    var selectedWallet by remember { mutableStateOf("NONE") }

    var showEntryDatePicker by remember { mutableStateOf(false) }
    var showCustomDueDatePicker by remember { mutableStateOf(false) }
    var showPeoplePopup by remember { mutableStateOf(false) }

    // Contact Picker Launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    if (nameIdx != -1) {
                        val name = it.getString(nameIdx)
                        if (!name.isNullOrBlank()) {
                            personName = name.trim().take(40)
                        }
                    }
                }
            }
        }
    }

    // Validation
    val isPersonValid = personName.trim().isNotEmpty() && personName.trim().length <= 40
    val isAmountValid = amount > 0.0
    val isDueDateValid = dueDate == null || dueDate!! >= entryDate
    val isValid = isPersonValid && isAmountValid && isDueDateValid

    BackHandler(onBack = {
        when (currentStep) {
            LendingEntryStep.AMOUNT_PAD -> performDismiss()
            LendingEntryStep.FILL_DETAILS -> if (!isEditing) currentStep = LendingEntryStep.AMOUNT_PAD else performDismiss()
            LendingEntryStep.SUMMARY_DRAWER -> currentStep = LendingEntryStep.FILL_DETAILS
        }
    })

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
                        if (currentStep == LendingEntryStep.SUMMARY_DRAWER) {
                            currentStep = LendingEntryStep.FILL_DETAILS
                        } else if (currentStep == LendingEntryStep.AMOUNT_PAD) {
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
                    if (targetState == LendingEntryStep.SUMMARY_DRAWER) {
                        (slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.82f)
                        ) + fadeIn(tween(180))).togetherWith(
                            slideOutVertically(
                                targetOffsetY = { it / 4 },
                                animationSpec = tween(180)
                            ) + fadeOut(tween(140))
                        )
                    } else if (initialState == LendingEntryStep.SUMMARY_DRAWER) {
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
                label = "lendingEntryStepTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    LendingEntryStep.AMOUNT_PAD -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            KokonutAmountPad(
                                label = if (type == "LENT") "Lending to" else "Borrowing from",
                                recipientName = if (personName.isNotBlank()) personName else (if (type == "LENT") "Lent" else "Borrowed"),
                                initialAmount = amount,
                                currency = CurrencyUtils.getCurrencySymbol(),
                                showHandle = true,
                                onClose = { performDismiss() },
                                hazeState = hazeState,
                                onContinue = { entered ->
                                    amount = entered
                                    currentStep = LendingEntryStep.FILL_DETAILS
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .imePadding()
                            )
                        }
                    }

                    LendingEntryStep.FILL_DETAILS -> {
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
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Top Drag Handle
                                    Box(
                                        modifier = Modifier
                                            .width(36.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.20f))
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Header Title
                                    Text(
                                        text = if (isEditing) "Edit Record" else (if (type == "LENT") "Track Loan Given" else "Track Loan Taken"),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = if (type == "LENT") "Log money you lent to friends or colleagues." else "Log money you borrowed from friends or colleagues.",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.5.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                                    )

                                    // Sleek Amount Summary Card with Edit Affordance
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable(enabled = !hasRepayments && !isEditing) {
                                                haptics.tick()
                                                currentStep = LendingEntryStep.AMOUNT_PAD
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
                                                    text = if (type == "LENT") "LENT AMOUNT" else "BORROWED AMOUNT",
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

                                            if (!hasRepayments && !isEditing) {
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
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Two-way toggle: I Lent / I Borrowed
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val isLent = type == "LENT"
                                        val isBorrowed = type == "BORROWED"

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = !hasRepayments) {
                                                    haptics.tick()
                                                    type = "LENT"
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isLent) SageGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            border = BorderStroke(
                                                width = if (isLent) 1.5.dp else 0.8.dp,
                                                color = if (isLent) SageGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "I Lent",
                                                    fontFamily = GoogleSansFlex,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (isLent) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isLent) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable(enabled = !hasRepayments) {
                                                    haptics.tick()
                                                    type = "BORROWED"
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isBorrowed) ExpenseRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                            border = BorderStroke(
                                                width = if (isBorrowed) 1.5.dp else 0.8.dp,
                                                color = if (isBorrowed) ExpenseRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "I Borrowed",
                                                    fontFamily = GoogleSansFlex,
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = if (isBorrowed) FontWeight.Bold else FontWeight.Medium
                                                    ),
                                                    color = if (isBorrowed) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (hasRepayments) {
                                        Text(
                                            text = "Amount and type locked because repayments exist.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Person Section
                                    Text(
                                        text = "Person",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = personName,
                                        onValueChange = { personName = it.take(40) },
                                        placeholder = {
                                            Text(
                                                if (type == "LENT") "Who did you lend to?" else "Who did you borrow from?",
                                                fontFamily = GoogleSansFlex,
                                                fontSize = 12.5.sp
                                            )
                                        },
                                        singleLine = true,
                                        trailingIcon = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                if (allPeople.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = {
                                                            haptics.tick()
                                                            showPeoplePopup = true
                                                        },
                                                        modifier = Modifier.size(34.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.People,
                                                            contentDescription = "Select Existing Person",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        haptics.tick()
                                                        contactPickerLauncher.launch(null)
                                                    },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Contacts,
                                                        contentDescription = "Pick Contact",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    if (allPeople.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            allPeople.take(6).forEach { p ->
                                                val isSelected = personName.trim().equals(p.displayName.trim(), ignoreCase = true)
                                                val initial = p.displayName.trim().take(1).uppercase(Locale.ROOT).ifBlank { "?" }
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                    border = BorderStroke(
                                                        width = if (isSelected) 1.5.dp else 0.8.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                                                    ),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            haptics.tick()
                                                            personName = p.displayName
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = initial,
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 10.sp
                                                                ),
                                                                color = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                        Text(
                                                            text = p.displayName,
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontFamily = GoogleSansFlex,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 11.sp
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Date & Time
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
                                                    showEntryDatePicker = true
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
                                                    text = LendingDateFormatters.formatMedium(entryDate),
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
                                                    val cal = Calendar.getInstance().apply { timeInMillis = entryDate }
                                                    val is24Hr = DateFormat.is24HourFormat(context)
                                                    TimePickerDialog(
                                                        context,
                                                        { _, h, m ->
                                                            entryDate = updateTimePreserveDate(h, m, entryDate)
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
                                                    text = LendingDateFormatters.formatTime(entryDate),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    fontFamily = GoogleSansFlex
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Expected Return / Due Date
                                    Text(
                                        text = "Expected Return Date",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val presets = listOf(
                                            Pair("No Due Date", null),
                                            Pair("In 7 Days", 7),
                                            Pair("In 15 Days", 15),
                                            Pair("In 30 Days", 30)
                                        )
                                        presets.forEach { (label, days) ->
                                            val targetDueDate = days?.let { d ->
                                                Calendar.getInstance().apply {
                                                    timeInMillis = entryDate
                                                    add(Calendar.DAY_OF_YEAR, d)
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 59)
                                                }.timeInMillis
                                            }
                                            val isSelected = (days == null && dueDate == null) ||
                                                    (targetDueDate != null && dueDate != null && abs(dueDate!! - targetDueDate) < 86_400_000L)

                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    haptics.tick()
                                                    dueDate = targetDueDate
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

                                        val isCustomSelected = dueDate != null && presets.none { (_, days) ->
                                            days != null && abs(dueDate!! - (entryDate + days * 86_400_000L)) < 86_400_000L
                                        }
                                        FilterChip(
                                            selected = isCustomSelected,
                                            onClick = {
                                                haptics.tick()
                                                showCustomDueDatePicker = true
                                            },
                                            label = {
                                                Text(
                                                    text = if (isCustomSelected && dueDate != null) LendingDateFormatters.formatShort(dueDate!!) else "Custom",
                                                    fontSize = 11.sp,
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
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
                                                selected = isCustomSelected,
                                                selectedBorderColor = MaterialTheme.colorScheme.onSurface,
                                                selectedBorderWidth = 1.5.dp,
                                                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                borderWidth = 0.8.dp
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    // Wallet Impact
                                    if (!isEditing) {
                                        Spacer(modifier = Modifier.height(14.dp))
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
                                            listOf(
                                                "CASH" to "Cash",
                                                "ONLINE" to "Online",
                                                "NONE" to "Not from balances"
                                            ).forEach { (key, label) ->
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
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Note
                                    OutlinedTextField(
                                        value = notes,
                                        onValueChange = { notes = it },
                                        label = { Text("Note (Optional)", fontFamily = GoogleSansFlex) },
                                        placeholder = { Text("What was this for?", fontFamily = GoogleSansFlex, fontSize = 12.5.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Action Buttons: Back, Cancel & Proceed (EXACTLY matching AddTransactionDialog!)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = {
                                            haptics.tick()
                                            if (!isEditing) {
                                                currentStep = LendingEntryStep.AMOUNT_PAD
                                            } else {
                                                performDismiss()
                                            }
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
                                                        currentStep = LendingEntryStep.SUMMARY_DRAWER
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

                    LendingEntryStep.SUMMARY_DRAWER -> {
                        val detailRows = listOfNotNull(
                            DrawerDetailRow(title = "Type", value = if (type == "LENT") "Lent" else "Borrowed"),
                            DrawerDetailRow(title = if (type == "LENT") "To" else "From", value = personName.trim()),
                            if (dueDate != null) DrawerDetailRow(title = "Expected Return", value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDate!!))) else null,
                            if (notes.isNotBlank()) DrawerDetailRow(title = "Note", value = notes.trim()) else null,
                            DrawerDetailRow(title = "Account", value = if (selectedWallet == "ONLINE") "Online" else if (selectedWallet == "CASH") "Cash" else "Unlinked")
                        )

                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            KokonutSmoothDrawer(
                                title = if (type == "LENT") "Review loan" else "Review borrowing",
                                description = "${personName.trim()} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entryDate))}",
                                totalAmount = amount,
                                slideLabel = if (type == "LENT") "Slide to lend ₹${amount.toInt()}" else "Slide to record borrowing",
                                detailRows = detailRows,
                                accountType = if (selectedWallet == "ONLINE") "Online" else if (selectedWallet == "CASH") "Cash" else "Unlinked",
                                confirmationType = if (type == "LENT") SlideConfirmationType.LEND else SlideConfirmationType.BORROW,
                                hazeState = hazeState,
                                onConfirm = {
                                    onSave(personName.trim(), amount, type, entryDate, dueDate, notes.ifBlank { null }, selectedWallet)
                                },
                                onSuccessComplete = {
                                    performDismiss()
                                },
                                onDismiss = {
                                    currentStep = LendingEntryStep.FILL_DETAILS
                                }
                            )
                        }
                    }
                }
            }
        }

        // Existing Profiles Popup
        if (showPeoplePopup && allPeople.isNotEmpty()) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredPeople = remember(searchQuery, allPeople) {
                if (searchQuery.isBlank()) allPeople else {
                    allPeople.filter { it.displayName.contains(searchQuery.trim(), ignoreCase = true) }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.70f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showPeoplePopup = false }
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    KanriGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        level = GlassLevel.EntrySheet,
                        hazeState = hazeState
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Select Profile",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${allPeople.size} created profiles",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                IconButton(
                                    onClick = { showPeoplePopup = false },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (allPeople.size > 3) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search by name...", fontFamily = GoogleSansFlex) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                )
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (filteredPeople.isEmpty()) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No matching profile found",
                                                fontFamily = GoogleSansFlex,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredPeople, key = { it.key }) { p ->
                                        val isSelected = personName.trim().equals(p.displayName.trim(), ignoreCase = true)
                                        val initial = p.displayName.trim().take(1).uppercase(Locale.ROOT).ifBlank { "?" }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                            border = BorderStroke(
                                                if (isSelected) 1.5.dp else 0.8.dp,
                                                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
                                                    haptics.tick()
                                                    personName = p.displayName
                                                    showPeoplePopup = false
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                                            .background(if (isSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceVariant)
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = initial,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp
                                                            ),
                                                            color = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    Text(
                                                        text = p.displayName,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            fontFamily = GoogleSansFlex,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                            fontSize = 15.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }

                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Material Date Picker Dialog for Entry Date
        if (showEntryDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = entryDate
            )
            KanriGlassDatePickerDialog(
                onDismissRequest = { showEntryDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selected = datePickerState.selectedDateMillis
                            if (selected != null) {
                                entryDate = combineDateAndPreserveTime(selected, entryDate)
                            }
                            showEntryDatePicker = false
                        }
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEntryDatePicker = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Material Date Picker Dialog for Custom Due Date
        if (showCustomDueDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = dueDate ?: (entryDate + 7L * 86_400_000L)
            )
            KanriGlassDatePickerDialog(
                onDismissRequest = { showCustomDueDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selected = datePickerState.selectedDateMillis
                            if (selected != null) {
                                dueDate = combineDueDateEndOfDay(selected)
                            }
                            showCustomDueDatePicker = false
                        }
                    ) {
                        Text("OK", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomDueDatePicker = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// -------------------------------------------------------------
// Date & Time Combination Helpers
// -------------------------------------------------------------
private fun combineDateAndPreserveTime(selectedDateUtcMillis: Long, originalTimeMillis: Long): Long {
    val calOriginal = Calendar.getInstance().apply { timeInMillis = originalTimeMillis }
    val hour = calOriginal.get(Calendar.HOUR_OF_DAY)
    val minute = calOriginal.get(Calendar.MINUTE)
    val second = calOriginal.get(Calendar.SECOND)

    val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDateUtcMillis
    }
    val year = calUtc.get(Calendar.YEAR)
    val month = calUtc.get(Calendar.MONTH)
    val day = calUtc.get(Calendar.DAY_OF_MONTH)

    val calTarget = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, second)
        set(Calendar.MILLISECOND, 0)
    }
    return calTarget.timeInMillis
}

private fun updateTimePreserveDate(hour: Int, minute: Int, originalTimeMillis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = originalTimeMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun combineDueDateEndOfDay(selectedDateUtcMillis: Long): Long {
    val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = selectedDateUtcMillis
    }
    val year = calUtc.get(Calendar.YEAR)
    val month = calUtc.get(Calendar.MONTH)
    val day = calUtc.get(Calendar.DAY_OF_MONTH)

    val calTarget = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return calTarget.timeInMillis
}
