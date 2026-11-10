package com.omkarnub.kanri

import android.content.Context
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.omkarnub.kanri.widget.core.BudgetWidgetState
import com.omkarnub.kanri.widget.core.ClimateStatus
import com.omkarnub.kanri.widget.core.DuesWidgetState
import com.omkarnub.kanri.widget.core.GoalsWidgetState
import com.omkarnub.kanri.widget.core.HeatmapWidgetState
import com.omkarnub.kanri.widget.core.MonthDeltaWidgetState
import com.omkarnub.kanri.widget.core.PacingStatus
import com.omkarnub.kanri.widget.core.QuickActionsWidgetState
import com.omkarnub.kanri.widget.core.ReviewInboxWidgetState
import com.omkarnub.kanri.widget.core.SafeToSpendWidgetState
import com.omkarnub.kanri.widget.core.SkyWidgetState
import com.omkarnub.kanri.widget.core.StreakWidgetState
import com.omkarnub.kanri.widget.core.TodayWidgetState
import com.omkarnub.kanri.widget.core.WalletsWidgetState
import com.omkarnub.kanri.widget.render.WidgetRenderer
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ExportWidgetPreviewsTest {

    @Test
    fun exportAllWidgetPreviews() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val outDir = File("/sdcard/Download/widget_previews")
        outDir.mkdirs()

        val density = 2.5f

        fun save(name: String, state: com.omkarnub.kanri.widget.core.WidgetState, widthDp: Float, heightDp: Float) {
            val w = (widthDp * density).toInt()
            val h = (heightDp * density).toInt()
            val bmp = WidgetRenderer.renderToBitmap(
                state = state,
                widthPx = w,
                heightPx = h,
                progress = 1.0f,
                density = density,
                context = appContext
            )
            val file = File(outDir, "$name.png")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }

        // 1. Today (2x2)
        save(
            "widget_preview_today",
            TodayWidgetState(
                spentToday = 1420.0,
                expenseCount = 3,
                pacingStatus = PacingStatus.HEALTHY,
                hourlySpend = List(24) { if (it in 9..14) (it * 100f) else null },
                currentHour = 14
            ),
            180f, 160f
        )

        // 2. Budget (2x1)
        save(
            "widget_preview_budget",
            BudgetWidgetState(
                hasBudget = true,
                remainingAmount = 51100.0,
                totalBudget = 200000.0,
                percentUsed = 0.74f,
                daysLeft = 19,
                pacingExpectedRatio = 0.40f
            ),
            180f, 90f
        )

        // 3. Wallets (2x2)
        save(
            "widget_preview_wallets",
            WalletsWidgetState(
                cashBalance = 1686.0,
                bankBalance = 100360.0,
                netWorth = 102046.0,
                cashProportion = 0.35f
            ),
            180f, 160f
        )

        // 4. Goals (2x2)
        save(
            "widget_preview_goals",
            GoalsWidgetState(
                hasGoal = true,
                goalName = "MacBook Pro",
                savedAmount = 75000.0,
                targetAmount = 150000.0,
                percentSaved = 0.50f
            ),
            180f, 160f
        )

        // 5. Dues (2x2)
        save(
            "widget_preview_dues",
            DuesWidgetState(
                receivableAmount = 69800.0,
                payableAmount = 2100.0,
                netAmount = 67700.0,
                receivableRatio = 0.85f,
                payableRatio = 0.15f
            ),
            180f, 160f
        )

        // 6. Sky (4x2)
        save(
            "widget_preview_sky",
            SkyWidgetState(
                climateStatus = ClimateStatus.STARRY,
                sunProgress = 0.65f,
                isDay = false,
                safeToSpend = 2600.0,
                todaySpent = 850.0,
                sevenDayPulse = floatArrayOf(0.4f, 0.7f, 0.2f, 0.9f, 0.5f, 0.3f, 0.6f)
            ),
            320f, 160f
        )

        // 7. Heatmap (4x3)
        val sampleGrid = FloatArray(35) { idx ->
            if (idx in 7..21) ((idx % 5) * 0.2f + 0.2f) else 0.05f
        }
        save(
            "widget_preview_heatmap",
            HeatmapWidgetState(
                monthName = "November",
                monthTotal = 148850.0,
                dailyIntensities = sampleGrid,
                todayCellIndex = 11
            ),
            320f, 210f
        )

        // 8. Streak (2x2)
        val sampleDays = BooleanArray(30) { it % 3 != 0 }
        save(
            "widget_preview_streak",
            StreakWidgetState(
                currentStreak = 4,
                bestStreak = 9,
                last30Days = sampleDays,
                todayIndex = 10
            ),
            180f, 160f
        )

        // 9. Safe to Spend (2x2)
        save(
            "widget_preview_safe_to_spend",
            SafeToSpendWidgetState(
                dailyAllowance = 2600.0,
                todaySpent = 850.0,
                spentRatio = 0.33f,
                isOverAllowance = false
            ),
            180f, 160f
        )

        // 10. Review Inbox (2x1)
        save(
            "widget_preview_review_inbox",
            ReviewInboxWidgetState(
                pendingCount = 3,
                pendingTotalAmount = 4500.0
            ),
            180f, 90f
        )

        // 11. Month Delta (2x2)
        save(
            "widget_preview_month_delta",
            MonthDeltaWidgetState(
                currentMonthSpendToDate = 18500.0,
                previousMonthSpendToDate = 15200.0,
                deltaPercent = 21f,
                isSpendingMore = true,
                dayOfMonth = 11
            ),
            180f, 160f
        )

        // 12. Quick Actions (4x1)
        save(
            "widget_preview_quick_actions",
            QuickActionsWidgetState(),
            320f, 90f
        )
    }
}

