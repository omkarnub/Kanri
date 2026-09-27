package com.omkarnub.kanri.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.theme.LightBackground
import com.omkarnub.kanri.ui.theme.ThemeMode
import com.omkarnub.kanri.ui.theme.ThemePreferences
import com.omkarnub.kanri.ui.common.KanriGlassTheme
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * Minimalist Frosted Glass Navigation Dock:
 * 1. No artificial lighting reflections in Dark / AMOLED.
 * 2. Light mode shade matches the app's cream light theme (#FEFAEC).
 * 3. Real hardware-accelerated frost blur with low opacity & frosted texture across all 3 themes.
 * 4. Minimalist design: no box/pill around the selected menu, only SVG icon & label highlighted.
 */
@Composable
fun KanriFloatingDock(
    selectedTab: KanriTab,
    onTabSelected: (KanriTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAddMenuOpen: Boolean = false,
    hazeState: HazeState? = null,
    themeMode: ThemeMode? = null
) {
    val dockTheme = rememberKanriGlassTheme(themeMode)

    // Launch entrance animation: fluid slide-up, scale, and fade when the app opens
    val dockEntrance = remember { Animatable(0f) }
    val addButtonEntrance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            dockEntrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 180, easing = FastOutSlowInEasing)
            )
        }
        launch {
            addButtonEntrance.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, delayMillis = 260, easing = FastOutSlowInEasing)
            )
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 14.dp)
            .padding(bottom = 8.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Main Navigation Glass Pill (Capsule enclosing the 4 tabs)
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(64.dp)
                .graphicsLayer {
                    translationY = (1f - dockEntrance.value) * 60.dp.toPx()
                    alpha = dockEntrance.value
                    scaleX = 0.94f + 0.06f * dockEntrance.value
                    scaleY = 0.94f + 0.06f * dockEntrance.value
                }
                .shadow(
                    elevation = dockTheme.shadowElevation,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = dockTheme.shadowColor,
                    ambientColor = dockTheme.shadowColor.copy(alpha = dockTheme.shadowColor.alpha * 0.6f)
                )
                .clip(RoundedCornerShape(32.dp))
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = dockTheme.hazeStyle
                        )
                    } else {
                        Modifier
                    }
                )
                .border(
                    dockTheme.glassBorder,
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = if (hazeState != null) Color.Transparent else dockTheme.fallbackBackgroundColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                KanriTab.entries.forEach { tab ->
                    DockTabItem(
                        tab = tab,
                        isSelected = selectedTab == tab,
                        onClick = { onTabSelected(tab) },
                        dockTheme = dockTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Circular Action Glass Button (+)
        DockAddButton(
            onClick = onAddClick,
            isAddMenuOpen = isAddMenuOpen,
            dockTheme = dockTheme,
            hazeState = hazeState,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    translationY = (1f - addButtonEntrance.value) * 60.dp.toPx()
                    alpha = addButtonEntrance.value
                    scaleX = 0.90f + 0.10f * addButtonEntrance.value
                    scaleY = 0.90f + 0.10f * addButtonEntrance.value
                }
        )
    }
}

@Composable
private fun DockTabItem(
    tab: KanriTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    dockTheme: KanriGlassTheme,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "tabScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabIconScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) dockTheme.activeContentColor else dockTheme.inactiveContentColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "tabColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isSelected) {
                        haptics.tick()
                    }
                    onClick()
                }
            )
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            }
        ) {
            when (tab) {
                KanriTab.HOME -> HomeDockIcon(tint = contentColor, isSelected = isSelected, size = 22.dp)
                KanriTab.INSIGHTS -> AnalyticsDockIcon(tint = contentColor, isSelected = isSelected, size = 22.dp)
                KanriTab.LEND_BORROW -> LendBorrowDockIcon(tint = contentColor, isSelected = isSelected, size = 22.dp)
                KanriTab.GOALS -> GoalsDockIcon(tint = contentColor, isSelected = isSelected, size = 22.dp)
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = tab.title,
                color = contentColor,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp
            )
        }
    }
}

@Composable
private fun DockAddButton(
    onClick: () -> Unit,
    isAddMenuOpen: Boolean = false,
    dockTheme: KanriGlassTheme,
    hazeState: HazeState?,
    modifier: Modifier = Modifier
) {
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "addButtonScale"
    )

    val targetRotation = if (isAddMenuOpen) 45f else (if (isPressed) 45f else 0f)

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "addButtonRotation"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = dockTheme.shadowElevation,
                shape = CircleShape,
                spotColor = dockTheme.shadowColor,
                ambientColor = dockTheme.shadowColor.copy(alpha = dockTheme.shadowColor.alpha * 0.6f)
            )
            .clip(CircleShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeChild(
                        state = hazeState,
                        style = dockTheme.hazeStyle
                    )
                } else {
                    Modifier
                }
            )
            .border(
                dockTheme.glassBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            ),
        shape = CircleShape,
        color = if (hazeState != null) Color.Transparent else dockTheme.fallbackBackgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AddDockIcon(
                tint = dockTheme.addButtonIconColor,
                size = 28.dp,
                strokeWidth = 2.2.dp,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
    }
}
