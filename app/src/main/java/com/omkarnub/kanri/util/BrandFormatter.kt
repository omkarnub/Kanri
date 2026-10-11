package com.omkarnub.kanri.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.Payments
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

data class PolishedBrandTag(
    val title: String,
    val icon: ImageVector,
    val badgeTint: Color,
    val isTransfer: Boolean = false
)

object BrandFormatter {

    private val KNOWN_ACRONYMS = setOf(
        "KFC", "PVR", "IRCTC", "BMS", "CRED", "ATM", "UPI", "SBI", "HDFC", "ICICI",
        "PNB", "BOB", "IDFC", "VPA", "TID", "POS", "INR", "OTP"
    )

    private val KNOWN_BRANDS_VPA = mapOf(
        "swiggy" to "Swiggy",
        "zomato" to "Zomato",
        "uber" to "Uber",
        "ola" to "Ola",
        "blinkit" to "Blinkit",
        "zepto" to "Zepto",
        "flipkart" to "Flipkart",
        "amazon" to "Amazon",
        "amazonpay" to "Amazon Pay",
        "amzn" to "Amazon",
        "phonepe" to "PhonePe",
        "paytm" to "Paytm",
        "gpay" to "Google Pay",
        "cred" to "CRED",
        "tataneu" to "Tata Neu",
        "netflix" to "Netflix",
        "spotify" to "Spotify",
        "starbucks" to "Starbucks",
        "mcdonalds" to "McDonald's",
        "dominos" to "Domino's",
        "myntra" to "Myntra",
        "nykaa" to "Nykaa",
        "jiomart" to "JioMart",
        "makemytrip" to "MakeMyTrip",
        "bookmyshow" to "BookMyShow",
        "irctc" to "IRCTC"
    )

    /**
     * Cleans raw merchant / counterparty strings extracted from SMS, notifications, or DB:
     * - Strips balance strings: "Starbucks. Avl Bal Rs 24" -> "Starbucks"
     * - Strips corporate suffixes: "Zomato Media Pvt Ltd" -> "Zomato Media"
     * - Formats VPAs nicely: "swiggy@icici" -> "Swiggy"
     * - Formats all-caps: "STARBUCKS" -> "Starbucks"
     * - Preserves ATM identifiers cleanly: "ATM TID ZMN8020" -> "ATM TID ZMN8020"
     */
    fun cleanMerchantName(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var cleaned = raw.trim()

        // 1. Remove leading prefixes if present
        val removePrefixes = listOf(
            "Cr. to ", "Cr to ", "Dr. to ", "Dr to ", "Cr. from ", "Cr from ", "Dr. from ", "Dr from ",
            "Paid to ", "paid to ", "Trf to ", "trf to ", "Transfer to ", "transfer to ",
            "to VPA ", "to vpa ", "to ", "from ", "VPA ", "vpa ", "Info: ", "info: ", "at ", "At "
        )
        for (prefix in removePrefixes) {
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        }

        // 2. ATM transactions: keep clean TID
        if (cleaned.startsWith("ATM", ignoreCase = true)) {
            return cleaned.trimEnd('.', ',', ';', ':')
        }

        // 3. Cut off trailing sentence after a period (". Avl Bal...", ". Your balance is...", etc.)
        if (cleaned.contains(". ")) {
            val parts = cleaned.split(Regex("""\.\s+"""))
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val candidate = parts[0].trim()
                if (candidate.length >= 2 && !isGenericKeyword(candidate)) {
                    cleaned = candidate
                }
            }
        }

        // 4. Strip trailing balance, account, reference, and date noise
        val noisePattern = Regex(
            """(?i)(?:[.,\s]+|^)(?:avl(?:[\s.]*bal(?:ance)?)?|available\s+bal(?:ance)?|avail\s+bal|clear\s+bal|total\s+bal|a\/c\s+bal|bal(?:ance)?[\s:]*(?:rs|inr|₹|\d)|limit|lmt|a\/c\b|acct\b|ref(?:[\s.]*no)?|rrn\b|utr\b|txn(?:\s*id)?|upi\s+ref|on\s+\d{1,2}[-/]|at\s+\d{1,2}:\d{2})\b.*$"""
        )
        cleaned = cleaned.replace(noisePattern, "").trim()

