package com.omkarnub.kanri.ui.lending

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import android.app.TimePickerDialog
import android.net.Uri
import android.provider.ContactsContract
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import kotlin.math.abs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.omkarnub.kanri.ui.common.GlassBackdrop
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDatePickerDialog
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.applyWindowBlur
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeState
import com.omkarnub.kanri.ui.common.kokonut.KokonutAmountPad
import com.omkarnub.kanri.ui.common.kokonut.KokonutSmoothDrawer
import com.omkarnub.kanri.ui.common.kokonut.KokonutSlideToConfirm
import com.omkarnub.kanri.ui.common.kokonut.SlideConfirmationType
import com.omkarnub.kanri.ui.common.kokonut.DrawerDetailRow
import java.text.SimpleDateFormat
import java.util.Date
import dev.chrisbanes.haze.hazeChild
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

enum class LendingEntryStep {
    AMOUNT_PAD,
    FILL_DETAILS,
    SUMMARY_DRAWER
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
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
    val glassTheme = rememberKanriGlassTheme()


    val isEditing = existingEntry != null
    val hasRepayments = existingEntry?.repayments?.isNotEmpty() == true
    var currentStep by remember {
        mutableStateOf(if (isEditing) LendingEntryStep.FILL_DETAILS else LendingEntryStep.AMOUNT_PAD)
    }

