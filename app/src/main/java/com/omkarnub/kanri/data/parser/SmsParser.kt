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
    UNKNOWN,
    WALLET_TRANSFER
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
    val timestamp: Long = System.currentTimeMillis(),
    val balance: Double? = null,
    val creditLimit: Double? = null,
    val isCard: Boolean = false,
    val isRefund: Boolean = false
)

object SmsParser {

    private val KNOWN_BANKS = mapOf(
        "SBI" to "State Bank of India",
        "BOB" to "Bank of Baroda",
        "BARODA" to "Bank of Baroda",
        "HDFC" to "HDFC Bank",
        "ICICI" to "ICICI Bank",
        "AXIS" to "Axis Bank",
        "KOTAK" to "Kotak Mahindra Bank",
        "PNB" to "Punjab National Bank",
        "CANARA" to "Canara Bank",
        "UNIONB" to "Union Bank of India",
        "BOI" to "Bank of India",
        "IDFC" to "IDFC First Bank",
        "YESBNK" to "Yes Bank",
        "INDUS" to "IndusInd Bank",
        "FEDBNK" to "Federal Bank",
        "RBLBNK" to "RBL Bank",
        "IDBI" to "IDBI Bank",
        "CENTBK" to "Central Bank of India",
        "UCO" to "UCO Bank",
        "MAHA" to "Bank of Maharashtra",
        "PAYTM" to "Paytm Payments Bank",
        "AIRTEL" to "Airtel Payments Bank",
        "JIO" to "Jio Payments Bank",
        "SRSWAT" to "Saraswat Bank",
        "COSMOS" to "Cosmos Bank",
        "FAMPAY" to "FamPay",
        "CRED" to "CRED",
        "ONECRD" to "OneCard",
        "SLICEC" to "Slice"
    )

    // Regex for extracting balances
    private val BALANCE_PATTERN = Pattern.compile(
        """(?i)(?:avl(?:bl)?\s*(?:bal|amt|balance)|available\s*(?:bal|balance|limit)|a/c\s*bal|ac\s*bal|total\s*bal|bal(?:ance)?|updated\s*bal(?:ance)?|new\s*bal(?:ance)?|remaining\s*bal(?:ance)?)[\s:]+(?:(?:is\s+)?(?:rs\.?|inr|₹)\s*)?([0-9,]+(?:\.\d{1,2})?)"""
    )

    // Regex for extracting credit limits
    private val CREDIT_LIMIT_PATTERN = Pattern.compile(
        """(?i)(?:available\s*credit\s*limit|avl\s*limit|available\s*limit|credit\s*limit)[\s:]+(?:(?:rs\.?|inr|₹)\s*)?([0-9,]+(?:\.\d{1,2})?)"""
    )

    // Regex for extracting account masks across all major bank variations (A/C, AC, Card, etc.)
    private val ACCOUNT_PATTERN = Pattern.compile(
        """(?i)\b(?:A/c|Account|acct|acc|ac|Card)\b\s*(?:no\.?)?\s*(?:ending\s+(?:in\s+|with\s+)?)?(?:with\s+)?(?:\.\.\.\s*|X+|\*+|-+)?(\d{3,6})"""
    )

    fun parse(body: String, sender: String? = null, timestamp: Long = System.currentTimeMillis()): ParsedTransaction? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return null

        // 1. Strict Negative Filtering (Ignore OTP, payment requests, bills due, pre-approved loans, mandates, failed txns)
        if (isNonTransactional(trimmed)) return null

        // 2. Transaction Type Detection (Debit vs Credit vs Refund)
        val type = detectTransactionType(trimmed) ?: return null
        val isRefund = detectIsRefund(trimmed)

        // 3. Balance and Limit Extraction (Disambiguate before extracting transaction amount)
        val balance = extractBalance(trimmed)
        val creditLimit = extractCreditLimit(trimmed)

        // 4. Amount Extraction (Filtered to ensure it doesn't match the balance)
        val amount = extractAmount(trimmed, balance, creditLimit) ?: return null

