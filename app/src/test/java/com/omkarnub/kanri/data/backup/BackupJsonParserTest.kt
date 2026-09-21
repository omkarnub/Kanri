package com.omkarnub.kanri.data.backup

import com.omkarnub.kanri.data.db.BudgetEntity
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.CounterpartyCategoryMapEntity
import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupJsonParserTest {

    @Test
    fun testSerializationAndDeserializationIntegrity() {
        val transactions = listOf(
            TransactionEntity(
                id = 1L,
                type = "DEBIT",
                amount = 450.0,
                sourceType = "UPI",
                counterparty = "swiggy@kotak",
                displayName = "Swiggy",
                bank = "Kotak",
                refNo = "REF123456",
                timestamp = 1726500000000L,
                categoryId = 2L,
                rawSms = "Rs 450 debited at swiggy",
                isDuplicate = false,
                isManualEntry = false
            ),
            TransactionEntity(
                id = 2L,
                type = "CREDIT",
                amount = 2500.0,
                sourceType = "UPI",
                counterparty = "boss@upi",
                displayName = null,
                bank = "HDFC",
                refNo = null,
                timestamp = 1726550000000L,
                categoryId = null,
                rawSms = "Rs 2500 credited",
                isDuplicate = false,
                isManualEntry = true
            )
        )

        val categories = listOf(
            CategoryEntity(id = 1L, name = "Food & Dining", colorHex = "#FF7043", iconName = "restaurant"),
            CategoryEntity(id = 2L, name = "Groceries", colorHex = "#4CAF50", iconName = "shopping_cart")
        )

        val budgets = listOf(
            BudgetEntity(monthKey = "2026-09", monthlyLimit = 25000.0)
        )

        val lendingRecords = listOf(
            LendingEntity(
                id = 10L,
                personName = "Rahul Sharma",
                amount = 1200.0,
                type = "LENT",
                date = 1726500000000L,
                dueDate = 1727500000000L,
                isSettled = false,
                notes = "Lunch share",
                linkedTransactionId = null
            ),
            LendingEntity(
                id = 11L,
                personName = "Amit Verma",
                amount = 500.0,
                type = "BORROWED",
                date = 1726510000000L,
                dueDate = null,
                isSettled = true,
                notes = null,
                linkedTransactionId = null
            )
        )

        val mappings = listOf(
            CounterpartyCategoryMapEntity(counterparty = "swiggy@kotak", categoryId = 1L)
        )

        val originalPayload = BackupPayload(
            version = 1,
            createdAt = 1726560000000L,
            appVersion = "1.0.0",
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            lendingRecords = lendingRecords,
            counterpartyMappings = mappings
        )

        // Serialize to JSON
        val json = BackupJsonParser.toJson(originalPayload)
        assertNotNull(json)
        assertTrue(json.contains("REF123456"))
        assertTrue(json.contains("Rahul Sharma"))
        assertTrue(json.contains("2026-09"))

        // Deserialize back from JSON
        val restored = BackupJsonParser.fromJson(json)

        // Verify metadata
        assertEquals(1, restored.version)
        assertEquals(1726560000000L, restored.createdAt)
        assertEquals("1.0.0", restored.appVersion)

        // Verify transactions
        assertEquals(2, restored.transactions.size)
        val t1 = restored.transactions[0]
        assertEquals("DEBIT", t1.type)
        assertEquals(450.0, t1.amount, 0.001)
        assertEquals("swiggy@kotak", t1.counterparty)
        assertEquals("REF123456", t1.refNo)
        assertEquals(2L, t1.categoryId)

        val t2 = restored.transactions[1]
        assertEquals("CREDIT", t2.type)
        assertEquals(2500.0, t2.amount, 0.001)
        assertNull(t2.refNo)
        assertNull(t2.categoryId)
        assertTrue(t2.isManualEntry)

        // Verify categories
        assertEquals(2, restored.categories.size)
        assertEquals("Food & Dining", restored.categories[0].name)
        assertEquals("#4CAF50", restored.categories[1].colorHex)

        // Verify budgets
        assertEquals(1, restored.budgets.size)
        assertEquals("2026-09", restored.budgets[0].monthKey)
        assertEquals(25000.0, restored.budgets[0].monthlyLimit, 0.001)

        // Verify lending records
        assertEquals(2, restored.lendingRecords.size)
        val l1 = restored.lendingRecords[0]
        assertEquals("Rahul Sharma", l1.personName)
        assertEquals(1200.0, l1.amount, 0.001)
        assertEquals("LENT", l1.type)
        assertEquals(false, l1.isSettled)
        assertEquals("Lunch share", l1.notes)

        val l2 = restored.lendingRecords[1]
        assertEquals("Amit Verma", l2.personName)
        assertEquals(500.0, l2.amount, 0.001)
        assertEquals("BORROWED", l2.type)
        assertEquals(true, l2.isSettled)

        // Verify counterparty mappings
        assertEquals(1, restored.counterpartyMappings.size)
        assertEquals("swiggy@kotak", restored.counterpartyMappings[0].counterparty)
        assertEquals(1L, restored.counterpartyMappings[0].categoryId)
    }

    @Test
    fun testEmptyPayloadIntegrity() {
        val emptyPayload = BackupPayload(
            transactions = emptyList(),
            categories = emptyList(),
            budgets = emptyList(),
            lendingRecords = emptyList(),
            counterpartyMappings = emptyList()
        )

        val json = BackupJsonParser.toJson(emptyPayload)
        val restored = BackupJsonParser.fromJson(json)

        assertEquals(0, restored.transactions.size)
        assertEquals(0, restored.categories.size)
        assertEquals(0, restored.budgets.size)
        assertEquals(0, restored.lendingRecords.size)
        assertEquals(0, restored.counterpartyMappings.size)
    }
}
