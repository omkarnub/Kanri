package com.omkarnub.kanri.util

import java.util.Locale

/**
 * Supported global currencies across all regions with codes, symbols, flags, and locale tags.
 */
enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val localeTag: String,
    val flag: String = ""
) {
    INR(code = "INR", symbol = "₹", displayName = "Indian Rupee", localeTag = "en-IN", flag = "🇮🇳"),
    USD(code = "USD", symbol = "$", displayName = "US Dollar", localeTag = "en-US", flag = "🇺🇸"),
    EUR(code = "EUR", symbol = "€", displayName = "Euro", localeTag = "de-DE", flag = "🇪🇺"),
    GBP(code = "GBP", symbol = "£", displayName = "British Pound", localeTag = "en-GB", flag = "🇬🇧"),
    TRY(code = "TRY", symbol = "₺", displayName = "Turkish Lira", localeTag = "tr-TR", flag = "🇹🇷"),
    AED(code = "AED", symbol = "AED", displayName = "UAE Dirham", localeTag = "ar-AE", flag = "🇦🇪"),
    SAR(code = "SAR", symbol = "SAR", displayName = "Saudi Riyal", localeTag = "ar-SA", flag = "🇸🇦"),
    CAD(code = "CAD", symbol = "CA$", displayName = "Canadian Dollar", localeTag = "en-CA", flag = "🇨🇦"),
    AUD(code = "AUD", symbol = "A$", displayName = "Australian Dollar", localeTag = "en-AU", flag = "🇦🇺"),
    JPY(code = "JPY", symbol = "¥", displayName = "Japanese Yen", localeTag = "ja-JP", flag = "🇯🇵"),
    CNY(code = "CNY", symbol = "CN¥", displayName = "Chinese Yuan", localeTag = "zh-CN", flag = "🇨🇳"),
    CHF(code = "CHF", symbol = "CHF", displayName = "Swiss Franc", localeTag = "de-CH", flag = "🇨🇭"),
    SGD(code = "SGD", symbol = "S$", displayName = "Singapore Dollar", localeTag = "en-SG", flag = "🇸🇬"),
    HKD(code = "HKD", symbol = "HK$", displayName = "Hong Kong Dollar", localeTag = "zh-HK", flag = "🇭🇰"),
    NZD(code = "NZD", symbol = "NZ$", displayName = "New Zealand Dollar", localeTag = "en-NZ", flag = "🇳🇿"),
    KRW(code = "KRW", symbol = "₩", displayName = "South Korean Won", localeTag = "ko-KR", flag = "🇰🇷"),
    BRL(code = "BRL", symbol = "R$", displayName = "Brazilian Real", localeTag = "pt-BR", flag = "🇧🇷"),
    MXN(code = "MXN", symbol = "Mex$", displayName = "Mexican Peso", localeTag = "es-MX", flag = "🇲🇽"),
    IDR(code = "IDR", symbol = "Rp", displayName = "Indonesian Rupiah", localeTag = "id-ID", flag = "🇮🇩"),
    MYR(code = "MYR", symbol = "RM", displayName = "Malaysian Ringgit", localeTag = "ms-MY", flag = "🇲🇾"),
    THB(code = "THB", symbol = "฿", displayName = "Thai Baht", localeTag = "th-TH", flag = "🇹🇭"),
    PHP(code = "PHP", symbol = "₱", displayName = "Philippine Peso", localeTag = "en-PH", flag = "🇵🇭"),
    VND(code = "VND", symbol = "₫", displayName = "Vietnamese Dong", localeTag = "vi-VN", flag = "🇻🇳"),
    ZAR(code = "ZAR", symbol = "R", displayName = "South African Rand", localeTag = "en-ZA", flag = "🇿🇦"),
    RUB(code = "RUB", symbol = "₽", displayName = "Russian Ruble", localeTag = "ru-RU", flag = "🇷🇺"),
    SEK(code = "SEK", symbol = "kr", displayName = "Swedish Krona", localeTag = "sv-SE", flag = "🇸🇪"),
    NOK(code = "NOK", symbol = "kr", displayName = "Norwegian Krone", localeTag = "no-NO", flag = "🇳🇴"),
    DKK(code = "DKK", symbol = "kr", displayName = "Danish Krone", localeTag = "da-DK", flag = "🇩🇰"),
    PLN(code = "PLN", symbol = "zł", displayName = "Polish Zloty", localeTag = "pl-PL", flag = "🇵🇱"),
    CZK(code = "CZK", symbol = "Kč", displayName = "Czech Koruna", localeTag = "cs-CZ", flag = "🇨🇿"),
    HUF(code = "HUF", symbol = "Ft", displayName = "Hungarian Forint", localeTag = "hu-HU", flag = "🇭🇺"),
    ILS(code = "ILS", symbol = "₪", displayName = "Israeli New Shekel", localeTag = "he-IL", flag = "🇮🇱"),
    EGP(code = "EGP", symbol = "E£", displayName = "Egyptian Pound", localeTag = "ar-EG", flag = "🇪🇬"),
    NGN(code = "NGN", symbol = "₦", displayName = "Nigerian Naira", localeTag = "en-NG", flag = "🇳🇬"),
    KES(code = "KES", symbol = "KSh", displayName = "Kenyan Shilling", localeTag = "sw-KE", flag = "🇰🇪"),
    PKR(code = "PKR", symbol = "₨", displayName = "Pakistani Rupee", localeTag = "ur-PK", flag = "🇵🇰"),
    BDT(code = "BDT", symbol = "৳", displayName = "Bangladeshi Taka", localeTag = "bn-BD", flag = "🇧🇩"),
    LKR(code = "LKR", symbol = "Rs", displayName = "Sri Lankan Rupee", localeTag = "si-LK", flag = "🇱🇰"),
    NPR(code = "NPR", symbol = "रू", displayName = "Nepalese Rupee", localeTag = "ne-NP", flag = "🇳🇵"),
    KWD(code = "KWD", symbol = "KD", displayName = "Kuwaiti Dinar", localeTag = "ar-KW", flag = "🇰🇼"),
    QAR(code = "QAR", symbol = "QR", displayName = "Qatari Riyal", localeTag = "ar-QA", flag = "🇶🇦"),
    OMR(code = "OMR", symbol = "RO", displayName = "Omani Rial", localeTag = "ar-OM", flag = "🇴🇲"),
    BHD(code = "BHD", symbol = "BD", displayName = "Bahraini Dinar", localeTag = "ar-BH", flag = "🇧🇭"),
    CLP(code = "CLP", symbol = "CLP$", displayName = "Chilean Peso", localeTag = "es-CL", flag = "🇨🇱"),
    COP(code = "COP", symbol = "COL$", displayName = "Colombian Peso", localeTag = "es-CO", flag = "🇨🇴"),
    ARS(code = "ARS", symbol = "ARS$", displayName = "Argentine Peso", localeTag = "es-AR", flag = "🇦🇷"),
    PEN(code = "PEN", symbol = "S/.", displayName = "Peruvian Sol", localeTag = "es-PE", flag = "🇵🇪"),
    TWD(code = "TWD", symbol = "NT$", displayName = "New Taiwan Dollar", localeTag = "zh-TW", flag = "🇹🇼"),
    UAH(code = "UAH", symbol = "₴", displayName = "Ukrainian Hryvnia", localeTag = "uk-UA", flag = "🇺🇦"),
    KZT(code = "KZT", symbol = "₸", displayName = "Kazakhstani Tenge", localeTag = "kk-KZ", flag = "🇰🇿"),
    GHS(code = "GHS", symbol = "GH₵", displayName = "Ghanaian Cedi", localeTag = "en-GH", flag = "🇬🇭"),
    MAD(code = "MAD", symbol = "MAD", displayName = "Moroccan Dirham", localeTag = "ar-MA", flag = "🇲🇦");

    val locale: Locale
        get() = Locale.forLanguageTag(localeTag)

    /**
     * Sensible default monthly budget baseline scaled to this currency's purchasing power.
     */
    val defaultMonthlyBudget: Double
        get() = when (this) {
            INR -> 25_000.0
            USD -> 2_500.0
            EUR -> 2_200.0
            GBP -> 2_000.0
            TRY -> 45_000.0
            AED -> 8_000.0
            SAR -> 8_000.0
            CAD -> 3_500.0
            AUD -> 3_500.0
            JPY -> 300_000.0
            CNY -> 15_000.0
            CHF -> 2_500.0
            SGD -> 3_500.0
            HKD -> 20_000.0
            NZD -> 3_500.0
            KRW -> 2_500_000.0
            BRL -> 6_000.0
            MXN -> 25_000.0
            IDR -> 10_000_000.0
            MYR -> 5_000.0
            THB -> 40_000.0
            PHP -> 60_000.0
            VND -> 25_000_000.0
            ZAR -> 25_000.0
            RUB -> 120_000.0
            SEK, NOK, DKK -> 25_000.0
            PLN -> 8_000.0
            CZK -> 45_000.0
            HUF -> 600_000.0
            ILS -> 9_000.0
            EGP -> 40_000.0
            NGN -> 800_000.0
            KES -> 150_000.0
            PKR -> 150_000.0
            BDT -> 100_000.0
            LKR -> 200_000.0
            NPR -> 100_000.0
            KWD -> 700.0
            QAR -> 8_000.0
            OMR -> 800.0
            BHD -> 800.0
            CLP -> 1_500_000.0
            COP -> 5_000_000.0
            ARS -> 1_500_000.0
            PEN -> 6_000.0
            TWD -> 60_000.0
            UAH -> 50_000.0
            KZT -> 600_000.0
            GHS -> 15_000.0
            MAD -> 15_000.0
        }

    /**
     * Dynamic quick-amount additive chips (+5, +10, etc.) tailored to this currency.
     */
    val quickPresets: List<Int>
        get() = when (this) {
            USD, EUR, GBP, CHF -> listOf(5, 10, 20, 50)
            CAD, AUD, NZD, SGD, HKD, QAR -> listOf(10, 20, 50, 100)
            KWD, BHD, OMR -> listOf(1, 5, 10, 20)
            AED, SAR, BRL, PLN, PEN, ILS, MYR -> listOf(20, 50, 100, 200)
            TRY, MXN, ZAR, SEK, NOK, DKK, CNY, EGP, GHS, MAD -> listOf(50, 100, 200, 500)
            INR, RUB, PHP, THB, BDT, NPR, PKR, LKR, CZK -> listOf(100, 500, 1000, 2000)
            JPY, TWD, UAH -> listOf(500, 1000, 5000, 10000)
            KRW, KES, NGN, HUF, KZT -> listOf(1000, 5000, 10000, 50000)
            IDR, VND, CLP, COP, ARS -> listOf(10000, 50000, 100000, 500000)
        }

    companion object {
        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return INR
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}
