package com.omkarnub.kanri.ui.popup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Concept B: Bottom-docked frosted card for instant transaction overlays.
 * Thumb-friendly, ambient radial lighting, glowing category chips,
 * dual actions (Done / Split Bill), and 8-second auto-dismiss timer.
 */
@Composable
fun InstantPopupCard(
    amount: Double,
    isDebit: Boolean,
    counterparty: String,
    bank: String?,
    sourceType: String,
    categories: List<CategoryEntity>,
    onDone: (categoryId: Long?, note: String) -> Unit,
    onOpenInApp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isVisible by remember { mutableStateOf(false) }

    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryName by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    // Auto-dismiss countdown (8 seconds)
    var isInteracted by remember { mutableStateOf(false) }
    var autoDismissProgress by remember { mutableFloatStateOf(1f) }

    fun triggerDismiss(andExecute: () -> Unit = onDismiss) {
        if (!isVisible) return
        coroutineScope.launch {
            isVisible = false
            delay(180) // Smooth slide-down exit duration
            andExecute()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 8-second auto-dismiss timer (pauses if user interacts)
    LaunchedEffect(isInteracted) {
        if (isInteracted) return@LaunchedEffect
        val totalMs = 8000L
        val stepMs = 50L
        var elapsed = 0L
        while (elapsed < totalMs) {
            delay(stepMs)
            elapsed += stepMs
            autoDismissProgress = (1f - (elapsed.toFloat() / totalMs)).coerceIn(0f, 1f)
        }
        triggerDismiss {
            onDone(selectedCategoryId, noteText)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val walletPrefs = remember { com.omkarnub.kanri.data.wallet.WalletPreferences.getInstance(context) }
    val isWalletConfigured = walletPrefs.isWalletSetupCompleted
    val atmMode = walletPrefs.atmWithdrawalMode
    val isAtmTransfer = isDebit && sourceType.equals("ATM", ignoreCase = true) && atmMode == com.omkarnub.kanri.data.wallet.AtmWithdrawalMode.TRANSFER

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    val accentColor = if (isDebit) Color(0xFFE54D2E) else Color(0xFF30A46C)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(280, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(160))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isInteracted = true
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF131318)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            border = BorderStroke(1.dp, Color(0xFF262632))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3F3F48))
                )

                // Top Row: Time detected + Close icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Just now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF71717A)
                    )

                    IconButton(
                        onClick = { triggerDismiss() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E26))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Hero Section with Ambient Radial Glow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.16f),
                                    Color.Transparent
                                ),
                                radius = 280f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "${if (isDebit) "-" else "+"} ${formatCurrency(amount)}",
                            fontFamily = GoogleSansFlex,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = (-0.5).sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isAtmTransfer) "ATM Cash Withdrawal" else counterparty.ifBlank { if (isDebit) "Expense" else "Income" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEDEDED),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Transaction source badge
                        val detailText = if (isAtmTransfer) {
                            "₹${formatCurrency(amount)} withdrawn → added to Cash"
                        } else {
                            val walletSuffix = if (isWalletConfigured) "Online" else null
                            listOfNotNull(
                                bank?.takeIf { it.isNotBlank() },
                                sourceType.takeIf { it.isNotBlank() },
                                walletSuffix
                            ).joinToString(" • ")
                        }

                        if (detailText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1C1C24),
                                border = BorderStroke(0.8.dp, Color(0xFF2C2C38))
                            ) {
                                Text(
                                    text = detailText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAtmTransfer) Color(0xFF30A46C) else Color(0xFFA1A1AA),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isAtmTransfer) {
                    // Category Chips Row with Search
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (isSearching) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1B1B22))
                                    .border(1.dp, Color(0xFF282834), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        isInteracted = true
                                        searchQuery = it
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(Color.White),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search categories...",
                                                color = Color(0xFF52525B),
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                IconButton(
                                    onClick = {
                                        isSearching = false
                                        searchQuery = ""
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close search",
                                        tint = Color(0xFF71717A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Horizontal Category Chips Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isSearching) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF191922),
                                    border = BorderStroke(1.dp, Color(0xFF272733)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isInteracted = true
                                            isSearching = true
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search Categories",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            filteredCategories.forEach { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.14f) else Color(0xFF191922),
                                    border = if (isSelected) BorderStroke(1.2.dp, accentColor) else BorderStroke(1.dp, Color(0xFF272733)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            isInteracted = true
                                            if (isSelected) {
                                                selectedCategoryId = null
                                                selectedCategoryName = null
                                            } else {
                                                selectedCategoryId = cat.id
                                                selectedCategoryName = cat.name
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryIcon(
                                            categoryName = cat.name,
                                            iconName = cat.iconName,
                                            tint = if (isSelected) accentColor else Color(0xFFA1A1AA),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFD4D4D8)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Note Input field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF181820))
                            .border(1.dp, Color(0xFF262632), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = noteText,
                            onValueChange = {
                                isInteracted = true
                                noteText = it
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (noteText.isEmpty()) {
                                    Text(
                                        text = "Add a note (optional)",
                                        color = Color(0xFF52525B),
                                        fontSize = 13.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Dual Action Row: Done & Split Bill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            triggerDismiss {
                                onDone(selectedCategoryId, noteText)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1B1B24),
                        border = BorderStroke(1.dp, Color(0xFF2C2C3A)),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                triggerDismiss {
                                    onOpenInApp()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = "Split Bill",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Split Bill",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Small hyperlink text to open app
                Text(
                    text = "Open in Kanri ›",
                    fontSize = 12.sp,
                    color = Color(0xFF71717A),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            triggerDismiss {
                                onOpenInApp()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )

                // Whisper-thin auto-dismiss countdown bar at the bottom edge
                if (!isInteracted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { autoDismissProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = accentColor,
                        trackColor = Color(0xFF1C1C24)
                    )
                }
            }
        }
    }
}
