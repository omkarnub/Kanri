package com.omkarnub.kanri.data.parser

import java.util.regex.Pattern

enum class TransactionType {
    DEBIT,
    CREDIT
}

enum class SourceType {
    UPI,
    ATM,
    CARD,
    BANK_TRANSFER,
    UNKNOWN
}

data class ParsedTransaction(
    val type: TransactionType,
    val amount: Double,
    val sourceType: SourceType,
    val counterparty: String?,
    val bank: String?,
    val refNo: String?,
    val account: String?,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)

object SmsParser {

    private val KNOWN_BANKS = mapOf(
        "BOB" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "SBI" to "State Bank of India",
        "HDFC" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "PNB" to "Punjab National Bank",
        "CANARA" to "Canara Bank",
        "UNIONB" to "Union Bank",
        "INDUS" to "IndusInd Bank",
        "PAYTM" to "Paytm Payments Bank",
        "AIRTEL" to "Airtel Payments Bank",
        "FAMPAY" to "FamPay"
    )

    fun parse(body: String, sender: String? = null): ParsedTransaction? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null

        // Ignore common OTP and non-transactional messages
        if (isNonTransactional(trimmed)) return null

        val type = detectTransactionType(trimmed) ?: return null
        val amount = extractAmount(trimmed) ?: return null
        val sourceType = detectSourceType(trimmed)
        val bank = detectBank(trimmed, sender)
        val refNo = extractRefNo(trimmed)
        val account = extractAccount(trimmed)
        val counterparty = extractCounterparty(trimmed, sourceType)

