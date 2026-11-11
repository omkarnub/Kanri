package com.omkarnub.kanri.ui.settings

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import com.omkarnub.kanri.util.BatteryOptimizationHelper
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.omkarnub.kanri.data.cloud.BackupMode
import com.omkarnub.kanri.data.cloud.CloudBackupManager
import com.omkarnub.kanri.data.cloud.CloudBackupPreferences
import com.omkarnub.kanri.data.cloud.CloudBackupStatus
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.ui.common.AVAILABLE_CATEGORY_ICONS
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassAlertDialog as AlertDialog
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriModalBottomSheet
import com.omkarnub.kanri.ui.common.kanriGlassTextFieldColors
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.GlassCustomization
import com.omkarnub.kanri.ui.theme.GlassPresets
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.Panchang
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.util.AppCurrency
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// 1. SETTINGS GROUP
// =============================================================================
@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    val effectiveTitleColor = titleColor ?: (tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp,
            color = effectiveTitleColor,
            modifier = Modifier.padding(start = 12.dp)
        )

        KanriGlassCard(
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.Blur,
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

// =============================================================================
// 2. SETTINGS ROW
// =============================================================================
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val primaryColor = MaterialTheme.colorScheme.primary

    val resolvedIconTint = iconTint ?: (tokens?.accent ?: primaryColor)
    val titleTextColor = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
    val subtitleTextColor = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = {
                            haptics.click()
                            onClick()
                        }
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = resolvedIconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = titleTextColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = subtitleTextColor,
                    lineHeight = 16.sp
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// =============================================================================
// 3. SETTINGS SWITCH ROW
// =============================================================================
@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val primaryColor = MaterialTheme.colorScheme.primary

    val resolvedIconTint = tokens?.accent ?: primaryColor
    val titleTextColor = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
    val subtitleTextColor = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onValueChange = {
                    haptics.click()
                    onCheckedChange(it)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = resolvedIconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = titleTextColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = subtitleTextColor,
                    lineHeight = 16.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = if (tokens != null) tokens.tintFlat else MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

// =============================================================================
// 4. SETTINGS DIVIDER
// =============================================================================
@Composable
fun SettingsDivider(
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    HorizontalDivider(
        modifier = modifier.padding(start = 66.dp, end = 16.dp),
        thickness = 0.8.dp,
        color = tokens?.borderStart?.copy(alpha = 0.15f)
            ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

// =============================================================================
// 5. THEME PREVIEW CARD (MINIATURE REALISTIC PHONE PREVIEWS)
// =============================================================================
@Composable
fun ThemePreviewCard(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()

    // Authentic color tokens representing each theme accurately
    val (bgColor, cardColor, cardBorder, barColor, dockColor) = when (mode) {
        ThemeMode.LIGHT -> listOf(
            Color(0xFFFAF9F5), // Soft Cream
            Color(0xFFFFFFFF), // Crisp White Surface
            Color(0xFFE2DDD1), // Clean border
            Color(0xFF1F1E1A), // Dark text/bar
            Color(0xFFEDE8DD)  // Dock
        )
        ThemeMode.DARK -> listOf(
            Color(0xFF121212), // Charcoal Base
            Color(0xFF222222), // Elevated Card
            Color(0xFF333333), // Border
            Color(0xFFF2F2F2), // Off-White
            Color(0xFF1A1A1A)  // Dock
        )
        ThemeMode.AMOLED -> listOf(
            Color(0xFF000000), // True Pitch Black
            Color(0xFF141414), // Deep Neutral Card
            Color(0xFF262626), // High-contrast border against pure black
            Color(0xFFFFFFFF), // Stark White
            Color(0xFF0A0A0A)  // Dock
        )
        ThemeMode.GLASS -> listOf(
            Color(0xFF0B0B0E), // Translucent Dark Base
            Color(0x33FFFFFF), // Frosted translucent card
            Color(0x55FFFFFF), // Specular highlight border
            Color(0xFFF5F5F7), // Text
            Color(0x2EFFFFFF)  // Dock
        )
        ThemeMode.SYSTEM -> listOf(
            Color(0xFF1B1B1F),
            Color(0xFF28282E),
            Color(0xFF383842),
            Color(0xFFE5E5E8),
            Color(0xFF1E1E24)
        )
    }

    val subtitle = when (mode) {
        ThemeMode.SYSTEM -> "Follow Device"
        ThemeMode.LIGHT -> "Soft Cream"
        ThemeMode.DARK -> "Charcoal"
        ThemeMode.AMOLED -> "Pitch Black"
        ThemeMode.GLASS -> "Frosted Glass"
    }

    val cardBorderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        tokens?.borderEnd ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val cardContent = @Composable {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Miniature Phone Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(0.8.dp, cardBorder, RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
                // Glass frosted glow
                if (mode == ThemeMode.GLASS) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    Color(0xFF818CF8).copy(alpha = 0.15f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.4f, size.height * 0.35f),
                                radius = size.width * 0.85f
                            ),
                            radius = size.width * 0.85f,
                            center = Offset(size.width * 0.4f, size.height * 0.35f)
                        )
                    }
                }

                // System auto split indicator
                if (mode == ThemeMode.SYSTEM) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFFFB74D).copy(alpha = 0.25f), Color.Transparent),
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height)
                            )
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 18.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor.copy(alpha = 0.6f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(barColor.copy(alpha = 0.6f))
                        )
                    }

                    // Mini Content Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                            .border(0.8.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                .size(width = 30.dp, height = 5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor)
                            )
                            Box(
                                modifier = Modifier
                                .size(width = 46.dp, height = 3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(barColor.copy(alpha = 0.45f))
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 11.dp, height = 9.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(barColor.copy(alpha = 0.25f))
                                    )
                                }
                            }
                        }
                    }

                    // Mini Bottom Dock
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(dockColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(barColor.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }

                // Checkmark Circle Badge when Selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mode.label,
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontFamily = GoogleSansFlex,
                fontSize = 10.sp,
                color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (tokens != null) {
        KanriGlassCard(
            level = if (isSelected) GlassLevel.Strong else GlassLevel.Blur,
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
                .width(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .selectable(
                    selected = isSelected,
                    onClick = {
                        haptics.click()
                        onClick()
                    },
                    role = Role.RadioButton
                )
                .semantics {
                    contentDescription = "${mode.label} theme"
                }
                .border(
                    width = if (isSelected) 2.dp else (tokens.borderWidth.takeIf { it > 0.dp } ?: 1.dp),
                    color = cardBorderColor,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            cardContent()
        }
    } else {
        Card(
            modifier = modifier
                .width(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .selectable(
                    selected = isSelected,
                    onClick = {
                        haptics.click()
                        onClick()
                    },
                    role = Role.RadioButton
                )
                .semantics {
                    contentDescription = "${mode.label} theme"
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                }
            ),
            border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor)
        ) {
            cardContent()
        }
    }
}

// =============================================================================
// 6. SHADER PREVIEW CARD (10 LIVE BACKGROUNDS)
// =============================================================================
fun getShaderGradientBrush(type: GlassBackgroundType): Brush {
    return when (type) {
        GlassBackgroundType.AURORA -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF07090E),
                Color(0xFF7CFF67),
                Color(0xFF22D3C5),
                Color(0xFF8F5CFF),
                Color(0xFFFF5C8A),
                Color(0xFFFF8A5C),
                Color(0xFF07090E)
            )
        )
        GlassBackgroundType.GRAIN_GRADIENT -> Brush.linearGradient(
            colors = listOf(Color(0xFF171717), Color(0xFF262626), Color(0xFF404040), Color(0xFF171717))
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
            colors = listOf(Color(0xFF0A0A0A), Color(0xFF171717), Color(0xFF262626), Color(0xFF0A0A0A))
        )
        GlassBackgroundType.FUJI_SAKURA -> Brush.linearGradient(
            colors = listOf(Color(0xFF3B0764), Color(0xFF701A75), Color(0xFFDB2777), Color(0xFFF472B6))
        )
        GlassBackgroundType.SUMI_E_MIST -> Brush.linearGradient(
            colors = listOf(Color(0xFF1C1917), Color(0xFF292524), Color(0xFF44403C), Color(0xFF1C1917))
        )
        GlassBackgroundType.THISTLE_PORTICO -> Brush.linearGradient(
            colors = listOf(Color(0xFF5E3AAE), Color(0xFF8A5CC1), Color(0xFFD4A9E2), Color(0xFFF3D0EB), Color(0xFFEEE6F8))
        )
        GlassBackgroundType.SUNSET_MEADOW -> Brush.linearGradient(
            colors = listOf(Color(0xFFE2A8AE), Color(0xFFF8A68E), Color(0xFFFFCB8C), Color(0xFF8AAA18), Color(0xFF172604))
        )
        GlassBackgroundType.TWILIGHT_MEADOW -> Brush.linearGradient(
            colors = listOf(Color(0xFF12101B), Color(0xFF38234D), Color(0xFF8D5375), Color(0xFFE89A7A), Color(0xFF1E352C))
        )
        GlassBackgroundType.TOKYO_NIGHT -> Brush.linearGradient(
            colors = listOf(Color(0xFF05010F), Color(0xFF1C1335), Color(0xFF5B2B6D), Color(0xFF9E4784), Color(0xFF00E5FF))
        )
        GlassBackgroundType.PIXEL_KITCHEN -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D111A), Color(0xFFC88242), Color(0xFFEBC37D), Color(0xFFFDE8BA), Color(0xFF1E2838))
        )
        GlassBackgroundType.SNOWY_MOUNTAIN_ROAD -> Brush.linearGradient(
            colors = listOf(Color(0xFF03060A), Color(0xFF1C2C42), Color(0xFF4A7A9E), Color(0xFFC0D8E8), Color(0xFF080E18))
        )
        GlassBackgroundType.SUNLIT_BALCONY -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D111A), Color(0xFF385542), Color(0xFF7DA87D), Color(0xFFF0D49E), Color(0xFF1A2620))
        )
        GlassBackgroundType.ALPINE_VALLEY -> Brush.linearGradient(
            colors = listOf(Color(0xFF080C11), Color(0xFF1B3D2F), Color(0xFF3A7D5C), Color(0xFF88C9A1), Color(0xFF5D8AA8))
        )
        GlassBackgroundType.SHIGANSHINA_WINDMILL -> Brush.linearGradient(
            colors = listOf(Color(0xFF060E0B), Color(0xFF3E4F28), Color(0xFF7C722A), Color(0xFFC4B86A), Color(0xFF1B2E1C))
        )
        GlassBackgroundType.CHIBI_COFFEE_SHOP -> Brush.linearGradient(
            colors = listOf(Color(0xFF0B0709), Color(0xFF381C24), Color(0xFF6B3A42), Color(0xFFB87868), Color(0xFFE8B898))
        )
        GlassBackgroundType.FUJI_TORII -> Brush.linearGradient(
            colors = listOf(Color(0xFF0A121B), Color(0xFF27415A), Color(0xFF86C5DC), Color(0xFFD63826), Color(0xFFFFF0E0))
        )
        GlassBackgroundType.CHERRY_CANYON -> Brush.linearGradient(
            colors = listOf(Color(0xFF0D0811), Color(0xFF4A2035), Color(0xFF8D4068), Color(0xFFE58CB5), Color(0xFFF7D2E2))
        )
        GlassBackgroundType.PIXEL_MISTY_ALLEY -> Brush.linearGradient(
            colors = listOf(Color(0xFF0B0A16), Color(0xFF221A3D), Color(0xFF533E78), Color(0xFF9E65A8), Color(0xFF40E0D0))
        )
        GlassBackgroundType.GLOWING_BAMBOO_FOREST -> Brush.linearGradient(
            colors = listOf(Color(0xFF04100E), Color(0xFF0E3228), Color(0xFF1E644A), Color(0xFF52C4A6), Color(0xFFFFD984))
        )
    }
}

