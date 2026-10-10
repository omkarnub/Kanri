package com.omkarnub.kanri.data.parser

import com.omkarnub.kanri.data.db.CategoryDao
import com.omkarnub.kanri.data.db.TransactionDao
import com.omkarnub.kanri.data.db.TransactionEntity

sealed class DeduplicationResult {
    data class Inserted(val id: Long, val entity: TransactionEntity) : DeduplicationResult()
    data class Enriched(val id: Long, val updatedCounterparty: String) : DeduplicationResult()
    data class SkippedDuplicate(val existingId: Long) : DeduplicationResult()
}

/**
 * Deduplication Engine for Kanri's dual SMS and Notification pipelines.
 *
 * Rules:
 * 1. SMS is primary for all Debits. NotificationListener is supplementary for Credits/Rewards only.
 * 2. When both an SMS and a notification fire for the same incoming payment:
 *    - If amount matches and timestamps are within 5 minutes (+/- 300,000 ms), they are merged
 *      into a single transaction rather than creating duplicates.
 *    - If notification arrived first, subsequent bank SMS enriches the bank institution, refNo, and raw SMS.
 *    - If bank SMS arrived first, subsequent notification enriches the payee/counterparty display name.
 * 3. If amounts differ or timestamps are > 5 minutes apart, they are kept as separate entries.
 * 4. Exact ref_no (UTR) match always deduplicates.
 */
object NotificationDeduplicationHelper {

    const val DEDUPE_WINDOW_MS = 5 * 60 * 1000L // 5 minutes sliding window

