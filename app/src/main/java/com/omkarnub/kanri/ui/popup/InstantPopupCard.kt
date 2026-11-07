package com.omkarnub.kanri.ui.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.formatCurrency
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.KanriMotion
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.BrandFormatter
import com.omkarnub.kanri.util.rememberKanriHaptics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * InstantPopupCard: Floating interactive card for instant payment detection overlay.
 * Redesigned clean, modern UX unified across all themes (Glass, Light, Dark, AMOLED):
 * 1. Card surface adapts to theme (translucent frosted glass on Glass, crisp solid elevation on Light/Dark/AMOLED)
 * 2. Dim grey timestamp in top center with subtle close button
 * 3. Prominent bold amount: Red for expense, Green for income
 * 4. Clean merchant name & bank/wallet source tag
 * 5. Top 5 categories only with left-aligned unboxed search hyperlink
 * 6. Smooth circular capsule notes input prompt
 * 7. Single Done button with circular capsule edges
 * 8. "Split expense ›" action with pre-filled amounts
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
    onOpenInApp: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategoryId: Long? = null,
    initialNote: String = ""
) {
    ThemedInstantPopupCardContent(
        amount = amount,
        isDebit = isDebit,
        counterparty = counterparty,
        bank = bank,
        sourceType = sourceType,
        categories = categories,
        initialCategoryId = initialCategoryId,
        initialNote = initialNote,
        onDone = onDone,
        onSplitBill = onSplitBill,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun ThemedInstantPopupCardContent(
    amount: Double,
    isDebit: Boolean,
    counterparty: String,
    bank: String?,
    sourceType: String,
    categories: List<CategoryEntity>,
    initialCategoryId: Long?,
    initialNote: String,
    onDone: (categoryId: Long?, note: String) -> Unit,
    onSplitBill: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = rememberKanriHaptics()

    val themeMode = remember { ThemePreferences.getInstance(context).themeMode }
    val isGlass = themeMode == ThemeMode.GLASS
    val isLight = themeMode == ThemeMode.LIGHT
    val isAmoled = themeMode == ThemeMode.AMOLED

    // Dynamic semantic styling per theme
    val amountColor = if (isDebit) Color(0xFFE54D2E) else Color(0xFF30A46C)
    val dimGrey = if (isLight) Color(0xFF76777A) else Color(0xFF8E8E93)
    val primaryTextColor = if (isLight) Color(0xFF161616) else Color.White
    val secondaryTextColor = if (isLight) Color(0xFF6B685F) else Color.White.copy(alpha = 0.65f)

    val pillUnselectedBg = if (isLight) Color(0xFFF2F0E8) else Color.White.copy(alpha = 0.08f)
    val pillUnselectedText = if (isLight) Color(0xFF161616) else Color.White
    val pillSelectedBg = if (isLight) Color(0xFF161616) else Color.White
    val pillSelectedText = if (isLight) Color(0xFFFAF9F5) else Color(0xFF0F1018)

    val inputBg = if (isLight) Color(0xFFF2F0E8) else Color.White.copy(alpha = 0.08f)
    val inputPlaceholder = if (isLight) Color(0xFF8A8880) else Color.White.copy(alpha = 0.40f)

    val doneBtnBg = if (isLight) Color(0xFF161616) else Color.White
    val doneBtnText = if (isLight) Color(0xFFFAF9F5) else Color(0xFF0F1018)

    val walletPrefs = remember { com.omkarnub.kanri.data.wallet.WalletPreferences.getInstance(context) }
    val isWalletConfigured = walletPrefs.isWalletSetupCompleted
    val atmMode = walletPrefs.atmWithdrawalMode
    val isAtmTransfer = isDebit && sourceType.equals("ATM", ignoreCase = true) && atmMode == com.omkarnub.kanri.data.wallet.AtmWithdrawalMode.TRANSFER

    var selectedCategoryId by remember(initialCategoryId) { mutableStateOf(initialCategoryId) }
    var noteText by remember(initialNote) { mutableStateOf(initialNote) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val formattedDateTime = remember {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date())
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    // Top 5 categories only
    val top5Categories = remember(filteredCategories, selectedCategoryId) {
        val ordered = if (selectedCategoryId != null) {
            val sel = filteredCategories.filter { it.id == selectedCategoryId }
            val rest = filteredCategories.filter { it.id != selectedCategoryId }
            sel + rest
        } else {
            filteredCategories
        }
        ordered.take(5)
    }

    val displayMerchant = if (isAtmTransfer) {
        "ATM Cash Withdrawal"
    } else {
        BrandFormatter.cleanMerchantName(counterparty).ifBlank {
            if (isDebit) "Expense" else "Income"
        }
    }

    val brandTag = BrandFormatter.formatBrandTag(
        bank = bank,
        sourceType = sourceType,
        isWalletConfigured = isWalletConfigured,
        isAtmTransfer = isAtmTransfer
    )

    val cardShape = RoundedCornerShape(28.dp)

    val cardModifier = when {
        isGlass -> modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(cardShape)
            .background(Color(0xFF141622).copy(alpha = 0.35f))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape = cardShape
            )
        isLight -> modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                spotColor = Color.Black.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(cardShape)
            .background(Color(0xFFFFFFFF))
            .border(width = 0.8.dp, color = Color(0xFFE5E2D9), shape = cardShape)
        isAmoled -> modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(cardShape)
            .background(Color(0xFF000000))
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.18f), shape = cardShape)
        else -> modifier // Dark
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                spotColor = Color.Black.copy(alpha = 0.45f),
                ambientColor = Color.Black.copy(alpha = 0.20f)
            )
            .clip(cardShape)
            .background(Color(0xFF161618))
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.14f), shape = cardShape)
    }

    Box(
        modifier = cardModifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            focusManager.clearFocus()
        }
    ) {
        if (isGlass) {
            // Specular inner highlight rim along the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.50f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top dim grey date and time in top center
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formattedDateTime,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = dimGrey,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        haptics.click()
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = dimGrey,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount in bold with Red for expense, Green for income
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(280, easing = KanriMotion.EaseOut)) +
                            scaleIn(initialScale = 0.92f, animationSpec = KanriMotion.SnappySpring))
                        .togetherWith(fadeOut(animationSpec = tween(140)))
                },
                label = "AmountAnimation"
            ) { targetAmount ->
                Text(
                    text = "${if (isDebit) "-" else "+"} ${formatCurrency(targetAmount)}",
                    fontFamily = GoogleSansFlex,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    letterSpacing = (-0.5).sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Merchant Name
            Text(
                text = displayMerchant,
                fontFamily = GoogleSansFlex,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Bank / Source info
            if (brandTag.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = brandTag.title,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = secondaryTextColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Section: Plain text dim hyperlink search button above top 5 pills, aligned LEFT
            if (!isAtmTransfer) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Hyperlink search button on the LEFT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    haptics.click()
                                    isSearching = !isSearching
                                    if (!isSearching) searchQuery = ""
                                }
                                .padding(horizontal = 4.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = dimGrey,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isSearching) "Close search" else "Search categories",
                                fontFamily = GoogleSansFlex,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = dimGrey
                            )
                        }
                    }

                    // Optional search input box when search is toggled
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(50))
                                .background(inputBg)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = dimGrey,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = GoogleSansFlex,
                                        color = primaryTextColor,
                                        fontSize = 13.sp
                                    ),
                                    cursorBrush = SolidColor(primaryTextColor),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "Search categories...",
                                                fontFamily = GoogleSansFlex,
                                                color = inputPlaceholder,
                                                fontSize = 13.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Top 5 Category Pills: Circular capsule edges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        top5Categories.forEach { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) pillSelectedBg else pillUnselectedBg,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        haptics.click()
                                        selectedCategoryId = if (isSelected) null else cat.id
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = pillSelectedText,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    CategoryIcon(
                                        categoryName = cat.name,
                                        iconName = cat.iconName,
                                        tint = if (isSelected) pillSelectedText else pillUnselectedText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = cat.name,
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) pillSelectedText else pillUnselectedText
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes prompt: Circular edges
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(inputBg)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = inputPlaceholder,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = GoogleSansFlex,
                                    color = primaryTextColor,
                                    fontSize = 13.sp
                                ),
                                cursorBrush = SolidColor(primaryTextColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (noteText.isEmpty()) {
                                        Text(
                                            text = "Add note (optional)...",
                                            fontFamily = GoogleSansFlex,
                                            color = inputPlaceholder,
                                            fontSize = 13.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (noteText.isNotEmpty()) {
                                IconButton(
                                    onClick = { noteText = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Single Done button with circular capsule edges (RoundedCornerShape(50))
            Button(
                onClick = {
                    haptics.click()
                    onDone(selectedCategoryId, noteText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = doneBtnBg,
                    contentColor = doneBtnText
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Done",
                    fontFamily = GoogleSansFlex,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = doneBtnText
                )
            }

            // Replace open in kanri with split expense hyperlink
            if (isDebit && !isAtmTransfer) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Split expense  ›",
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = dimGrey,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            haptics.click()
                            onSplitBill()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0C15)
@Composable
private fun GlassInstantPopupDebitPreview() {
    val sampleCategories = listOf(
        CategoryEntity(id = 1L, name = "Food & Dining", colorHex = "#FF7043", iconName = "restaurant"),
        CategoryEntity(id = 2L, name = "Groceries", colorHex = "#10B981", iconName = "shopping_cart"),
        CategoryEntity(id = 3L, name = "Shopping", colorHex = "#8B5CF6", iconName = "shopping_bag"),
        CategoryEntity(id = 4L, name = "Auto / Taxi", colorHex = "#F59E0B", iconName = "taxi"),
        CategoryEntity(id = 5L, name = "Entertainment", colorHex = "#EC4899", iconName = "movie")
    )
    InstantPopupCard(
        amount = 380.00,
        isDebit = true,
        counterparty = "Swiggy",
        bank = "HDFC Bank",
        sourceType = "UPI",
        categories = sampleCategories,
        initialCategoryId = 1L,
        onDone = { _, _ -> },
        onSplitBill = {},
        onOpenInApp = {},
        onDismiss = {}
    )
}
