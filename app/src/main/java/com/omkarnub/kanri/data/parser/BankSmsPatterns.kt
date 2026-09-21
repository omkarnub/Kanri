package com.omkarnub.kanri.data.parser

data class BankSmsPattern(
    val bankCode: String,
    val bankName: String,
    val senderPrefixes: List<String>,
    val bodyKeywords: List<String>,
    val customAmountRegex: List<String> = emptyList(),
    val customAccountRegex: List<String> = emptyList(),
    val customCounterpartyRegex: List<String> = emptyList()
)

object BankSmsPatterns {

    val ALL_PATTERNS: List<BankSmsPattern> = listOf(
        BankSmsPattern(
            bankCode = "BOB",
            bankName = "Bank of Baroda",
            senderPrefixes = listOf("BOB-TXN", "BOBSMS", "BARODA", "BOB"),
            bodyKeywords = listOf("BANK OF BARODA", "BOB")
        ),
        BankSmsPattern(
            bankCode = "SBI",
            bankName = "State Bank of India",
            senderPrefixes = listOf("SBI-UPI", "SBIPAY", "SBISMS", "SBIINB", "SBI"),
            bodyKeywords = listOf("STATE BANK OF INDIA", "SBI")
        ),
        BankSmsPattern(
            bankCode = "HDFC",
            bankName = "HDFC Bank",
            senderPrefixes = listOf("HDFCBK", "HDFC", "HDFCSMS"),
            bodyKeywords = listOf("HDFC BANK", "HDFC")
        ),
        BankSmsPattern(
            bankCode = "ICICI",
            bankName = "ICICI Bank",
            senderPrefixes = listOf("ICICIB", "ICICI", "ICICISMS"),
            bodyKeywords = listOf("ICICI BANK", "ICICI")
        ),
        BankSmsPattern(
            bankCode = "KOTAK",
            bankName = "Kotak Mahindra Bank",
            senderPrefixes = listOf("KOTAKB", "KOTAK"),
            bodyKeywords = listOf("KOTAK MAHINDRA", "KOTAK BANK", "KOTAK")
        ),
        BankSmsPattern(
            bankCode = "AXIS",
            bankName = "Axis Bank",
            senderPrefixes = listOf("AXISBK", "AXISB", "AXIS"),
            bodyKeywords = listOf("AXIS BANK", "AXIS")
        ),
        BankSmsPattern(
            bankCode = "IDFC",
            bankName = "IDFC First Bank",
            senderPrefixes = listOf("IDFCFB", "IDFCB", "IDFC"),
            bodyKeywords = listOf("IDFC FIRST BANK", "IDFC FIRST", "IDFC BANK", "IDFC")
        ),
        BankSmsPattern(
            bankCode = "YESBNK",
            bankName = "Yes Bank",
            senderPrefixes = listOf("YESBNK", "YESB", "YESBANK"),
            bodyKeywords = listOf("YES BANK", "YESBANK")
        ),
        BankSmsPattern(
            bankCode = "INDBNK",
            bankName = "IndusInd Bank",
            senderPrefixes = listOf("INDBNK", "INDUSB", "INDUS"),
            bodyKeywords = listOf("INDUSIND BANK", "INDUSIND", "INDUS")
        ),
        BankSmsPattern(
            bankCode = "PNB",
            bankName = "Punjab National Bank",
            senderPrefixes = listOf("PNBSMS", "PUNJAB", "PNB"),
            bodyKeywords = listOf("PUNJAB NATIONAL BANK", "PNB")
        ),
        BankSmsPattern(
            bankCode = "UNIONB",
            bankName = "Union Bank of India",
            senderPrefixes = listOf("UNIONB", "UBISMS", "UNION"),
            bodyKeywords = listOf("UNION BANK OF INDIA", "UNION BANK", "UBI")
        ),
        BankSmsPattern(
            bankCode = "CANARA",
            bankName = "Canara Bank",
            senderPrefixes = listOf("CANBNK", "CANARAB", "CANARA"),
            bodyKeywords = listOf("CANARA BANK", "CANARA")
        ),
        BankSmsPattern(
            bankCode = "BOI",
            bankName = "Bank of India",
            senderPrefixes = listOf("BOISMS", "BOITXN", "BOI"),
            bodyKeywords = listOf("BANK OF INDIA", "BOI")
        ),
        BankSmsPattern(
            bankCode = "SRSWAT",
            bankName = "Saraswat Bank",
            senderPrefixes = listOf("SRSWAT", "SARASWAT", "SRSWTB"),
            bodyKeywords = listOf("SARASWAT CO-OPERATIVE BANK", "SARASWAT BANK", "SARASWAT")
        ),
        BankSmsPattern(
            bankCode = "COSMOS",
            bankName = "Cosmos Bank",
            senderPrefixes = listOf("COSMOS", "CSMOSB"),
            bodyKeywords = listOf("COSMOS CO-OPERATIVE BANK", "COSMOS BANK", "COSMOS")
        ),
        // Fallback UPI Apps
        BankSmsPattern(
            bankCode = "PHONPE",
            bankName = "PhonePe",
            senderPrefixes = listOf("PHONPE", "PHONEPE", "PHTXN"),
            bodyKeywords = listOf("PHONEPE")
        ),
        BankSmsPattern(
            bankCode = "GPAY",
            bankName = "Google Pay",
            senderPrefixes = listOf("GPAY", "GOOGLP", "GOOGLE"),
            bodyKeywords = listOf("GOOGLE PAY", "GPAY")
        ),
        BankSmsPattern(
            bankCode = "PAYTM",
            bankName = "Paytm",
            senderPrefixes = listOf("PAYTM", "PAYTMP", "PYTM"),
            bodyKeywords = listOf("PAYTM PAYMENTS BANK", "PAYTM UPI", "PAYTM")
        )
    )

    fun extractSenderHeader(sender: String?): String {
        if (sender.isNullOrBlank()) return ""
        val trimmed = sender.trim().uppercase()
        val dashIndex = trimmed.indexOf('-')
        return if (dashIndex != -1 && dashIndex < trimmed.length - 1) {
            trimmed.substring(dashIndex + 1)
        } else {
            trimmed
        }
    }

    fun isAllowlistedSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val header = extractSenderHeader(sender)
        val upperSender = sender.trim().uppercase()
        return ALL_PATTERNS.any { pattern ->
            pattern.senderPrefixes.any { prefix ->
                val p = prefix.uppercase()
                header == p || header.contains(p) || upperSender.contains(p)
            }
        }
    }

    fun findBySender(sender: String?): BankSmsPattern? {
        if (sender.isNullOrBlank()) return null
        val header = extractSenderHeader(sender)
        val upperSender = sender.trim().uppercase()
        return ALL_PATTERNS.firstOrNull { pattern ->
            pattern.senderPrefixes.any { prefix ->
                val p = prefix.uppercase()
                header == p || header.contains(p) || upperSender.contains(p)
            }
        }
    }

    fun findByBody(body: String): BankSmsPattern? {
        val upper = body.uppercase()
        for (pattern in ALL_PATTERNS) {
            for (keyword in pattern.bodyKeywords) {
                if (keyword.length > 3 && upper.contains(keyword)) {
                    return pattern
                }
            }
        }
        for (pattern in ALL_PATTERNS) {
            for (keyword in pattern.bodyKeywords) {
                if (upper.contains(" $keyword ") || upper.startsWith("$keyword ") || upper.contains("$keyword BANK") || upper.endsWith(" $keyword")) {
                    return pattern
                }
            }
        }
        return null
    }
}
