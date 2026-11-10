package com.omkarnub.kanri.widget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.omkarnub.kanri.widget.core.ClimateStatus
import com.omkarnub.kanri.widget.core.PacingStatus
import com.omkarnub.kanri.widget.core.PixelTheme
import com.omkarnub.kanri.widget.core.TodayWidgetState
import com.omkarnub.kanri.widget.core.BudgetWidgetState
import com.omkarnub.kanri.widget.core.WalletsWidgetState
import com.omkarnub.kanri.widget.core.GoalsWidgetState
import com.omkarnub.kanri.widget.core.DuesWidgetState
import com.omkarnub.kanri.widget.core.SkyWidgetState
import com.omkarnub.kanri.widget.core.HeatmapWidgetState
import com.omkarnub.kanri.widget.core.StreakWidgetState
import com.omkarnub.kanri.widget.core.SafeToSpendWidgetState
import com.omkarnub.kanri.widget.core.ReviewInboxWidgetState
import com.omkarnub.kanri.widget.core.MonthDeltaWidgetState
import com.omkarnub.kanri.widget.core.QuickActionsWidgetState
import com.omkarnub.kanri.widget.core.WidgetState
import com.omkarnub.kanri.widget.font.PixelFont
import com.omkarnub.kanri.widget.visuals.DotVisuals
import kotlin.math.abs
import kotlin.math.sin

/**
 * Pure Canvas renderer for all 12 Kanri Pixel DotTiles widgets.
 * Combines authentic Nothing Ndot57 dot-matrix typography for numbers
 * with clean, high-contrast system typography and fluid tactile data visuals.
 * Layout geometry is dynamically calculated with strict vertical bounding zones
 * to mathematically eliminate any overlap across all widget sizes (2x1, 2x2, 4x1, 4x2, 4x3).
 */
object WidgetRenderer {