        // 5. If it's a VPA (contains @), extract and polish brand name
        if (cleaned.contains("@")) {
            val vpaHandle = cleaned.substringBefore("@").trim()
            val lowerHandle = vpaHandle.lowercase(Locale.ROOT)

            // Check if handle matches a known brand or prefix (e.g. "swiggy-order", "zomato.pay")
            var matchedBrand: String? = null
            for ((key, name) in KNOWN_BRANDS_VPA) {
                if (lowerHandle == key || lowerHandle.startsWith("$key.") || lowerHandle.startsWith("$key-") || lowerHandle.startsWith("${key}_")) {
                    matchedBrand = name
                    break
                }
            }

            if (matchedBrand != null) {
                return matchedBrand
            }

            // If handle has numbers or order IDs after hyphen/dot, strip them
            val cleanHandle = vpaHandle.split('.', '-', '_').firstOrNull { it.isNotBlank() } ?: vpaHandle
            if (cleanHandle.length >= 3 && !cleanHandle.all { it.isDigit() }) {
                cleaned = cleanHandle
            }
        }

        // 6. Strip corporate legal suffixes
        val corporateSuffixes = listOf(
            " Pvt Ltd", " Private Limited", " Pvt. Ltd.", " LLP", " Ltd.", " Ltd", " Inc.", " Inc", " Corp"
        )
        for (suffix in corporateSuffixes) {
            if (cleaned.endsWith(suffix, ignoreCase = true)) {
                cleaned = cleaned.substring(0, cleaned.length - suffix.length).trim()
            }
        }

        // 7. Strip trailing punctuation
        cleaned = cleaned.trimEnd('.', ',', '(', ')', ';', ':', '-', '/')

        // 8. Capitalization polish: if all UPPERCASE and not a short acronym, title-case words
        if (cleaned.length > 3 && cleaned.all { !it.isLetter() || it.isUpperCase() }) {
            cleaned = titleCase(cleaned)
        }

