package com.omkarnub.kanri.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import com.omkarnub.kanri.util.rememberKanriHaptics
import com.omkarnub.kanri.widget.BudgetProgressWidgetProvider
import com.omkarnub.kanri.widget.BudgetRingWidgetProvider
import com.omkarnub.kanri.widget.GoalsRingWidgetProvider
import com.omkarnub.kanri.widget.LendBorrowWidgetProvider
import com.omkarnub.kanri.widget.MoneySpentTodayWidgetProvider
import com.omkarnub.kanri.widget.QuickAddWidgetProvider
import com.omkarnub.kanri.widget.SplitBillWidgetProvider
import com.omkarnub.kanri.widget.WalletBalanceWidgetProvider
import com.omkarnub.kanri.widget.WidgetStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pins a widget to the Android launcher home screen directly using [AppWidgetManager.requestPinAppWidget].
 */
fun pinWidgetToHome(context: Context, providerClass: Class<*>) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    if (appWidgetManager.isRequestPinAppWidgetSupported) {
        val myProvider = ComponentName(context, providerClass)
        appWidgetManager.requestPinAppWidget(myProvider, null, null)
    } else {
        Toast.makeText(
            context,
            "Long-press your phone home screen to place this widget",
            Toast.LENGTH_LONG
        ).show()
    }
}

// =============================================================================
// 1. SPENT TODAY WIDGET PREVIEW
// =============================================================================

@Composable
fun SpentTodayWidgetPreview(
    style: WidgetStyle,
    amount: Double = 1450.0,
    txCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }

    WidgetPreviewCardWrapper(
        title = "Spent Today",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, MoneySpentTodayWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                IPhoneSpentToday(amount = amount, txCount = txCount, replayTrigger = replayTrigger)
            }
            WidgetStyle.GLASS_100X -> {
                Glass100xSpentToday(amount = amount, txCount = txCount, replayTrigger = replayTrigger)
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Spent Today",
                    subtitle = if (txCount == 0) "no spends" else "$txCount expenses",
                    value = amount.toInt().toString(),
                    unit = CurrencyUtils.getCurrencySymbol(),
                    visual = DotTileVisual.Slider(
                        value = 0.42f,
                        mode = "fill",
                        ticks = 36,
                        labels = Pair("calm", "heavy")
                    ),
                    jitterMagnitude = 20,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

@Composable
private fun IPhoneSpentToday(
    amount: Double,
    txCount: Int,
    replayTrigger: Int
) {
    val animProgress = remember(replayTrigger) { Animatable(0f) }
    LaunchedEffect(replayTrigger) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, spring(dampingRatio = 0.75f, stiffness = 300f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xF01C1C1E))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF2D55))
                    )
                    Text(
                        text = "SPENT TODAY",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.08.sp,
                        fontSize = 11.sp,
                        color = Color(0xFFFF2D55)
                    )
                }
                Text(
                    text = if (txCount == 1) "1 spend" else "$txCount spends",
                    fontFamily = GoogleSansFlex,
                    fontSize = 11.sp,
                    color = Color(0x99FFFFFF)
                )
            }

            // Big Currency Value
            Text(
                text = CurrencyUtils.formatCurrency(amount * animProgress.value),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )

            // Cupertino Micro Bar Visual (Morning, Afternoon, Evening, Night)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val heights = listOf(0.35f, 0.70f, 0.95f, 0.20f)
                val labels = listOf("Morn", "Noon", "Eve", "Night")
                heights.forEachIndexed { idx, h ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((18 * h * animProgress.value).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (idx == 2) Color(0xFFFF2D55) else Color(0x40FFFFFF))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = labels[idx],
                            fontSize = 8.sp,
                            fontFamily = GoogleSansFlex,
                            color = Color(0x66FFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Glass100xSpentToday(
    amount: Double,
    txCount: Int,
    replayTrigger: Int
) {
    val animProgress = remember(replayTrigger) { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    LaunchedEffect(replayTrigger) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0x30FFFFFF), Color(0x15FFFFFF), Color(0x0CFFFFFF))
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x99FFFFFF),
                        Color(0x33FFFFFF),
                        Color(0x0DFFFFFF)
                    )
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(18.dp)
    ) {
        // Specular ambient bloom behind amount
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x3300E5FF), Color.Transparent),
                    center = Offset(size.width * 0.4f, size.height * 0.5f),
                    radius = size.width * 0.45f
                )
            )
            // Shimmer reflection line
            val x = shimmerOffset * size.width
            drawLine(
                brush = Brush.linearGradient(
                    listOf(Color.Transparent, Color(0x33FFFFFF), Color.Transparent),
                    start = Offset(x - 50f, 0f),
                    end = Offset(x + 50f, size.height)
                ),
                start = Offset(x - 50f, 0f),
                end = Offset(x + 50f, size.height),
                strokeWidth = 3f
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "SPENT TODAY",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.sp,
                fontSize = 11.sp,
                color = Color(0xCCFFFFFF)
            )

            Text(
                text = CurrencyUtils.formatCurrency(amount * animProgress.value),
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x2200E5FF))
                    .border(0.75.dp, Color(0x5500E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (txCount == 0) "No spends today • In peace" else "$txCount expenses recorded today",
                    fontFamily = GoogleSansFlex,
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
        }
    }
}

