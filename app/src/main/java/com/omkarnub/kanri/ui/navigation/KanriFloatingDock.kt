package com.omkarnub.kanri.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassTheme
import com.omkarnub.kanri.ui.common.kanriGlass
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.ui.theme.KanriMotion
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    val tokens = LocalKanriGlass.current

    // Launch entrance animation: fluid slide-up, scale, and fade when the app opens
    val dockEntrance = remember { Animatable(0f) }
    val addButtonEntrance = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            dockEntrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            addButtonEntrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    Row(
        modifier = modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 8.dp)
            .navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Main Navigation Glass Pill (Capsule enclosing the 4 tabs)
        val pillModifier = if (tokens != null) {
            val glassHazeStyle = remember(tokens.tintBlur, tokens.blurRadius, tokens.isDark) {
                val dockBg = (if (tokens.isDark) Color(0xFF14151E) else Color(0xFFFAF9F5)).copy(
                    alpha = if (tokens.isDark) 0.38f else 0.58f
                )
                val dockTint = (if (tokens.isDark) Color.White else Color(0xFF18181B)).copy(
                    alpha = if (tokens.isDark) 0.16f else 0.25f
                )
                HazeStyle(
                    backgroundColor = dockBg,
                    tint = HazeTint(dockTint),
                    blurRadius = tokens.blurRadius.coerceAtLeast(24.dp),
                    noiseFactor = 0.02f
                )
            }
            Modifier
                .weight(1f)
                .height(64.dp)
                .graphicsLayer {
                    translationY = (1f - dockEntrance.value) * 60.dp.toPx()
                    alpha = dockEntrance.value
                    scaleX = 0.94f + 0.06f * dockEntrance.value
                    scaleY = 0.94f + 0.06f * dockEntrance.value
                }
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(32.dp),
                    spotColor = Color.Black.copy(alpha = 0.035f),
                    ambientColor = Color.Black.copy(alpha = 0.015f)
                )
                .clip(RoundedCornerShape(32.dp))
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = glassHazeStyle
                        )
                    } else {
                        Modifier.background(Color.White.copy(alpha = 0.14f))
                    }
                )
                .border(
                    width = 0.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        } else {
            Modifier
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
                        Modifier.hazeEffect(
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
                )
        }

        Box(
            modifier = pillModifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
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

        Spacer(modifier = Modifier.width(8.dp))

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
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = KanriMotion.PressSpring,
        label = "tabScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabIconScale"
    )

    val dotScale by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.70f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabDotScale"
    )

    val targetAlpha = if (isSelected) 1.0f else 0.50f
    val baseColor = if (tokens != null) {
        tokens.textPrimary
    } else {
        dockTheme.activeContentColor
    }

    val contentColor by animateColorAsState(
        targetValue = baseColor.copy(alpha = targetAlpha),
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "tabColor"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .scale(scale)
            .padding(horizontal = 2.dp, vertical = 2.dp)
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
            .padding(horizontal = 2.dp, vertical = 2.dp),
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

            Spacer(modifier = Modifier.height(2.dp))

            val isLendBorrow = tab == KanriTab.LEND_BORROW
            AnimatedContent(
                targetState = tab.title,
                transitionSpec = {
                    (slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(150))
                    )
                },
                label = "tabTextMotion"
            ) { titleText ->
                Text(
                    text = titleText,
                    color = contentColor,
                    fontSize = if (isLendBorrow) 8.5.sp else 9.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    letterSpacing = if (isLendBorrow) (-0.5).sp else (-0.2).sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Small 4dp dot under selected tab with spring animation
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotScale
                    }
                    .background(
                        color = baseColor,
                        shape = CircleShape
                    )
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
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = KanriMotion.PressSpring,
        label = "addButtonScale"
    )

    val targetRotation = if (isAddMenuOpen) 45f else (if (isPressed) 45f else 0f)

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "addButtonRotation"
    )

    val buttonModifier = if (tokens != null) {
        val glassHazeStyle = remember(tokens.tintBlur, tokens.blurRadius, tokens.isDark) {
            val dockBg = (if (tokens.isDark) Color(0xFF14151E) else Color(0xFFFAF9F5)).copy(
                alpha = if (tokens.isDark) 0.40f else 0.60f
            )
            val dockTint = (if (tokens.isDark) Color.White else Color(0xFF18181B)).copy(
                alpha = if (tokens.isDark) 0.18f else 0.28f
            )
            HazeStyle(
                backgroundColor = dockBg,
                tint = HazeTint(dockTint),
                blurRadius = tokens.blurRadius.coerceAtLeast(24.dp),
                noiseFactor = 0.02f
            )
        }
        modifier
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                spotColor = Color.Black.copy(alpha = 0.035f),
                ambientColor = Color.Black.copy(alpha = 0.015f)
            )
            .clip(CircleShape)
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(
                        state = hazeState,
                        style = glassHazeStyle
                    )
                } else {
                    Modifier.background(Color.White.copy(alpha = 0.16f))
                }
            )
            .border(
                width = 0.8.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.click()
                    onClick()
                }
            )
    } else {
        modifier
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
                    Modifier.hazeEffect(
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
            )
            .background(if (hazeState != null) Color.Transparent else dockTheme.fallbackBackgroundColor)
    }

    Box(
        modifier = buttonModifier,
        contentAlignment = Alignment.Center
    ) {
        AddDockIcon(
            tint = if (tokens != null) tokens.accent else dockTheme.addButtonIconColor,
            size = 28.dp,
            strokeWidth = 2.2.dp,
            modifier = Modifier.graphicsLayer { rotationZ = rotation }
        )
    }
}
