package com.omkarnub.kanri.ui.common.kokonut

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omkarnub.kanri.ui.common.GlassLevel
import com.omkarnub.kanri.ui.common.KanriGlassCard
import com.omkarnub.kanri.ui.theme.LocalKanriGlass
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 1:1 Port of Kokonut UI Stack Panel (A6. stack-panel.tsx)
 *
 * Exact constants:
 * Push spring: stiffness = 223.8f, dampingRatio = 1f (duration 0.42, bounce 0)
 * Pop spring: stiffness = 341.5f, dampingRatio = 1f (duration 0.34, bounce 0)
 * Height spring: stiffness = 273f, dampingRatio = 1f (duration 0.38, bounce 0)
 * PARALLAX = 0.3
 * POP_DISTANCE = 0.35
 * POP_VELOCITY = 500 px/s
 * TOGGLE: 36x22dp, thumb 18dp at 2dp offset, translateX 14dp
 */

const val STACK_PARALLAX = 0.3f
const val STACK_POP_DISTANCE = 0.35f
const val STACK_POP_VELOCITY = 500f

val STACK_PUSH_SPRING = spring<Float>(stiffness = 223.8f, dampingRatio = 1f)
val STACK_POP_SPRING = spring<Float>(stiffness = 341.5f, dampingRatio = 1f)
val STACK_HEIGHT_SPRING = spring<Float>(stiffness = 273f, dampingRatio = 1f)
val STACK_EASE_OUT = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

enum class StackRowTone {
    DEFAULT,
    DANGER
}

data class StackRow(
    val id: String,
    val label: String,
    val detail: String? = null,
    val icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    val to: String? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val onSelect: (() -> Unit)? = null,
    val selected: Boolean? = null,
    val checked: Boolean? = null,
    val onCheckedChange: ((Boolean) -> Unit)? = null,
    val tone: StackRowTone = StackRowTone.DEFAULT
)

data class StackSection(
    val title: String? = null,
    val rows: List<StackRow>,
    val footer: String? = null
)

data class StackPage(
    val title: String,
    val sections: List<StackSection>
)

@Composable
fun KokonutToggle(
    on: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val density = LocalDensity.current

    // Toggle 36x22dp, thumb 18dp at 2dp offset, translateX 14dp when on
    val thumbOffsetDp by animateFloatAsState(
        targetValue = if (on) 14f else 0f,
        animationSpec = tween(durationMillis = 200, easing = STACK_EASE_OUT),
        label = "toggleThumb"
    )

    val trackBg = if (on) {
        if (dark) Zinc100 else Zinc900
    } else {
        if (dark) Zinc700 else Zinc200
    }

    Box(
        modifier = modifier
            .size(width = 36.dp, height = 22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(trackBg)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = with(density) { thumbOffsetDp.dp.toPx().roundToInt() }, y = 0) }
                .size(18.dp)
                .shadow(elevation = 1.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (dark) Zinc950 else Color.White)
        )
    }
}

