package com.omkarnub.kanri.ui.popup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.RollingText
import com.omkarnub.kanri.ui.common.kokonut.KokonutSlideToConfirm
import com.omkarnub.kanri.ui.common.kokonut.SlideConfirmationType
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
 * 2. Visual drag handle pill at top + natural 1:1 drag-down to dismiss with rubberband resistance
 * 3. Dim grey timestamp in top center with subtle close button
 * 4. Prominent bold amount: Red for expense, Green for income
 * 5. Clean merchant name & bank/wallet source tag
 * 6. Top 5 categories only with left-aligned unboxed search hyperlink
 * 7. Smooth circular capsule notes input prompt
 * 8. Slide-to-confirm slider matching the app's internal workflow (KokonutSlideToConfirm)
 * 9. "Split expense ›" action with pre-filled amounts
 */
@Composable
fun InstantPopupCard(
    amount: Double,
    isDebit: Boolean,
    counterparty: String,
    bank: String?,
    sourceType: String,
    categories: List<CategoryEntity>,
    onDone: suspend (categoryId: Long?, note: String) -> Unit,
    onSplitBill: () -> Unit,
    onOpenInApp: () -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialCategoryId: Long? = null,
    initialNote: String = "",
    currentBalance: Double? = null
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
        currentBalance = currentBalance,
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
    currentBalance: Double?,
    onDone: suspend (categoryId: Long?, note: String) -> Unit,
    onSplitBill: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val haptics = rememberKanriHaptics()
    val density = LocalDensity.current
    val viewConfig = LocalViewConfiguration.current
    val touchSlop = viewConfig.touchSlop
    val coroutineScope = rememberCoroutineScope()

    var showBalanceView by remember { mutableStateOf(false) }

    val yAnim = remember { Animatable(0f) }
    var isDraggingDown by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val maxUpwardResistance = with(density) { 24.dp.toPx() }

    val currentY = if (isDraggingDown) dragY else yAnim.value

    fun calculateDragY(rawY: Float): Float {
        return if (rawY >= 0f) {
            rawY
        } else {
            -maxUpwardResistance * (1f - exp(rawY / (maxUpwardResistance * 2f)))
        }
    }

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

    // Dynamically ranked category pills: pre-selected category first, followed by top contextually ranked categories
    val top5Categories = remember(filteredCategories, selectedCategoryId) {
        val ordered = if (selectedCategoryId != null) {
            val sel = filteredCategories.filter { it.id == selectedCategoryId }
            val rest = filteredCategories.filter { it.id != selectedCategoryId }
            sel + rest
        } else {
            filteredCategories
        }
        ordered.take(7)
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

    val baseModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .offset { IntOffset(0, currentY.roundToInt().coerceAtLeast(0)) }
        .graphicsLayer {
            val dragPx = currentY.coerceAtLeast(0f)
            val maxFadeDist = with(density) { 320.dp.toPx() }
            alpha = (1f - (dragPx / maxFadeDist) * 0.40f).coerceIn(0.60f, 1f)
        }
        .pointerInput(Unit) {
            val velocityTracker = VelocityTracker()

            awaitEachGesture {
                val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                velocityTracker.resetTracking()
                velocityTracker.addPosition(down.uptimeMillis, down.position)

                val startY = down.position.y
                val startX = down.position.x
                var totalDx = 0f
                var totalDy = 0f
                var dragStarted = false

                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                    if (change.changedToUp()) {
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        break
                    }

                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    totalDx = change.position.x - startX
                    totalDy = change.position.y - startY

                    if (!dragStarted) {
                        // Disambiguation: horizontal swipe takes priority for category scroll & slider
                        if (abs(totalDx) > touchSlop && abs(totalDx) > abs(totalDy) * 1.15f) {
                            break
                        }
                        if (abs(totalDy) > touchSlop && abs(totalDy) > abs(totalDx) * 1.15f) {
                            dragStarted = true
                            isDraggingDown = true
                            dragY = calculateDragY(totalDy)
                        }
                    }

                    if (dragStarted) {
                        change.consume()
                        dragY = calculateDragY(totalDy)
                    }
                }

                if (dragStarted) {
                    val releaseVelocityY = velocityTracker.calculateVelocity().y
                    val dismissThreshold = with(density) { 90.dp.toPx() }
                    val commitOffset = dragY

                    val shouldDismiss = (commitOffset >= dismissThreshold) ||
                            (releaseVelocityY > 1000f && commitOffset > with(density) { 20.dp.toPx() })

                    coroutineScope.launch {
                        yAnim.snapTo(commitOffset)
                        isDraggingDown = false

                        if (shouldDismiss) {
                            haptics.click()
                            val exitDistance = with(density) { 450.dp.toPx() }
                            launch {
                                yAnim.animateTo(
                                    targetValue = exitDistance,
                                    initialVelocity = releaseVelocityY.coerceAtLeast(600f),
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = 0.86f
                                    )
                                )
                            }
                            onDismiss()
                        } else {
                            yAnim.animateTo(
                                targetValue = 0f,
                                initialVelocity = releaseVelocityY,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = 0.82f
                                )
                            )
                        }
                    }
                }
            }
        }

    val cardModifier = when {
        isGlass -> baseModifier
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
        isLight -> baseModifier
            .shadow(
                elevation = 16.dp,
                shape = cardShape,
                spotColor = Color.Black.copy(alpha = 0.10f),
                ambientColor = Color.Black.copy(alpha = 0.04f)
            )
            .clip(cardShape)
            .background(Color(0xFFFFFFFF))
            .border(width = 0.8.dp, color = Color(0xFFE5E2D9), shape = cardShape)
        isAmoled -> baseModifier
            .clip(cardShape)
            .background(Color(0xFF000000))
            .border(width = 0.8.dp, color = Color.White.copy(alpha = 0.18f), shape = cardShape)
        else -> baseModifier // Dark
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

        AnimatedContent(
            targetState = showBalanceView,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(420, easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f))
                    ) + fadeIn(animationSpec = tween(320)))
                        .togetherWith(
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> -fullHeight / 2 },
                                animationSpec = tween(320, easing = CubicBezierEasing(0.16f, 1.0f, 0.3f, 1.0f))
                            ) + fadeOut(animationSpec = tween(220))
                        ).using(
                            SizeTransform(clip = false) { _, _ ->
                                spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = 0.85f
                                )
                            }
                        )
                } else {
                    fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
                }
            },
            label = "PopupCardContentTransition"
        ) { isBalanceState ->
            if (!isBalanceState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            // Drag handle pill affordance (Natural Drag-Down to Dismiss)
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(dimGrey.copy(alpha = 0.35f))
            )

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

            // Primary Slide to Confirm CTA (#A4 KokonutSlideToConfirm)
            KokonutSlideToConfirm(
                label = if (isDebit) "Slide to save expense" else "Slide to save income",
                confirmingLabel = "Saving…",
                doneLabel = if (isDebit) "Expense saved!" else "Income added!",
                confirmationType = if (isDebit) SlideConfirmationType.EXPENSE else SlideConfirmationType.INCOME,
                onConfirm = {
                    onDone(selectedCategoryId, noteText)
                },
                onSuccessComplete = {
                    showBalanceView = true
                },
                modifier = Modifier.fillMaxWidth()
            )

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
    } else {
        CurrentBalanceContent(
            balance = currentBalance ?: if (isDebit) (15000.0 - amount) else (15000.0 + amount),
            amount = amount,
            isDebit = isDebit,
            brandTitle = brandTag.title,
            dimGrey = dimGrey,
            primaryTextColor = primaryTextColor,
            secondaryTextColor = secondaryTextColor,
            onDismiss = onDismiss
        )
    }
}
}
}

