package com.omkarnub.kanri.widget.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
 * with clean, high-contrast system typography and tactile data visuals.
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
            color = PixelTheme.COLOR_BORDER_INT
            style = Paint.Style.STROKE
            strokeWidth = 2f
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

        val cornerRadius = 22f * density
        val padding = (14f * density).coerceAtMost(widthPx * 0.08f)

        // Draw tile background with 1px inner border
        val tileRect = RectF(1.5f, 1.5f, widthPx - 1.5f, heightPx - 1.5f)
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint)

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

    // --- 1. TODAY WIDGET ---
    private fun renderToday(
        canvas: Canvas,
        context: Context?,
        state: TodayWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Today", left, top, titleSize)
        val captionText = if (state.expenseCount > 0) "${state.expenseCount} expenses" else "No spend today"
        PixelFont.drawSubtitle(canvas, captionText, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 2f * density, iconSize)

        // Hero spend
        val heroText = PixelFont.formatIndianCompact(state.spentToday)
        val heroColor = when (state.pacingStatus) {
            PacingStatus.OVER -> PixelTheme.COLOR_CORAL_INT
            PacingStatus.WARNING -> PixelTheme.COLOR_AMBER_INT
            PacingStatus.HEALTHY -> PixelTheme.COLOR_DOT_ON_INT
        }
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress)

        // Bottom Dots timeline
        val visualH = (18f * density).coerceIn(14f, 26f * density)
        val timelineBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        DotVisuals.drawDots(
            canvas, timelineBounds, state.hourlySpend,
            activeSlotIndex = state.currentHour,
            dotColor = heroColor,
            labels = Pair("00H", "23H"),
            density = density
        )
    }

    // --- 2. BUDGET WIDGET ---
    private fun renderBudget(
        canvas: Canvas,
        context: Context?,
        state: BudgetWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        if (!state.hasBudget) {
            PixelFont.drawTitle(canvas, "Budget", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "Not configured", left, top + titleSize + 3f * density, subSize)
            val heroSize = (26f * density).coerceIn(18f, height * 0.3f)
            PixelFont.drawHeroMetric(canvas, context, "Set Budget", left, top + titleSize + subSize + heroSize + 6f * density, heroSize, PixelTheme.COLOR_AMBER_INT, progress)
            PixelFont.drawLabel(canvas, "Tap to configure in app", left, bottom - 8f * density, 11f * density, PixelTheme.COLOR_DOT_MID_INT)
            return
        }

        val isOver = state.remainingAmount < 0
        val titleText = if (isOver) "Over Budget" else "Remaining"
        val titleColor = if (isOver) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT
        PixelFont.drawTitle(canvas, titleText, left, top, titleSize, titleColor)

        val pctInt = (state.percentUsed * 100).toInt()
        val subText = "$pctInt% used · ${state.daysLeft}d left"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 2f * density, iconSize)

        // Hero value
        val heroText = PixelFont.formatIndianCompact(abs(state.remainingAmount))
        val accentColor = when {
            isOver -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.90f -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.70f -> PixelTheme.COLOR_AMBER_INT
            else -> PixelTheme.COLOR_MINT_INT
        }
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        val displayPrefix = if (isOver) "+ " else ""
        PixelFont.drawHeroMetric(canvas, context, displayPrefix + heroText, left, heroBaseline, heroSize, accentColor, progress)

        // Bottom Slider
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val sliderBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentUsed,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = accentColor,
            pacingMarker = state.pacingExpectedRatio,
            labels = Pair("0%", "100%"),
            density = density
        )
    }

    // --- 3. WALLETS WIDGET ---
    private fun renderWallets(
        canvas: Canvas,
        context: Context?,
        state: WalletsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Wallets", left, top, titleSize)
        PixelFont.drawSubtitle(canvas, "Cash & Bank accounts", left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BOLT, right - iconSize, top + 2f * density, iconSize)

        val rowY1 = top + titleSize + subSize + 16f * density
        val rowY2 = rowY1 + 18f * density
        val rowY3 = rowY2 + 18f * density
        val labelSize = 12f * density

        // Cash Row
        PixelFont.drawLabel(canvas, "CASH", left, rowY1, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        val cashStr = PixelFont.formatIndianCompact(state.cashBalance)
        PixelFont.drawRightLabel(canvas, cashStr, right, rowY1, labelSize, if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

        // Bank Row
        PixelFont.drawLabel(canvas, "BANK", left, rowY2, labelSize, PixelTheme.COLOR_DOT_MID_INT)
        val bankStr = PixelFont.formatIndianCompact(state.bankBalance)
        PixelFont.drawRightLabel(canvas, bankStr, right, rowY2, labelSize, if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

        // Net Worth Row
        PixelFont.drawLabel(canvas, "NET", left, rowY3, labelSize, PixelTheme.COLOR_MINT_INT)
        val netStr = PixelFont.formatIndianCompact(state.netWorth)
        PixelFont.drawRightLabel(canvas, netStr, right, rowY3, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Bottom Rise proportion gauge
        val visualH = (14f * density).coerceIn(10f, 20f * density)
        val riseBounds = RectF(left, bottom - visualH - 4f * density, right, bottom - 4f * density)
        DotVisuals.drawRise(canvas, riseBounds, state.cashProportion, tiers = 5, activeColor = PixelTheme.COLOR_MINT_INT, density = density)
    }

    // --- 4. GOALS WIDGET ---
    private fun renderGoals(
        canvas: Canvas,
        context: Context?,
        state: GoalsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        if (!state.hasGoal) {
            PixelFont.drawTitle(canvas, "Savings Goal", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "No active goal", left, top + titleSize + 3f * density, subSize)
            val heroSize = (26f * density).coerceIn(18f, height * 0.3f)
            PixelFont.drawHeroMetric(canvas, context, "Add Goal", left, top + titleSize + subSize + heroSize + 6f * density, heroSize, PixelTheme.COLOR_AMBER_INT, progress)
            PixelFont.drawLabel(canvas, "Tap to create in app", left, bottom - 8f * density, 11f * density, PixelTheme.COLOR_DOT_MID_INT)
            return
        }

        val title = if (state.goalName.isNotEmpty()) state.goalName else "Savings Goal"
        PixelFont.drawTitle(canvas, title, left, top, titleSize)
        val curStr = PixelFont.formatIndianCompact(state.savedAmount)
        val tarStr = PixelFont.formatIndianCompact(state.targetAmount)
        PixelFont.drawSubtitle(canvas, "$curStr of $tarStr", left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 2f * density, iconSize)

        // Hero progress %
        val pctInt = (state.percentSaved * 100).toInt()
        val heroText = "$pctInt%"
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress)

        // Bottom Slider
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val sliderBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentSaved,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = PixelTheme.COLOR_MINT_INT,
            labels = Pair("0%", "100%"),
            density = density
        )
    }

    // --- 5. DUES WIDGET ---
    private fun renderDues(
        canvas: Canvas,
        context: Context?,
        state: DuesWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Lend & Borrow", left, top, titleSize)
        val subText = if (state.netAmount >= 0) "Net receivable" else "Net payable"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.DROP, right - iconSize, top + 2f * density, iconSize)

        val rowY1 = top + titleSize + subSize + 16f * density
        val rowY2 = rowY1 + 18f * density
        val rowY3 = rowY2 + 18f * density
        val labelSize = 12f * density

        // Receivable Row (Mint)
        PixelFont.drawLabel(canvas, "GET", left, rowY1, labelSize, PixelTheme.COLOR_MINT_INT)
        val getStr = PixelFont.formatIndianCompact(state.receivableAmount)
        PixelFont.drawRightLabel(canvas, getStr, right, rowY1, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Payable Row (Coral)
        PixelFont.drawLabel(canvas, "OWE", left, rowY2, labelSize, PixelTheme.COLOR_CORAL_INT)
        val oweStr = PixelFont.formatIndianCompact(state.payableAmount)
        PixelFont.drawRightLabel(canvas, oweStr, right, rowY2, labelSize, PixelTheme.COLOR_CORAL_INT, useNdot = true, context = context)

        // Net Row
        val netColor = if (state.netAmount >= 0) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
        PixelFont.drawLabel(canvas, "NET", left, rowY3, labelSize, netColor)
        val netPrefix = if (state.netAmount >= 0) "+ " else "- "
        val netStr = netPrefix + PixelFont.formatIndianCompact(abs(state.netAmount))
        PixelFont.drawRightLabel(canvas, netStr, right, rowY3, labelSize, netColor, useNdot = true, context = context)

        // Bottom Steps bars
        val stepData = floatArrayOf(state.receivableRatio, state.payableRatio)
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val stepBounds = RectF(left, bottom - visualH - 4f * density, right, bottom - 4f * density)
        DotVisuals.drawSteps(canvas, stepBounds, stepData, maxDotsPerBar = 5, activeColor = PixelTheme.COLOR_DOT_ON_INT, density = density)
    }

    // --- 6. SKY WIDGET (Financial Weather Flagship) ---
    private fun renderSky(
        canvas: Canvas,
        context: Context?,
        state: SkyWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)

        // Header
        val statusText = state.climateStatus.name
        val statusColor = when (state.climateStatus) {
            ClimateStatus.CLEAR, ClimateStatus.SUNRISE, ClimateStatus.STARRY -> PixelTheme.COLOR_MINT_INT
            ClimateStatus.CLOUDY -> PixelTheme.COLOR_AMBER_INT
            ClimateStatus.STORM -> PixelTheme.COLOR_CORAL_INT
        }
        PixelFont.drawTitle(canvas, "Kanri · Weather", left, top, titleSize)
        PixelFont.drawSubtitle(canvas, "Pacing: $statusText", left, top + titleSize + 3f * density, subSize, statusColor)

        // Celestial sky well
        val skyHeight = height * 0.44f
        val skyBottom = top + titleSize + subSize + skyHeight + 4f * density
        val skyRect = RectF(left, top + titleSize + subSize + 8f * density, right, skyBottom)

        // Recessed sky well background
        val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF050508.toInt()
            style = Paint.Style.FILL
        }
        val wellBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x18FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val wellRadius = 12f * density
        canvas.drawRoundRect(skyRect, wellRadius, wellRadius, wellPaint)
        canvas.drawRoundRect(skyRect, wellRadius, wellRadius, wellBorderPaint)

        // Celestial Arc
        val centerX = skyRect.centerX()
        val arcRadius = skyRect.width() * 0.42f
        val arcCenterY = skyRect.bottom + 6f * density
        val celestialColor = if (state.isDay) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_CYAN_INT

        DotVisuals.drawArc(
            canvas, centerX, arcCenterY, arcRadius,
            startAngleDeg = 200f, sweepAngleDeg = 140f,
            progress = state.sunProgress * progress,
            onColor = celestialColor,
            density = density
        )

        // If night: draw twinkling stars
        if (!state.isDay) {
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PixelTheme.COLOR_DOT_ON_INT }
            val starCoords = listOf(
                Pair(skyRect.left + skyRect.width() * 0.15f, skyRect.top + 10f * density),
                Pair(skyRect.left + skyRect.width() * 0.35f, skyRect.top + 6f * density),
                Pair(skyRect.left + skyRect.width() * 0.72f, skyRect.top + 12f * density),
                Pair(skyRect.left + skyRect.width() * 0.88f, skyRect.top + 8f * density)
            )
            starCoords.forEach { (sx, sy) ->
                canvas.drawCircle(sx, sy, 1.5f * density, starPaint)
            }
        }

        // Stepped dot terrain silhouette along bottom of sky well
        val hillCols = 16
        val colWidth = skyRect.width() / hillCols
        for (i in 0 until hillCols) {
            val hx = skyRect.left + i * colWidth
            val hillHeight = (4f + 6f * sin((i.toDouble() / hillCols) * Math.PI).toFloat()) * density
            canvas.drawRect(hx, skyRect.bottom - hillHeight, hx + colWidth - 1f, skyRect.bottom, terrainPaint)
        }

        // Ground metrics row
        val groundY = skyBottom + 16f * density
        val labelSize = 11.5f * density
        val todayStr = "SPENT " + PixelFont.formatIndianCompact(state.todaySpent)
        val safeStr = "SAFE " + PixelFont.formatIndianCompact(state.safeToSpend)
        PixelFont.drawLabel(canvas, todayStr, left, groundY, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)
        PixelFont.drawRightLabel(canvas, safeStr, right, groundY, labelSize, PixelTheme.COLOR_CYAN_INT, useNdot = true, context = context)

        // 7-day pulse row
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val pulseBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        DotVisuals.drawSteps(
            canvas, pulseBounds, state.sevenDayPulse,
            maxDotsPerBar = 4,
            activeColor = statusColor,
            labels = Pair("7D AGO", "TODAY"),
            density = density
        )
    }

    // --- 7. MONTH HEATMAP WIDGET ---
    private fun renderHeatmap(
        canvas: Canvas,
        context: Context?,
        state: HeatmapWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)

        val monthLabel = if (state.monthName.isNotEmpty()) state.monthName else "Month Spend"
        PixelFont.drawTitle(canvas, monthLabel, left, top, titleSize)
        val totalStr = PixelFont.formatIndianCompact(state.monthTotal)
        PixelFont.drawSubtitle(canvas, "Total: $totalStr", left, top + titleSize + 3f * density, subSize)
        PixelFont.drawRightLabel(canvas, totalStr, right, top + titleSize * 0.8f, titleSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // Heatmap Calendar Grid (7 columns x 5 rows)
        val gridBounds = RectF(left, top + titleSize + subSize + 10f * density, right, bottom - 4f * density)
        DotVisuals.drawGrid(
            canvas, gridBounds,
            cols = 7, rows = 5,
            intensities = state.dailyIntensities,
            todayIndex = state.todayCellIndex,
            onColor = PixelTheme.COLOR_MINT_INT,
            density = density
        )
    }

    // --- 8. NO-SPEND STREAK WIDGET ---
    private fun renderStreak(
        canvas: Canvas,
        context: Context?,
        state: StreakWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "No-Spend Streak", left, top, titleSize)
        val subText = "Best: ${state.bestStreak} days"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.FLAME, right - iconSize, top + 2f * density, iconSize, PixelTheme.COLOR_AMBER_INT)

        // Hero streak
        val heroText = "${state.currentStreak} DAYS"
        val heroColor = if (state.currentStreak > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress)

        // Bottom 30-day dot row
        val rowY = bottom - 10f * density
        val count = 30
        val spacing = width / count.coerceAtLeast(1)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.2f * density }

        for (i in 0 until count) {
            val cx = left + i * spacing + spacing / 2f
            val isNoSpend = if (i < state.last30Days.size) state.last30Days[i] else false

            if (isNoSpend) {
                fillP.color = PixelTheme.COLOR_AMBER_INT
                canvas.drawCircle(cx, rowY, 2.5f * density, fillP)
            } else {
                strokeP.color = PixelTheme.COLOR_DOT_DIM_INT
                canvas.drawCircle(cx, rowY, 1.8f * density, strokeP)
            }

            if (i == state.todayIndex) {
                strokeP.color = PixelTheme.COLOR_CYAN_INT
                canvas.drawCircle(cx, rowY, 4.5f * density, strokeP)
            }
        }
    }

    // --- 9. SAFE TO SPEND WIDGET ---
    private fun renderSafeToSpend(
        canvas: Canvas,
        context: Context?,
        state: SafeToSpendWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Safe to Spend", left, top, titleSize)
        val caption = "Today spent: " + PixelFont.formatIndianCompact(state.todaySpent)
        PixelFont.drawSubtitle(canvas, caption, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 2f * density, iconSize)

        // Hero allowance
        val heroText = PixelFont.formatIndianCompact(state.dailyAllowance) + " /d"
        val heroColor = if (state.isOverAllowance) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress)

        // Bottom Slider
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val sliderBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.spentRatio,
            mode = DotVisuals.SliderMode.GLOW,
            markerColor = heroColor,
            labels = Pair("₹0", PixelFont.formatIndianCompact(state.dailyAllowance)),
            density = density
        )
    }

    // --- 10. REVIEW INBOX WIDGET ---
    private fun renderReviewInbox(
        canvas: Canvas,
        context: Context?,
        state: ReviewInboxWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Review Inbox", left, top, titleSize)
        val isClear = state.pendingCount == 0
        val subText = if (isClear) "All caught up" else "Total: " + PixelFont.formatIndianCompact(state.pendingTotalAmount)
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.INBOX, right - iconSize, top + 2f * density, iconSize)

        // Hero
        val heroText = if (isClear) "Clear" else "${state.pendingCount} SMS"
        val heroColor = if (isClear) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_AMBER_INT
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress)

        // Bottom Blocks visual
        val visualH = (14f * density).coerceIn(10f, 20f * density)
        val blocksBounds = RectF(left, bottom - visualH - 4f * density, right, bottom - 4f * density)
        DotVisuals.drawBlocks(
            canvas, blocksBounds,
            count = state.pendingCount,
            maxSlots = 12,
            activeColor = PixelTheme.COLOR_AMBER_INT,
            density = density
        )
    }

    // --- 11. MONTH DELTA WIDGET ---
    private fun renderMonthDelta(
        canvas: Canvas,
        context: Context?,
        state: MonthDeltaWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val titleSize = (16f * density).coerceIn(13f, 20f * density)
        val subSize = (11f * density).coerceIn(9f, 13f * density)
        val iconSize = 18f * density

        PixelFont.drawTitle(canvas, "Month Delta", left, top, titleSize)
        val curStr = PixelFont.formatIndianCompact(state.currentMonthSpendToDate)
        val prevStr = PixelFont.formatIndianCompact(state.previousMonthSpendToDate)
        PixelFont.drawSubtitle(canvas, "$curStr vs $prevStr (day ${state.dayOfMonth})", left, top + titleSize + 3f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.PULSE, right - iconSize, top + 2f * density, iconSize)

        // Hero delta
        val sign = if (state.isSpendingMore) "+" else "-"
        val pctText = "$sign${state.deltaPercent.toInt()}%"
        val heroColor = if (state.isSpendingMore) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        val heroSize = (32f * density).coerceIn(20f, height * 0.32f)
        val heroBaseline = top + titleSize + subSize + heroSize * 1.05f + 4f * density
        PixelFont.drawHeroMetric(canvas, context, pctText, left, heroBaseline, heroSize, heroColor, progress)

        // Bottom Comparison Slider / Steps
        val visualH = (16f * density).coerceIn(12f, 22f * density)
        val sliderBounds = RectF(left, bottom - visualH - 12f * density, right, bottom - 12f * density)
        val ratio = (state.currentMonthSpendToDate / (state.previousMonthSpendToDate.coerceAtLeast(1.0))).toFloat()
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = ratio.coerceIn(0f, 1f),
            mode = DotVisuals.SliderMode.GLOW,
            markerColor = heroColor,
            labels = Pair("LAST MO", "THIS MO"),
            density = density
        )
    }

    // --- 12. QUICK ACTIONS WIDGET ---
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

        val btnPadding = 6f * density
        val btnRadius = 14f * density

        val btnBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF14141A.toInt()
            style = Paint.Style.FILL
        }
        val btnBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x22FFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        for (i in 0 until count) {
            val (label, icon, color) = actions[i]
            val bx1 = left + i * colWidth + btnPadding
            val bx2 = left + (i + 1) * colWidth - btnPadding
            val by1 = top + btnPadding
            val by2 = bottom - btnPadding

            val btnRect = RectF(bx1, by1, bx2, by2)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBgPaint)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBorderPaint)

            // Draw Icon
            val iconSize = 22f * density
            val ix = btnRect.centerX() - iconSize / 2f
            val iy = btnRect.centerY() - iconSize * 0.75f
            DotVisuals.drawIcon(canvas, icon, ix, iy, iconSize, color)

            // Draw Label
            val labelSize = 10.5f * density
            PixelFont.drawLabel(
                canvas, label,
                btnRect.centerX() - (label.length * 3f * density),
                btnRect.bottom - 8f * density,
                labelSize, PixelTheme.COLOR_DOT_ON_INT
            )
        }
    }
}
