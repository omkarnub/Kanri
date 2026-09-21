package com.omkarnub.kanri.ui.insights

import com.omkarnub.kanri.data.db.TransactionDao
import com.omkarnub.kanri.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Debug-only seed generator for stress-testing Insights queries on 20,000 transactions.
 */
object InsightsSeedDataGenerator {

    suspend fun seed20kTransactionsIfEmpty(transactionDao: TransactionDao, force: Boolean = false) = withContext(Dispatchers.IO) {
        val existingCount = transactionDao.getAllTransactionsSync().size
        if (!force && existingCount >= 20_000) return@withContext

        val counterparties = listOf(
            "Swiggy", "Zomato", "Uber", "Amazon", "Blinkit", "Flipkart", "Netflix", "Spotify",
            "Starbucks", "Shell Petrol", "Airtel", "Jio", "Electricity Board", "Salary Corp",
            "Freelance Client", "Mutual Fund", "Rent Transfer", "Google Play", "Apple Store", "Decathlon"
        )

        val sources = listOf("UPI", "CARD", "ATM", "BANK_TRANSFER")
        val now = System.currentTimeMillis()
        val oneYearMillis = 365L * 86_400_000L

        val batch = ArrayList<TransactionEntity>(1000)
        val random = Random(42)

        for (i in 1..20_000) {
            val isIncome = random.nextInt(100) < 15 // 15% income, 85% expense
            val counterparty = counterparties[random.nextInt(counterparties.size)]
            val source = sources[random.nextInt(sources.size)]
            val timestamp = now - (random.nextDouble() * oneYearMillis).toLong()

            val amount = if (isIncome) {
                (random.nextInt(20, 1500) * 100).toDouble()
            } else {
                when (random.nextInt(100)) {
                    in 0..45 -> (random.nextInt(20, 200)).toDouble() // small spends
                    in 46..85 -> (random.nextInt(200, 2500)).toDouble()
                    in 86..96 -> (random.nextInt(2500, 10000)).toDouble()
                    else -> (random.nextInt(10000, 50000)).toDouble() // occasional large
                }
            }

            batch.add(
                TransactionEntity(
                    id = 0,
                    type = if (isIncome) "CREDIT" else "DEBIT",
                    amount = amount,
                    sourceType = source,
                    counterparty = counterparty,
                    displayName = counterparty,
                    bank = "HDFC",
                    refNo = "REF${i}_$timestamp",
                    timestamp = timestamp,
                    categoryId = if (random.nextBoolean()) (random.nextLong(1, 8)) else null,
                    rawSms = "Debug transaction #$i",
                    isDuplicate = false,
                    isManualEntry = false,
                    notes = null
                )
            )

            if (batch.size >= 1000) {
                transactionDao.insertAll(batch)
                batch.clear()
            }
        }

        if (batch.isNotEmpty()) {
            transactionDao.insertAll(batch)
            batch.clear()
        }
    }
}
