package com.omkarnub.kanri.data.lending

import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import kotlin.math.abs
import kotlin.math.min

object LendingMoneyEngine {

    fun effectiveOriginal(entity: LendingEntity): Double =
        entity.originalAmount ?: entity.amount

    fun outstanding(entity: LendingEntity): Double =
        if (entity.isSettled) 0.0 else entity.amount

    fun canEditAmountOrType(repayments: List<LendingRepaymentEntity>): Boolean =
        repayments.isEmpty()

    /**
     * Executes repay(r).
     * Requires 0 < r <= outstanding.
     * If original_amount is null, sets it to current amount.
     * If r < amount: inserts repayment row, amount -= r.
     * If r == amount: inserts repayment row, is_settled = 1, amount unchanged.
     */
    fun applyRepayment(
        entity: LendingEntity,
        repaymentAmount: Double,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): Pair<LendingEntity, LendingRepaymentEntity> {
        require(repaymentAmount > 0.0) { "Repayment amount must be greater than zero ($repaymentAmount)" }
        val currentOutstanding = outstanding(entity)
        require(!entity.isSettled && currentOutstanding > 0.0) { "Cannot repay an already settled entry" }
        require(repaymentAmount <= currentOutstanding + 0.0001) {
            "Repayment amount ($repaymentAmount) cannot exceed outstanding balance ($currentOutstanding)"
        }

        val original = entity.originalAmount ?: entity.amount
        val repayment = LendingRepaymentEntity(
            lendingId = entity.id,
            amount = repaymentAmount,
            paidAt = paidAt,
            note = note
        )

        val isExact = abs(repaymentAmount - currentOutstanding) < 0.0001 || repaymentAmount >= currentOutstanding
        val updated = if (isExact) {
            entity.copy(
                originalAmount = original,
                amount = entity.amount, // amount unchanged
                isSettled = true
            )
        } else {
            entity.copy(
                originalAmount = original,
                amount = (entity.amount - repaymentAmount).coerceAtLeast(0.0),
                isSettled = false
            )
        }
        return Pair(updated, repayment)
    }

    /**
     * Executes settleAll(entry) == repay(outstanding).
     */
    fun applySettleAll(
        entity: LendingEntity,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): Pair<LendingEntity, LendingRepaymentEntity> {
        val currentOutstanding = outstanding(entity)
        return applyRepayment(entity, currentOutstanding, paidAt, note)
    }

    /**
     * Executes undoLastRepayment(entry, repayments).
     * Works on the latest repayment only.
     * If entry is settled: is_settled = 0, amount unchanged.
     * Otherwise: amount += r.
     */
    fun applyUndoLastRepayment(
        entity: LendingEntity,
        repayments: List<LendingRepaymentEntity>
    ): Pair<LendingEntity, LendingRepaymentEntity> {
        require(repayments.isNotEmpty()) { "No repayments to undo" }
        val latest = repayments.sortedWith(
            compareByDescending<LendingRepaymentEntity> { it.paidAt }.thenByDescending { it.id }
        ).first()

        val updated = if (entity.isSettled) {
            entity.copy(
                isSettled = false // amount left unchanged
            )
        } else {
            entity.copy(
                amount = entity.amount + latest.amount
            )
        }
        return Pair(updated, latest)
    }

    /**
     * Reopening a legacy settled entry (no repayment rows) just flips is_settled = 0.
     */
    fun applyReopenLegacy(
        entity: LendingEntity,
        repayments: List<LendingRepaymentEntity>
    ): LendingEntity {
        require(repayments.isEmpty()) { "Cannot reopen legacy settled entry when repayment rows exist; undo repayment instead" }
        require(entity.isSettled) { "Entry is not settled" }
        return entity.copy(isSettled = false)
    }

    /**
     * Person-level repayment allocates oldest-first (by date ASC, id ASC)
     * across that person's open entries of the chosen direction, capped at total outstanding.
     */
    fun allocatePersonRepayment(
        openEntries: List<LendingEntity>,
        repaymentAmount: Double,
        paidAt: Long = System.currentTimeMillis(),
        note: String? = null
    ): List<Pair<LendingEntity, LendingRepaymentEntity>> {
        require(repaymentAmount > 0.0) { "Repayment amount must be positive" }
        val sorted = openEntries.sortedWith(compareBy<LendingEntity> { it.date }.thenBy { it.id })
        val totalOutstanding = sorted.sumOf { outstanding(it) }
        val toAllocate = min(repaymentAmount, totalOutstanding)
        var remaining = toAllocate
        val results = mutableListOf<Pair<LendingEntity, LendingRepaymentEntity>>()

        for (entry in sorted) {
            if (remaining <= 0.0001) break
            val out = outstanding(entry)
            if (out <= 0.0) continue
            val chunk = min(remaining, out)
            val result = applyRepayment(entry, chunk, paidAt, note)
            results.add(result)
            remaining -= chunk
        }
        return results
    }
}
