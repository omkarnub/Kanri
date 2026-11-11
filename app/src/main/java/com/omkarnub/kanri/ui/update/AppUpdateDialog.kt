package com.omkarnub.kanri.ui.update

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassButton
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.KanriGlassDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.omkarnub.kanri.BuildConfig
import com.omkarnub.kanri.data.update.AppUpdateInfo
import com.omkarnub.kanri.data.update.AppUpdateManager
import com.omkarnub.kanri.data.update.AppUpdatePreferences
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

private enum class UpdateState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    PERMISSION_NEEDED,
    FAILED
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberKanriHaptics()
    val updateManager = remember { AppUpdateManager.getInstance(context) }
    val updatePrefs = remember { AppUpdatePreferences.getInstance(context) }

    var updateState by remember { mutableStateOf(UpdateState.IDLE) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var totalBytes by remember { mutableLongStateOf(updateInfo.apkSize) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    // Check if APK was already downloaded in a previous attempt
    LaunchedEffect(updateInfo) {
        val existing = updateManager.getDownloadedApk(context, updateInfo)
        if (existing != null) {
            downloadedFile = existing
            updateState = UpdateState.READY_TO_INSTALL
        }
    }

    KanriGlassDialog(
        onDismissRequest = {
            if (updateState != UpdateState.DOWNLOADING) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = updateState != UpdateState.DOWNLOADING,
            dismissOnClickOutside = updateState != UpdateState.DOWNLOADING
        )
    ) {
        KanriGlassCard(
            shape = RoundedCornerShape(24.dp),
            level = GlassLevel.Strong,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (updateState) {
                            UpdateState.DOWNLOADING -> Icons.Default.Download
                            UpdateState.READY_TO_INSTALL -> Icons.Default.CheckCircle
                            UpdateState.PERMISSION_NEEDED -> Icons.Default.Security
                            UpdateState.FAILED -> Icons.Default.ErrorOutline
                            else -> Icons.Default.SystemUpdate
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "New Version Available",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KanriGlassCard(
                        level = GlassLevel.Flat,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "v${updateInfo.versionName}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Current: v${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (updateInfo.apkSize > 0) {
                        val sizeMb = "%.1f MB".format(updateInfo.apkSize / (1024.0 * 1024.0))
                        Text(
                            text = "•",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = sizeMb,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                    text = "WHAT'S NEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                    // Clean scrollable container for release notes
                    KanriGlassCard(
                        shape = RoundedCornerShape(12.dp),
                        level = GlassLevel.Flat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 220.dp)
                    ) {
                    val scrollState = rememberScrollState()
                    val cleanedNotes = remember(updateInfo.releaseNotes) {
                        cleanReleaseNotes(updateInfo.releaseNotes)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = cleanedNotes.ifBlank { "Performance improvements, stability fixes, and UI refinements." },
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

                Spacer(modifier = Modifier.height(14.dp))

                // Dynamic Status Section
                when (updateState) {
                    UpdateState.DOWNLOADING -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Downloading update...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (downloadProgress >= 0f) "${(downloadProgress * 100).toInt()}%" else "...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            LinearProgressIndicator(
                                progress = { if (downloadProgress >= 0f) downloadProgress else 0.5f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.onSurface,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            if (totalBytes > 0) {
                                val currentMb = "%.1f".format(downloadedBytes / (1024.0 * 1024.0))
                                val totalMb = "%.1f MB".format(totalBytes / (1024.0 * 1024.0))
                                Text(
                                    text = "$currentMb MB / $totalMb",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    UpdateState.READY_TO_INSTALL -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Update downloaded. Ready to install.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    UpdateState.PERMISSION_NEEDED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Enable 'Install unknown apps' permission in Android Settings to apply update.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    UpdateState.FAILED -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage ?: "Download failed. Please check internet connection.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    UpdateState.IDLE -> {
                        // Clean resting state
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (updateState != UpdateState.DOWNLOADING) {
                        TextButton(
                            onClick = {
                                haptics.tick()
                                // Remember dismissed version so user isn't pestered constantly
                                updatePrefs.dismissedVersion = updateInfo.versionName
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "Later",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    when (updateState) {
                        UpdateState.IDLE -> {
                            KanriGlassButton(
                                text = "Update Now",
                                level = GlassLevel.Strong,
                                shape = RoundedCornerShape(10.dp),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    haptics.primaryAction()
                                    updateState = UpdateState.DOWNLOADING
                                    downloadJob = scope.launch {
                                        val result = updateManager.downloadApk(
                                            context = context,
                                            updateInfo = updateInfo,
                                            onProgress = { progress, downloaded, total ->
                                                downloadProgress = progress
                                                downloadedBytes = downloaded
                                                if (total > 0) totalBytes = total
                                            }
                                        )

                                        result.onSuccess { apkFile ->
                                            downloadedFile = apkFile
                                            updateState = UpdateState.READY_TO_INSTALL
                                            haptics.success()

                                            // Automatically trigger installer
                                            val installResult = updateManager.installApk(context, apkFile)
                                            installResult.onFailure { exc ->
                                                if (exc is SecurityException) {
                                                    updateState = UpdateState.PERMISSION_NEEDED
                                                } else {
                                                    errorMessage = exc.message
                                                    updateState = UpdateState.FAILED
                                                }
                                            }
                                        }.onFailure { exc ->
                                            errorMessage = exc.localizedMessage ?: "Download failed"
                                            updateState = UpdateState.FAILED
                                            haptics.warning()
                                        }
                                    }
                                }
                            )
                        }

                        UpdateState.DOWNLOADING -> {
                            KanriGlassButton(
                                text = "Cancel",
                                level = GlassLevel.Flat,
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    downloadJob?.cancel()
                                    updateState = UpdateState.IDLE
                                }
                            )
                        }

                        UpdateState.READY_TO_INSTALL -> {
                            KanriGlassButton(
                                text = "Install Now",
                                level = GlassLevel.Strong,
                                shape = RoundedCornerShape(10.dp),
                                icon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                onClick = {
                                    haptics.primaryAction()
                                    val file = downloadedFile
                                    if (file != null) {
                                        val res = updateManager.installApk(context, file)
                                        res.onFailure { exc ->
                                            if (exc is SecurityException) {
                                                updateState = UpdateState.PERMISSION_NEEDED
                                            } else {
                                                errorMessage = exc.message
                                                updateState = UpdateState.FAILED
                                            }
                                        }
                                    }
                                }
                            )
                        }

                        UpdateState.PERMISSION_NEEDED -> {
                            KanriGlassButton(
                                text = "Grant Permission",
                                level = GlassLevel.Strong,
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    haptics.click()
                                    val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                    // Reset state so when user returns they can tap install
                                    updateState = UpdateState.READY_TO_INSTALL
                                }
                            )
                        }

                        UpdateState.FAILED -> {
                            KanriGlassButton(
                                text = "Retry",
                                level = GlassLevel.Strong,
                                shape = RoundedCornerShape(10.dp),
                                onClick = {
                                    updateState = UpdateState.IDLE
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Strips raw markdown headers and hashes for clean minimalist presentation
 */
private fun cleanReleaseNotes(raw: String): String {
    if (raw.isBlank()) return ""
    return raw.lines()
        .filter { !it.trim().startsWith("---") && !it.trim().startsWith("**Checksums") }
        .joinToString("\n") { line ->
            var l = line.trim()
            while (l.startsWith("#")) {
                l = l.removePrefix("#").trim()
            }
            l
        }
        .trim()
}
