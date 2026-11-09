package com.omkarnub.kanri.widget.render

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
import com.omkarnub.kanri.widget.font.DotMatrixFont as Font
import com.omkarnub.kanri.widget.visuals.DotVisuals
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pure Canvas renderer for all 12 Kanri Pixel DotTiles widgets.
 * Output is 100% shared between home screen RemoteViews Bitmaps and Compose Canvas previews.
 */
object WidgetRenderer {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PixelTheme.COLOR_BG_INT
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PixelTheme.COLOR_BORDER_INT
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val terrainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = PixelTheme.COLOR_DOT_DIM_INT
        style = Paint.Style.FILL
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
        jitterY: Float = 0f
    ) {
        if (widthPx <= 0f || heightPx <= 0f) return

        val cornerRadius = 22f * density
        val padding = 14f * density

        // Draw tile background with 1px inner border
        val tileRect = RectF(1f, 1f, widthPx - 1f, heightPx - 1f)
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(tileRect, cornerRadius, cornerRadius, borderPaint)

        // Save canvas for subtle ambient jitter drift if enabled
        canvas.save()
        if (jitterX != 0f || jitterY != 0f) {
            canvas.translate(jitterX, jitterY)
        }

        val contentLeft = padding
        val contentTop = padding
        val contentRight = widthPx - padding
        val contentBottom = heightPx - padding
        val contentWidth = contentRight - contentLeft
        val contentHeight = contentBottom - contentTop

        when (state) {
            is TodayWidgetState -> renderToday(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is BudgetWidgetState -> renderBudget(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is WalletsWidgetState -> renderWallets(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is GoalsWidgetState -> renderGoals(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is DuesWidgetState -> renderDues(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is SkyWidgetState -> renderSky(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is HeatmapWidgetState -> renderHeatmap(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is StreakWidgetState -> renderStreak(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is SafeToSpendWidgetState -> renderSafeToSpend(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is ReviewInboxWidgetState -> renderReviewInbox(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is MonthDeltaWidgetState -> renderMonthDelta(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
            is QuickActionsWidgetState -> renderQuickActions(canvas, state, contentLeft, contentTop, contentRight, contentBottom, contentWidth, contentHeight, progress)
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
        density: Float = 2.5f
    ): Bitmap {
        val w = widthPx.coerceIn(120, 1200)
        val h = heightPx.coerceIn(80, 1000)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        render(canvas, state, w.toFloat(), h.toFloat(), progress, density)
        return bitmap
    }

    // --- 1. TODAY WIDGET ---
    private fun renderToday(
        canvas: Canvas,
        state: TodayWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        // Top label
        Font.draw(canvas, "TODAY", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        // Hero spend
        val heroText = Font.formatIndianCompact(state.spentToday)
        val heroColor = when (state.pacingStatus) {
            PacingStatus.OVER -> PixelTheme.COLOR_CORAL_INT
            PacingStatus.WARNING -> PixelTheme.COLOR_AMBER_INT
            PacingStatus.HEALTHY -> PixelTheme.COLOR_DOT_ON_INT
        }
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val captionText = if (state.expenseCount > 0) "${state.expenseCount} EXPENSES" else "NO SPEND"
        Font.draw(canvas, captionText, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom Dots timeline
        val timelineBounds = RectF(left, bottom - 26f, right, bottom - 10f)
        DotVisuals.drawDots(
            canvas, timelineBounds, state.hourlySpend,
            activeSlotIndex = state.currentHour,
            dotColor = heroColor,
            labels = Pair("00H", "23H")
        )
    }

    // --- 2. BUDGET WIDGET ---
    private fun renderBudget(
        canvas: Canvas,
        state: BudgetWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        if (!state.hasBudget) {
            Font.draw(canvas, "BUDGET", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)
            Font.draw(canvas, "SET BUDGET", left, top + 22f, 10f, color = PixelTheme.COLOR_AMBER_INT, progress = progress)
            Font.draw(canvas, "TAP TO CONFIGURE", left, bottom - 16f, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)
            return
        }

        val isOver = state.remainingAmount < 0
        val labelText = if (isOver) "OVER BUDGET" else "REMAINING"
        val labelColor = if (isOver) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_MID_INT
        Font.draw(canvas, labelText, left, top, PixelTheme.PITCH_LABEL, color = labelColor)

        // Hero value
        val heroText = Font.formatIndianCompact(abs(state.remainingAmount))
        val accentColor = when {
            isOver -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.90f -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.70f -> PixelTheme.COLOR_AMBER_INT
            else -> PixelTheme.COLOR_MINT_INT
        }
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = accentColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val pctInt = (state.percentUsed * 100).toInt()
        val captionText = "$pctInt% USED · ${state.daysLeft}D LEFT"
        Font.draw(canvas, captionText, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom Slider with optional expected pacing marker
        val sliderBounds = RectF(left, bottom - 24f, right, bottom - 8f)
        val isWide = width >= 260f
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentUsed,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = accentColor,
            pacingMarker = if (isWide) state.pacingExpectedRatio else null,
            labels = Pair("0%", "100%")
        )
    }

    // --- 3. WALLETS WIDGET ---
    private fun renderWallets(
        canvas: Canvas,
        state: WalletsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        Font.draw(canvas, "WALLETS", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        val rowY1 = top + 20f
        val rowY2 = rowY1 + 18f
        val rowY3 = rowY2 + 20f

        val pitch = PixelTheme.PITCH_LABEL * 1.1f
        // Cash Row
        Font.draw(canvas, "CASH", left, rowY1, pitch, color = PixelTheme.COLOR_DOT_MID_INT)
        val cashStr = Font.formatIndianCompact(state.cashBalance)
        Font.drawRightAligned(canvas, cashStr, right, rowY1, pitch, color = if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, progress = progress)

        // Bank Row
        Font.draw(canvas, "BANK", left, rowY2, pitch, color = PixelTheme.COLOR_DOT_MID_INT)
        val bankStr = Font.formatIndianCompact(state.bankBalance)
        Font.drawRightAligned(canvas, bankStr, right, rowY2, pitch, color = if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, progress = progress)

        // Net Worth Row
        Font.draw(canvas, "NET", left, rowY3, pitch, color = PixelTheme.COLOR_MINT_INT)
        val netStr = Font.formatIndianCompact(state.netWorth)
        Font.drawRightAligned(canvas, netStr, right, rowY3, pitch, color = PixelTheme.COLOR_MINT_INT, progress = progress)

        // Bottom Rise proportion gauge
        val riseBounds = RectF(left, bottom - 18f, right, bottom - 6f)
        DotVisuals.drawRise(canvas, riseBounds, state.cashProportion, tiers = 6, activeColor = PixelTheme.COLOR_MINT_INT)
    }

    // --- 4. GOALS WIDGET ---
    private fun renderGoals(
        canvas: Canvas,
        state: GoalsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        if (!state.hasGoal) {
            Font.draw(canvas, "SAVINGS GOAL", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)
            Font.draw(canvas, "NO GOAL", left, top + 22f, 10f, color = PixelTheme.COLOR_AMBER_INT, progress = progress)
            Font.draw(canvas, "TAP TO CREATE", left, bottom - 16f, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)
            return
        }

        val nameLabel = if (state.goalName.length > 12) state.goalName.take(11) + "·" else state.goalName.uppercase()
        Font.draw(canvas, nameLabel, left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        // Hero percent or Checkmark if 100%
        val heroText = if (state.isCompleted) "✓ 100%" else "${(state.percentSaved * 100).toInt()}%"
        val heroColor = if (state.isCompleted) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_DOT_ON_INT
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val caption = "${Font.formatIndianCompact(state.savedAmount)} / ${Font.formatIndianCompact(state.targetAmount)}"
        Font.draw(canvas, caption, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom Slider with milestone ticks at 25, 50, 75, 100
        val sliderBounds = RectF(left, bottom - 24f, right, bottom - 8f)
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.percentSaved,
            mode = DotVisuals.SliderMode.FILL,
            markerColor = if (state.isCompleted) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CYAN_INT,
            labels = Pair("0%", "100%")
        )
    }

    // --- 5. DUES WIDGET ---
    private fun renderDues(
        canvas: Canvas,
        state: DuesWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        val label = if (state.overdueCount > 0) "DUES · ${state.overdueCount} OVERDUE" else "LEND & BORROW"
        val labelColor = if (state.overdueCount > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT
        Font.draw(canvas, label, left, top, PixelTheme.PITCH_LABEL, color = labelColor)

        val pitch = PixelTheme.PITCH_LABEL * 1.05f
        val rowY1 = top + 20f
        val rowY2 = rowY1 + 18f
        val rowY3 = rowY2 + 20f

        // GET Row (Mint)
        Font.draw(canvas, "GET", left, rowY1, pitch, color = PixelTheme.COLOR_MINT_INT)
        Font.drawRightAligned(canvas, Font.formatIndianCompact(state.receivableAmount), right, rowY1, pitch, color = PixelTheme.COLOR_MINT_INT, progress = progress)

        // OWE Row (Coral)
        Font.draw(canvas, "OWE", left, rowY2, pitch, color = PixelTheme.COLOR_CORAL_INT)
        Font.drawRightAligned(canvas, Font.formatIndianCompact(state.payableAmount), right, rowY2, pitch, color = PixelTheme.COLOR_CORAL_INT, progress = progress)

        // NET Row
        val netColor = if (state.netAmount >= 0) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
        Font.draw(canvas, "NET", left, rowY3, pitch, color = netColor)
        val netPrefix = if (state.netAmount >= 0) "+ " else "- "
        val netText = netPrefix + Font.formatIndianCompact(abs(state.netAmount))
        Font.drawRightAligned(canvas, netText, right, rowY3, pitch, color = netColor, progress = progress)

        // Bottom Two Steps bars on shared scale
        val stepData = floatArrayOf(state.receivableRatio, state.payableRatio)
        val stepBounds = RectF(left, bottom - 20f, right, bottom - 6f)
        DotVisuals.drawSteps(canvas, stepBounds, stepData, maxDotsPerBar = 5, activeColor = PixelTheme.COLOR_DOT_ON_INT)
    }

    // --- 6. SKY WIDGET (4x2 Flagship) ---
    private fun renderSky(
        canvas: Canvas,
        state: SkyWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        val skyHeight = height * 0.52f
        val skyBottom = top + skyHeight
        val centerX = left + width / 2f

        // Draw Celestial Arc
        val arcRadius = width * 0.42f
        val arcCenterY = skyBottom + 12f
        val celestialColor = if (state.isDay) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_CYAN_INT

        DotVisuals.drawArc(
            canvas, centerX, arcCenterY, arcRadius,
            startAngleDeg = 200f, sweepAngleDeg = 140f,
            progress = state.sunProgress * progress,
            onColor = celestialColor
        )

        // If night: draw twinkling stars
        if (!state.isDay) {
            val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PixelTheme.COLOR_DOT_ON_INT }
            val starCoords = listOf(
                Pair(left + width * 0.15f, top + 15f),
                Pair(left + width * 0.35f, top + 8f),
                Pair(left + width * 0.72f, top + 18f),
                Pair(left + width * 0.88f, top + 10f)
            )
            starCoords.forEach { (sx, sy) ->
                canvas.drawCircle(sx, sy, 1.8f, starPaint)
            }
        }

        // Stepped dot terrain silhouette along bottom of sky
        val hillCols = 16
        val colWidth = width / hillCols
        for (i in 0 until hillCols) {
            val hx = left + i * colWidth
            val hillHeight = 6f + 8f * sin((i.toDouble() / hillCols) * Math.PI).toFloat()
            canvas.drawRect(hx, skyBottom - hillHeight, hx + colWidth - 1f, skyBottom, terrainPaint)
        }

        // Climate status badge
        val statusText = state.climateStatus.name
        val statusColor = when (state.climateStatus) {
            ClimateStatus.CLEAR, ClimateStatus.SUNRISE, ClimateStatus.STARRY -> PixelTheme.COLOR_MINT_INT
            ClimateStatus.CLOUDY -> PixelTheme.COLOR_AMBER_INT
            ClimateStatus.STORM -> PixelTheme.COLOR_CORAL_INT
        }
        Font.draw(canvas, "WEATHER · $statusText", left, top, PixelTheme.PITCH_LABEL * 0.9f, color = statusColor)

        // Ground metrics row
        val groundY = skyBottom + 10f
        val todayStr = "SPENT ${Font.formatIndianCompact(state.todaySpent)}"
        val safeStr = "SAFE ${Font.formatIndianCompact(state.safeToSpend)}"
        Font.draw(canvas, todayStr, left, groundY, PixelTheme.PITCH_LABEL * 0.9f, color = PixelTheme.COLOR_DOT_ON_INT, progress = progress)
        Font.drawRightAligned(canvas, safeStr, right, groundY, PixelTheme.PITCH_LABEL * 0.9f, color = PixelTheme.COLOR_CYAN_INT, progress = progress)

        // 7-day pulse row
        val pulseBounds = RectF(left, bottom - 18f, right, bottom - 4f)
        DotVisuals.drawSteps(
            canvas, pulseBounds, state.sevenDayPulse,
            maxDotsPerBar = 4,
            activeColor = statusColor,
            labels = Pair("7D AGO", "TODAY")
        )
    }

    // --- 7. MONTH HEATMAP WIDGET ---
    private fun renderHeatmap(
        canvas: Canvas,
        state: HeatmapWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        val label = if (state.monthName.isNotEmpty()) state.monthName.uppercase() else "CALENDAR"
        Font.draw(canvas, label, left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        val totalStr = Font.formatIndianCompact(state.monthTotal)
        Font.drawRightAligned(canvas, totalStr, right, top, PixelTheme.PITCH_LABEL * 1.1f, color = PixelTheme.COLOR_DOT_ON_INT, progress = progress)

        // Heatmap Calendar Grid (7 columns x 5 rows)
        val gridBounds = RectF(left, top + 22f, right, bottom - 6f)
        DotVisuals.drawGrid(
            canvas, gridBounds,
            cols = 7, rows = 5,
            intensities = state.dailyIntensities,
            todayIndex = state.todayCellIndex,
            onColor = PixelTheme.COLOR_MINT_INT
        )
    }

    // --- 8. NO-SPEND STREAK WIDGET ---
    private fun renderStreak(
        canvas: Canvas,
        state: StreakWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        Font.draw(canvas, "NO SPEND", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        // Hero current streak
        val heroText = "${state.currentStreak} DAYS"
        val heroColor = if (state.currentStreak > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val caption = if (state.currentStreak == 0) "START TODAY · BEST ${state.bestStreak}" else "BEST ${state.bestStreak} DAYS"
        Font.draw(canvas, caption, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom 30-day dot row
        val rowY = bottom - 14f
        val count = 30
        val spacing = width / count.coerceAtLeast(1)
        val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.5f }

        for (i in 0 until count) {
            val cx = left + i * spacing + spacing / 2f
            val isNoSpend = if (i < state.last30Days.size) state.last30Days[i] else false

            if (isNoSpend) {
                fillP.color = PixelTheme.COLOR_AMBER_INT
                canvas.drawCircle(cx, rowY, 2.8f, fillP)
            } else {
                strokeP.color = PixelTheme.COLOR_DOT_DIM_INT
                canvas.drawCircle(cx, rowY, 2.2f, strokeP)
            }

            if (i == state.todayIndex) {
                strokeP.color = PixelTheme.COLOR_CYAN_INT
                canvas.drawCircle(cx, rowY, 5f, strokeP)
            }
        }
    }

    // --- 9. SAFE TO SPEND WIDGET ---
    private fun renderSafeToSpend(
        canvas: Canvas,
        state: SafeToSpendWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        val label = if (state.hasBudget) "SAFE / DAY" else "AVG / DAY"
        Font.draw(canvas, label, left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        // Hero allowance
        val heroText = Font.formatIndianCompact(state.dailyAllowance)
        val heroColor = if (state.isOverAllowance) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val caption = "TODAY ${Font.formatIndianCompact(state.todaySpent)}"
        Font.draw(canvas, caption, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom Slider
        val sliderBounds = RectF(left, bottom - 24f, right, bottom - 8f)
        DotVisuals.drawSlider(
            canvas, sliderBounds,
            value = state.spentRatio,
            mode = DotVisuals.SliderMode.GLOW,
            markerColor = heroColor,
            labels = Pair("₹0", Font.formatIndianCompact(state.dailyAllowance))
        )
    }

    // --- 10. REVIEW INBOX WIDGET ---
    private fun renderReviewInbox(
        canvas: Canvas,
        state: ReviewInboxWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        Font.draw(canvas, "TO REVIEW", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        val isClear = state.pendingCount == 0
        val heroText = if (isClear) "✓ CLEAR" else "${state.pendingCount} PENDING"
        val heroColor = if (isClear) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_AMBER_INT
        val heroPitch = Font.findFittingPitch(heroText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, heroText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val caption = if (isClear) "ALL CAUGHT UP" else "TOTAL ${Font.formatIndianCompact(state.pendingTotalAmount)}"
        Font.draw(canvas, caption, left, captionY, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom Blocks visual
        val blocksBounds = RectF(left, bottom - 20f, right, bottom - 8f)
        DotVisuals.drawBlocks(
            canvas, blocksBounds,
            count = state.pendingCount,
            maxSlots = 12,
            activeColor = PixelTheme.COLOR_AMBER_INT
        )
    }

    // --- 11. MONTH DELTA WIDGET ---
    private fun renderMonthDelta(
        canvas: Canvas,
        state: MonthDeltaWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        Font.draw(canvas, "VS LAST MONTH", left, top, PixelTheme.PITCH_LABEL, color = PixelTheme.COLOR_DOT_MID_INT)

        // Hero delta
        val arrow = if (state.isSpendingMore) "▲ " else "▼ "
        val pctText = "$arrow${state.deltaPercent.toInt()}%"
        val heroColor = if (state.isSpendingMore) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        val heroPitch = Font.findFittingPitch(pctText, width, PixelTheme.PITCH_HERO_LARGE, 8f)
        val heroY = top + 18f
        Font.draw(canvas, pctText, left, heroY, heroPitch, color = heroColor, progress = progress)

        // Caption
        val captionY = heroY + (7 * heroPitch) + 8f
        val curStr = Font.formatIndianCompact(state.currentMonthSpendToDate)
        val prevStr = Font.formatIndianCompact(state.previousMonthSpendToDate)
        val caption = "$curStr VS $prevStr AT DAY ${state.dayOfMonth}"
        Font.draw(canvas, caption, left, captionY, PixelTheme.PITCH_LABEL * 0.82f, color = PixelTheme.COLOR_DOT_MID_INT)

        // Bottom two Steps bars
        val maxVal = maxOf(state.currentMonthSpendToDate, state.previousMonthSpendToDate, 1.0)
        val barData = floatArrayOf(
            (state.previousMonthSpendToDate / maxVal).toFloat(),
            (state.currentMonthSpendToDate / maxVal).toFloat()
        )
        val stepBounds = RectF(left, bottom - 22f, right, bottom - 6f)
        DotVisuals.drawSteps(
            canvas, stepBounds, barData,
            maxDotsPerBar = 5,
            activeColor = heroColor,
            labels = Pair("LAST MO", "THIS MO")
        )
    }

    // --- 12. QUICK ACTIONS WIDGET ---
    private fun renderQuickActions(
        canvas: Canvas,
        state: QuickActionsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float
    ) {
        val count = state.actions.size.coerceAtLeast(1)
        val slotWidth = width / count

        for (i in 0 until count) {
            val ax = left + i * slotWidth
            val action = state.actions[i]
            val cx = ax + slotWidth / 2f
            val cy = top + height * 0.35f

            // Draw Icon
            val iconType = when (action) {
                com.omkarnub.kanri.widget.core.QuickActionType.ADD_EXPENSE -> DotVisuals.DotIcon.PLUS
                com.omkarnub.kanri.widget.core.QuickActionType.ADD_INCOME -> DotVisuals.DotIcon.ARROW_UP
                com.omkarnub.kanri.widget.core.QuickActionType.SPLIT_BILL -> DotVisuals.DotIcon.SPLIT
            }
            DotVisuals.drawIcon(canvas, iconType, cx - 14f, cy - 14f, dotPitch = 4f, color = PixelTheme.COLOR_DOT_ON_INT)

            // Draw action label below icon
            val labelText = when (action) {
                com.omkarnub.kanri.widget.core.QuickActionType.ADD_EXPENSE -> "+EXPENSE"
                com.omkarnub.kanri.widget.core.QuickActionType.ADD_INCOME -> "+INCOME"
                com.omkarnub.kanri.widget.core.QuickActionType.SPLIT_BILL -> "SPLIT"
            }
            Font.drawCentered(canvas, labelText, ax, ax + slotWidth, cy + 22f, PixelTheme.PITCH_LABEL * 0.85f, color = PixelTheme.COLOR_DOT_MID_INT, progress = progress)
        }
    }
}