// =============================================================================
// 2. WALLET BALANCE WIDGET PREVIEW
// =============================================================================

@Composable
fun WalletBalanceWidgetPreview(
    style: WidgetStyle,
    cash: Double = 4250.0,
    online: Double = 38500.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }
    val total = cash + online

    WidgetPreviewCardWrapper(
        title = "Wallet Balances",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, WalletBalanceWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NET WORTH",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xFF0A84FF)
                            )
                            Text(
                                text = "Total " + CurrencyUtils.formatCompactCurrency(total),
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        // Apple Wallet Dual Stack Representation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Cash card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2E30D158))
                                    .border(1.dp, Color(0x5530D158), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Cash Wallet",
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 10.sp,
                                        color = Color(0xFF30D158)
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCompactCurrency(cash),
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            // Bank card
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2E0A84FF))
                                    .border(1.dp, Color(0x550A84FF), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Online Bank",
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 10.sp,
                                        color = Color(0xFF0A84FF)
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCompactCurrency(online),
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIQUID BALANCES",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xCCFFFFFF)
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(total),
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2200E676))
                                    .border(1.dp, Color(0x4400E676), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Cash", fontSize = 10.sp, color = Color(0xFF00E676))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(cash),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2200E5FF))
                                    .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Online", fontSize = 10.sp, color = Color(0xFF00E5FF))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(online),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Wallets",
                    subtitle = "Cash & Online",
                    value = (total / 1000).toInt().toString(),
                    unit = "k",
                    visual = DotTileVisual.Rise(
                        value = (cash / total).toFloat().coerceIn(0.1f, 0.9f),
                        labels = Pair("Cash", "Online")
                    ),
                    jitterMagnitude = 1,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// 3. BUDGET RING WIDGET PREVIEW
// =============================================================================

@Composable
fun BudgetRingWidgetPreview(
    style: WidgetStyle,
    spent: Double = 18500.0,
    budget: Double = 50000.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }
    val ratio = if (budget > 0) (spent / budget).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining = (budget - spent).coerceAtLeast(0.0)

    WidgetPreviewCardWrapper(
        title = "Budget Ring",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, BudgetRingWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                val animProgress = remember(replayTrigger) { Animatable(0f) }
                LaunchedEffect(replayTrigger) {
                    animProgress.snapTo(0f)
                    animProgress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 250f))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "MONTHLY BUDGET",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0xFFFF2D55)
                        )

                        // Circular Activity Ring Canvas
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = size.width * 0.13f
                                val pad = stroke / 2f
                                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

                                // Track
                                drawArc(
                                    color = Color(0x33FF2D55),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )

                                // Progress
                                drawArc(
                                    color = Color(0xFFFF2D55),
                                    startAngle = -90f,
                                    sweepAngle = ratio * 360f * animProgress.value,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LEFT",
                                    fontSize = 10.sp,
                                    fontFamily = GoogleSansFlex,
                                    color = Color(0x99FFFFFF)
                                )
                                Text(
                                    text = CurrencyUtils.formatCompactCurrency(remaining),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = GoogleSansFlex,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: " + CurrencyUtils.formatCompactCurrency(spent),
                                fontSize = 11.sp,
                                color = Color(0x99FFFFFF)
                            )
                            Text(
                                text = "Limit: " + CurrencyUtils.formatCompactCurrency(budget),
                                fontSize = 11.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                val animProgress = remember(replayTrigger) { Animatable(0f) }
                LaunchedEffect(replayTrigger) {
                    animProgress.snapTo(0f)
                    animProgress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SAFE TO SPEND",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0xCCFFFFFF)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(130.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Ambient Bloom
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0x4400E5FF), Color.Transparent),
                                        center = center,
                                        radius = size.width * 0.45f
                                    )
                                )

                                val stroke = size.width * 0.11f
                                val pad = stroke / 2f
                                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

                                // Frosted track
                                drawArc(
                                    color = Color(0x2EFFFFFF),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )

                                // Neon Cyan to Emerald gradient arc
                                drawArc(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = ratio * 360f * animProgress.value,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Remaining", fontSize = 10.sp, color = Color(0xCCFFFFFF))
                                Text(
                                    CurrencyUtils.formatCompactCurrency(remaining),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Spent: " + CurrencyUtils.formatCompactCurrency(spent),
                                fontSize = 11.sp,
                                color = Color(0xB3FFFFFF)
                            )
                            Text(
                                "Limit: " + CurrencyUtils.formatCompactCurrency(budget),
                                fontSize = 11.sp,
                                color = Color(0xB3FFFFFF)
                            )
                        }
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Budget Ring",
                    subtitle = "spent of limit",
                    value = (spent / 1000).toInt().toString(),
                    unit = "k",
                    visual = DotTileVisual.Slider(
                        value = ratio,
                        mode = "glow",
                        ticks = 40,
                        labels = Pair("₹0", CurrencyUtils.formatCompactCurrency(budget))
                    ),
                    jitterMagnitude = 1,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// 4. BUDGET PROGRESS WIDGET PREVIEW
