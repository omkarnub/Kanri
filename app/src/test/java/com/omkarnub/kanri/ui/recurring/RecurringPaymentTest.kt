package com.omkarnub.kanri.ui.recurring

import com.omkarnub.kanri.data.db.RecurringPaymentEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

class RecurringPaymentTest {

    private fun makeCalendar(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testAdvanceDueDateMonthly() {
        val initialCal = makeCalendar(2026, Calendar.SEPTEMBER, 15)
        val advanced = RecurringPaymentCalculator.advanceDueDate(initialCal.timeInMillis, "MONTHLY")

        val resultCal = Calendar.getInstance().apply { timeInMillis = advanced }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, resultCal.get(Calendar.MONTH))
        assertEquals(15, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAdvanceDueDateYearly() {
        val initialCal = makeCalendar(2026, Calendar.SEPTEMBER, 15)
        val advanced = RecurringPaymentCalculator.advanceDueDate(initialCal.timeInMillis, "YEARLY")

        val resultCal = Calendar.getInstance().apply { timeInMillis = advanced }
        assertEquals(2027, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.SEPTEMBER, resultCal.get(Calendar.MONTH))
        assertEquals(15, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAdvanceDueDateWeekly() {
        val initialCal = makeCalendar(2026, Calendar.SEPTEMBER, 15)
        val advanced = RecurringPaymentCalculator.advanceDueDate(initialCal.timeInMillis, "WEEKLY")

        val resultCal = Calendar.getInstance().apply { timeInMillis = advanced }
        assertEquals(22, resultCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCalculateTotalMonthlyCommitted() {
        val list = listOf(
            RecurringPaymentEntity(
                id = 1,
                title = "Netflix",
                amount = 649.0,
                billingCycle = "MONTHLY",
                nextDueTimestamp = 0L
            ),
            RecurringPaymentEntity(
                id = 2,
                title = "Amazon Prime",
                amount = 1499.0,
                billingCycle = "YEARLY",
                nextDueTimestamp = 0L
            ),
            RecurringPaymentEntity(
                id = 3,
                title = "Gym",
                amount = 500.0,
                billingCycle = "WEEKLY",
                nextDueTimestamp = 0L
            ),
            RecurringPaymentEntity(
                id = 4,
                title = "Inactive Sub",
                amount = 1000.0,
                billingCycle = "MONTHLY",
                nextDueTimestamp = 0L,
                isActive = false
            )
        )

        val total = RecurringPaymentCalculator.calculateTotalMonthlyCommitted(list)
        // 649 + (1499 / 12 = 124.916) + (500 * 52/12 = 2166.66) ~ 2940.58
        assertEquals(649.0 + (1499.0 / 12.0) + (500.0 * 52.0 / 12.0), total, 0.1)
    }

    @Test
    fun testAutoDetectCandidatesFromTransactions() {
        val baseTime = System.currentTimeMillis()
        val txs = listOf(
            TransactionEntity(
                id = 1,
                type = "DEBIT",
                amount = 649.0,
                sourceType = "UPI",
                counterparty = "Netflix India",
                bank = "HDFC",
                refNo = "REF1",
                timestamp = baseTime - TimeUnit.DAYS.toMillis(60),
                rawSms = "sms"
            ),
            TransactionEntity(
                id = 2,
                type = "DEBIT",
                amount = 649.0,
                sourceType = "UPI",
                counterparty = "Netflix India",
                bank = "HDFC",
                refNo = "REF2",
                timestamp = baseTime - TimeUnit.DAYS.toMillis(30),
                rawSms = "sms"
            ),
            TransactionEntity(
                id = 3,
                type = "DEBIT",
                amount = 120.0,
                sourceType = "UPI",
                counterparty = "Chai Tapri",
                bank = "SBI",
                refNo = "REF3",
                timestamp = baseTime,
                rawSms = "sms"
            )
        )

        val candidates = RecurringPaymentCalculator.autoDetectCandidates(txs)
        assertEquals(1, candidates.size)
        assertEquals("Netflix India", candidates[0].title)
        assertEquals(649.0, candidates[0].amount, 0.01)
        assertEquals(2, candidates[0].occurrenceCount)
    }
}
