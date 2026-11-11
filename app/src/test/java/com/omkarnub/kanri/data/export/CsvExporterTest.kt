package com.omkarnub.kanri.data.export

import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import com.omkarnub.kanri.data.db.TransactionWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testEscapeCsv_plainString_returnsSame() {
        val result = CsvExporter.escapeCsv("Simple Text")
        assertEquals("Simple Text", result)
    }

    @Test
    fun testEscapeCsv_withComma_quoted() {
        val result = CsvExporter.escapeCsv("Groceries, Food & Drinks")
        assertEquals("\"Groceries, Food & Drinks\"", result)
    }

    @Test
    fun testEscapeCsv_withDoubleQuotes_doubledAndQuoted() {
        val result = CsvExporter.escapeCsv("Paid for \"Special\" dinner")
        assertEquals("\"Paid for \"\"Special\"\" dinner\"", result)
    }

    @Test
    fun testEscapeCsv_withNewline_quoted() {
        val result = CsvExporter.escapeCsv("Line 1\nLine 2")
        assertEquals("\"Line 1\nLine 2\"", result)
    }

    @Test
    fun testCalculateSummary() {
        val transactions = listOf(
            createTx(id = 1, amount = 1500.0, type = "DEBIT"),
            createTx(id = 2, amount = 500.0, type = "DEBIT"),
            createTx(id = 3, amount = 3000.0, type = "CREDIT")
        )

        val summary = CsvExporter.calculateSummary(transactions)
        assertEquals(3, summary.transactionCount)
        assertEquals(2000.0, summary.totalDebit, 0.001)
        assertEquals(3000.0, summary.totalCredit, 0.001)
        assertEquals(1000.0, summary.netAmount, 0.001)
    }

    @Test
    fun testGenerateCsv_containsHeadersAndRows() {
        val transactions = listOf(
            createTx(
                id = 1,
                amount = 250.75,
                type = "DEBIT",
                counterparty = "Starbucks Coffee",
                categoryName = "Food & Dining",
                notes = "Latte, Croissant"
            )
        )

        val csv = CsvExporter.generateCsv(transactions, "September 2026")

        // Contains headers
        assertTrue(csv.contains("# Kanri Financial Statement"))
        assertTrue(csv.contains("# Period: September 2026"))
        assertTrue(csv.contains("Date,Time,Type,Amount (INR),Category,Counterparty,Source,Bank,Reference No,Notes"))

        // Contains data row with escaped values
        assertTrue(csv.contains("DEBIT,250.75"))
        assertTrue(csv.contains("Food & Dining"))
        assertTrue(csv.contains("Starbucks Coffee"))
        assertTrue(csv.contains("\"Latte, Croissant\""))

        // Contains summary
        assertTrue(csv.contains("# SUMMARY"))
        assertTrue(csv.contains("# Total Spent (Debit): INR 250.75"))
        assertTrue(csv.contains("# Total Received (Credit): INR 0.00"))
        assertTrue(csv.contains("# Net Cash Flow: INR -250.75"))
    }

    @Test
    fun testGenerateCsv_usesActiveCurrencyCodeAndExcludesTransfersAndDuplicatesFromSummary() {
        try {
            com.omkarnub.kanri.util.CurrencyUtils.setCurrency(com.omkarnub.kanri.util.AppCurrency.USD)
            val validDebit = createTx(id = 1, amount = 100.0, type = "DEBIT", counterparty = "Cafe")
            val validCredit = createTx(id = 2, amount = 400.0, type = "CREDIT", counterparty = "Salary")
            val dupRaw = createTx(id = 3, amount = 999.0, type = "DEBIT", counterparty = "Dup")
            val dupTx = dupRaw.copy(transaction = dupRaw.transaction.copy(isDuplicate = true))
            val transferRaw = createTx(id = 4, amount = 500.0, type = "DEBIT", counterparty = "ATM")
            val transferTx = transferRaw.copy(
                transaction = transferRaw.transaction.copy(wallet = "ONLINE", transferToWallet = "CASH")
            )

            val allTxs = listOf(validDebit, validCredit, dupTx, transferTx)
            val summary = CsvExporter.calculateSummary(allTxs)
            assertEquals(100.0, summary.totalDebit, 0.001)
            assertEquals(400.0, summary.totalCredit, 0.001)
            assertEquals(300.0, summary.netAmount, 0.001)

            val csv = CsvExporter.generateCsv(allTxs, "October 2026")
            assertTrue(csv.contains("Amount (USD)"))
            assertTrue(csv.contains("# Total Spent (Debit): USD 100.00"))
            assertTrue(csv.contains("# Total Received (Credit): USD 400.00"))
            assertTrue(csv.contains("# Net Cash Flow: USD 300.00"))
            assertTrue(csv.contains("TRANSFER,500.00"))
        } finally {
            com.omkarnub.kanri.util.CurrencyUtils.setCurrency(com.omkarnub.kanri.util.AppCurrency.INR)
        }
    }

    private fun createTx(
        id: Long,
        amount: Double,
        type: String,
        counterparty: String = "Merchant",
        categoryName: String = "General",
        notes: String? = null
    ): TransactionWithCategory {
        val entity = TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            sourceType = "UPI",
            timestamp = 1789632000000L, // fixed timestamp
            counterparty = counterparty,
            displayName = notes,
            bank = "HDFC",
            refNo = "REF$id",
            categoryId = 1L,
            rawSms = "Test SMS",
            isDuplicate = false,
            isManualEntry = false
        )
        val category = CategoryEntity(
            id = 1L,
            name = categoryName,
            colorHex = "#4CAF50",
            iconName = "restaurant"
        )
        return TransactionWithCategory(transaction = entity, category = category)
    }
}
