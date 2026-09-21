package com.omkarnub.kanri.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {

    private val indianLocale = Locale.forLanguageTag("en-IN")

    private val currencyFormatterWithDecimals: NumberFormat by lazy {
        NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }

    private val currencyFormatterWithoutDecimals: NumberFormat by lazy {
        NumberFormat.getNumberInstance(indianLocale).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }

    /**
     * Standard Indian currency formatting with ₹ symbol and lakh/crore commas.
     * Example: formatCurrency(123456.78) -> "₹1,23,456.78"
     */
    fun formatCurrency(amount: Double, includeDecimals: Boolean = true): String {
        val formatter = if (includeDecimals) currencyFormatterWithDecimals else currencyFormatterWithoutDecimals
        return "₹${formatter.format(amount)}"
    }

    /**
     * Compact Indian currency abbreviation for large values.
     * Examples:
     * - 15,000,000 -> "₹1.5Cr"
     * - 120,000    -> "₹1.2L"
     * - 4,500      -> "₹4.5k"
     * - 450        -> "₹450"
     */
    fun formatCompactCurrency(amount: Double): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = abs(amount)
        val formatted = when {
            absAmount >= 10_000_000.0 -> {
                val cr = absAmount / 10_000_000.0
                "${String.format(Locale.US, "%.1f", cr).removeSuffix(".0")}Cr"
            }
            absAmount >= 100_000.0 -> {
                val lakh = absAmount / 100_000.0
                "${String.format(Locale.US, "%.1f", lakh).removeSuffix(".0")}L"
            }
            absAmount >= 1_000.0 -> {
                val k = absAmount / 1_000.0
                "${String.format(Locale.US, "%.1f", k).removeSuffix(".0")}k"
            }
            else -> {
                currencyFormatterWithoutDecimals.format(absAmount)
            }
        }
        return "${sign}₹$formatted"
    }
}
