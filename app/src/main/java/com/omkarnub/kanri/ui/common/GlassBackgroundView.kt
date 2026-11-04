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
    AURORA("aurora", "Gradient Aura", "aurora.html"),
    GRAIN_GRADIENT("grain_gradient", "Grain Gradient", "grain_gradient.html"),
    HERO_GEOMETRIC("hero_geometric", "Hero Geometric", "hero_geometric.html"),
    PRISM_GRADIENT("prism_gradient", "Prism Swirl", "prism_gradient.html"),
    CLOUD_SHADER("cloud_shader", "Cloudscape", "cloud_shader.html"),
    INK_RIDGES("ink_ridges", "Ink Ridges", "ink_ridges.html"),
    FUJI_SAKURA("fuji_sakura", "Fuji & Sakura", "fuji_sakura.html"),
    SUMI_E_MIST("sumi_e_mist", "Sumi-e Mist", "sumi_e_mist.html");

    companion object {
        fun fromId(id: String?): GlassBackgroundType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AURORA
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
