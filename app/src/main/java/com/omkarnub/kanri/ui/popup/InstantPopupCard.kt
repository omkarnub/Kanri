package com.omkarnub.kanri.ui.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
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
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.BrandFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Concept B: Bottom-docked frosted glassmorphism card for instant transaction overlays.
 * Features:
 * - Theme-aware styling matching user-selected mode (Dark, AMOLED, or Light)
 * - Pure monochrome luxury palette (zero blue accents on buttons, cursors, or input boxes)
 * - Inline Split Bill flow with interactive people counter & real-time share math
 * - Soft spring entrance and smooth content morph animations
 * - Persistent overlay (manual dismissal only)
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
    onSplitBill: () -> Unit,
    onOpenInApp: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var isVisible by remember { mutableStateOf(false) }

    // Theme resolution
    val themeMode = remember { ThemePreferences.getInstance(context).themeMode }
    val isLight = themeMode == ThemeMode.LIGHT
    val isAmoled = themeMode == ThemeMode.AMOLED

    // Theme-matched monochrome design tokens strictly matching user theme
    val containerBg = when {
        isLight -> Color(0xF8FAF9F5) // Soft frosted alabaster
        isAmoled -> Color(0xFA000000) // Deep AMOLED pitch black
        else -> Color(0xF2121212) // Frosted charcoal dark
    }

    val primaryTextColor = if (isLight) Color(0xFF000000) else Color(0xFFF2F2F5)
    val secondaryTextColor = if (isLight) Color(0xFF6B685F) else Color(0xFFA6A6A6)
    val chipBg = if (isLight) Color(0xFFF4F2EB) else if (isAmoled) Color(0xFF141414) else Color(0xFF1E1E1E)
    val inputBg = if (isLight) Color(0xFFECEAE2) else if (isAmoled) Color(0xFF1A1A1A) else Color(0xFF242424)
    val neutralBorder = if (isLight) Color(0x18000000) else Color(0x22FFFFFF)
    val activeBorder = if (isLight) Color(0xFF000000) else Color(0xFFFFFFFF)

    val specularGradient = if (isLight) {
        Brush.verticalGradient(
            listOf(
                Color(0x50FFFFFF),
                Color(0x12000000),
                Color(0x06000000)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0x35FFFFFF),
                Color(0x12FFFFFF),
                Color(0x18202020)
            )
        )
    }

    val doneBtnBg = if (isLight) Color(0xFF000000) else Color(0xFFFFFFFF)
    val doneBtnContent = if (isLight) Color(0xFFFAF9F5) else Color(0xFF121212)
    val secondaryBtnBg = if (isLight) Color(0xFFECEAE2) else if (isAmoled) Color(0xFF1E1E1E) else Color(0xFF262626)
    val secondaryBtnContent = if (isLight) Color(0xFF000000) else Color(0xFFF2F2F5)

    // Form state
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var noteText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    fun triggerDismiss(andExecute: () -> Unit = onDismiss) {
        if (!isVisible) return
        coroutineScope.launch {
            isVisible = false
            delay(180)
            andExecute()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

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
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(240)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(160))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 30.dp, bottomEnd = 30.dp),
            colors = CardDefaults.cardColors(containerColor = containerBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
            border = BorderStroke(1.dp, specularGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (isLight) Color(0x30000000) else Color(0x55FFFFFF))
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
                        color = secondaryTextColor
                    )

                    IconButton(
                        onClick = { triggerDismiss() },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isLight) Color(0x14000000) else Color(0x28FFFFFF))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isLight) Color(0xFF44444A) else Color(0xFFCCCCCC),
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
                                    accentColor.copy(alpha = if (isLight) 0.05f else 0.07f),
                                    Color.Transparent
                                ),
                                radius = 240f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Smoothly animated amount display
                        AnimatedContent(
                            targetState = amount,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                                        scaleIn(initialScale = 0.90f, animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f)))
                                    .togetherWith(
                                        fadeOut(animationSpec = tween(180))
                                    )
                            },
                            label = "AmountAnimation"
                        ) { targetAmount ->
                            Text(
                                text = "${if (isDebit) "-" else "+"} ${formatCurrency(targetAmount)}",
                                fontFamily = GoogleSansFlex,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                letterSpacing = (-0.5).sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Polished merchant display name
                        val displayMerchant = if (isAtmTransfer) {
                            "ATM Cash Withdrawal"
                        } else {
                            BrandFormatter.cleanMerchantName(counterparty).ifBlank {
                                if (isDebit) "Expense" else "Income"
                            }
                        }

                        Text(
                            text = displayMerchant,
                            fontFamily = GoogleSansFlex,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Polished fintech brand & source tag (monochrome)
                        val brandTag = BrandFormatter.formatBrandTag(
                            bank = bank,
                            sourceType = sourceType,
                            isWalletConfigured = isWalletConfigured,
                            isAtmTransfer = isAtmTransfer
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = chipBg,
                            border = BorderStroke(0.8.dp, neutralBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = brandTag.icon,
                                    contentDescription = null,
                                    tint = if (brandTag.isTransfer) Color(0xFF30A46C) else primaryTextColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = brandTag.title,
                                    fontFamily = GoogleSansFlex,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor,
                                    letterSpacing = 0.1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Standard Mode: Category Chips + Notes + [Done] [Split Bill]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                            if (!isAtmTransfer) {
                                if (isSearching) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(inputBg)
                                            .border(1.dp, activeBorder, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = primaryTextColor,
                                                fontSize = 13.sp
                                            ),
                                            cursorBrush = SolidColor(if (isLight) Color.Black else Color.White),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (searchQuery.isEmpty()) {
                                                    Text(
                                                        text = "Search categories...",
                                                        color = secondaryTextColor,
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
                                                tint = secondaryTextColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Category Chips Row
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
                                            color = chipBg,
                                            border = BorderStroke(1.dp, neutralBorder),
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
                                                    tint = secondaryTextColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    filteredCategories.forEach { cat ->
                                        val isSelected = selectedCategoryId == cat.id
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) {
                                                if (isLight) Color(0xFF141416) else Color(0xFFFFFFFF)
                                            } else chipBg,
                                            border = if (isSelected) {
                                                BorderStroke(1.2.dp, activeBorder)
                                            } else BorderStroke(1.dp, neutralBorder),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    selectedCategoryId = if (isSelected) null else cat.id
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CategoryIcon(
                                                    categoryName = cat.name,
                                                    iconName = cat.iconName,
                                                    tint = if (isSelected) {
                                                        if (isLight) Color.White else Color.Black
                                                    } else secondaryTextColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cat.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isSelected) {
                                                        if (isLight) Color.White else Color.Black
                                                    } else primaryTextColor
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Note Input Box (Pure monochrome)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(inputBg)
                                        .border(1.dp, neutralBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    BasicTextField(
                                        value = noteText,
                                        onValueChange = { noteText = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = primaryTextColor,
                                            fontSize = 13.sp
                                        ),
                                        cursorBrush = SolidColor(if (isLight) Color.Black else Color.White),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { innerTextField ->
                                            if (noteText.isEmpty()) {
                                                Text(
                                                    text = "Add a note (optional)",
                                                    color = secondaryTextColor,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Dual Action Row: Done & Split Bill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        containerColor = doneBtnBg,
                                        contentColor = doneBtnContent
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                ) {
                                    Text(
                                        text = "Done",
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                if (isDebit && !isAtmTransfer) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = secondaryBtnBg,
                                        border = BorderStroke(1.dp, neutralBorder),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                triggerDismiss {
                                                    onSplitBill()
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
                                                tint = secondaryBtnContent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Split Bill",
                                                fontFamily = GoogleSansFlex,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = secondaryBtnContent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                Spacer(modifier = Modifier.height(6.dp))

                // Small hyperlink text to open app
                Text(
                    text = "Open in Kanri ›",
                    fontSize = 12.sp,
                    color = secondaryTextColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            triggerDismiss {
                                onOpenInApp()
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
