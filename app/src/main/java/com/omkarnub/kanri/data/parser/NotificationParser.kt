package com.omkarnub.kanri.data.parser

import java.util.regex.Pattern

object NotificationParser {

    const val PKG_GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PKG_PHONEPE = "com.phonepe.app"
    const val PKG_PAYTM = "net.one97.paytm"
    const val PKG_AMAZON_IN = "in.amazon.mShop.android.shopping"
    const val PKG_AMAZON_COM = "com.amazon.mShop.android.shopping"
    const val PKG_FAMPAY = "com.fampay.in"
    const val PKG_BHIM = "in.org.npci.upiapp"
    const val PKG_CRED = "com.dreamplug.androidapp"

    val SUPPORTED_PACKAGES = setOf(
        PKG_GPAY,
        PKG_PHONEPE,
        PKG_PAYTM,
        PKG_AMAZON_IN,
        PKG_AMAZON_COM,
        PKG_FAMPAY,
        PKG_BHIM,
        PKG_CRED
    )

    fun getAppNameForPackage(packageName: String): String {
        return when (packageName) {
            PKG_GPAY -> "Google Pay"
            PKG_PHONEPE -> "PhonePe"
            PKG_PAYTM -> "Paytm"
            PKG_AMAZON_IN, PKG_AMAZON_COM -> "Amazon Pay"
            PKG_FAMPAY -> "FamPay"
            PKG_BHIM -> "BHIM UPI"
            PKG_CRED -> "CRED"
            else -> "UPI"
        }
    }

    private val NON_TRANSACTIONAL_KEYWORDS = listOf(
        "otp", "verification code", "security code",
        "due in", "payment due", "bill due", "reminder",
        "win up to", "earn up to", "cashback up to", "chance to win",
        "flat 50%", "discount", "voucher", "offer valid", "limited period",
        "recharge now", "apply now", "spin and win", "scratch card awaits",
        "rate your", "complete your profile", "check your credit score"
    )

    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:(?:₹|Rs\.?|INR)\s*([\d,]+(?:\.\d{1,2})?)|([\d,]+(?:\.\d{1,2})?)\s*(?:₹|Rs\.?|INR))""",
        Pattern.CASE_INSENSITIVE
    )

    private val REF_PATTERN = Pattern.compile(
        """(?:Ref|UPI Ref|UTR|Txn ID|Transaction ID)[:\s#-]*([A-Za-z0-9]{6,22})""",
        Pattern.CASE_INSENSITIVE
    )

    private val SENDER_PATTERNS = listOf(
        // "Rahul Sharma sent you ₹500" or "Rahul Sharma paid you ₹500"
        Pattern.compile("""^([A-Za-z0-9 .'-]+?)\s+(?:sent|paid)\s+you""", Pattern.CASE_INSENSITIVE),
        // "You received ₹500 from Rahul Sharma" or "Payment of ₹300 received from Suresh"
        Pattern.compile("""(?:received|credited|payment of).*?from\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|to|in|at)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE),
        // General "from Rahul Sharma via UPI" or "from Rahul Sharma"
        Pattern.compile("""\bfrom\s+([A-Za-z0-9 .'-]+?)(?:\s+(?:via|on|ref|using|with|to|in|at)\b|[.,!]|$)""", Pattern.CASE_INSENSITIVE)
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

        // 1. Filter out promotional & OTP notices
        if (NON_TRANSACTIONAL_KEYWORDS.any { lower.contains(it) }) {
            return null
        }

        // 2. Must indicate received money / credit
        val isReceived = lower.contains("received") ||
            lower.contains("credited") ||
            lower.contains("sent you") ||
            lower.contains("paid you") ||
            lower.contains("added to your") ||
            lower.contains("cashback received") ||
            lower.contains("cashback of") ||
            lower.contains("you won")

        if (!isReceived) return null

        // 3. Extract Amount
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.0) return null

        // 4. Extract Counterparty / Sender
        val counterparty = extractCounterparty(safeTitle, safeText, lower)

        // 5. Extract Reference number if present
        val refNo = extractRefNo(combined)

        val appName = getAppNameForPackage(packageName)

        return ParsedTransaction(
            type = TransactionType.CREDIT,
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

    private fun extractCounterparty(title: String, text: String, lower: String): String {
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
                    return cleanSenderName(candidate)
                }
            }
        }

        for (pattern in SENDER_PATTERNS) {
            val matcher = pattern.matcher(title)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && isValidName(candidate)) {
                    return cleanSenderName(candidate)
                }
            }
        }

        // If title itself is a person's name (common in Google Pay / WhatsApp Pay)
        if (title.isNotBlank() && !title.equals("Payment received", ignoreCase = true) &&
            !title.equals("Money Received", ignoreCase = true) &&
            !title.equals("Google Pay", ignoreCase = true) &&
            !title.equals("PhonePe", ignoreCase = true) &&
            !title.equals("Paytm", ignoreCase = true) &&
            isValidName(title)
        ) {
            return cleanSenderName(title)
        }

        return "UPI Payment"
    }

    private fun isValidName(name: String): Boolean {
        val lower = name.lowercase().trim()
        if (lower.length < 2 || lower.length > 50) return false
        if (lower.contains("account") || lower.contains("balance") || lower.contains("bank") || lower.contains("payment")) {
            return false
        }
        return true
    }

    private fun cleanSenderName(name: String): String {
        return name
            .replace(Regex("""^from\s+""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+via.*$""", RegexOption.IGNORE_CASE), "")
            .trim()
            .trimEnd('.', ',', '!', '-')
    }

    private fun extractRefNo(text: String): String? {
        val matcher = REF_PATTERN.matcher(text)
        return if (matcher.find()) matcher.group(1)?.trim() else null
    }
}
