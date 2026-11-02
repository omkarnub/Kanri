package com.omkarnub.kanri.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.ViewParent
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.PopupProperties
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.GlassCapabilities
import com.omkarnub.kanri.ui.theme.LightBackground
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.util.rememberKanriHaptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.hazeEffect

enum class GlassLevel {
    Blur,
    Flat,
    Strong
}

/**
 * Global CompositionLocal to pass HazeState from root backdrop host down to all screens,
 * popups, and dropdown menus.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Unified glass modifier supporting three hierarchy levels:
 * - [GlassLevel.Blur]: Live backdrop blur + tintBlur + gradient border + soft shadow.
 * - [GlassLevel.Flat]: tintFlat fill + gradient border, NO blur. Performant for lists.
 * - [GlassLevel.Strong]: tintStrong fill + stronger blur/fallback for transient dialogs/popups.
 *
 * When no glass theme is active (LocalKanriGlass == null), seamlessly falls back
 * to standard Material surface colors to preserve solid themes regression-free.
 */
fun Modifier.kanriGlass(
    level: GlassLevel = GlassLevel.Blur,
    shape: Shape? = null,
    hazeState: HazeState? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val tokens = LocalKanriGlass.current
    val currentHaze = hazeState ?: LocalHazeState.current
    val isBlurCapable = GlassCapabilities.isBlurEnabled()
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val effectiveShape = remember(tokens?.cornerRadius, shape) {
        when {
            tokens != null && (shape == null || shape == RoundedCornerShape(24.dp) || shape == RoundedCornerShape(20.dp)) -> {
                RoundedCornerShape(tokens.cornerRadius)
            }
            else -> shape ?: RoundedCornerShape(24.dp)
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_press_scale"
    )

    if (tokens != null) {
        val borderBrush = remember(tokens.borderStart, tokens.borderEnd) {
            Brush.linearGradient(
                colors = listOf(tokens.borderStart, tokens.borderEnd),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }

        val baseModifier = this
            .scale(scale)
            .then(
                if (level == GlassLevel.Blur) {
                    Modifier.shadow(
                        elevation = 4.dp,
                        shape = effectiveShape,
                        spotColor = tokens.shadow,
                        ambientColor = tokens.shadow.copy(alpha = tokens.shadow.alpha * 0.4f)
                    )
                } else if (level == GlassLevel.Strong) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = effectiveShape,
                        spotColor = tokens.shadow,
                        ambientColor = tokens.shadow.copy(alpha = tokens.shadow.alpha * 0.4f)
                    )
                } else {
                    Modifier
                }
            )
            .clip(effectiveShape)

        val surfaceModifier = when (level) {
            GlassLevel.Blur -> {
                if (isBlurCapable && currentHaze != null) {
                    baseModifier.hazeEffect(
                        state = currentHaze,
                        style = HazeStyle(
                            backgroundColor = Color.Transparent,
                            tint = HazeTint(tokens.tintBlur),
                            blurRadius = tokens.blurRadius
                        )
                    )
                } else if (isBlurCapable) {
                    baseModifier.background(tokens.tintBlur.copy(alpha = if (tokens.isDark) 0.16f else 0.72f))
                } else {
                    baseModifier.background(tokens.fallbackSurface)
                }
            }
            GlassLevel.Flat -> {
                baseModifier.background(tokens.tintFlat)
            }
            GlassLevel.Strong -> {
                if (isBlurCapable && currentHaze != null) {
                    val frostBg = (if (tokens.isDark) Color(0xFF14151B) else Color(0xFFFAF9F5)).copy(alpha = 0.70f)
                    baseModifier.hazeEffect(
                        state = currentHaze,
                        style = HazeStyle(
                            backgroundColor = frostBg,
                            tint = HazeTint(tokens.tintStrong),
                            blurRadius = (tokens.blurRadius * 1.45f).coerceAtLeast(36.dp),
                            noiseFactor = 0.04f
                        )
                    )
                } else if (isBlurCapable) {
                    baseModifier
                        .background(tokens.fallbackSurface)
                        .background(tokens.tintStrong)
                } else {
                    baseModifier
                        .background(tokens.fallbackSurface)
                        .background(tokens.tintStrong)
                }
            }
        }

        surfaceModifier
            .then(
                if (tokens.edgeHairline.alpha > 0f) {
                    Modifier.border(width = 1.dp, color = tokens.edgeHairline, shape = effectiveShape)
                } else {
                    Modifier
                }
            )
            .border(width = tokens.borderWidth, brush = borderBrush, shape = effectiveShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                haptics.click()
                                onClick()
                            }
                        )
                        .then(
                            if (isPressed) {
                                Modifier.background(Color.White.copy(alpha = 0.06f))
                            } else Modifier
                        )
                } else Modifier
            )
    } else {
        // Solid themes (Light, Dark, AMOLED, System):
        // Render high-fidelity, polished surfaces calibrated to each theme's lighting and elevation.
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        val cardBg = when (level) {
            GlassLevel.Strong -> if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color(0xFFFFFFFF)
            GlassLevel.Blur -> if (isDark) MaterialTheme.colorScheme.surfaceContainer else Color(0xFFFFFFFF)
            GlassLevel.Flat -> if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer
        }

        val borderBrush = if (isDark) {
            Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f))
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                )
            )
        }

        val shadowElevation = if (level == GlassLevel.Strong) {
            if (isDark) 6.dp else 2.5.dp
        } else if (level == GlassLevel.Blur) {
            if (isDark) 3.dp else 1.dp
        } else {
            0.dp
        }

        val spotShadow = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.035f)
        val ambientShadow = if (isDark) Color.Black.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.015f)

        this
            .scale(scale)
            .then(
                if (shadowElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = shadowElevation,
                        shape = effectiveShape,
                        spotColor = spotShadow,
                        ambientColor = ambientShadow
                    )
                } else Modifier
            )
            .clip(effectiveShape)
            .background(cardBg)
            .border(
                width = 0.8.dp,
                brush = borderBrush,
                shape = effectiveShape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = onClick
                    )
                } else Modifier
            )
    }
}

