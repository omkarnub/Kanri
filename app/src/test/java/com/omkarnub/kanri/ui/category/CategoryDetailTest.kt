package com.omkarnub.kanri.ui.category

import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CategoryDetailTest {

    private val foodCategory = CategoryEntity(id = 1L, name = "Food & Dining", colorHex = "#EF476F", iconName = "restaurant")
    private val shoppingCategory = CategoryEntity(id = 2L, name = "Shopping", colorHex = "#118AB2", iconName = "shopping_bag")

    private fun createTx(
        id: Long,
        amount: Double,
        type: String = "DEBIT",
        categoryId: Long? = 1L,
        counterparty: String? = "Swiggy",
        timestamp: Long,
        source: String = "UPI"
    ): TransactionWithCategory {
        val cat = when (categoryId) {
            1L -> foodCategory
            2L -> shoppingCategory
            else -> null
        }
        val entity = TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            sourceType = source,
            counterparty = counterparty,
            bank = "Kotak",
            refNo = "REF$id",
            timestamp = timestamp,
            rawSms = "",
            categoryId = categoryId
        )
        return TransactionWithCategory(transaction = entity, category = cat)
    }

    private fun getTimeForMonth(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun testComputeSixMonthTrend_returnsSixChronologicalPoints() {
        val targetYear = 2026
        val targetMonth = 8 // September (0-indexed: 8)

        val txSeptember = createTx(id = 1, amount = 1200.0, categoryId = 1L, timestamp = getTimeForMonth(2026, 8, 15))
        val txAugust = createTx(id = 2, amount = 850.0, categoryId = 1L, timestamp = getTimeForMonth(2026, 7, 20))
        val txJuly = createTx(id = 3, amount = 500.0, categoryId = 1L, timestamp = getTimeForMonth(2026, 6, 10))
        val txOtherCat = createTx(id = 4, amount = 3000.0, categoryId = 2L, timestamp = getTimeForMonth(2026, 8, 10))
        val txCredit = createTx(id = 5, amount = 2000.0, type = "CREDIT", categoryId = 1L, timestamp = getTimeForMonth(2026, 8, 5))

        val trend = CategoryDetailUtils.computeSixMonthTrend(
            transactions = listOf(txSeptember, txAugust, txJuly, txOtherCat, txCredit),
            categoryId = 1L,
            targetYear = targetYear,
            targetMonth = targetMonth
        )

        assertEquals(6, trend.size)

        // Point 5 (last) should be target month (September)
        val sepPoint = trend[5]
        assertEquals(2026, sepPoint.year)
        assertEquals(8, sepPoint.month)
        assertEquals(1200.0, sepPoint.totalSpent, 0.001)
        assertEquals(1, sepPoint.transactionCount)

        // Point 4 should be August
        val augPoint = trend[4]
        assertEquals(2026, augPoint.year)
        assertEquals(7, augPoint.month)
        assertEquals(850.0, augPoint.totalSpent, 0.001)
        assertEquals(1, augPoint.transactionCount)

        // Point 3 should be July
        val julPoint = trend[3]
        assertEquals(2026, julPoint.year)
        assertEquals(6, julPoint.month)
        assertEquals(500.0, julPoint.totalSpent, 0.001)
        assertEquals(1, julPoint.transactionCount)

        // Older months (Apr, May, Jun) should be 0.0
        assertEquals(0.0, trend[0].totalSpent, 0.001)
        assertEquals(0.0, trend[1].totalSpent, 0.001)
        assertEquals(0.0, trend[2].totalSpent, 0.001)
    }

    @Test
    fun testCalculateMoMDeltaPercent() {
        // Increase
        val increase = CategoryDetailUtils.calculateMoMDeltaPercent(1500.0, 1000.0)
        assertNotNull(increase)
        assertEquals(50.0f, increase!!, 0.01f)

        // Decrease
        val decrease = CategoryDetailUtils.calculateMoMDeltaPercent(750.0, 1000.0)
        assertNotNull(decrease)
        assertEquals(-25.0f, decrease!!, 0.01f)

        // Unchanged
        val unchanged = CategoryDetailUtils.calculateMoMDeltaPercent(1000.0, 1000.0)
        assertNotNull(unchanged)
        assertEquals(0.0f, unchanged!!, 0.01f)

        // Previous month zero -> returns null
        val prevZero = CategoryDetailUtils.calculateMoMDeltaPercent(1000.0, 0.0)
        assertNull(prevZero)
    }

    @Test
    fun testFilterTransactionsByScope() {
        val targetYear = 2026
        val targetMonth = 8 // September

        val txSep = createTx(id = 1, amount = 500.0, timestamp = getTimeForMonth(2026, 8, 10))
        val txAug = createTx(id = 2, amount = 400.0, timestamp = getTimeForMonth(2026, 7, 15))
        val txJun = createTx(id = 3, amount = 300.0, timestamp = getTimeForMonth(2026, 5, 20))
        val txJan = createTx(id = 4, amount = 200.0, timestamp = getTimeForMonth(2026, 0, 5))

        val allTxs = listOf(txSep, txAug, txJun, txJan)

        // This Month
        val thisMonth = CategoryDetailUtils.filterTransactionsByScope(
            transactions = allTxs,
            categoryId = 1L,
            scope = CategoryTimeScope.THIS_MONTH,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
        assertEquals(1, thisMonth.size)
        assertEquals(1L, thisMonth[0].transaction.id)

        // Last 3 Months (September, August, July)
        val last3M = CategoryDetailUtils.filterTransactionsByScope(
            transactions = allTxs,
            categoryId = 1L,
            scope = CategoryTimeScope.LAST_3_MONTHS,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
        assertEquals(2, last3M.size) // Sep & Aug

        // Last 6 Months (September down to April)
        val last6M = CategoryDetailUtils.filterTransactionsByScope(
            transactions = allTxs,
            categoryId = 1L,
            scope = CategoryTimeScope.LAST_6_MONTHS,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
        assertEquals(3, last6M.size) // Sep, Aug, Jun

        // All Time
        val allTime = CategoryDetailUtils.filterTransactionsByScope(
            transactions = allTxs,
            categoryId = 1L,
            scope = CategoryTimeScope.ALL_TIME,
            targetYear = targetYear,
            targetMonth = targetMonth
        )
        assertEquals(4, allTime.size)
    }

    @Test
    fun testComputeTopPayees_aggregatesAndRanksCorrectly() {
        val tx1 = createTx(id = 1, amount = 600.0, counterparty = "Swiggy", timestamp = 1000L)
        val tx2 = createTx(id = 2, amount = 400.0, counterparty = "Swiggy", timestamp = 2000L)
        val tx3 = createTx(id = 3, amount = 500.0, counterparty = "Zomato", timestamp = 3000L)
        val tx4 = createTx(id = 4, amount = 200.0, counterparty = "Starbucks", timestamp = 4000L)

        val topPayees = CategoryDetailUtils.computeTopPayees(listOf(tx1, tx2, tx3, tx4))

        assertEquals(3, topPayees.size)

        // #1 should be Swiggy with 1000 total (1000 / 1700 = ~58.8%)
        val p1 = topPayees[0]
        assertEquals("Swiggy", p1.name)
        assertEquals(1000.0, p1.amount, 0.001)
        assertEquals(2, p1.transactionCount)
        assertTrue(p1.percentage > 58f && p1.percentage < 59f)

        // #2 should be Zomato with 500 total (~29.4%)
        val p2 = topPayees[1]
        assertEquals("Zomato", p2.name)
        assertEquals(500.0, p2.amount, 0.001)
        assertEquals(1, p2.transactionCount)

        // #3 should be Starbucks with 200 total (~11.7%)
        val p3 = topPayees[2]
        assertEquals("Starbucks", p3.name)
        assertEquals(200.0, p3.amount, 0.001)
        assertEquals(1, p3.transactionCount)
    }

    @Test
    fun testComputeSixMonthTrend_uncategorizedSupport() {
        val targetYear = 2026
        val targetMonth = 8

        val uncategorizedTx = createTx(
            id = 1,
            amount = 999.0,
            categoryId = null,
            counterparty = "Local Store",
            timestamp = getTimeForMonth(2026, 8, 12)
        )
        val categorizedTx = createTx(
            id = 2,
            amount = 500.0,
            categoryId = 1L,
            counterparty = "Swiggy",
            timestamp = getTimeForMonth(2026, 8, 12)
        )

        val trend = CategoryDetailUtils.computeSixMonthTrend(
            transactions = listOf(uncategorizedTx, categorizedTx),
            categoryId = null,
            targetYear = targetYear,
            targetMonth = targetMonth
        )

        val currentPoint = trend.last()
        assertEquals(999.0, currentPoint.totalSpent, 0.001)
        assertEquals(1, currentPoint.transactionCount)
    }
}
