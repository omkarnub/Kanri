package com.omkarnub.kanri.data.lending

import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LendingMoneyRulesTest {

    @Test
    fun testEffectiveOriginalAndOutstanding() {
        val entryWithNoOriginal = LendingEntity(
            id = 1L,
            personName = "Alice",
            amount = 500.0,
            type = "LENT",
            originalAmount = null,
            isSettled = false
        )
        assertEquals(500.0, LendingMoneyEngine.effectiveOriginal(entryWithNoOriginal), 0.001)
        assertEquals(500.0, LendingMoneyEngine.outstanding(entryWithNoOriginal), 0.001)

        val entryWithOriginal = LendingEntity(
            id = 2L,
            personName = "Bob",
            amount = 300.0,
            type = "LENT",
            originalAmount = 1000.0,
            isSettled = false
        )
        assertEquals(1000.0, LendingMoneyEngine.effectiveOriginal(entryWithOriginal), 0.001)
        assertEquals(300.0, LendingMoneyEngine.outstanding(entryWithOriginal), 0.001)

        val settledEntry = LendingEntity(
            id = 3L,
            personName = "Charlie",
            amount = 500.0,
            type = "LENT",
            originalAmount = 500.0,
            isSettled = true
        )
        assertEquals(500.0, LendingMoneyEngine.effectiveOriginal(settledEntry), 0.001)
        assertEquals(0.0, LendingMoneyEngine.outstanding(settledEntry), 0.001)
    }

    @Test
    fun testPartialRepaymentSetsOriginalAmountAndReducesAmount() {
        val entry = LendingEntity(
            id = 10L,
            personName = "Dave",
            amount = 1000.0,
            type = "LENT",
            originalAmount = null,
            isSettled = false
        )

        val (updated, repayment) = LendingMoneyEngine.applyRepayment(
            entity = entry,
            repaymentAmount = 400.0,
            paidAt = 1726500000000L,
            note = "UPI partial"
        )

        assertEquals(1000.0, updated.originalAmount ?: 0.0, 0.001)
        assertEquals(600.0, updated.amount, 0.001)
        assertFalse(updated.isSettled)
        assertEquals(10L, repayment.lendingId)
        assertEquals(400.0, repayment.amount, 0.001)
        assertEquals(1726500000000L, repayment.paidAt)
        assertEquals("UPI partial", repayment.note)
    }

    @Test
    fun testExactRepaymentLeavesAmountUnchangedAndSetsSettled() {
        val entry = LendingEntity(
            id = 20L,
            personName = "Eve",
            amount = 500.0,
            type = "BORROWED",
            originalAmount = null,
            isSettled = false
        )

        val (updated, repayment) = LendingMoneyEngine.applyRepayment(
            entity = entry,
            repaymentAmount = 500.0,
            paidAt = 1726600000000L,
            note = "Full clearance"
        )

        // As per Section 3.2: "If r == amount: insert a repayment row, set is_settled = 1, and leave amount unchanged."
        assertEquals(500.0, updated.originalAmount ?: 0.0, 0.001)
        assertEquals(500.0, updated.amount, 0.001)
        assertTrue(updated.isSettled)
        assertEquals(500.0, repayment.amount, 0.001)
    }

    @Test
    fun testOverRepaymentRejected() {
        val entry = LendingEntity(
            id = 30L,
            personName = "Frank",
            amount = 200.0,
            type = "LENT",
            isSettled = false
        )

        try {
            LendingMoneyEngine.applyRepayment(entry, 250.0)
            fail("Expected IllegalArgumentException for over-repayment")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("cannot exceed") == true)
        }
    }

    @Test
    fun testZeroOrNegativeRepaymentRejected() {
        val entry = LendingEntity(
            id = 31L,
            personName = "Grace",
            amount = 200.0,
            type = "LENT",
            isSettled = false
        )

        try {
            LendingMoneyEngine.applyRepayment(entry, 0.0)
            fail("Expected IllegalArgumentException for zero repayment")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }

        try {
            LendingMoneyEngine.applyRepayment(entry, -50.0)
            fail("Expected IllegalArgumentException for negative repayment")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("greater than zero") == true)
        }
    }

    @Test
    fun testSettleAllEqualsRepayOutstanding() {
        val entry = LendingEntity(
            id = 40L,
            personName = "Heidi",
            amount = 750.0,
            type = "LENT",
            originalAmount = 1000.0,
            isSettled = false
        )

        val (updated, repayment) = LendingMoneyEngine.applySettleAll(entry)
        assertTrue(updated.isSettled)
        assertEquals(750.0, updated.amount, 0.001)
        assertEquals(1000.0, updated.originalAmount ?: 0.0, 0.001)
        assertEquals(750.0, repayment.amount, 0.001)
    }

    @Test
    fun testUndoLastRepaymentFromSettledLeavesAmountUnchangedAndUnsettles() {
        val settledEntry = LendingEntity(
            id = 50L,
            personName = "Ivan",
            amount = 300.0,
            type = "LENT",
            originalAmount = 300.0,
            isSettled = true
        )
        val repayments = listOf(
            LendingRepaymentEntity(id = 1L, lendingId = 50L, amount = 300.0, paidAt = 1000L)
        )

        val (updated, deletedRepayment) = LendingMoneyEngine.applyUndoLastRepayment(settledEntry, repayments)
        assertFalse(updated.isSettled)
        assertEquals(300.0, updated.amount, 0.001)
        assertEquals(1L, deletedRepayment.id)
    }

    @Test
    fun testUndoLastRepaymentFromPartPaidRestoresAmount() {
        val partPaidEntry = LendingEntity(
            id = 60L,
            personName = "Judy",
            amount = 300.0,
            type = "LENT",
            originalAmount = 1000.0,
            isSettled = false
        )
        val r1 = LendingRepaymentEntity(id = 101L, lendingId = 60L, amount = 500.0, paidAt = 1000L)
        val r2 = LendingRepaymentEntity(id = 102L, lendingId = 60L, amount = 200.0, paidAt = 2000L)

        val (updated, deletedRepayment) = LendingMoneyEngine.applyUndoLastRepayment(partPaidEntry, listOf(r1, r2))
        assertFalse(updated.isSettled)
        // 300 + latest repayment (200) = 500
        assertEquals(500.0, updated.amount, 0.001)
        assertEquals(102L, deletedRepayment.id)
    }

    @Test
    fun testReopenLegacySettledEntry() {
        val legacySettled = LendingEntity(
            id = 70L,
            personName = "Kevin",
            amount = 500.0,
            type = "LENT",
            originalAmount = null,
            isSettled = true
        )

        val reopened = LendingMoneyEngine.applyReopenLegacy(legacySettled, emptyList())
        assertFalse(reopened.isSettled)
        assertEquals(500.0, reopened.amount, 0.001)
    }

    @Test
    fun testReopenLegacyFailsIfRepaymentsExist() {
        val settledWithRepayments = LendingEntity(
            id = 71L,
            personName = "Laura",
            amount = 500.0,
            type = "LENT",
            isSettled = true
        )
        val rep = LendingRepaymentEntity(id = 1L, lendingId = 71L, amount = 500.0, paidAt = 1000L)

        try {
            LendingMoneyEngine.applyReopenLegacy(settledWithRepayments, listOf(rep))
            fail("Expected IllegalArgumentException when repayments exist")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("undo repayment instead") == true)
        }
    }

    @Test
    fun testEditingAmountOrTypeBlockedWhenRepaymentsExist() {
        assertTrue(LendingMoneyEngine.canEditAmountOrType(emptyList()))
        assertFalse(
            LendingMoneyEngine.canEditAmountOrType(
                listOf(LendingRepaymentEntity(id = 1L, lendingId = 1L, amount = 50.0, paidAt = 1000L))
            )
        )
    }

    @Test
    fun testFifoAllocationAcrossOpenEntriesByDate() {
        val entryOldest = LendingEntity(
            id = 1L,
            personName = "Mike",
            amount = 300.0,
            type = "LENT",
            date = 1000L,
            isSettled = false
        )
        val entryMiddle = LendingEntity(
            id = 2L,
            personName = "Mike",
            amount = 500.0,
            type = "LENT",
            date = 2000L,
            isSettled = false
        )
        val entryNewest = LendingEntity(
            id = 3L,
            personName = "Mike",
            amount = 400.0,
            type = "LENT",
            date = 3000L,
            isSettled = false
        )

        // Repaying 600 should fully clear entryOldest (300) and partially pay entryMiddle (300 of 500), leaving entryNewest untouched
        val results = LendingMoneyEngine.allocatePersonRepayment(
            openEntries = listOf(entryNewest, entryOldest, entryMiddle), // unsorted input
            repaymentAmount = 600.0,
            paidAt = 5000L
        )

        assertEquals(2, results.size)

        val (firstUpdated, firstRepayment) = results[0]
        assertEquals(1L, firstUpdated.id)
        assertTrue(firstUpdated.isSettled)
        assertEquals(300.0, firstRepayment.amount, 0.001)

        val (secondUpdated, secondRepayment) = results[1]
        assertEquals(2L, secondUpdated.id)
        assertFalse(secondUpdated.isSettled)
        assertEquals(200.0, secondUpdated.amount, 0.001) // 500 - 300 = 200 remaining
        assertEquals(300.0, secondRepayment.amount, 0.001)
    }

    @Test
    fun testFifoAllocationCapsAtTotalOutstanding() {
        val entry = LendingEntity(
            id = 10L,
            personName = "Nina",
            amount = 250.0,
            type = "LENT",
            date = 1000L,
            isSettled = false
        )

        val results = LendingMoneyEngine.allocatePersonRepayment(
            openEntries = listOf(entry),
            repaymentAmount = 1000.0 // higher than total outstanding 250
        )

        assertEquals(1, results.size)
        val (updated, repayment) = results[0]
        assertTrue(updated.isSettled)
        assertEquals(250.0, repayment.amount, 0.001)
    }
}