/**
 * Convenience composable for a glass card.
 */
@Composable
fun KanriGlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Blur,
    shape: Shape? = null,
    hazeState: HazeState? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.kanriGlass(
            level = level,
            shape = shape,
            hazeState = hazeState,
            onClick = onClick
        )
    ) {
        content()
    }
}

/**
 * Convenience composable for a glass pill.
 */
@Composable
fun KanriGlassPill(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Flat,
    hazeState: HazeState? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    KanriGlassCard(
        modifier = modifier,
        level = level,
        shape = CircleShape,
        hazeState = hazeState,
        onClick = onClick,
        content = content
    )
}

/**
 * Convenience composable for a text glass pill.
 */
@Composable
fun KanriGlassPill(
    text: String,
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Flat,
    hazeState: HazeState? = null,
    onClick: (() -> Unit)? = null
) {
    KanriGlassPill(
        modifier = modifier,
        level = level,
        hazeState = hazeState,
        onClick = onClick
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Legacy theme tokens preserved for backwards compatibility with existing UI components.
 */
data class KanriGlassTheme(
    val glassBorder: BorderStroke,
    val activeContentColor: Color,
    val inactiveContentColor: Color,
    val addButtonIconColor: Color,
    val shadowColor: Color,
    val shadowElevation: Dp,
    val hazeStyle: HazeStyle,
    val popupHazeStyle: HazeStyle,
    val fallbackBackgroundColor: Color
)

fun getKanriGlassTheme(
    themeMode: ThemeMode
): KanriGlassTheme {
    return when (themeMode) {
        ThemeMode.GLASS -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
            activeContentColor = DarkOffWhite,
            inactiveContentColor = Color(0x99FFFFFF),
            addButtonIconColor = DarkOffWhite,
            shadowColor = Color.Black.copy(alpha = 0.30f),
            shadowElevation = 14.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color.White.copy(alpha = 0.08f),
                tint = HazeTint(Color.White.copy(alpha = 0.04f)),
                blurRadius = 24.dp,
                noiseFactor = 0.03f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color.White.copy(alpha = 0.12f),
                tint = HazeTint(Color.White.copy(alpha = 0.06f)),
                blurRadius = 36.dp,
                noiseFactor = 0.04f
            ),
            fallbackBackgroundColor = Color.White.copy(alpha = 0.08f)
        )

        ThemeMode.DARK, ThemeMode.SYSTEM -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0x28FFFFFF)),
            activeContentColor = DarkOffWhite,
            inactiveContentColor = Color(0x66FFFFFF),
            addButtonIconColor = DarkOffWhite,
            shadowColor = Color.Black.copy(alpha = 0.40f),
            shadowElevation = 14.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color(0xFF141414).copy(alpha = 0.55f),
                tint = HazeTint(Color(0x35141414)),
                blurRadius = 24.dp,
                noiseFactor = 0.05f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color(0xFF141414).copy(alpha = 0.65f),
                tint = HazeTint(Color(0xFF0A0A0A).copy(alpha = 0.50f)),
                blurRadius = 36.dp,
                noiseFactor = 0.07f
            ),
            fallbackBackgroundColor = Color(0xFF1A1A1A).copy(alpha = 0.92f)
        )

        ThemeMode.AMOLED -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0x30FFFFFF)),
            activeContentColor = DarkOffWhite,
            inactiveContentColor = Color(0x66FFFFFF),
            addButtonIconColor = DarkOffWhite,
            shadowColor = Color.Black.copy(alpha = 0.55f),
            shadowElevation = 14.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color(0xFF000000).copy(alpha = 0.60f),
                tint = HazeTint(Color(0x30000000)),
                blurRadius = 24.dp,
                noiseFactor = 0.05f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color(0xFF000000).copy(alpha = 0.70f),
                tint = HazeTint(Color(0xFF000000).copy(alpha = 0.60f)),
                blurRadius = 36.dp,
                noiseFactor = 0.07f
            ),
            fallbackBackgroundColor = Color(0xFF0E0E0E).copy(alpha = 0.94f)
        )

        ThemeMode.LIGHT -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0xFFE2DEC9).copy(alpha = 0.65f)),
            activeContentColor = Color(0xFF000000),
            inactiveContentColor = Color(0x60000000),
            addButtonIconColor = Color(0xFF000000),
            shadowColor = Color.Black.copy(alpha = 0.04f),
            shadowElevation = 8.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color.White.copy(alpha = 0.85f),
                tint = HazeTint(Color.White.copy(alpha = 0.65f)),
                blurRadius = 24.dp,
                noiseFactor = 0.03f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color.White.copy(alpha = 0.92f),
                tint = HazeTint(Color.White.copy(alpha = 0.75f)),
                blurRadius = 36.dp,
                noiseFactor = 0.04f
            ),
            fallbackBackgroundColor = Color.White.copy(alpha = 0.94f)
        )
    }
}