// =============================================================================

@Composable
fun BudgetProgressWidgetPreview(
    style: WidgetStyle,
    spent: Double = 18500.0,
    budget: Double = 50000.0,
    daysLeft: Int = 18,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }
    val percentUsed = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0

    WidgetPreviewCardWrapper(
        title = "Budget Progress",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, BudgetProgressWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PACING",
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xFF5E5CE6)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x335E5CE6))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$percentUsed% used",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5E5CE6)
                                )
                            }
                        }

                        Text(
                            text = CurrencyUtils.formatCurrency(spent),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )

                        // Segmented iOS Progress Gauge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x33FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentUsed / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF5E5CE6))
                            )
                        }

                        Text(
                            text = "of ${CurrencyUtils.formatCompactCurrency(budget)} • $daysLeft days left",
                            fontSize = 11.sp,
                            color = Color(0x99FFFFFF)
                        )
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "MONTHLY BUDGET",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xCCFFFFFF)
                            )
                            Text(
                                "$percentUsed%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF00E5FF)
                            )
                        }

                        Text(
                            CurrencyUtils.formatCurrency(spent),
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x22FFFFFF))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(percentUsed / 100f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00E5FF), Color(0xFF00E676))
                                        )
                                    )
                            )
                        }

                        Text(
                            "of ${CurrencyUtils.formatCompactCurrency(budget)} • $daysLeft days in cycle",
                            fontSize = 11.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Progress",
                    subtitle = "$daysLeft days left",
                    value = percentUsed.toString(),
                    unit = "%",
                    visual = DotTileVisual.Rise(
                        value = percentUsed / 100f,
                        labels = Pair("₹0", CurrencyUtils.formatCompactCurrency(budget))
                    ),
                    jitterMagnitude = 0,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// 5. GOALS RING WIDGET PREVIEW
// =============================================================================

@Composable
fun GoalsRingWidgetPreview(
    style: WidgetStyle,
    goalTitle: String = "Japan Vacation",
    emoji: String = "✈️",
    saved: Double = 65000.0,
    target: Double = 100000.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }
    val percent = if (target > 0) ((saved / target) * 100).toInt().coerceIn(0, 100) else 0

    WidgetPreviewCardWrapper(
        title = "Savings Goal",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, GoalsRingWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "SAVINGS GOAL",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0xFF30D158)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = size.width * 0.12f
                                val pad = stroke / 2f
                                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

                                drawArc(
                                    color = Color(0x3330D158),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )

                                drawArc(
                                    color = Color(0xFF30D158),
                                    startAngle = -90f,
                                    sweepAngle = (percent / 100f) * 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 22.sp)
                                Text(
                                    text = "$percent%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            text = goalTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${CurrencyUtils.formatCompactCurrency(saved)} of ${CurrencyUtils.formatCompactCurrency(target)}",
                            fontSize = 11.sp,
                            color = Color(0x99FFFFFF)
                        )
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "DREAM GOAL",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0xCCFFFFFF)
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        listOf(Color(0x44BF5AF2), Color.Transparent),
                                        center = center,
                                        radius = size.width * 0.45f
                                    )
                                )
                                val stroke = size.width * 0.11f
                                val pad = stroke / 2f
                                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)

                                drawArc(
                                    color = Color(0x2EFFFFFF),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )

                                drawArc(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFFBF5AF2), Color(0xFF0A84FF))
                                    ),
                                    startAngle = -90f,
                                    sweepAngle = (percent / 100f) * 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad, pad),
                                    size = arcSize,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(emoji, fontSize = 22.sp)
                                Text(
                                    "$percent%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Text(
                            goalTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            "${CurrencyUtils.formatCompactCurrency(saved)} / ${CurrencyUtils.formatCompactCurrency(target)}",
                            fontSize = 11.sp,
                            color = Color(0xB3FFFFFF)
                        )
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = goalTitle,
                    subtitle = "target reach",
                    value = percent.toString(),
                    unit = "%",
                    visual = DotTileVisual.Steps(
                        data = listOf(0.2f, 0.35f, 0.45f, 0.60f, 0.75f, 0.85f, 1f),
                        labels = Pair(CurrencyUtils.formatCompactCurrency(0.0), CurrencyUtils.formatCompactCurrency(target))
                    ),
                    jitterMagnitude = 0,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// 6. LEND & BORROW WIDGET PREVIEW