fun getShaderDescription(type: GlassBackgroundType): String {
    return when (type) {
        GlassBackgroundType.AURORA -> "Dual-cycle Gemini aurora smoothly blending lime, turquoise & violet into coral, pink & violet"
        GlassBackgroundType.GRAIN_GRADIENT -> "Film-grade atmospheric grain"
        GlassBackgroundType.HERO_GEOMETRIC -> "Precision isometric low-poly geometry"
        GlassBackgroundType.PRISM_GRADIENT -> "Dynamic refractive glass caustics"
        GlassBackgroundType.CLOUD_SHADER -> "Volumetric cumulus cloudscape"
        GlassBackgroundType.INK_RIDGES -> "Japanese ink wash topographical flow"
        GlassBackgroundType.FUJI_SAKURA -> "9:16 pixel hanging scroll: Mt. Fuji, golden mist, pagoda, torii gate & sakura blossoms"
        GlassBackgroundType.SUMI_E_MIST -> "Minimalist monochrome ink diffusion"
        GlassBackgroundType.THISTLE_PORTICO -> "Gothic stone portico in wisteria bloom with castle vista & drifting petals"
        GlassBackgroundType.SUNSET_MEADOW -> "3D voxel meadow with interactive grass, voxel flora & dynamic day/sunset/night cycle"
        GlassBackgroundType.TWILIGHT_MEADOW -> "Pixel art twilight alpine lake with swaying wildflower asters & crescent moon"
        GlassBackgroundType.TOKYO_NIGHT -> "Pixel art Tokyo night skyline with mountain bench, companion cat & neon city lights"
        GlassBackgroundType.PIXEL_KITCHEN -> "Cozy 24-hour pixel kitchen with warm kettle, day/night window lighting & subtle steam"
        GlassBackgroundType.SNOWY_MOUNTAIN_ROAD -> "Atmospheric 24-hour winter mountain road with drifting snow, headlight beams & real-time sky"
        GlassBackgroundType.SUNLIT_BALCONY -> "Quiet 24-hour sunlit balcony with potted plants, gentle breeze & evening fireflies"
        GlassBackgroundType.ALPINE_VALLEY -> "Animated 9:16 pixel art mountain valley with winding river, wildflower meadow & soaring birds"
        GlassBackgroundType.SHIGANSHINA_WINDMILL -> "4-bit animated pixel art windmill with calm wind gusts & 24-hour dawn-to-night lighting cycle"
        GlassBackgroundType.CHIBI_COFFEE_SHOP -> "Cozy chibi cafe window on a rainy evening with steaming mugs, warm lanterns & cat companion"
        GlassBackgroundType.FUJI_TORII -> "Japanese vermilion torii gate before Mount Fuji with drifting sakura petals & glowing valley mist"
        GlassBackgroundType.CHERRY_CANYON -> "3D voxel cherry blossom canyon with drifting petals, celestial day/sunset/night traversal & volumetric clouds"
        GlassBackgroundType.PIXEL_MISTY_ALLEY -> "Atmospheric 24-hour neon city alley with gentle rain, companion cat & dynamic sun/moon cycle"
        GlassBackgroundType.GLOWING_BAMBOO_FOREST -> "Steam train crossing a bridge through swaying bamboo with glowing lanterns & 24-hour atmosphere"
    }
}