@Composable
fun rememberKanriGlassTheme(
    themeMode: ThemeMode? = null
): KanriGlassTheme {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val effectiveThemeMode = themeMode ?: currentThemeMode

    return remember(effectiveThemeMode) {
        getKanriGlassTheme(effectiveThemeMode)
    }
}

fun Modifier.frostedGlass(
    glassTheme: KanriGlassTheme,
    hazeState: HazeState? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    applyShadow: Boolean = true
): Modifier = this
    .then(
        if (applyShadow) {
            Modifier.shadow(
                elevation = glassTheme.shadowElevation,
                shape = shape,
                spotColor = glassTheme.shadowColor,
                ambientColor = glassTheme.shadowColor.copy(alpha = glassTheme.shadowColor.alpha * 0.6f)
            )
        } else {
            Modifier
        }
    )
    .clip(shape)
    .then(
        if (hazeState != null) {
            Modifier.hazeEffect(
                state = hazeState,
                style = glassTheme.hazeStyle
            )
        } else {
            Modifier
        }
    )
    .background(if (hazeState != null) Color.Transparent else glassTheme.fallbackBackgroundColor)
    .border(
        border = glassTheme.glassBorder,
        shape = shape
    )

@Composable
fun KanriDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    val glassTheme = rememberKanriGlassTheme()
    val hazeState = LocalHazeState.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = offset,
        properties = properties,
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier.frostedGlass(
            glassTheme = glassTheme,
            hazeState = hazeState,
            shape = shape
        )
    ) {
        content()
    }
}

@Composable
fun KanriDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
        }
        Box(modifier = Modifier.weight(1f)) {
            text()
        }
        if (trailingIcon != null) {
            trailingIcon()
        }
    }
}

/**
 * Resolves the underlying native Dialog Window from Compose hierarchy.
 */
fun View.findDialogWindow(): Window? {
    var p: ViewParent? = parent
    while (p != null) {
        if (p is DialogWindowProvider) {
            return p.window
        }
        if (p is View) {
            val ctx = p.context
            if (ctx is DialogWindowProvider) {
                return ctx.window
            }
            if (ctx is android.app.Dialog) {
                return ctx.window
            }
        }
        p = p.parent
    }
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is DialogWindowProvider) {
            return ctx.window
        }
        if (ctx is android.app.Dialog) {
            return ctx.window
        }
        ctx = ctx.baseContext
    }
    return null
}

fun applyWindowBlur(window: Window?, blurRadiusPx: Int = 110) {
    if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val params = window.attributes
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            params.blurBehindRadius = blurRadiusPx.coerceIn(50, 160)
            window.attributes = params
        } catch (_: Exception) {}
    }
}

