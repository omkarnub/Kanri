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

    private val bgPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_BG_INT
            style = Paint.Style.FILL
        }
    }

    private val terrainPaintLocal = ThreadLocal.withInitial {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_DOT_DIM_INT
            style = Paint.Style.FILL
        }
    }

    private val bgPaint: Paint get() = bgPaintLocal.get()!!
    private val terrainPaint: Paint get() = terrainPaintLocal.get()!!

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
        var w = widthPx.coerceIn(120, 1200)
        var h = heightPx.coerceIn(80, 900)
        var effectiveDensity = density
        val maxBytes = 1_500_000L
        val estimatedBytes = w.toLong() * h.toLong() * 4L
        if (estimatedBytes > maxBytes) {
            val scale = kotlin.math.sqrt(maxBytes.toDouble() / estimatedBytes.toDouble()).toFloat()
            w = (w * scale).toInt().coerceAtLeast(120)
            h = (h * scale).toInt().coerceAtLeast(80)
            effectiveDensity = density * scale
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        render(canvas, state, w.toFloat(), h.toFloat(), progress, effectiveDensity, context = context)
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
        val isWide = width > 220f * density
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val heroColor = when (state.pacingStatus) {
            PacingStatus.OVER -> PixelTheme.COLOR_CORAL_INT
            PacingStatus.WARNING -> PixelTheme.COLOR_AMBER_INT
            PacingStatus.HEALTHY -> PixelTheme.COLOR_DOT_ON_INT
        }
        val animSpent = state.spentToday * animP
        val heroText = PixelFont.formatIndianCompact(animSpent)

        if (isWide) {
            // 4x2 Wide layout: Two balanced columns
            val rightLeft = left + width * 0.48f

            // Left Column: Title, Subtitle, Large Hero Metric, Daily Limit pill
            PixelFont.drawTitle(canvas, "Today", left, top, titleSize)
            val captionText = if (state.expenseCount > 0) "${state.expenseCount} expenses" else "No spend today"
            PixelFont.drawSubtitle(canvas, captionText, left, top + titleSize + 2.5f * density, subSize)

            val heroBaseline = top + height * 0.60f
            val heroSize = (26f * density).coerceIn(18f * density, 34f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            val limitY = heroBaseline + 14f * density
            val limitStr = "LIMIT: " + PixelFont.formatIndianCompact(state.dailyLimit)
            PixelFont.drawLabel(canvas, limitStr, left, limitY, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            // Right Column: Pacing bar on top + 4 session checkpoint breakdown cards
            val pacingTop = top + 2f * density
            val pacingH = height - 4f * density
            val pacingBounds = RectF(rightLeft, pacingTop, right, pacingTop + pacingH)
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
        } else {
            // 2x2 Standard layout: Single column vertically balanced
            PixelFont.drawTitle(canvas, "Today", left, top, titleSize)
            val captionText = if (state.expenseCount > 0) "${state.expenseCount} expenses" else "No spend today"
            PixelFont.drawSubtitle(canvas, captionText, left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 1f * density, iconSize)

            val visualH = if (isCompactH) 24f * density else 38f * density
            val visualBottom = bottom - 2f * density
            val visualTop = visualBottom - visualH
            val pacingBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(16f * density, 30f * density)
            val heroBaseline = headerBottom + (availMid * 0.68f)

            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

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
    }

    // --- 2. BUDGET WIDGET (2x1, 2x2, 4x2) ---
    private fun renderBudget(
        canvas: Canvas,
        context: Context?,
        state: BudgetWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

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
        val accentColor = when {
            isOver -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.90f -> PixelTheme.COLOR_CORAL_INT
            state.percentUsed >= 0.70f -> PixelTheme.COLOR_AMBER_INT
            else -> PixelTheme.COLOR_MINT_INT
        }
        val animRemaining = abs(state.remainingAmount) * animP
        val heroText = (if (isOver) "+ " else "") + PixelFont.formatIndianCompact(animRemaining)

        if (isWide) {
            // 4x2 Wide layout: Two balanced columns
            val rightLeft = left + width * 0.48f

            // Left Column
            PixelFont.drawTitle(canvas, "Monthly Budget", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "${state.daysLeft} days left in cycle", left, top + titleSize + 2.5f * density, subSize)

            val heroBaseline = top + height * 0.58f
            val heroSize = (26f * density).coerceIn(18f * density, 34f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, accentColor, progress = 1.0f)

            val spentStr = "SPENT " + PixelFont.formatIndianCompact(state.spentAmount * animP) + " OF " + PixelFont.formatIndianCompact(state.totalBudget)
            PixelFont.drawLabel(canvas, spentStr, left, heroBaseline + 14f * density, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            // Right Column: Dual Progress Bar + Daily safe allowance
            val dualBounds = RectF(rightLeft, top + 2f * density, right, bottom - 2f * density)
            DotVisuals.drawDualProgressBar(
                canvas, dualBounds,
                usedRatio = state.percentUsed,
                totalBudget = state.totalBudget,
                spentAmount = state.spentAmount,
                remainingAmount = state.remainingAmount,
                expectedRatio = state.pacingExpectedRatio,
                usedColor = accentColor,
                density = density,
                progress = progress
            )
        } else if (isCompactH) {
            // 2x1 Compact layout: Side by side metrics
            val midX = left + width * 0.50f
            PixelFont.drawTitle(canvas, titleText, left, top, 12.5f * density, titleColor)
            PixelFont.drawSubtitle(canvas, "${state.daysLeft}d left", left, top + 14f * density, 9f * density)

            val heroBaseline = bottom - 4f * density
            val heroSize = (16f * density).coerceIn(13f * density, 20f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, accentColor, progress = 1.0f)

            // Right half: Compact Progress Bar
            val barTop = top + 10f * density
            val pctInt = (state.percentUsed * animP * 100).toInt()
            PixelFont.drawLabel(canvas, "$pctInt% USED", midX, barTop + 9f * density, 9f * density, accentColor, useNdot = true)
            PixelFont.drawRightLabel(canvas, PixelFont.formatIndianCompact(state.totalBudget), right, barTop + 9f * density, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            val trackRect = RectF(midX, barTop + 14f * density, right, barTop + 19f * density)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            fillPaint.color = PixelTheme.COLOR_DOT_DIM_INT
            canvas.drawRoundRect(trackRect, 2f * density, 2f * density, fillPaint)

            val fillW = (trackRect.width() * state.percentUsed.coerceIn(0f, 1f) * animP).coerceAtLeast(3f * density)
            fillPaint.color = accentColor
            canvas.drawRoundRect(trackRect.left, trackRect.top, trackRect.left + fillW, trackRect.bottom, 2f * density, 2f * density, fillPaint)
        } else {
            // 2x2 Standard layout: Single column vertically balanced
            PixelFont.drawTitle(canvas, titleText, left, top, titleSize, titleColor)
            val subText = "${state.daysLeft}d left in cycle"
            PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 1f * density, iconSize)

            val visualH = (40f * density).coerceIn(32f * density, 48f * density)
            val visualBottom = bottom - 2f * density
            val visualTop = visualBottom - visualH
            val dualBarBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(16f * density, 30f * density)
            val heroBaseline = headerBottom + (availMid * 0.68f)

            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, accentColor, progress = 1.0f)

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
    }

    // --- 3. WALLETS WIDGET (2x2, 4x2) ---
    private fun renderWallets(
        canvas: Canvas,
        context: Context?,
        state: WalletsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val netStr = PixelFont.formatIndianCompact(state.netWorth * animP)
        val cashPct = ((state.cashProportion * animP) * 100).toInt()
        val bankPct = (100 - cashPct).coerceIn(0, 100)
        val cashStr = PixelFont.formatIndianCompact(state.cashBalance * animP)
        val bankStr = PixelFont.formatIndianCompact(state.bankBalance * animP)

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF121216.toInt()
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1AFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val meterBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_DOT_DIM_INT
            style = Paint.Style.FILL
        }
        val meterFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = PixelTheme.COLOR_MINT_INT
            style = Paint.Style.FILL
        }

        if (isWide) {
            // 4x2 Wide layout: Hero Net Worth on left + Two Account Cards on right
            val rightLeft = left + width * 0.44f

            // Left Column
            PixelFont.drawTitle(canvas, "Wallets", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "Net Worth Overview", left, top + titleSize + 2.5f * density, subSize)

            val heroBaseline = top + height * 0.60f
            val heroSize = (26f * density).coerceIn(18f * density, 34f * density)
            PixelFont.drawHeroMetric(canvas, context, netStr, left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)

            val allocStr = "ALLOCATION: $cashPct% CASH · $bankPct% BANK"
            PixelFont.drawLabel(canvas, allocStr, left, heroBaseline + 14f * density, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            // Right Column: Two Tiles (Cash and Bank)
            val cardGap = 8f * density
            val cardH = (height - cardGap) / 2f
            val cardR = 10f * density

            // Cash Tile
            val cashRect = RectF(rightLeft, top, right, top + cardH)
            canvas.drawRoundRect(cashRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(cashRect, cardR, cardR, cardBorderPaint)

            val cTextY = cashRect.top + 14f * density
            PixelFont.drawLabel(canvas, "CASH ACCOUNT ($cashPct%)", cashRect.left + 10f * density, cTextY, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, cashStr, cashRect.right - 10f * density, cTextY, 11f * density, if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

            val cMeterRect = RectF(cashRect.left + 10f * density, cashRect.bottom - 9f * density, cashRect.right - 10f * density, cashRect.bottom - 5f * density)
            canvas.drawRoundRect(cMeterRect, 2f * density, 2f * density, meterBgPaint)
            val cFillW = (cMeterRect.width() * (cashPct / 100f)).coerceAtLeast(2f * density)
            canvas.drawRoundRect(cMeterRect.left, cMeterRect.top, cMeterRect.left + cFillW, cMeterRect.bottom, 2f * density, 2f * density, meterFillPaint)

            // Bank Tile
            val bankRect = RectF(rightLeft, top + cardH + cardGap, right, top + 2f * cardH + cardGap)
            canvas.drawRoundRect(bankRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(bankRect, cardR, cardR, cardBorderPaint)

            val bTextY = bankRect.top + 14f * density
            PixelFont.drawLabel(canvas, "BANK / UPI ($bankPct%)", bankRect.left + 10f * density, bTextY, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, bankStr, bankRect.right - 10f * density, bTextY, 11f * density, if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

            val bMeterRect = RectF(bankRect.left + 10f * density, bankRect.bottom - 9f * density, bankRect.right - 10f * density, bankRect.bottom - 5f * density)
            canvas.drawRoundRect(bMeterRect, 2f * density, 2f * density, meterBgPaint)
            val bFillW = (bMeterRect.width() * (bankPct / 100f)).coerceAtLeast(2f * density)
            canvas.drawRoundRect(bMeterRect.left, bMeterRect.top, bMeterRect.left + bFillW, bMeterRect.bottom, 2f * density, 2f * density, meterFillPaint)
        } else {
            // 2x2 Standard layout
            PixelFont.drawTitle(canvas, "Wallets", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "Net balance & breakdown", left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BOLT, right - iconSize, top + 1f * density, iconSize)

            val headerBottom = top + titleSize + subSize + 4f * density
            val heroLabelY = headerBottom + 10f * density
            PixelFont.drawLabel(canvas, "NET BALANCE", left, heroLabelY, 9f * density, PixelTheme.COLOR_MINT_INT, useNdot = true)

            val heroBaseline = heroLabelY + 22f * density
            val heroSize = (20f * density).coerceIn(16f * density, 24f * density)
            PixelFont.drawHeroMetric(canvas, context, netStr, left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)

            // Two clean cards in the bottom half
            val cardGap = 6f * density
            val cardsTop = heroBaseline + 10f * density
            val cardH = (bottom - cardsTop - cardGap) / 2f
            val cardR = 8f * density

            // Cash Card
            val cashRect = RectF(left, cardsTop, right, cardsTop + cardH)
            canvas.drawRoundRect(cashRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(cashRect, cardR, cardR, cardBorderPaint)
            val cy = cashRect.centerY() + 3.5f * density
            PixelFont.drawLabel(canvas, "CASH ($cashPct%)", cashRect.left + 8f * density, cy, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, cashStr, cashRect.right - 8f * density, cy, 10.5f * density, if (state.cashBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

            // Bank Card
            val bankRect = RectF(left, cardsTop + cardH + cardGap, right, cardsTop + 2 * cardH + cardGap)
            canvas.drawRoundRect(bankRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(bankRect, cardR, cardR, cardBorderPaint)
            val by = bankRect.centerY() + 3.5f * density
            PixelFont.drawLabel(canvas, "BANK ($bankPct%)", bankRect.left + 8f * density, by, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, bankStr, bankRect.right - 8f * density, by, 10.5f * density, if (state.bankBalance < 0) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)
        }
    }

    // --- 4. GOALS WIDGET (2x2, 4x1) ---
    private fun renderGoals(
        canvas: Canvas,
        context: Context?,
        state: GoalsWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        if (!state.hasGoal) {
            PixelFont.drawTitle(canvas, "Savings Goal", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "No active goal", left, top + titleSize + 2.5f * density, subSize)
            val heroSize = (18f * density).coerceIn(14f, height * 0.28f)
            PixelFont.drawHeroMetric(canvas, context, "Add Goal", left, top + titleSize + subSize + heroSize + 4f * density, heroSize, PixelTheme.COLOR_AMBER_INT, progress)
            return
        }

        val title = if (state.goalName.isNotEmpty()) state.goalName else "Savings Goal"
        val pctInt = ((state.percentSaved * animP) * 100).toInt()
        val curStr = PixelFont.formatIndianCompact(state.savedAmount * animP)
        val tarStr = PixelFont.formatIndianCompact(state.targetAmount)
        val remStr = PixelFont.formatIndianCompact((state.targetAmount - state.savedAmount).coerceAtLeast(0.0))

        if (isWide && isCompactH) {
            // 4x1 Wide Compact: Left title & hero %, Right slider & amounts
            val midX = left + width * 0.44f
            PixelFont.drawTitle(canvas, title, left, top + 2f * density, 12.5f * density)
            PixelFont.drawSubtitle(canvas, "$curStr of $tarStr", left, top + 16f * density, 9f * density)

            val heroBaseline = bottom - 4f * density
            val heroSize = (20f * density).coerceIn(16f * density, 24f * density)
            PixelFont.drawHeroMetric(canvas, context, "$pctInt%", left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)

            // Right half: Slider & remaining
            val sliderBounds = RectF(midX, top + 12f * density, right, bottom - 12f * density)
            PixelFont.drawRightLabel(canvas, "$remStr TO GO", right, top + 8f * density, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            DotVisuals.drawSlider(
                canvas, sliderBounds,
                value = state.percentSaved,
                mode = DotVisuals.SliderMode.FILL,
                markerColor = PixelTheme.COLOR_MINT_INT,
                density = density,
                progress = progress
            )
        } else {
            // 2x2 Standard layout
            PixelFont.drawTitle(canvas, title, left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "$curStr of $tarStr", left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.SPARK, right - iconSize, top + 1f * density, iconSize)

            val visualH = (14f * density).coerceIn(10f, 18f * density)
            val visualBottom = bottom - 8f * density
            val visualTop = visualBottom - visualH
            val sliderBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(18f * density, 32f * density)
            val heroBaseline = headerBottom + (availMid * 0.65f)

            PixelFont.drawHeroMetric(canvas, context, "$pctInt%", left, heroBaseline, heroSize, PixelTheme.COLOR_MINT_INT, progress = 1.0f)
            PixelFont.drawRightLabel(canvas, "$remStr TO GO", right, heroBaseline, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            val labels = Pair("0%", "100%")
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
    }

    // --- 5. DUES WIDGET (2x2, 4x2) ---
    private fun renderDues(
        canvas: Canvas,
        context: Context?,
        state: DuesWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val netPrefix = if (state.netAmount >= 0) "Net + " else "Net - "
        val netStr = netPrefix + PixelFont.formatIndianCompact(abs(state.netAmount) * animP)
        val netColor = if (state.netAmount >= 0) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT

        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF121216.toInt()
            style = Paint.Style.FILL
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x1AFFFFFF
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        if (isWide) {
            // 4x2 Wide layout: Left summary + Right Top Contacts list
            val rightLeft = left + width * 0.40f

            // Left Column: Header, Net Hero, Total Get/Owe metrics
            PixelFont.drawTitle(canvas, "Lend & Borrow", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "Net Balance", left, top + titleSize + 2.5f * density, subSize)

            val heroBaseline = top + height * 0.55f
            val heroSize = (22f * density).coerceIn(18f * density, 30f * density)
            PixelFont.drawHeroMetric(canvas, context, netStr, left, heroBaseline, heroSize, netColor, progress = 1.0f)

            val getY = heroBaseline + 14f * density
            val oweY = getY + 12f * density
            val getStr = "TOTAL GET: + " + PixelFont.formatIndianCompact(state.receivableAmount * animP)
            val oweStr = "TOTAL OWE: - " + PixelFont.formatIndianCompact(state.payableAmount * animP)
            PixelFont.drawLabel(canvas, getStr, left, getY, 9f * density, PixelTheme.COLOR_MINT_INT, useNdot = true)
            PixelFont.drawLabel(canvas, oweStr, left, oweY, 9f * density, PixelTheme.COLOR_CORAL_INT, useNdot = true)

            // Right Column: Top Contacts Card List
            PixelFont.drawLabel(canvas, "TOP CONTACTS", rightLeft, top + 10f * density, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            val listTop = top + 16f * density
            if (state.topPersons.isEmpty()) {
                val emptyY = top + height * 0.50f
                PixelFont.drawLabel(canvas, "ALL BALANCES SETTLED", rightLeft, emptyY, 10.5f * density, PixelTheme.COLOR_MINT_INT, useNdot = true)
            } else {
                val maxRows = 4
                val displayList = state.topPersons.take(maxRows)
                val rowH = (bottom - listTop - (displayList.size - 1) * 4f * density) / displayList.size.coerceAtLeast(1)
                for (i in displayList.indices) {
                    val person = displayList[i]
                    val ry1 = listTop + i * (rowH + 4f * density)
                    val ry2 = ry1 + rowH
                    val rowRect = RectF(rightLeft, ry1, right, ry2)
                    canvas.drawRoundRect(rowRect, 6f * density, 6f * density, cardBgPaint)
                    canvas.drawRoundRect(rowRect, 6f * density, 6f * density, cardBorderPaint)

                    val pColor = if (person.isReceivable) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
                    val sign = if (person.isReceivable) "+ " else "- "
                    val pAmtStr = sign + PixelFont.formatIndianCompact(abs(person.netAmount) * animP)
                    val textY = rowRect.centerY() + 3.5f * density

                    PixelFont.drawLabel(canvas, person.name.uppercase(), rowRect.left + 8f * density, textY, 9.5f * density, PixelTheme.COLOR_DOT_ON_INT, useNdot = true)
                    PixelFont.drawRightLabel(canvas, pAmtStr, rowRect.right - 8f * density, textY, 10f * density, pColor, useNdot = true, context = context)
                }
            }
        } else {
            // 2x2 Standard layout: Single column
            PixelFont.drawTitle(canvas, "Lend & Borrow", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, netStr, left, top + titleSize + 2.5f * density, subSize, netColor)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.DROP, right - iconSize, top + 1f * density, iconSize)

            val headerBottom = top + titleSize + subSize + 6f * density
            val labelSize = (10f * density).coerceIn(8.5f, 11f * density)

            if (state.topPersons.isEmpty()) {
                val emptyY = headerBottom + 24f * density
                PixelFont.drawLabel(canvas, "ALL BALANCES SETTLED", left, emptyY, labelSize, PixelTheme.COLOR_MINT_INT, useNdot = true)
            } else {
                val maxRows = 3
                val displayList = state.topPersons.take(maxRows)
                val rowGap = 5f * density
                val rowH = (bottom - headerBottom - (displayList.size - 1) * rowGap) / displayList.size.coerceAtLeast(1)
                for (i in displayList.indices) {
                    val person = displayList[i]
                    val ry1 = headerBottom + i * (rowH + rowGap)
                    val ry2 = ry1 + rowH
                    val rowRect = RectF(left, ry1, right, ry2)
                    canvas.drawRoundRect(rowRect, 6f * density, 6f * density, cardBgPaint)
                    canvas.drawRoundRect(rowRect, 6f * density, 6f * density, cardBorderPaint)

                    val pColor = if (person.isReceivable) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_CORAL_INT
                    val sign = if (person.isReceivable) "+ " else "- "
                    val pAmtStr = sign + PixelFont.formatIndianCompact(abs(person.netAmount) * animP)
                    val textY = rowRect.centerY() + 3.5f * density

                    PixelFont.drawLabel(canvas, person.name.uppercase(), rowRect.left + 8f * density, textY, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true)
                    PixelFont.drawRightLabel(canvas, pAmtStr, rowRect.right - 8f * density, textY, labelSize, pColor, useNdot = true, context = context)
                }
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
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val animP = progress.coerceIn(0f, 1f)

        val statusColor = when (state.climateStatus) {
            ClimateStatus.CLEAR, ClimateStatus.SUNRISE, ClimateStatus.STARRY -> PixelTheme.COLOR_MINT_INT
            ClimateStatus.CLOUDY -> PixelTheme.COLOR_AMBER_INT
            ClimateStatus.STORM -> PixelTheme.COLOR_CORAL_INT
        }
        PixelFont.drawTitle(canvas, "Kanri Sky", left, top, titleSize)
        val statusText = "${state.cityName} · ${state.conditionText}"
        PixelFont.drawSubtitle(canvas, statusText, left, top + titleSize + 2.5f * density, subSize, statusColor)

        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())
        PixelFont.drawRightLabel(canvas, "LIVE $timeStr", right, top + titleSize * 0.8f, subSize, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

        // Financial strip on bottom
        val stepBarH = (12f * density).coerceIn(10f, 16f * density)
        val pulseBounds = RectF(left, bottom - stepBarH - 8f * density, right, bottom - 8f * density)
        DotVisuals.drawSteps(
            canvas, pulseBounds, state.sevenDayPulse,
            maxDotsPerBar = 4,
            activeColor = statusColor,
            labels = Pair("7D AGO", "TODAY"),
            density = density,
            progress = progress
        )

        // Metrics right above 7-day steps
        val groundBaseline = pulseBounds.top - 6f * density
        val labelSize = 9.5f * density
        val animToday = state.todaySpent * animP
        val animSafe = state.safeToSpend * animP
        val todayStr = "SPENT " + PixelFont.formatIndianCompact(animToday)
        val safeStr = "SAFE " + PixelFont.formatIndianCompact(animSafe) + "/D"
        PixelFont.drawLabel(canvas, todayStr, left, groundBaseline, labelSize, PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)
        PixelFont.drawRightLabel(canvas, safeStr, right, groundBaseline, labelSize, PixelTheme.COLOR_CYAN_INT, useNdot = true, context = context)

        // Recessed Sky Well
        val skyTop = top + titleSize + subSize + 6f * density
        val skyBottom = groundBaseline - 6f * density
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
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val animP = progress.coerceIn(0f, 1f)

        val monthLabel = if (state.monthName.isNotEmpty()) "${state.monthName} Spend" else "Month Spend"
        PixelFont.drawTitle(canvas, monthLabel, left, top, titleSize)
        val animTotal = state.monthTotal * animP
        val totalStr = PixelFont.formatIndianCompact(animTotal)
        PixelFont.drawSubtitle(canvas, "Total: $totalStr", left, top + titleSize + 2.5f * density, subSize)
        PixelFont.drawRightLabel(canvas, totalStr, right, top + titleSize * 0.8f, titleSize, PixelTheme.COLOR_MINT_INT, useNdot = true, context = context)

        // GitHub Contribution Heatmap Grid (7 rows Mon..Sun x 5 columns)
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
        val isWide = width > 220f * density
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val animStreak = (state.currentStreak * animP).toInt()
        val heroText = "$animStreak DAYS"
        val heroColor = if (state.currentStreak > 0) PixelTheme.COLOR_AMBER_INT else PixelTheme.COLOR_DOT_MID_INT

        if (isWide && isCompactH) {
            // 4x1 Wide Compact: Left streak hero, Right 10-flame timeline
            val midX = left + width * 0.40f
            PixelFont.drawTitle(canvas, "No-Spend Streak", left, top + 2f * density, 12.5f * density)
            PixelFont.drawSubtitle(canvas, "Best: ${state.bestStreak}d", left, top + 16f * density, 9f * density)

            val heroBaseline = bottom - 4f * density
            val heroSize = (20f * density).coerceIn(16f * density, 24f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            // Right half: 10-flame timeline
            val flameCount = 10
            val flameW = (right - midX) / flameCount
            val flameSize = (11f * density).coerceIn(9f, 14f * density)
            val flameCenterY = top + height * 0.48f

            for (i in 0 until flameCount) {
                val dayOffset = (flameCount - 1) - i
                val historyIdx = 29 - dayOffset
                val isNoSpend = if (historyIdx in state.last30Days.indices) state.last30Days[historyIdx] else false
                val flameCx = midX + i * flameW + flameW / 2f
                val dotP = (animP * flameCount - i).coerceIn(0f, 1f)

                if (dotP > 0f) {
                    val glyph = if (isNoSpend) DotVisuals.PixelGlyph.FLAME_LIT else DotVisuals.PixelGlyph.FLAME_DIM
                    val glyphColor = if (isNoSpend) PixelTheme.COLOR_AMBER_INT else 0x44FFFFFF
                    DotVisuals.drawPixelGlyph(canvas, glyph, flameCx, flameCenterY, flameSize * dotP, glyphColor)
                }
            }
        } else {
            // 2x2 Standard layout: Single column vertically balanced
            PixelFont.drawTitle(canvas, "No-Spend Streak", left, top, titleSize)
            val subText = "Best: ${state.bestStreak} days"
            PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.FLAME, right - iconSize, top + 1f * density, iconSize, PixelTheme.COLOR_AMBER_INT)

            val visualBottom = bottom - 2f * density
            val visualTop = visualBottom - 20f * density

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(16f * density, 32f * density)
            val heroBaseline = headerBottom + (availMid * 0.68f)

            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            // Row of 7 pixelated flame icons for the last 7 days
            val flameCount = 7
            val slotWidth = width / flameCount
            val flameSize = (13f * density).coerceIn(10f, 16f * density)
            val flameCenterY = (visualTop + visualBottom) / 2f

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
    }

    // --- 9. SAFE TO SPEND WIDGET (2x2, 4x1) ---
    private fun renderSafeToSpend(
        canvas: Canvas,
        context: Context?,
        state: SafeToSpendWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val isCompactH = height < 115f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val heroColor = if (state.isOverAllowance) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT
        val animAllowance = state.dailyAllowance * animP
        val heroText = PixelFont.formatIndianCompact(animAllowance) + " /d"
        val animToday = state.todaySpent * animP

        if (isWide && isCompactH) {
            // 4x1 Wide Compact: Left allowance hero, Right today spend & slider
            val midX = left + width * 0.44f
            PixelFont.drawTitle(canvas, "Safe to Spend", left, top + 2f * density, 12.5f * density)
            val caption = "Spent: " + PixelFont.formatIndianCompact(animToday)
            PixelFont.drawSubtitle(canvas, caption, left, top + 16f * density, 9f * density)

            val heroBaseline = bottom - 4f * density
            val heroSize = (18f * density).coerceIn(14f * density, 22f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            // Right half: Slider & percentage
            val pctInt = (state.spentRatio * animP * 100).toInt()
            PixelFont.drawRightLabel(canvas, "$pctInt% USED", right, top + 8f * density, 9f * density, heroColor, useNdot = true)
            val sliderBounds = RectF(midX, top + 14f * density, right, bottom - 10f * density)
            DotVisuals.drawSlider(
                canvas, sliderBounds,
                value = state.spentRatio,
                mode = DotVisuals.SliderMode.GLOW,
                markerColor = heroColor,
                density = density,
                progress = progress
            )
        } else {
            // 2x2 Standard layout
            PixelFont.drawTitle(canvas, "Safe to Spend", left, top, titleSize)
            val caption = "Today: " + PixelFont.formatIndianCompact(animToday)
            PixelFont.drawSubtitle(canvas, caption, left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.BATTERY, right - iconSize, top + 1f * density, iconSize)

            val visualH = (14f * density).coerceIn(10f, 18f * density)
            val visualBottom = bottom - 8f * density
            val visualTop = visualBottom - visualH
            val sliderBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(16f * density, 30f * density)
            val heroBaseline = headerBottom + (availMid * 0.65f)

            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            val remToday = (state.dailyAllowance - state.todaySpent).coerceAtLeast(0.0)
            val remLabel = PixelFont.formatIndianCompact(remToday) + " LEFT TODAY"
            PixelFont.drawRightLabel(canvas, remLabel, right, heroBaseline, 9.5f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            val labels = Pair(
                com.omkarnub.kanri.util.CurrencyUtils.formatCurrency(0.0, includeDecimals = false),
                PixelFont.formatIndianCompact(state.dailyAllowance)
            )
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
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val isClear = state.pendingCount == 0
        val animTotal = state.pendingTotalAmount * animP
        val animCount = (state.pendingCount * animP).toInt()
        val heroText = if (isClear) "Clear" else "$animCount SMS"
        val heroColor = if (isClear) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_AMBER_INT

        if (isCompactH) {
            // 2x1 or 4x1 Compact layout
            val midX = left + width * 0.50f
            PixelFont.drawTitle(canvas, "Review Inbox", left, top + 2f * density, 12.5f * density)
            val subText = if (isClear) "All caught up" else "Total: " + PixelFont.formatIndianCompact(animTotal)
            PixelFont.drawSubtitle(canvas, subText, left, top + 16f * density, 9f * density)

            val heroBaseline = bottom - 4f * density
            val heroSize = (18f * density).coerceIn(14f * density, 22f * density)
            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            // Right half: Blocks
            val blocksBounds = RectF(midX, top + 14f * density, right, bottom - 10f * density)
            DotVisuals.drawBlocks(
                canvas, blocksBounds,
                count = state.pendingCount,
                maxSlots = 10,
                activeColor = PixelTheme.COLOR_AMBER_INT,
                density = density,
                progress = progress
            )
        } else {
            // 2x2 Standard layout
            PixelFont.drawTitle(canvas, "Review Inbox", left, top, titleSize)
            val subText = if (isClear) "All caught up" else "Total: " + PixelFont.formatIndianCompact(animTotal)
            PixelFont.drawSubtitle(canvas, subText, left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.INBOX, right - iconSize, top + 1f * density, iconSize)

            val visualH = (12f * density).coerceIn(8f, 16f * density)
            val visualBottom = bottom - 4f * density
            val visualTop = visualBottom - visualH
            val blocksBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(16f * density, 30f * density)
            val heroBaseline = headerBottom + (availMid * 0.65f)

            PixelFont.drawHeroMetric(canvas, context, heroText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            val actionPrompt = if (isClear) "UP TO DATE" else "TAP TO CATEGORIZE"
            PixelFont.drawRightLabel(canvas, actionPrompt, right, heroBaseline, 9f * density, if (isClear) PixelTheme.COLOR_MINT_INT else PixelTheme.COLOR_DOT_MID_INT, useNdot = true)

            DotVisuals.drawBlocks(
                canvas, blocksBounds,
                count = state.pendingCount,
                maxSlots = 14,
                activeColor = PixelTheme.COLOR_AMBER_INT,
                density = density,
                progress = progress
            )
        }
    }

    // --- 11. MONTH DELTA WIDGET (2x2, 4x2) ---
    private fun renderMonthDelta(
        canvas: Canvas,
        context: Context?,
        state: MonthDeltaWidgetState,
        left: Float, top: Float, right: Float, bottom: Float, width: Float, height: Float,
        progress: Float, density: Float
    ) {
        val isWide = width > 220f * density
        val titleSize = (14.5f * density).coerceIn(12f, 18f * density)
        val subSize = (10f * density).coerceIn(8.5f, 12f * density)
        val iconSize = (16f * density).coerceIn(12f, 20f * density)
        val animP = progress.coerceIn(0f, 1f)

        val sign = if (state.isSpendingMore) "+" else "-"
        val animDelta = (state.deltaPercent * animP).toInt()
        val pctText = "$sign$animDelta%"
        val heroColor = if (state.isSpendingMore) PixelTheme.COLOR_CORAL_INT else PixelTheme.COLOR_MINT_INT

        val animCur = state.currentMonthSpendToDate * animP
        val curStr = PixelFont.formatIndianCompact(animCur)
        val prevStr = PixelFont.formatIndianCompact(state.previousMonthSpendToDate)

        if (isWide) {
            // 4x2 Wide layout: Two balanced columns
            val rightLeft = left + width * 0.44f

            // Left Column
            PixelFont.drawTitle(canvas, "Month Delta", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "vs Day ${state.dayOfMonth} Last Month", left, top + titleSize + 2.5f * density, subSize)

            val heroBaseline = top + height * 0.60f
            val heroSize = (26f * density).coerceIn(18f * density, 34f * density)
            PixelFont.drawHeroMetric(canvas, context, pctText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            val statusStr = if (state.isSpendingMore) "SPENDING HIGHER" else "SAVING ON PACE"
            PixelFont.drawLabel(canvas, statusStr, left, heroBaseline + 14f * density, 9f * density, heroColor, useNdot = true)

            // Right Column: Two Comparison Tiles
            val cardGap = 8f * density
            val cardH = (height - cardGap) / 2f
            val cardR = 10f * density
            val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF121216.toInt(); style = Paint.Style.FILL }
            val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x1AFFFFFF; style = Paint.Style.STROKE; strokeWidth = 1f }

            // Current Month Card
            val curRect = RectF(rightLeft, top, right, top + cardH)
            canvas.drawRoundRect(curRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(curRect, cardR, cardR, cardBorderPaint)
            val cTextY = curRect.centerY() + 3.5f * density
            PixelFont.drawLabel(canvas, "THIS MONTH (TO DATE)", curRect.left + 10f * density, cTextY, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, curStr, curRect.right - 10f * density, cTextY, 11f * density, PixelTheme.COLOR_DOT_ON_INT, useNdot = true, context = context)

            // Previous Month Card
            val prevRect = RectF(rightLeft, top + cardH + cardGap, right, top + 2 * cardH + cardGap)
            canvas.drawRoundRect(prevRect, cardR, cardR, cardBgPaint)
            canvas.drawRoundRect(prevRect, cardR, cardR, cardBorderPaint)
            val pTextY = prevRect.centerY() + 3.5f * density
            PixelFont.drawLabel(canvas, "LAST MONTH (SAME DAY)", prevRect.left + 10f * density, pTextY, 9f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true)
            PixelFont.drawRightLabel(canvas, prevStr, prevRect.right - 10f * density, pTextY, 11f * density, PixelTheme.COLOR_DOT_MID_INT, useNdot = true, context = context)
        } else {
            // 2x2 Standard layout
            PixelFont.drawTitle(canvas, "Month Delta", left, top, titleSize)
            PixelFont.drawSubtitle(canvas, "$curStr vs $prevStr", left, top + titleSize + 2.5f * density, subSize)
            DotVisuals.drawIcon(canvas, DotVisuals.DotIcon.PULSE, right - iconSize, top + 1f * density, iconSize)

            val visualH = (14f * density).coerceIn(10f, 18f * density)
            val visualBottom = bottom - 8f * density
            val visualTop = visualBottom - visualH
            val sliderBounds = RectF(left, visualTop, right, visualBottom)

            val headerBottom = top + titleSize + subSize + 4f * density
            val availMid = visualTop - headerBottom
            val heroSize = (availMid * 0.55f).coerceIn(18f * density, 32f * density)
            val heroBaseline = headerBottom + (availMid * 0.65f)

            PixelFont.drawHeroMetric(canvas, context, pctText, left, heroBaseline, heroSize, heroColor, progress = 1.0f)

            val diff = abs(state.currentMonthSpendToDate - state.previousMonthSpendToDate)
            val diffLabel = (if (state.isSpendingMore) "+ " else "- ") + PixelFont.formatIndianCompact(diff)
            PixelFont.drawRightLabel(canvas, diffLabel, right, heroBaseline, 9.5f * density, heroColor, useNdot = true)

            val labels = Pair("LAST MO", "THIS MO")
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

            val glyphSize = (18f * density * animProgress).coerceIn(12f, 22f * density)
            val gcx = btnRect.centerX()
            val gcy = btnRect.centerY() - 7f * density
            DotVisuals.drawPixelGlyph(canvas, glyph, gcx, gcy, glyphSize, color)

            val labelSize = (9.5f * density).coerceIn(8f, 11f * density)
            PixelFont.drawLabel(
                canvas, label,
                btnRect.centerX() - (label.length * 2.7f * density),
                btnRect.bottom - 7f * density,
                labelSize, PixelTheme.COLOR_DOT_ON_INT,
                useNdot = true
            )
        }
    }
}