// =============================================================================

@Composable
fun LendBorrowWidgetPreview(
    style: WidgetStyle,
    toReceive: Double = 5200.0,
    toPay: Double = 1800.0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }

    WidgetPreviewCardWrapper(
        title = "Lend & Borrow",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, LendBorrowWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PENDING SETTLEMENTS",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xFF30D158)
                            )
                            Text(
                                "Net +${CurrencyUtils.formatCompactCurrency(toReceive - toPay)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF30D158)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2E30D158))
                                    .border(1.dp, Color(0x5530D158), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("To Receive", fontSize = 10.sp, color = Color(0xFF30D158))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(toReceive),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2EFF453A))
                                    .border(1.dp, Color(0x55FF453A), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("To Pay", fontSize = 10.sp, color = Color(0xFFFF453A))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(toPay),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "LEND & BORROW",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xCCFFFFFF)
                            )
                            Text(
                                "Active",
                                fontSize = 11.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x2200E676))
                                    .border(1.dp, Color(0x4400E676), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Receivable", fontSize = 10.sp, color = Color(0xFF00E676))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(toReceive),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x22FF5252))
                                    .border(1.dp, Color(0x44FF5252), RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text("Payable", fontSize = 10.sp, color = Color(0xFFFF5252))
                                    Text(
                                        CurrencyUtils.formatCompactCurrency(toPay),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Dues Ledger",
                    subtitle = "net receivables",
                    value = ((toReceive - toPay) / 1000).toInt().toString(),
                    unit = "k",
                    visual = DotTileVisual.Blocks(
                        data = listOf(Pair(0.15f, 0.35f), Pair(0.55f, 0.75f)),
                        labels = Pair("Pay", "Receive")
                    ),
                    jitterMagnitude = 0,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// 7. QUICK ADD WIDGET PREVIEW
// =============================================================================

@Composable
fun QuickAddWidgetPreview(
    style: WidgetStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var clickedAction by remember { mutableStateOf<String?>(null) }

    WidgetPreviewCardWrapper(
        title = "Quick Record",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, QuickAddWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            clickedAction = "Replayed"
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "QUICK RECORD",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0x99FFFFFF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x33FF2D55))
                                    .border(1.dp, Color(0x55FF2D55), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptics.click()
                                        clickedAction = "Expense"
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                                    Text("Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x3330D158))
                                    .border(1.dp, Color(0x5530D158), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptics.click()
                                        clickedAction = "Income"
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF30D158), modifier = Modifier.size(16.dp))
                                    Text("Income", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "RAPID RECORD",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.08.sp,
                            fontSize = 11.sp,
                            color = Color(0xCCFFFFFF)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptics.click()
                                        clickedAction = "Expense"
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    Text("Spend", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x22FFFFFF))
                                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(16.dp))
                                    .clickable {
                                        haptics.click()
                                        clickedAction = "Income"
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                    Text("Income", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Quick Add",
                    subtitle = "tap to launch",
                    value = "1-TAP",
                    unit = "",
                    visual = DotTileVisual.Dots(
                        data = listOf(0.2f, 0.5f, 0.8f, 1f, 0.7f, 0.4f),
                        labels = Pair("Rapid", "Smart")
                    ),
                    jitterMagnitude = 0,
                    onClick = {
                        haptics.click()
                        clickedAction = "DotAdd"
                    }
                )
            }
        }
    }
}