/**
 * Frosted Glass Bottom Sheet matching luxury glassmorphism across all screens.
 * Features transparent host, native API 31+ window blur, luminous gradient border,
 * soft elevation shadow, and ergonomic drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanriModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    hazeState: HazeState? = null,
    dragHandle: @Composable (() -> Unit)? = {
        val tokens = LocalKanriGlass.current
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(width = 44.dp, height = 5.dp)
                .clip(CircleShape)
                .background(
                    if (tokens != null) tokens.textTertiary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f)
                )
        )
    },
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    val currentHaze = hazeState ?: LocalHazeState.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface,
        scrimColor = if (tokens != null) {
            if (tokens.isDark) Color.Black.copy(alpha = 0.45f) else Color(0xFF12182B).copy(alpha = 0.30f)
        } else BottomSheetDefaults.ScrimColor,
        shape = shape,
        dragHandle = null,
        modifier = modifier
    ) {
        val view = LocalView.current
        val density = LocalDensity.current
        val blurRadiusPx = remember(tokens, density) {
            val dpVal = tokens?.blurRadius ?: 32.dp
            with(density) { dpVal.roundToPx() }.coerceIn(60, 160)
        }
        DisposableEffect(view, blurRadiusPx) {
            fun attachBlur() {
                val window = view.findDialogWindow()
                if (window != null) {
                    applyWindowBlur(window, blurRadiusPx)
                }
            }
            attachBlur()
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    attachBlur()
                }
                override fun onViewDetachedFromWindow(v: View) {}
            }
            view.addOnAttachStateChangeListener(listener)
            view.post { attachBlur() }
            onDispose {
                view.removeOnAttachStateChangeListener(listener)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .kanriGlass(
                    level = GlassLevel.Strong,
                    shape = shape,
                    hazeState = currentHaze
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (dragHandle != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        dragHandle()
                    }
                }
                content()
            }
        }
    }
}

/**
 * Standardized Frosted Glass Dialog with native window blur and tactile glass card surface.
 */
