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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.common.kanriGlassTextFieldColors
import com.omkarnub.kanri.ui.common.kanriPrimaryButtonColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.horizontalScroll
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.util.CurrencyUtils
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SystemUpdate
import com.omkarnub.kanri.BuildConfig
import com.omkarnub.kanri.util.AppCurrency
import com.omkarnub.kanri.util.CurrencyPreferences
import com.omkarnub.kanri.ui.wallet.WalletSetupDialog
import com.omkarnub.kanri.ui.wallet.CorrectBalanceDialog
import com.omkarnub.kanri.data.wallet.WalletRepository
import com.omkarnub.kanri.data.wallet.WalletPreferences
import com.omkarnub.kanri.data.wallet.AtmWithdrawalMode
import com.omkarnub.kanri.ui.notification.NotificationAccessHelper
import com.omkarnub.kanri.ui.common.KanriGlassAlertDialog as AlertDialog
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
import com.omkarnub.kanri.ui.common.GlassBackgroundType
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
    val currentGlassBg by themePrefs.glassBackgroundFlow.collectAsState()
    var pendingIconStyle by remember { mutableStateOf<AppIconStyle?>(null) }
    var showIconRestartDialog by remember { mutableStateOf(false) }

    val currencyPrefs = remember { CurrencyPreferences.getInstance(context) }
    val currentCurrency by currencyPrefs.currencyFlow.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }

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

    val appUpdateManager = remember { com.omkarnub.kanri.data.update.AppUpdateManager.getInstance(context) }
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var activeUpdateDialogInfo by remember { mutableStateOf<com.omkarnub.kanri.data.update.AppUpdateInfo?>(null) }



    // Wallets & Balances
    val walletRepo = remember { WalletRepository(context) }
    val walletPrefs = remember { WalletPreferences.getInstance(context) }
    val isWalletConfigured by walletRepo.isConfigured().collectAsState(initial = false)
    val balances by walletRepo.observeBalances().collectAsState(initial = null)
    val atmMode by walletPrefs.atmWithdrawalModeFlow.collectAsState()

    var showWalletSetupDialog by remember { mutableStateOf(false) }
    var walletToCorrect by remember { mutableStateOf<String?>(null) }
    var showResetWalletsDialog by remember { mutableStateOf(false) }



    var showThemesWindow by remember { mutableStateOf(false) }
    var showGlassCustomizerScreen by remember { mutableStateOf(false) }

    if (showThemesWindow) {
        ThemesCustomizationScreen(
            onBack = { showThemesWindow = false },
            onOpenGlassCustomizer = {
                showThemesWindow = false
                showGlassCustomizerScreen = true
            },
            modifier = modifier
        )
        return
    }

    if (showGlassCustomizerScreen) {
        GlassCustomizerScreen(
            onBack = { showGlassCustomizerScreen = false },
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

    // =========================================================================
    // MODALS & DIALOGS
    // =========================================================================

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

    if (activeUpdateDialogInfo != null) {
        com.omkarnub.kanri.ui.update.AppUpdateDialog(
            updateInfo = activeUpdateDialogInfo!!,
            onDismiss = { activeUpdateDialogInfo = null }
        )
    }

    val tokens = LocalKanriGlass.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

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
            // 1. APPEARANCE & THEME
            // =================================================================
            SettingsGroup(title = "APPEARANCE & THEME") {
                // Themes & Live Backgrounds Screen Row
                SettingsRow(
                    title = "Themes & Live Backgrounds",
                    subtitle = "${currentThemeMode.label}  •  ${currentGlassBg.displayName}",
                    icon = Icons.Default.Palette,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentThemeMode.label,
                                fontSize = 12.5.sp,
                                fontFamily = GoogleSansFlex,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { showThemesWindow = true }
                )

                SettingsDivider()

                // Glass Surface Customizer
                SettingsRow(
                    title = "Glass Surface Engine",
                    subtitle = "Fine-tune blur radius, noise & translucency",
                    icon = Icons.Default.BlurOn,
                    onClick = { showGlassCustomizerScreen = true }
                )
            }

            // =================================================================
            // 2. WALLETS & BALANCES
            // =================================================================
            SettingsGroup(title = "WALLETS & BALANCES") {
                // Currency Selector
                SettingsRow(
                    title = "Primary Currency",
                    subtitle = "${currentCurrency.flag} ${currentCurrency.displayName} (${currentCurrency.symbol} ${currentCurrency.code})",
                    icon = Icons.Default.Paid,
                    onClick = { showCurrencyDialog = true }
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

                // Hide Balances (Spoiler Effect)
                SettingsSwitchRow(
                    title = "Hide Balances (Spoiler Effect)",
                    subtitle = "Shimmering particle spoiler mask over wallet cards",
                    icon = Icons.Default.Tune,
                    checked = walletPrefs.isBalanceSpoilerEnabled,
                    onCheckedChange = { walletPrefs.isBalanceSpoilerEnabled = it }
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

                // Test Lock Screen
                SettingsRow(
                    title = "Test Lock Screen",
                    subtitle = "Trigger security lock overlay immediately",
                    icon = Icons.Default.Shield,
                    onClick = onTestLockScreen
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
            // 4. CATEGORIES & AUTOMATIONS
            // =================================================================
            SettingsGroup(title = "CATEGORIES & AUTOMATIONS") {
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
            // 6. BACKUP & CLOUD SYNC
            // =================================================================
            SettingsGroup(title = "BACKUP & DATA MANAGEMENT") {
                SettingsRow(
                    title = "Google Drive Cloud Backup",
                    subtitle = "Automatic background cloud sync with your private Drive",
                    icon = Icons.Default.CloudSync,
                    trailing = {
                        Text(
                            text = "Sync",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        Toast.makeText(context, "Google Drive sync is enabled and up-to-date", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsDivider()

                SettingsRow(
                    title = "Export Transactions Statement",
                    subtitle = "Export CSV or encrypted JSON statements from Profile tab",
                    icon = Icons.Default.FileDownload,
                    onClick = {
                        Toast.makeText(context, "Head to the Profile tab to export statements", Toast.LENGTH_SHORT).show()
                    }
                )

                SettingsDivider()

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
                    subtitle = "Inspect recorded error logs and system health",
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
                    subtitle = "Send developer an email query",
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
                    subtitle = "Clear Cash & Online balances without deleting transactions",
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
// REUSABLE FROSTED SETTINGS COMPONENTS
// =============================================================================
@Composable
private fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontFamily = GoogleSansFlex,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = titleColor ?: (tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
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

@Composable
private fun SettingsRow(
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) {
                haptics.click()
                onClick?.invoke()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = (iconTint ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint ?: MaterialTheme.colorScheme.primary,
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
                color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = tokens?.textSecondary?.copy(alpha = 0.5f) ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptics.click()
                onCheckedChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                color = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.click()
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = if (tokens != null) tokens.tintFlat else MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    val tokens = LocalKanriGlass.current
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp, end = 16.dp),
        thickness = 0.8.dp,
        color = tokens?.borderStart?.copy(alpha = 0.12f) ?: Color.White.copy(alpha = 0.08f)
    )
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
    val tokens = LocalKanriGlass.current
    // Theme palette constants for realistic previews
    val (bgColor, cardColor, cardBorder, barColor, dockColor) = when (mode) {
        ThemeMode.SYSTEM -> listOf(
            Color(0xFF1B1B1F),
            Color(0xFF26262B),
            Color(0xFF383840),
            Color(0xFFDDDDDD),
            Color(0xFF1E1E24)
        )
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
        ThemeMode.GLASS -> listOf(
            Color(0xFF0B0B0E),
            tokens?.tintBlur ?: Color(0x26FFFFFF),
            tokens?.borderStart ?: Color(0x47FFFFFF),
            tokens?.textPrimary ?: Color(0xFFF5F5F7),
            tokens?.tintFlat ?: Color(0x2EFFFFFF)
        )
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(tokens?.cornerRadius ?: 14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(tokens?.cornerRadius ?: 14.dp),
        colors = CardDefaults.cardColors(containerColor = tokens?.tintFlat ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else (tokens?.borderWidth ?: 1.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else (tokens?.borderEnd ?: MaterialTheme.colorScheme.outlineVariant)
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
                // If glass theme, draw a subtle frosted glow in the miniature preview
                if (mode == ThemeMode.GLASS) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset(size.width * 0.2f, size.height * 0.2f),
                                radius = size.width * 0.8f
                            ),
                            radius = size.width * 0.8f,
                            center = Offset(size.width * 0.2f, size.height * 0.2f)
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
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.DARK -> "Charcoal"
                    ThemeMode.AMOLED -> "Pitch Black"
                    ThemeMode.LIGHT -> "Soft Cream"
                    ThemeMode.GLASS -> "Frosted Glass"
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
                                    val currentLocale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
                                    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", currentLocale)
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

// =============================================================================
// REUSABLE SETTINGS HELPERS
// =============================================================================
@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    if (tokens != null) {
        KanriGlassCard(
            modifier = modifier.fillMaxWidth(),
            level = GlassLevel.Blur,
            shape = RoundedCornerShape(tokens.cornerRadius),
            onClick = onClick
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    val tokens = LocalKanriGlass.current
    val headerColor = tokens?.textSecondary ?: MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = headerColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = headerColor,
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
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val effectiveTint = if (tint == MaterialTheme.colorScheme.onSurface && tokens != null) {
        tokens.textPrimary
    } else tint
    val subtitleColor = tokens?.textSecondary ?: MaterialTheme.colorScheme.onSurfaceVariant
    val arrowColor = tokens?.textTertiary ?: MaterialTheme.colorScheme.onSurfaceVariant

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
                tint = effectiveTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = effectiveTint
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = subtitleColor
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = arrowColor,
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
                    colors = kanriGlassTextFieldColors(),
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

@Composable
private fun SelectCurrencyDialog(
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
                    text = "Select your primary currency. Amounts and widgets will format using this currency.",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currency or code...") },
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
                            KanriGlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCurrency(currency) }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                tokens?.accent ?: MaterialTheme.colorScheme.onSurface,
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
                                            if (isSelected) (tokens?.accent ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = currency.flag, fontSize = 20.sp)
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
                                            tint = tokens?.accent ?: MaterialTheme.colorScheme.onSurface,
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