    val scope = rememberCoroutineScope()
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
    var isManualInput by remember { mutableStateOf(false) }
    val initialAmountStr = remember(amount) { if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString() }
    var manualText by remember { mutableStateOf(initialAmountStr) }
    var manualTextFieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialAmountStr, selection = TextRange(initialAmountStr.length)))
    }
    var notes by remember { mutableStateOf(existingEntry?.lending?.notes ?: (initialNotes ?: "")) }
    var entryDate by remember { mutableLongStateOf(existingEntry?.lending?.date ?: initialDate) }
    var dueDate by remember { mutableStateOf<Long?>(existingEntry?.lending?.dueDate ?: initialDueDate) }
    var selectedWallet by remember { mutableStateOf("NONE") }

    var showEntryDatePicker by remember { mutableStateOf(false) }
    var showCustomDueDatePicker by remember { mutableStateOf(false) }
    var showPeoplePopup by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

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

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
        }
    }

    // Validation
    val isPersonValid = personName.trim().isNotEmpty() && personName.trim().length <= 40
    val isAmountValid = amount > 0.0
    val isDueDateValid = dueDate == null || dueDate!! >= entryDate
    val isValid = isPersonValid && isAmountValid && isDueDateValid

    val tokens = LocalKanriGlass.current

    val pageBgColor = if (tokens != null) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.background
    }

    val sectionCardColor = if (tokens != null) {
        tokens.tintFlat
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    val sectionCardBorder = BorderStroke(
        width = 0.8.dp,
        color = if (tokens != null) {
            tokens.borderStart
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        }
    )

    BackHandler(onBack = {
        when (currentStep) {
            LendingEntryStep.AMOUNT_PAD -> performDismiss()
            LendingEntryStep.FILL_DETAILS -> if (!isEditing) currentStep = LendingEntryStep.AMOUNT_PAD else performDismiss()
            LendingEntryStep.SUMMARY_DRAWER -> currentStep = LendingEntryStep.FILL_DETAILS
        }
    })

    Dialog(
        onDismissRequest = { performDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let { w ->
                w.setDimAmount(0f)
                w.setBackgroundDrawableResource(android.R.color.transparent)
                applyWindowBlur(w)
            }
            onDispose {}
        }

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
                            if (currentStep == LendingEntryStep.SUMMARY_DRAWER) {
                                currentStep = LendingEntryStep.FILL_DETAILS
                            } else if (currentStep == LendingEntryStep.AMOUNT_PAD) {
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
                                    manualText = if (entered % 1.0 == 0.0) entered.toLong().toString() else entered.toString()
                                    manualTextFieldValue = TextFieldValue(text = manualText, selection = TextRange(manualText.length))
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
                                .fillMaxSize()
                                .background(pageBgColor)
                        ) {
            if (tokens != null) {
                GlassBackdrop(
                    modifier = Modifier.fillMaxSize(),
                    tokens = tokens
                )
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (isEditing) "Edit Record" else "Add Record",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (!isEditing) {
                                    currentStep = LendingEntryStep.AMOUNT_PAD
                                } else {
                                    onDismiss()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding(),
                        color = if (tokens != null) {
                            tokens.fallbackSurface.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.background
                        },
                        tonalElevation = 0.dp,
                        border = BorderStroke(
                            0.8.dp,
                            if (tokens != null) tokens.borderStart else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (isValid) {
                                        currentStep = LendingEntryStep.SUMMARY_DRAWER
                                    }
                                },
                                enabled = isValid,
                                shape = RoundedCornerShape(16.dp),
                                colors = kanriPrimaryButtonColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (isEditing) "Proceed to Update" else "Proceed",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // Two-way toggle: I Lent / I Borrowed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isLent = type == "LENT"
                        val isBorrowed = type == "BORROWED"

                        // I Lent
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !hasRepayments) { type = "LENT" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLent) SageGreen.copy(alpha = 0.18f) else sectionCardColor,
                            border = if (isLent) BorderStroke(1.2.dp, SageGreen) else sectionCardBorder
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "I Lent",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (isLent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isLent) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // I Borrowed
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !hasRepayments) { type = "BORROWED" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBorrowed) ExpenseRed.copy(alpha = 0.18f) else sectionCardColor,
                            border = if (isBorrowed) BorderStroke(1.2.dp, ExpenseRed) else sectionCardBorder
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 11.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "I Borrowed",
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

                    // Sleek Amount Summary Card with Edit Affordance (replaces duplicate steppers)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = sectionCardColor,
                        border = sectionCardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !hasRepayments && !isEditing) {
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
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val formatted = CurrencyUtils.formatCurrency(amount, includeDecimals = (amount % 1.0 != 0.0))
                                Text(
                                    text = formatted,
                                    fontSize = 28.sp,
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
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Person Section: Action Buttons (Profiles Popup & Contacts) + Name Input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PERSON",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 10.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (allPeople.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = sectionCardColor,
                                        border = sectionCardBorder,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { showPeoplePopup = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.People,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Profiles (${allPeople.size})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = sectionCardColor,
                                    border = sectionCardBorder,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { contactPickerLauncher.launch(null) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Contacts,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Contacts",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Person Name Input + Trailing Quick Pickers
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it.take(40) },
                            label = { Text("Person Name") },
                            placeholder = { Text("Type name or select profile") },
                            singleLine = true,
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (allPeople.isNotEmpty()) {
                                        IconButton(
                                            onClick = { showPeoplePopup = true },
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
                                        onClick = { contactPickerLauncher.launch(null) },
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
                                unfocusedBorderColor = if (tokens != null) tokens.borderStart else MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Quick-select People Chips row (Single-tap selection from existing profiles)
                        if (allPeople.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                allPeople.take(6).forEach { p ->
                                    val isSelected = personName.trim().equals(p.displayName.trim(), ignoreCase = true)
                                    val initial = p.displayName.trim().take(1).uppercase(Locale.ROOT).ifBlank { "?" }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) else sectionCardColor,
                                        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface) else sectionCardBorder,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                personName = p.displayName
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initial,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = p.displayName,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (allPeople.size > 6) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = sectionCardColor,
                                        border = sectionCardBorder,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { showPeoplePopup = true }
                                    ) {
                                        Text(
                                            text = "+${allPeople.size - 6} more",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Counterparty suggestion chip if available and not yet matching
                        if (personName.isBlank() && !initialNotes.isNullOrBlank() && allPeople.none { it.displayName.equals(initialNotes.trim(), ignoreCase = true) }) {
                            val cleanNote = initialNotes.trim().take(30)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = sectionCardColor,
                                border = sectionCardBorder,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { personName = cleanNote }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Use counterparty: \"$cleanNote\"",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recorded Date & Time Section: Pick Date + Pick Time + Today/Yesterday chips
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DATE & TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = sectionCardColor,
                                border = sectionCardBorder,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showEntryDatePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Pick Date",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = LendingDateFormatters.formatMedium(entryDate),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Time Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = sectionCardColor,
                                border = sectionCardBorder,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Pick Time",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = LendingDateFormatters.formatTime(entryDate),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Quick date chips: Today, Yesterday
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val now = Calendar.getInstance()
                            val isToday = {
                                val c = Calendar.getInstance().apply { timeInMillis = entryDate }
                                now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)
                            }()
                            val isYesterday = {
                                val c = Calendar.getInstance().apply { timeInMillis = entryDate }
                                now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && (now.get(Calendar.DAY_OF_YEAR) - c.get(Calendar.DAY_OF_YEAR) == 1)
                            }()

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isToday) MaterialTheme.colorScheme.inverseSurface else sectionCardColor,
                                border = if (!isToday) sectionCardBorder else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val calNow = Calendar.getInstance()
                                        val calEntry = Calendar.getInstance().apply { timeInMillis = entryDate }
                                        calNow.set(Calendar.HOUR_OF_DAY, calEntry.get(Calendar.HOUR_OF_DAY))
                                        calNow.set(Calendar.MINUTE, calEntry.get(Calendar.MINUTE))
                                        calNow.set(Calendar.SECOND, calEntry.get(Calendar.SECOND))
                                        entryDate = calNow.timeInMillis
                                    }
                            ) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isToday) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isYesterday) MaterialTheme.colorScheme.inverseSurface else sectionCardColor,
                                border = if (!isYesterday) sectionCardBorder else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val calYest = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                        val calEntry = Calendar.getInstance().apply { timeInMillis = entryDate }
                                        calYest.set(Calendar.HOUR_OF_DAY, calEntry.get(Calendar.HOUR_OF_DAY))
                                        calYest.set(Calendar.MINUTE, calEntry.get(Calendar.MINUTE))
                                        calYest.set(Calendar.SECOND, calEntry.get(Calendar.SECOND))
                                        entryDate = calYest.timeInMillis
                                    }
                            ) {
                                Text(
                                    text = "Yesterday",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isYesterday) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Return Date Presets: No Due Date, 7d, 15d, 30d, Custom
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DUE DATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 10.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val presets = listOf(
                            Pair("No Due Date", null),
                            Pair("In 7 Days", 7),
                            Pair("In 15 Days", 15),
                            Pair("In 30 Days", 30)
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
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

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.inverseSurface else sectionCardColor,
                                    border = if (!isSelected) sectionCardBorder else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            dueDate = targetDueDate
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Custom Date Picker chip
                            val isCustomSelected = dueDate != null && presets.none { (_, days) ->
                                days != null && abs(dueDate!! - (entryDate + days * 86_400_000L)) < 86_400_000L
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCustomSelected) MaterialTheme.colorScheme.inverseSurface else sectionCardColor,
                                border = if (!isCustomSelected) sectionCardBorder else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showCustomDueDatePicker = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Custom Date",
                                        tint = if (isCustomSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (isCustomSelected && dueDate != null) {
                                            LendingDateFormatters.formatShort(dueDate!!)
                                        } else {
                                            "Custom"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.5.sp
                                        ),
                                        color = if (isCustomSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (!isEditing) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (type == "LENT") "WALLET (MONEY GIVEN FROM)" else "WALLET (MONEY RECEIVED INTO)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 10.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "NONE" to "Don't affect balances",
                                    "CASH" to "Cash",
                                    "ONLINE" to "Online"
                                ).forEach { (key, label) ->
                                    val isSelected = selectedWallet == key
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.inverseSurface else sectionCardColor,
                                        border = if (!isSelected) sectionCardBorder else null,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedWallet = key }
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 11.5.sp
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Note Input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note (Optional)") },
                        placeholder = { Text("What was this for?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = if (tokens != null) tokens.borderStart else MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

        // -------------------------------------------------------------
        // Existing Profiles Popup (Plain Themed Card)
        // -------------------------------------------------------------
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
                // Secondary Scrim
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
                        level = GlassLevel.Strong,
                        hazeState = hazeState
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Popup Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Select Profile",
                                        style = MaterialTheme.typography.titleMedium.copy(
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

                            // Search bar if > 3 people
                            if (allPeople.size > 3) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search by name...") },
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

                            // Profiles List: Avatar initial + Display Name ONLY (no amount, no status)
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
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else sectionCardColor,
                                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface) else sectionCardBorder,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable {
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

                    KokonutSmoothDrawer(
                        title = if (type == "LENT") "Review loan" else "Review borrowing",
                        description = "${personName.trim()} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entryDate))}",
                        totalAmount = amount,
                        slideLabel = if (type == "LENT") "Slide to lend ₹${amount.toInt()}" else "Slide to record borrowing",
                        detailRows = detailRows,
                        accountType = if (selectedWallet == "ONLINE") "Online" else "Cash",
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
