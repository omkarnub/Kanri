package com.omkarnub.kanri.data.parser

import java.util.regex.Pattern

object NotificationParser {

    // UPI & Payment Apps
    const val PKG_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PKG_PHONEPE = "com.phonepe.app"
    const val PKG_PAYTM = "net.one97.paytm"
    const val PKG_FAMPAY = "com.fampay.in"
    const val PKG_BHIM = "in.org.npci.upiapp"
    const val PKG_AMAZON_IN = "in.amazon.mShop.android.shopping"
    const val PKG_AMAZON_GLOBAL = "com.amazon.mShop.android.shopping"
    const val PKG_CRED = "com.dreamplug.androidapp"
    const val PKG_NAVI = "com.naviapp"
    const val PKG_JUPITER = "money.jupiter"
    const val PKG_FIBE = "co.epifi.gro"

    // SMS Apps (Used to capture bank SMS notifications directly via NotificationListener)
    const val PKG_GOOGLE_MESSAGES = "com.google.android.apps.messaging"
    const val PKG_SAMSUNG_MESSAGING = "com.samsung.android.messaging"
    const val PKG_ONEPLUS_MMS = "com.oneplus.mms"
    const val PKG_COLOROS_MMS = "com.coloros.mms"
    const val PKG_HEYTAP_MMS = "com.heytap.mms"
    const val PKG_XIAOMI_MMS = "com.android.mms"
    const val PKG_SMS_ORGANIZER = "com.microsoft.android.smsorganizer"
    const val PKG_TRUECALLER = "com.truecaller"

    // Major Indian Bank Apps
    const val PKG_HDFC = "com.snapwork.hdfc"
    const val PKG_ICICI = "com.csam.icici.bank.imobile"
    const val PKG_SBI_YONO = "com.sbi.lotusintouch"
    const val PKG_SBI_LITE = "com.sbi.yonolite"
    const val PKG_AXIS = "com.axis.mobile"
    const val PKG_KOTAK = "com.msf.kbank.mobile"
    const val PKG_PNB = "com.pnb.pnbone"
    const val PKG_BOB = "com.bob.bobworld"
    const val PKG_IDFC = "com.idfcfirstbank.optimus"
    const val PKG_INDUSIND = "com.indusind.mobile"

    /**
     * Supplementary packages for incoming money and reward alerts only.
     * Core Principle: SMS is the only authoritative source for debits.
     * NotificationListenerService is strictly scoped to these packages for incoming credits/rewards.
     */
    val SUPPORTED_RECEIVE_PACKAGES = setOf(
        PKG_GPAY,
        PKG_AMAZON_IN,
        PKG_AMAZON_GLOBAL,
        PKG_PAYTM,
        PKG_FAMPAY,
        PKG_PHONEPE,
        PKG_BHIM
    )

    fun isReceiveAppSupported(packageName: String): Boolean = SUPPORTED_RECEIVE_PACKAGES.contains(packageName)

    val SUPPORTED_SMS_PACKAGES = setOf(
        PKG_GOOGLE_MESSAGES,
        PKG_SAMSUNG_MESSAGING,
        PKG_ONEPLUS_MMS,
        PKG_COLOROS_MMS,
        PKG_HEYTAP_MMS,
        PKG_XIAOMI_MMS,
        PKG_SMS_ORGANIZER,
        PKG_TRUECALLER
    )

    val SUPPORTED_PAYMENT_PACKAGES = setOf(
        PKG_GPAY,
        PKG_PHONEPE,
        PKG_PAYTM,
        PKG_FAMPAY,
        PKG_BHIM,
        PKG_AMAZON_IN,
        PKG_AMAZON_GLOBAL,
        PKG_CRED,
        PKG_NAVI,
        PKG_JUPITER,
        PKG_FIBE,
        PKG_HDFC,
        PKG_ICICI,
        PKG_SBI_YONO,
        PKG_SBI_LITE,
        PKG_AXIS,
        PKG_KOTAK,
        PKG_PNB,
        PKG_BOB,
        PKG_IDFC,
        PKG_INDUSIND
    )