        return ParsedTransaction(
            type = type,
            amount = amount,
            sourceType = sourceType,
            counterparty = counterparty,
            bank = bank,
            refNo = refNo,
            account = account,
            rawText = trimmed
        )
    }

    private fun detectTransactionType(body: String): TransactionType? {
        val lower = body.lowercase()

        // Actual reversals and refunds (exclude disclaimers like "will be automatically reversed within 48 hours")
        val isActualRefundOrReversal = lower.contains("refunded to") ||
                lower.contains("refund of") ||
                lower.contains("credited back") ||
                lower.contains("has been reversed") ||
                lower.contains("is reversed") ||
                lower.contains("reversal of") ||
                (lower.contains("reversed") && !lower.contains("will be") && !lower.contains("in case"))

        if (isActualRefundOrReversal) {
            return TransactionType.CREDIT
        }

        val isDebit = lower.contains("debited") ||
                lower.contains("withdrawn") ||
                lower.contains("spent") ||
                lower.contains("sent rs") ||
                lower.contains("sent inr") ||
                lower.contains("sent to") ||
                lower.contains("you sent") ||
                lower.contains("money sent") ||
                lower.contains("paid rs") ||
                lower.contains("paid inr") ||
                lower.contains("paid to") ||
                lower.startsWith("paid ")

        val isCredit = lower.contains("credited") ||
                lower.contains("deposited") ||
                lower.contains("received rs") ||
                lower.contains("received inr") ||
                lower.contains("received from") ||
                lower.contains("you received") ||
                lower.contains("money received")

        return when {
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun isNonTransactional(body: String): Boolean {
        val lower = body.lowercase()

        // Declined or failed transactions without successful debit
        if ((lower.contains("declined") || lower.contains("transaction failed")) &&
            !lower.contains("debited") && !lower.contains("credited")
        ) {
            return true
        }

        // Pure OTP or login messages
        if ((lower.contains("otp") || lower.contains("verification code")) &&
            !lower.contains("debited") && !lower.contains("credited") && !lower.contains("withdrawn")
        ) {
            return true
        }
        // Promotional messages
        if (lower.contains("pre-approved loan") || lower.contains("apply now") || lower.contains("win cash")) {
            return true
        }
        return false
    }

    private fun detectBank(body: String, sender: String?): String? {
        // Priority 1: Check sender ID via BankSmsPatterns
        if (!sender.isNullOrBlank()) {
            val matchedBank = BankSmsPatterns.findBySender(sender)
            if (matchedBank != null) {
                return matchedBank.bankName
            }
        }

        // Priority 2: Check message body via BankSmsPatterns
        val matchedByBody = BankSmsPatterns.findByBody(body)
        if (matchedByBody != null) {
            return matchedByBody.bankName
        }

        // Priority 3: Fallback check
        val bodyUpper = body.uppercase()
        for ((code, name) in KNOWN_BANKS) {
            if (bodyUpper.contains("$code BANK") || bodyUpper.contains("BANK OF $code") || bodyUpper.contains(" $code ")) {
                return name
            }
        }
        for ((code, name) in KNOWN_BANKS) {
            if (bodyUpper.contains(code)) {
                return name
            }
        }
        return null
    }

    private fun detectSourceType(body: String): SourceType {
        val lower = body.lowercase()
        return when {
            lower.contains("atm") -> SourceType.ATM
            lower.contains("upi") || lower.contains("vpa") || lower.contains("@") -> SourceType.UPI
            lower.contains("credit card") || lower.contains("debit card") || lower.contains("spent") || lower.contains("pos") -> SourceType.CARD
            lower.contains("imps") || lower.contains("neft") || lower.contains("rtgs") -> SourceType.BANK_TRANSFER
            else -> SourceType.UNKNOWN
        }
    }

    private fun extractAmount(body: String): Double? {
        // Pattern 1: Rs. 4000.00 / INR 500 / Rs 1,500.00 / ₹500
        val p1 = Pattern.compile("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        val m1 = p1.matcher(body)
        if (m1.find()) {
            val amt = parseAmount(m1.group(1))
            if (amt != null) return amt
        }

        // Pattern 2: debited by 500.0 / credited by 1200.0 / debited for Rs... / debited with INR...
        val p2 = Pattern.compile("""(?:debited|credited|withdrawn|spent|paid|sent|received)\s+(?:by|for|with|of)?\s*(?:Rs\.?|INR|₹)?\s*([\d,]+(?:\.\d+)?)""", Pattern.CASE_INSENSITIVE)
        val m2 = p2.matcher(body)
        if (m2.find()) {
            val amt = parseAmount(m2.group(1))
            if (amt != null) return amt
        }

        return null
    }

    private fun extractRefNo(body: String): String? {
        val pattern = Pattern.compile(
            """\b(?:UPI\s+Ref(?:\s+no)?|Ref(?:no|\.|\s+no)?|UTR|Txn\s*ID|UPI\/)\b\s*[:#.\/]?\s*([0-9a-zA-Z]+)""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }


    private fun extractAccount(body: String): String? {
        // "A/c ... 8477", "A/C X8477", "Credit Card ending 1234", "A/C *1234", "A/c no. XX3948"
        val pattern = Pattern.compile(
            """(?:A/c|Account|acct|acc\.?|Card)\s*(?:no\.?)?\s*(?:ending\s+)?(?:with\s+)?(\.{2,}\s*\d+|[xX*]+\s*\d+|\d{4,})""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun extractCounterparty(body: String, sourceType: SourceType): String? {
        if (sourceType == SourceType.ATM) {
            val tidPattern = Pattern.compile("""at\s+ATM\s+(?:TID\s+)?([A-Za-z0-9]+)""", Pattern.CASE_INSENSITIVE)
            val tidMatcher = tidPattern.matcher(body)
            if (tidMatcher.find()) {
                return "ATM TID ${tidMatcher.group(1)}"
            }
            return "ATM"
        }

        val delimiter = """(?=\s+(?:on|via|ref|refno|upi|using|from|order|purchase|\()|\s*[\.,;]|$)"""

        val patterns = listOf(
            // "Info: UPI/Apollo Pharmacy" or "Info: Dominos"
            Pattern.compile("""(?:Info:\s*UPI\/|Info:\s*)([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "to swiggy@icici on 12-09-26" or "to Rohit Kumar Refno..." or "towards Amazon Pay..."
            Pattern.compile("""(?:trf\s+to|towards\s+|paid\s+to|to\s+(?:VPA\s+)?|to\s+)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "Transferred to Uber"
            Pattern.compile("""(?:Transferred\s+to\s+)([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "for Swiggy order..." or "for D-Mart purchase..."
            Pattern.compile("""(?:refund\s+for|for\s+)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "from transfer from John Doe (UPI..." or "from friend@upi"
            Pattern.compile("""(?:transfer\s+from|from\s+)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "by Vikas" or "by Neha" (excluding amounts like "by Rs" / "by INR")
            Pattern.compile("""(?:by\s+(?!Rs|INR|₹|\d))([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "at STARBUCKS on 14-09-26"
            Pattern.compile("""at\s+([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val candidate = cleanCounterparty(matcher.group(1))
                if (!candidate.isNullOrBlank() &&
                    !candidate.equals("ATM", ignoreCase = true) &&
                    !candidate.startsWith("Rs", ignoreCase = true) &&
                    !candidate.startsWith("INR", ignoreCase = true)
                ) {
                    return candidate
                }
            }
        }

        return null
    }

    private fun parseAmount(str: String?): Double? {
        if (str == null) return null
        return try {
            str.replace(",", "").toDouble()
        } catch (_: Exception) {
            null
        }
    }

    private fun cleanCounterparty(party: String?): String? {
        if (party == null) return null
        var cleaned = party.trim()

        // Strip leading prefixes
        val removePrefixes = listOf("to ", "from ", "VPA ", "vpa ", "transfer from ", "by ", "by transfer from ", "through UPI from ", "through UPI to ")
        for (prefix in removePrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }

        // Strip trailing punctuation or brackets
        cleaned = cleaned.trimEnd('.', ',', '(', ')', ';', ':')

        return cleaned.ifEmpty { null }
    }
}