// =============================================================================
// 8. SPLIT BILL WIDGET PREVIEW
// =============================================================================

@Composable
fun SplitBillWidgetPreview(
    style: WidgetStyle,
    billTotal: Double = 3200.0,
    peopleCount: Int = 4,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    var replayTrigger by remember { mutableIntStateOf(0) }
    val perPerson = billTotal / peopleCount

    WidgetPreviewCardWrapper(
        title = "Split the Bill",
        styleTag = style.tag,
        onPin = {
            haptics.click()
            pinWidgetToHome(context, SplitBillWidgetProvider::class.java)
        },
        onReplay = {
            haptics.click()
            replayTrigger++
        },
        modifier = modifier
    ) {
        when (style) {
            WidgetStyle.IPHONE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xF01C1C1E))
                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(24.dp))
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0x330A84FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF0A84FF))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SPLIT THE BILL",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xFF0A84FF)
                            )
                            Text(
                                "${CurrencyUtils.formatCompactCurrency(perPerson)} / person",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                "Total ${CurrencyUtils.formatCompactCurrency(billTotal)} • $peopleCount people",
                                fontSize = 11.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0x66FFFFFF), modifier = Modifier.size(16.dp))
                    }
                }
            }
            WidgetStyle.GLASS_100X -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x30FFFFFF), Color(0x18FFFFFF), Color(0x0CFFFFFF))
                            )
                        )
                        .border(
                            1.5.dp,
                            Brush.linearGradient(
                                listOf(Color(0x99FFFFFF), Color(0x33FFFFFF), Color(0x0DFFFFFF))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x22FFFFFF))
                                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = Color(0xFF00E5FF))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "GROUP SPLIT",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.08.sp,
                                fontSize = 11.sp,
                                color = Color(0xCCFFFFFF)
                            )
                            Text(
                                "${CurrencyUtils.formatCompactCurrency(perPerson)} each",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                "Total ${CurrencyUtils.formatCompactCurrency(billTotal)} amongst $peopleCount friends",
                                fontSize = 11.sp,
                                color = Color(0x99FFFFFF)
                            )
                        }
                    }
                }
            }
            WidgetStyle.PIXEL_MATRIX -> {
                DotTileCard(
                    title = "Split Bill",
                    subtitle = "$peopleCount people equal",
                    value = perPerson.toInt().toString(),
                    unit = CurrencyUtils.getCurrencySymbol(),
                    visual = DotTileVisual.Slider(
                        value = 0.5f,
                        mode = "glow",
                        ticks = 30,
                        labels = Pair("1p", "10p")
                    ),
                    jitterMagnitude = 0,
                    onClick = { replayTrigger++ }
                )
            }
        }
    }
}

// =============================================================================
// REUSABLE CARD WRAPPER WITH PIN & REPLAY CONTROLS
// =============================================================================

@Composable
fun WidgetPreviewCardWrapper(
    title: String,
    styleTag: String,
    onPin: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label and Action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = styleTag,
                        fontSize = 10.sp,
                        fontFamily = GoogleSansFlex,
                        color = Color(0xCCFFFFFF)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onReplay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Replay Animation",
                        tint = Color(0x99FFFFFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onPin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Pin,
                        contentDescription = "Pin to Home Screen",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Widget 1:1 Content
        content()
    }
}