        // 5. Metadata Extraction
        val sourceType = detectSourceType(trimmed)
        val isCard = sourceType == SourceType.CARD || trimmed.contains("Card", ignoreCase = true)
        val bank = detectBank(trimmed, sender)
        val refNo = extractRefNo(trimmed)
        val account = extractAccount(trimmed)
        val counterparty = extractCounterparty(trimmed, sourceType)
        val resolvedTimestamp = extractTimestamp(trimmed, timestamp)

        return ParsedTransaction(
            type = type,
            amount = amount,
            sourceType = sourceType,
            counterparty = counterparty,
            bank = bank,
            refNo = refNo,
            account = account,
            rawText = trimmed,
            timestamp = resolvedTimestamp,
            balance = balance,
            creditLimit = creditLimit,
            isCard = isCard,
            isRefund = isRefund
        )
    }

    private fun detectIsRefund(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("refunded to") ||
                lower.contains("refund of") ||
                lower.contains("refund for") ||
                lower.contains("credited back") ||
                lower.contains("has been reversed") ||
                lower.contains("is reversed") ||
                lower.contains("reversal of") ||
                (lower.contains("reversal") && lower.contains("credited"))
    }

    private fun detectTransactionType(body: String): TransactionType? {
        val lower = body.lowercase()

        // 1. Reversals & Refunds are always credited to user
        if (detectIsRefund(body)) {
            return TransactionType.CREDIT
        }

        // Exclude disclaimers like "will be automatically reversed within 48 hours"
        val isActualReversal = (lower.contains("reversed") || lower.contains("reversal")) &&
                !lower.contains("will be") &&
                !lower.contains("in case") &&
                !lower.contains("if not")

        if (isActualReversal) {
            return TransactionType.CREDIT
        }

        // Check for Dr. / Dr patterns (Debit)
        val hasDrPattern = Pattern.compile(
            """(?i)(?:\bdr\.?\s+(?:from|by|for|with|of|to)\b|(?:rs\.?|inr|₹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:is\s+)?dr\.?\b|\bdr\.?\s*(?:by|for|with|of|:)?\s*(?:rs\.?|inr|₹)\b|\b(?:a/c|ac|account)\b[^\n\r]*?\b(?:is\s+)?dr\.?\b|\bdr\s*:\s*(?:rs\.?|inr|₹)?\s*[\d,]+)"""
        ).matcher(body).find()

        // Check for Cr. / Cr patterns (Credit)
        val hasCrPattern = Pattern.compile(
            """(?i)(?:\bcr\.?\s+(?:to|in|by|for|with|of)\s+(?:your\s+)?(?:a/c|ac|account)\b|(?:rs\.?|inr|₹)\s*[\d,]+(?:\.\d{1,2})?\s+(?:is\s+)?cr\.?\b|\bcr\.?\s*(?:by|for|with|of|:)?\s*(?:rs\.?|inr|₹)\b|\b(?:a/c|ac|account)\b[^\n\r]*?\b(?:is\s+)?cr\.?\b|\bcr\s*:\s*(?:rs\.?|inr|₹)?\s*[\d,]+)"""
        ).matcher(body).find()

        val isDebit = hasDrPattern ||
                lower.contains("debited") ||
                lower.contains("withdrawn") ||
                lower.contains("withdrew") ||
                lower.contains("spent") ||
                lower.contains("sent rs") ||
                lower.contains("sent inr") ||
                lower.contains("sent to") ||
                lower.contains("you sent") ||
                lower.contains("money sent") ||
                (lower.contains("paid rs") && !lower.contains("fampaid")) ||
                (lower.contains("paid inr") && !lower.contains("fampaid")) ||
                (lower.contains("paid to") && !lower.contains("fampaid")) ||
                (lower.startsWith("paid ") && !lower.contains("fampaid")) ||
                lower.contains("paying to") ||
                lower.contains("deducted") ||
                lower.contains("purchase of") ||
                lower.contains("used at") ||
                lower.contains("charged")

        val isCredit = hasCrPattern ||
                lower.contains("credited") ||
                lower.contains("deposited") ||
                lower.contains("received rs") ||
                lower.contains("received inr") ||
                lower.contains("received from") ||
                lower.contains("you received") ||
                lower.contains("money received") ||
                lower.contains("salary credited") ||
                lower.contains("cashback received") ||
                lower.contains("added to your account") ||
                lower.contains("added to a/c") ||
                lower.contains("fampaid") ||
                lower.contains("#fampaid") ||
                lower.contains("you got") ||
                lower.contains("sent you") ||
                lower.contains("sent to you") ||
                lower.contains("paid you")

        return when {
            // Ambiguity: "debited for Rs 850 ... and credited to VPA swiggy@icici" or "Dr. from A/C ... and Cr. to VPA"
            // The user's account was debited, and the merchant's VPA was credited.
            isDebit && isCredit -> {
                val userAccountDebited = lower.contains("debited from your") ||
                    lower.contains("debited from a/c") ||
                    lower.contains("debited from account") ||
                    lower.contains("debited from ac") ||
                    lower.contains("a/c is debited") ||
                    lower.contains("a/c debited") ||
                    lower.contains("is debited for") ||
                    lower.contains("debited with") ||
                    lower.contains("debited for") ||
                    lower.contains("account debited") ||
                    Pattern.compile("""(?i)\bdebited\s+from\s+(?:a/c|ac|account)\b""").matcher(body).find() ||
                    Pattern.compile("""(?i)\bdr\.?\s+from\s+(?:a/c|ac|account)\b""").matcher(body).find() ||
                    Pattern.compile("""(?i)\b(?:a/c|ac|account)\b(?:\s+(?:no\.?|number|ending\s+(?:in|with)))?\s*[:\s]*[0-9*xX.-]*\s+(?:is\s+)?dr\.?\b""").matcher(body).find()

                val userAccountCredited = lower.contains("credited to your") ||
                    lower.contains("credited to a/c") ||
                    lower.contains("credited to account") ||
                    lower.contains("credited to ac") ||
                    lower.contains("a/c is credited") ||
                    lower.contains("a/c credited") ||
                    lower.contains("credited with") ||
                    lower.contains("fampaid") ||
                    lower.contains("sent you") ||
                    Pattern.compile("""(?i)\bcr\.?\s+(?:to|in)\s+(?:your\s+)?(?:a/c|ac|account)\b""").matcher(body).find() ||
                    Pattern.compile("""(?i)\b(?:a/c|ac|account)\b(?:\s+(?:no\.?|number|ending\s+(?:in|with)))?\s*[:\s]*[0-9*xX.-]*\s+(?:is\s+)?cr\.?\b""").matcher(body).find()

                if (userAccountDebited && !userAccountCredited) {
                    TransactionType.DEBIT
                } else if (userAccountCredited && !userAccountDebited) {
                    TransactionType.CREDIT
                } else if (userAccountDebited) {
                    TransactionType.DEBIT
                } else {
                    TransactionType.DEBIT
                }
            }
            isDebit -> TransactionType.DEBIT
            isCredit -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun isNonTransactional(body: String): Boolean {
        val lower = body.lowercase()

        // 1. Declined or failed transactions
        if (lower.contains("declined") || lower.contains("transaction failed") || lower.contains("txn failed") || lower.contains("failed to transfer")) {
            if (!lower.contains("debited") && !lower.contains("credited") && !lower.contains("withdrawn")) {
                return true
            }
            if (lower.contains("declined due to") || lower.contains("declined as") || lower.contains("declined on")) {
                return true
            }
        }

        // 1b. Collect requests & payment requests from strangers/scammers
        if (lower.contains("requested") || lower.contains("request from") || lower.contains("payment request") || lower.contains("collect request")) {
            if (!lower.contains("debited") && !lower.contains("credited")) {
                return true
            }
        }

        // 2. Pure OTP or login authentication messages
        val hasOtp = lower.contains("otp") ||
                lower.contains("one time password") ||
                lower.contains("verification code") ||
                lower.contains("security code") ||
                lower.contains("login pin") ||
                lower.contains("secret code") ||
                lower.contains("do not share with anyone")

        if (hasOtp && !lower.contains("debited") && !lower.contains("credited") && !lower.contains("withdrawn")) {
            return true
        }

        // 3. Payment Request & Collect Request Messages (Not an actual completed transaction)
        val isPaymentRequest = lower.contains("has requested") ||
                lower.contains("payment request") ||
                lower.contains("collect request") ||
                lower.contains("requesting payment") ||
                lower.contains("requested money") ||
                lower.contains("request to pay") ||
                lower.contains("requests rs") ||
                lower.contains("approve payment") ||
                lower.contains("ignore if already paid")

        if (isPaymentRequest) return true

        // 4. Credit Card Bill Due & Minimum Amount Due Reminders
        val isBillReminder = (lower.contains("is due") ||
                lower.contains("bill due") ||
                lower.contains("payment due") ||
                lower.contains("min amount due") ||
                lower.contains("minimum amount due") ||
                lower.contains("total due") ||
                lower.contains("is overdue") ||
                lower.contains("in arrears") ||
                lower.contains("pls pay min") ||
                (lower.contains("pay by") && lower.contains("due"))) &&
                !lower.contains("debited") && !lower.contains("withdrawn") && !lower.contains("credited")

        if (isBillReminder) return true

        // 5. Mandate & Subscription Creation / Scheduled Debits
        val isMandateAlert = (lower.contains("e-mandate") ||
                lower.contains("upi-mandate") ||
                (lower.contains("mandate") && (lower.contains("created") || lower.contains("set for") || lower.contains("registered")))) &&
                !lower.contains("debited") && !lower.contains("withdrawn") && !lower.contains("credited")

        if (isMandateAlert) return true

        if (lower.contains("will be debited") && !lower.contains("has been debited") && !lower.contains("is debited")) {
            return true
        }

        // 6. E-Statements
        if ((lower.contains("e-statement") || lower.contains("statement of your") || lower.contains("stmt")) &&
            !lower.contains("debited") && !lower.contains("credited") && !lower.contains("withdrawn")
        ) {
            return true
        }

        // 7. Promotional / Loans / Marketing
        if (lower.contains("pre-approved loan") ||
            lower.contains("pre-approved personal loan") ||
            lower.contains("apply now") ||
            lower.contains("win cash") ||
            lower.contains("cashback offer") ||
            lower.contains("congratulations! you are eligible") ||
            lower.contains("scratch card awaits")
        ) {
            return true
        }

        // 8. Account Opening Confirmation
        if (lower.contains("we are pleased to inform that") || lower.contains("account has been opened")) {
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

        // Priority 3: Fallback check against known bank list
        val bodyWithoutVpa = body.uppercase().replace(Regex("""[A-Z0-9.\-_]+@[A-Z0-9.\-_]+"""), " ")
        for ((code, name) in KNOWN_BANKS) {
            if (bodyWithoutVpa.contains("$code BANK") || bodyWithoutVpa.contains("BANK OF $code") || bodyWithoutVpa.contains(" $code ")) {
                return name
            }
        }
        for ((code, name) in KNOWN_BANKS) {
            if (Regex("""\b${Regex.escape(code)}\b""").containsMatchIn(bodyWithoutVpa)) {
                return name
            }
        }
        return null
    }

    private fun detectSourceType(body: String): SourceType {
        val lower = body.lowercase()
        return when {
            lower.contains("atm") || lower.contains("cash withdrawal") || lower.contains("cash wdl") -> SourceType.ATM
            lower.contains("upi") || lower.contains("vpa") || lower.contains("@") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("bhim") -> SourceType.UPI
            lower.contains("credit card") || lower.contains("debit card") || lower.contains("spent") || lower.contains("card ending") || lower.contains("pos") || lower.contains("swipe") -> SourceType.CARD
            lower.contains("imps") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("netbanking") || lower.contains("inb") || lower.contains("fund transfer") -> SourceType.BANK_TRANSFER
            else -> SourceType.UNKNOWN
        }
    }

    fun extractBalance(body: String): Double? {
        val m = BALANCE_PATTERN.matcher(body)
        if (m.find()) {
            val raw = m.group(1)?.replace(",", "") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }

    fun extractCreditLimit(body: String): Double? {
        val m = CREDIT_LIMIT_PATTERN.matcher(body)
        if (m.find()) {
            val raw = m.group(1)?.replace(",", "") ?: return null
            return raw.toDoubleOrNull()
        }
        return null
    }

    private fun extractAmount(body: String, balance: Double?, creditLimit: Double?): Double? {
        // Pattern 1: Explicit currency symbol prefix (Rs. / INR / ₹)
        val p1 = Pattern.compile("""(?:Rs\.?|INR|₹)\s*([\d,]+(?:\.\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
        val m1 = p1.matcher(body)
        val candidateAmounts = mutableListOf<Double>()
        while (m1.find()) {
            val amt = parseAmount(m1.group(1))
            if (amt != null && amt > 0.0) {
                candidateAmounts.add(amt)
            }
        }

        // Return first currency amount that is NOT balance or credit limit
        for (amt in candidateAmounts) {
            if (amt != balance && amt != creditLimit) {
                return amt
            }
        }

        // Pattern 2: Verb-coupled without currency (e.g. "debited by 500.00" or "credited by 1200.00")
        val verbPattern = Pattern.compile(
            """(?i)(?:debited|credited|withdrawn|spent|paid|sent|received)\s+(?:by|for|with|of)\s*(?:Rs\.?|INR|₹)?\s*([0-9,]+(?:\.\d{1,2})?)"""
        )
        val vm = verbPattern.matcher(body)
        while (vm.find()) {
            val amt = parseAmount(vm.group(1))
            if (amt != null && amt != balance && amt != creditLimit && amt > 0.0) {
                return amt
            }
        }

        // Pattern 3: Trailing currency symbol (e.g. "500.00 Rs" or "250 INR")
        val p3 = Pattern.compile("""([\d,]+(?:\.\d{1,2})?)\s*(?:Rs\.?|INR|₹)""", Pattern.CASE_INSENSITIVE)
        val m3 = p3.matcher(body)
        while (m3.find()) {
            val amt = parseAmount(m3.group(1))
            if (amt != null && amt != balance && amt != creditLimit && amt > 0.0) {
                return amt
            }
        }

        if (candidateAmounts.isNotEmpty()) {
            return candidateAmounts.first()
        }

        return null
    }

    fun extractRefNo(body: String): String? {
        val pattern = Pattern.compile(
            """\b(?:UPI\s+Ref(?:\s*no\.?)?|UPI\s*[:\/]|Ref(?:\s*no\.?|\.)?|UTR|Txn\s*ID|Transaction\s*ID|RRN)\b\s*[:#.\/]?\s*([0-9a-zA-Z]{6,25})""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(body)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Extracts explicit transaction date/time from SMS text for immediate and delayed-delivery alerts.
     * Prevents carrier delivery delays from distorting transaction dates or the 5-minute dedup window.
     */
    fun extractTimestamp(body: String, fallback: Long = System.currentTimeMillis()): Long {
        // Pattern 1: e.g. 01-09-2026 19:07:46 or 14-09-26 18:22:10 or 15-09-2026 or 12/09/2026 or 18-09-26
        val numDatePattern = Pattern.compile(
            """\b(\d{1,2})[-/](\d{1,2})[-/](\d{2,4})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?\b"""
        )
        val nm = numDatePattern.matcher(body)
        if (nm.find()) {
            try {
                val day = nm.group(1)!!.toInt()
                val month = nm.group(2)!!.toInt()
                var year = nm.group(3)!!.toInt()
                if (year < 100) year += 2000
                val hour = nm.group(4)?.toIntOrNull() ?: 12
                val min = nm.group(5)?.toIntOrNull() ?: 0
                val sec = nm.group(6)?.toIntOrNull() ?: 0

                if (day in 1..31 && month in 1..12 && year in 2020..2035) {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(year, month - 1, day, hour, min, sec)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        // Pattern 1b: Year-first e.g. 2026:10:10 02:44:01 or 2026-10-10 02:44:01 or 2026/10/10
        val yearFirstPattern = Pattern.compile(
            """\b(\d{4})[:\-/](\d{1,2})[:\-/](\d{1,2})(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?\b"""
        )
        val ym = yearFirstPattern.matcher(body)
        if (ym.find()) {
            try {
                val year = ym.group(1)!!.toInt()
                val month = ym.group(2)!!.toInt()
                val day = ym.group(3)!!.toInt()
                val hour = ym.group(4)?.toIntOrNull() ?: 12
                val min = ym.group(5)?.toIntOrNull() ?: 0
                val sec = ym.group(6)?.toIntOrNull() ?: 0

                if (day in 1..31 && month in 1..12 && year in 2020..2035) {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(year, month - 1, day, hour, min, sec)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        // Pattern 2: e.g. 15Sep26, 15-Sep-2026, 12-Sep-26, 15 Sep 2026
        val alphaDatePattern = Pattern.compile(
            """\b(\d{1,2})[-/\s]?(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[-/\s]?(\d{2,4})?(?:\s+(\d{1,2}):(\d{2})(?::(\d{2}))?)?\b""",
            Pattern.CASE_INSENSITIVE
        )
        val am = alphaDatePattern.matcher(body)
        if (am.find()) {
            try {
                val day = am.group(1)!!.toInt()
                val monthStr = am.group(2)!!.lowercase()
                val month = when (monthStr) {
                    "jan" -> 1
                    "feb" -> 2
                    "mar" -> 3
                    "apr" -> 4
                    "may" -> 5
                    "jun" -> 6
                    "jul" -> 7
                    "aug" -> 8
                    "sep" -> 9
                    "oct" -> 10
                    "nov" -> 11
                    "dec" -> 12
                    else -> 0
                }
                var year = am.group(3)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                if (year < 100) year += 2000
                val hour = am.group(4)?.toIntOrNull() ?: 12
                val min = am.group(5)?.toIntOrNull() ?: 0
                val sec = am.group(6)?.toIntOrNull() ?: 0

                if (day in 1..31 && month in 1..12 && year in 2020..2035) {
                    val cal = java.util.Calendar.getInstance()
                    cal.set(year, month - 1, day, hour, min, sec)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }

        return fallback
    }

    fun extractAccount(body: String): String? {
        val pattern = ACCOUNT_PATTERN.matcher(body)
        if (pattern.find()) {
            val digits = pattern.group(1)?.trim() ?: return null
            val cleanDigits = digits.filter { it.isDigit() }
            return if (cleanDigits.length >= 4) "••" + cleanDigits.takeLast(4) else "••$cleanDigits"
        }
        return null
    }

    private fun extractCounterparty(body: String, sourceType: SourceType): String? {
        // ATM Transactions
        if (sourceType == SourceType.ATM) {
            val tidPattern = Pattern.compile("""at\s+ATM\s+(?:TID\s+)?([A-Za-z0-9]+)""", Pattern.CASE_INSENSITIVE)
            val tidMatcher = tidPattern.matcher(body)
            if (tidMatcher.find()) {
                return "ATM TID ${tidMatcher.group(1)}"
            }
            val locPattern = Pattern.compile("""at\s+ATM\s+([A-Za-z0-9\s]+?)(?:\s+Ref|\s+Avl|\s*\.|$)""", Pattern.CASE_INSENSITIVE)
            val locMatcher = locPattern.matcher(body)
            if (locMatcher.find()) {
                val loc = locMatcher.group(1)?.trim() ?: ""
                return if (loc.isNotEmpty()) "ATM $loc" else "ATM"
            }
            return "ATM"
        }

        // Strategy 1: VPA with bracketed Display Name -> e.g. "VPA swiggy@icici (Bundl Technologies)"
        // or check if bracketed text is a reference number like (UPI Ref no 123456789012)
        val vpaDisplayPattern = Pattern.compile(
            """VPA\s+([^@\s]+@[^\s]+)\s*\(([^)]+)\)""",
            Pattern.CASE_INSENSITIVE
        )
        val vpaDisplayMatcher = vpaDisplayPattern.matcher(body)
        if (vpaDisplayMatcher.find()) {
            val vpa = vpaDisplayMatcher.group(1)
            val insideParens = vpaDisplayMatcher.group(2)?.trim()
            val lowerParens = insideParens?.lowercase() ?: ""
            val isReferenceInParens = lowerParens.contains("ref") ||
                    lowerParens.contains("upi") ||
                    lowerParens.contains("rrn") ||
                    lowerParens.contains("txn") ||
                    (insideParens != null && insideParens.count { it.isDigit() } >= 6)

            if (!isReferenceInParens && !insideParens.isNullOrBlank()) {
                val rawName = cleanCounterparty(insideParens)
                if (isValidCounterparty(rawName)) {
                    return rawName
                }
            } else if (!vpa.isNullOrBlank()) {
                return cleanCounterparty(vpa)
            }
        }

        val delimiter = """(?=\s+(?:on|via|ref|refno|upi|using|from|order|purchase|\()|\s*[\.,;]|$)"""

        val patterns = listOf(
            // "Cr. to 9307704640-2.wallet@phonepe" or "Cr. to vishalkolhekar09-1@okaxis" or "Dr. to merchant@upi"
            Pattern.compile("""(?:\bCr\.?\s+to|\bDr\.?\s+to)\s+(?!A/C|A\/c|Account|ac\b)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "Dr. from sender@upi" or "Cr. from friend@okaxis" (excluding account masks like A/C)
            Pattern.compile("""(?:\bCr\.?\s+from|\bDr\.?\s+from)\s+(?!A/C|A\/c|Account|ac\b)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "Info: UPI/Apollo Pharmacy" or "Info: Dominos" or "Info: SALARY-Google"
            Pattern.compile("""(?:Info:\s*UPI\/|Info:\s*)([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "by transfer to Flipkart" or "transfer to Uber"
            Pattern.compile("""(?:by\s+transfer\s+to|transfer\s+to)\s+([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "to swiggy@icici on 12-09-26" or "to Rohit Kumar Refno..." or "towards Amazon Pay..."
            Pattern.compile("""(?:trf\s+to|towards\s+|paid\s+to|to\s+(?:VPA\s+)?|to\s+)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "Transferred to Uber"
            Pattern.compile("""(?:Transferred\s+to\s+)([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "refund for Swiggy order..." or "for D-Mart purchase..."
            Pattern.compile("""(?:refund\s+for|for\s+)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "by transfer from Ramesh" or "transfer from John Doe (UPI..." or "from friend@upi" (excluding user account)
            Pattern.compile("""(?:by\s+transfer\s+from|transfer\s+from|from\s+)(?!A/C|A\/c|Account|ac\b)([A-Za-z0-9.\-_@]+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "by Vikas" or "by Neha" (excluding amounts like "by Rs" / "by INR")
            Pattern.compile("""(?:by\s+(?!Rs|INR|₹|\d))([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE),
            // "at STARBUCKS on 14-09-26"
            Pattern.compile("""at\s+([A-Za-z0-9.\-_&']+(?:\s+[A-Za-z0-9.\-_&']+)*?)$delimiter""", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(body)
            if (matcher.find()) {
                val candidate = cleanCounterparty(matcher.group(1))
                if (isValidCounterparty(candidate)) {
                    return candidate
                }
            }
        }

        return null
    }

    private fun isValidCounterparty(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = name.lowercase().trim()
        val junkKeywords = setOf("atm", "upi", "inr", "rs", "bank", "account", "a/c", "card", "transaction", "payment", "customer", "dear")
        if (junkKeywords.contains(lower)) return false
        if (lower.startsWith("rs") || lower.startsWith("inr") || lower.startsWith("₹")) return false
        if (name.all { it.isDigit() || it == '-' || it == '.' }) return false
        return true
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
        val removePrefixes = listOf(
            "Cr. to ", "Cr to ", "Dr. to ", "Dr to ", "Cr. from ", "Cr from ", "Dr. from ", "Dr from ",
            "to ", "from ", "VPA ", "vpa ", "by transfer to ", "transfer to ", "by transfer from ", "transfer from ", "by ",
            "through UPI from ", "through UPI to ", "UPI/", "UPI-", "POS/", "ECOM/", "INB/", "NEFT/", "IMPS/"
        )
        for (prefix in removePrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }

        // If string contains slash (e.g. "Zomato/zomato@icici"), pick the cleaner merchant name
        if (cleaned.contains("/")) {
            val segments = cleaned.split("/")
            val validSeg = segments.firstOrNull { isValidCounterparty(it) && !it.contains("@") }
            if (validSeg != null) {
                cleaned = validSeg.trim()
            }
        }

        // Strip corporate legal suffixes like "Pvt Ltd", "Private Limited", "LLP", "Ltd"
        val corporateSuffixes = listOf(" Pvt Ltd", " Private Limited", " Pvt. Ltd.", " LLP", " Ltd.", " Ltd")
        for (suffix in corporateSuffixes) {
            if (cleaned.endsWith(suffix, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
            }
        }

        // Strip trailing punctuation or brackets
        cleaned = cleaned.trimEnd('.', ',', '(', ')', ';', ':')

        return cleaned.ifEmpty { null }
    }
}