    suspend fun processIncomingTransaction(
        dao: TransactionDao,
        categoryDao: CategoryDao,
        parsed: ParsedTransaction,
        atmMode: com.omkarnub.kanri.data.wallet.AtmWithdrawalMode = com.omkarnub.kanri.data.wallet.AtmWithdrawalMode.SPENDING
    ): DeduplicationResult {
        // 1. Check exact ref_no if present
        if (!parsed.refNo.isNullOrBlank()) {
            val existingByRef = dao.findByRefNo(parsed.refNo)
            if (existingByRef != null) {
                var enriched = false
                if (shouldEnrich(existingByRef.counterparty, parsed.counterparty)) {
                    val enrichedName = parsed.counterparty!!
                    dao.enrichCounterpartyIfEmpty(existingByRef.id, enrichedName, enrichedName)
                    enriched = true
                }
                if (!parsed.bank.isNullOrBlank() && (existingByRef.bank == null || isPaymentAppBank(existingByRef.bank))) {
                    dao.enrichBankIfGeneric(existingByRef.id, parsed.bank)
                    enriched = true
                }
                if (parsed.rawText.isNotBlank() && isBankSms(parsed.rawText)) {
                    dao.enrichRawSmsIfFromNotification(existingByRef.id, parsed.rawText)
                    enriched = true
                }
                if (enriched) {
                    return DeduplicationResult.Enriched(existingByRef.id, parsed.counterparty ?: existingByRef.counterparty ?: "")
                }
                return DeduplicationResult.SkippedDuplicate(existingByRef.id)
            }
        }

        // 2. Sliding window check: match by Type, Amount, and Timestamp (+/- 5 minutes)
        val minTime = parsed.timestamp - DEDUPE_WINDOW_MS
        val maxTime = parsed.timestamp + DEDUPE_WINDOW_MS
        val existingMatch = dao.findRecentMatching(
            type = parsed.type.name,
            amount = parsed.amount,
            minTimestamp = minTime,
            maxTimestamp = maxTime
        )

        val hasDifferentRefNo = existingMatch != null &&
                !parsed.refNo.isNullOrBlank() &&
                !existingMatch.refNo.isNullOrBlank() &&
                parsed.refNo != existingMatch.refNo

        val hasDistinctMerchants = existingMatch != null &&
                !parsed.counterparty.isNullOrBlank() &&
                !existingMatch.counterparty.isNullOrBlank() &&
                !isGenericMerchant(parsed.counterparty) &&
                !isGenericMerchant(existingMatch.counterparty) &&
                !parsed.counterparty!!.trim().equals(existingMatch.counterparty!!.trim(), ignoreCase = true)

        if (existingMatch != null && !hasDifferentRefNo && !hasDistinctMerchants) {
            var enriched = false
            if (shouldEnrich(existingMatch.counterparty, parsed.counterparty)) {
                val enrichedName = parsed.counterparty!!
                dao.enrichCounterpartyIfEmpty(existingMatch.id, enrichedName, enrichedName)
                enriched = true
            }
            if (!parsed.refNo.isNullOrBlank() && existingMatch.refNo.isNullOrBlank()) {
                dao.enrichRefNoIfEmpty(existingMatch.id, parsed.refNo)
                enriched = true
            }
            if (!parsed.bank.isNullOrBlank() && (existingMatch.bank == null || isPaymentAppBank(existingMatch.bank))) {
                dao.enrichBankIfGeneric(existingMatch.id, parsed.bank)
                enriched = true
            }
            if (parsed.rawText.isNotBlank() && isBankSms(parsed.rawText)) {
                dao.enrichRawSmsIfFromNotification(existingMatch.id, parsed.rawText)
                enriched = true
            }
            if (enriched) {
                return DeduplicationResult.Enriched(existingMatch.id, parsed.counterparty ?: existingMatch.counterparty ?: "")
            }
            return DeduplicationResult.SkippedDuplicate(existingMatch.id)
        }

        val resolvedWallet = com.omkarnub.kanri.data.wallet.WalletResolver.resolve(
            sourceType = parsed.sourceType.name,
            type = parsed.type.name,
            atmMode = atmMode,
            isRefund = parsed.isRefund
        )
        val isTransfer = resolvedWallet.transferToWallet != null

        // 3. New unique transaction -> check for auto-assigned category
        var categoryId: Long? = null
        if (!isTransfer) {
            if (parsed.sourceType == SourceType.ATM) {
                val atmCat = categoryDao.getCategoryByName("Cash & ATM")
                categoryId = atmCat?.id
            } else if (!parsed.counterparty.isNullOrBlank()) {
                val mapping = categoryDao.findSmartRuleForCounterparty(parsed.counterparty)
                    ?: categoryDao.getMappingForCounterparty(parsed.counterparty)
                categoryId = mapping?.categoryId
            }
        }

        // Evaluate review criteria
        val otherCategory = categoryDao.getCategoryByName("Other")
        val isUncategorizedOrOther = categoryId == null || categoryId == otherCategory?.id
        val isMerchantEmptyOrUnknown = parsed.counterparty.isNullOrBlank() ||
                parsed.counterparty.trim().equals("unknown", ignoreCase = true) ||
                parsed.counterparty.trim().equals("upi", ignoreCase = true) ||
                parsed.counterparty.trim().equals("upi payment", ignoreCase = true) ||
                parsed.counterparty.trim().equals("bank transfer", ignoreCase = true)
        val isLowConfidence = parsed.sourceType == SourceType.UNKNOWN || (parsed.bank == null && parsed.refNo == null)

        val isCashDepositCredit = parsed.type == TransactionType.CREDIT && (
                parsed.rawText.contains("cdm", ignoreCase = true) ||
                        parsed.rawText.contains("cash deposit", ignoreCase = true) ||
                        parsed.rawText.contains("deposited in cash", ignoreCase = true)
                )

        val (needsReview, reviewReason) = when {
            isTransfer -> false to null
            isCashDepositCredit -> true to "Looks like a cash deposit"
            hasDifferentRefNo -> true to "Probable duplicate (same amount within 5m)"
            isMerchantEmptyOrUnknown -> true to "Merchant unknown or missing"
            isUncategorizedOrOther -> true to "Uncategorized or Other category"
            isLowConfidence -> true to "Low parse confidence"
            else -> false to null
        }

        val entity = TransactionEntity(
            type = parsed.type.name,
            amount = parsed.amount,
            sourceType = parsed.sourceType.name,
            counterparty = parsed.counterparty,
            displayName = parsed.counterparty,
            bank = parsed.bank,
            refNo = parsed.refNo,
            timestamp = parsed.timestamp,
            categoryId = categoryId,
            rawSms = parsed.rawText,
            needsReview = needsReview,
            reviewReason = reviewReason,
            wallet = resolvedWallet.wallet,
            transferToWallet = resolvedWallet.transferToWallet
        )

        val insertedId = dao.insert(entity)
        return DeduplicationResult.Inserted(insertedId, entity)
    }

    private fun shouldEnrich(existingCounterparty: String?, incomingCounterparty: String?): Boolean {
        if (incomingCounterparty.isNullOrBlank()) return false
        if (existingCounterparty.isNullOrBlank()) return true
        val existingLower = existingCounterparty.trim().lowercase()
        return isGenericMerchant(existingLower) && !isGenericMerchant(incomingCounterparty)
    }

    private fun isGenericMerchant(name: String?): Boolean {
        if (name.isNullOrBlank()) return true
        val lower = name.trim().lowercase()
        return lower == "upi" || lower == "upi payment" || lower == "bank transfer" ||
                lower == "expense" || lower == "income" || lower == "unknown" ||
                lower == "payment" || lower == "upi credit"
    }

    private fun isPaymentAppBank(bank: String?): Boolean {
        if (bank.isNullOrBlank()) return true
        val lower = bank.trim().lowercase()
        return lower == "google pay" || lower == "paytm" || lower == "phonepe" ||
                lower == "fampay" || lower == "bhim upi" || lower == "amazon pay" ||
                lower == "payment"
    }

    private fun isBankSms(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("debited") || lower.contains("credited") || lower.contains("a/c") ||
                lower.contains("account") || lower.contains("withdrawn") || lower.contains("spent")
    }
}
