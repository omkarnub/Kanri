package com.omkarnub.kanri.ui.recurring

import com.omkarnub.kanri.data.db.RecurringPaymentEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class RecurringCandidate(
    val title: String,
    val amount: Double,
    val counterparty: String?,
    val occurrenceCount: Int
)

object RecurringPaymentCalculator {

    fun calculateDaysRemaining(
        nextDueTimestamp: Long,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Int {
        val diffMs = nextDueTimestamp - currentTimestamp
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        return if (diffMs < 0 && diffMs > -TimeUnit.DAYS.toMillis(1)) {
            // Within overdue by less than 24 hours
            -1
        } else if (diffMs >= 0 && diffMs < TimeUnit.DAYS.toMillis(1)) {
            // Due today
            0
        } else {
            days
        }
    }

    fun advanceDueDate(
        currentDueTimestamp: Long,
        billingCycle: String
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = currentDueTimestamp }
        when (billingCycle.uppercase(Locale.US)) {
            "YEARLY" -> cal.add(Calendar.YEAR, 1)
            "WEEKLY" -> cal.add(Calendar.DAY_OF_YEAR, 7)
            else -> cal.add(Calendar.MONTH, 1) // Default MONTHLY
        }
        return cal.timeInMillis
    }

    fun calculateTotalMonthlyCommitted(subscriptions: List<RecurringPaymentEntity>): Double {
        var total = 0.0
        for (sub in subscriptions) {
            if (!sub.isActive) continue
            total += when (sub.billingCycle.uppercase(Locale.US)) {
                "YEARLY" -> sub.amount / 12.0
                "WEEKLY" -> sub.amount * (52.0 / 12.0)
                else -> sub.amount // MONTHLY
            }
        }
        return total
    }

    fun autoDetectCandidates(transactions: List<TransactionEntity>): List<RecurringCandidate> {
        val debits = transactions.filter { it.type.equals("DEBIT", ignoreCase = true) }
        val grouped = debits.groupBy { (it.counterparty ?: it.displayName ?: "Unknown").trim() }

        val candidates = mutableListOf<RecurringCandidate>()
        for ((counterparty, txList) in grouped) {
            if (counterparty.isBlank() || counterparty.equals("Unknown", ignoreCase = true)) continue
            // Group by approximate amount within 5%
            val amountBuckets = txList.groupBy { Math.round(it.amount * 10) / 10.0 }
            for ((amount, matchingList) in amountBuckets) {
                if (matchingList.size >= 2) {
                    candidates.add(
                        RecurringCandidate(
                            title = counterparty,
                            amount = amount,
                            counterparty = counterparty,
                            occurrenceCount = matchingList.size
                        )
                    )
                }
            }
        }
        return candidates.sortedByDescending { it.occurrenceCount }
    }
}
