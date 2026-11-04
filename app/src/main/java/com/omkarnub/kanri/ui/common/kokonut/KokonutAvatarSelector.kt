package com.omkarnub.kanri.ui.common.kokonut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDialog
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch

data class PresetAvatar(
    val id: String,
    val label: String,
    val emoji: String,
    val gradientColors: List<Color>
)

val PRESET_AVATARS = listOf(
    PresetAvatar("amber", "Amber", "A", listOf(Color(0xFFFF9966), Color(0xFFFF5E62))),
    PresetAvatar("emerald", "Emerald", "E", listOf(Color(0xFF56AB2F), Color(0xFFA8E063))),
    PresetAvatar("solar", "Solar", "S", listOf(Color(0xFFF7971E), Color(0xFFFFD200))),
    PresetAvatar("ruby", "Ruby", "R", listOf(Color(0xFF8A2387), Color(0xFFE94057), Color(0xFFF27121))),
    PresetAvatar("cyan", "Cyan", "C", listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
    PresetAvatar("rose", "Rose", "K", listOf(Color(0xFFFF758C), Color(0xFFFF7EB3))),
    PresetAvatar("onyx", "Onyx", "O", listOf(Color(0xFF434343), Color(0xFF000000))),
    PresetAvatar("violet", "Violet", "V", listOf(Color(0xFF654EA3), Color(0xFFEAAFC8))),
    PresetAvatar("mint", "Mint", "M", listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
    PresetAvatar("azure", "Azure", "Z", listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))),
    PresetAvatar("coral", "Coral", "P", listOf(Color(0xFFFF0844), Color(0xFFFFB199))),
    PresetAvatar("bronze", "Bronze", "B", listOf(Color(0xFF8B5A2B), Color(0xFFC49A6C)))
)

fun renderPresetAvatarToBitmap(preset: PresetAvatar, size: Int = 256): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw circular background gradient
    val paint = Paint().apply {
        isAntiAlias = true
        shader = android.graphics.LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            preset.gradientColors.first().toArgb(),
            preset.gradientColors.last().toArgb(),
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Draw clean monogram initial
    val textPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textSize = size * 0.44f
        textAlign = Paint.Align.CENTER
    }
    val yPos = (size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(preset.emoji, size / 2f, yPos, textPaint)

    return bitmap
}

/**
 * Avatar Selector Dialog allowing users to pick from modern preset avatars
 * or choose a custom photo from their device.
 */
@Composable
fun KokonutAvatarSelectorDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val tokens = LocalKanriGlass.current

    var selectedPreset by remember { mutableStateOf<PresetAvatar?>(PRESET_AVATARS.first()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            haptics.success()
            val userPrefs = UserProfilePreferences.getInstance(context)
            val saved = userPrefs.savePhotoFromUri(uri)
            if (saved) {
                onAvatarSelected(null)
                onDismiss()
            }
        }
    }

    KanriGlassDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Choose Your Avatar",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select an avatar icon or upload your custom picture",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Preview Circle
            selectedPreset?.let { preset ->
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(preset.gradientColors)
                        )
                        .border(
                            width = 2.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = preset.emoji,
                        fontSize = 42.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = preset.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4-Column Grid of Preset Avatars
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                items(PRESET_AVATARS, key = { it.id }) { preset ->
                    val isSelected = (preset.id == selectedPreset?.id)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(preset.gradientColors)
                            )
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.25f),
                                shape = CircleShape
                            )
                            .clickable {
                                haptics.click()
                                selectedPreset = preset
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = preset.emoji,
                            fontSize = 28.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Custom Picture Button
            OutlinedButton(
                onClick = {
                    haptics.click()
                    photoPickerLauncher.launch("image/*")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = tokens?.accent ?: MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Upload Custom Picture",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save Avatar Button
            Button(
                onClick = {
                    selectedPreset?.let { preset ->
                        haptics.success()
                        val bitmap = renderPresetAvatarToBitmap(preset)
                        val userPrefs = UserProfilePreferences.getInstance(context)
                        userPrefs.saveBitmapInternal(bitmap)
                        onAvatarSelected(bitmap)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tokens?.isDark == true) Color(0xFFEDEDED) else Color(0xFF18181B),
                    contentColor = if (tokens?.isDark == true) Color(0xFF111114) else Color.White
                )
            ) {
                Text(
                    text = "Apply Selected Avatar",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

