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
        // ==========================================
        // 1. TOP PUBLIC SECTOR BANKS
        // ==========================================
        BankSmsPattern(
            bankCode = "SBI",
            bankName = "State Bank of India",
            senderPrefixes = listOf("SBIPAY", "SBIINB", "SBISMS", "CBSSBI", "SBIPSG", "SBIUPI", "ATMSBI", "SBIBK", "SBIDBT", "SBI"),
            bodyKeywords = listOf("STATE BANK OF INDIA", "SBIPAY", "YONO", "SBI BANK", "SBI")
        ),
        BankSmsPattern(
            bankCode = "BOB",
            bankName = "Bank of Baroda",
            senderPrefixes = listOf("BOBTXN", "BOBSMS", "BARODA", "BOBALR", "BOB"),
            bodyKeywords = listOf("BANK OF BARODA", "BOB BANK", "BOB")
        ),
        BankSmsPattern(
            bankCode = "PNB",
            bankName = "Punjab National Bank",
            senderPrefixes = listOf("PNBSMS", "PUNJAB", "PNBTXN", "PNB"),
            bodyKeywords = listOf("PUNJAB NATIONAL BANK", "PNB")
        ),
        BankSmsPattern(
            bankCode = "CANARA",
            bankName = "Canara Bank",
            senderPrefixes = listOf("CANBNK", "CANARAB", "CANARA", "CANAR"),
            bodyKeywords = listOf("CANARA BANK", "CANARA")
        ),
        BankSmsPattern(
            bankCode = "UNIONB",
            bankName = "Union Bank of India",
            senderPrefixes = listOf("UNIONB", "UBISMS", "UNION", "UBITXN", "UBI"),
            bodyKeywords = listOf("UNION BANK OF INDIA", "UNION BANK", "UBI")
        ),
        BankSmsPattern(
            bankCode = "BOI",
            bankName = "Bank of India",
            senderPrefixes = listOf("BOISMS", "BOITXN", "BOIIND", "BOI"),
            bodyKeywords = listOf("BANK OF INDIA", "BOI")
        ),
        BankSmsPattern(
            bankCode = "INDIANB",
            bankName = "Indian Bank",
            senderPrefixes = listOf("INDIANB", "IDBMS", "INDALR", "INDIAB"),
            bodyKeywords = listOf("INDIAN BANK")
        ),
        BankSmsPattern(
            bankCode = "CENTBK",
            bankName = "Central Bank of India",
            senderPrefixes = listOf("CENTBK", "CBISMS", "CENTRAL"),
            bodyKeywords = listOf("CENTRAL BANK OF INDIA", "CENTRAL BANK")
        ),
        BankSmsPattern(
            bankCode = "UCOBNK",
            bankName = "UCO Bank",
            senderPrefixes = listOf("UCOBNK", "UCOSMS", "UCOBK", "UCO"),
            bodyKeywords = listOf("UCO BANK")
        ),
        BankSmsPattern(
            bankCode = "MAHABK",
            bankName = "Bank of Maharashtra",
            senderPrefixes = listOf("MAHABK", "BOMSMS", "MAHA"),
            bodyKeywords = listOf("BANK OF MAHARASHTRA")
        ),
        BankSmsPattern(
            bankCode = "PSBBNK",
            bankName = "Punjab & Sind Bank",
            senderPrefixes = listOf("PSBBNK", "PUNJSB", "PSBSMS"),
            bodyKeywords = listOf("PUNJAB & SIND BANK", "PUNJAB AND SIND")
        ),
        BankSmsPattern(
            bankCode = "IOBCHN",
            bankName = "Indian Overseas Bank",
            senderPrefixes = listOf("IOBCHN", "IOBSMS", "IOBBANK", "IOB"),
            bodyKeywords = listOf("INDIAN OVERSEAS BANK", "IOB")
        ),

        // ==========================================
        // 2. TOP PRIVATE SECTOR BANKS
        // ==========================================
        BankSmsPattern(
            bankCode = "HDFC",
            bankName = "HDFC Bank",
            senderPrefixes = listOf("HDFCBK", "HDFC", "HDFCSMS", "HDFCB", "HDFCCC"),
            bodyKeywords = listOf("HDFC BANK", "HDFCBANK", "HDFC")
        ),
        BankSmsPattern(
            bankCode = "ICICI",
            bankName = "ICICI Bank",
            senderPrefixes = listOf("ICICIB", "ICICI", "ICICISMS", "ICICIA", "ICICIT"),
            bodyKeywords = listOf("ICICI BANK", "ICICIBANK", "ICICI")
        ),
        BankSmsPattern(
            bankCode = "AXIS",
            bankName = "Axis Bank",
            senderPrefixes = listOf("AXISBK", "AXISB", "AXIS", "AXISMS", "AXISTX"),
            bodyKeywords = listOf("AXIS BANK", "AXIS")
        ),
        BankSmsPattern(
            bankCode = "KOTAK",
            bankName = "Kotak Mahindra Bank",
            senderPrefixes = listOf("KOTAKB", "KOTAK", "KMBALR", "KMBTXN"),
            bodyKeywords = listOf("KOTAK MAHINDRA", "KOTAK BANK", "KOTAK")
        ),
        BankSmsPattern(
            bankCode = "IDFC",
            bankName = "IDFC First Bank",
            senderPrefixes = listOf("IDFCFB", "IDFCB", "IDFC", "IDFCBN"),
            bodyKeywords = listOf("IDFC FIRST BANK", "IDFC FIRST", "IDFC BANK", "IDFC")
        ),
        BankSmsPattern(
            bankCode = "INDUS",
            bankName = "IndusInd Bank",
            senderPrefixes = listOf("INDBNK", "INDUSB", "INDUS", "INDUSI"),
            bodyKeywords = listOf("INDUSIND BANK", "INDUSIND", "INDUS")
        ),
        BankSmsPattern(
            bankCode = "YESBNK",
            bankName = "Yes Bank",
            senderPrefixes = listOf("YESBNK", "YESB", "YESBANK", "YESPAY"),
            bodyKeywords = listOf("YES BANK", "YESBANK")
        ),
        BankSmsPattern(
            bankCode = "FEDBNK",
            bankName = "Federal Bank",
            senderPrefixes = listOf("FEDBNK", "FEDERAL", "FEDPAY"),
            bodyKeywords = listOf("FEDERAL BANK")
        ),
        BankSmsPattern(
            bankCode = "RBLBNK",
            bankName = "RBL Bank",
            senderPrefixes = listOf("RBLBNK", "RBLCRD", "RATNAK", "RBLPAY"),
            bodyKeywords = listOf("RBL BANK", "RATNAKAR BANK")
        ),
        BankSmsPattern(
            bankCode = "IDBIBK",
            bankName = "IDBI Bank",
            senderPrefixes = listOf("IDBIBK", "IDBI", "IDBISMS"),
            bodyKeywords = listOf("IDBI BANK", "IDBI")
        ),
        BankSmsPattern(
            bankCode = "SIBLTD",
            bankName = "South Indian Bank",
            senderPrefixes = listOf("SIBLTD", "SIBBNK", "SIBSMS"),
            bodyKeywords = listOf("SOUTH INDIAN BANK", "SIB")
        ),
        BankSmsPattern(
            bankCode = "BANDHN",
            bankName = "Bandhan Bank",
            senderPrefixes = listOf("BNDHAN", "BANDHN", "BANDHAN"),
            bodyKeywords = listOf("BANDHAN BANK")
        ),
        BankSmsPattern(
            bankCode = "CUBBNK",
            bankName = "City Union Bank",
            senderPrefixes = listOf("CUBBNK", "CUBTXN", "CITYUB"),
            bodyKeywords = listOf("CITY UNION BANK", "CUB BANK")
        ),
        BankSmsPattern(
            bankCode = "KVBANK",
            bankName = "Karur Vysya Bank",
            senderPrefixes = listOf("KVBANK", "KVBSMS", "KVB"),
            bodyKeywords = listOf("KARUR VYSYA BANK", "KVB")
        ),
        BankSmsPattern(
            bankCode = "KBLBNK",
            bankName = "Karnataka Bank",
            senderPrefixes = listOf("KBLBNK", "KTKBNK", "KBLSMS"),
            bodyKeywords = listOf("KARNATAKA BANK")
        ),
        BankSmsPattern(
            bankCode = "SCBANK",
            bankName = "Standard Chartered Bank",
            senderPrefixes = listOf("SCBANK", "SCISMS", "STANBK", "SCB"),
            bodyKeywords = listOf("STANDARD CHARTERED", "SCB")
        ),
        BankSmsPattern(
            bankCode = "HSBCBK",
            bankName = "HSBC Bank",
            senderPrefixes = listOf("HSBCBK", "HSBCIN", "HSBC"),
            bodyKeywords = listOf("HSBC BANK", "HSBC")
        ),
        BankSmsPattern(
            bankCode = "CITIBK",
            bankName = "Citi Bank",
            senderPrefixes = listOf("CITIBK", "CITI", "CITISMS"),
            bodyKeywords = listOf("CITIBANK", "CITI BANK")
        ),
        BankSmsPattern(
            bankCode = "DBSBNK",
            bankName = "DBS Bank",
            senderPrefixes = listOf("DBSBNK", "DBSIN", "DBS"),
            bodyKeywords = listOf("DBS BANK", "DIGIBANK")
        ),

        // ==========================================
        // 3. SMALL FINANCE & NEO BANKS
        // ==========================================
        BankSmsPattern(
            bankCode = "AUBANK",
            bankName = "AU Small Finance Bank",
            senderPrefixes = listOf("AUBANK", "AUFINB", "AUSMS"),
            bodyKeywords = listOf("AU SMALL FINANCE BANK", "AU BANK")
        ),
        BankSmsPattern(
            bankCode = "EQUTAS",
            bankName = "Equitas Small Finance Bank",
            senderPrefixes = listOf("EQUTAS", "EQUITAS", "EQTSMS"),
            bodyKeywords = listOf("EQUITAS SMALL FINANCE BANK", "EQUITAS BANK")
        ),
        BankSmsPattern(
            bankCode = "UJJIVN",
            bankName = "Ujjivan Small Finance Bank",
            senderPrefixes = listOf("UJJIVN", "UJJIVAN"),
            bodyKeywords = listOf("UJJIVAN SMALL FINANCE BANK", "UJJIVAN")
        ),
        BankSmsPattern(
            bankCode = "UTKSHB",
            bankName = "Utkarsh Small Finance Bank",
            senderPrefixes = listOf("UTKSHB", "UTKARSH"),
            bodyKeywords = listOf("UTKARSH SMALL FINANCE BANK", "UTKARSH")
        ),
        BankSmsPattern(
            bankCode = "SURYOD",
            bankName = "Suryoday Small Finance Bank",
            senderPrefixes = listOf("SURYOD", "SURYODAY"),
            bodyKeywords = listOf("SURYODAY SMALL FINANCE BANK")
        ),
        BankSmsPattern(
            bankCode = "JUPITR",
            bankName = "Jupiter",
            senderPrefixes = listOf("JUPITR", "JUPITER"),
            bodyKeywords = listOf("JUPITER MONEY", "JUPITER")
        ),
        BankSmsPattern(
            bankCode = "FIALRT",
            bankName = "Fi Money",
            senderPrefixes = listOf("FIALRT", "FIMONY", "EPIFI"),
            bodyKeywords = listOf("FI MONEY", "EPIFI")
        ),
        BankSmsPattern(
            bankCode = "NIYO",
            bankName = "Niyo",
            senderPrefixes = listOf("NIYO", "NIYOBX"),
            bodyKeywords = listOf("NIYO")
        ),

        // ==========================================
        // 4. PAYMENTS BANKS & WALLETS
        // ==========================================
        BankSmsPattern(
            bankCode = "PAYTM",
            bankName = "Paytm",
            senderPrefixes = listOf("PAYTM", "PAYTMB", "PYTM", "PAYTMP"),
            bodyKeywords = listOf("PAYTM PAYMENTS BANK", "PAYTM UPI", "PAYTM WALLET", "PAYTM")
        ),
        BankSmsPattern(
            bankCode = "AIRTEL",
            bankName = "Airtel Payments Bank",
            senderPrefixes = listOf("AIRTEL", "AIRPAY", "APBL"),
            bodyKeywords = listOf("AIRTEL PAYMENTS BANK", "AIRTEL MONEY", "AIRTEL")
        ),
        BankSmsPattern(
            bankCode = "JIOPAY",
            bankName = "Jio Payments Bank",
            senderPrefixes = listOf("JIOPAY", "JIOBNK"),
            bodyKeywords = listOf("JIO PAYMENTS BANK", "JIOPAY")
        ),
        BankSmsPattern(
            bankCode = "FAMPAY",
            bankName = "FamPay",
            senderPrefixes = listOf("FAMPAY", "FAMP"),
            bodyKeywords = listOf("FAMPAY")
        ),
        BankSmsPattern(
            bankCode = "AMZPAY",
            bankName = "Amazon Pay",
            senderPrefixes = listOf("AMZPAY", "AMAZON"),
            bodyKeywords = listOf("AMAZON PAY")
        ),
        BankSmsPattern(
            bankCode = "MBKWIK",
            bankName = "MobiKwik",
            senderPrefixes = listOf("MBKWIK", "MOBIKW"),
            bodyKeywords = listOf("MOBIKWIK")
        ),

        // ==========================================
        // 5. CO-OPERATIVE BANKS
        // ==========================================
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
        BankSmsPattern(
            bankCode = "TJSBNK",
            bankName = "TJSB Sahakari Bank",
            senderPrefixes = listOf("TJSBNK", "TJSB"),
            bodyKeywords = listOf("TJSB SAHAKARI BANK", "TJSB BANK")
        ),
        BankSmsPattern(
            bankCode = "SVCCBK",
            bankName = "SVC Bank",
            senderPrefixes = listOf("SVCCBK", "SVCBNK", "SVC"),
            bodyKeywords = listOf("SVC CO-OPERATIVE BANK", "SVC BANK")
        ),

        // ==========================================
        // 6. CREDIT CARDS & PAY LATER
        // ==========================================
        BankSmsPattern(
            bankCode = "SBICRD",
            bankName = "SBI Card",
            senderPrefixes = listOf("SBICRD", "SBICARD"),
            bodyKeywords = listOf("SBI CARD", "SBICARD")
        ),
        BankSmsPattern(
            bankCode = "ONECRD",
            bankName = "OneCard",
            senderPrefixes = listOf("ONECRD", "ONECARD"),
            bodyKeywords = listOf("ONECARD", "ONE CARD")
        ),
        BankSmsPattern(
            bankCode = "SLICEC",
            bankName = "Slice",
            senderPrefixes = listOf("SLICEC", "SLICE"),
            bodyKeywords = listOf("SLICE CARD", "SLICE")
        ),
        BankSmsPattern(
            bankCode = "UNICRD",
            bankName = "Uni Card",
            senderPrefixes = listOf("UNICRD", "UNI"),
            bodyKeywords = listOf("UNI CARD")
        ),
        BankSmsPattern(
            bankCode = "CRED",
            bankName = "CRED",
            senderPrefixes = listOf("CREDTX", "CRED"),
            bodyKeywords = listOf("CRED PAY", "CRED")
        ),
        BankSmsPattern(
            bankCode = "LAZYPY",
            bankName = "LazyPay",
            senderPrefixes = listOf("LAZYPY", "LAZYPAY"),
            bodyKeywords = listOf("LAZYPAY")
        ),
        BankSmsPattern(
            bankCode = "SIMPPL",
            bankName = "Simpl",
            senderPrefixes = listOf("SIMPPL", "SIMPL"),
            bodyKeywords = listOf("GETSIMPL", "SIMPL")
        ),

        // ==========================================
        // 7. UPI APPS & NPCI
        // ==========================================
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
            bankCode = "BHIM",
            bankName = "BHIM UPI",
            senderPrefixes = listOf("BHIM", "NPCI"),
            bodyKeywords = listOf("BHIM UPI", "NPCI")
        )
    )

    fun extractSenderHeader(sender: String?): String {
        if (sender.isNullOrBlank()) return ""
        var trimmed = sender.trim().uppercase()
        // Strip leading circle/operator prefix like "VM-", "AD-", "BW-", "BZ-"
        val firstDash = trimmed.indexOf('-')
        if (firstDash in 1..4 && firstDash < trimmed.length - 1) {
            trimmed = trimmed.substring(firstDash + 1)
        }
        // Strip trailing suffix like "-S", "-T", "-G"
        val lastDash = trimmed.lastIndexOf('-')
        if (lastDash != -1 && lastDash >= trimmed.length - 3) {
            trimmed = trimmed.substring(0, lastDash)
        }
        return trimmed
    }

    fun isAllowlistedSender(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val upperSender = sender.trim().uppercase()
        val cleanAlphanumeric = upperSender.filter { it.isLetterOrDigit() }
        val header = extractSenderHeader(sender)

        return ALL_PATTERNS.any { pattern ->
            pattern.senderPrefixes.any { prefix ->
                val p = prefix.uppercase()
                header == p || header.contains(p) || upperSender.contains(p) || cleanAlphanumeric.contains(p)
            }
        }
    }

    fun findBySender(sender: String?): BankSmsPattern? {
        if (sender.isNullOrBlank()) return null
        val upperSender = sender.trim().uppercase()
        val cleanAlphanumeric = upperSender.filter { it.isLetterOrDigit() }
        val header = extractSenderHeader(sender)

        // Find all matching patterns, prioritizing the most specific (longest prefix match)
        return ALL_PATTERNS
            .filter { pattern ->
                pattern.senderPrefixes.any { prefix ->
                    val p = prefix.uppercase()
                    header == p || header.contains(p) || upperSender.contains(p) || cleanAlphanumeric.contains(p)
                }
            }
            .maxByOrNull { pattern ->
                pattern.senderPrefixes
                    .filter { p ->
                        val up = p.uppercase()
                        header == up || header.contains(up) || upperSender.contains(up) || cleanAlphanumeric.contains(up)
                    }
                    .maxOfOrNull { it.length } ?: 0
            }
    }

    fun findByBody(body: String): BankSmsPattern? {
        val upper = body.uppercase()
        // Exact longer keyword first
        for (pattern in ALL_PATTERNS) {
            for (keyword in pattern.bodyKeywords) {
                if (keyword.length > 3 && upper.contains(keyword)) {
                    return pattern
                }
            }
        }
        // Word boundary match for shorter keywords (e.g. "BOB", "SBI", "UBI")
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