    private val bgPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_BG_INT
            style = Paint.Style.FILL
        }
    }

    private val borderPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    }

    private val innerRimPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
    }

    private val terrainPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_DOT_DIM_INT
            style = Paint.Style.FILL
        }
    }

    /**
     * Renders [state] to [canvas] within ([widthPx], [heightPx]).
     */
    fun render(
        canvas: Canvas,
        state: WidgetState,
        widthPx: Float,
        heightPx: Float,
        progress: Float = 1.0f,
        density: Float = 2.5f,
        jitterX: Float = 0f,
        jitterY: Float = 0f,
        context: Context? = null
    ) {
        if (widthPx <= 0f || heightPx <= 0f) return

        val cornerRadius = (20f * density).coerceAtMost(heightPx * 0.25f)
        val padding = (12f * density).coerceIn(8f * density, 16f * density)

        // Calculate stroke and insets so corners and edges are 100% inside canvas buffer
        val strokeWidthPx = (1.5f * density).coerceIn(2.5f, 5f)
        val inset = strokeWidthPx / 2f + 0.8f
        val tileRect = RectF(inset, inset, widthPx - inset, heightPx - inset)

        // 1. Solid Obsidian tile background
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, bgPaint)

        // 2. Tactile outer border with top-rim and corner specular highlight
        borderPaint.strokeWidth = strokeWidthPx
        borderPaint.shader = LinearGradient(
            0f, inset,
            0f, heightPx - inset,
            intArrayOf(
                PixelTheme.COLOR_BORDER_HIGHLIGHT_INT,
                PixelTheme.COLOR_BORDER_INT,
                PixelTheme.COLOR_BORDER_LOWLIGHT_INT
            ),
            floatArrayOf(0f, 0.40f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint)
        borderPaint.shader = null

        // 3. Subtle secondary inner corner shine along upper edge
        val innerRimInset = inset + 1.2f * density
        if (widthPx > innerRimInset * 2 && heightPx > innerRimInset * 2) {
            val innerRect = RectF(innerRimInset, innerRimInset, widthPx - innerRimInset, heightPx - innerRimInset)
            val innerRadius = (cornerRadius - 1.2f * density).coerceAtLeast(4f)
            innerRimPaint.strokeWidth = 1f * density
            innerRimPaint.shader = LinearGradient(
                0f, innerRimInset,
                0f, innerRimInset + 24f * density,
                intArrayOf(0x30FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(innerRect, innerRadius, innerRadius, innerRimPaint)
            innerRimPaint.shader = null
        }

        // Save canvas for subtle ambient jitter drift if enabled
        canvas.save()
        if (jitterX != 0f || jitterY != 0f) {
            canvas.translate(jitterX, jitterY)
        }

        val left = padding
        val top = padding
        val right = widthPx - padding
        val bottom = heightPx - padding
        val width = right - left
        val height = bottom - top

        when (state) {
            is TodayWidgetState -> renderToday(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is BudgetWidgetState -> renderBudget(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is WalletsWidgetState -> renderWallets(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is GoalsWidgetState -> renderGoals(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is DuesWidgetState -> renderDues(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is SkyWidgetState -> renderSky(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is HeatmapWidgetState -> renderHeatmap(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is StreakWidgetState -> renderStreak(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is SafeToSpendWidgetState -> renderSafeToSpend(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is ReviewInboxWidgetState -> renderReviewInbox(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is MonthDeltaWidgetState -> renderMonthDelta(canvas, context, state, left, top, right, bottom, width, height, progress, density)
            is QuickActionsWidgetState -> renderQuickActions(canvas, context, state, left, top, right, bottom, width, height, progress, density)
        }

        canvas.restore()
    }

    /**
     * Renders [state] to a hardware/software Bitmap.
     */
    fun renderToBitmap(
        state: WidgetState,
        widthPx: Int,
        heightPx: Int,
        progress: Float = 1.0f,
        density: Float = 2.5f,
        context: Context? = null
    ): Bitmap {
        val w = widthPx.coerceIn(120, 1400)
        val h = heightPx.coerceIn(80, 1000)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        render(canvas, state, w.toFloat(), h.toFloat(), progress, density, context = context)
        return bitmap
    }

    // --- 1. TODAY WIDGET (2x2, 4x2) ---
    private fun renderToday(
        canvas: Canvas,
        context: Context?,
        state: TodayWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Today", left, top, titleSize)
        val captionText = if (state.expenseCount > 0) "${state.expenseCount} expenses" else "No spend today"
        PixelFont.drawSubtitle(canvas, captionText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 1f * density, iconSize)

        // Visual zone at bottom
        val visualH = if (isCompactH) 10f * density else 12f * density
        val visualBottom = if (isCompactH) bottom else bottom - 12f * density
        val visualTop = visualBottom - visualH
        val timelineBounds = RectF(left, visualTop, right, visualBottom)

        // Hero spend zone strictly bounded between header and visual
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val animSpent = state.spentToday * progress.coerceIn(0f, 1f)
        val heroText = PixelFont.formatIndianCompact(animSpent)
        val heroColor = when (state.pacingStatus) {
            PacingStatus.OVER -> PixelTheme.COLOR_CORAL_INT
            PacingStatus.WARNING -> PixelTheme.COLOR_AMBER_INT
            PacingStatus.HEALTHY -> PixelTheme.COLOR_DOT_ON_INT
        }
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom Dots timeline with progress animation
        val labels = if (isCompactH) null else Pair("00H", "23H")
        DotVisuals.drawDots(
            canvas, timelineBounds, state.hourlySpend,
            activeSlotIndex = state.currentHour,
            dotColor = heroColor,
            labels = labels,
            density = density,
            progress = progress
        )
    }

    // --- 2. BUDGET WIDGET (2x1, 2x2, 4x2) ---
    private fun renderBudget(
        canvas: Canvas,
        context: Context?,
        state: BudgetWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        if (!state.hasBudget) {
            PixelFont.drawTitle(canvas, "Budget", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "Not configured", left, top + titleSize + 2.5f * density, subSize)
            val heroSize = (18f * density).coerceIn(14f, height * 0.28f)
            PixelFont.drawHeroMetric(canvas, context, "Set Budget", left, top + titleSize + subSize + heroSize + 4f * density, heroSize, PixelTheme.COLOR_AMBER_INT, progress)
            return
        }

        val isOver = state.remainingAmount < 0
        val titleText = if (isOver) "Over Budget" else "Remaining"
        val titleColor = if (isOver) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT
        PixelFont.drawTitle(canvas, titleText, left, top, titleSize, titleColor)

        val pctInt = ((state.percentUsed * progress.coerceIn(0f, 1f)) * 100).toInt()
        val subText = "$pctInt% used · ${state.daysLeft}d left"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 1f * density, iconSize)

        // Visual zone at bottom
        val visualH = if (isCompactH) 8f * density else 12f * density
        val visualBottom = if (isCompactH) bottom else bottom - 10f * density
        val visualTop = visualBottom - visualH
        val sliderBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone strictly between headerBottom and visualTop
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val animRemaining = abs(state.remainingAmount) * progress.coerceIn(0f, 1f)
        val heroText = PixelFont.formatIndianCompact(animRemaining)
        val accentColor = when {
            isOver -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.90f -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.70f -> PixelTheme.COLOR_AMBER_INT
            else -> PixelTheme.COLOR_MINT_INT
        }
        val displayPrefix = if (isOver) "+ " else ""
        PixelFont.drawHeroMetric(canvas, context, displayPrefix + heroText, left, heroBaseline, heroSize, accentColor, progress = 1.0f)

        // Bottom Slider with progress animation
        val labels = if (isCompactH) null else Pair("0%", "100%")
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentUsed,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = accentColor,
            pacingMarker = state.pacingExpectedRatio,
            labels = labels,
            density = density,
            progress = progress
        )
    }

    // --- 3. WALLETS WIDGET (2x2, 4x2) ---
    private fun renderWallets(
        canvas: Canvas,
        context: Context?,
        state: WalletsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Wallets", left, top, titleSize)
        PixelFont.drawSubtitle(canvas, "Cash & Bank accounts", left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BOLT, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = (12f * density).coerceIn(8f, 16f * density)
        val visualBounds = RectF(left, bottom - visualH - 2f * density, right, bottom - 2f * density)

        // Content rows
        val headerBottom = top + titleSize + subSize + 8f * density
        val availHeight = visualBounds.top - headerBottom
        val rowSpacing = (availHeight / 3f).coerceIn(14f * density, 22f * density)
        val labelSize = (11f * density).coerceIn(9f, 13f * density)

        val rowY1 = headerBottom + rowSpacing * 0.7f
        val rowY2 = rowY1 + rowSpacing
        val rowY3 = rowY2 + rowSpacing

        val animP = progress.coerceIn(0f, 1f)

        // Cash Row
        PixelFont.drawLabel(canvas, "CASH", left, rowY1, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        val cashStr = PixelFont.formatIndianCompact(state.cashBalance * animP)
        PixelFont.drawRightLabel(canvas, cashStr, right, rowY1, labelSize, if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

        // Bank Row
        PixelFont.drawLabel(canvas, "BANK", left, rowY2, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        val bankStr = PixelFont.formatIndianCompact(state.bankBalance * animP)
        PixelFont.drawRightLabel(canvas, bankStr, right, rowY2, labelSize, if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

        // Net Worth Row
        PixelFont.drawLabel(canvas, "NET", left, rowY3, labelSize, PixelTheme.COLOR_MINT_INT)
        val netStr = PixelFont.formatIndianCompact(state.netWorth * animP)
        PixelFont.drawRightLabel(canvas, netStr, right, rowY3, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Bottom Rise proportion gauge
        DotVisuals.drawRise(canvas, visualBounds, state.cashProportion, tiers = 5, activeColor = PixelTheme.COLOR_MINT_INT, density = density, progress = progress)
    }

    // --- 4. GOALS WIDGET (2x2, 4x1) ---
    private fun renderGoals(
        canvas: Canvas,
        context: Context?,
        state: GoalsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        if (!state.hasGoal) {
            PixelFont.drawTitle(canvas, "Savings Goal", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "No active goal", left, top + titleSize + 2.5f * density, subSize)
            val heroSize = (18f * density).coerceIn(14f, height * 0.28f)
            PixelFont.drawHeroMetric(canvas, context, "Add Goal", left, top + titleSize + subSize + heroSize + 4f * density, heroSize, PixelTheme.COLOR_AMBER_INT, progress)
            return
        }

        val title = if (state.goalName.isNotEmpty()) state.goalName else "Savings Goal"
        PixelFont.drawTitle(canvas, title, left, top, titleSize)
        val curStr = PixelFont.formatIndianCompact(state.savedAmount * progress.coerceIn(0f, 1f))
        val tarStr = PixelFont.formatIndianCompact(state.targetAmount)
        PixelFont.drawSubtitle(canvas, "$curStr of $tarStr", left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = if (isCompactH) 8f * density else 12f * density
        val visualBottom = if (isCompactH) bottom else bottom - 10f * density
        val visualTop = visualBottom - visualH
        val sliderBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val pctInt = ((state.percentSaved * progress.coerceIn(0f, 1f)) * 100).toInt()
        val heroText = "$pctInt%"
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)

        // Bottom Slider
        val labels = if (isCompactH) null else Pair("0%", "100%")
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentSaved,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = PixelTheme.COLOR_MINT_INT,
            labels = labels,
            density = density,
            progress = progress
        )
    }

    // --- 5. DUES WIDGET (2x2, 4x2) ---
    private fun renderDues(
        canvas: Canvas,
        context: Context?,
        state: DuesWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Lend & Borrow", left, top, titleSize)
        val subText = if (state.netAmount >= 0) "Net receivable" else "Net payable"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.DROP, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = (12f * density).coerceIn(8f, 16f * density)
        val visualBounds = RectF(left, bottom - visualH - 2f * density, right, bottom - 2f * density)

        val headerBottom = top + titleSize + subSize + 8f * density
        val availHeight = visualBounds.top - headerBottom
        val rowSpacing = (availHeight / 3f).coerceIn(14f * density, 22f * density)
        val labelSize = (11f * density).coerceIn(9f, 13f * density)

        val rowY1 = headerBottom + rowSpacing * 0.7f
        val rowY2 = rowY1 + rowSpacing
        val rowY3 = rowY2 + rowSpacing

        val animP = progress.coerceIn(0f, 1f)

        // Receivable Row (Mint)
        PixelFont.drawLabel(canvas, "GET", left, rowY1, labelSize, PixelTheme.COLOR_MINT_INT)
        val getStr = PixelFont.formatIndianCompact(state.receivableAmount * animP)
        PixelFont.drawRightLabel(canvas, getStr, right, rowY1, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Payable Row (Coral)
        PixelFont.drawLabel(canvas, "OWE", left, rowY2, labelSize, PixelTheme.COLOR_CORAL_INT)
        val oweStr = PixelFont.formatIndianCompact(state.payableAmount * animP)
        PixelFont.drawRightLabel(canvas, oweStr, right, rowY2, labelSize, PixelTheme.COLOR_CORAL_INT, useNdot = true, context = context)

        // Net Row
        val netColor = if (state.netAmount >= 0) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
        PixelFont.drawLabel(canvas, "NET", left, rowY3, labelSize, netColor)
        val netPrefix = if (state.netAmount >= 0) "+ " else "- "
        val netStr = netPrefix + PixelFont.formatIndianCompact(abs(state.netAmount) * animP)
        PixelFont.drawRightLabel(canvas, netStr, right, rowY3, labelSize, netColor, useNdot = true, context = context)

        // Bottom Rise proportion gauge (Mint receivable ratio)
        DotVisuals.drawRise(canvas, visualBounds, state.receivableRatio, tiers = 6, activeColor = PixelTheme.COLOR_MINT_INT, density = density, progress = progress)
    }

    // --- 6. SKY WIDGET (Financial Weather Flagship, 4x2) ---
    private fun renderSky(
        canvas: Canvas,
        context: Context?,
        state: SkyWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)

        // Header
        val statusText = state.climateStatus.name
        val statusColor = when (state.climateStatus) {
            ClimateStatus.CLEAR, ClimateStatus.SUNRISE, ClimateStatus.STARRY -> PixelTheme.COLOR_MINT_INT
            ClimateStatus.CLOUDY -> PixelTheme.COLOR_AMBER_INT
            ClimateStatus.STORM -> PixelTheme.COLOR_CORAL_INT
        }
        PixelFont.drawTitle(canvas, "Kanri · Weather", left, top, titleSize)
        PixelFont.drawSubtitle(canvas, "Pacing: $statusText", left, top + titleSize + 2.5f * density, subSize, statusColor)

        // Bottom 7-day pulse row
        val visualH = (12f * density).coerceIn(10f, 16f * density)
        val pulseBounds = RectF(left, bottom - visualH - 10f * density, right, bottom - 10f * density)
        DotVisuals.drawSteps(
            canvas, pulseBounds, state.sevenDayPulse,
            maxDotsPerBar = 4,
            activeColor = statusColor,
            labels = Pair("7D AGO", "TODAY"),
            density = density,
            progress = progress
        )

        // Ground metrics row right above pulse
        val groundBaseline = pulseBounds.top - 8f * density
        val labelSize = 10f * density
        val animToday = state.todaySpent * progress.coerceIn(0f, 1f)
        val animSafe = state.safeToSpend * progress.coerceIn(0f, 1f)
        val todayStr = "SPENT " + PixelFont.formatIndianCompact(animToday)
        val safeStr = "SAFE " + PixelFont.formatIndianCompact(animSafe)
        PixelFont.drawLabel(canvas, todayStr, left, groundBaseline, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)
        PixelFont.drawRightLabel(canvas, safeStr, right, groundBaseline, labelSize, PixelTheme.COLOR_CYAN_INT, useNdot = true, context = context)

        // Recessed Celestial Sky Well strictly bounded
        val skyTop = top + titleSize + subSize + 6f * density
        val skyBottom = groundBaseline - 12f * density
        val skyRect = RectF(left, skyTop, right, skyBottom)

        if (skyRect.height() > 16f * density) {
            val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF050508.toInt()
                style = Paint.Style.FILL
            }
            val wellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x14FFFFFF
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val wellRadius = 10f * density
            canvas.drawRoundRect(skyRect, wellRadius, wellRadius, wellPaint)
            canvas.drawRoundRect(skyRect, wellRadius, wellRadius, wellBorderPaint)

            // Celestial Arc inside skyRect
            val celestialColor = if (state.isDay) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_CYAN_INT
            DotVisuals.drawArc(
                canvas, skyRect,
                progress = state.sunProgress * progress,
                onColor = celestialColor,
                density = density
            )

            // Twinkling stars if night
            if (!state.isDay) {
                val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PixelTheme.COLOR_DOT_ON_INT }
                val starCoords = listOf(
                    Pair(skyRect.left + skyRect.width() * 0.18f, skyRect.top + skyRect.height() * 0.28f),
                    Pair(skyRect.left + skyRect.width() * 0.42f, skyRect.top + skyRect.height() * 0.20f),
                    Pair(skyRect.left + skyRect.width() * 0.68f, skyRect.top + skyRect.height() * 0.32f),
                    Pair(skyRect.left + skyRect.width() * 0.84f, skyRect.top + skyRect.height() * 0.22f)
                )
                starCoords.forEach { (sx, sy) ->
                    canvas.drawCircle(sx, sy, 1.2f * density, starPaint)
                }
            }

            // Rolling hill terrain silhouette at bottom of sky well
            val hillCols = 16
            val colWidth = skyRect.width() / hillCols
            val horizonY = skyRect.bottom
            for (i in 0 until hillCols) {
                val hx = skyRect.left + i * colWidth
                val hillHeight = (2f + 4f * sin((i.toDouble() / hillCols) * Math.PI).toFloat()) * density
                canvas.drawRect(hx, horizonY - hillHeight, hx + colWidth - 0.8f, horizonY, terrainPaint)
            }
        }
    }

    // --- 7. MONTH HEATMAP WIDGET (4x3, 4x2) ---
    private fun renderHeatmap(
        canvas: Canvas,
        context: Context?,
        state: HeatmapWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)

        val monthLabel = if (state.monthName.isNotEmpty()) state.monthName else "Month Spend"
        PixelFont.drawTitle(canvas, monthLabel, left, top, titleSize)
        val animTotal = state.monthTotal * progress.coerceIn(0f, 1f)
        val totalStr = PixelFont.formatIndianCompact(animTotal)
        PixelFont.drawSubtitle(canvas, "Total: $totalStr", left, top + titleSize + 2.5f * density, subSize)
        PixelFont.drawRightLabel(canvas, totalStr, right, top + titleSize * 0.8f, titleSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Heatmap Calendar Grid (7 columns x 5 rows)
        val gridTop = top + titleSize + subSize + 6f * density
        val gridBounds = RectF(left, gridTop, right, bottom - 2f * density)
        DotVisuals.drawGrid(
            canvas, gridBounds,
            cols = 7, rows = 5,
            intensities = state.dailyIntensities,
            todayIndex = state.todayCellIndex,
            onColor = PixelTheme.COLOR_MINT_INT,
            density = density,
            progress = progress
        )
    }

    // --- 8. NO-SPEND STREAK WIDGET (2x2, 4x1) ---
    private fun renderStreak(
        canvas: Canvas,
        context: Context?,
        state: StreakWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "No-Spend Streak", left, top, titleSize)
        val subText = "Best: ${state.bestStreak} days"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.FLAME, right - iconSize, top + 1f * density, iconSize, PixelTheme.COLOR_AMBER_INT)

        // Visual zone
        val visualBottom = bottom - 2f * density
        val visualTop = visualBottom - 10f * density

        // Hero zone
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val animStreak = (state.currentStreak * progress.coerceIn(0f, 1f)).toInt()
        val heroText = "$animStreak DAYS"
        val heroColor = if (state.currentStreak > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom 30-day dot row
        val rowY = visualBottom - 4f * density
        val count = 30
        val spacing = width / count.coerceAtLeast(1)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * density }
        val animProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until count) {
            val cx = left + i * spacing + spacing / 2f
            val isNoSpend = if (i < state.last30Days.size) state.last30Days[i] else false
            val dotP = (animProgress * count - i).coerceIn(0f, 1f)

            if (isNoSpend && dotP > 0f) {
                fillP.color = PixelTheme.COLOR_AMBER_INT
                canvas.drawCircle(cx, rowY, 2f * density * dotP, fillP)
            } else {
                strokeP.color = PixelTheme.COLOR_DOT_DIM_INT
                canvas.drawCircle(cx, rowY, 1.4f * density, strokeP)
            }

            if (i == state.todayIndex && animProgress > 0.4f) {
                strokeP.color = PixelTheme.COLOR_CYAN_INT
                canvas.drawCircle(cx, rowY, 3.8f * density, strokeP)
            }
        }
    }

    // --- 9. SAFE TO SPEND WIDGET (2x2, 4x1) ---
    private fun renderSafeToSpend(
        canvas: Canvas,
        context: Context?,
        state: SafeToSpendWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Safe to Spend", left, top, titleSize)
        val animToday = state.todaySpent * progress.coerceIn(0f, 1f)
        val caption = "Today: " + PixelFont.formatIndianCompact(animToday)
        PixelFont.drawSubtitle(canvas, caption, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = if (isCompactH) 8f * density else 12f * density
        val visualBottom = if (isCompactH) bottom else bottom - 10f * density
        val visualTop = visualBottom - visualH
        val sliderBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val animAllowance = state.dailyAllowance * progress.coerceIn(0f, 1f)
        val heroText = PixelFont.formatIndianCompact(animAllowance) + " /d"
        val heroColor = if (state.isOverAllowance) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom Slider
        val labels = if (isCompactH) null else Pair("₹0", PixelFont.formatIndianCompact(state.dailyAllowance))
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.spentRatio,
            mode = DotVisuals.SliderMode.GLOW,
            markerColor = heroColor,
            labels = labels,
            density = density,
            progress = progress
        )
    }

    // --- 10. REVIEW INBOX WIDGET (2x1, 2x2) ---
    private fun renderReviewInbox(
        canvas: Canvas,
        context: Context?,
        state: ReviewInboxWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Review Inbox", left, top, titleSize)
        val isClear = state.pendingCount == 0
        val animTotal = state.pendingTotalAmount * progress.coerceIn(0f, 1f)
        val subText = if (isClear) "All caught up" else "Total: " + PixelFont.formatIndianCompact(animTotal)
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.INBOX, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = (10f * density).coerceIn(8f, 14f * density)
        val visualBottom = bottom - 2f * density
        val visualTop = visualBottom - visualH
        val blocksBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val animCount = (state.pendingCount * progress.coerceIn(0f, 1f)).toInt()
        val heroText = if (isClear) "Clear" else "$animCount SMS"
        val heroColor = if (isClear) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_AMBER_INT
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom Blocks visual
        DotVisuals.drawBlocks(
            canvas, blocksBounds,
            count = state.pendingCount,
            maxSlots = 12,
            activeColor = PixelTheme.COLOR_AMBER_INT,
            density = density,
            progress = progress
        )
    }

    // --- 11. MONTH DELTA WIDGET (2x2, 4x2) ---
    private fun renderMonthDelta(
        canvas: Canvas,
        context: Context?,
        state: MonthDeltaWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "Month Delta", left, top, titleSize)
        val animCur = state.currentMonthSpendToDate * progress.coerceIn(0f, 1f)
        val curStr = PixelFont.formatIndianCompact(animCur)
        val prevStr = PixelFont.formatIndianCompact(state.previousMonthSpendToDate)
        PixelFont.drawSubtitle(canvas, "$curStr vs $prevStr (day ${state.dayOfMonth})", left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.PULSE, right - iconSize, top + 1f * density, iconSize)

        // Visual zone
        val visualH = if (isCompactH) 8f * density else 12f * density
        val visualBottom = if (isCompactH) bottom else bottom - 10f * density
        val visualTop = visualBottom - visualH
        val sliderBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.52f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.65f)

        val sign = if (state.isSpendingMore) "+" else "-"
        val animDelta = (state.deltaPercent * progress.coerceIn(0f, 1f)).toInt()
        val pctText = "$sign$animDelta%"
        val heroColor = if (state.isSpendingMore) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        PixelFont.drawHeroMetric(canvas, context, pctText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom Comparison Slider with progress
        val labels = if (isCompactH) null else Pair("LAST MO", "THIS MO")
        val ratio = (state.currentMonthSpendToDate / (state.previousMonthSpendToDate.coerceAtLeast(1.0))).toFloat()
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = ratio.coerceIn(0f, 1f),
            mode = DotVisuals.SliderMode.GLOW,
            markerColor = heroColor,
            labels = labels,
            density = density,
            progress = progress
        )
    }

    // --- 12. QUICK ACTIONS WIDGET (4x1, 2x1) ---
    private fun renderQuickActions(
        canvas: Canvas,
        context: Context?,
        state: QuickActionsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val count = 3
        val colWidth = width / count
        val actions = listOf(
            Triple("Expense", DotVisuals.DotIcon.PLUS, PixelTheme.COLOR_CORAL_INT),
            Triple("Income", DotVisuals.DotIcon.DROP, PixelTheme.COLOR_MINT_INT),
            Triple("Split", DotVisuals.DotIcon.SPLIT, PixelTheme.COLOR_CYAN_INT)
        )

        val btnPadding = 4f * density
        val btnRadius = (12f * density).coerceAtMost(height * 0.25f)

        val btnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF14141A.toInt()
            style = Paint.Style.FILL
        }
        val btnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val animProgress = progress.coerceIn(0f, 1f)

        for (i in 0 until count) {
            val (label, icon, color) = actions[i]
            val bx1 = left + i * colWidth + btnPadding
            val bx2 = left + (i + 1) * colWidth - btnPadding
            val by1 = top + btnPadding
            val by2 = bottom - btnPadding

            val btnRect = RectF(bx1, by1, bx2, by2)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBgPaint)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBorderPaint)

            // Draw Icon scaled by animation progress
            val iconSize = (18f * density * animProgress).coerceIn(10f, 22f * density)
            val ix = btnRect.centerX() - iconSize / 2f
            val iy = btnRect.centerY() - iconSize * 0.8f
            DotVisuals.drawIcon(canvas, icon, ix, iy, iconSize, color)

            // Draw Label
            val labelSize = (9.5f * density).coerceIn(8f, 11f * density)
            PixelFont.drawLabel(
                canvas, label,
                btnRect.centerX() - (label.length * 2.8f * density),
                btnRect.bottom - 6f * density,
                labelSize, PixelTheme.COLOR_DOT_ON_INT
            )
        }
    }
}
