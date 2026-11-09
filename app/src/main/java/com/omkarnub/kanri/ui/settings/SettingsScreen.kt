package com.omkarnub.kanri.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.BuildConfig
import com.omkarnub.kanri.data.backup.BackupRepository
import com.omkarnub.kanri.data.cloud.BackupMode
import com.omkarnub.kanri.data.cloud.CloudBackupPreferences
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.export.ExportScheduler
import com.omkarnub.kanri.data.reset.FactoryResetManager
import com.omkarnub.kanri.data.security.SecurityPreferences
import com.omkarnub.kanri.data.update.AppUpdateInfo
import com.omkarnub.kanri.data.update.AppUpdateManager
import com.omkarnub.kanri.data.wallet.AtmWithdrawalMode
import com.omkarnub.kanri.data.wallet.WalletPreferences
import com.omkarnub.kanri.data.wallet.WalletRepository
import com.omkarnub.kanri.ui.common.GlassBackgroundType
import com.omkarnub.kanri.ui.common.KanriGlassAlertDialog as AlertDialog
import com.omkarnub.kanri.ui.common.kanriGlassTextFieldColors
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import com.omkarnub.kanri.ui.export.ExportStatementSheet
import com.omkarnub.kanri.ui.home.isCustomCategory
import com.omkarnub.kanri.ui.theme.AppIconManager
import com.omkarnub.kanri.ui.theme.AppIconStyle
import com.omkarnub.kanri.ui.theme.GlassCapabilities
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.KanriTheme
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.update.AppUpdateDialog
import com.omkarnub.kanri.ui.wallet.CorrectBalanceDialog
import com.omkarnub.kanri.ui.wallet.WalletSetupDialog
import com.omkarnub.kanri.util.CurrencyPreferences
import com.omkarnub.kanri.util.CurrencyUtils
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
    val tokens = LocalKanriGlass.current

    // -------------------------------------------------------------------------
    // Preferences & State Holders
    // -------------------------------------------------------------------------
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val currentIconStyle by themePrefs.appIconStyleFlow.collectAsState()
    val currentGlassBg by themePrefs.glassBackgroundFlow.collectAsState()



    val securityPrefs = remember { SecurityPreferences.getInstance(context) }
    var isLockEnabled by remember { mutableStateOf(securityPrefs.isLockEnabled) }

    val currencyPrefs = remember { CurrencyPreferences.getInstance(context) }
    val currentCurrency by currencyPrefs.currencyFlow.collectAsState()
    val budgetPrefs = remember { com.omkarnub.kanri.data.budget.BudgetPreferences.getInstance(context) }
    val budgetCycleStartDay by budgetPrefs.budgetCycleStartDayFlow.collectAsState()

    val walletRepo = remember { WalletRepository(context) }
    val walletPrefs = remember { WalletPreferences.getInstance(context) }
    val isWalletConfigured by walletRepo.isConfigured().collectAsState(initial = false)
    val balances by walletRepo.observeBalances().collectAsState(initial = null)
    val atmMode by walletPrefs.atmWithdrawalModeFlow.collectAsState()
    val isBalanceSpoilerEnabled by walletPrefs.isBalanceSpoilerEnabledFlow.collectAsState()

    val exportSchedule by ExportScheduler.scheduleFlow.collectAsState()
    val isAutoMonthly = (exportSchedule == ExportScheduler.ExportSchedule.MONTHLY)

    val db = remember { KanriDatabase.getDatabase(context) }
    val allCategories by db.categoryDao().getAllCategories().collectAsState(initial = emptyList())
    val customCategories = remember(allCategories) { allCategories.filter { isCustomCategory(it) } }
    val merchantRules by db.categoryDao().getAllMappingsFlow().collectAsState(initial = emptyList())

    val backupRepository = remember { BackupRepository(context) }
    val appUpdateManager = remember { AppUpdateManager.getInstance(context) }

    // -------------------------------------------------------------------------
    // Sub-Screen & Dialog States
    // -------------------------------------------------------------------------
    var showThemeSettingsScreen by rememberSaveable { mutableStateOf(false) }
    var showGlassCustomizerScreen by rememberSaveable { mutableStateOf(false) }
    var showMerchantRulesScreen by rememberSaveable { mutableStateOf(false) }
    var showWidgetsScreen by rememberSaveable { mutableStateOf(false) }

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showBudgetCycleDialog by remember { mutableStateOf(false) }
    var showExportStatementSheet by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showCrashLogsDialog by remember { mutableStateOf(false) }
    var showSuggestFeatureDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }

    var pendingIconStyle by remember { mutableStateOf<AppIconStyle?>(null) }
    var showIconRestartDialog by remember { mutableStateOf(false) }

    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateCategoryDialog by remember { mutableStateOf(false) }

    var showWalletSetupDialog by remember { mutableStateOf(false) }
    var walletToCorrect by remember { mutableStateOf<String?>(null) }
    var showResetWalletsDialog by remember { mutableStateOf(false) }

    var showKillAppDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmationInput by remember { mutableStateOf("") }
    var isResetting by remember { mutableStateOf(false) }

    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var activeUpdateDialogInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    // Direct Offline Backup Export & Restore
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var exportPassphraseInput by remember { mutableStateOf("") }
    var pendingExportPassphrase by remember { mutableStateOf<String?>(null) }

    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var isPendingRestoreEncrypted by remember { mutableStateOf(false) }
    var restorePassphraseInput by remember { mutableStateOf("") }
    var restoreReplaceExisting by remember { mutableStateOf(false) }

    // Launcher for exporting offline backup JSON
    val exportDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val pass = pendingExportPassphrase
                val result = backupRepository.exportBackupToUri(uri, pass)
                result.onSuccess {
                    haptics.success()
                    val msg = if (pass != null) "Encrypted backup exported" else "Offline backup exported"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }.onFailure {
                    haptics.warning()
                    Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Launcher for selecting an existing backup to restore
    val importDataLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            scope.launch {
                isPendingRestoreEncrypted = backupRepository.isBackupFileEncrypted(uri)
                restorePassphraseInput = ""
                restoreReplaceExisting = false
            }
        }
    }

    // -------------------------------------------------------------------------
    // Sub-Screen Overlays
    // -------------------------------------------------------------------------
    if (showThemeSettingsScreen) {
        ThemeSettingsScreen(
            onBack = { showThemeSettingsScreen = false },
            onNavigateToGlassCustomizer = {
                showThemeSettingsScreen = false
                showGlassCustomizerScreen = true
            },
            modifier = modifier
        )
        return
    }

    if (showGlassCustomizerScreen) {
        GlassCustomizerScreen(
            onBack = {
                showGlassCustomizerScreen = false
                showThemeSettingsScreen = true
            },
            modifier = modifier
        )
        return
    }

    if (showMerchantRulesScreen) {
        SmartMerchantRulesScreen(
            onBack = { showMerchantRulesScreen = false },
            modifier = modifier
        )
        return
    }

    if (showWidgetsScreen) {
        WidgetsScreen(
            onBack = { showWidgetsScreen = false },
            modifier = modifier
        )
        return
    }

    // -------------------------------------------------------------------------
    // Dialogs & Sheets
    // -------------------------------------------------------------------------
    if (showCurrencyDialog) {
        SelectCurrencyDialog(
            currentCurrency = currentCurrency,
            onSelectCurrency = { selected ->
                haptics.success()
                currencyPrefs.currency = selected
                showCurrencyDialog = false
            },
            onDismiss = { showCurrencyDialog = false }
        )
    }

    if (showBudgetCycleDialog) {
        BudgetCycleStartDialog(
            currentDay = budgetCycleStartDay,
            onSelectDay = { selectedDay ->
                haptics.success()
                budgetPrefs.budgetCycleStartDay = selectedDay
                showBudgetCycleDialog = false
                Toast.makeText(context, "Budget cycle updated to Day $selectedDay", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showBudgetCycleDialog = false }
        )
    }

    if (showExportStatementSheet) {
        ExportStatementSheet(onDismiss = { showExportStatementSheet = false })
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }

    if (showCrashLogsDialog) {
        CrashLogsDialog(onDismiss = { showCrashLogsDialog = false })
    }

    if (showSuggestFeatureDialog) {
        SuggestFeatureDialog(onDismiss = { showSuggestFeatureDialog = false })
    }

    if (showContactDialog) {
        ContactUsDialog(onDismiss = { showContactDialog = false })
    }

    if (showIconRestartDialog && pendingIconStyle != null) {
        val targetStyle = pendingIconStyle!!
        AlertDialog(
            onDismissRequest = {
                showIconRestartDialog = false
                pendingIconStyle = null
            },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Restart App?", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex) },
            text = {
                Text(
                    text = "Switching launcher icon to ${targetStyle.label} requires restarting Kanri so the home screen updates.\n\nRestart now?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.5.sp
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

    if (categoryToEdit != null) {
        CategoryEditCreateDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { newName, newIcon ->
                val target = categoryToEdit!!
                categoryToEdit = null
                scope.launch {
                    db.categoryDao().updateCategory(target.copy(name = newName, iconName = newIcon))
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
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete this category? Any transactions currently assigned to '${cat.name}' will be safely reassigned to 'Other'.",
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
                    Toast.makeText(context, "Wallet opening balances configured", Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, "Balance corrected successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showResetWalletsDialog) {
        AlertDialog(
            onDismissRequest = { showResetWalletsDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Opening Balances?", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex) },
            text = {
                Text("This will clear your Cash and Online opening balances and return to the unconfigured state. None of your recorded transactions will be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.warning()
                        scope.launch {
                            walletRepo.reset()
                            showResetWalletsDialog = false
                            Toast.makeText(context, "Opening balances cleared", Toast.LENGTH_SHORT).show()
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

    // Direct Export Dialog
    if (showExportBackupDialog) {
        AlertDialog(
            onDismissRequest = { showExportBackupDialog = false },
            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = { Text("Export Offline Backup", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex) },
            text = {
                Column {
                    Text(
                        text = "Set an optional passphrase to encrypt your backup file with AES-GCM. Leave blank to export as plain JSON.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPassphraseInput,
                        onValueChange = { exportPassphraseInput = it },
                        label = { Text("Passphrase (optional)") },
                        singleLine = true,
                        colors = kanriGlassTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExportBackupDialog = false
                        pendingExportPassphrase = exportPassphraseInput.trim().ifEmpty { null }
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                        exportDataLauncher.launch("kanri_backup_$timestamp.json")
                    },
                    colors = kanriPrimaryButtonColors()
                ) {
                    Text("Choose Destination")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct Restore Dialog
    if (pendingRestoreUri != null) {
        val targetUri = pendingRestoreUri!!
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = if (isPendingRestoreEncrypted) "Restore Encrypted Backup" else "Restore Backup File",
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isPendingRestoreEncrypted)
                            "This backup is protected with AES-GCM encryption. Enter the passphrase used during export to decrypt."
                        else
                            "Restoring will import all transactions and categories. Existing local records will be merged safely.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isPendingRestoreEncrypted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = restorePassphraseInput,
                            onValueChange = { restorePassphraseInput = it },
                            label = { Text("Passphrase") },
                            singleLine = true,
                            colors = kanriGlassTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { restoreReplaceExisting = !restoreReplaceExisting }
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = restoreReplaceExisting,
                            onCheckedChange = { restoreReplaceExisting = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Replace all existing local data",
                            fontSize = 12.sp,
                            color = if (restoreReplaceExisting) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingRestoreUri = null
                        scope.launch {
                            val pass = restorePassphraseInput.trim().ifEmpty { null }
                            val result = backupRepository.restoreBackupFromUri(
                                uri = targetUri,
                                passphrase = pass,
                                clearExisting = restoreReplaceExisting
                            )
                            result.onSuccess { stats ->
                                haptics.success()
                                Toast.makeText(context, "Restored ${stats.transactionCount} transactions successfully", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                haptics.warning()
                                Toast.makeText(context, "Restore failed: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = kanriPrimaryButtonColors()
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                Text("Force Close App?", fontWeight = FontWeight.Bold, fontFamily = GoogleSansFlex, color = MaterialTheme.colorScheme.error)
            },
            text = {
                Text(
                    text = "Kanri will terminate immediately and release background runtime memory.",
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
                    text = "Factory Reset Kanri?",
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFlex,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text(
                        text = "This will permanently wipe ALL transactions, categories, budgets, and savings goals. Database encryption keys will be rotated and Kanri will restart into the fresh setup walkthrough.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Type \"DELETE\" below to confirm wipe:",
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
                        colors = kanriGlassTextFieldColors(),
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

    if (activeUpdateDialogInfo != null) {
        AppUpdateDialog(
            updateInfo = activeUpdateDialogInfo!!,
            onDismiss = { activeUpdateDialogInfo = null }
        )
    }

    // -------------------------------------------------------------------------
    // Main Settings Scaffold
    // -------------------------------------------------------------------------
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = if (tokens != null) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Settings",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Preferences & App Management",
                            fontSize = 11.5.sp,
                            fontFamily = GoogleSansFlex,
                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // =================================================================
            // 1. CUSTOMISATION
            // =================================================================
            SettingsGroup(title = "CUSTOMISATION") {
                val themeLabel = when (currentThemeMode) {
                    ThemeMode.SYSTEM -> "Follow System"
                    ThemeMode.LIGHT -> "Light Theme"
                    ThemeMode.DARK -> "Dark Theme"
                    ThemeMode.AMOLED -> "AMOLED Black"
                    ThemeMode.GLASS -> "Glass (${currentGlassBg.displayName})"
                }
                SettingsRow(
                    title = "Themes",
                    subtitle = themeLabel,
                    icon = Icons.Default.ColorLens,
                    onClick = {
                        haptics.click()
                        showThemeSettingsScreen = true
                    }
                )

                SettingsDivider()

                // Home Screen Widgets & 1:1 Previews
                SettingsRow(
                    title = "Widgets",
                    subtitle = "Pixel DotTiles • 12 Interactive Widgets",
                    icon = Icons.Default.Widgets,
                    trailing = {
                        Text(
                            text = "⠶ Pixel DotTiles",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        haptics.click()
                        showWidgetsScreen = true
                    }
                )

                SettingsDivider()

                // App Launcher Icon
                SettingsRow(
                    title = "App Launcher Icon",
                    subtitle = "Currently using ${currentIconStyle.label} icon",
                    icon = Icons.Default.AutoAwesome,
                    trailing = {
                        Text(
                            text = currentIconStyle.label,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        val nextStyle = if (currentIconStyle == AppIconStyle.DARK) AppIconStyle.LIGHT else AppIconStyle.DARK
                        pendingIconStyle = nextStyle
                        showIconRestartDialog = true
                    }
                )
            }

            // =================================================================
            // 2. WALLETS & CURRENCY
            // =================================================================
            SettingsGroup(title = "WALLETS & CURRENCY") {
                // Primary Currency
                val currencyFlag = if (currentCurrency.flag.isNotBlank()) "${currentCurrency.flag} " else ""
                SettingsRow(
                    title = "Primary Currency",
                    subtitle = "$currencyFlag${currentCurrency.displayName} (${currentCurrency.symbol} ${currentCurrency.code})",
                    icon = Icons.Default.Paid,
                    onClick = { showCurrencyDialog = true }
                )

                SettingsDivider()

                // Budget Cycle Start Day
                val cycleSubtitle = if (budgetCycleStartDay == 1) {
                    "Day 1 of month (Calendar Cycle)"
                } else {
                    "Day $budgetCycleStartDay of every month (Salary Cycle)"
                }
                SettingsRow(
                    title = "Budget Cycle Start Day",
                    subtitle = cycleSubtitle,
                    icon = Icons.Default.CalendarToday,
                    onClick = { showBudgetCycleDialog = true }
                )

                SettingsDivider()

                // Cash Balance
                SettingsRow(
                    title = "Cash Balance",
                    subtitle = CurrencyUtils.formatCurrency(balances?.cash ?: 0.0),
                    icon = Icons.Default.Payments,
                    trailing = {
                        Text(
                            text = "Correct",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { walletToCorrect = "CASH" }
                )

                SettingsDivider()

                // Online Balance
                SettingsRow(
                    title = "Online / Bank Balance",
                    subtitle = CurrencyUtils.formatCurrency(balances?.online ?: 0.0),
                    icon = Icons.Default.AccountBalance,
                    trailing = {
                        Text(
                            text = "Correct",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { walletToCorrect = "ONLINE" }
                )

                SettingsDivider()

                // ATM Withdrawal Flow
                SettingsRow(
                    title = "ATM Withdrawal Flow",
                    subtitle = if (atmMode == AtmWithdrawalMode.SPENDING) "Treated as Spending (Expense)" else "Treated as Internal Transfer (Bank to Cash)",
                    icon = Icons.Default.AccountBalanceWallet,
                    trailing = {
                        Text(
                            text = if (atmMode == AtmWithdrawalMode.SPENDING) "Spending" else "Transfer",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        val next = if (atmMode == AtmWithdrawalMode.SPENDING) AtmWithdrawalMode.TRANSFER else AtmWithdrawalMode.SPENDING
                        walletPrefs.atmWithdrawalMode = next
                        haptics.click()
                        Toast.makeText(context, "ATM mode: ${next.name}", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsDivider()

                // Setup / Reconfigure Wallets
                SettingsRow(
                    title = "Reconfigure Wallets",
                    subtitle = "Set opening cash & online balances",
                    icon = Icons.Default.Edit,
                    onClick = { showWalletSetupDialog = true }
                )
            }

            // =================================================================
            // 3. SECURITY & PRIVACY
            // =================================================================
            SettingsGroup(title = "SECURITY & PRIVACY") {
                // App Lock Switch
                SettingsSwitchRow(
                    title = "Biometric / Device PIN Lock",
                    subtitle = "Require biometric verification when launching Kanri",
                    icon = Icons.Default.Lock,
                    checked = isLockEnabled,
                    onCheckedChange = { enabled ->
                        securityPrefs.isLockEnabled = enabled
                        isLockEnabled = enabled
                    }
                )

                SettingsDivider()

                // Hide Balances (Privacy Mask)
                SettingsSwitchRow(
                    title = "Hide Balances",
                    subtitle = "Obfuscate wallet balances until tapped or revealed",
                    icon = if (isBalanceSpoilerEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    checked = isBalanceSpoilerEnabled,
                    onCheckedChange = { walletPrefs.isBalanceSpoilerEnabled = it }
                )

                SettingsDivider()

                // Privacy Policy
                SettingsRow(
                    title = "Privacy Policy",
                    subtitle = "Zero tracking & 100% offline-first architecture",
                    icon = Icons.Default.Policy,
                    onClick = { showPrivacyPolicyDialog = true }
                )
            }

            // =================================================================
            // 4. RULES & CATEGORIES
            // =================================================================
            SettingsGroup(title = "RULES & CATEGORIES") {
                // Smart Merchant Rules
                SettingsRow(
                    title = "Smart Merchant Rules",
                    subtitle = "${merchantRules.size} merchant category mappings active",
                    icon = Icons.Default.AutoFixHigh,
                    onClick = { showMerchantRulesScreen = true }
                )

                SettingsDivider()

                // Custom Categories Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Categories",
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${customCategories.size} custom categories created",
                            fontSize = 12.sp,
                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showCreateCategoryDialog = true },
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

                if (customCategories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Default categories are active. Tap Add to create custom tags.",
                            fontSize = 12.sp,
                            color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    customCategories.forEach { cat ->
                        SettingsDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = cat.iconName, fontSize = 16.sp)
                                    }
                                }
                                Text(
                                    text = cat.name,
                                    fontSize = 14.sp,
                                    fontFamily = GoogleSansFlex,
                                    fontWeight = FontWeight.Medium,
                                    color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { categoryToEdit = cat }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(onClick = { categoryToDelete = cat }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 5. PERMISSIONS & SYSTEM DETECTION
            // =================================================================
            SettingsGroup(title = "PERMISSIONS & DETECTION") {
                Box(modifier = Modifier.padding(16.dp)) {
                    PermissionsStatusOverview(context = context)
                }
            }

            // =================================================================
            // 6. DATA & BACKUP
            // =================================================================
            SettingsGroup(title = "DATA & BACKUP") {
                // Generate Statement
                SettingsRow(
                    title = "Generate Statement (PDF / CSV)",
                    subtitle = "Export detailed spending report with category breakdowns",
                    icon = Icons.Default.FileDownload,
                    onClick = { showExportStatementSheet = true }
                )

                SettingsDivider()

                // Export Offline Backup
                SettingsRow(
                    title = "Export Offline Backup",
                    subtitle = "Save an encrypted JSON backup file directly to your device",
                    icon = Icons.Default.CloudUpload,
                    onClick = {
                        exportPassphraseInput = ""
                        showExportBackupDialog = true
                    }
                )

                SettingsDivider()

                // Restore Offline Backup
                SettingsRow(
                    title = "Restore Backup File",
                    subtitle = "Restore database and categories from an existing backup file",
                    icon = Icons.Default.CloudDownload,
                    onClick = {
                        importDataLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )

                SettingsDivider()

                // Monthly Automated Statement
                SettingsSwitchRow(
                    title = "Monthly Automated Statement",
                    subtitle = "Automatically compile financial statement on the 1st of each month",
                    icon = Icons.Default.Refresh,
                    checked = isAutoMonthly,
                    onCheckedChange = { checked ->
                        val newSched = if (checked) ExportScheduler.ExportSchedule.MONTHLY else ExportScheduler.ExportSchedule.OFF
                        ExportScheduler.setSchedule(context, newSched)
                        Toast.makeText(context, if (checked) "Monthly automated statement scheduled" else "Automated statement disabled", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsDivider()

                // Replay Intro Walkthrough
                SettingsRow(
                    title = "Replay Intro Walkthrough",
                    subtitle = "Restart the onboarding slides and feature tour",
                    icon = Icons.Default.Replay,
                    onClick = onReplayOnboarding
                )
            }

            // =================================================================
            // 7. ABOUT & SUPPORT
            // =================================================================
            SettingsGroup(title = "ABOUT & SUPPORT") {
                SettingsRow(
                    title = "Kanri Version",
                    subtitle = "v${BuildConfig.VERSION_NAME} (Production Build)",
                    icon = Icons.Default.Info,
                    trailing = {
                        if (isCheckingForUpdates) {
                            Text(
                                text = "Checking...",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Check Updates",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        if (!isCheckingForUpdates) {
                            isCheckingForUpdates = true
                            scope.launch {
                                try {
                                    val info = appUpdateManager.checkForUpdate().getOrNull()
                                    if (info != null) activeUpdateDialogInfo = info
                                    else Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Update check failed", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isCheckingForUpdates = false
                                }
                            }
                        }
                    }
                )

                SettingsDivider()

                SettingsRow(
                    title = "App Health & Crash Logs",
                    subtitle = "Inspect recorded error logs and system health diagnostics",
                    icon = Icons.Default.BugReport,
                    onClick = { showCrashLogsDialog = true }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Suggest a Feature",
                    subtitle = "Share your ideas and feature requests directly",
                    icon = Icons.Default.Lightbulb,
                    onClick = { showSuggestFeatureDialog = true }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Contact Support",
                    subtitle = "Send developer an email query or feedback",
                    icon = Icons.Default.Email,
                    onClick = { showContactDialog = true }
                )
            }

            // =================================================================
            // 8. DANGER ZONE
            // =================================================================
            SettingsGroup(title = "DANGER ZONE", titleColor = MaterialTheme.colorScheme.error) {
                SettingsRow(
                    title = "Reset Opening Balances",
                    subtitle = "Clear Cash & Online opening balances without deleting transactions",
                    icon = Icons.Default.Refresh,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showResetWalletsDialog = true }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Terminate App Process",
                    subtitle = "Force close Kanri background process immediately",
                    icon = Icons.Default.PowerSettingsNew,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showKillAppDialog = true }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Reset Entire App",
                    subtitle = "Permanently wipe database and all user settings",
                    icon = Icons.Default.DeleteForever,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = { showResetDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// =============================================================================
// COMPOSE PREVIEWS FOR EVERY THEME
// =============================================================================
@Preview(name = "Settings - Light Theme", showBackground = true)
@Composable
fun SettingsScreenPreview_Light() {
    KanriTheme(themeMode = ThemeMode.LIGHT) {
        SettingsScreen(onBack = {})
    }
}

@Preview(name = "Settings - Dark Theme", showBackground = true)
@Composable
fun SettingsScreenPreview_Dark() {
    KanriTheme(themeMode = ThemeMode.DARK) {
        SettingsScreen(onBack = {})
    }
}

@Preview(name = "Settings - AMOLED Theme", showBackground = true)
@Composable
fun SettingsScreenPreview_Amoled() {
    KanriTheme(themeMode = ThemeMode.AMOLED) {
        SettingsScreen(onBack = {})
    }
}

@Preview(name = "Settings - Glass Theme", showBackground = true)
@Composable
fun SettingsScreenPreview_Glass() {
    KanriTheme(themeMode = ThemeMode.GLASS) {
        SettingsScreen(onBack = {})
    }
}
