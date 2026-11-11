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

        val tileRect = RectF(0f, 0f, widthPx, heightPx)

        // Solid Obsidian tile background (no white outline)
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, bgPaint)

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
        val visualH = if (isCompactH) 22f * density else 34f * density
        val visualBottom = bottom - 2f * density
        val visualTop = visualBottom - visualH
        val pacingBounds = RectF(left, visualTop, right, visualBottom)

        // Hero spend zone strictly bounded between header and visual
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.55f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.68f)

        val animSpent = state.spentToday * progress.coerceIn(0f, 1f)
        val heroText = PixelFont.formatIndianCompact(animSpent)
        val heroColor = when (state.pacingStatus) {
            PacingStatus.OVER -> PixelTheme.COLOR_CORAL_INT
            PacingStatus.WARNING -> PixelTheme.COLOR_AMBER_INT
            PacingStatus.HEALTHY -> PixelTheme.COLOR_DOT_ON_INT
        }
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Bottom Pacing Indicator comparing Today's spend vs Daily Limit + 4 sessions
        DotVisuals.drawTodayPacing(
            canvas, pacingBounds,
            spentToday = state.spentToday,
            dailyLimit = state.dailyLimit,
            spentPercent = state.spentPercent,
            morningSpend = state.morningSpend,
            afternoonSpend = state.afternoonSpend,
            eveningSpend = state.eveningSpend,
            nightSpend = state.nightSpend,
            activeColor = heroColor,
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

        val subText = "${state.daysLeft}d left in cycle"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 1f * density, iconSize)

        // Visual zone at bottom for Dual Progress Bar
        val visualH = if (isCompactH) 26f * density else 38f * density
        val visualBottom = bottom - 2f * density
        val visualTop = visualBottom - visualH
        val dualBarBounds = RectF(left, visualTop, right, visualBottom)

        // Hero zone strictly between headerBottom and visualTop
        val headerBottom = top + titleSize + subSize + 4f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.55f).coerceIn(15f * density, 30f * density)
        val heroBaseline = headerBottom + (availMid * 0.68f)

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

        // Dual Progress Bars: Top = Used Budget, Bottom = Total Budget Scale
        DotVisuals.drawDualProgressBar(
            canvas, dualBarBounds,
            usedRatio = state.percentUsed,
            totalBudget = state.totalBudget,
            spentAmount = state.spentAmount,
            remainingAmount = state.remainingAmount,
            expectedRatio = state.pacingExpectedRatio,
            usedColor = accentColor,
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
        PixelFont.drawSubtitle(canvas, "Net balance & accounts", left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BOLT, right - iconSize, top + 1f * density, iconSize)

        val headerBottom = top + titleSize + subSize + 6f * density
        val animP = progress.coerceIn(0f, 1f)

        // Hero: NET BALANCE
        val heroLabelY = headerBottom + 10f * density
        PixelFont.drawLabel(canvas, "NET BALANCE", left, heroLabelY, 9.5f * density, PixelTheme.COLOR_MINT_INT, useNdot = true)
        val netStr = PixelFont.formatIndianCompact(state.netWorth * animP)
        val heroSize = (18f * density).coerceIn(14f * density, 24f * density)
        val heroBaseline = heroLabelY + heroSize + 2f * density
        PixelFont.drawHeroMetric(canvas, context, netStr, left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)

        // Account rows
        val rowY1 = heroBaseline + 14f * density
        val rowY2 = rowY1 + 14f * density
        val labelSize = (10.5f * density).coerceIn(9f, 12f * density)

        // Cash row
        val cashPct = ((state.cashProportion * animP) * 100).toInt()
        PixelFont.drawLabel(canvas, "CASH ($cashPct%)", left, rowY1, labelSize, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
        val cashStr = PixelFont.formatIndianCompact(state.cashBalance * animP)
        val cashColor = if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT
        PixelFont.drawRightLabel(canvas, cashStr, right, rowY1, labelSize, cashColor, useNdot = true, context = context)

        // Bank row
        val bankPct = (100 - cashPct).coerceIn(0, 100)
        PixelFont.drawLabel(canvas, "BANK ($bankPct%)", left, rowY2, labelSize, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
        val bankStr = PixelFont.formatIndianCompact(state.bankBalance * animP)
        val bankColor = if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT
        PixelFont.drawRightLabel(canvas, bankStr, right, rowY2, labelSize, bankColor, useNdot = true, context = context)
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
        val animP = progress.coerceIn(0f, 1f)
        val netPrefix = if (state.netAmount >= 0) "Net + " else "Net - "
        val netStr = netPrefix + PixelFont.formatIndianCompact(abs(state.netAmount) * animP)
        val netColor = if (state.netAmount >= 0) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
        PixelFont.drawSubtitle(canvas, netStr, left, top + titleSize + 2.5f * density, subSize, netColor)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.DROP, right - iconSize, top + 1f * density, iconSize)

        val headerBottom = top + titleSize + subSize + 8f * density
        val labelSize = (10.5f * density).coerceIn(9f, 12f * density)
        val rowSpacing = (15f * density).coerceIn(12f, 18f * density)

        if (state.topPersons.isEmpty()) {
            val emptyY = headerBottom + 18f * density
            PixelFont.drawLabel(canvas, "ALL BALANCES SETTLED", left, emptyY, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true)
        } else {
            val maxRows = if (height < 140f * density) 3 else 4
            val displayList = state.topPersons.take(maxRows)
            for (i in displayList.indices) {
                val person = displayList[i]
                val rowY = headerBottom + (i + 1) * rowSpacing
                val pColor = if (person.isReceivable) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
                val sign = if (person.isReceivable) "+ " else "- "
                val pAmtStr = sign + PixelFont.formatIndianCompact(abs(person.netAmount) * animP)

                PixelFont.drawLabel(canvas, person.name.uppercase(), left, rowY, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true)
                PixelFont.drawRightLabel(canvas, pAmtStr, right, rowY, labelSize, pColor, useNdot = true, context = context)
            }
        }
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

        // Header: Location/Condition on left, Live Clock on right
        val statusText = "${state.cityName} · ${state.conditionText}"
        val statusColor = when (state.climateStatus) {
            ClimateStatus.CLEAR, ClimateStatus.SUNRISE, ClimateStatus.STARRY -> PixelTheme.COLOR_MINT_INT
            ClimateStatus.CLOUDY -> PixelTheme.COLOR_AMBER_INT
            ClimateStatus.STORM -> PixelTheme.COLOR_CORAL_INT
        }
        PixelFont.drawTitle(canvas, "Kanri Sky", left, top, titleSize)
        PixelFont.drawSubtitle(canvas, statusText, left, top + titleSize + 2.5f * density, subSize, statusColor)

        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        PixelFont.drawRightLabel(canvas, "LIVE $timeStr", right, top + titleSize * 0.8f, subSize, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

        // Bottom 7-day financial pulse row
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
        val safeStr = "SAFE " + PixelFont.formatIndianCompact(animSafe) + "/D"
        PixelFont.drawLabel(canvas, todayStr, left, groundBaseline, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)
        PixelFont.drawRightLabel(canvas, safeStr, right, groundBaseline, labelSize, PixelTheme.COLOR_CYAN_INT, useNdot = true, context = context)

        // Recessed Celestial Sky Well strictly bounded
        val skyTop = top + titleSize + subSize + 6f * density
        val skyBottom = groundBaseline - 8f * density
        val skyRect = RectF(left, skyTop, right, skyBottom)

        if (skyRect.height() > 16f * density) {
            DotVisuals.drawWeatherScene(
                canvas, skyRect,
                sunProgress = state.sunProgress,
                isDay = state.isDay,
                moonPhase = state.moonPhaseFrac,
                weatherKind = state.weatherKind,
                density = density,
                progress = progress
            )
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

        // GitHub-Style Heatmap Grid (7 rows x 5 columns)
        val gridTop = top + titleSize + subSize + 6f * density
        val gridBounds = RectF(left, gridTop, right, bottom - 2f * density)
        DotVisuals.drawGithubHeatmap(
            canvas, gridBounds,
            intensities = state.dailyIntensities,
            todayIndex = state.todayCellIndex,
            activeColor = PixelTheme.COLOR_MINT_INT,
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
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10.5f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)

        PixelFont.drawTitle(canvas, "No-Spend Streak", left, top, titleSize)
        val subText = "Best: ${state.bestStreak} days"
        PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
        DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.FLAME, right - iconSize, top + 1f * density, iconSize, PixelTheme.COLOR_AMBER_INT)

        // Visual zone for pixelated flame icons
        val visualBottom = bottom - 2f * density
        val visualTop = visualBottom - 18f * density

        // Hero zone: Large Ndot57 numerals + "DAYS NO SPEND"
        val headerBottom = top + titleSize + subSize + 5f * density
        val availMid = visualTop - headerBottom
        val heroSize = (availMid * 0.55f).coerceIn(16f * density, 32f * density)
        val heroBaseline = headerBottom + (availMid * 0.68f)

        val animStreak = (state.currentStreak * progress.coerceIn(0f, 1f)).toInt()
        val heroText = "$animStreak DAYS"
        val heroColor = if (state.currentStreak > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT
        PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

        // Row of 7 pixelated flame icons for recent days
        val flameCount = 7
        val slotWidth = width / flameCount
        val flameSize = (12f * density).coerceIn(10f, 16f * density)
        val flameCenterY = (visualTop + visualBottom) / 2f
        val animP = progress.coerceIn(0f, 1f)

        for (i in 0 until flameCount) {
            val dayOffset = (flameCount - 1) - i
            val historyIdx = 29 - dayOffset
            val isNoSpend = if (historyIdx in state.last30Days.indices) state.last30Days[historyIdx] else false
            val flameCx = left + i * slotWidth + slotWidth / 2f
            val dotP = (animP * flameCount - i).coerceIn(0f, 1f)

            if (dotP > 0f) {
                val glyph = if (isNoSpend) DotVisuals.PixelGlyph.FLAME_LIT else DotVisuals.PixelGlyph.FLAME_DIM
                val glyphColor = if (isNoSpend) PixelTheme.COLOR_AMBER_INT else 0x44FFFFFF
                DotVisuals.drawPixelGlyph(canvas, glyph, flameCx, flameCenterY, flameSize * dotP, glyphColor)
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
            Triple("EXPENSE", DotVisuals.PixelGlyph.EXPENSE, PixelTheme.COLOR_CORAL_INT),
            Triple("INCOME", DotVisuals.PixelGlyph.INCOME, PixelTheme.COLOR_MINT_INT),
            Triple("SPLIT", DotVisuals.PixelGlyph.SPLIT, PixelTheme.COLOR_CYAN_INT)
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
            val (label, glyph, color) = actions[i]
            val bx1 = left + i * colWidth + btnPadding
            val bx2 = left + (i + 1) * colWidth - btnPadding
            val by1 = top + btnPadding
            val by2 = bottom - btnPadding

            val btnRect = RectF(bx1, by1, bx2, by2)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBgPaint)
            canvas.drawRoundRect(btnRect, btnRadius, btnRadius, btnBorderPaint)

            // Draw Pixel Glyph
            val glyphSize = (16f * density * animProgress).coerceIn(10f, 20f * density)
            val gcx = btnRect.centerX()
            val gcy = btnRect.centerY() - 6f * density
            DotVisuals.drawPixelGlyph(canvas, glyph, gcx, gcy, glyphSize, color)

            // Draw Label
            val labelSize = (9.5f * density).coerceIn(8f, 11f * density)
            PixelFont.drawLabel(
                canvas, label,
                btnRect.centerX() - (label.length * 2.7f * density),
                btnRect.bottom - 6f * density,
                labelSize, PixelTheme.COLOR_DOT_ON_INT,
                useNdot = true
            )
        }
    }
}