@Composable
fun KokonutStackPanel(
    pages: Map<String, StackPage>,
    root: String,
    onBackToApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current
    val haptics = rememberKanriHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val stack = remember { mutableStateListOf(root) }
    var busy by remember { mutableStateOf(false) }

    val topKey = stack.lastOrNull() ?: root
    val topPage = pages[topKey] ?: pages[root]!!
    val underKey = if (stack.size > 1) stack[stack.size - 2] else null
    val parentPage = underKey?.let { pages[it] }

    val xAnim = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }

    fun pop(): Boolean {
        if (stack.size < 2 || busy) return false
        busy = true
        haptics.click()

        scope.launch {
            // Pop animation to width
            xAnim.animateTo(
                targetValue = 1000f,
                animationSpec = STACK_POP_SPRING
            )
            stack.removeAt(stack.size - 1)
            xAnim.snapTo(0f)
            busy = false
        }
        return true
    }

    fun push(to: String) {
        if (!pages.containsKey(to) || busy) return
        busy = true
        haptics.click()

        scope.launch {
            stack.add(to)
            xAnim.snapTo(1000f)
            xAnim.animateTo(
                targetValue = 0f,
                animationSpec = STACK_PUSH_SPRING
            )
            busy = false
        }
    }

    BackHandler(enabled = true) {
        if (!pop()) {
            onBackToApp()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val panelWidthPx = constraints.maxWidth.toFloat()

        // Card container: max width 384dp, rounded 16dp, 1dp border zinc-200/800, bg white/zinc-950, clipped
        KanriGlassCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .width(384.dp)
                .clip(RoundedCornerShape(16.dp)),
            level = GlassLevel.Blur,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header: height 48, px 8, bottom border zinc-100 / dark zinc-900 (or glass)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(
                            if (tokens != null) Color.Transparent else if (dark) Zinc950 else Color.White
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    // Back button (visible when stack > 1)
                    if (parentPage != null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { pop() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Back to ${parentPage.title}",
                                tint = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc600,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = parentPage.title,
                                fontSize = 14.sp,
                                color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc600,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }

                    // Header title: centered, 15sp semibold
                    Text(
                        text = topPage.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                HorizontalDivider(
                    color = if (tokens != null) tokens.borderStart.copy(alpha = 0.3f) else if (dark) Zinc900 else Zinc100,
                    thickness = 1.dp
                )

                // Page Body Area with parallax & swipe back
                val currentOffset = xAnim.value.coerceAtLeast(0f)
                val underOffset = -STACK_PARALLAX * (panelWidthPx - currentOffset)
                val shadeAlpha = (0.06f * (1f - currentOffset / panelWidthPx)).coerceIn(0f, 0.06f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(stack.size, busy) {
                            if (stack.size <= 1 || busy) return@pointerInput

                            detectHorizontalDragGestures(
                                onHorizontalDrag = { change, dragAmount ->
                                    if (dragAmount > 0 || xAnim.value > 0) {
                                        change.consume()
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        val next = (xAnim.value + dragAmount).coerceAtLeast(0f)
                                        scope.launch { xAnim.snapTo(next) }
                                    }
                                },
                                onDragEnd = {
                                    val vel = velocityTracker.calculateVelocity().x
                                    if (xAnim.value > panelWidthPx * STACK_POP_DISTANCE || vel > STACK_POP_VELOCITY) {
                                        pop()
                                    } else {
                                        scope.launch { xAnim.animateTo(0f, STACK_PUSH_SPRING) }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { xAnim.animateTo(0f, STACK_PUSH_SPRING) }
                                }
                            )
                        }
                ) {
                    // Under Page (only visible when actively stacked/sliding to prevent bleed)
                    if (parentPage != null && (xAnim.value > 0f || busy)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(x = underOffset.roundToInt(), y = 0) }
                        ) {
                            StackPageView(
                                page = parentPage,
                                onOpen = { }
                            )
                            // Shade overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = shadeAlpha))
                            )
                        }
                    }

                    // Top Page
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(x = currentOffset.roundToInt(), y = 0) }
                            .background(
                                if (tokens != null) {
                                    if (xAnim.value > 0f || busy) tokens.tintStrong else Color.Transparent
                                } else {
                                    if (dark) Zinc950 else Color.White
                                }
                            )
                    ) {
                        StackPageView(
                            page = topPage,
                            onOpen = { row ->
                                if (row.onSelect != null) {
                                    row.onSelect.invoke()
                                } else if (row.checked != null && row.onCheckedChange != null) {
                                    row.onCheckedChange.invoke(!row.checked)
                                } else if (!row.to.isNullOrEmpty()) {
                                    push(row.to)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StackPageView(
    page: StackPage,
    onOpen: (StackRow) -> Unit
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        page.sections.forEach { section ->
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Title: 12sp medium zinc-500/400, mb 6, px 12
                if (!section.title.isNullOrEmpty()) {
                    Text(
                        text = section.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Section Card: rounded 12, bg zinc-50 / dark zinc-900 (or glass), 1dp dividers
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (tokens != null) tokens.tintFlat else if (dark) Zinc900 else Zinc50
                        )
                        .then(
                            if (tokens != null) {
                                Modifier.border(1.dp, tokens.borderStart.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            } else Modifier
                        )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        section.rows.forEachIndexed { idx, row ->
                            if (idx > 0) {
                                HorizontalDivider(
                                    color = if (tokens != null) tokens.borderStart.copy(alpha = 0.15f) else if (dark) Zinc800.copy(alpha = 0.7f) else Zinc100,
                                    thickness = 1.dp
                                )
                            }
                            StackRowView(
                                row = row,
                                onClick = { onOpen(row) }
                            )
                        }
                    }
                }

                // Section Footer: 12sp zinc-400 / dark zinc-500, mt 6, px 12
                if (!section.footer.isNullOrEmpty()) {
                    Text(
                        text = section.footer,
                        fontSize = 12.sp,
                        color = tokens?.textSecondary ?: if (dark) Zinc500 else Zinc400,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StackRowView(
    row: StackRow,
    onClick: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val tokens = LocalKanriGlass.current

    val labelColor = when (row.tone) {
        StackRowTone.DANGER -> if (dark) Color(0xFFF87171) else Color(0xFFDC2626)
        else -> tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (row.icon != null) {
            Icon(
                imageVector = row.icon,
                contentDescription = null,
                tint = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc600,
                modifier = Modifier.size(18.dp)
            )
        }
        // Row label: 15sp, single line ellipsized, flex 1
        Text(
            text = row.label,
            fontSize = 15.sp,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Detail: 14sp zinc-500/400, max-w-[45%], ellipsized
        if (!row.detail.isNullOrEmpty()) {
            Text(
                text = row.detail,
                fontSize = 14.sp,
                color = tokens?.textSecondary ?: if (dark) Zinc400 else Zinc500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
                textAlign = TextAlign.End
            )
        }

        // Custom Trailing slot
        if (row.trailing != null) {
            row.trailing.invoke()
        }

        // Switch Toggle
        if (row.checked != null) {
            KokonutToggle(on = row.checked)
        }

        // Check selection indicator (16dp)
        if (row.selected != null) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = tokens?.textPrimary ?: if (dark) Zinc100 else Zinc900,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { alpha = if (row.selected) 1f else 0f }
            )
        }

        // Chevron-right (16dp)
        if (!row.to.isNullOrEmpty()) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = tokens?.textSecondary ?: if (dark) Zinc500 else Zinc400,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
