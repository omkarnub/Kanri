package com.omkarnub.kanri.ui.search

import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SearchFilterTest {

    private val foodCategory = CategoryEntity(id = 1L, name = "Food & Dining", colorHex = "#FF5722", iconName = "Restaurant")
    private val transportCategory = CategoryEntity(id = 2L, name = "Transport", colorHex = "#2196F3", iconName = "DirectionsCar")

    private fun createTx(
        id: Long,
        type: String,
        amount: Double,
        counterparty: String?,
        sourceType: String = "UPI",
        bank: String? = "Kotak",
        refNo: String? = "REF12345",
        displayName: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        category: CategoryEntity? = null
    ): TransactionWithCategory {
        val entity = TransactionEntity(
            id = id,
            type = type,
            amount = amount,
            sourceType = sourceType,
            counterparty = counterparty,
            displayName = displayName,
            bank = bank,
            refNo = refNo,
            timestamp = timestamp,
            categoryId = category?.id,
            rawSms = "raw sms",
            isDuplicate = false,
            isManualEntry = false
        )
        return TransactionWithCategory(transaction = entity, category = category)
    }

    @Test
    fun testMatchesQuery_caseInsensitivePayee() {
        val tx = createTx(id = 1, type = "DEBIT", amount = 250.0, counterparty = "Swiggy Bangalore")
        assertTrue(SearchFilterUtils.matchesQuery(tx, "swiggy"))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "BANGALORE"))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "wig"))
        assertFalse(SearchFilterUtils.matchesQuery(tx, "zomato"))
    }

    @Test
    fun testMatchesQuery_bankAndRefNoAndNotes() {
        val tx = createTx(
            id = 1,
            type = "CREDIT",
            amount = 50000.0,
            counterparty = "Employer Corp",
            bank = "HDFC Bank",
            refNo = "TXN987654",
            displayName = "Monthly Salary Payment"
        )
        assertTrue(SearchFilterUtils.matchesQuery(tx, "hdfc"))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "987654"))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "salary"))
        assertFalse(SearchFilterUtils.matchesQuery(tx, "bonus"))
    }

    @Test
    fun testMatchesQuery_categoryName() {
        val tx = createTx(
            id = 1,
            type = "DEBIT",
            amount = 1450.0,
            counterparty = "Blue Tokai Coffee",
            category = foodCategory
        )
        assertTrue(SearchFilterUtils.matchesQuery(tx, "Food"))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "dining"))
    }

    @Test
    fun testMatchesQuery_blankQueryMatchesEverything() {
        val tx = createTx(id = 1, type = "DEBIT", amount = 100.0, counterparty = "Chai")
        assertTrue(SearchFilterUtils.matchesQuery(tx, ""))
        assertTrue(SearchFilterUtils.matchesQuery(tx, "   "))
    }

    @Test
    fun testTypeFilter_debitAndCredit() {
        val debitTx = createTx(id = 1, type = "DEBIT", amount = 100.0, counterparty = "Shop")
        val creditTx = createTx(id = 2, type = "CREDIT", amount = 500.0, counterparty = "Friend")

        assertTrue(SearchFilterUtils.matchesFilter(debitTx, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), null, null))
        assertTrue(SearchFilterUtils.matchesFilter(debitTx, "", TransactionTypeFilter.DEBIT, null, null, emptySet(), emptySet(), null, null))
        assertFalse(SearchFilterUtils.matchesFilter(debitTx, "", TransactionTypeFilter.CREDIT, null, null, emptySet(), emptySet(), null, null))

        assertTrue(SearchFilterUtils.matchesFilter(creditTx, "", TransactionTypeFilter.CREDIT, null, null, emptySet(), emptySet(), null, null))
        assertFalse(SearchFilterUtils.matchesFilter(creditTx, "", TransactionTypeFilter.DEBIT, null, null, emptySet(), emptySet(), null, null))
    }

    @Test
    fun testAmountBounds() {
        val tx500 = createTx(id = 1, type = "DEBIT", amount = 500.0, counterparty = "Grocery")

        // Min bound
        assertTrue(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), 500.0, null))
        assertFalse(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), 500.01, null))

        // Max bound
        assertTrue(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), null, 500.0))
        assertFalse(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), null, 499.99))

        // Range bound
        assertTrue(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), 100.0, 1000.0))
        assertFalse(SearchFilterUtils.matchesFilter(tx500, "", TransactionTypeFilter.ALL, null, null, emptySet(), emptySet(), 600.0, 1000.0))
    }

    @Test
    fun testCategoryFilter() {
        val foodTx = createTx(id = 1, type = "DEBIT", amount = 300.0, counterparty = "Dinner", category = foodCategory)
        val transportTx = createTx(id = 2, type = "DEBIT", amount = 150.0, counterparty = "Uber", category = transportCategory)
        val uncategorizedTx = createTx(id = 3, type = "DEBIT", amount = 50.0, counterparty = "Unknown", category = null)

        val selectedFoodOnly = setOf(1L)
        assertTrue(SearchFilterUtils.matchesFilter(foodTx, "", TransactionTypeFilter.ALL, null, null, selectedFoodOnly, emptySet(), null, null))
        assertFalse(SearchFilterUtils.matchesFilter(transportTx, "", TransactionTypeFilter.ALL, null, null, selectedFoodOnly, emptySet(), null, null))
        assertFalse(SearchFilterUtils.matchesFilter(uncategorizedTx, "", TransactionTypeFilter.ALL, null, null, selectedFoodOnly, emptySet(), null, null))

        val selectedBoth = setOf(1L, 2L)
        assertTrue(SearchFilterUtils.matchesFilter(foodTx, "", TransactionTypeFilter.ALL, null, null, selectedBoth, emptySet(), null, null))
        assertTrue(SearchFilterUtils.matchesFilter(transportTx, "", TransactionTypeFilter.ALL, null, null, selectedBoth, emptySet(), null, null))
    }

    @Test
    fun testSourceTypeFilter() {
        val upiTx = createTx(id = 1, type = "DEBIT", amount = 200.0, counterparty = "Store", sourceType = "UPI")
        val atmTx = createTx(id = 2, type = "DEBIT", amount = 2000.0, counterparty = "ATM Cash", sourceType = "ATM")

        val selectedUpi = setOf("UPI")
        assertTrue(SearchFilterUtils.matchesFilter(upiTx, "", TransactionTypeFilter.ALL, null, null, emptySet(), selectedUpi, null, null))
        assertFalse(SearchFilterUtils.matchesFilter(atmTx, "", TransactionTypeFilter.ALL, null, null, emptySet(), selectedUpi, null, null))
    }

    @Test
    fun testDateRangeBounds_today() {
        val refCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 17, 15, 30, 0)
        }
        val (start, end) = SearchFilterUtils.getDateRangeBounds(
            preset = DateRangePreset.TODAY,
            referenceMillis = refCalendar.timeInMillis
        )

        assertNotNull(start)
        assertNotNull(end)

        val calStart = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(0, calStart.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calStart.get(Calendar.MINUTE))
        assertEquals(0, calStart.get(Calendar.SECOND))
        assertEquals(17, calStart.get(Calendar.DAY_OF_MONTH))

        val calEnd = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(23, calEnd.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calEnd.get(Calendar.MINUTE))
        assertEquals(59, calEnd.get(Calendar.SECOND))
        assertEquals(17, calEnd.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testDateRangeBounds_allTime() {
        val (start, end) = SearchFilterUtils.getDateRangeBounds(DateRangePreset.ALL_TIME)
        assertNull(start)
        assertNull(end)
    }
}
