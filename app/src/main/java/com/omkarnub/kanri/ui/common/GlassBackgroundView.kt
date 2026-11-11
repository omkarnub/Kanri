package com.omkarnub.kanri.ui.common

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omkarnub.kanri.ui.theme.GlassCapabilities

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
    val isMotion = GlassCapabilities.isMotionEnabled()

    if (!isMotion) {
        // Fallback for prefers-reduced-motion
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF020617))
                    )
                )
        )
        return
    }

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

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webViewInstance = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = false
                    allowFileAccess = true
                    allowContentAccess = false
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mediaPlaybackRequiresUserGesture = false
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                isClickable = false
                isFocusable = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { _, _ -> true }
                loadUrl("file:///android_asset/backgrounds/${backgroundType.fileName}")
            }
        },
        update = { webView ->
            webViewInstance = webView
            val targetUrl = "file:///android_asset/backgrounds/${backgroundType.fileName}"
            if (webView.url != targetUrl) {
                webView.loadUrl(targetUrl)
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
        modifier = modifier.fillMaxSize()
    )
}
