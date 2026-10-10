package com.omkarnub.kanri.ui.popup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.omkarnub.kanri.ui.theme.Panchang
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean, minimalistic on-screen transaction modal popup.
 * Center-aligned, elegant typography, category search, optional note input,
 * explicit Done action, and snappy professional animations.
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

    fun triggerDismiss(andExecute: () -> Unit = onDismiss) {
        if (!isVisible) return
        coroutineScope.launch {
            isVisible = false
            delay(160) // Snappy exit duration
            andExecute()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
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

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ),
        exit = scaleOut(
            targetScale = 0.97f,
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        ) + fadeOut(
            animationSpec = tween(140, easing = FastOutSlowInEasing)
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF131316)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 18.dp),
            border = BorderStroke(1.dp, Color(0xFF26262C))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Row: Time detected + Minimalist Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF71717A),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center: Amount
                Text(
                    text = "${if (isDebit) "-" else "+"} ${formatCurrency(amount)}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDebit) Color(0xFFE54D2E) else Color(0xFF30A46C),
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Center: Transaction Counterparty
                Text(
                    text = if (isAtmTransfer) "ATM Cash Withdrawal" else counterparty.ifBlank { if (isDebit) "Expense" else "Income" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFEDEDED),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Center: Transaction Details (Bank • Source • Wallet)
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
                    Text(
                        text = detailText,
                        fontSize = 12.sp,
                        color = if (isAtmTransfer) Color(0xFF30A46C) else Color(0xFF71717A),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (!isAtmTransfer) {
                // Categories with Search Button
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isSearching) {
                        // Inline Search Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1B1B20))
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
                                onValueChange = { searchQuery = it },
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isSearching) {
                            // Search Toggle Button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1A1A20),
                                border = BorderStroke(1.dp, Color(0xFF272730)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isSearching = true }
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
                                color = if (isSelected) Color.White else Color(0xFF1A1A20),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF272730)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIcon(
                                        categoryName = cat.name,
                                        iconName = cat.iconName,
                                        tint = if (isSelected) Color.Black else Color(0xFFA1A1AA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color(0xFFD4D4D8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Small input box for note (optional)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1A1A20))
                        .border(1.dp, Color(0xFF272730), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
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

                Spacer(modifier = Modifier.height(18.dp))
                }

                // Just a Done button
                Button(
                    onClick = {
                        triggerDismiss {
                            onDone(selectedCategoryId, noteText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
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

                Spacer(modifier = Modifier.height(10.dp))

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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