    val SUPPORTED_PACKAGES = SUPPORTED_PAYMENT_PACKAGES + SUPPORTED_SMS_PACKAGES

    fun isSmsApp(packageName: String): Boolean = SUPPORTED_SMS_PACKAGES.contains(packageName)

    fun isPackageSupported(packageName: String): Boolean = SUPPORTED_PACKAGES.contains(packageName)

    fun getAppNameForPackage(packageName: String): String {
        return when (packageName) {
            PKG_GPAY -> "Google Pay"
            PKG_PHONEPE -> "PhonePe"
            PKG_PAYTM -> "Paytm"
            PKG_FAMPAY -> "FamPay"
            PKG_BHIM -> "BHIM UPI"
            PKG_AMAZON_IN, PKG_AMAZON_GLOBAL -> "Amazon Pay"
            PKG_CRED -> "CRED"
            PKG_NAVI -> "Navi"
            PKG_JUPITER -> "Jupiter"
            PKG_FIBE -> "Fi Money"
            PKG_HDFC -> "HDFC Bank"
            PKG_ICICI -> "ICICI Bank"
            PKG_SBI_YONO, PKG_SBI_LITE -> "State Bank of India"
            PKG_AXIS -> "Axis Bank"
            PKG_KOTAK -> "Kotak Mahindra Bank"
            PKG_PNB -> "Punjab National Bank"
            PKG_BOB -> "Bank of Baroda"
            PKG_IDFC -> "IDFC First Bank"
            PKG_INDUSIND -> "IndusInd Bank"
            else -> "Payment"
        }
    }