        return cleaned
    }

    /**
     * Formats raw bank and source types into a high-end brand tag for the UI.
     */
    fun formatBrandTag(
        bank: String?,
        sourceType: String?,
        isWalletConfigured: Boolean = true,
        isAtmTransfer: Boolean = false
    ): PolishedBrandTag {
        if (isAtmTransfer) {
            return PolishedBrandTag(
                title = "ATM → Cash Wallet",
                icon = Icons.Default.LocalAtm,
                badgeTint = Color(0xFF30A46C), // Vibrant Green
                isTransfer = true
            )
        }

        val cleanSource = sourceType?.uppercase()?.trim() ?: "UNKNOWN"
        val cleanBank = polishBankName(bank)

        // Case 1: Cash transaction
        if (cleanSource == "CASH") {
            return PolishedBrandTag(
                title = "Cash Wallet",
                icon = Icons.Default.Payments,
                badgeTint = Color(0xFF30A46C)
            )
        }

        // Case 2: Bank is identified
        if (!cleanBank.isNullOrBlank()) {
            return when (cleanSource) {
                "UPI" -> PolishedBrandTag(
                    title = "$cleanBank • UPI",
                    icon = Icons.Default.Bolt,
                    badgeTint = Color(0xFFE2E4E9) // Monochrome Silver
                )
                "CARD" -> PolishedBrandTag(
                    title = "$cleanBank • Card",
                    icon = Icons.Default.CreditCard,
                    badgeTint = Color(0xFFE2E4E9) // Monochrome Silver
                )
                "BANK_TRANSFER" -> PolishedBrandTag(
                    title = "$cleanBank • Netbanking",
                    icon = Icons.Default.AccountBalance,
                    badgeTint = Color(0xFFE2E4E9) // Monochrome Silver
                )
                "ATM" -> PolishedBrandTag(
                    title = "$cleanBank • ATM",
                    icon = Icons.Default.LocalAtm,
                    badgeTint = Color(0xFF30A46C) // Cash Green
                )
                else -> PolishedBrandTag(
                    title = if (isWalletConfigured) "$cleanBank • Online" else cleanBank,
                    icon = Icons.Default.AccountBalance,
                    badgeTint = Color(0xFFE2E4E9)
                )
            }
        }

        // Case 3: Bank is null / unknown
        return when (cleanSource) {
            "UPI" -> PolishedBrandTag(
                title = if (isWalletConfigured) "UPI • Online" else "UPI Payment",
                icon = Icons.Default.Bolt,
                badgeTint = Color(0xFFE2E4E9)
            )
            "CARD" -> PolishedBrandTag(
                title = if (isWalletConfigured) "Card • Online" else "Card Payment",
                icon = Icons.Default.CreditCard,
                badgeTint = Color(0xFFE2E4E9)
            )
            "BANK_TRANSFER" -> PolishedBrandTag(
                title = if (isWalletConfigured) "Bank Transfer • Online" else "Bank Transfer",
                icon = Icons.Default.AccountBalance,
                badgeTint = Color(0xFFE2E4E9)
            )
            "ATM" -> PolishedBrandTag(
                title = "Cash ATM",
                icon = Icons.Default.LocalAtm,
                badgeTint = Color(0xFF30A46C)
            )
            // NEVER output "UNKNOWN"! Replace with polished "Online Account"
            else -> PolishedBrandTag(
                title = if (isWalletConfigured) "Online Account" else "Digital Payment",
                icon = Icons.Default.AccountBalanceWallet,
                badgeTint = Color(0xFFD4D4D8)
            )
        }
    }

    /**
     * Polishes raw source types for simple text chips (e.g. in list views)
     */
    fun formatSourceLabel(sourceType: String?): String {
        return when (sourceType?.uppercase()?.trim()) {
            "UPI" -> "UPI"
            "CARD" -> "Card"
            "ATM" -> "ATM"
            "BANK_TRANSFER" -> "Netbanking"
            "CASH" -> "Cash"
            "WALLET_TRANSFER" -> "Transfer"
            "UNKNOWN", null, "" -> "Online"
            else -> titleCase(sourceType)
        }
    }

    /**
     * Standardizes Indian bank names for clean badge presentation.
     */
    fun polishBankName(bank: String?): String? {
        if (bank.isNullOrBlank()) return null
        val trimmed = bank.trim()
        val upper = trimmed.uppercase(Locale.ROOT)

        return when {
            upper.contains("HDFC") -> "HDFC Bank"
            upper == "SBI" || upper.contains("STATE BANK") -> "SBI"
            upper.contains("ICICI") -> "ICICI Bank"
            upper.contains("AXIS") -> "Axis Bank"
            upper.contains("KOTAK") -> "Kotak Bank"
            upper.contains("BARODA") || upper == "BOB" -> "Bank of Baroda"
            upper.contains("PUNJAB NATIONAL") || upper == "PNB" -> "PNB"
            upper.contains("CANARA") -> "Canara Bank"
            upper.contains("INDUSIND") -> "IndusInd Bank"
            upper.contains("IDFC") -> "IDFC FIRST Bank"
            upper.contains("UNION") -> "Union Bank"
            upper.contains("YES BANK") || upper == "YES" -> "Yes Bank"
            upper.contains("FEDERAL") -> "Federal Bank"
            upper.contains("PAYTM") -> "Paytm Bank"
            upper.contains("AIRTEL") -> "Airtel Payments Bank"
            trimmed.endsWith("Bank", ignoreCase = true) -> trimmed
            else -> trimmed
        }
    }

    private fun isGenericKeyword(s: String): Boolean {
        val lower = s.lowercase(Locale.ROOT).trim()
        return lower in setOf("rs", "inr", "bal", "avl", "dear", "customer", "acc", "account", "a/c", "alert", "spent")
    }

    private fun titleCase(input: String): String {
        return input.split(" ").joinToString(" ") { word ->
            val clean = word.trim()
            if (clean.uppercase(Locale.ROOT) in KNOWN_ACRONYMS) {
                clean.uppercase(Locale.ROOT)
            } else {
                clean.lowercase(Locale.ROOT).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }
    }
}
