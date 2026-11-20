package com.omkarnub.kanri.ui.common

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omkarnub.kanri.ui.theme.GlassCapabilities
import com.omkarnub.kanri.ui.theme.KanriMotion
import com.omkarnub.kanri.ui.theme.ThemePreferences

enum class GlassBackgroundType(val id: String, val displayName: String, val fileName: String) {
    AURORA("aurora", "Gemini Aurora", "aurora.html"),
    GRAIN_GRADIENT("grain_gradient", "Grain Gradient", "grain_gradient.html"),
    HERO_GEOMETRIC("hero_geometric", "Hero Geometric", "hero_geometric.html"),
    PRISM_GRADIENT("prism_gradient", "Prism Swirl", "prism_gradient.html"),
    CLOUD_SHADER("cloud_shader", "Cloudscape", "cloud_shader.html"),
    INK_RIDGES("ink_ridges", "Ink Ridges", "ink_ridges.html"),
    FUJI_SAKURA("fuji_sakura", "Fuji & Sakura", "fuji_sakura.html"),
    SUMI_E_MIST("sumi_e_mist", "Sumi-e Mist", "sumi_e_mist.html"),
    THISTLE_PORTICO("thistle_portico", "Thistle Portico", "thistle_portico.html"),
    SUNSET_MEADOW("sunset_meadow", "Still Sunset Meadow", "sunset_meadow.html"),
    TWILIGHT_MEADOW("twilight_meadow", "Twilight Meadow", "twilight_meadow.html"),
    TOKYO_NIGHT("tokyo_night", "Tokyo Night", "tokyo_night.html"),
    PIXEL_KITCHEN("pixel_kitchen", "Pixel Kitchen", "pixel_kitchen.html"),
    SNOWY_MOUNTAIN_ROAD("snowy_mountain_road", "Snowy Mountain Road", "snowy_mountain_road.html"),
    SUNLIT_BALCONY("sunlit_balcony", "Sunlit Balcony", "sunlit_balcony.html"),
    ALPINE_VALLEY("alpine_valley", "Alpine Valley", "alpine_valley.html"),
    SHIGANSHINA_WINDMILL("shiganshina_windmill", "Shiganshina Windmill", "shiganshina_windmill.html"),
    CHIBI_COFFEE_SHOP("chibi_coffee_shop", "Chibi Coffee Shop", "chibi_coffee_shop.html"),
    FUJI_TORII("fuji_torii", "Fuji Torii Gate", "fuji_torii.html"),
    CHERRY_CANYON("cherry_canyon", "Cherry Blossom Canyon", "cherry_canyon.html"),
    PIXEL_MISTY_ALLEY("pixel_misty_alley", "Pixel Misty Alley", "pixel_misty_alley.html"),
    GLOWING_BAMBOO_FOREST("glowing_bamboo_forest", "Glowing Bamboo Forest", "glowing_bamboo_forest.html");

    companion object {
        fun fromId(id: String?): GlassBackgroundType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: SUMI_E_MIST
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GlassShaderBackground(
    backgroundType: GlassBackgroundType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val isAnimationEnabled by themePrefs.animateBackgroundFlow.collectAsState()
    val isMotionCapable = GlassCapabilities.isMotionEnabled()

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isPowerSaveMode by remember { mutableStateOf(powerManager?.isPowerSaveMode == true) }
    var isScreenOff by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                        isPowerSaveMode = powerManager?.isPowerSaveMode == true
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        isScreenOff = true
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        isScreenOff = false
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
        }
    }

    val isUserInteracting by com.omkarnub.kanri.MainActivity.isUserInteracting
    val shouldAnimate = isAnimationEnabled && isMotionCapable && !isPowerSaveMode && !isScreenOff && !isUserInteracting

    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewInstance?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewInstance?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.stopLoading()
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    val crossfadeAlpha by animateFloatAsState(
        targetValue = if (isAnimationEnabled) 1f else 0.94f,
        animationSpec = tween(durationMillis = 240, easing = KanriMotion.EaseOut),
        label = "bg_crossfade"
    )

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewInstance = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, false)
                }
                overScrollMode = View.OVER_SCROLL_NEVER
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mediaPlaybackRequiresUserGesture = false
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val url = request?.url?.toString().orEmpty()
                        return !url.startsWith("file:///android_asset/")
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: android.webkit.RenderProcessGoneDetail?
                    ): Boolean {
                        // Prevent host app termination if WebView render process runs out of memory
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject 30 FPS RAF cap and zero-wakeup pause mechanism
                        view?.evaluateJavascript(
                            """
                            (function() {
                                if (window.__rafThrottled) return;
                                window.__rafThrottled = true;
                                window.__kanriPause = false;
                                let pendingCbs = [];
                                const origRAF = window.requestAnimationFrame;
                                let lastT = 0;
                                const interval = 1000 / 30;

                                window.__kanriSetPause = function(pause) {
                                    const wasPaused = window.__kanriPause;
                                    window.__kanriPause = !!pause;
                                    if (wasPaused && !pause && pendingCbs.length > 0) {
                                        const list = pendingCbs.slice();
                                        pendingCbs = [];
                                        list.forEach(function(cb) { origRAF(cb); });
                                    }
                                };

                                window.requestAnimationFrame = function(cb) {
                                    if (window.__kanriPause) {
                                        pendingCbs.push(cb);
                                        return 0;
                                    }
                                    return origRAF(function(time) {
                                        if (window.__kanriPause) {
                                            pendingCbs.push(cb);
                                            return;
                                        }
                                        if (time - lastT < interval) {
                                            return origRAF(cb);
                                        }
                                        lastT = time;
                                        cb(time);
                                    });
                                };
                            })();
                            """.trimIndent(),
                            null
                        )
                        val pause = !shouldAnimate
                        view?.evaluateJavascript("window.__kanriSetPause?.($pause);", null)
                    }
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isClickable = false
                isFocusable = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        view.performClick()
                    }
                    true
                }
                loadUrl("file:///android_asset/backgrounds/${backgroundType.fileName}")
            }
        },
        update = { webView ->
            webViewInstance = webView
            val targetUrl = "file:///android_asset/backgrounds/${backgroundType.fileName}"
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
            val pause = !shouldAnimate
            webView.evaluateJavascript("window.__kanriSetPause?.($pause);", null)
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = crossfadeAlpha
            }
    )
}
