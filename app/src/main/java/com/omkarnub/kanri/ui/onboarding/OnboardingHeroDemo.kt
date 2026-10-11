package com.omkarnub.kanri.ui.onboarding

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.omkarnub.kanri.R
import com.omkarnub.kanri.ui.common.AnimatedNumber
import com.omkarnub.kanri.ui.common.CategoryIcon
import com.omkarnub.kanri.ui.common.rememberKanriGlassTheme
import com.omkarnub.kanri.ui.theme.ExpenseRed
import com.omkarnub.kanri.ui.theme.GoogleSansFlex
import com.omkarnub.kanri.ui.theme.IncomeGreen
import com.omkarnub.kanri.util.rememberKanriHaptics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Transition style — rotates every scenario for visual variety
// ─────────────────────────────────────────────────────────────────────────────
private enum class TransitionStyle { SlideFromRight, SlideFromLeft, RiseFromCenter }

private fun transitionForIndex(idx: Int): TransitionStyle = when (idx % 3) {
    0    -> TransitionStyle.SlideFromRight
    1    -> TransitionStyle.SlideFromLeft
    else -> TransitionStyle.RiseFromCenter
}

// ─────────────────────────────────────────────────────────────────────────────
// State machine
// ─────────────────────────────────────────────────────────────────────────────
class OnboardingHeroStateMachine(
    val scenarios: List<DemoScenario> = DemoScenarios.list,
    private val delayProvider: suspend (Long) -> Unit = { delay(it) }
) {
    var state by mutableStateOf(DemoStageState.Idle)
        internal set
    var currentScenarioIndex by mutableIntStateOf(0)
        internal set
    var loopCount by mutableIntStateOf(0)
        internal set
    var isPaused by mutableStateOf(false)
        internal set
    var hasFiredSmsHaptic by mutableStateOf(false)
        internal set
    var hasFiredLandHaptic by mutableStateOf(false)
        internal set
    var reduceMotion by mutableStateOf(false)
        internal set

    var onHapticTick: (() -> Unit)? = null
    var onHapticLand: (() -> Unit)? = null
    var onMeshPulse: ((Boolean) -> Unit)? = null

    val currentScenario: DemoScenario
        get() = scenarios[currentScenarioIndex % scenarios.size]

    companion object {
        const val T_INTRO_DELAY = 600L
        const val T_SMS_ENTER   = 1100L  // SMS visible duration
        const val T_SMS_HOLD    = 500L   // hold pause
        const val T_MORPH       = 560L   // covers SMS exit + card enter + simultaneous reactions
        const val T_MORPH_WAIT  = T_MORPH
        const val T_DIGITS_ROLL = 800L
        const val T_GAUGE_REACT = 600L
        const val T_HOLD        = 1600L
        const val T_EXIT        = 380L
        const val T_EXIT_WAIT   = T_EXIT
    }

    suspend fun run() {
        if (reduceMotion) { state = DemoStageState.Holding; return }
        if (loopCount == 0 && state == DemoStageState.Idle) delayProvider(T_INTRO_DELAY)

        while (true) {
            waitWhilePaused()

            state = DemoStageState.SmsEntering
            if (!hasFiredSmsHaptic) { hasFiredSmsHaptic = true; onHapticTick?.invoke() }
            delayProvider(T_SMS_ENTER); waitWhilePaused()

            state = DemoStageState.SmsHolding
            delayProvider(T_SMS_HOLD); waitWhilePaused()

            state = DemoStageState.Morphing
            delayProvider(T_MORPH_WAIT); waitWhilePaused()

            state = DemoStageState.CardLanded
            delayProvider(T_DIGITS_ROLL)
            if (!hasFiredLandHaptic) { hasFiredLandHaptic = true; onHapticLand?.invoke() }
            waitWhilePaused()

            state = DemoStageState.GaugeReacting
            delayProvider(T_GAUGE_REACT); waitWhilePaused()

            state = DemoStageState.Holding
            delayProvider(T_HOLD); waitWhilePaused()

            state = DemoStageState.Exiting
            delayProvider(T_EXIT_WAIT); waitWhilePaused()

            currentScenarioIndex = (currentScenarioIndex + 1) % scenarios.size
            if (currentScenarioIndex == 0) loopCount++

            state = DemoStageState.Idle
            delayProvider(80L)
        }
    }

    private suspend fun waitWhilePaused() { while (isPaused) delayProvider(50L) }
    fun pause()  { isPaused = true }
    fun resume() { isPaused = false }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Demo composable
// No camera zoom. Clean static background. 3 rotating transition styles.
// Strictly sequential alpha (SMS gone → card enters, never overlap).
// Seamless simultaneous digit rolling + mini gauge reactivity.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OnboardingHeroDemo(
    modifier: Modifier = Modifier,
    isExternalPaused: Boolean = false,
    onScenarioChange: ((DemoScenario) -> Unit)? = null,
    onPulseChange: (Float) -> Unit = {}
) {
    val context       = LocalContext.current
    val haptics       = rememberKanriHaptics()
    val lifecycleOwner = LocalLifecycleOwner.current

    val reduceMotion = remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f) == 0f
        } catch (_: Exception) { false }
    }

    val stateMachine = remember { OnboardingHeroStateMachine().apply { this.reduceMotion = reduceMotion } }

    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val effectivePaused = isExternalPaused || !isResumed
    LaunchedEffect(effectivePaused) {
        if (effectivePaused) stateMachine.pause() else stateMachine.resume()
    }

    DisposableEffect(stateMachine) {
        stateMachine.onHapticTick = { haptics.tick() }
        stateMachine.onHapticLand = { haptics.tick() }
        stateMachine.onMeshPulse  = null
        onDispose {
            stateMachine.onHapticTick = null
            stateMachine.onHapticLand = null
            stateMachine.onMeshPulse  = null
        }
    }

    LaunchedEffect(stateMachine) { stateMachine.run() }

    val scenario     = stateMachine.currentScenario
    val currentState = stateMachine.state
    val scenarioIdx  = stateMachine.currentScenarioIndex
    val style        = transitionForIndex(scenarioIdx)

    LaunchedEffect(scenario) {
        onScenarioChange?.invoke(scenario)
    }

    // ── SMS animatables ───────────────────────────────────────────────────────
    val smsAlpha   = remember { Animatable(0f) }
    val smsOffsetY = remember { Animatable(50f) }   // slides up smoothly from bottom

    // ── Card animatables — values depend on style ─────────────────────────────
    // offsetX: used by SlideFromRight (+) and SlideFromLeft (-)
    // offsetY: used by RiseFromCenter
    // scale: used by RiseFromCenter
    val cardAlpha   = remember { Animatable(0f) }
    val cardOffsetX = remember { Animatable(0f) }
    val cardOffsetY = remember { Animatable(0f) }
    val cardScale   = remember { Animatable(1f) }

    var gaugeLevel by remember { mutableFloatStateOf(DemoScenarios.NEUTRAL_GAUGE_LEVEL) }

    // Helper: snap card animatables to starting position for a given style
    suspend fun initCard(s: TransitionStyle) {
        when (s) {
            TransitionStyle.SlideFromRight -> { cardOffsetX.snapTo(220f);  cardOffsetY.snapTo(0f);  cardScale.snapTo(1f)    }
            TransitionStyle.SlideFromLeft  -> { cardOffsetX.snapTo(-220f); cardOffsetY.snapTo(0f);  cardScale.snapTo(1f)    }
            TransitionStyle.RiseFromCenter -> { cardOffsetX.snapTo(0f);    cardOffsetY.snapTo(28f); cardScale.snapTo(0.92f) }
        }
    }

    if (reduceMotion) {
        LaunchedEffect(Unit) {
            smsAlpha.snapTo(0f)
            cardAlpha.snapTo(1f); cardOffsetX.snapTo(0f); cardOffsetY.snapTo(0f); cardScale.snapTo(1f)
            gaugeLevel = scenario.targetGauge
        }
    } else {
        LaunchedEffect(currentState, scenario) {
            when (currentState) {

                // ── Hard snap reset ──────────────────────────────────────────
                DemoStageState.Idle -> {
                    smsAlpha.snapTo(0f)
                    smsOffsetY.snapTo(50f)
                    cardAlpha.snapTo(0f)
                    initCard(style)
                    gaugeLevel = DemoScenarios.NEUTRAL_GAUGE_LEVEL
                }

                // ── SMS slides up + fades in smoothly ────────────────────────
                DemoStageState.SmsEntering -> {
                    cardAlpha.snapTo(0f)
                    coroutineScope {
                        launch {
                            smsAlpha.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                        }
                        launch {
                            smsOffsetY.animateTo(
                                0f,
                                spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
                            )
                        }
                    }
                }

                DemoStageState.SmsHolding -> { /* smooth hold, no sudden pop */ }

                // ── MORPH: Phase 1 SMS exits → Phase 2 card enters ──────────
                // Card glides in with simultaneous digit roll and gauge reaction
                DemoStageState.Morphing -> {
                    // SMS exits cleanly upward
                    coroutineScope {
                        launch { smsAlpha.animateTo(0f, tween(160, easing = FastOutSlowInEasing)) }
                        launch { smsOffsetY.animateTo(-24f, tween(180, easing = FastOutSlowInEasing)) }
                    }

                    // Card enters + gauge animates simultaneously
                    coroutineScope {
                        launch {
                            cardAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                        }
                        launch {
                            gaugeLevel = scenario.targetGauge
                        }
                        when (style) {
                            TransitionStyle.SlideFromRight -> launch {
                                cardOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                            }
                            TransitionStyle.SlideFromLeft -> launch {
                                cardOffsetX.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                            }
                            TransitionStyle.RiseFromCenter -> {
                                launch {
                                    cardOffsetY.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow))
                                }
                                launch {
                                    cardScale.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        }
                    }
                }

                DemoStageState.CardLanded  -> {
                    gaugeLevel = scenario.targetGauge
                }

                DemoStageState.GaugeReacting -> {
                    gaugeLevel = scenario.targetGauge
                }

                DemoStageState.Holding -> { /* peaceful hold */ }

                // ── Card exits — style-specific reverse ──────────────────────
                DemoStageState.Exiting -> {
                    coroutineScope {
                        launch { cardAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
                        when (style) {
                            TransitionStyle.SlideFromRight -> launch {
                                cardOffsetX.animateTo(
                                    -120f,
                                    tween(220, easing = FastOutSlowInEasing)
                                )
                            }
                            TransitionStyle.SlideFromLeft -> launch {
                                cardOffsetX.animateTo(
                                    120f,
                                    tween(220, easing = FastOutSlowInEasing)
                                )
                            }
                            TransitionStyle.RiseFromCenter -> {
                                launch { cardOffsetY.animateTo(-20f, tween(220, easing = FastOutSlowInEasing)) }
                                launch { cardScale.animateTo(0.95f, tween(200, easing = FastOutSlowInEasing)) }
                            }
                        }
                    }
                    gaugeLevel = DemoScenarios.NEUTRAL_GAUGE_LEVEL
                }
            }
        }
    }

    val talkback = stringResource(scenario.semanticsResId)

    // No camera zoom wrapper — clean flat viewport
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) { contentDescription = talkback },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            // SMS card
            if (smsAlpha.value > 0.001f) {
                DemoSmsCard(
                    scenario  = scenario,
                    modifier  = Modifier
                        .fillMaxWidth(0.88f)
                        .graphicsLayer {
                            alpha        = smsAlpha.value
                            translationY = smsOffsetY.value * density
                        }
                )
            }

            // Transaction card + gauge (smooth simultaneous entry)
            if (cardAlpha.value > 0.001f || reduceMotion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .graphicsLayer {
                            alpha        = cardAlpha.value
                            translationX = cardOffsetX.value * density
                            translationY = cardOffsetY.value * density
                            scaleX       = cardScale.value
                            scaleY       = cardScale.value
                        },
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DemoTransactionCard(
                        scenario      = scenario,
                        animateDigits = !reduceMotion && currentState >= DemoStageState.Morphing,
                        isActive      = currentState >= DemoStageState.Morphing && currentState < DemoStageState.Exiting,
                        modifier      = Modifier.weight(1f)
                    )
                    OnboardingMiniGauge(
                        level       = gaugeLevel,
                        accentColor = if (scenario.isDebit) ExpenseRed else IncomeGreen,
                        size        = 76.dp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DemoSmsCard - Clean, modern bank notification with crisp, elegant details
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DemoSmsCard(
    scenario:  DemoScenario,
    modifier:  Modifier = Modifier
) {
    val glassTheme   = rememberKanriGlassTheme()
    val smsText      = stringResource(scenario.smsResId)
    val surfaceColor = glassTheme.fallbackBackgroundColor.copy(alpha = 1f)

    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(20.dp),
        color           = surfaceColor,
        border          = BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.12f)
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier         = Modifier.size(22.dp).clip(CircleShape).background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalance, null, Modifier.size(12.dp), tint = Color(0xFFCCCCCC))
                    }
                    Text(
                        stringResource(R.string.onboarding_bank_sender),
                        fontFamily = GoogleSansFlex, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp,
                        color = Color(0xFFDDDDDD)
                    )
                }
                Text(
                    stringResource(R.string.onboarding_timestamp_now),
                    fontFamily = GoogleSansFlex, fontSize = 10.5.sp, color = Color(0xFF777777)
                )
            }

            Spacer(Modifier.height(8.dp))

            val annotated = remember(smsText, scenario) {
                buildAnnotatedString {
                    var cursor = 0
                    val hits = listOf(scenario.highlightAmount, scenario.highlightPayee)
                        .filter { smsText.contains(it) }
                        .map { it to smsText.indexOf(it) }
                        .sortedBy { it.second }
                    if (hits.isEmpty()) {
                        append(smsText)
                    } else {
                        hits.forEach { (word, idx) ->
                            if (idx >= cursor) {
                                append(smsText.substring(cursor, idx))
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                ) {
                                    append(word)
                                }
                                cursor = idx + word.length
                            }
                        }
                        if (cursor < smsText.length) append(smsText.substring(cursor))
                    }
                }
            }
            Text(
                annotated,
                fontFamily = GoogleSansFlex,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = Color(0xFFAAAAAA),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DemoTransactionCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DemoTransactionCard(
    scenario:      DemoScenario,
    animateDigits: Boolean,
    isActive:      Boolean,
    modifier:      Modifier = Modifier
) {
    val glassTheme   = rememberKanriGlassTheme()
    val payee        = stringResource(scenario.payeeResId)
    val categoryName = stringResource(scenario.categoryResId)
    val sourceName   = stringResource(scenario.sourceResId)
    val accentColor  = if (scenario.isDebit) ExpenseRed else IncomeGreen
    val surfaceColor = glassTheme.fallbackBackgroundColor.copy(alpha = 1f)

    val iconScale by animateFloatAsState(
        targetValue   = if (isActive) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label         = "iconScale"
    )
    val detailsOffsetX by animateFloatAsState(
        targetValue   = if (isActive) 0f else 14f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow),
        label         = "detailsX"
    )

    Surface(
        modifier        = modifier,
        shape           = RoundedCornerShape(20.dp),
        color           = surfaceColor,
        border          = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) accentColor.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.10f)
        ),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    categoryName = categoryName,
                    iconName     = scenario.categoryIconName,
                    tint         = MaterialTheme.colorScheme.onSurface,
                    modifier     = Modifier.size(21.dp)
                )
            }

            Spacer(Modifier.width(11.dp))

            Column(
                modifier = Modifier.weight(1f).graphicsLayer { translationX = detailsOffsetX * density }
            ) {
                Text(payee, fontFamily = GoogleSansFlex, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF2A2A2A)) {
                        Text(sourceName, fontFamily = GoogleSansFlex, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888), modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp))
                    }
                    Text("• $categoryName", fontFamily = GoogleSansFlex, fontSize = 10.sp, color = Color(0xFF777777), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.graphicsLayer { translationX = detailsOffsetX * 0.5f * density }
            ) {
                if (!scenario.isDebit) {
                    Text("+", fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accentColor)
                }
                AnimatedNumber(
                    value           = if (animateDigits) scenario.rawAmount else 0.0,
                    style           = MaterialTheme.typography.titleMedium.copy(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color           = accentColor,
                    durationMillis  = 600,
                    animateFromZero = true
                )
            }
        }
    }
}