@Composable
fun KanriGlassDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    hazeState: HazeState? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    val currentHaze = hazeState ?: LocalHazeState.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val view = LocalView.current
        val density = LocalDensity.current
        val blurRadiusPx = remember(tokens, density) {
            val dpVal = tokens?.blurRadius ?: 32.dp
            with(density) { dpVal.roundToPx() }.coerceIn(60, 160)
        }
        DisposableEffect(view, blurRadiusPx) {
            fun attachBlur() {
                val window = view.findDialogWindow()
                if (window != null) {
                    applyWindowBlur(window, blurRadiusPx)
                    if (tokens != null) {
                        window.setDimAmount(0.35f)
                    }
                }
            }
            attachBlur()
            val listener = object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    attachBlur()
                }
                override fun onViewDetachedFromWindow(v: View) {}
            }
            view.addOnAttachStateChangeListener(listener)
            view.post { attachBlur() }
            onDispose {
                view.removeOnAttachStateChangeListener(listener)
            }
        }

        val dialogEntrance = remember { Animatable(0.94f) }
        val dialogAlpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch {
                dialogEntrance.animateTo(
                    1f,
                    animationSpec = spring(
                        dampingRatio = 0.80f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                dialogAlpha.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 180, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f))
                )
            }
        }

        val dialogBg = if (tokens != null) {
            if (tokens.isDark) Color(0xFF14151B).copy(alpha = 0.72f)
            else Color(0xFFFAF9F5).copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.surface
        }
        val dialogBorder = if (tokens != null) {
            Brush.linearGradient(
                listOf(tokens.borderStart, tokens.borderEnd),
                Offset.Zero,
                Offset.Infinite
            )
        } else {
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            if (isDark) {
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.05f))
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f),
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
                    )
                )
            }
        }

        val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = modifier
                    .graphicsLayer {
                        scaleX = dialogEntrance.value
                        scaleY = dialogEntrance.value
                        alpha = dialogAlpha.value
                    }
                    .shadow(
                        elevation = if (tokens != null) 24.dp else (if (isDarkSurface) 16.dp else 6.dp),
                        shape = shape,
                        spotColor = if (tokens != null || isDarkSurface) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.06f)
                    )
                    .border(
                        width = tokens?.borderWidth ?: 1.dp,
                        brush = dialogBorder,
                        shape = shape
                    ),
                shape = shape,
                color = dialogBg
            ) {
                Box(
                    modifier = Modifier.background(
                        if (tokens != null) {
                            Brush.verticalGradient(
                                listOf(
                                    if (tokens.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            )
                        } else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Glass-styled text field colors for OutlinedTextField inputs.
 * Ensures consistent translucent backgrounds and luminous borders across dialogs and screens.
 */
@Composable
fun kanriGlassTextFieldColors(): TextFieldColors {
    val tokens = LocalKanriGlass.current
    return if (tokens != null) {
        val containerColor = if (tokens.isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
        val focusedBorder = tokens.accent
        val unfocusedBorder = if (tokens.isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f)
        val textColor = if (tokens.isDark) Color.White else Color(0xFF18181B)
        val placeholderColor = if (tokens.isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f)

        OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.02f),
            focusedBorderColor = focusedBorder,
            unfocusedBorderColor = unfocusedBorder,
            focusedTextColor = textColor,
            unfocusedTextColor = textColor,
            focusedLabelColor = focusedBorder,
            unfocusedLabelColor = placeholderColor,
            focusedPlaceholderColor = placeholderColor,
            unfocusedPlaceholderColor = placeholderColor,
            cursorColor = focusedBorder
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }
}

/**
 * High-contrast primary button colors helper for Glass and Solid themes.
 * Prevents white-on-white text glitches in glass mode.
 */
@Composable
fun kanriPrimaryButtonColors(): ButtonColors {
    val tokens = LocalKanriGlass.current
    return if (tokens != null) {
        ButtonDefaults.buttonColors(
            containerColor = if (tokens.isDark) Color(0xFFEDEDED) else Color(0xFF18181B),
            contentColor = if (tokens.isDark) Color(0xFF111114) else Color.White,
            disabledContainerColor = if (tokens.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
            disabledContentColor = if (tokens.isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.35f)
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

/**
 * Interactive glass button with haptic feedback, spring scale animation, and gradient border.
 */
@Composable
fun KanriGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Flat,
    shape: Shape = RoundedCornerShape(14.dp),
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btn_press_scale"
    )

    val buttonFill = if (tokens != null) {
        val baseAlpha = when (level) {
            GlassLevel.Strong -> 0.22f
            GlassLevel.Blur -> 0.16f
            GlassLevel.Flat -> 0.12f
        }
        if (tokens.isDark) Color.White.copy(alpha = baseAlpha) else Color.Black.copy(alpha = baseAlpha * 0.7f)
    } else null

    Box(
        modifier = modifier
            .scale(scale)
            .kanriGlass(
                level = level,
                shape = shape,
                hazeState = hazeState,
                onClick = if (enabled) onClick else null
            )
            .then(if (buttonFill != null) Modifier.background(buttonFill, shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val defaultContentColor = tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
        CompositionLocalProvider(LocalContentColor provides defaultContentColor) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Convenience overload of KanriGlassButton accepting text and optional leading icon.
 */
@Composable
fun KanriGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Flat,
    shape: Shape = RoundedCornerShape(14.dp),
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    textColor: Color? = null,
    icon: (@Composable () -> Unit)? = null
) {
    KanriGlassButton(
        onClick = onClick,
        modifier = modifier,
        level = level,
        shape = shape,
        hazeState = hazeState,
        enabled = enabled
    ) {
        val tokens = LocalKanriGlass.current
        val color = textColor ?: tokens?.textPrimary ?: MaterialTheme.colorScheme.onSurface
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides color) {
                icon()
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Frosted Glass Alert Dialog with native window blur and tactile glass card surface.
 * Drop-in replacement for standard AlertDialog.
 */
@Composable
fun KanriGlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    containerColor: Color = Color.Unspecified,
    textContentColor: Color = Color.Unspecified,
    titleContentColor: Color = Color.Unspecified,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    KanriGlassDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(0.92f),
        shape = shape,
        properties = properties
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
            }
            if (title != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (text != null) 12.dp else 16.dp)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurface
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        ) {
                            title()
                        }
                    }
                }
            }
            if (text != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        ProvideTextStyle(
                            value = MaterialTheme.typography.bodyMedium
                        ) {
                            text()
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissButton != null) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                confirmButton()
            }
        }
    }
}

/**
 * Frosted Glass DatePicker Dialog with native window blur and tactile glass surface.
 * Drop-in replacement for standard DatePickerDialog.
 */
@Composable
fun KanriGlassDatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    KanriGlassDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.fillMaxWidth(0.95f),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dismissButton != null) {
                    dismissButton()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                confirmButton()
            }
        }
    }
}



