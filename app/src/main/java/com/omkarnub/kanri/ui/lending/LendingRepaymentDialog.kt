package com.omkarnub.kanri.ui.lending

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.omkarnub.kanri.ui.common.AnimatedNumberText
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassButton
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDialog
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.util.CurrencyUtils
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeChild
import kotlin.math.min

private val SageGreen = Color(0xFF30A46C)
private val ExpenseRed = Color(0xFFE54D2E)

@OptIn(ExperimentalHazeApi::class)
@Composable
fun LendingRepaymentDialog(
    personName: String,
    toReceive: Double,
    toPay: Double,
    initialDirection: String = if (toReceive > 0) "LENT" else "BORROWED",
    maxAllowed: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (direction: String, amount: Double, date: Long, note: String?) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val hazeState = LocalHazeState.current
    val glassTheme = rememberKanriGlassTheme()

    val hasBothDirections = toReceive > 0 && toPay > 0
    var selectedDirection by remember { mutableStateOf(initialDirection) }

    val currentOutstanding = maxAllowed ?: if (selectedDirection == "LENT") toReceive else toPay
    var amount by remember { mutableDoubleStateOf(currentOutstanding.coerceAtLeast(1.0)) }
    var isManualInput by remember { mutableStateOf(false) }
    var manualText by remember { mutableStateOf(amount.toLong().toString()) }
    var manualTextFieldValue by remember {
        val str = amount.toLong().toString()
        mutableStateOf(TextFieldValue(text = str, selection = TextRange(str.length)))
    }
    var note by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isManualInput) {
        if (isManualInput) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(selectedDirection) {
        val newOutstanding = maxAllowed ?: if (selectedDirection == "LENT") toReceive else toPay
        amount = newOutstanding.coerceAtLeast(1.0)
        val str = amount.toLong().toString()
        manualText = str
        manualTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
    }

    val isValid = amount > 0.0 && amount <= (currentOutstanding + 0.0001)

    KanriGlassDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        KanriGlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            level = GlassLevel.Strong,
            hazeState = hazeState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Record Repayment",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "For $personName",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        KanriGlassCard(
                            modifier = Modifier.size(32.dp),
                            shape = CircleShape,
                            level = GlassLevel.Flat,
                            onClick = onDismiss
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Direction Selector (if both directions exist)
                    if (hasBothDirections && maxAllowed == null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isTheyPaid = selectedDirection == "LENT"
                            val isIPaid = selectedDirection == "BORROWED"

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDirection = "LENT" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isTheyPaid) SageGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTheyPaid) SageGreen else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "They paid me",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isTheyPaid) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isTheyPaid) SageGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDirection = "BORROWED" },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isIPaid) ExpenseRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isIPaid) ExpenseRed else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "I paid them",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isIPaid) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (isIPaid) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Amount Stepper with [-] and [+]
                    KanriGlassCard(
                        shape = RoundedCornerShape(20.dp),
                        level = GlassLevel.Flat,
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
                                    val str = next.toLong().toString()
                                    manualText = str
                                    manualTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
                                    isManualInput = false
                                },
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

                            // Central Amount Number
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isManualInput) {
                                    OutlinedTextField(
                                        value = manualTextFieldValue,
                                        onValueChange = { newValue ->
                                            val filtered = newValue.text.filter { ch -> ch.isDigit() }
                                            manualTextFieldValue = newValue.copy(text = filtered)
                                            manualText = filtered
                                            val parsed = filtered.toDoubleOrNull()
                                            if (parsed != null) {
                                                amount = parsed
                                            }
                                        },
                                        prefix = { Text("${CurrencyUtils.getCurrencySymbol()} ", fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                val parsed = manualTextFieldValue.text.toDoubleOrNull()
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
                                            .clickable {
                                                val str = amount.toLong().toString()
                                                manualText = str
                                                manualTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
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

                            // Increase [+]
                            IconButton(
                                onClick = {
                                    val step = if (amount < 50) 10.0 else 50.0
                                    val next = min(currentOutstanding, amount + step)
                                    amount = next
                                    val str = next.toLong().toString()
                                    manualText = str
                                    manualTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
                                    isManualInput = false
                                },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Full Amount Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    amount = currentOutstanding
                                    val str = currentOutstanding.toLong().toString()
                                    manualText = str
                                    manualTextFieldValue = TextFieldValue(text = str, selection = TextRange(str.length))
                                }
                        ) {
                            Text(
                                text = "Full amount (${CurrencyUtils.formatCurrency(currentOutstanding)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Note Input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (Optional)") },
                        placeholder = { Text("e.g. UPI payment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    KanriGlassButton(
                        text = "Record Repayment",
                        onClick = {
                            if (isValid) {
                                onConfirm(selectedDirection, amount, System.currentTimeMillis(), note.ifBlank { null })
                            }
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(16.dp),
                        level = GlassLevel.Strong,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )

                    // UPI Pay Button (when paying what you borrowed)
                    if (selectedDirection.equals("BORROWED", ignoreCase = true)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        KanriGlassButton(
                            text = "Pay with UPI (GPay / PhonePe / Paytm)",
                            onClick = {
                                LendingUpiHelper.launchUpiPayment(
                                    context = context,
                                    personName = personName,
                                    amount = amount,
                                    note = note.ifBlank { null }
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            level = GlassLevel.Strong,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }

