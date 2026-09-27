package com.omkarnub.kanri.data.db

import com.omkarnub.kanri.widget.KanriWidgetActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantRulesTest {

    @Test
    fun testMerchantRuleEntityCreation() {
        val rule = CounterpartyCategoryMapEntity(counterparty = "Starbucks", categoryId = 5L)
        assertEquals("Starbucks", rule.counterparty)
        assertEquals(5L, rule.categoryId)
    }

    @Test
    fun testSmartKeywordMatchingLogic() {
        val rules = listOf(
            CounterpartyCategoryMapEntity("Uber", 1L),
            CounterpartyCategoryMapEntity("Uber Eats", 2L),
            CounterpartyCategoryMapEntity("Swiggy", 3L),
            CounterpartyCategoryMapEntity("Starbucks", 4L),
            CounterpartyCategoryMapEntity("Netflix", 5L),
            CounterpartyCategoryMapEntity("Amazon", 6L)
        )

        // Exact match
        fun match(merchant: String): CounterpartyCategoryMapEntity? {
            return rules.filter { merchant.contains(it.counterparty, ignoreCase = true) }
                .maxByOrNull { it.counterparty.length }
        }

        // Test 1: Substring match
        val swiggyMatch = match("SWIGGY-128491 BANGALORE")
        assertNotNull(swiggyMatch)
        assertEquals(3L, swiggyMatch?.categoryId)

        // Test 2: Substring with spaces
        val starbucksMatch = match("Starbucks Coffee India Pvt Ltd")
        assertNotNull(starbucksMatch)
        assertEquals(4L, starbucksMatch?.categoryId)

        // Test 3: Longest matching rule priority (Uber Eats vs Uber)
        val uberEatsMatch = match("UBER EATS ORDER #5521")
        assertNotNull(uberEatsMatch)
        assertEquals("Uber Eats", uberEatsMatch?.counterparty)
        assertEquals(2L, uberEatsMatch?.categoryId)

        val regularUberMatch = match("UBER TRIP #7712")
        assertNotNull(regularUberMatch)
        assertEquals("Uber", regularUberMatch?.counterparty)
        assertEquals(1L, regularUberMatch?.categoryId)

        // Test 4: Case insensitive
        val netflixMatch = match("netflix.com recurring charge")
        assertNotNull(netflixMatch)
        assertEquals(5L, netflixMatch?.categoryId)

        // Test 5: No match
        val unmatched = match("ICICI Bank NetBanking")
        assertNull(unmatched)
    }

    @Test
    fun testQuickSettingsTileActionContract() {
        assertEquals("com.omkarnub.kanri.action.ADD_EXPENSE", KanriWidgetActions.ACTION_ADD_EXPENSE)
        assertTrue(KanriWidgetActions.ACTION_ADD_EXPENSE.startsWith("com.omkarnub.kanri.action."))
    }
}
