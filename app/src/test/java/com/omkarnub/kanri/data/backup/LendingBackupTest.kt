package com.omkarnub.kanri.data.backup

import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LendingBackupTest {

    @Test
    fun testBackupRoundTripWithRepaymentsAndOriginalAmount() {
        val lending = listOf(
            LendingEntity(
                id = 1L,
                personName = "Aarav Patel",
                amount = 400.0,
                type = "LENT",
                date = 1726500000000L,
                dueDate = 1727500000000L,
                isSettled = false,
                notes = "Dinner split",
                originalAmount = 1000.0
            )
        )
        val repayments = listOf(
            LendingRepaymentEntity(
                id = 10L,
                lendingId = 1L,
                amount = 600.0,
                paidAt = 1726550000000L,
                note = "GPay partial"
            )
        )

        val payload = BackupPayload(
            version = 2,
            createdAt = 1726560000000L,
            appVersion = "1.0.0",
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            lendingRecords = lending,
            counterpartyMappings = emptyList(),
            lendingRepayments = repayments
        )

        val json = BackupJsonParser.toJson(payload)
        val parsed = BackupJsonParser.fromJson(json)

        assertEquals(1, parsed.lendingRecords.size)
        val parsedLending = parsed.lendingRecords[0]
        assertEquals("Aarav Patel", parsedLending.personName)
        assertEquals(400.0, parsedLending.amount, 0.001)
        assertEquals(1000.0, parsedLending.originalAmount ?: 0.0, 0.001)

        assertEquals(1, parsed.lendingRepayments.size)
        val parsedRepayment = parsed.lendingRepayments[0]
        assertEquals(10L, parsedRepayment.id)
        assertEquals(1L, parsedRepayment.lendingId)
        assertEquals(600.0, parsedRepayment.amount, 0.001)
        assertEquals(1726550000000L, parsedRepayment.paidAt)
        assertEquals("GPay partial", parsedRepayment.note)
    }

    @Test
    fun testLegacyV1BackupRestoresCleanlyWithoutRepayments() {
        val legacyJson = """
            {
              "version": 1,
              "createdAt": 1726500000000,
              "appVersion": "1.0.0",
              "transactions": [],
              "categories": [],
              "budgets": [],
              "lendingRecords": [
                {
                  "id": 5,
                  "personName": "Rohan",
                  "amount": 750.0,
                  "type": "BORROWED",
                  "date": 1726500000000,
                  "dueDate": null,
                  "isSettled": true,
                  "notes": "Cab fare",
                  "linkedTransactionId": null
                }
              ],
              "counterpartyMappings": []
            }
        """.trimIndent()

        val parsed = BackupJsonParser.fromJson(legacyJson)

        assertEquals(1, parsed.lendingRecords.size)
        val record = parsed.lendingRecords[0]
        assertEquals(5L, record.id)
        assertEquals("Rohan", record.personName)
        assertEquals(750.0, record.amount, 0.001)
        assertNull(record.originalAmount)
        assertTrue(record.isSettled)

        // Ensure lendingRepayments list defaults cleanly to empty list
        assertNotNull(parsed.lendingRepayments)
        assertTrue(parsed.lendingRepayments.isEmpty())
    }
}
