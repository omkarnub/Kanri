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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import com.omkarnub.kanri.ui.wallet.WalletSetupDialog
import com.omkarnub.kanri.ui.wallet.CorrectBalanceDialog
import com.omkarnub.kanri.data.wallet.WalletRepository
import com.omkarnub.kanri.data.wallet.WalletPreferences
import com.omkarnub.kanri.data.wallet.AtmWithdrawalMode
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import com.omkarnub.kanri.data.cloud.BackupMode
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.data.cloud.CloudBackupErrorType
import com.omkarnub.kanri.data.cloud.CloudBackupManager
import com.omkarnub.kanri.data.cloud.CloudBackupPreferences
import com.omkarnub.kanri.data.cloud.CloudBackupStatus
import com.omkarnub.kanri.data.cloud.CloudBackupWorker
import com.omkarnub.kanri.data.cloud.DriveAuthResult
import com.omkarnub.kanri.data.cloud.GoogleAuthManager
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.crash.CrashLogger
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.reset.FactoryResetManager
import com.omkarnub.kanri.data.security.SecurityPreferences
import com.omkarnub.kanri.ui.common.AVAILABLE_CATEGORY_ICONS
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.home.isCustomCategory
import com.omkarnub.kanri.ui.theme.AppIconManager
import com.omkarnub.kanri.ui.theme.AppIconStyle
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit = {},
    onTestLockScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberKanriHaptics()

    // 1. App Lock & Security Preferences
    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    var isLockEnabled by remember { mutableStateOf(securityPrefs.isLockEnabled) }

    // 4 & 5. Theme & App Icon Preferences
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val isDynamicEnabled by themePrefs.dynamicColorFlow.collectAsState()
    val currentIconStyle by themePrefs.appIconStyleFlow.collectAsState()
    var pendingIconStyle by remember { mutableStateOf<AppIconStyle?>(null) }
    var showIconRestartDialog by remember { mutableStateOf(false) }

    // 6. Custom Categories
    val db = remember { KanriDatabase.getDatabase(context) }
    val allCategories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())
    val customCategories = remember(allCategories) {
        allCategories.filter { isCustomCategory(it) }
    }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    // Smart Merchant Rules Screen
    var showMerchantRulesScreen by remember { mutableStateOf(false) }
    val merchantRules by db.categoryDao().getAllMappingsFlow().collectAsState(initial = emptyList())

    // Dialog state handlers
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmationInput by remember { mutableStateOf("") }
    var isResetting by remember { mutableStateOf(false) }

    var showCrashLogsDialog by remember { mutableStateOf(false) }
    var showSuggestFeatureDialog by remember { mutableStateOf(false) }
    var showKillAppDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Cloud Backup State
    val cloudBackupPrefs = remember { CloudBackupPreferences.getInstance(context) }
    val cloudBackupManager = remember { CloudBackupManager.getInstance(context) }
    val googleAuthManager = remember { GoogleAuthManager.getInstance(context) }

    val currentBackupMode by cloudBackupPrefs.backupModeFlow.collectAsState()
    val accountEmail by cloudBackupPrefs.accountEmailFlow.collectAsState()
    val accountName by cloudBackupPrefs.accountNameFlow.collectAsState()
    val lastBackupTimestamp by cloudBackupPrefs.lastBackupTimestampFlow.collectAsState()
    val lastBackupStatus by cloudBackupPrefs.lastBackupStatusFlow.collectAsState()
    val lastErrorMessage by cloudBackupPrefs.lastErrorMessageFlow.collectAsState()
    val lastErrorType by cloudBackupPrefs.lastErrorTypeFlow.collectAsState()

    var showSignOutOrOfflineDialog by remember { mutableStateOf(false) }
    var isSwitchingToOfflineOnly by remember { mutableStateOf(false) }
    var showRestoreFromCloudDialog by remember { mutableStateOf(false) }
    var restorePassphraseInput by remember { mutableStateOf("") }
    var isCloudActionInProgress by remember { mutableStateOf(false) }

    // Wallets & Balances
    val walletRepo = remember { WalletRepository(context) }
    val walletPrefs = remember { WalletPreferences.getInstance(context) }
    val isWalletConfigured by walletRepo.isConfigured().collectAsState(initial = false)
    val balances by walletRepo.observeBalances().collectAsState(initial = null)
    val atmMode by walletPrefs.atmWithdrawalModeFlow.collectAsState()

    var showWalletSetupDialog by remember { mutableStateOf(false) }
    var walletToCorrect by remember { mutableStateOf<String?>(null) }
    var showResetWalletsDialog by remember { mutableStateOf(false) }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            haptics.success()
            Toast.makeText(context, "Google Drive authorization granted", Toast.LENGTH_SHORT).show()
            CloudBackupWorker.schedule(context)
            scope.launch {
                isCloudActionInProgress = true
                cloudBackupManager.performCloudBackup()
                isCloudActionInProgress = false
            }
        } else {
            haptics.warning()
            Toast.makeText(context, "Drive authorization was cancelled or denied", Toast.LENGTH_SHORT).show()
        }
    }

    if (showMerchantRulesScreen) {
        SmartMerchantRulesScreen(
            onBack = { showMerchantRulesScreen = false },
            modifier = modifier
        )
        return
    }

    // =========================================================================
    // MODALS & DIALOGS
    // =========================================================================

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    // 2] Reset App Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Reset Application Data?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will permanently delete ALL transactions, categories, budgets, and savings goals. Encryption keys will be rotated and Kanri will restart into the setup walkthrough.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Type \"DELETE\" below to confirm:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetConfirmationInput,
                        onValueChange = { resetConfirmationInput = it },
                        placeholder = { Text("DELETE") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetConfirmationInput.trim() == "DELETE") {
                            haptics.warning()
                            isResetting = true
                            scope.launch {
                                FactoryResetManager.executeFactoryReset(context)
                                isResetting = false
                                showResetDialog = false
                                Toast.makeText(context, "Kanri has been reset to factory defaults.", Toast.LENGTH_LONG).show()
                                onReplayOnboarding()
                            }
                        } else {
                            Toast.makeText(context, "Please type DELETE to confirm reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = resetConfirmationInput.trim() == "DELETE" && !isResetting
                ) {
                    Text(if (isResetting) "Resetting..." else "Permanently Wipe")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    enabled = !isResetting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // 5] App Icon Restart Dialog
    if (showIconRestartDialog && pendingIconStyle != null) {
        val targetStyle = pendingIconStyle!!
        AlertDialog(
            onDismissRequest = {
                showIconRestartDialog = false
                pendingIconStyle = null
            },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restart App?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Switching launcher icon to ${targetStyle.label} requires restarting Kanri so the home screen updates.\n\nWould you like to restart now?",
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIconRestartDialog = false
                        themePrefs.appIconStyle = targetStyle
                        AppIconManager.restartApp(context, targetStyle, currentThemeMode)
                    }
                ) {
                    Text("Restart Now")
                }
            },
            dismissButton = {
                TextButton(
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

    // 6] Category Edit / Create Dialogs
    if (categoryToEdit != null) {
        CategoryEditCreateDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { newName, newIcon ->
                val target = categoryToEdit!!
                categoryToEdit = null
                scope.launch {
                    db.categoryDao().updateCategory(
                        target.copy(name = newName, iconName = newIcon)
                    )
                    Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showCreateCategoryDialog) {
        CategoryEditCreateDialog(
            category = null,
            onDismiss = { showCreateCategoryDialog = false },
            onSave = { newName, newIcon ->
                showCreateCategoryDialog = false
                scope.launch {
                    db.categoryDao().insertCategory(
                        CategoryEntity(
                            name = newName,
                            colorHex = "#6366F1",
                            iconName = newIcon,
                            isCustom = true
                        )
                    )
                    Toast.makeText(context, "Category created", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete '${cat.name}'?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this category? Any transactions currently assigned to '${cat.name}' will be moved to 'Other'.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val catId = cat.id
                        categoryToDelete = null
                        scope.launch {
                            val otherCat = db.categoryDao().getCategoryByName("Other")
                            db.transactionDao().reassignTransactionsCategory(catId, otherCat?.id)
                            db.categoryDao().deleteMappingsForCategory(catId)
                            db.categoryDao().deleteCategoryById(catId)
                            Toast.makeText(context, "Category '${cat.name}' deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 7] Consumer Grade Crash Logs Dialog
    if (showCrashLogsDialog) {
        ConsumerCrashLogsDialog(onDismiss = { showCrashLogsDialog = false })
    }

    // 8] Contact Us Dialog
    if (showContactDialog) {
        ContactUsDialog(onDismiss = { showContactDialog = false })
    }

    // 9] Suggest a Feature Dialog
    if (showSuggestFeatureDialog) {
        SuggestFeatureDialog(onDismiss = { showSuggestFeatureDialog = false })
    }

    // 10] Kill App Confirmation Dialog
    if (showKillAppDialog) {
        AlertDialog(
            onDismissRequest = { showKillAppDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Force Close App?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Text(
                    "Kanri will terminate immediately and close background runtime memory.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showKillAppDialog = false
                        haptics.tick()
                        (context as? Activity)?.finishAffinity()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Terminate App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKillAppDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 11] Cloud Backup Sign-Out or Switch-to-Offline Dialog
    if (showSignOutOrOfflineDialog) {
        val titleText = if (isSwitchingToOfflineOnly) "Switch to Fully Offline?" else "Sign Out of Google Drive?"
        val messageText = if (isSwitchingToOfflineOnly) {
            "Automatic cloud backups will stop and Kanri will operate in 100% offline mode. What would you like to do with your existing encrypted backup ('kanri_backup.enc') in Google Drive?"
        } else {
            "You will be signed out of Google Drive. Automatic cloud backups will stop. What would you like to do with your existing backup file in Google Drive?"
        }

        AlertDialog(
            onDismissRequest = { if (!isCloudActionInProgress) showSignOutOrOfflineDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(text = titleText, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = messageText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tick()
                        scope.launch {
                            isCloudActionInProgress = true
                            cloudBackupManager.deleteCloudBackup()
                            if (isSwitchingToOfflineOnly) {
                                cloudBackupPrefs.backupMode = BackupMode.OFFLINE
                            } else {
                                googleAuthManager.signOut()
                                cloudBackupPrefs.clearAccount()
                                cloudBackupPrefs.backupMode = BackupMode.OFFLINE
                            }
                            CloudBackupWorker.cancel(context)
                            isCloudActionInProgress = false
                            showSignOutOrOfflineDialog = false
                            Toast.makeText(context, "Cloud copy deleted & switched to Fully Offline", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isCloudActionInProgress
                ) {
                    Text(if (isCloudActionInProgress) "Deleting..." else "Delete from Drive")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { if (!isCloudActionInProgress) showSignOutOrOfflineDialog = false },
                        enabled = !isCloudActionInProgress
                    ) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            haptics.tick()
                            scope.launch {
                                if (isSwitchingToOfflineOnly) {
                                    cloudBackupPrefs.backupMode = BackupMode.OFFLINE
                                } else {
                                    googleAuthManager.signOut()
                                    cloudBackupPrefs.clearAccount()
                                    cloudBackupPrefs.backupMode = BackupMode.OFFLINE
                                }
                                CloudBackupWorker.cancel(context)
                                showSignOutOrOfflineDialog = false
                                Toast.makeText(context, "Backup file kept in Drive. Switched to Fully Offline.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isCloudActionInProgress
                    ) {
                        Text("Leave in Drive", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    // 12] Restore from Cloud Confirmation Dialog
    if (showRestoreFromCloudDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCloudActionInProgress) showRestoreFromCloudDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(text = "Restore from Google Drive?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Restoring will replace all current transactions, categories, budgets, and debt records with the encrypted backup from your Google Drive. This cannot be undone.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Encryption Passphrase (Optional):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Leave blank if you use standard Google Account encryption.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = restorePassphraseInput,
                        onValueChange = { restorePassphraseInput = it },
                        placeholder = { Text("Custom passphrase if set") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.warning()
                        scope.launch {
                            isCloudActionInProgress = true
                            val result = cloudBackupManager.restoreFromCloud(
                                explicitPassphrase = restorePassphraseInput.takeIf { it.isNotBlank() },
                                clearExisting = true
                            )
                            isCloudActionInProgress = false
                            showRestoreFromCloudDialog = false
                            result.onSuccess { stats ->
                                haptics.success()
                                Toast.makeText(
                                    context,
                                    "Restored ${stats.transactionCount} transactions & ${stats.lendingCount} debt records from Google Drive!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { error ->
                                haptics.warning()
                                Toast.makeText(context, "Restore failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isCloudActionInProgress
                ) {
                    Text(if (isCloudActionInProgress) "Restoring..." else "Confirm Restore")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreFromCloudDialog = false },
                    enabled = !isCloudActionInProgress
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Wallets & Balances Dialogs
    if (showWalletSetupDialog) {
        WalletSetupDialog(
            initialCash = balances?.cash ?: 0.0,
            initialOnline = balances?.online ?: 0.0,
            initialAtmMode = atmMode,
            isInitialSetup = !isWalletConfigured,
            onDismiss = { showWalletSetupDialog = false },
            onConfirm = { cash, online, mode ->
                scope.launch {
                    walletRepo.setup(cash, online, mode)
                    showWalletSetupDialog = false
                    Toast.makeText(context, "Wallet balances updated", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (walletToCorrect != null) {
        val targetWallet = walletToCorrect!!
        val currentBal = if (targetWallet == "CASH") balances?.cash ?: 0.0 else balances?.online ?: 0.0
        CorrectBalanceDialog(
            wallet = targetWallet,
            currentBalance = currentBal,
            onDismiss = { walletToCorrect = null },
            onConfirm = { desired ->
                scope.launch {
                    walletRepo.correct(targetWallet, desired)
                    walletToCorrect = null
                    Toast.makeText(context, "Balance corrected", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showResetWalletsDialog) {
        AlertDialog(
            onDismissRequest = { showResetWalletsDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Balances?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will clear your Cash and Online opening balances and return to unconfigured state. None of your transactions will be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.warning()
                        scope.launch {
                            walletRepo.reset()
                            showResetWalletsDialog = false
                            Toast.makeText(context, "Wallet balances reset", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetWalletsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // =========================================================================
    // MAIN SETTINGS LAYOUT
    // =========================================================================
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptics.tick()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -----------------------------------------------------------------
            // BALANCES & WALLETS
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "BALANCES & WALLETS", icon = Icons.Default.AccountBalanceWallet)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Status & Balances
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isWalletConfigured) "Wallet Tracking Active" else "Balances Not Configured",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isWalletConfigured && balances != null)
                                    "Cash: ${com.omkarnub.kanri.util.CurrencyUtils.formatCurrency(balances!!.cash)} • Online: ${com.omkarnub.kanri.util.CurrencyUtils.formatCurrency(balances!!.online)}"
                                else
                                    "Set up Cash and Online balances to track money in hand vs bank",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                haptics.click()
                                showWalletSetupDialog = true
                            }
                        ) {
                            Text(if (isWalletConfigured) "Edit Baselines" else "Set Up")
                        }
                    }

                    if (isWalletConfigured) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Correct Live Balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Correct Balance",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adjust opening amount to match your actual balance right now",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        walletToCorrect = "CASH"
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Cash", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        walletToCorrect = "ONLINE"
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Online", fontSize = 12.sp)
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // ATM Withdrawal Mode
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "ATM Withdrawal Handling",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (atmMode == AtmWithdrawalMode.TRANSFER)
                                            "Move money to Cash (excluded from spending)"
                                        else
                                            "Count as spending (no Cash credit)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = atmMode == AtmWithdrawalMode.TRANSFER,
                                    onClick = {
                                        haptics.click()
                                        scope.launch {
                                            walletRepo.setAtmWithdrawalMode(AtmWithdrawalMode.TRANSFER)
                                            Toast.makeText(context, "ATM mode: Move money to Cash", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = { Text("Move to Cash (Transfer)") }
                                )
                                FilterChip(
                                    selected = atmMode == AtmWithdrawalMode.SPENDING,
                                    onClick = {
                                        haptics.click()
                                        scope.launch {
                                            walletRepo.setAtmWithdrawalMode(AtmWithdrawalMode.SPENDING)
                                            Toast.makeText(context, "ATM mode: Count as spending", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    label = { Text("Count as Spending") }
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Reset Balances
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Balances",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Clear opening balances without deleting any transactions",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = {
                                    haptics.warning()
                                    showResetWalletsDialog = true
                                }
                            ) {
                                Text("Reset", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 1 & 3: SECURITY & PERMISSIONS
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "SECURITY & ACCESS", icon = Icons.Default.Shield)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1] App Lock
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Lock",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Authenticate using Fingerprint, Face, PIN, or Pattern on launch",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { checked ->
                                haptics.tick()
                                isLockEnabled = checked
                                securityPrefs.isLockEnabled = checked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3] Permissions Status Section
                    Text(
                        text = "App Permissions",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Review operating permissions granted to Kanri",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionsStatusOverview(context = context)
                }
            }

            // -----------------------------------------------------------------
            // CLOUD BACKUP & STORAGE
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "CLOUD BACKUP & SYNC", icon = Icons.Default.Cloud)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data Storage & Privacy",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your financial ledger is purely local and completely private to your device",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Offline Mode Content
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "100% Offline Mode Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Zero telemetry, zero network calls. Your transactions are stored securely in local SQLCipher AES-256 storage.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    if (false) {
                        // Cloud Backup Mode Content
                        if (accountEmail.isNullOrBlank()) {
                            // Not signed in yet
                            Text(
                                text = "Sign In with Google",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Connect your personal Google Drive to enable silent, encrypted automatic backups (kanri_backup.enc). Kanri only requests the restricted drive.file scope.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    haptics.tick()
                                    scope.launch {
                                        isCloudActionInProgress = true
                                        val signInResult = googleAuthManager.signInWithGoogle(context)
                                        signInResult.onSuccess { account ->
                                            cloudBackupPrefs.setSignedInAccount(account.email, account.displayName, account.id)
                                            val authResult = googleAuthManager.requestDriveAuthorization(account.email)
                                            authResult.onSuccess { driveAuth ->
                                                when (driveAuth) {
                                                    is DriveAuthResult.NeedsResolution -> {
                                                        driveConsentLauncher.launch(
                                                            IntentSenderRequest.Builder(driveAuth.pendingIntent.intentSender).build()
                                                        )
                                                    }
                                                    is DriveAuthResult.Authorized -> {
                                                        CloudBackupWorker.schedule(context)
                                                        cloudBackupManager.performCloudBackup()
                                                        haptics.success()
                                                        Toast.makeText(context, "Connected to Google Drive!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }.onFailure { e ->
                                                Toast.makeText(context, "Drive authorization failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }.onFailure { e ->
                                            Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        isCloudActionInProgress = false
                                    }
                                },
                                enabled = !isCloudActionInProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isCloudActionInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Signing In...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Signed in account row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column {
                                        if (!accountName.isNullOrBlank()) {
                                            Text(
                                                text = accountName ?: "",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = accountEmail ?: "",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        isSwitchingToOfflineOnly = false
                                        showSignOutOrOfflineDialog = true
                                    }
                                ) {
                                    Text(
                                        text = "Sign Out",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Last Backup Timestamp
                            val formattedDate = if (lastBackupTimestamp > 0L) {
                                SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(lastBackupTimestamp))
                            } else {
                                "Never"
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (lastBackupTimestamp > 0L) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = if (lastBackupTimestamp > 0L) Color(0xFF06D6A0) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Last backed up: $formattedDate",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isCloudActionInProgress || lastBackupStatus == CloudBackupStatus.IN_PROGRESS) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Diagnostics Banner (reusing HealthCheck style)
                            if (lastErrorType != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val errorType = lastErrorType!!
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = errorType.name.replace("_", " "),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = lastErrorMessage ?: errorType.userMessage,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                when (errorType) {
                                                    CloudBackupErrorType.TOKEN_EXPIRED,
                                                    CloudBackupErrorType.PERMISSION_REVOKED -> {
                                                        scope.launch {
                                                            isCloudActionInProgress = true
                                                            val driveAuth = googleAuthManager.requestDriveAuthorization(accountEmail)
                                                            driveAuth.onSuccess { auth ->
                                                                if (auth is DriveAuthResult.NeedsResolution) {
                                                                    driveConsentLauncher.launch(
                                                                        IntentSenderRequest.Builder(auth.pendingIntent.intentSender).build()
                                                                    )
                                                                } else {
                                                                    cloudBackupManager.performCloudBackup()
                                                                }
                                                            }
                                                            isCloudActionInProgress = false
                                                        }
                                                    }
                                                    CloudBackupErrorType.DECRYPTION_FAILED -> {
                                                        showRestoreFromCloudDialog = true
                                                    }
                                                    else -> {
                                                        scope.launch {
                                                            isCloudActionInProgress = true
                                                            cloudBackupManager.performCloudBackup()
                                                            isCloudActionInProgress = false
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = errorType.actionLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Manual Actions: "Back Up Now" and "Restore from Cloud"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        haptics.primaryAction()
                                        scope.launch {
                                            isCloudActionInProgress = true
                                            val res = cloudBackupManager.performCloudBackup()
                                            isCloudActionInProgress = false
                                            res.onSuccess {
                                                haptics.success()
                                                Toast.makeText(context, "Encrypted backup saved to Google Drive!", Toast.LENGTH_SHORT).show()
                                            }.onFailure { e ->
                                                haptics.warning()
                                                Toast.makeText(context, "Backup failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = !isCloudActionInProgress,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Back Up Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        showRestoreFromCloudDialog = true
                                    },
                                    enabled = !isCloudActionInProgress,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Cloud", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            SettingsSectionHeader(title = "THEMES & PREVIEW", icon = Icons.Default.ColorLens)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme Selection",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Choose your preferred minimalist design palette",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Realistic Mini Theme Previews Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            ThemePreviewCard(
                                mode = mode,
                                isSelected = currentThemeMode == mode,
                                onClick = {
                                    haptics.tick()
                                    if (mode != currentThemeMode) {
                                        themePrefs.themeMode = mode
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Material You Dynamic Color",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tint interface based on your system wallpaper",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isDynamicEnabled,
                                onCheckedChange = { checked ->
                                    haptics.tick()
                                    themePrefs.isDynamicColor = checked
                                }
                            )
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 5: APP ICON
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "APP ICON", icon = Icons.Default.Apps)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Launcher App Icon",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select dark or light launcher home screen icon",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppIconStyle.entries.forEach { style ->
                            val isSelected = currentIconStyle == style
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        haptics.click()
                                        if (style != currentIconStyle) {
                                            pendingIconStyle = style
                                            showIconRestartDialog = true
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.surfaceVariant
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (style == AppIconStyle.DARK) Color(0xFF000000) else Color(0xFFFEFAEC)
                                            )
                                            .border(
                                                1.dp,
                                                if (style == AppIconStyle.LIGHT) Color(0xFFDDD5B8) else Color(0xFF262626),
                                                RoundedCornerShape(14.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                id = if (style == AppIconStyle.DARK) R.drawable.panchang_light else R.drawable.panchang_dark
                                            ),
                                            contentDescription = "${style.label} Icon",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = style.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = if (isSelected) "Active" else "Select",
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 6: CUSTOM CATEGORIES
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "CUSTOM CATEGORIES", icon = Icons.Default.Tune)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Custom Categories",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${customCategories.size} customized category rules",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showCreateCategoryDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add custom category",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (customCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom categories yet. Tap '+' to create one.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            customCategories.forEachIndexed { index, cat ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryIcon(
                                            categoryName = cat.name,
                                            iconName = cat.iconName,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = cat.name,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { categoryToEdit = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { categoryToDelete = cat },
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

            // -----------------------------------------------------------------
            // 7: AUTOMATIONS & SMART MERCHANT RULES
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "AUTOMATIONS & RULES", icon = Icons.Default.AutoFixHigh)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        haptics.tick()
                        showMerchantRulesScreen = true
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Smart Merchant Rules",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-assign categories by merchant keywords",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "${merchantRules.size} rules",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // -----------------------------------------------------------------
            // 8: SUPPORT, CRASH LOGS & FEEDBACK
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "SUPPORT & FEEDBACK", icon = Icons.Default.HealthAndSafety)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 7] Crash Logs
                    SettingsRow(
                        icon = Icons.Default.HealthAndSafety,
                        title = "App Health & Crash Logs",
                        subtitle = "Review local stability diagnostics with zero cloud telemetry",
                        onClick = { showCrashLogsDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 8] Contact Us
                    SettingsRow(
                        icon = Icons.Default.Email,
                        title = "Contact Us",
                        subtitle = "support@kanri.app • Reach the developer directly",
                        onClick = { showContactDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 9] Suggest a Feature
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "Suggest a Feature",
                        subtitle = "Share your feedback and feature requests",
                        onClick = { showSuggestFeatureDialog = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 10] Privacy Policy
                    SettingsRow(
                        icon = Icons.Default.Shield,
                        title = "Privacy Policy",
                        subtitle = "Offline-first data sovereignty, zero trackers & permissions transparency",
                        onClick = { showPrivacyPolicyDialog = true }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 2 & 10: DANGER ZONE (RESET APP & KILL APP)
            // -----------------------------------------------------------------
            SettingsSectionHeader(title = "SYSTEM & ACTIONS", icon = Icons.Default.PowerSettingsNew)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 2] Reset App
                    SettingsRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Reset App",
                        subtitle = "Permanently wipe all records, categories, and encryption keys",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            resetConfirmationInput = ""
                            showResetDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 10] Kill App
                    SettingsRow(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Kill App",
                        subtitle = "Immediately terminate application process and close memory",
                        tint = MaterialTheme.colorScheme.onSurface,
                        onClick = { showKillAppDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// =============================================================================
// PERMISSIONS STATUS OVERVIEW COMPOSABLE
// =============================================================================
@Composable
private fun PermissionsStatusOverview(context: Context) {
    val haptics = rememberKanriHaptics()
    val isSmsAllowed = com.omkarnub.kanri.util.SmsPermissionHelper.hasSmsPermissions(context)
    val isNotificationAccessAllowed = NotificationAccessHelper.isNotificationAccessGranted(context)

    val isNotifAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    val isOverlayAllowed = Settings.canDrawOverlays(context)

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    val isBatteryUnrestricted = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionItemRow(name = "Bank SMS Detection (Primary)", isAllowed = isSmsAllowed)
        PermissionItemRow(name = "Payment Notification Listener (Supplementary)", isAllowed = isNotificationAccessAllowed)
        PermissionItemRow(name = "Transaction Notifications", isAllowed = isNotifAllowed)
        PermissionItemRow(name = "Instant Floating Overlay", isAllowed = isOverlayAllowed)
        PermissionItemRow(name = "Background Battery", isAllowed = isBatteryUnrestricted, allowedLabel = "Unrestricted", deniedLabel = "Optimized")

        Spacer(modifier = Modifier.height(4.dp))

        if (!isSmsAllowed) {
            Button(
                onClick = {
                    haptics.click()
                    if (context is android.app.Activity) {
                        com.omkarnub.kanri.util.SmsPermissionHelper.requestSmsPermissions(context)
                    } else {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Enable Bank SMS Detection", fontSize = 12.sp)
            }
        }

        if (!isNotificationAccessAllowed) {
            Button(
                onClick = {
                    haptics.click()
                    NotificationAccessHelper.openNotificationAccessSettings(context)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Enable Notification Access", fontSize = 12.sp)
            }
        }

        OutlinedButton(
            onClick = {
                haptics.click()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Manage Permissions in System Settings", fontSize = 12.sp)
        }
    }
}

@Composable
private fun PermissionItemRow(
    name: String,
    isAllowed: Boolean,
    allowedLabel: String = "Allowed",
    deniedLabel: String = "Denied"
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isAllowed)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(
                0.8.dp,
                if (isAllowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Text(
                text = if (isAllowed) allowedLabel else deniedLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isAllowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// =============================================================================
// THEME PREVIEW CARD COMPOSABLE
// =============================================================================
@Composable
private fun ThemePreviewCard(
    mode: ThemeMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Theme palette constants for realistic previews
    val (bgColor, cardColor, cardBorder, barColor, dockColor) = when (mode) {
        ThemeMode.DARK -> listOf(
            Color(0xFF141414),
            Color(0xFF222222),
            Color(0xFF333333),
            Color(0xFFDDDDDD),
            Color(0xFF1A1A1A)
        )
        ThemeMode.AMOLED -> listOf(
            Color(0xFF000000),
            Color(0xFF121212),
            Color(0xFF262626),
            Color(0xFFFFFFFF),
            Color(0xFF0A0A0A)
        )
        ThemeMode.LIGHT -> listOf(
            Color(0xFFFBF8EE),
            Color(0xFFFFFFFF),
            Color(0xFFE2DDD1),
            Color(0xFF222222),
            Color(0xFFEDE8DD)
        )
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Miniature Phone Window Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .border(1.dp, cardBorder, RoundedCornerShape(10.dp))
                    .padding(6.dp)
            ) {
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
                                .size(width = 16.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(barColor.copy(alpha = 0.5f))
                        )
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(barColor.copy(alpha = 0.5f))
                        )
                    }

                    // Mini Content Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cardColor)
                            .border(0.8.dp, cardBorder, RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(width = 28.dp, height = 5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                            Box(
                                modifier = Modifier
                                    .size(width = 44.dp, height = 3.dp)
                                    .clip(RoundedCornerShape(1.5.dp))
                                    .background(barColor.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 8.dp)
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
                            .height(12.dp)
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
                                        .background(barColor.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }

                // Checkmark Circle Badge if selected
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = mode.label,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = when (mode) {
                    ThemeMode.DARK -> "Charcoal"
                    ThemeMode.AMOLED -> "Pitch Black"
                    ThemeMode.LIGHT -> "Soft Cream"
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============================================================================
// CONSUMER CRASH LOGS DIALOG
// =============================================================================
@Composable
private fun ConsumerCrashLogsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(CrashLogger.getCrashLogs(context)) }
    val haptics = rememberKanriHaptics()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HealthAndSafety,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text("App Health & Logs", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "All Systems Running Smoothly",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "No diagnostic errors or crash events recorded.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "${logs.size} stability reports stored locally:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    logs.forEach { logFile ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                                        .format(Date(logFile.lastModified()))
                                    Text(
                                        text = "Diagnostic Report",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (logs.isNotEmpty()) {
                TextButton(
                    onClick = {
                        haptics.tick()
                        CrashLogger.clearAllLogs(context)
                        logs = emptyList()
                        Toast.makeText(context, "Diagnostic logs cleared", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear Logs")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (logs.isNotEmpty()) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

// =============================================================================
// CONTACT US DIALOG
// =============================================================================
@Composable
private fun ContactUsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val haptics = rememberKanriHaptics()
    val email = "support@kanri.app"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Support", fontWeight = FontWeight.Bold) },
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
// SUGGEST A FEATURE DIALOG
// =============================================================================
@Composable
private fun SuggestFeatureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val categories = listOf("Analytics", "Automation", "Budgeting", "UI & Design", "Other")
    var selectedCategory by remember { mutableStateOf(categories.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suggest a Feature", fontWeight = FontWeight.Bold) },
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
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Use Case") },
                    placeholder = { Text("How would this help your daily expense tracking?") },
                    maxLines = 4,
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

// =============================================================================
// REUSABLE SETTINGS HELPERS
// =============================================================================
@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val haptics = rememberKanriHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                haptics.click()
                onClick()
            })
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = tint
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CategoryEditCreateDialog(
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleSmall,
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
                        .height(150.dp)
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