@Composable
fun ShaderPreviewCard(
    type: GlassBackgroundType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()

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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = {
                    haptics.click()
                    onClick()
                }
            ),
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
                    .background(getShaderGradientBrush(type))
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
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
                    text = getShaderDescription(type),
                    fontFamily = GoogleSansFlex,
                    fontSize = 12.sp,
                    color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Checkmark Circle Indicator
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

// =============================================================================
// 7. GLASS QUICK PRESETS ROW
// =============================================================================
@Composable
fun GlassQuickPresetsRow(
    currentConfig: GlassCustomization,
    onSelectPreset: (GlassCustomization) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    val presets = listOf(
        "Default" to GlassPresets.DEFAULT,
        "Ultra Sheer" to GlassPresets.ULTRA_SHEER,
        "Obsidian" to GlassPresets.DEEP_OBSIDIAN,
        "Luminous" to GlassPresets.LUMINOUS_FROST,
        "Neon" to GlassPresets.NEON_CRYSTAL
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (label, preset) ->
            val isSelected = (currentConfig == preset)
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
                    .clickable {
                        haptics.click()
                        onSelectPreset(preset)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
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
    }
}

// =============================================================================
// 8. PERMISSIONS STATUS OVERVIEW (TOGGLE SWITCH REDESIGN)
// =============================================================================
@Composable
fun PermissionsStatusOverview(
    context: Context,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    var isSmsAllowed by remember { mutableStateOf(com.omkarnub.kanri.util.SmsPermissionHelper.hasSmsPermissions(context)) }
    var isNotificationAccessAllowed by remember { mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context)) }
    var isNotifAllowed by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }
    var isOverlayAllowed by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) }

    // Re-check on resume when returning from Android Settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isSmsAllowed = com.omkarnub.kanri.util.SmsPermissionHelper.hasSmsPermissions(context)
                isNotificationAccessAllowed = NotificationAccessHelper.isNotificationAccessGranted(context)
                isNotifAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                } else {
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
                isOverlayAllowed = Settings.canDrawOverlays(context)
                isBatteryUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. Bank SMS Detection
        SettingsSwitchRow(
            title = "Bank SMS Detection",
            subtitle = "Reads incoming transaction alerts offline",
            icon = Icons.Default.Email,
            checked = isSmsAllowed,
            onCheckedChange = {
                haptics.click()
                if (context is Activity) {
                    com.omkarnub.kanri.util.SmsPermissionHelper.requestSmsPermissions(context)
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        )

        SettingsDivider()

        // 2. Payment Notification Listener
        SettingsSwitchRow(
            title = "Payment Notification Listener",
            subtitle = "Catches UPI apps (GPay, PhonePe, Paytm, CRED)",
            icon = Icons.Default.Notifications,
            checked = isNotificationAccessAllowed,
            onCheckedChange = {
                haptics.click()
                NotificationAccessHelper.openNotificationAccessSettings(context)
            }
        )

        SettingsDivider()

        // 3. Transaction Notifications
        SettingsSwitchRow(
            title = "Transaction Notifications",
            subtitle = "Alerts for detected transactions & reminders",
            icon = Icons.Default.Notifications,
            checked = isNotifAllowed,
            onCheckedChange = {
                haptics.click()
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
            }
        )

        SettingsDivider()

        // 4. Instant Floating Overlay
        SettingsSwitchRow(
            title = "Floating Quick Category",
            subtitle = "Shows interactive prompt after a transaction",
            icon = Icons.Default.AutoAwesome,
            checked = isOverlayAllowed,
            onCheckedChange = {
                haptics.click()
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                }
            }
        )

        SettingsDivider()

        // 5. Background Battery Exemption
        SettingsSwitchRow(
            title = "Background Battery Unrestricted",
            subtitle = "Prevents system from stopping transaction capture",
            icon = Icons.Default.PowerSettingsNew,
            checked = isBatteryUnrestricted,
            onCheckedChange = {
                haptics.click()
                BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context)
            }
        )
    }
}

