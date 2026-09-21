package com.omkarnub.kanri.data.analytics

import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DeltaCalculatorTest {

    private fun createTx(amount: Double, type: String, year: Int, month: Int, day: Int): TransactionEntity {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return TransactionEntity(
            id = 0,
            type = type,
            amount = amount,
            sourceType = "UPI",
            counterparty = "Merchant",
            bank = "HDFC",
            refNo = "REF${cal.timeInMillis}",
            timestamp = cal.timeInMillis,
            rawSms = "SMS"
        )
    }

    @Test
    fun calculateDelta_currentMonth_comparesToClampedDayInPreviousMonth() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
        }

        val transactions = listOf(
            // September (Current) up to Sep 20: Total = 3,000
            createTx(1000.0, "DEBIT", 2026, 9, 5),
            createTx(2000.0, "DEBIT", 2026, 9, 15),

            // August (Previous):
            // Aug 5: 1000 (within Aug 1-20)
            // Aug 15: 1000 (within Aug 1-20)
            // Aug 28: 5000 (after Aug 20 -> MUST BE EXCLUDED)
            createTx(1000.0, "DEBIT", 2026, 8, 5),
            createTx(1000.0, "DEBIT", 2026, 8, 15),
            createTx(5000.0, "DEBIT", 2026, 8, 28)
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 9,
            transactions = transactions,
            nowCalendar = nowCal
        )

        // Aug 1-20 = 2,000. Sep 1-20 = 3,000. Diff = +1,000 (+50%)
        assertTrue(delta is Delta.Up)
        val up = delta as Delta.Up
        assertEquals(50, up.percent)
        assertEquals(1000.0, up.absolute, 0.01)
        assertEquals("August 1–20", up.prevComparisonDesc)
    }

    @Test
    fun calculateDelta_currentMonth_handlesClampingForShortMonth() {
        // March 31 compared to February (28 days in 2026)
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.MARCH, 31, 12, 0, 0)
        }

        val transactions = listOf(
            createTx(2000.0, "DEBIT", 2026, 3, 10),
            // Feb 28
            createTx(1000.0, "DEBIT", 2026, 2, 28)
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 3,
            transactions = transactions,
            nowCalendar = nowCal
        )

        assertTrue(delta is Delta.Up)
        val up = delta as Delta.Up
        assertEquals(100, up.percent)
        assertEquals("February 1–28", up.prevComparisonDesc)
    }

    @Test
    fun calculateDelta_pastMonth_comparesFullMonthToFullPreviousMonth() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }

        // Selected month is August 2026 (Past month)
        val transactions = listOf(
            // Full August: 1000 + 4000 = 5,000
            createTx(1000.0, "DEBIT", 2026, 8, 5),
            createTx(4000.0, "DEBIT", 2026, 8, 28),

            // Full July: 10,000
            createTx(10000.0, "DEBIT", 2026, 7, 15)
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 8,
            transactions = transactions,
            nowCalendar = nowCal
        )

        // 5000 vs 10000 -> -50% (Down)
        assertTrue(delta is Delta.Down)
        val down = delta as Delta.Down
        assertEquals(50, down.percent)
        assertEquals(5000.0, down.absolute, 0.01)
        assertEquals("July", down.prevComparisonDesc)
    }

    @Test
    fun calculateDelta_zeroPreviousExpenses_returnsNoData() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }

        val transactions = listOf(
            createTx(1000.0, "DEBIT", 2026, 9, 5)
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 9,
            transactions = transactions,
            nowCalendar = nowCal
        )

        assertEquals(Delta.NoData, delta)
    }

    @Test
    fun calculateDelta_lessThanOnePercentDiff_returnsFlat() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }

        val transactions = listOf(
            createTx(1000.0, "DEBIT", 2026, 9, 5),
            createTx(1005.0, "DEBIT", 2026, 8, 5) // 0.5% diff
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 9,
            transactions = transactions,
            nowCalendar = nowCal
        )

        assertTrue(delta is Delta.Flat)
        val flat = delta as Delta.Flat
        assertEquals(5.0, flat.absolute, 0.01)
    }

    @Test
    fun calculateDelta_ignoresIncomeTransactions() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 20, 12, 0, 0)
        }

        val transactions = listOf(
            createTx(1000.0, "DEBIT", 2026, 9, 5),
            createTx(50000.0, "CREDIT", 2026, 9, 10), // Income should be ignored
            createTx(2000.0, "DEBIT", 2026, 8, 5)
        )

        val delta = DeltaCalculator.calculateDelta(
            selectedYear = 2026,
            selectedMonth = 9,
            transactions = transactions,
            nowCalendar = nowCal
        )

        // 1,000 vs 2,000 = -50% (Down)
        assertTrue(delta is Delta.Down)
        val down = delta as Delta.Down
        assertEquals(50, down.percent)
    }
}
