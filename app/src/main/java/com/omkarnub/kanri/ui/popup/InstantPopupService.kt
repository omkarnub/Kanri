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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.KanriDatabase
import com.omkarnub.kanri.ui.theme.KanriTheme
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
        const val EXTRA_TRANSACTION_ID = "extra_tx_id"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_IS_DEBIT = "extra_is_debit"
        const val EXTRA_COUNTERPARTY = "extra_counterparty"
        const val EXTRA_BANK = "extra_bank"
        const val EXTRA_SOURCE_TYPE = "extra_source_type"

        private const val CHANNEL_ID = "kanri_overlay_service_channel"
        private const val NOTIFICATION_ID = 4001
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
            .setSmallIcon(R.mipmap.ic_launcher)
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
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 110 // Positioned right below the status bar like a heads-up notification
        }

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(overlayOwner)
            setViewTreeViewModelStoreOwner(overlayOwner)
            setViewTreeSavedStateRegistryOwner(overlayOwner)

            setContent {
                KanriTheme {
                    val db = KanriDatabase.getDatabase(context)
                    var categories by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf<List<com.omkarnub.kanri.data.db.CategoryEntity>>(emptyList())
                    }

                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        val cats = withContext(Dispatchers.IO) {
                            db.categoryDao().getAllCategoriesSync()
                        }
                        categories = cats
                    }

                    InstantPopupCard(
                        amount = amount,
                        isDebit = isDebit,
                        counterparty = counterparty,
                        bank = bank,
                        sourceType = sourceType,
                        categories = categories,
                        autoDismissSeconds = 10,
                        onCategorySelected = { catId ->
                            serviceScope.launch(Dispatchers.IO) {
                                if (txId > 0L) {
                                    db.transactionDao().updateCategoryId(txId, catId)
                                    if (counterparty.isNotBlank()) {
                                        db.transactionDao().updateCategoryForCounterparty(counterparty, catId)
                                        db.categoryDao().setMapping(
                                            CounterpartyCategoryMapEntity(
                                                counterparty = counterparty.trim().lowercase(),
                                                categoryId = catId
                                            )
                                        )
                                    }
                                }
                                delay(1200)
                                withContext(Dispatchers.Main) {
                                    stopSelf()
                                }
                            }
                        },
                        onOpenInApp = {
                            val openIntent = (packageManager.getLaunchIntentForPackage(packageName)
                                ?: Intent(this@InstantPopupService, MainActivity::class.java)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            startActivity(openIntent)
                            stopSelf()
                        },
                        onDismiss = {
                            stopSelf()
                        }
                    )
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
