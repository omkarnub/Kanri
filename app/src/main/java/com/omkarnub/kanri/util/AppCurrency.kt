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

    companion object {
        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return INR
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}
