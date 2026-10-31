package com.omkarnub.kanri.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.theme.GlassCustomization
import com.omkarnub.kanri.ui.theme.GlassPresets
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.theme.createCustomGlassTokens
import com.omkarnub.kanri.ui.theme.lerpColor
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassCustomizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val savedCustomization by themePrefs.glassCustomizationFlow.collectAsState()

    // Working copy for real-time responsive drag
    var config by remember(savedCustomization) { mutableStateOf(savedCustomization) }

    fun applyChange(newConfig: GlassCustomization) {
        config = newConfig
        themePrefs.glassCustomization = newConfig
    }

    val previewTokens = remember(config) { createCustomGlassTokens(config) }
    val previewHazeState = remember { HazeState() }
    var previewMode by remember { mutableStateOf(0) } // 0: Overview, 1: Recent Tx, 2: Settings, 3: Controls

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CUSTOMIZE GLASS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.click()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Reset defaults button
                    IconButton(
                        onClick = {
                            haptics.warning()
                            applyChange(GlassPresets.DEFAULT)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Defaults",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Save & Exit button shifted to top
                    Button(
                        onClick = {
                            haptics.click()
                            applyChange(config)
                            onBack()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .padding(end = 4.dp)
                    ) {
                        Text(
                            text = "Save & Exit",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =================================================================
            // 1. SINGLE REAL-TIME LIVE PREVIEW HERO AT TOP
            // =================================================================
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.3.sp,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        // Mode Selector Pills
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Overview", "Recent Tx", "Settings", "Controls").forEachIndexed { index, title ->
                                val isSelected = previewMode == index
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .border(
                                            0.8.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            haptics.tick()
                                            previewMode = index
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = title,
                                        fontFamily = GoogleSansFlex,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Hero container with rich, colorful ambient backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(config.cornerRadiusDp.dp.coerceAtLeast(16.dp)))
                    ) {
                        // Background sampling source for live Haze blur
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .hazeSource(previewHazeState)
                                .background(Color(0xFF09090E))
                        ) {
                            // Glowing Amber / Coral Orb
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .offset(x = (-25).dp, y = (-25).dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFF7A00).copy(alpha = 0.75f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )

                            // Glowing Emerald Mint Orb
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 35.dp, y = 35.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF30A46C).copy(alpha = 0.70f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )

                            // Glowing Purple / Violet Orb in center
                            Box(
                                modifier = Modifier
                                    .size(150.dp)
                                    .align(Alignment.Center)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF7F56D9).copy(alpha = 0.60f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )

                            // High-contrast background typography that demonstrates real-time diffusion
                            Text(
                                text = "✦ KANRI FROST ENGINE • LIVE ✦",
                                fontFamily = Panchang,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 2.sp,
                                color = Color.White.copy(alpha = 0.30f),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp)
                            )
                        }

                        // Real-time Frosted Glass Component based on previewMode
                        CompositionLocalProvider(
                            LocalKanriGlass provides previewTokens,
                            LocalHazeState provides previewHazeState
                        ) {
                            when (previewMode) {
                                1 -> {
                                    // Recent Transactions Preview
                                    KanriGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        level = GlassLevel.Blur,
                                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                                        hazeState = previewHazeState
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "RECENT TRANSACTIONS",
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp,
                                                    fontSize = 10.5.sp,
                                                    color = previewTokens.textSecondary
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .background(previewTokens.tintFlat)
                                                        .border(previewTokens.borderWidth, previewTokens.borderStart, RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "3 records",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = previewTokens.textPrimary
                                                    )
                                                }
                                            }

                                            PreviewTxRow(
                                                icon = Icons.Default.Paid,
                                                title = "Blue Tokai Coffee",
                                                category = "Food & Dining",
                                                wallet = "Cash",
                                                amount = "-$4.50",
                                                amountColor = previewTokens.expenseColor,
                                                tokens = previewTokens
                                            )

                                            HorizontalDivider(color = previewTokens.borderEnd)

                                            PreviewTxRow(
                                                icon = Icons.Default.AccountBalanceWallet,
                                                title = "Client Project Deposit",
                                                category = "Income",
                                                wallet = "Bank",
                                                amount = "+$1,250.00",
                                                amountColor = previewTokens.incomeColor,
                                                tokens = previewTokens
                                            )

                                            HorizontalDivider(color = previewTokens.borderEnd)

                                            PreviewTxRow(
                                                icon = Icons.Default.Tune,
                                                title = "Supermarket Groceries",
                                                category = "Groceries",
                                                wallet = "Online",
                                                amount = "-$48.20",
                                                amountColor = previewTokens.expenseColor,
                                                tokens = previewTokens
                                            )
                                        }
                                    }
                                }
                                2 -> {
                                    // Settings Section Preview
                                    KanriGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        level = GlassLevel.Blur,
                                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                                        hazeState = previewHazeState
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "SETTINGS SECTION",
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp,
                                                    fontSize = 10.5.sp,
                                                    color = previewTokens.textSecondary
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .background(previewTokens.tintFlat)
                                                        .border(previewTokens.borderWidth, previewTokens.borderStart, RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "Glass Surface",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = previewTokens.textPrimary
                                                    )
                                                }
                                            }

                                            // Row 1: App Lock
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(previewTokens.tintFlat)
                                                            .border(previewTokens.borderWidth, previewTokens.borderStart, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Shield,
                                                            contentDescription = null,
                                                            tint = previewTokens.textPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text("App Lock", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = previewTokens.textPrimary)
                                                        Text("Biometric verification", fontSize = 11.sp, color = previewTokens.textSecondary)
                                                    }
                                                }
                                                Switch(
                                                    checked = true,
                                                    onCheckedChange = {},
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = previewTokens.textPrimary,
                                                        checkedTrackColor = previewTokens.tintFlat
                                                    )
                                                )
                                            }

                                            HorizontalDivider(color = previewTokens.borderEnd)

                                            // Row 2: Currency
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(previewTokens.tintFlat)
                                                            .border(previewTokens.borderWidth, previewTokens.borderStart, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Paid,
                                                            contentDescription = null,
                                                            tint = previewTokens.textPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text("App Currency", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = previewTokens.textPrimary)
                                                        Text("USD ($) • Default", fontSize = 11.sp, color = previewTokens.textSecondary)
                                                    }
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .background(previewTokens.tintFlat)
                                                        .border(previewTokens.borderWidth, previewTokens.borderStart, RoundedCornerShape((previewTokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Text("Change", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = previewTokens.textPrimary)
                                                }
                                            }

                                            HorizontalDivider(color = previewTokens.borderEnd)

                                            // Row 3: Rules
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(previewTokens.tintFlat)
                                                            .border(previewTokens.borderWidth, previewTokens.borderStart, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.AutoFixHigh,
                                                            contentDescription = null,
                                                            tint = previewTokens.textPrimary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text("Smart Merchant Rules", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = previewTokens.textPrimary)
                                                        Text("14 active auto-rules", fontSize = 11.sp, color = previewTokens.textSecondary)
                                                    }
                                                }
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = previewTokens.textTertiary, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Controls & Chips Preview
                                    KanriGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        level = GlassLevel.Blur,
                                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                                        hazeState = previewHazeState
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = "GLASS CONTROLS & CHIPS",
                                                fontFamily = GoogleSansFlex,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.2.sp,
                                                fontSize = 10.5.sp,
                                                color = previewTokens.textSecondary
                                            )

                                            // Filter chips row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .background(previewTokens.tintFlat)
                                                        .border(previewTokens.borderWidth, previewTokens.borderStart, RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text("All (48)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = previewTokens.textPrimary)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .background(previewTokens.tintFlat.copy(alpha = 0.5f))
                                                        .border(previewTokens.borderWidth, previewTokens.borderEnd, RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(previewTokens.expenseColor))
                                                        Text("Spent", fontSize = 11.5.sp, color = previewTokens.textSecondary)
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .background(previewTokens.tintFlat.copy(alpha = 0.5f))
                                                        .border(previewTokens.borderWidth, previewTokens.borderEnd, RoundedCornerShape((config.cornerRadiusDp.dp * 0.4f).coerceAtLeast(6.dp)))
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(previewTokens.incomeColor))
                                                        Text("Income", fontSize = 11.5.sp, color = previewTokens.textSecondary)
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Action buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape((config.cornerRadiusDp.dp * 0.45f).coerceAtLeast(8.dp)))
                                                        .background(previewTokens.tintFlat)
                                                        .border(previewTokens.borderWidth, previewTokens.borderStart, RoundedCornerShape((config.cornerRadiusDp.dp * 0.45f).coerceAtLeast(8.dp)))
                                                        .clickable { haptics.click() }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+ Add Record", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = previewTokens.textPrimary)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape((config.cornerRadiusDp.dp * 0.45f).coerceAtLeast(8.dp)))
                                                        .background(previewTokens.tintFlat.copy(alpha = 0.5f))
                                                        .border(previewTokens.borderWidth, previewTokens.borderEnd, RoundedCornerShape((config.cornerRadiusDp.dp * 0.45f).coerceAtLeast(8.dp)))
                                                        .clickable { haptics.click() }
                                                        .padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Move Money", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = previewTokens.textSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // Original Overview Card
                                    KanriGlassCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        level = GlassLevel.Blur,
                                        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
                                        hazeState = previewHazeState
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "TOTAL SAVINGS",
                                                    fontFamily = GoogleSansFlex,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.2.sp,
                                                    fontSize = 10.5.sp,
                                                    color = previewTokens.textSecondary
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(previewTokens.tintFlat)
                                                        .border(
                                                            previewTokens.borderWidth,
                                                            previewTokens.borderStart,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "Live Glass",
                                                        fontFamily = GoogleSansFlex,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = previewTokens.textPrimary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "$24,580.00",
                                                fontFamily = GoogleSansFlex,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 26.sp,
                                                color = previewTokens.textPrimary
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                text = "+14.2% monthly pace • On Track",
                                                fontFamily = GoogleSansFlex,
                                                fontSize = 11.5.sp,
                                                color = previewTokens.textSecondary
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val expenseColor = config.expenseColor
                                                val incomeColor = config.incomeColor
                                                val expenseTint = config.expenseTintOpacity.coerceIn(0.04f, 0.50f)
                                                val incomeTint = config.incomeTintOpacity.coerceIn(0.04f, 0.50f)
                                                val darkFactor = if (config.tintLightness < 0f) (-config.tintLightness).coerceIn(0f, 1f) else 0f
                                                val darkBaseColor = lerpColor(Color(0xFF14141A), Color(0xFF08080C), darkFactor)
                                                val baseBackingAlpha = (0.65f + darkFactor * 0.28f).coerceIn(0.50f, 0.95f)
                                                val btnShape = RoundedCornerShape(config.cornerRadiusDp.dp.coerceIn(10.dp, 18.dp))

                                                // Live Preview Expense Button
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(btnShape)
                                                        .background(darkBaseColor.copy(alpha = baseBackingAlpha))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    expenseColor.copy(alpha = (expenseTint * (1.1f + darkFactor * 0.35f)).coerceIn(0.10f, 0.65f)),
                                                                    expenseColor.copy(alpha = (expenseTint * 0.40f).coerceIn(0.02f, 0.35f))
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            BorderStroke(
                                                                config.borderWidthDp.dp.coerceIn(0.8.dp, 2.dp),
                                                                Brush.linearGradient(
                                                                    listOf(
                                                                        expenseColor.copy(alpha = (expenseTint * (2.8f + darkFactor * 0.6f)).coerceIn(0.25f, 0.95f)),
                                                                        expenseColor.copy(alpha = (expenseTint * 1.2f).coerceIn(0.08f, 0.60f))
                                                                    )
                                                                )
                                                            ),
                                                            btnShape
                                                        )
                                                        .clickable { haptics.click() }
                                                        .padding(vertical = 11.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(expenseColor)
                                                        )
                                                        Text(
                                                            text = "Expense",
                                                            fontFamily = GoogleSansFlex,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp,
                                                            color = Color.White
                                                        )
                                                    }
                                                }

                                                // Live Preview Income Button
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(btnShape)
                                                        .background(darkBaseColor.copy(alpha = baseBackingAlpha))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    incomeColor.copy(alpha = (incomeTint * (1.1f + darkFactor * 0.35f)).coerceIn(0.10f, 0.65f)),
                                                                    incomeColor.copy(alpha = (incomeTint * 0.40f).coerceIn(0.02f, 0.35f))
                                                                )
                                                            )
                                                        )
                                                        .border(
                                                            BorderStroke(
                                                                config.borderWidthDp.dp.coerceIn(0.8.dp, 2.dp),
                                                                Brush.linearGradient(
                                                                    listOf(
                                                                        incomeColor.copy(alpha = (incomeTint * (2.8f + darkFactor * 0.6f)).coerceIn(0.25f, 0.95f)),
                                                                        incomeColor.copy(alpha = (incomeTint * 1.2f).coerceIn(0.08f, 0.60f))
                                                                    )
                                                                )
                                                            ),
                                                            btnShape
                                                        )
                                                        .clickable { haptics.click() }
                                                        .padding(vertical = 11.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(incomeColor)
                                                        )
                                                        Text(
                                                            text = "Income",
                                                            fontFamily = GoogleSansFlex,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp,
                                                            color = Color.White
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

            // =================================================================
            // 2. QUICK STYLE PRESETS
            // =================================================================
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "QUICK STYLE PRESETS",
                        fontFamily = GoogleSansFlex,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.3.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(
                            label = "Default Frosted",
                            isSelected = config == GlassPresets.DEFAULT,
                            onClick = {
                                haptics.click()
                                applyChange(GlassPresets.DEFAULT)
                            }
                        )
                        PresetChip(
                            label = "Ultra Sheer",
                            isSelected = config == GlassPresets.ULTRA_SHEER,
                            onClick = {
                                haptics.click()
                                applyChange(GlassPresets.ULTRA_SHEER)
                            }
                        )
                        PresetChip(
                            label = "Deep Obsidian",
                            isSelected = config == GlassPresets.DEEP_OBSIDIAN,
                            onClick = {
                                haptics.click()
                                applyChange(GlassPresets.DEEP_OBSIDIAN)
                            }
                        )
                        PresetChip(
                            label = "Luminous Frost",
                            isSelected = config == GlassPresets.LUMINOUS_FROST,
                            onClick = {
                                haptics.click()
                                applyChange(GlassPresets.LUMINOUS_FROST)
                            }
                        )
                        PresetChip(
                            label = "Neon Crystal",
                            isSelected = config == GlassPresets.NEON_CRYSTAL,
                            onClick = {
                                haptics.click()
                                applyChange(GlassPresets.NEON_CRYSTAL)
                            }
                        )
                    }
                }
            }

            // =================================================================
            // 3. ESSENTIAL SLIDER 1: FROSTED BLUR
            // =================================================================
            item {
                val blurDp = config.blurRadiusDp
                val blurDescriptor = when {
                    blurDp <= 0f -> "Off (Crystal Clear)"
                    blurDp <= 12f -> "${blurDp.roundToInt()} dp • Subtle Frost"
                    blurDp <= 28f -> "${blurDp.roundToInt()} dp • Balanced Frost"
                    blurDp <= 42f -> "${blurDp.roundToInt()} dp • Deep Frost"
                    else -> "${blurDp.roundToInt()} dp • Heavy Mist"
                }

                CustomizerCard(
                    title = "FROSTED BLUR",
                    icon = Icons.Default.BlurOn
                ) {
                    SettingSliderRow(
                        label = "Blur Strength",
                        valueBadge = blurDescriptor,
                        subtext = "Softens and diffuses background wallpaper and charts behind cards",
                        value = config.blurRadiusDp,
                        range = 0f..50f,
                        minLabel = "Crystal Clear (0 dp)",
                        maxLabel = "Heavy Mist (50 dp)",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(blurRadiusDp = it))
                        }
                    )
                }
            }

            // =================================================================
            // 4. ESSENTIAL SLIDER 2: GLASS OPACITY
            // =================================================================
            item {
                val opacityPercent = (config.surfaceOpacity * 100).roundToInt()
                val opacityDescriptor = when {
                    config.surfaceOpacity <= 0.04f -> "$opacityPercent% • Ultra Sheer"
                    config.surfaceOpacity <= 0.12f -> "$opacityPercent% • Refined Translucent"
                    config.surfaceOpacity <= 0.25f -> "$opacityPercent% • Frosted Surface"
                    else -> "$opacityPercent% • Solid Glass"
                }

                CustomizerCard(
                    title = "SURFACE OPACITY",
                    icon = Icons.Default.Opacity
                ) {
                    SettingSliderRow(
                        label = "Glass Transparency",
                        valueBadge = opacityDescriptor,
                        subtext = "Controls how see-through or solid the card surface appears",
                        value = config.surfaceOpacity,
                        range = 0.02f..0.40f,
                        minLabel = "See-Through (2%)",
                        maxLabel = "Milky Solid (40%)",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(surfaceOpacity = it))
                        }
                    )
                }
            }

            // =================================================================
            // 5. ESSENTIAL SLIDER 3: GLASS TONE & TINT
            // =================================================================
            item {
                val tint = config.tintLightness
                val tintDescriptor = when {
                    tint <= -0.2f -> "Smoky Obsidian (${(-tint * 100).roundToInt()}%)"
                    tint >= 0.2f -> "Iced Frost (+${(tint * 100).roundToInt()}%)"
                    else -> "Neutral Crystal"
                }

                CustomizerCard(
                    title = "GLASS TINT & TONE",
                    icon = Icons.Default.InvertColors
                ) {
                    SettingSliderRow(
                        label = "Color Tone",
                        valueBadge = tintDescriptor,
                        subtext = "Slide left for dark smoky obsidian glass, or right for bright white frost",
                        value = config.tintLightness,
                        range = -1.0f..1.0f,
                        minLabel = "◀ Dark Charcoal",
                        maxLabel = "Bright Frost ▶",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(tintLightness = it))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Tone Shortcut Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TonePresetButton(
                            text = "Smoky Dark",
                            isSelected = config.tintLightness <= -0.5f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(tintLightness = -0.75f))
                            }
                        )
                        TonePresetButton(
                            text = "Neutral",
                            isSelected = config.tintLightness in -0.1f..0.1f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(tintLightness = 0.0f))
                            }
                        )
                        TonePresetButton(
                            text = "Ice Frost",
                            isSelected = config.tintLightness >= 0.5f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(tintLightness = 0.70f))
                            }
                        )
                    }
                }
            }

            // =================================================================
            // ESSENTIAL SLIDER: EXPENSE (RED) TINT
            // =================================================================
            item {
                val tintPercent = (config.expenseTintOpacity * 100).roundToInt()
                val tintDescriptor = when {
                    config.expenseTintOpacity <= 0.08f -> "$tintPercent% • Subtle Whisper"
                    config.expenseTintOpacity <= 0.18f -> "$tintPercent% • Balanced Frosted Tint"
                    config.expenseTintOpacity <= 0.28f -> "$tintPercent% • Vibrant Glow"
                    else -> "$tintPercent% • Rich Saturated Tint"
                }

                CustomizerCard(
                    title = "EXPENSE (RED) TINT",
                    icon = Icons.Default.InvertColors
                ) {
                    val expenseSwatches = listOf(
                        "Crimson" to 0xFFE54D2EL,
                        "Rose" to 0xFFF43F5EL,
                        "Flame" to 0xFFFF3366L,
                        "Coral" to 0xFFFF6B4AL,
                        "Ruby" to 0xFFDC2626L
                    )

                    ColorSwatchRow(
                        title = "Color Shade",
                        selectedColorHex = config.expenseColorHex,
                        swatches = expenseSwatches,
                        onColorSelected = { newHex ->
                            haptics.click()
                            applyChange(config.copy(expenseColorHex = newHex))
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSliderRow(
                        label = "Red Tint Strength",
                        valueBadge = tintDescriptor,
                        subtext = "Controls how deeply red-frosted the Expense action button appears",
                        value = config.expenseTintOpacity,
                        range = 0.04f..0.45f,
                        minLabel = "Subtle (4%)",
                        maxLabel = "Vibrant (45%)",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(expenseTintOpacity = it))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TonePresetButton(
                            text = "Subtle (8%)",
                            isSelected = config.expenseTintOpacity in 0.06f..0.11f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(expenseTintOpacity = 0.08f))
                            }
                        )
                        TonePresetButton(
                            text = "Balanced (18%)",
                            isSelected = config.expenseTintOpacity in 0.14f..0.22f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(expenseTintOpacity = 0.18f))
                            }
                        )
                        TonePresetButton(
                            text = "Vibrant (28%)",
                            isSelected = config.expenseTintOpacity in 0.24f..0.32f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(expenseTintOpacity = 0.28f))
                            }
                        )
                        TonePresetButton(
                            text = "Rich (38%)",
                            isSelected = config.expenseTintOpacity >= 0.35f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(expenseTintOpacity = 0.38f))
                            }
                        )
                    }
                }
            }

            // =================================================================
            // ESSENTIAL SLIDER: INCOME (GREEN) TINT
            // =================================================================
            item {
                val tintPercent = (config.incomeTintOpacity * 100).roundToInt()
                val tintDescriptor = when {
                    config.incomeTintOpacity <= 0.08f -> "$tintPercent% • Subtle Whisper"
                    config.incomeTintOpacity <= 0.18f -> "$tintPercent% • Balanced Frosted Tint"
                    config.incomeTintOpacity <= 0.28f -> "$tintPercent% • Vibrant Glow"
                    else -> "$tintPercent% • Rich Saturated Tint"
                }

                CustomizerCard(
                    title = "INCOME (GREEN) TINT",
                    icon = Icons.Default.InvertColors
                ) {
                    val incomeSwatches = listOf(
                        "Emerald" to 0xFF30A46CL,
                        "Jade" to 0xFF10B981L,
                        "Lime" to 0xFF84CC16L,
                        "Spring" to 0xFF22C55EL,
                        "Teal" to 0xFF14B8A6L
                    )

                    ColorSwatchRow(
                        title = "Color Shade",
                        selectedColorHex = config.incomeColorHex,
                        swatches = incomeSwatches,
                        onColorSelected = { newHex ->
                            haptics.click()
                            applyChange(config.copy(incomeColorHex = newHex))
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSliderRow(
                        label = "Green Tint Strength",
                        valueBadge = tintDescriptor,
                        subtext = "Controls how deeply green-frosted the Income action button appears",
                        value = config.incomeTintOpacity,
                        range = 0.04f..0.45f,
                        minLabel = "Subtle (4%)",
                        maxLabel = "Vibrant (45%)",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(incomeTintOpacity = it))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TonePresetButton(
                            text = "Subtle (8%)",
                            isSelected = config.incomeTintOpacity in 0.06f..0.11f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(incomeTintOpacity = 0.08f))
                            }
                        )
                        TonePresetButton(
                            text = "Balanced (18%)",
                            isSelected = config.incomeTintOpacity in 0.14f..0.22f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(incomeTintOpacity = 0.18f))
                            }
                        )
                        TonePresetButton(
                            text = "Vibrant (28%)",
                            isSelected = config.incomeTintOpacity in 0.24f..0.32f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(incomeTintOpacity = 0.28f))
                            }
                        )
                        TonePresetButton(
                            text = "Rich (38%)",
                            isSelected = config.incomeTintOpacity >= 0.35f,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptics.click()
                                applyChange(config.copy(incomeTintOpacity = 0.38f))
                            }
                        )
                    }
                }
            }

            // =================================================================
            // 6. ESSENTIAL SLIDER 4: BORDER SHINE
            // =================================================================
            item {
                val borderPercent = (config.borderOpacity * 100).roundToInt()
                val borderDescriptor = when {
                    config.borderOpacity <= 0.02f -> "Borderless (Stealth)"
                    config.borderOpacity <= 0.20f -> "$borderPercent% • Subtle Rim"
                    config.borderOpacity <= 0.45f -> "$borderPercent% • Refined Glow"
                    else -> "$borderPercent% • Radiant Edge"
                }

                CustomizerCard(
                    title = "BORDER & EDGE SHINE",
                    icon = Icons.Default.BorderColor
                ) {
                    SettingSliderRow(
                        label = "Border Highlight",
                        valueBadge = borderDescriptor,
                        subtext = "Gleaming specular rim light outlining glass cards and buttons",
                        value = config.borderOpacity,
                        range = 0.0f..0.70f,
                        minLabel = "Borderless",
                        maxLabel = "Radiant Rim",
                        onValueChange = {
                            haptics.tick()
                            val newBorderOpacity = it
                            val newWidth = (0.8f + newBorderOpacity * 0.8f).coerceIn(0.8f, 1.6f)
                            applyChange(config.copy(borderOpacity = newBorderOpacity, borderWidthDp = newWidth))
                        }
                    )
                }
            }

            // =================================================================
            // 7. ESSENTIAL SLIDER 5: CORNER CURVATURE
            // =================================================================
            item {
                val radius = config.cornerRadiusDp.roundToInt()
                val radiusDescriptor = when {
                    radius <= 14 -> "$radius dp • Sleek & Sharp"
                    radius <= 24 -> "$radius dp • Modern Rounded"
                    else -> "$radius dp • Soft Pebble"
                }

                CustomizerCard(
                    title = "CORNER CURVATURE",
                    icon = Icons.Default.Tune
                ) {
                    SettingSliderRow(
                        label = "Corner Roundedness",
                        valueBadge = radiusDescriptor,
                        subtext = "Curvature radius of cards, bottom sheets, and the navigation dock",
                        value = config.cornerRadiusDp,
                        range = 10f..32f,
                        minLabel = "Sleek (10 dp)",
                        maxLabel = "Pebble (32 dp)",
                        onValueChange = {
                            haptics.tick()
                            applyChange(config.copy(cornerRadiusDp = it))
                        }
                    )
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE HELPER UI COMPONENTS
// =============================================================================

@Composable
private fun CustomizerCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    val tokens = LocalKanriGlass.current
    if (tokens != null) {
        KanriGlassCard(
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.Flat,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                CardHeader(title = title, icon = icon)
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    } else {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                CardHeader(title = title, icon = icon)
                Spacer(modifier = Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorSwatchRow(
    title: String,
    selectedColorHex: Long,
    swatches: List<Pair<String, Long>>,
    onColorSelected: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = GoogleSansFlex,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            swatches.forEach { (name, hex) ->
                val isSelected = selectedColorHex == hex
                val color = Color(hex)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                            .clickable { onColorSelected(hex) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = name,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = name,
                        fontFamily = GoogleSansFlex,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    valueBadge: String,
    subtext: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    minLabel: String,
    maxLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current value pill badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = valueBadge,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtext,
            fontFamily = GoogleSansFlex,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
            Text(
                text = maxLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontFamily = GoogleSansFlex,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun TonePresetButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .border(
                1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = GoogleSansFlex,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PreviewTxRow(
    icon: ImageVector,
    title: String,
    category: String,
    wallet: String,
    amount: String,
    amountColor: Color,
    tokens: com.omkarnub.kanri.ui.theme.KanriGlassTokens
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(tokens.tintFlat)
                    .border(tokens.borderWidth, tokens.borderStart, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tokens.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = tokens.textPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape((tokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                            .background(tokens.tintFlat)
                            .border(tokens.borderWidth * 0.8f, tokens.borderStart, RoundedCornerShape((tokens.cornerRadius * 0.35f).coerceIn(4.dp, 8.dp)))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = tokens.textSecondary
                        )
                    }
                    Text(
                        text = "• $wallet",
                        fontSize = 10.5.sp,
                        color = tokens.textTertiary
                    )
                }
            }
        }
        Text(
            text = amount,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            color = amountColor
        )
    }
}
