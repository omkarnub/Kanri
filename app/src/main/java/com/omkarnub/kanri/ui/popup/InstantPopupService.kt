package com.omkarnub.kanri.ui.popup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.omkarnub.kanri.R
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.ui.home.formatCurrency
import android.util.Log
import com.omkarnub.kanri.ui.theme.KanriMotion
import com.omkarnub.kanri.ui.theme.KanriTheme
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.BrandFormatter
import com.omkarnub.kanri.widget.KanriWidgetsUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstantPopupService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var floatingComposeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    companion object {
        private const val TAG = "InstantPopupService"
        const val EXTRA_TRANSACTION_ID = "extra_tx_id"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_IS_DEBIT = "extra_is_debit"
        const val EXTRA_COUNTERPARTY = "extra_counterparty"
        const val EXTRA_BANK = "extra_bank"
        const val EXTRA_SOURCE_TYPE = "extra_source_type"
        const val EXTRA_RAW_SMS = "extra_raw_sms"

        private const val CHANNEL_ID = "kanri_overlay_service_channel"
        private const val NOTIFICATION_ID = 4001

        fun triggerTestPopup(context: Context, isDebit: Boolean = true) {
            val intent = Intent(context, InstantPopupService::class.java).apply {
                putExtra(EXTRA_TRANSACTION_ID, 9999L)
                putExtra(EXTRA_AMOUNT, if (isDebit) 380.00 else 1250.00)
                putExtra(EXTRA_IS_DEBIT, isDebit)
                putExtra(EXTRA_COUNTERPARTY, if (isDebit) "Swiggy" else "Rahul Sharma")
                putExtra(EXTRA_BANK, if (isDebit) "HDFC Bank" else "Google Pay")
                putExtra(EXTRA_SOURCE_TYPE, "UPI")
                putExtra(EXTRA_RAW_SMS, if (isDebit) "Sent Rs.380.00 to Swiggy on 09-Nov-26 via UPI. Avl Bal Rs.14,620.00" else "Received Rs.1250.00 from Rahul. Avl Bal Rs.28,750.00")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startAsForeground()
    }

    private fun startAsForeground() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kanri Overlay")
            .setContentText("Detecting payment...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val txId = intent.getLongExtra(EXTRA_TRANSACTION_ID, -1L)
        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val isDebit = intent.getBooleanExtra(EXTRA_IS_DEBIT, true)
        val counterparty = intent.getStringExtra(EXTRA_COUNTERPARTY) ?: ""
        val bank = intent.getStringExtra(EXTRA_BANK)
        val sourceType = intent.getStringExtra(EXTRA_SOURCE_TYPE) ?: "UPI"
        val rawSms = intent.getStringExtra(EXTRA_RAW_SMS) ?: ""

        val secPrefs = com.omkarnub.kanri.data.security.SecurityPreferences.getInstance(this)
        if (secPrefs.isLockEnabled && com.omkarnub.kanri.data.security.SecurityManager.isAppLocked.value) {
            Log.w(TAG, "App is locked. Suppressing payment overlay.")
            stopSelf()
            return START_NOT_STICKY
        }

        if (txId <= 0L && amount <= 0.0) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        showFloatingOverlay(
            txId = txId,
            amount = amount,
            isDebit = isDebit,
            counterparty = counterparty,
            bank = bank,
            sourceType = sourceType,
            rawSms = rawSms
        )

        return START_NOT_STICKY
    }

    private fun showFloatingOverlay(
        txId: Long,
        amount: Double,
        isDebit: Boolean,
        counterparty: String,
        bank: String?,
        sourceType: String,
        rawSms: String = ""
    ) {
        removeExistingOverlay()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) {
            stopSelf()
            return
        }

        val overlayOwner = OverlayLifecycleOwner()
        lifecycleOwner = overlayOwner

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            val isGlassTheme = ThemePreferences.getInstance(this@InstantPopupService).themeMode == ThemeMode.GLASS
            if (isGlassTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                val density = resources.displayMetrics.density
                blurBehindRadius = (45 * density).toInt().coerceAtLeast(40)
            }
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(overlayOwner)
            setViewTreeViewModelStoreOwner(overlayOwner)
            setViewTreeSavedStateRegistryOwner(overlayOwner)

            setContent {
                KanriTheme {
                    val db = KanriDatabase.getDatabase(context)
                    val themePrefs = remember {
                        ThemePreferences.getInstance(context)
                    }
                    val isGlass = themePrefs.themeMode == ThemeMode.GLASS

                    var isSheetOpen by remember { mutableStateOf(false) }
                    val coroutineScope = rememberCoroutineScope()

                    fun dismissWithAnimation(onComplete: () -> Unit = {}) {
                        if (!isSheetOpen) return
                        coroutineScope.launch {
                            isSheetOpen = false
                            delay(160)
                            onComplete()
                            stopSelf()
                        }
                    }

                    LaunchedEffect(Unit) {
                        delay(30)
                        isSheetOpen = true
                    }

                    var categories by remember {
                        mutableStateOf<List<CategoryEntity>>(emptyList())
                    }
                    var initialCategoryId by remember {
                        mutableStateOf<Long?>(null)
                    }
                    var initialNote by remember {
                        mutableStateOf("")
                    }
                    var currentBalance by remember {
                        mutableStateOf<Double?>(null)
                    }

                    LaunchedEffect(Unit) {
                        val result = withContext(Dispatchers.IO) {
                            val allCats = db.categoryDao().getAllCategoriesSync()
                            val recentTxs = db.transactionDao().getAllTransactionsWithCategorySync().take(150)
                            val frequencyMap = recentTxs.mapNotNull { it.category?.id }.groupingBy { it }.eachCount()

                            // Contextual time-of-day scoring for intelligent category ranking
                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            val timeAffinity: (String) -> Int = { name ->
                                val lower = name.lowercase()
                                when (hour) {
                                    in 6..10 -> when {
                                        lower.contains("food") || lower.contains("dining") || lower.contains("cafe") -> 35
                                        lower.contains("auto") || lower.contains("taxi") || lower.contains("transit") || lower.contains("fuel") -> 30
                                        lower.contains("grocer") -> 20
                                        else -> 0
                                    }
                                    in 11..15 -> when {
                                        lower.contains("food") || lower.contains("dining") -> 35
                                        lower.contains("grocer") -> 20
                                        lower.contains("shopping") -> 15
                                        else -> 0
                                    }
                                    in 16..20 -> when {
                                        lower.contains("auto") || lower.contains("taxi") || lower.contains("transit") || lower.contains("fuel") -> 30
                                        lower.contains("grocer") -> 25
                                        lower.contains("shopping") -> 20
                                        lower.contains("food") || lower.contains("dining") -> 20
                                        else -> 0
                                    }
                                    else -> when {
                                        lower.contains("food") || lower.contains("dining") -> 35
                                        lower.contains("entertainment") -> 25
                                        else -> 0
                                    }
                                }
                            }

                            val dynamicallyRankedCats = allCats.sortedWith(
                                compareByDescending<CategoryEntity> { (frequencyMap[it.id] ?: 0) * 10 + timeAffinity(it.name) }
                                    .thenBy { it.name }
                            )

                            // Smart pre-resolution of category, note & current balance
                            var foundId: Long? = null
                            var foundNote = ""
                            var foundBal: Double? = null

                            if (rawSms.isNotBlank()) {
                                foundBal = com.omkarnub.kanri.data.parser.SmsParser.extractBalance(rawSms)
                            }

                            if (txId > 0L) {
                                val tx = db.transactionDao().getTransactionById(txId)
                                foundId = tx?.categoryId
                                foundNote = tx?.notes ?: ""
                                if (foundBal == null && tx?.rawSms?.isNotBlank() == true) {
                                    foundBal = com.omkarnub.kanri.data.parser.SmsParser.extractBalance(tx.rawSms)
                                }
                            }
                            if (foundId == null && counterparty.isNotBlank()) {
                                val lower = counterparty.trim().lowercase()
                                val rule = db.categoryDao().findSmartRuleForCounterparty(lower)
                                    ?: db.categoryDao().getMappingForCounterparty(lower)
                                foundId = rule?.categoryId
                            }
                            if (foundId == null && counterparty.isNotBlank()) {
                                val lower = counterparty.lowercase()
                                when {
                                    lower.contains("swiggy") || lower.contains("zomato") || lower.contains("food") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("mcdonald") -> {
                                        foundId = db.categoryDao().getCategoryByName("Food & Dining")?.id
                                    }
                                    lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("taxi") -> {
                                        foundId = db.categoryDao().getCategoryByName("Auto / Taxi Fare")?.id
                                    }
                                    lower.contains("blinkit") || lower.contains("zepto") || lower.contains("instamart") || lower.contains("grocery") -> {
                                        foundId = db.categoryDao().getCategoryByName("Groceries")?.id
                                    }
                                }
                            }

                            if (foundBal == null && !bank.isNullOrBlank()) {
                                for (item in recentTxs) {
                                    val t = item.transaction
                                    if (t.bank.equals(bank, ignoreCase = true) && t.rawSms.isNotBlank()) {
                                        val b = com.omkarnub.kanri.data.parser.SmsParser.extractBalance(t.rawSms)
                                        if (b != null) {
                                            foundBal = if (t.id == txId) b else if (isDebit) (b - amount).coerceAtLeast(0.0) else b + amount
                                            break
                                        }
                                    }
                                }
                            }

                            if (foundBal == null) {
                                for (item in recentTxs) {
                                    val t = item.transaction
                                    if (t.rawSms.isNotBlank()) {
                                        val b = com.omkarnub.kanri.data.parser.SmsParser.extractBalance(t.rawSms)
                                        if (b != null) {
                                            foundBal = if (isDebit) (b - amount).coerceAtLeast(0.0) else b + amount
                                            break
                                        }
                                    }
                                }
                            }

                            PreResolutionResult(dynamicallyRankedCats, foundId, foundNote, foundBal)
                        }

                        categories = result.categories
                        initialCategoryId = result.categoryId
                        initialNote = result.note
                        currentBalance = result.balance
                    }

                    val scrimColor = if (isGlass) {
                        Color.Black.copy(alpha = 0.45f)
                    } else {
                        Color.Black.copy(alpha = 0.55f)
                    }

                    // Scrim backdrop with fade entrance & exit animation
                    AnimatedVisibility(
                        visible = isSheetOpen,
                        enter = fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)),
                        exit = fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scrimColor)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    dismissWithAnimation()
                                }
                        )
                    }

                    // Floating card with exact pop-up and pop-down animation from AddExpenseIncomePopup.kt
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AnimatedVisibility(
                            visible = isSheetOpen,
                            enter = fadeIn(animationSpec = tween(180, easing = KanriMotion.EaseOut)) +
                                    scaleIn(
                                        initialScale = 0.90f,
                                        animationSpec = KanriMotion.SnappySpring
                                    ) +
                                    slideInVertically(
                                        initialOffsetY = { (it * 0.10f).toInt() },
                                        animationSpec = KanriMotion.SmoothSlideSpring
                                    ),
                            exit = fadeOut(animationSpec = tween(140, easing = KanriMotion.EaseOut)) +
                                    scaleOut(
                                        targetScale = 0.94f,
                                        animationSpec = tween(140, easing = KanriMotion.EaseOut)
                                    ) +
                                    slideOutVertically(
                                        targetOffsetY = { (it * 0.08f).toInt() },
                                        animationSpec = tween(140, easing = KanriMotion.EaseOut)
                                    )
                        ) {
                            InstantPopupCard(
                                amount = amount,
                                isDebit = isDebit,
                                counterparty = counterparty,
                                bank = bank,
                                sourceType = sourceType,
                                categories = categories,
                                initialCategoryId = initialCategoryId,
                                initialNote = initialNote,
                                currentBalance = currentBalance,
                                onDone = { catId, note ->
                                    withContext(Dispatchers.IO) {
                                            if (txId > 0L) {
                                                val cleanNote = note.trim().ifEmpty { null }
                                                if (catId != null) {
                                                    db.transactionDao().updateCategoryAndNotes(txId, catId, cleanNote)
                                                    if (counterparty.isNotBlank()) {
                                                        db.transactionDao().updateCategoryForCounterparty(counterparty, catId)
                                                        db.categoryDao().setMapping(
                                                            CounterpartyCategoryMapEntity(
                                                                counterparty = counterparty.trim().lowercase(java.util.Locale.ROOT),
                                                                categoryId = catId
                                                            )
                                                        )
                                                    }
                                                } else if (cleanNote != null) {
                                                    db.transactionDao().updateNotes(txId, cleanNote)
                                                }
                                            }
                                            com.omkarnub.kanri.widget.KanriWidgetsUpdater.updateAllWidgets(applicationContext)
                                    }
                                },
                                onSplitBill = {
                                    dismissWithAnimation {
                                        val cleanMerchant = BrandFormatter.cleanMerchantName(counterparty).ifBlank { "Expense" }
                                        com.omkarnub.kanri.widget.KanriWidgetActions.triggerSplitBill(amount, cleanMerchant)

                                        val openIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(this@InstantPopupService).apply {
                                            action = com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_SPLIT_BILL
                                            putExtra(com.omkarnub.kanri.widget.KanriWidgetActions.EXTRA_AMOUNT, amount)
                                            putExtra(com.omkarnub.kanri.widget.KanriWidgetActions.EXTRA_DESCRIPTION, cleanMerchant)
                                            if (txId > 0L) {
                                                putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, txId)
                                            }
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        }
                                        try {
                                            startActivity(openIntent)
                                        } catch (e: Exception) {
                                            Log.e("InstantPopupService", "Failed to open app for split bill", e)
                                        }
                                    }
                                },
                                onOpenInApp = {
                                    dismissWithAnimation {
                                        val openIntent = com.omkarnub.kanri.ui.theme.AppIconManager.createLauncherIntent(this@InstantPopupService).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        try {
                                            startActivity(openIntent)
                                        } catch (e: Exception) {
                                            Log.e("InstantPopupService", "Failed to open app", e)
                                        }
                                    }
                                },
                                onDismiss = {
                                    dismissWithAnimation()
                                }
                            )
                        }
                    }
                }
            }
        }

        floatingComposeView = composeView
        try {
            windowManager?.addView(composeView, params)
            overlayOwner.start()
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun removeExistingOverlay() {
        floatingComposeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingComposeView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeExistingOverlay()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kanri Instant Overlay Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Runs the instant floating transaction overlay"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val appViewModelStore = ViewModelStore()

        init {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun start() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            appViewModelStore.clear()
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = appViewModelStore
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    }

    private data class PreResolutionResult(
        val categories: List<CategoryEntity>,
        val categoryId: Long?,
        val note: String,
        val balance: Double?
    )
}