@Composable
private fun CurrentBalanceContent(
    balance: Double,
    amount: Double,
    isDebit: Boolean,
    brandTitle: String,
    dimGrey: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onDismiss: () -> Unit
) {
    // Automatically dismiss after 3 seconds as requested
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }

    val deltaColor = if (isDebit) Color(0xFFE54D2E) else Color(0xFF30A46C)
    val deltaSign = if (isDebit) "-" else "+"
    val deltaText = "($deltaSign${formatCurrency(amount)})"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag handle pill affordance (Natural Drag-Down to Dismiss)
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(50))
                .background(dimGrey.copy(alpha = 0.35f))
        )

        Spacer(modifier = Modifier.height(6.dp))

        // "Current Balance" title
        Text(
            text = "Current Balance",
            fontFamily = GoogleSansFlex,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = dimGrey,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Balance amount with rolling text animation + bracketed delta amount (+amount / -amount)
        val formattedBalance = remember(balance) {
            formatCurrency(balance)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RollingText(
                text = formattedBalance,
                textColor = primaryTextColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                minCycles = 3,
                cycleVariance = 3,
                durationSeconds = 1.4f,
                durationVarianceSeconds = 0.4f
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = deltaText,
                fontFamily = GoogleSansFlex,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = deltaColor,
                letterSpacing = (-0.2).sp
            )
        }

        if (brandTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = brandTitle,
                fontFamily = GoogleSansFlex,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                color = secondaryTextColor,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
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
