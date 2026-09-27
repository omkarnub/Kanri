package com.omkarnub.kanri.ui.lending

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Triggers a crisp tactile haptic impulse when the split animation occurs.
 */
private fun triggerSplitHaptic(context: Context, hapticFeedback: HapticFeedback) {
    try {
        com.omkarnub.kanri.util.KanriHaptics.create(context, hapticFeedback).primaryAction()
    } catch (_: Exception) {}
}

/**
 * Full-page screen for splitting group expenses with a fully-animated monochrome ring chart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitExpenseScreen(
    onDismiss: () -> Unit,
    onSaveLenderSplit: (friendNames: List<String>, perPersonShare: Double, eventDescription: String) -> Unit,
    onSaveBorrowerSplit: (payerName: String, userShare: Double, eventDescription: String) -> Unit,
    existingPeople: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    var amountText by remember { mutableStateOf("") }
    var peopleCount by remember { mutableIntStateOf(3) }
    var eventDescription by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(SplitRole.I_PAID) }
    var payerName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Animation & View state
    var isSplitCompleted by remember { mutableStateOf(false) }
    var isSplitting by remember { mutableStateOf(false) }
    val splitProgress = remember { Animatable(0f) }

    // Dynamic names list for friends splitting with the user
    val friendNames = remember { mutableStateListOf<String>() }

    // Initialize or adjust friend slots based on peopleCount
    LaunchedEffect(Unit) {
        val needed = (peopleCount - 1).coerceAtLeast(1)
        for (i in 0 until needed) {
            val defaultName = if (i < existingPeople.size) existingPeople[i] else "Friend ${i + 1}"
            friendNames.add(defaultName)
        }
    }

    fun syncFriendNames(newCount: Int) {
        val needed = (newCount - 1).coerceAtLeast(1)
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

    val totalAmount = amountText.toDoubleOrNull() ?: 0.0
    val perPersonShare = if (peopleCount > 0) totalAmount / peopleCount else 0.0
    val othersTotal = perPersonShare * (peopleCount - 1)

    BackHandler {
        if (isSplitCompleted) {
            coroutineScope.launch {
                splitProgress.animateTo(0f, animationSpec = tween(380))
                isSplitCompleted = false
            }
        } else {
            onDismiss()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Split Group Expense",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // ==========================================
            // FULLY ANIMATED MONOCHROME RING (DONUT) CHART
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedMonochromeRingChart(
                    peopleCount = peopleCount,
                    perPersonShare = perPersonShare,
                    totalAmount = totalAmount,
                    splitProgress = splitProgress.value,
                    sizeDp = 250.dp,
                    strokeWidthDp = 22.dp
                )
            }

            // ==========================================
            // STAGE 1: AMOUNT INPUT & DIGIT COUNTER & "SPLIT NOW"
            // ==========================================
            if (!isSplitCompleted && !isSplitting) {
                // Rock-Solid, Non-Glitched Amount Input with GoogleSansFlex
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Text(
                        text = "TOTAL BILL AMOUNT",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { focusRequester.requestFocus() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "₹",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (amountText.length > 6) 28.sp else 36.sp,
                                color = if (amountText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            BasicTextField(
                                value = amountText,
                                onValueChange = { input ->
                                    if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                        val dotIdx = input.indexOf('.')
                                        if (dotIdx == -1 || input.length - dotIdx <= 3) {
                                            amountText = input
                                            errorMessage = null
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = GoogleSansFlex,
                                    fontSize = if (amountText.length > 6) 28.sp else 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.focusRequester(focusRequester),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (amountText.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                fontFamily = GoogleSansFlex,
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontFamily = GoogleSansFlex,
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==========================================
                // PROPER ANIMATED DIGIT COUNTER FOR PEOPLE
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        0.8.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NUMBER OF PEOPLE",
                            fontFamily = GoogleSansFlex,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Decrease [-] Button with smooth tactile feedback
                            IconButton(
                                onClick = {
                                    if (peopleCount > 2) {
                                        peopleCount--
                                        syncFriendNames(peopleCount)
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = peopleCount > 2,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (peopleCount > 2) MaterialTheme.colorScheme.surfaceContainerHighest
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = if (peopleCount > 2) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Proper Animated Number Counter
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedContent(
                                    targetState = peopleCount,
                                    transitionSpec = {
                                        if (targetState > initialState) {
                                            (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it } + fadeIn())
                                                .togetherWith(slideOutVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it } + fadeOut())
                                        } else {
                                            (slideInVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it } + fadeIn())
                                                .togetherWith(slideOutVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it } + fadeOut())
                                        }
                                    },
                                    label = "digit_counter_anim"
                                ) { count ->
                                    Text(
                                        text = "$count",
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Text(
                                    text = "You + ${peopleCount - 1} friends",
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Increase [+] Button with smooth tactile feedback
                            IconButton(
                                onClick = {
                                    if (peopleCount < 20) {
                                        peopleCount++
                                        syncFriendNames(peopleCount)
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                enabled = peopleCount < 20,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (peopleCount < 20) MaterialTheme.colorScheme.surfaceContainerHighest
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = if (peopleCount < 20) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Clean, Professional "Split Now" Button (Strictly NO Emojis)
                Button(
                    onClick = {
                        if (totalAmount <= 0.0) {
                            errorMessage = "Please enter a valid bill amount"
                            return@Button
                        }
                        triggerSplitHaptic(context, hapticFeedback)
                        coroutineScope.launch {
                            isSplitting = true
                            splitProgress.snapTo(0f)
                            splitProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 700,
                                    easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
                                )
                            )
                            isSplitting = false
                            isSplitCompleted = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Split Now",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ==========================================
            // STAGE 2: EXPLODED BREAKDOWN & PRE-EXISTING / NEW PROFILES
            // ==========================================
            if (isSplitCompleted || isSplitting) {
                AnimatedVisibility(
                    visible = splitProgress.value > 0.40f,
                    enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                fontFamily = GoogleSansFlex,
                                color = Color(0xFFFF6B6B),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Text(
                            text = "Who Paid the Bill?",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Role selection cards (Strictly NO Emojis)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Option 1: I Paid Full Bill (Money I Lent)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.2.dp,
                                        if (selectedRole == SplitRole.I_PAID) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedRole = SplitRole.I_PAID },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == SplitRole.I_PAID) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "I Paid Full Bill",
                                            fontFamily = GoogleSansFlex,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (selectedRole == SplitRole.I_PAID) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Collect from friends (Money I Lent)",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Option 2: Someone Else Paid (Money I Borrowed)
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        1.2.dp,
                                        if (selectedRole == SplitRole.SOMEONE_ELSE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedRole = SplitRole.SOMEONE_ELSE },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedRole == SplitRole.SOMEONE_ELSE) {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Someone Else Paid",
                                            fontFamily = GoogleSansFlex,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        if (selectedRole == SplitRole.SOMEONE_ELSE) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "I owe my share (Money I Borrowed)",
                                        fontFamily = GoogleSansFlex,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ==========================================
                        // PARTICIPANT PROFILES: SELECT EXISTING OR CREATE NEW
                        // ==========================================
                        if (selectedRole == SplitRole.I_PAID) {
                            Text(
                                text = "Who is splitting with you? (${friendNames.size} friends)",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // For each friend slot, show existing profile picker + custom name input
                            friendNames.forEachIndexed { index, currentName ->
                                FriendSlotProfileCard(
                                    slotIndex = index + 1,
                                    currentName = currentName,
                                    perPersonShare = perPersonShare,
                                    existingPeople = existingPeople,
                                    onNameChange = { newName ->
                                        friendNames[index] = newName
                                        errorMessage = null
                                    },
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Who paid the bill?",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            PayerProfileCard(
                                currentName = payerName,
                                userShare = perPersonShare,
                                existingPeople = existingPeople,
                                onNameChange = {
                                    payerName = it
                                    errorMessage = null
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Event / Description
                        OutlinedTextField(
                            value = eventDescription,
                            onValueChange = { eventDescription = it },
                            label = {
                                Text(
                                    "Event / Description (Optional)",
                                    fontFamily = GoogleSansFlex
                                )
                            },
                            placeholder = {
                                Text(
                                    "e.g. Dinner, Movie tickets, Grocery run",
                                    fontFamily = GoogleSansFlex
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(
                                fontFamily = GoogleSansFlex,
                                fontSize = 14.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary Action: Save to Lend & Borrow
                        Button(
                            onClick = {
                                if (selectedRole == SplitRole.I_PAID) {
                                    val blankIndices = friendNames.mapIndexedNotNull { idx, name ->
                                        if (name.trim().isBlank()) idx + 1 else null
                                    }
                                    if (blankIndices.isNotEmpty()) {
                                        errorMessage = "Please enter or select a name for Person ${blankIndices.first()}"
                                        return@Button
                                    }
                                    onSaveLenderSplit(friendNames.toList(), perPersonShare, eventDescription)
                                    onDismiss()
                                } else {
                                    if (payerName.trim().isBlank()) {
                                        errorMessage = "Please enter or select who paid the bill"
                                        return@Button
                                    }
                                    onSaveBorrowerSplit(payerName.trim(), perPersonShare, eventDescription)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (selectedRole == SplitRole.I_PAID) {
                                    "Save to Lend & Borrow (+${CurrencyUtils.formatCurrency(othersTotal)})"
                                } else {
                                    "Save to Lend & Borrow (-${CurrencyUtils.formatCurrency(perPersonShare)})"
                                },
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Secondary Actions: Adjust Split / Done
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        splitProgress.animateTo(0f, animationSpec = tween(380))
                                        isSplitCompleted = false
                                    }
                                }
                            ) {
                                Text(
                                    text = "← Adjust Split",
                                    fontFamily = GoogleSansFlex,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }

                            TextButton(onClick = onDismiss) {
                                Text(
                                    text = "Done (Just Calculate)",
                                    fontFamily = GoogleSansFlex,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual card for a friend slot with pre-existing profile selection and custom new name input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendSlotProfileCard(
    slotIndex: Int,
    currentName: String,
    perPersonShare: Double,
    existingPeople: List<String>,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmed = currentName.trim()
    val matchedExisting = existingPeople.firstOrNull { it.trim().equals(trimmed, ignoreCase = true) }
    val isExistingProfile = matchedExisting != null
    val isNewProfile = !isExistingProfile && trimmed.isNotBlank() && !trimmed.startsWith("Friend ")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.9.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Slot number and Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${slotIndex + 1}",
                            fontFamily = GoogleSansFlex,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Person ${slotIndex + 1}",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "+${CurrencyUtils.formatCurrency(perPersonShare)}",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFF00D09C)
                )
            }

            // Quick Pre-existing Profiles Picker
            if (existingPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isExistingProfile,
                        onClick = {
                            if (isExistingProfile) onNameChange("")
                        },
                        label = {
                            Text(
                                "+ New Person",
                                fontFamily = GoogleSansFlex,
                                fontSize = 11.5.sp,
                                fontWeight = if (!isExistingProfile) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    existingPeople.forEach { person ->
                        val isThisSelected = person.trim().equals(trimmed, ignoreCase = true)
                        FilterChip(
                            selected = isThisSelected,
                            onClick = { onNameChange(person) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isThisSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.take(1).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = person,
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Editable Text Field
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Person Name", fontFamily = GoogleSansFlex, fontSize = 12.sp) },
                placeholder = { Text("e.g. Alex, Maya", fontFamily = GoogleSansFlex, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = if (isExistingProfile) Icons.Default.Person else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isExistingProfile) Color(0xFF00D09C) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontFamily = GoogleSansFlex,
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Status Badge
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isExistingProfile) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00D09C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Existing profile: will sync to \"$matchedExisting\"",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00D09C)
                    )
                } else if (isNewProfile) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New profile: will create a new person in Lend & Borrow",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Select an existing profile or type a new name",
                        fontFamily = GoogleSansFlex,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Card for selecting the bill payer (when someone else paid the bill).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerProfileCard(
    currentName: String,
    userShare: Double,
    existingPeople: List<String>,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmed = currentName.trim()
    val matchedExisting = existingPeople.firstOrNull { it.trim().equals(trimmed, ignoreCase = true) }
    val isExistingProfile = matchedExisting != null
    val isNewProfile = !isExistingProfile && trimmed.isNotBlank()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.9.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bill Payer",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "-${CurrencyUtils.formatCurrency(userShare)}",
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    color = Color(0xFFFF6B6B)
                )
            }

            if (existingPeople.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !isExistingProfile,
                        onClick = {
                            if (isExistingProfile) onNameChange("")
                        },
                        label = {
                            Text(
                                "+ New Person",
                                fontFamily = GoogleSansFlex,
                                fontSize = 11.5.sp,
                                fontWeight = if (!isExistingProfile) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    existingPeople.forEach { person ->
                        val isThisSelected = person.trim().equals(trimmed, ignoreCase = true)
                        FilterChip(
                            selected = isThisSelected,
                            onClick = { onNameChange(person) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isThisSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = person.take(1).uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isThisSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = person,
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isThisSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Payer's Name", fontFamily = GoogleSansFlex, fontSize = 12.sp) },
                placeholder = { Text("e.g. Rahul, Sneha", fontFamily = GoogleSansFlex, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = if (isExistingProfile) Icons.Default.Person else Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isExistingProfile) Color(0xFF00D09C) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontFamily = GoogleSansFlex,
                    fontSize = 14.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isExistingProfile) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF00D09C),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Existing profile: you will owe \"$matchedExisting\"",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00D09C)
                    )
                } else if (isNewProfile) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New profile: will create a new person in Lend & Borrow",
                        fontFamily = GoogleSansFlex,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Select who paid or type their name",
                        fontFamily = GoogleSansFlex,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
