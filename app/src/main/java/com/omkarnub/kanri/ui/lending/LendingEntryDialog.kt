package com.omkarnub.kanri.ui.lending

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.LendingWithRepayments
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import dev.chrisbanes.haze.hazeChild
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LendingEntryDialog(
    initialType: String = "LENT",
    initialPersonName: String = "",
    existingEntry: LendingWithRepayments? = null,
    recentPeople: List<String> = emptyList(),
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
    var amount by remember { mutableDoubleStateOf(existingEntry?.lending?.amount ?: 100.0) }
    var isManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf(amount.toLong().toString()) }
    var notes by remember { mutableStateOf(existingEntry?.lending?.notes ?: "") }
    var entryDate by remember { mutableLongStateOf(existingEntry?.lending?.date ?: System.currentTimeMillis()) }
    var dueDate by remember { mutableStateOf<Long?>(existingEntry?.lending?.dueDate) }

    var showCustomDatePicker by remember { mutableStateOf(false) }
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
                .background(Color.Black.copy(alpha = 0.55f))
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

                    // Person Name Input + Contact Picker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it.take(40) },
                            label = { Text("Person Name") },
                            placeholder = { Text("e.g. John Doe") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { contactPickerLauncher.launch(null) }) {
                                    Icon(
                                        imageVector = Icons.Default.Contacts,
                                        contentDescription = "Pick Contact",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        // Recent People Chips
                        if (recentPeople.isNotEmpty() && !isEditing) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                recentPeople.take(4).forEach { recent ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { personName = recent }
                                    ) {
                                        Text(
                                            text = recent,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
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
                                    .clickable { showCustomDatePicker = true }
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
    }

    // Material Date Picker Dialog for Custom Date
    if (showCustomDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: (entryDate + 7L * 86_400_000L)
        )
        DatePickerDialog(
            onDismissRequest = { showCustomDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            dueDate = selected
                        }
                        showCustomDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
