package com.omkarnub.kanri.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.omkarnub.kanri.ui.theme.DarkOffWhite
import com.omkarnub.kanri.ui.theme.LightBackground
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * Styling tokens for the frosted glass aesthetic, shared between the floating dock,
 * popup menus, and popup sheets across the whole app.
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

/**
 * Global CompositionLocal to pass HazeState from root navigation down to all screens,
 * popups, and dropdown menus.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

fun getKanriGlassTheme(themeMode: ThemeMode): KanriGlassTheme {
    return when (themeMode) {
        ThemeMode.DARK -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0x28FFFFFF)), // Subtle, uniform glass border for dock
            activeContentColor = DarkOffWhite, // Off-white for dark mode components
            inactiveContentColor = Color(0x66FFFFFF), // Muted translucent white (~40%)
            addButtonIconColor = DarkOffWhite,
            shadowColor = Color.Black.copy(alpha = 0.40f),
            shadowElevation = 14.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color(0xFF141414).copy(alpha = 0.55f), // Pure neutral charcoal translucent base (zero blue)
                tint = HazeTint(Color(0x35141414)), // Subtle neutral dark frost tint for floating dock
                blurRadius = 24.dp,        // Deep frost blur
                noiseFactor = 0.05f        // Frosted glass micro-texture
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color(0xFF141414).copy(alpha = 0.65f), // Rich dark neutral translucent base
                tint = HazeTint(Color(0xFF0A0A0A).copy(alpha = 0.50f)), // Dark frosted glass tint for popup cards
                blurRadius = 36.dp,        // Deep lush frost blur
                noiseFactor = 0.07f        // Frosted glass micro-texture
            ),
            fallbackBackgroundColor = Color(0xEB1A1A1A) // Pure neutral charcoal fallback
        )

        ThemeMode.AMOLED -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0x30FFFFFF)), // Subtle, uniform glass border
            activeContentColor = DarkOffWhite,
            inactiveContentColor = Color(0x66FFFFFF),
            addButtonIconColor = DarkOffWhite,
            shadowColor = Color.Black.copy(alpha = 0.55f),
            shadowElevation = 14.dp,
            hazeStyle = HazeStyle(
                backgroundColor = Color(0xFF000000).copy(alpha = 0.60f), // Translucent black base
                tint = HazeTint(Color(0x30000000)), // Subtle black frost tint
                blurRadius = 24.dp,
                noiseFactor = 0.05f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = Color(0xFF000000).copy(alpha = 0.70f), // Pure deep black translucent base
                tint = HazeTint(Color(0xFF000000).copy(alpha = 0.60f)), // Deep dark frost tint
                blurRadius = 36.dp,
                noiseFactor = 0.07f
            ),
            fallbackBackgroundColor = Color(0xF00E0E0E)
        )

        ThemeMode.LIGHT -> KanriGlassTheme(
            glassBorder = BorderStroke(0.8.dp, Color(0x20000000)), // Soft subtle border
            activeContentColor = Color(0xFF000000), // Crisp monochrome black
            inactiveContentColor = Color(0x60000000), // Muted translucent black (~38%)
            addButtonIconColor = Color(0xFF000000),
            shadowColor = Color(0x15000000),
            shadowElevation = 12.dp,
            hazeStyle = HazeStyle(
                backgroundColor = LightBackground.copy(alpha = 0.60f), // Translucent soft cream base for dock
                tint = HazeTint(LightBackground.copy(alpha = 0.25f)), // Subtle cream frost tint
                blurRadius = 24.dp,
                noiseFactor = 0.04f
            ),
            popupHazeStyle = HazeStyle(
                backgroundColor = LightBackground.copy(alpha = 0.75f), // Rich translucent soft cream base for popup
                tint = HazeTint(LightBackground.copy(alpha = 0.35f)), // Rich cream frost tint
                blurRadius = 36.dp,
                noiseFactor = 0.05f
            ),
            fallbackBackgroundColor = LightBackground.copy(alpha = 0.88f)
        )
    }
}

@Composable
fun rememberKanriGlassTheme(themeMode: ThemeMode? = null): KanriGlassTheme {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences.getInstance(context) }
    val currentThemeMode by themePrefs.themeModeFlow.collectAsState()
    val effectiveThemeMode = themeMode ?: currentThemeMode

    return remember(effectiveThemeMode) {
        getKanriGlassTheme(effectiveThemeMode)
    }
}

/**
 * Frosted glass modifier matching the exact look and feel of the floating navigation dock.
 */
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
            Modifier.hazeChild(
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

/**
 * Frosted Glass Dropdown Menu with the exact same visual styling as the floating dock.
 */
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

/**
 * Dropdown Menu Item tailored for frosted glass menus with subtle pressed states
 * and crisp typography.
 */
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
