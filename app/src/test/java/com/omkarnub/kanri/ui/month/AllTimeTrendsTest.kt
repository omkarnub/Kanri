package com.omkarnub.kanri.ui.month

import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AllTimeTrendsTest {

    private fun makeCalendar(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testEmptyTransactionsReturnsEmptyTrends() {
        val state = AllTimeTrendsCalculator.calculate(emptyList())
        assertTrue(state.monthlyTrends.isEmpty())
        assertEquals(0.0, state.totalAllTimeSpent, 0.001)
        assertEquals(0.0, state.totalAllTimeReceived, 0.001)
        assertEquals(0.0, state.totalAllTimeSavings, 0.001)
    }

    private fun createTx(
        id: Long,
        type: String,
        amount: Double,
        timestamp: Long
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type,
            amount = amount,
            sourceType = "UPI",
            counterparty = "Test",
            displayName = "Test",
            bank = "HDFC",
            refNo = "REF$id",
            timestamp = timestamp,
            categoryId = null,
            rawSms = "Mock SMS"
        )
    }

    @Test
    fun testMultiMonthCalculationAndAggregation() {
        val refCal = makeCalendar(2026, Calendar.SEPTEMBER, 17)

        val txList = listOf(
            // June 2026
            createTx(1, "DEBIT", 2000.0, makeCalendar(2026, Calendar.JUNE, 10).timeInMillis),
            createTx(2, "CREDIT", 10000.0, makeCalendar(2026, Calendar.JUNE, 1).timeInMillis),
            // July 2026
            createTx(3, "DEBIT", 3000.0, makeCalendar(2026, Calendar.JULY, 15).timeInMillis),
            createTx(4, "CREDIT", 12000.0, makeCalendar(2026, Calendar.JULY, 1).timeInMillis),
            // September 2026
            createTx(5, "DEBIT", 2500.0, makeCalendar(2026, Calendar.SEPTEMBER, 17).timeInMillis),
            createTx(6, "CREDIT", 51250.0, makeCalendar(2026, Calendar.SEPTEMBER, 17).timeInMillis)
        )

        val state = AllTimeTrendsCalculator.calculate(
            transactions = txList,
            selectedMetric = TrendMetricType.SPEND,
            referenceCalendar = refCal
        )

        assertEquals(TrendMetricType.SPEND, state.selectedMetric)
        assertEquals(7500.0, state.totalAllTimeSpent, 0.001)
        assertEquals(73250.0, state.totalAllTimeReceived, 0.001)
        assertEquals(65750.0, state.totalAllTimeSavings, 0.001)

        // Find June, July, August, September items
        val june = state.monthlyTrends.find { it.year == 2026 && it.month == Calendar.JUNE }
        val july = state.monthlyTrends.find { it.year == 2026 && it.month == Calendar.JULY }
        val august = state.monthlyTrends.find { it.year == 2026 && it.month == Calendar.AUGUST }
        val sept = state.monthlyTrends.find { it.year == 2026 && it.month == Calendar.SEPTEMBER }

        assertNotNull(june)
        assertNotNull(july)
        assertNotNull(august)
        assertNotNull(sept)

        assertEquals(2000.0, june!!.totalSpent, 0.001)
        assertEquals(10000.0, june.totalReceived, 0.001)
        assertEquals(8000.0, june.netSavings, 0.001)
        assertEquals(80.0f, june.savingsRate, 0.1f)

        assertEquals(3000.0, july!!.totalSpent, 0.001)
        assertEquals(12000.0, july.totalReceived, 0.001)
        assertEquals(9000.0, july.netSavings, 0.001)
        // July MoM spend vs June: (3000 - 2000)/2000 = +50%
        assertNotNull(july.momSpendDeltaPercent)
        assertEquals(50.0, july.momSpendDeltaPercent!!, 0.1)

        // August had 0 transactions
        assertEquals(0.0, august!!.totalSpent, 0.001)
        assertEquals(0.0, august.totalReceived, 0.001)
        assertEquals(0.0, august.netSavings, 0.001)

        // September
        assertEquals(2500.0, sept!!.totalSpent, 0.001)
        assertEquals(51250.0, sept.totalReceived, 0.001)
        assertEquals(48750.0, sept.netSavings, 0.001)
    }

    @Test
    fun testActiveMonthsAverages() {
        val refCal = makeCalendar(2026, Calendar.SEPTEMBER, 1)

        val txList = listOf(
            createTx(1, "DEBIT", 4000.0, makeCalendar(2026, Calendar.AUGUST, 1).timeInMillis),
            createTx(2, "CREDIT", 8000.0, makeCalendar(2026, Calendar.AUGUST, 1).timeInMillis),
            createTx(3, "DEBIT", 6000.0, makeCalendar(2026, Calendar.SEPTEMBER, 1).timeInMillis),
            createTx(4, "CREDIT", 12000.0, makeCalendar(2026, Calendar.SEPTEMBER, 1).timeInMillis)
        )

        val state = AllTimeTrendsCalculator.calculate(txList, referenceCalendar = refCal)

        // 2 active months
        assertEquals(5000.0, state.averageMonthlySpent, 0.001) // (4000 + 6000) / 2
        assertEquals(10000.0, state.averageMonthlyReceived, 0.001) // (8000 + 12000) / 2
        assertEquals(5000.0, state.averageMonthlySavings, 0.001) // (4000 + 6000) / 2
    }
}
