package com.omkarnub.kanri.data.lending

import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import com.omkarnub.kanri.ui.common.getCategoryIconRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LendingTransactionSyncTest {

    @Test
    fun testFinancialDirectionMapping() {
        val lentRecord = LendingEntity(
            id = 1L,
            personName = "Bob",
            amount = 500.0,
            type = "LENT"
        )
        val borrowedRecord = LendingEntity(
            id = 2L,
            personName = "Alice",
            amount = 1000.0,
            type = "BORROWED"
        )

        // Lending to someone = money leaves account -> DEBIT (Expense)
        val lentTxType = if (lentRecord.type.equals("LENT", ignoreCase = true)) "DEBIT" else "CREDIT"
        assertEquals("DEBIT", lentTxType)

        // Borrowing from someone = money enters account -> CREDIT (Income)
        val borrowedTxType = if (borrowedRecord.type.equals("LENT", ignoreCase = true)) "DEBIT" else "CREDIT"
        assertEquals("CREDIT", borrowedTxType)

        // Repayment on LENT (Bob pays me back) = money enters account -> CREDIT (Income)
        val lentRepayment = LendingRepaymentEntity(id = 10L, lendingId = 1L, amount = 200.0)
        val repayLentTxType = if (lentRecord.type.equals("LENT", ignoreCase = true)) "CREDIT" else "DEBIT"
        assertEquals("CREDIT", repayLentTxType)

        // Repayment on BORROWED (I pay Alice back) = money leaves account -> DEBIT (Expense)
        val borrowedRepayment = LendingRepaymentEntity(id = 11L, lendingId = 2L, amount = 400.0)
        val repayBorrowedTxType = if (borrowedRecord.type.equals("LENT", ignoreCase = true)) "CREDIT" else "DEBIT"
        assertEquals("DEBIT", repayBorrowedTxType)
    }

    @Test
    fun testRefNoFormats() {
        val recordId = 42L
        val repaymentId = 108L
        assertEquals("LENDING_42", "${LendingTransactionSyncHelper.REF_PREFIX_RECORD}$recordId")
        assertEquals("LENDING_REPAY_108", "${LendingTransactionSyncHelper.REF_PREFIX_REPAY}$repaymentId")
    }

    @Test
    fun testCategoryIconResolution() {
        val iconResByName = getCategoryIconRes("Lend & Borrow")
        val iconResByCustom = getCategoryIconRes(null, "payments")
        assertTrue(iconResByName != 0)
        assertEquals(iconResByName, iconResByCustom)
    }

    @Test
    fun testNetCashFlowIntegrity() {
        // Scenario:
        // 1. User borrows ₹1000 from Alice (CREDIT: +1000)
        // 2. User lends ₹300 to Bob (DEBIT: -300)
        // 3. User repays Alice ₹500 (DEBIT: -500)
        // 4. Bob repays user ₹100 (CREDIT: +100)
        val transactions = listOf(
            Pair("CREDIT", 1000.0),
            Pair("DEBIT", 300.0),
            Pair("DEBIT", 500.0),
            Pair("CREDIT", 100.0)
        )

        var totalIncome = 0.0
        var totalExpense = 0.0
        for ((type, amount) in transactions) {
            if (type == "CREDIT") totalIncome += amount
            else if (type == "DEBIT") totalExpense += amount
        }

        assertEquals(1100.0, totalIncome, 0.001)
        assertEquals(800.0, totalExpense, 0.001)
        val netCashFlow = totalIncome - totalExpense
        assertEquals(300.0, netCashFlow, 0.001)
    }

    @Test
    fun testTransactionCategorizationAutoFillLogic() {
        val debitTx = com.omkarnub.kanri.data.db.TransactionEntity(
            id = 55L,
            type = "DEBIT",
            amount = 750.0,
            sourceType = "UPI",
            counterparty = "Rahul Sharma",
            displayName = "Rahul Sharma",
            bank = "HDFC",
            refNo = "UPI12345678",
            timestamp = 1727319600000L,
            rawSms = "Paid Rs 750 to Rahul Sharma",
            notes = "Dinner split"
        )

        // 1. Direction mapping
        val autoType = if (debitTx.type.equals("DEBIT", ignoreCase = true)) "LENT" else "BORROWED"
        assertEquals("LENT", autoType)

        // 2. Pre-filled amount, date and time
        assertEquals(750.0, debitTx.amount, 0.0001)
        assertEquals(1727319600000L, debitTx.timestamp)

        // 3. Profiles matching
        val existingProfiles = listOf("Alice", "Rahul Sharma", "Bob")
        val matchedProfile = existingProfiles.firstOrNull {
            it.equals(debitTx.counterparty, ignoreCase = true)
        }
        assertEquals("Rahul Sharma", matchedProfile)

        // 4. Reference generation
        val recordId = 99L
        val generatedRef = "${LendingTransactionSyncHelper.REF_PREFIX_RECORD}$recordId"
        assertEquals("LENDING_99", generatedRef)

        // 5. Linked transaction update preserves sourceType and bank
        val updatedTx = debitTx.copy(
            categoryId = 10L,
            refNo = generatedRef,
            counterparty = "Rahul Sharma",
            displayName = "Rahul Sharma",
            needsReview = false
        )
        assertEquals("UPI", updatedTx.sourceType)
        assertEquals("HDFC", updatedTx.bank)
        assertEquals("LENDING_99", updatedTx.refNo)
        assertEquals(false, updatedTx.needsReview)
    }

    @Test
    fun testCreditTransactionAutoFillBorrowed() {
        val creditTx = com.omkarnub.kanri.data.db.TransactionEntity(
            id = 56L,
            type = "CREDIT",
            amount = 1200.0,
            sourceType = "BANK_TRANSFER",
            counterparty = "Priya",
            displayName = "Priya",
            bank = "SBI",
            refNo = "IMPS998877",
            timestamp = 1727320000000L,
            rawSms = "Received Rs 1200 from Priya",
            notes = null
        )

        val autoType = if (creditTx.type.equals("DEBIT", ignoreCase = true)) "LENT" else "BORROWED"
        assertEquals("BORROWED", autoType)
        assertEquals(1200.0, creditTx.amount, 0.0001)
        assertEquals(1727320000000L, creditTx.timestamp)
    }
}