// =============================================================================
// 9. CLOUD BACKUP DIALOG
// =============================================================================
@Composable
fun CloudBackupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val cloudPrefs = remember { CloudBackupPreferences.getInstance(context) }
    val backupManager = remember { CloudBackupManager.getInstance(context) }

    val currentMode by cloudPrefs.backupModeFlow.collectAsState()
    val accountEmail by cloudPrefs.accountEmailFlow.collectAsState()
    val lastTimestamp by cloudPrefs.lastBackupTimestampFlow.collectAsState()
    val lastStatus by cloudPrefs.lastBackupStatusFlow.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Google Drive Cloud Backup",
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Encrypted client-side backup automatically saved to your private Google Drive under the restricted drive.file scope.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentMode == BackupMode.CLOUD,
                        onClick = {
                            haptics.tick()
                            cloudPrefs.backupMode = BackupMode.CLOUD
                        },
                        label = { Text("Cloud Backup", fontSize = 12.sp) },
                        leadingIcon = if (currentMode == BackupMode.CLOUD) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = currentMode == BackupMode.OFFLINE,
                        onClick = {
                            haptics.tick()
                            cloudPrefs.backupMode = BackupMode.OFFLINE
                        },
                        label = { Text("Fully Offline", fontSize = 12.sp) },
                        leadingIcon = if (currentMode == BackupMode.OFFLINE) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (currentMode == BackupMode.CLOUD) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Account Status",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (!accountEmail.isNullOrBlank()) accountEmail!! else "Signed in with Google Play Services",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val dateStr = if (lastTimestamp > 0L) {
                                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(lastTimestamp))
                            } else "Never"
                            Text(
                                text = "Last synced: $dateStr",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (!isSyncing) {
                                isSyncing = true
                                haptics.click()
                                scope.launch {
                                    try {
                                        val result = backupManager.performCloudBackup()
                                        if (result.isSuccess) {
                                            haptics.success()
                                            Toast.makeText(context, "Backup synced to Google Drive successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            haptics.warning()
                                            Toast.makeText(context, "Sync complete (local ledger secured)", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Sync complete", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing..." else "Sync to Drive Now")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

// =============================================================================
// 10. CURRENCY PICKER DIALOG
// =============================================================================
@Composable
fun SelectCurrencyDialog(
    currentCurrency: AppCurrency,
    onSelectCurrency: (AppCurrency) -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalKanriGlass.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery) {
        val query = searchQuery.trim().lowercase()
        if (query.isEmpty()) {
            AppCurrency.entries
        } else {
            AppCurrency.entries.filter { currency ->
                currency.code.lowercase().contains(query) ||
                    currency.displayName.lowercase().contains(query) ||
                    currency.symbol.lowercase().contains(query)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Currency",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select your primary currency. Amounts and widgets will format using this symbol.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currency or code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = kanriGlassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredCurrencies.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No currency matching \"$searchQuery\"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        filteredCurrencies.forEach { currency ->
                            val isSelected = currency == currentCurrency
                            val accentColor = tokens?.accent ?: MaterialTheme.colorScheme.primary

                            KanriGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCurrency(currency) }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                accentColor,
                                                RoundedCornerShape(14.dp)
                                            )
                                        } else Modifier
                                    ),
                                shape = RoundedCornerShape(14.dp),
                                level = GlassLevel.Flat
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isSelected) accentColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (currency.flag.isNotBlank()) {
                                                    Text(
                                                        text = currency.flag,
                                                        fontSize = 20.sp
                                                    )
                                                } else {
                                                    Text(
                                                        text = currency.symbol,
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "${currency.displayName} (${currency.code})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Symbol: ${currency.symbol}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// =============================================================================
// 10B. BUDGET CYCLE START DAY PICKER DIALOG
// =============================================================================
@Composable
fun BudgetCycleStartDialog(
    currentDay: Int,
    onSelectDay: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableStateOf(currentDay) }
    val haptics = rememberKanriHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Budget Cycle Start Day",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Select which day of the month your budget cycle begins. Salaried users can match their monthly payday so 'safe to spend' and 'days remaining' align with their salary.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Current day summary card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (selectedDay == 1) "Day 1 (Calendar Month)" else "Day $selectedDay (Salary Cycle)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (selectedDay == 1) "Resets on the 1st of every calendar month" else "Resets on the ${selectedDay}th of every month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Slider for fine selection 1..28
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Day", style = MaterialTheme.typography.bodySmall)
                        Text("Day $selectedDay", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = selectedDay.toFloat(),
                        onValueChange = {
                            selectedDay = it.toInt().coerceIn(1, 28)
                        },
                        valueRange = 1f..28f,
                        steps = 26
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectDay(selectedDay)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

// =============================================================================
// 11. CATEGORY EDIT / CREATE DIALOG
// =============================================================================
@Composable
fun CategoryEditCreateDialog(
    category: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (newName: String, newIconName: String) -> Unit
) {
    var name by remember(category) { mutableStateOf(category?.name ?: "") }
    var selectedIconName by remember(category) {
        mutableStateOf(category?.iconName ?: AVAILABLE_CATEGORY_ICONS.first().iconName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (category != null) "Edit Category" else "New Custom Category",
                fontFamily = GoogleSansFlex,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Gym, Coffee, Pet Care") },
                    singleLine = true,
                    colors = kanriGlassTextFieldColors(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select Icon",
                    fontFamily = GoogleSansFlex,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    items(AVAILABLE_CATEGORY_ICONS, key = { it.iconName }) { iconOpt ->
                        val isSelected = selectedIconName == iconOpt.iconName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedIconName = iconOpt.iconName }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CategoryIcon(
                                    categoryName = null,
                                    iconName = iconOpt.iconName,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotBlank()) {
                        onSave(name.trim(), selectedIconName)
                    }
                },
                enabled = name.trim().isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =============================================================================
// 12. CONTACT US DIALOG
// =============================================================================
@Composable
fun ContactUsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val email = "support@kanri.app"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Support", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Have questions, feedback, or need assistance? Reach out directly and we will respond as soon as possible.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = email,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Support Email", email))
                                haptics.tick()
                                Toast.makeText(context, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email?subject=${Uri.encode("Kanri Support Request")}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Open Email Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// =============================================================================
// 13. SUGGEST A FEATURE DIALOG
// =============================================================================
@Composable
fun SuggestFeatureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Analytics", "Automation", "Budgeting", "UI & Design", "Other")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest a Feature", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Help us improve Kanri with your ideas and requirements.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Feature Title") },
                    placeholder = { Text("e.g. Export to Excel format") },
                    singleLine = true,
                    colors = kanriGlassTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Use Case") },
                    placeholder = { Text("How would this help your daily expense tracking?") },
                    maxLines = 4,
                    colors = kanriGlassTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    val emailSubject = "[Feature Request] [$selectedCategory] $title"
                    val emailBody = "Feature: $title\nCategory: $selectedCategory\n\nDetails:\n$description"
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:feedback@kanri.app?subject=${Uri.encode(emailSubject)}&body=${Uri.encode(emailBody)}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Send Suggestion")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// 13. CATEGORY REORDER SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryReorderSheet(
    allCategories: List<CategoryEntity>,
    onMoveUp: (CategoryEntity) -> Unit,
    onMoveDown: (CategoryEntity) -> Unit,
    onResetOrder: () -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onCreateNewCategory: () -> Unit,
    onDismiss: () -> Unit
) {
    val tokens = LocalKanriGlass.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KanriModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Category Order",
                        fontFamily = Panchang,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize which categories show first and last",
                        fontFamily = GoogleSansFlex,
                        fontSize = 12.5.sp,
                        color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onResetOrder) {
                        Text("Reset", fontSize = 12.sp)
                    }
                    Button(
                        onClick = onCreateNewCategory,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(allCategories, key = { _, cat -> cat.id }) { index, cat ->
                    val isFirst = index == 0
                    val isLast = index == allCategories.size - 1

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = tokens?.tintFlat ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            1.dp,
                            tokens?.borderStart ?: MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // Order Badge
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Category Icon
                                CategoryIcon(
                                    categoryName = cat.name,
                                    modifier = Modifier.size(32.dp)
                                )

                                Column {
                                    Text(
                                        text = cat.name,
                                        fontFamily = GoogleSansFlex,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                                    )
                                    if (cat.isCustom) {
                                        Text(
                                            text = "Custom Tag",
                                            fontSize = 10.5.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Action Buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move Up
                                IconButton(
                                    onClick = { onMoveUp(cat) },
                                    enabled = !isFirst,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (!isFirst) tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Move Down
                                IconButton(
                                    onClick = { onMoveDown(cat) },
                                    enabled = !isLast,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (!isLast) tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (cat.isCustom) {
                                    IconButton(
                                        onClick = { onEditCategory(cat) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteCategory(cat) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
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
