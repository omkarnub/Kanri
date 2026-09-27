package com.omkarnub.kanri.ui.lending

import android.app.TimePickerDialog
import android.net.Uri
import android.provider.ContactsContract
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeChild
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalHazeApi::class)
@Composable
fun LendingEntryDialog(
    initialType: String = "LENT",
    initialPersonName: String = "",
    initialAmount: Double = 100.0,
    initialDate: Long = System.currentTimeMillis(),
    initialDueDate: Long? = null,
    initialNotes: String? = null,
    existingEntry: LendingWithRepayments? = null,
    recentPeople: List<String> = emptyList(),
    allPeople: List<PersonSummary> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (personName: String, amount: Double, type: String, date: Long, dueDate: Long?, notes: String?) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val glassTheme = rememberKanriGlassTheme()


    val isEditing = existingEntry != null
    val hasRepayments = existingEntry?.repayments?.isNotEmpty() == true

    var type by remember { mutableStateOf(existingEntry?.lending?.type ?: initialType) }
    var personName by remember { mutableStateOf(existingEntry?.lending?.personName ?: initialPersonName) }
    var amount by remember { mutableDoubleStateOf(existingEntry?.lending?.amount ?: initialAmount) }
    var isManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf(if (amount % 1.0 == 0.0) amount.toLong().toString() else amount.toString()) }
    var notes by remember { mutableStateOf(existingEntry?.lending?.notes ?: (initialNotes ?: "")) }
    var entryDate by remember { mutableLongStateOf(existingEntry?.lending?.date ?: initialDate) }
    var dueDate by remember { mutableStateOf<Long?>(existingEntry?.lending?.dueDate ?: initialDueDate) }

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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Backdrop Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Frosted Dialog Card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = glassTheme.shadowElevation + 6.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = glassTheme.shadowColor,
                        ambientColor = glassTheme.shadowColor.copy(alpha = glassTheme.shadowColor.alpha * 0.7f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .then(
                        if (hazeState != null) {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = glassTheme.popupHazeStyle
                            ) {
                                canDrawArea = { true }
                            }
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
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isEditing) "Edit Record" else "Add Record",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

                        // I Lent
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !hasRepayments) { type = "LENT" },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLent) SageGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isLent) SageGreen else MaterialTheme.colorScheme.outlineVariant
                            )
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
                            color = if (isBorrowed) ExpenseRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            border = BorderStroke(
                                1.dp,
                                if (isBorrowed) ExpenseRed else MaterialTheme.colorScheme.outlineVariant
                            )
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

                    // Amount Stepper with [-] and [+]
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Decrease [-]
                            IconButton(
                                onClick = {
                                    val step = if (amount <= 50) 10.0 else 50.0
                                    val next = (amount - step).coerceAtLeast(0.0)
                                    amount = next
                                    manualText = next.toLong().toString()
                                    isManualInput = false
                                },
                                enabled = !hasRepayments,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Central Amount Number / Tap to type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isManualInput && !hasRepayments) {
                                    OutlinedTextField(
                                        value = manualText,
                                        onValueChange = {
                                            val filtered = it.filter { ch -> ch.isDigit() }
                                            manualText = filtered
                                            val parsed = filtered.toDoubleOrNull()
                                            if (parsed != null) {
                                                amount = parsed
                                            }
                                        },
                                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                val parsed = manualText.toDoubleOrNull()
                                                if (parsed != null) amount = parsed
                                                isManualInput = false
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable(enabled = !hasRepayments) {
                                                manualText = amount.toLong().toString()
                                                isManualInput = true
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        val formatted = CurrencyUtils.formatCurrency(amount, includeDecimals = false)
                                        AnimatedNumberText(
                                            text = formatted,
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 26.sp,
                                            animateFromZero = false
                                        )
                                        if (!hasRepayments) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
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
                            }

                            // Increase [+]
                            IconButton(
                                onClick = {
                                    val step = if (amount < 50) 10.0 else 50.0
                                    val next = amount + step
                                    amount = next
                                    manualText = next.toLong().toString()
                                    isManualInput = false
                                },
                                enabled = !hasRepayments,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Increment Chips: +10 +50 +100 +500
                    if (!hasRepayments) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10.0, 50.0, 100.0, 500.0).forEach { inc ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            amount += inc
                                            manualText = amount.toLong().toString()
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+${inc.toInt()}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            ),
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
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
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(
                                            if (isSelected) 1.5.dp else 0.8.dp,
                                            if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                        ),
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
                                                    .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = initial,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    ),
                                                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
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
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = if (!isToday) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null,
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
                                    color = if (isToday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isYesterday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = if (!isYesterday) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null,
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
                                    color = if (isYesterday) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                    border = if (!isSelected) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null,
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
                                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = if (isCustomSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = if (!isCustomSelected) BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant) else null,
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
                                        tint = if (isCustomSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
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
                                        color = if (isCustomSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    Button(
                        onClick = {
                            if (isValid) {
                                onSave(personName.trim(), amount, type, entryDate, dueDate, notes.ifBlank { null })
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add Entry",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // Existing Profiles Popup (Avatar + Name ONLY, no owe/lent, no settled)
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
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = glassTheme.shadowElevation + 6.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = glassTheme.shadowColor,
                                ambientColor = glassTheme.shadowColor.copy(alpha = glassTheme.shadowColor.alpha * 0.7f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .then(
                                if (hazeState != null) {
                                    Modifier.hazeChild(
                                        state = hazeState,
                                        style = glassTheme.popupHazeStyle
                                    ) {
                                        canDrawArea = { true }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .border(glassTheme.glassBorder, shape = RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = if (hazeState != null) Color.Transparent else glassTheme.fallbackBackgroundColor
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
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            border = BorderStroke(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                                            ),
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
                                                            .background(if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant)
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = initial,
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 15.sp
                                                            ),
                                                            color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
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

    // Material Date Picker Dialog for Entry Date
    if (showEntryDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = entryDate
        )
        DatePickerDialog(
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
                    Text("OK", color = MaterialTheme.colorScheme.onSurface)
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
        DatePickerDialog(
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
                    Text("OK", color = MaterialTheme.colorScheme.onSurface)
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
