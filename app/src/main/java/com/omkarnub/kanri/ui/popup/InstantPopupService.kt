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
import com.omkarnub.kanri.MainActivity
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
            sourceType = sourceType
        )

        return START_NOT_STICKY
    }

    private fun showFloatingOverlay(
        txId: Long,
        amount: Double,
        isDebit: Boolean,
        counterparty: String,
        bank: String?,
        sourceType: String
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

                    LaunchedEffect(Unit) {
                        val cats = withContext(Dispatchers.IO) {
                            db.categoryDao().getAllCategoriesSync()
                        }
                        categories = cats

                        // Smart pre-resolution of category & note
                        val (resolvedCatId, resolvedNote) = withContext(Dispatchers.IO) {
                            var foundId: Long? = null
                            var foundNote = ""
                            if (txId > 0L) {
                                val tx = db.transactionDao().getTransactionById(txId)
                                foundId = tx?.categoryId
                                foundNote = tx?.notes ?: ""
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
                            Pair(foundId, foundNote)
                        }
                        initialCategoryId = resolvedCatId
                        initialNote = resolvedNote
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
                                onDone = { catId, note ->
                                    dismissWithAnimation {
                                        serviceScope.launch(Dispatchers.IO) {
                                            if (txId > 0L) {
                                                val cleanNote = note.trim().ifEmpty { null }
                                                if (catId != null) {
                                                    db.transactionDao().updateCategoryAndNotes(txId, catId, cleanNote)
                                                    if (counterparty.isNotBlank()) {
                                                        db.transactionDao().updateCategoryForCounterparty(counterparty, catId)
                                                        db.categoryDao().setMapping(
                                                            CounterpartyCategoryMapEntity(
                                                                counterparty = counterparty.trim().lowercase(),
                                                                categoryId = catId
                                                            )
                                                        )
                                                    }
                                                } else if (cleanNote != null) {
                                                    db.transactionDao().updateNotes(txId, cleanNote)
                                                }
                                            }
                                        }
                                    }
                                },
                                onSplitBill = {
                                    dismissWithAnimation {
                                        val cleanMerchant = BrandFormatter.cleanMerchantName(counterparty).ifBlank { "Expense" }
                                        com.omkarnub.kanri.widget.KanriWidgetActions.triggerSplitBill(amount, cleanMerchant)

                                        val openIntent = (packageManager.getLaunchIntentForPackage(packageName)
                                            ?: Intent(this@InstantPopupService, MainActivity::class.java)).apply {
                                            action = com.omkarnub.kanri.widget.KanriWidgetActions.ACTION_SPLIT_BILL
                                            putExtra(com.omkarnub.kanri.widget.KanriWidgetActions.EXTRA_AMOUNT, amount)
                                            putExtra(com.omkarnub.kanri.widget.KanriWidgetActions.EXTRA_DESCRIPTION, cleanMerchant)
                                            if (txId > 0L) {
                                                putExtra(InstantPopupService.EXTRA_TRANSACTION_ID, txId)
                                            }
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                        }
                                        startActivity(openIntent)
                                    }
                                },
                                onOpenInApp = {
                                    dismissWithAnimation {
                                        val openIntent = (packageManager.getLaunchIntentForPackage(packageName)
                                            ?: Intent(this@InstantPopupService, MainActivity::class.java)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        }
                                        startActivity(openIntent)
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
}
