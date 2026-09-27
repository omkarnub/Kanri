package com.omkarnub.kanri.ui.lending

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitExpenseLogicTest {

    @Test
    fun testPerPersonShareCalculations() {
        val totalAmount = 1500.0
        val peopleCount = 3
        val share = totalAmount / peopleCount
        val othersTotal = share * (peopleCount - 1)

        assertEquals(500.0, share, 0.001)
        assertEquals(1000.0, othersTotal, 0.001)
    }

    @Test
    fun testOddAmountShareCalculation() {
        val totalAmount = 1000.0
        val peopleCount = 3
        val share = totalAmount / peopleCount
        val othersTotal = share * (peopleCount - 1)

        assertEquals(333.333, share, 0.01)
        assertEquals(666.666, othersTotal, 0.01)
    }

    @Test
    fun testFriendNamesSynchronization() {
        val friendNames = mutableListOf("Friend 1", "Friend 2")

        fun sync(newCount: Int) {
            val needed = (newCount - 1).coerceAtLeast(1)
            while (friendNames.size < needed) {
                friendNames.add("Friend ${friendNames.size + 1}")
            }
            while (friendNames.size > needed) {
                friendNames.removeAt(friendNames.size - 1)
            }
        }

        // Increase to 5 people -> 4 friends
        sync(5)
        assertEquals(4, friendNames.size)
        assertEquals("Friend 1", friendNames[0])
        assertEquals("Friend 4", friendNames[3])

        // Decrease to 2 people -> 1 friend
        sync(2)
        assertEquals(1, friendNames.size)
        assertEquals("Friend 1", friendNames[0])
    }

    @Test
    fun testLenderVsBorrowerTotals() {
        val totalAmount = 1200.0
        val peopleCount = 4
        val perPersonShare = totalAmount / peopleCount
        val othersTotal = perPersonShare * (peopleCount - 1)

        // Lender pays full bill (1200), collects 3 shares (900)
        assertEquals(900.0, othersTotal, 0.001)
        // Borrower pays nothing upfront, owes 1 share (300)
        assertEquals(300.0, perPersonShare, 0.001)
    }

    @Test
    fun testFriendNameSanitization() {
        val rawNames = listOf("  Rahul ", "", "  ", "Sneha")
        val sanitized = rawNames.mapIndexed { index, name ->
            name.trim().ifBlank { "Friend ${index + 1}" }
        }

        assertEquals("Rahul", sanitized[0])
        assertEquals("Friend 2", sanitized[1])
        assertEquals("Friend 3", sanitized[2])
        assertEquals("Sneha", sanitized[3])
    }

    @Test
    fun testExistingProfileMatching() {
        val existingPeople = listOf("Bob", "Alice", "Rahul")
        val input1 = "bob"
        val input2 = "  Alice  "
        val input3 = "Charlie"

        val match1 = existingPeople.firstOrNull { it.trim().equals(input1.trim(), ignoreCase = true) }
        val match2 = existingPeople.firstOrNull { it.trim().equals(input2.trim(), ignoreCase = true) }
        val match3 = existingPeople.firstOrNull { it.trim().equals(input3.trim(), ignoreCase = true) }

        assertEquals("Bob", match1)
        assertEquals("Alice", match2)
        assertEquals(null, match3) // Charlie is a new person profile
    }
}

