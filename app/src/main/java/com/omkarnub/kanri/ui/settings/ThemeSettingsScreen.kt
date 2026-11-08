package com.omkarnub.kanri.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.rememberKanriHaptics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    onNavigateToGlassCustomizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val themePrefs = remember { ThemePreferences.getInstance(context) }

    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val activeShader by themePrefs.glassBackgroundFlow.collectAsState()

    BackHandler {
        onBack()
    }

    val tokens = LocalKanriGlass.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "THEMES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 15.sp
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Section 1: Theme Mode Selection
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SELECT THEME",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Choose your preferred interface theme. Settings apply instantly across the whole app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5 Theme Cards Row (Horizontal scrollable)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemePreviewCard(
                            mode = ThemeMode.SYSTEM,
                            isSelected = currentThemeMode == ThemeMode.SYSTEM,
                            onClick = { themePrefs.themeMode = ThemeMode.SYSTEM }
                        )
                        ThemePreviewCard(
                            mode = ThemeMode.LIGHT,
                            isSelected = currentThemeMode == ThemeMode.LIGHT,
                            onClick = { themePrefs.themeMode = ThemeMode.LIGHT }
                        )
                        ThemePreviewCard(
                            mode = ThemeMode.DARK,
                            isSelected = currentThemeMode == ThemeMode.DARK,
                            onClick = { themePrefs.themeMode = ThemeMode.DARK }
                        )
                        ThemePreviewCard(
                            mode = ThemeMode.AMOLED,
                            isSelected = currentThemeMode == ThemeMode.AMOLED,
                            onClick = { themePrefs.themeMode = ThemeMode.AMOLED }
                        )
                        ThemePreviewCard(
                            mode = ThemeMode.GLASS,
                            isSelected = currentThemeMode == ThemeMode.GLASS,
                            onClick = { themePrefs.themeMode = ThemeMode.GLASS }
                        )
                    }
                }
            }

            // Section 2: Glass Theme Options (Live Shaders Side-by-Side + Glass Customiser)
            item {
                AnimatedVisibility(
                    visible = currentThemeMode == ThemeMode.GLASS,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            thickness = 1.dp
                        )

                        // Customise Glass Components entry button
                        CustomiseGlassRow(
                            onClick = {
                                haptics.click()
                                onNavigateToGlassCustomizer()
                            }
                        )

                        // Live Shader Backgrounds Header
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "LIVE SHADER BACKGROUNDS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = Panchang,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Select an animated WebGL background rendered live beneath translucent frosted glass.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Shader backgrounds list view (single column, no previews, Apply button beside each)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassBackgroundType.entries.forEach { shaderType ->
                                ShaderListItem(
                                    type = shaderType,
                                    isSelected = activeShader == shaderType,
                                    onApply = { themePrefs.glassBackground = shaderType }
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
private fun CustomiseGlassRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val rowContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = onClick
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Customise glass components",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Adjust frosted blur, glass tint, edge shine, and corner curvature",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (tokens != null) {
        KanriGlassCard(
            level = GlassLevel.Blur,
            modifier = modifier.fillMaxWidth()
        ) {
            rowContent()
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = modifier.fillMaxWidth()
        ) {
            rowContent()
        }
    }
}

@Composable
private fun ShaderListItem(
    type: GlassBackgroundType,
    isSelected: Boolean,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    val tokens = LocalKanriGlass.current

    val itemContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = {
                        haptics.click()
                        onApply()
                    }
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = getShaderDescription(type),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Button(
                    onClick = {
                        haptics.click()
                        onApply()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Applied",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Applied",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        haptics.click()
                        onApply()
                    },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = "Apply",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }

    if (tokens != null) {
        KanriGlassCard(
            level = if (isSelected) GlassLevel.Strong else GlassLevel.Flat,
            shape = RoundedCornerShape(14.dp),
            modifier = modifier
                .fillMaxWidth()
                .border(
                    width = if (isSelected) 1.5.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            itemContent()
        }
    } else {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                if (isSelected) 1.5.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            ),
            modifier = modifier.fillMaxWidth()
        ) {
            itemContent()
        }
    }
}
