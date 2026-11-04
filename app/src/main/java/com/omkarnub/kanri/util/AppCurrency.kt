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
    INR(code = "INR", symbol = "₹", displayName = "Indian Rupee", localeTag = "en-IN"),
    USD(code = "USD", symbol = "$", displayName = "US Dollar", localeTag = "en-US"),
    EUR(code = "EUR", symbol = "€", displayName = "Euro", localeTag = "de-DE"),
    GBP(code = "GBP", symbol = "£", displayName = "British Pound", localeTag = "en-GB"),
    TRY(code = "TRY", symbol = "₺", displayName = "Turkish Lira", localeTag = "tr-TR"),
    AED(code = "AED", symbol = "AED", displayName = "UAE Dirham", localeTag = "ar-AE"),
    SAR(code = "SAR", symbol = "SAR", displayName = "Saudi Riyal", localeTag = "ar-SA"),
    CAD(code = "CAD", symbol = "CA$", displayName = "Canadian Dollar", localeTag = "en-CA"),
    AUD(code = "AUD", symbol = "A$", displayName = "Australian Dollar", localeTag = "en-AU"),
    JPY(code = "JPY", symbol = "¥", displayName = "Japanese Yen", localeTag = "ja-JP"),
    CNY(code = "CNY", symbol = "CN¥", displayName = "Chinese Yuan", localeTag = "zh-CN"),
    CHF(code = "CHF", symbol = "CHF", displayName = "Swiss Franc", localeTag = "de-CH"),
    SGD(code = "SGD", symbol = "S$", displayName = "Singapore Dollar", localeTag = "en-SG"),
    HKD(code = "HKD", symbol = "HK$", displayName = "Hong Kong Dollar", localeTag = "zh-HK"),
    NZD(code = "NZD", symbol = "NZ$", displayName = "New Zealand Dollar", localeTag = "en-NZ"),
    KRW(code = "KRW", symbol = "₩", displayName = "South Korean Won", localeTag = "ko-KR"),
    BRL(code = "BRL", symbol = "R$", displayName = "Brazilian Real", localeTag = "pt-BR"),
    MXN(code = "MXN", symbol = "Mex$", displayName = "Mexican Peso", localeTag = "es-MX"),
    IDR(code = "IDR", symbol = "Rp", displayName = "Indonesian Rupiah", localeTag = "id-ID"),
    MYR(code = "MYR", symbol = "RM", displayName = "Malaysian Ringgit", localeTag = "ms-MY"),
    THB(code = "THB", symbol = "฿", displayName = "Thai Baht", localeTag = "th-TH"),
    PHP(code = "PHP", symbol = "₱", displayName = "Philippine Peso", localeTag = "en-PH"),
    VND(code = "VND", symbol = "₫", displayName = "Vietnamese Dong", localeTag = "vi-VN"),
    ZAR(code = "ZAR", symbol = "R", displayName = "South African Rand", localeTag = "en-ZA"),
    RUB(code = "RUB", symbol = "₽", displayName = "Russian Ruble", localeTag = "ru-RU"),
    SEK(code = "SEK", symbol = "kr", displayName = "Swedish Krona", localeTag = "sv-SE"),
    NOK(code = "NOK", symbol = "kr", displayName = "Norwegian Krone", localeTag = "no-NO"),
    DKK(code = "DKK", symbol = "kr", displayName = "Danish Krone", localeTag = "da-DK"),
    PLN(code = "PLN", symbol = "zł", displayName = "Polish Zloty", localeTag = "pl-PL"),
    CZK(code = "CZK", symbol = "Kč", displayName = "Czech Koruna", localeTag = "cs-CZ"),
    HUF(code = "HUF", symbol = "Ft", displayName = "Hungarian Forint", localeTag = "hu-HU"),
    ILS(code = "ILS", symbol = "₪", displayName = "Israeli New Shekel", localeTag = "he-IL"),
    EGP(code = "EGP", symbol = "E£", displayName = "Egyptian Pound", localeTag = "ar-EG"),
    NGN(code = "NGN", symbol = "₦", displayName = "Nigerian Naira", localeTag = "en-NG"),
    KES(code = "KES", symbol = "KSh", displayName = "Kenyan Shilling", localeTag = "sw-KE"),
    PKR(code = "PKR", symbol = "₨", displayName = "Pakistani Rupee", localeTag = "ur-PK"),
    BDT(code = "BDT", symbol = "৳", displayName = "Bangladeshi Taka", localeTag = "bn-BD"),
    LKR(code = "LKR", symbol = "Rs", displayName = "Sri Lankan Rupee", localeTag = "si-LK"),
    NPR(code = "NPR", symbol = "रू", displayName = "Nepalese Rupee", localeTag = "ne-NP"),
    KWD(code = "KWD", symbol = "KD", displayName = "Kuwaiti Dinar", localeTag = "ar-KW"),
    QAR(code = "QAR", symbol = "QR", displayName = "Qatari Riyal", localeTag = "ar-QA"),
    OMR(code = "OMR", symbol = "RO", displayName = "Omani Rial", localeTag = "ar-OM"),
    BHD(code = "BHD", symbol = "BD", displayName = "Bahraini Dinar", localeTag = "ar-BH"),
    CLP(code = "CLP", symbol = "CLP$", displayName = "Chilean Peso", localeTag = "es-CL"),
    COP(code = "COP", symbol = "COL$", displayName = "Colombian Peso", localeTag = "es-CO"),
    ARS(code = "ARS", symbol = "ARS$", displayName = "Argentine Peso", localeTag = "es-AR"),
    PEN(code = "PEN", symbol = "S/.", displayName = "Peruvian Sol", localeTag = "es-PE"),
    TWD(code = "TWD", symbol = "NT$", displayName = "New Taiwan Dollar", localeTag = "zh-TW"),
    UAH(code = "UAH", symbol = "₴", displayName = "Ukrainian Hryvnia", localeTag = "uk-UA"),
    KZT(code = "KZT", symbol = "₸", displayName = "Kazakhstani Tenge", localeTag = "kk-KZ"),
    GHS(code = "GHS", symbol = "GH₵", displayName = "Ghanaian Cedi", localeTag = "en-GH"),
    MAD(code = "MAD", symbol = "MAD", displayName = "Moroccan Dirham", localeTag = "ar-MA");

    val locale: Locale
        get() = Locale.forLanguageTag(localeTag)

    companion object {
        fun fromCode(code: String?): AppCurrency {
            if (code.isNullOrBlank()) return INR
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: INR
        }
    }
}
