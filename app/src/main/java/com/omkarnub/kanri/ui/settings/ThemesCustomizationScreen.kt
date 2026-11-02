package com.omkarnub.kanri.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.LocalHazeState
import com.omkarnub.kanri.ui.theme.AppIconManager
import com.omkarnub.kanri.ui.theme.AppIconStyle
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.theme.isAppInDarkTheme
import com.omkarnub.kanri.util.rememberKanriHaptics

/**
 * Visual gradient palette representations for the 8 Live Shaders
 */
private fun getShaderGradient(type: GlassBackgroundType): Brush {
    return when (type) {
        GlassBackgroundType.AURORA -> Brush.linearGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF064E3B), Color(0xFF0F172A))
        )
        GlassBackgroundType.GRAIN_GRADIENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF18181B), Color(0xFF27272A), Color(0xFF3F3F46), Color(0xFF18181B))
        )
        GlassBackgroundType.HERO_GEOMETRIC -> Brush.linearGradient(
            colors = listOf(Color(0xFF0B132B), Color(0xFF1C2541), Color(0xFF3A506B), Color(0xFF0B132B))
        )
        GlassBackgroundType.PRISM_GRADIENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95), Color(0xFF831843), Color(0xFF1E1B4B))
        )
        GlassBackgroundType.CLOUD_SHADER -> Brush.linearGradient(
            colors = listOf(Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF075985))
        )
        GlassBackgroundType.INK_RIDGES -> Brush.linearGradient(
            colors = listOf(Color(0xFF09090B), Color(0xFF18181B), Color(0xFF27272A), Color(0xFF09090B))
        )
        GlassBackgroundType.FUJI_SAKURA -> Brush.linearGradient(
            colors = listOf(Color(0xFF3B0764), Color(0xFF701A75), Color(0xFFDB2777), Color(0xFFF472B6))
        )
        GlassBackgroundType.SUMI_E_MIST -> Brush.linearGradient(
            colors = listOf(Color(0xFF1C1917), Color(0xFF292524), Color(0xFF44403C), Color(0xFF1C1917))
        )
    }
}

