package com.omkarnub.kanri.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.theme.GlassCustomization
import com.omkarnub.kanri.ui.theme.GlassPresets
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.rememberKanriHaptics
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

    var config by remember(savedCustomization) { mutableStateOf(savedCustomization) }

    fun updateConfig(newConfig: GlassCustomization) {
        config = newConfig
        themePrefs.glassCustomization = newConfig
    }

    BackHandler {
        onBack()
    }

    val liveTokens = remember(config) { com.omkarnub.kanri.ui.theme.createCustomGlassTokens(config) }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalKanriGlass provides liveTokens
    ) {
        val tokens = LocalKanriGlass.current

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "CUSTOMISE GLASS COMPONENTS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = Panchang,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                fontSize = 13.sp
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive Real-Time Live Preview Card
                item {
                    LiveGlassPreviewCard(config = config)
                }

                item {
                    Text(
                        text = "Adjust the sliders below to fine-tune blur depth, translucency tint, specular edge reflections, and corner roundness.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }

                // 1. Frosted Blur
                item {
                    GlassSliderControlCard(
                        title = "Frosted blur",
                        subtitle = "Controls the depth of blur under glass cards",
                        valueText = "${config.blurRadiusDp.roundToInt()} dp",
                        icon = Icons.Default.BlurOn,
                        value = config.blurRadiusDp,
                        valueRange = 0f..50f,
                        config = config,
                        onValueChange = { newValue ->
                            haptics.tick()
                            updateConfig(config.copy(blurRadiusDp = newValue))
                        }
                    )
                }

                // 2. Glass Tint
                item {
                    GlassSliderControlCard(
                        title = "Glass tint",
                        subtitle = "Adjusts opacity and depth of translucent surfaces",
                        valueText = "${(config.surfaceOpacity * 100).roundToInt()}%",
                        icon = Icons.Default.Opacity,
                        value = config.surfaceOpacity,
                        valueRange = 0.02f..0.40f,
                        config = config,
                        onValueChange = { newValue ->
                            haptics.tick()
                            updateConfig(config.copy(surfaceOpacity = newValue))
                        }
                    )
                }

                // 3. Border & Edge Shine
                item {
                    GlassSliderControlCard(
                        title = "Border & edge shine",
                        subtitle = "Highlights card edges with specular luminance",
                        valueText = "${(config.borderOpacity * 100).roundToInt()}%",
                        icon = Icons.Default.BorderColor,
                        value = config.borderOpacity,
                        valueRange = 0.0f..0.70f,
                        config = config,
                        onValueChange = { newValue ->
                            haptics.tick()
                            val newWidth = if (newValue < 0.10f) 0.8f else 1.0f
                            updateConfig(config.copy(borderOpacity = newValue, borderWidthDp = newWidth))
                        }
                    )
                }

                // 4. Corner Curvature
                item {
                    GlassSliderControlCard(
                        title = "Corner curvature",
                        subtitle = "Sets the corner roundness of all glass components",
                        valueText = "${config.cornerRadiusDp.roundToInt()} dp",
                        icon = Icons.Default.AutoFixHigh,
                        value = config.cornerRadiusDp,
                        valueRange = 10f..32f,
                        config = config,
                        onValueChange = { newValue ->
                            haptics.tick()
                            updateConfig(config.copy(cornerRadiusDp = newValue))
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Reset to default button
                        OutlinedButton(
                            onClick = {
                                haptics.warning()
                                updateConfig(GlassPresets.DEFAULT)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset to default",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            )
                        }

                        // Save & Exit button
                        Button(
                            onClick = {
                                haptics.success()
                                updateConfig(config)
                                onBack()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = kanriPrimaryButtonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save & Exit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveGlassPreviewCard(config: GlassCustomization) {
    KanriGlassCard(
        level = GlassLevel.Blur,
        shape = RoundedCornerShape(config.cornerRadiusDp.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = "LIVE GLASS PREVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Panchang,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            fontSize = 11.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (config.blurRadiusDp <= 0f) "Clear Glass" else "Frosted ${config.blurRadiusDp.roundToInt()}dp",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "TOTAL BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = Panchang,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹ 2,45,800.00",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = Panchang,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassPreviewStatChip(
                    label = "Blur",
                    value = "${config.blurRadiusDp.roundToInt()} dp",
                    modifier = Modifier.weight(1f)
                )
                GlassPreviewStatChip(
                    label = "Tint",
                    value = "${(config.surfaceOpacity * 100).roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )
                GlassPreviewStatChip(
                    label = "Edge",
                    value = "${(config.borderOpacity * 100).roundToInt()}%",
                    modifier = Modifier.weight(1f)
                )
                GlassPreviewStatChip(
                    label = "Radius",
                    value = "${config.cornerRadiusDp.roundToInt()} dp",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GlassPreviewStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun GlassSliderControlCard(
    title: String,
    subtitle: String,
    valueText: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    config: GlassCustomization,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val cardContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = GoogleSansFlex
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )
        }
    }

    if (tokens != null) {
        KanriGlassCard(
            level = GlassLevel.Blur,
            shape = RoundedCornerShape(config.cornerRadiusDp.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            cardContent()
        }
    } else {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = modifier.fillMaxWidth()
        ) {
            cardContent()
        }
    }
}