    private val NON_TRANSACTIONAL_KEYWORDS = listOf(
        "otp", "verification code", "security code",
        "due in", "payment due", "bill due", "reminder", "bill generated",
        "win up to", "earn up to", "cashback up to", "chance to win",
        "flat 50%", "discount", "voucher", "offer valid", "limited period",
        "recharge now", "apply now", "spin and win", "scratch card awaits",
        "rate your", "complete your profile", "check your credit score",
        // Failed / Declined / Cancelled filters (Fix for Flaw 1)
        "failed", "declined", "unsuccessful", "cancelled", "canceled",
        "payment failed", "txn failed", "transaction failed",
        "could not be", "couldn't be", "unable to process", "payment unsuccessful",
        // Collect request / Scams filters (Fix for Flaw 2)
        "requested", "request from", "collect request", "has requested",
        "payment request", "mandate", "autopay scheduled",
        // Shipping / E-commerce updates (Fix for Flaw 3)
        "order shipped", "delivered", "out for delivery", "in transit", "dispatched"
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:₹|Rs\.?|INR))""",
        Pattern.CASE_INSENSITIVE
    )

    private val REF_PATTERN = Pattern.compile(
        """(?:Ref|UPI Ref|UTR|Txn ID|Transaction ID)[:\s#-]*([A-Za-z0-9]{6,22})""",
        Pattern.CASE_INSENSITIVE
    )

    // Patterns for Inbound / Credit payments
    private val SENDER_PATTERNS = listOf(
        // FamPay "YOU GOT #FAMPAID XYZ SENT YOU ₹1" or "#FAMPAID XYZ sent you"
        Pattern.compile("""(?:#fampaid|you got)\s+([A-Za-z0-9 .'-]+?)\s+(?:sent|paid)\s+(?:you|to you)""", Pattern.CASE_INSENSITIVE),
        // "Rahul Sharma sent you ₹500" or "Rahul Sharma paid you ₹500"
        Pattern.compile("""^([A-Za-z0-9 .'-]+?)\s+(?:sent|paid)\s+you""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""\b([A-Za-z0-9 .'-]+?)\s+(?:sent|paid)\s+(?:you|to you)""", Pattern.CASE_INSENSITIVE),
        // "You received ₹500 from Rahul Sharma" or "Payment of ₹300 received from Suresh"
        Pattern.compile("""(?:received|credited|payment of).*?from\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|to|in|at)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE),
        // General "from Rahul Sharma via UPI" or "from Rahul Sharma"
        Pattern.compile("""\bfrom\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|to|in|at)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE)
    )

    // Patterns for Outbound / Debit payments
    private val RECIPIENT_PATTERNS = listOf(
        // "Paid ₹500 to Starbucks" / "Payment of ₹200 to Chai Point" / "Sent ₹1000 to John"
        Pattern.compile("""(?:paid|sent|payment(?:\s+of)?|transfer(?:red)?(?:\s+of)?)\s*(?:(?:₹|rs\.?|inr)\s*[\d,.]+\s+)?(?:to|at)\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|for|at|successful)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE),
        // "Paid to Starbucks ₹500" / "Sent to John ₹100"
        Pattern.compile("""(?:paid|sent)\s+to\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:₹|rs\.?|inr)|\s+(?:via|on|ref)|$)""", Pattern.CASE_INSENSITIVE),
        // "Paid at Starbucks ₹500"
        Pattern.compile("""paid\s+at\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:₹|rs\.?|inr)|\s+(?:via|on|ref)|$)""", Pattern.CASE_INSENSITIVE),
        // "You paid ₹150 to Swiggy"
        Pattern.compile("""you paid\s+(?:(?:₹|rs\.?|inr)\s*[\d,.]+\s+)?(?:to|at)\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|for|successful)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE),
        // "Payment to Swiggy successful"
        Pattern.compile("""payment to\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:is|was)?\s+successful|\s+via|\s+using|[.,!]|$)""", Pattern.CASE_INSENSITIVE),
        // "₹500 paid to Starbucks"
        Pattern.compile("""(?:₹|rs\.?|inr)\s*[\d,.]+\s+paid to\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|for|successful)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE)
    )

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        subText: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedTransaction? {
        val safeTitle = title?.trim() ?: ""
        val safeText = text?.trim() ?: ""
        val safeSub = subText?.trim() ?: ""
        val combined = "$safeTitle $safeText $safeSub".trim()

        if (combined.isEmpty()) return null

        val lower = combined.lowercase()

        // 1. Filter out promotional & OTP & Failed & Request notices
        if (NON_TRANSACTIONAL_KEYWORDS.any { lower.contains(it) }) {
            return null
        }

        // 2. Detect Debit vs Credit
        val isCreditExplicit = lower.contains("received") ||
            lower.contains("credited") ||
            lower.contains("sent you") ||
            lower.contains("sent to you") ||
            lower.contains("paid you") ||
            lower.contains("added to your") ||
            lower.contains("cashback") ||
            lower.contains("you won") ||
            lower.contains("reward") ||
            lower.contains("refund") ||
            lower.contains("fampaid") ||
            lower.contains("#fampaid") ||
            lower.contains("you got") ||
            lower.contains("transferred to you")

        val hasPaid = lower.contains("paid") && !lower.contains("fampaid")
        val isDebitExplicit = lower.contains("paid to") ||
            lower.contains("paid at") ||
            lower.contains("you paid") ||
            lower.contains("debited") ||
            lower.contains("sent to") ||
            lower.contains("payment to") ||
            lower.contains("transferred to") ||
            lower.contains("transfer to") ||
            lower.contains("spent") ||
            lower.contains("paid successfully") ||
            lower.contains("successful payment") ||
            (hasPaid && !lower.contains("paid you")) ||
            (lower.contains("sent") && !lower.contains("sent you") && !lower.contains("sent to you"))

        val transactionType: TransactionType = when {
            isCreditExplicit && !isDebitExplicit -> TransactionType.CREDIT
            isDebitExplicit && !isCreditExplicit -> TransactionType.DEBIT
            isCreditExplicit && isDebitExplicit -> {
                // Priority check for inbound money indicators (e.g. FamPay, received, sent you)
                if (lower.contains("received") || lower.contains("credited") || lower.contains("sent you") || lower.contains("sent to you") || lower.contains("paid you") || lower.contains("refund") || lower.contains("cashback") || lower.contains("fampaid") || lower.contains("you got")) {
                    TransactionType.CREDIT
                } else {
                    TransactionType.DEBIT
                }
            }
            else -> return null
        }

        // 3. Extract Amount
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.0) return null

        // 4. Extract Counterparty / Sender / Recipient
        val counterparty = if (transactionType == TransactionType.CREDIT) {
            extractCreditCounterparty(safeTitle, safeText, lower)
        } else {
            extractDebitCounterparty(safeTitle, safeText, lower)
        }

        // 5. Extract Reference number if present
        val refNo = extractRefNo(combined)

        val appName = getAppNameForPackage(packageName)

        return ParsedTransaction(
            type = transactionType,
            amount = amount,
            sourceType = SourceType.UPI,
            counterparty = counterparty,
            bank = appName,
            refNo = refNo,
            account = null,
            rawText = combined,
            timestamp = timestamp
        )
    }

    private fun extractAmount(text: String): Double? {
        val matcher = AMOUNT_PATTERN.matcher(text)
        if (matcher.find()) {
            val amountStr = matcher.group(1) ?: matcher.group(2) ?: return null
            val clean = amountStr.replace(",", "").trim()
            return clean.toDoubleOrNull()
        }
        return null
    }

    private fun extractCreditCounterparty(title: String, text: String, lower: String): String {
        // Check for cashback / rewards first
        if (lower.contains("cashback") || lower.contains("you won") || lower.contains("reward")) {
            return "Cashback & Rewards"
        }

        // Try sender regexes on text then title
        for (pattern in SENDER_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidName(candidate)) {
                    return cleanPartyName(candidate)
                }
            }
        }

        for (pattern in SENDER_PATTERNS) {
            val matcher = pattern.matcher(title)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidName(candidate)) {
                    return cleanPartyName(candidate)
                }
            }
        }

        // If title itself is a person's name (common in Google Pay / WhatsApp Pay)
        if (title.isNotBlank() && !isGenericTitle(title) && isValidName(title)) {
            return cleanPartyName(title)
        }

        return "UPI Credit"
    }

    private fun extractDebitCounterparty(title: String, text: String, lower: String): String {
        // Try recipient regexes on text first
        for (pattern in RECIPIENT_PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidName(candidate)) {
                    return cleanPartyName(candidate)
                }
            }
        }

        for (pattern in RECIPIENT_PATTERNS) {
            val matcher = pattern.matcher(title)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidName(candidate)) {
                    return cleanPartyName(candidate)
                }
            }
        }

        // If title itself is the merchant name (e.g. "Swiggy", "Zomato", "Starbucks")
        if (title.isNotBlank() && !isGenericTitle(title) && isValidName(title)) {
            return cleanPartyName(title)
        }

        return "UPI Payment"
    }

    private fun isGenericTitle(title: String): Boolean {
        val lower = title.lowercase().trim()
        val genericWords = listOf(
            "payment received", "money received", "google pay", "phonepe", "paytm",
            "bhim", "cred", "payment successful", "transaction successful",
            "paid successfully", "payment done", "money sent", "upi payment"
        )
        return genericWords.any { lower == it || lower.contains(it) }
    }

    private fun isValidName(name: String): Boolean {
        val lower = name.lowercase().trim()
        if (lower.length < 2 || lower.length > 50) return false
        if (lower.contains("account") || lower.contains("balance") || lower.contains("bank") || lower.contains("payment")) {
            return false
        }
        return true
    }

    private fun cleanPartyName(name: String): String {
        return name
            .replace(Regex("""^(?:from|to|at)\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+via.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+using.*$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .trimEnd('.', ',', '!', '-')
    }

    private fun extractRefNo(text: String): String? {
        val matcher = REF_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }
}