private fun getShaderSubtitle(type: GlassBackgroundType): String {
    return when (type) {
        GlassBackgroundType.AURORA -> "Fluid dual-frequency iridescent waves"
        GlassBackgroundType.GRAIN_GRADIENT -> "Film-grade atmospheric grain"
        GlassBackgroundType.HERO_GEOMETRIC -> "Precision isometric low-poly geometry"
        GlassBackgroundType.PRISM_GRADIENT -> "Dynamic refractive glass caustics"
        GlassBackgroundType.CLOUD_SHADER -> "Volumetric cumulus cloudscape"
        GlassBackgroundType.INK_RIDGES -> "Japanese ink wash topographical flow"
        GlassBackgroundType.FUJI_SAKURA -> "Ethereal sakura dawn over Mt. Fuji"
        GlassBackgroundType.SUMI_E_MIST -> "Minimalist monochrome ink diffusion"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesCustomizationScreen(
    onBack: () -> Unit,
    onOpenGlassCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val hazeState = LocalHazeState.current
    val tokens = LocalKanriGlass.current

    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val currentGlassBg by themePrefs.glassBackgroundFlow.collectAsState()
    val isDynamicEnabled by themePrefs.dynamicColorFlow.collectAsState()
    val currentIconStyle by themePrefs.appIconStyleFlow.collectAsState()

    var showIconRestartDialog by remember { mutableStateOf(false) }
    var pendingIconStyle by remember { mutableStateOf<AppIconStyle?>(null) }

    BackHandler { onBack() }

    // Icon Restart Dialog
    if (showIconRestartDialog && pendingIconStyle != null) {
        val target = pendingIconStyle!!
        com.omkarnub.kanri.ui.common.KanriGlassAlertDialog(
            onDismissRequest = {
                showIconRestartDialog = false
                pendingIconStyle = null
            },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restart App?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Switching launcher icon to ${target.label} requires restarting Kanri so your home screen updates.\n\nRestart now?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIconRestartDialog = false
                        themePrefs.appIconStyle = target
                        AppIconManager.restartApp(context, target, currentThemeMode)
                    }
                ) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showIconRestartDialog = false
                        pendingIconStyle = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "THEMES & CUSTOMIZATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Panchang,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 13.5.sp
                            ),
                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live Shaders & System Aesthetics",
                            fontSize = 11.5.sp,
                            fontFamily = GoogleSansFlex,
                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.click()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.warning()
                            themePrefs.themeMode = ThemeMode.SYSTEM
                            themePrefs.glassBackground = GlassBackgroundType.AURORA
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults",
                            tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =========================================================================
            // 1. LIVE PREVIEW HERO CARD
            // =========================================================================
            item {
                KanriGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    level = GlassLevel.Strong,
                    hazeState = hazeState
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(getShaderGradient(currentGlassBg))
                            .padding(16.dp)
                    ) {
                        // Frosted pill in preview
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Column {
                                    Text(
                                        text = currentGlassBg.displayName,
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Active Theme: ${currentThemeMode.label}",
                                        fontSize = 11.5.sp,
                                        fontFamily = GoogleSansFlex,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. THEME MODE SELECTION
            // =========================================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "THEME MODE",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            Triple(ThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
                            Triple(ThemeMode.DARK, "Dark", Icons.Default.DarkMode),
                            Triple(ThemeMode.AMOLED, "AMOLED", Icons.Default.NightlightRound),
                            Triple(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                            Triple(ThemeMode.GLASS, "Glass", Icons.Default.Opacity)
                        ).forEach { (mode, title, icon) ->
                            val isSelected = (currentThemeMode == mode)
                            ThemeModeCard(
                                title = title,
                                icon = icon,
                                isSelected = isSelected,
                                onClick = {
                                    haptics.click()
                                    themePrefs.themeMode = mode
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. LIVE SHADER BACKGROUNDS (8 PRESETS)
            // =========================================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LIVE SHADER BACKGROUNDS",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentThemeMode != ThemeMode.GLASS) {
                            Text(
                                text = "Active in Glass Mode",
                                fontFamily = GoogleSansFlex,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassBackgroundType.entries.forEach { bgType ->
                            val isSelected = (currentGlassBg == bgType)
                            ShaderOptionCard(
                                type = bgType,
                                isSelected = isSelected,
                                onClick = {
                                    haptics.click()
                                    themePrefs.glassBackground = bgType
                                    if (currentThemeMode != ThemeMode.GLASS) {
                                        themePrefs.themeMode = ThemeMode.GLASS
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 4. DYNAMIC SYSTEM COLORS & GLASS FINE-TUNING
            // =========================================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SURFACE & ENGINE CUSTOMIZATIONS",
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    KanriGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        level = GlassLevel.Blur,
                        hazeState = hazeState
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Dynamic Colors Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.tick()
                                        themePrefs.isDynamicColor = !isDynamicEnabled
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Dynamic Colors",
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Extract tint palette from system wallpaper",
                                            fontSize = 12.sp,
                                            fontFamily = GoogleSansFlex,
                                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isDynamicEnabled,
                                    onCheckedChange = {
                                        haptics.tick()
                                        themePrefs.isDynamicColor = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = tokens?.borderStart ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Glass Customizer Screen Launcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.click()
                                        onOpenGlassCustomizer()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BlurOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Glass Customizer Engine",
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Fine-tune blur radius, noise, border opacity & specular light",
                                            fontSize = 12.sp,
                                            fontFamily = GoogleSansFlex,
                                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(
                                thickness = 0.6.dp,
                                color = tokens?.borderStart ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // App Launcher Icon
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.click()
                                        val nextStyle = if (currentIconStyle == AppIconStyle.DARK) AppIconStyle.LIGHT else AppIconStyle.DARK
                                        pendingIconStyle = nextStyle
                                        showIconRestartDialog = true
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ColorLens,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "App Launcher Icon",
                                            fontFamily = GoogleSansFlex,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Currently using ${currentIconStyle.label} icon",
                                            fontSize = 12.sp,
                                            fontFamily = GoogleSansFlex,
                                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = currentIconStyle.label,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShaderOptionCard(
    type: GlassBackgroundType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.01f else 1f,
        animationSpec = spring(stiffness = 450f, dampingRatio = 0.8f),
        label = "shaderScale"
    )

    KanriGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Gradient Swatch Preview (46dp rounded)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getShaderGradient(type))
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Name & Description
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = type.displayName,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getShaderSubtitle(type),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Radio / Checkmark Badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(
                            1.5.dp,
                            tokens?.borderStart ?: MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current

    KanriGlassCard(
        modifier = modifier
            .width(96.dp)
            .height(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else (tokens?.tintFlat ?: MaterialTheme.colorScheme.surfaceContainerHighest)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else (tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) (tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface) else (tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

