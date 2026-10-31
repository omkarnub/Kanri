package com.omkarnub.kanri.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {

    @Volatile
    private var activeCurrency: AppCurrency = AppCurrency.INR

    fun setCurrency(currency: AppCurrency) {
        activeCurrency = currency
    }

    fun getCurrency(): AppCurrency = activeCurrency

    fun getCurrencySymbol(): String = activeCurrency.symbol

    fun getCurrencyCode(): String = activeCurrency.code

    private fun getFormatter(includeDecimals: Boolean, currency: AppCurrency): NumberFormat {
        return NumberFormat.getNumberInstance(currency.locale).apply {
            minimumFractionDigits = if (includeDecimals) 2 else 0
            maximumFractionDigits = if (includeDecimals) 2 else 0
        }
    }

    /**
     * Standard currency formatting with active currency symbol and locale-appropriate grouping.
     * Examples:
     * - INR: formatCurrency(123456.78) -> "₹1,23,456.78"
     * - USD: formatCurrency(123456.78) -> "$123,456.78"
     * - TRY: formatCurrency(123456.78) -> "₺123.456,78"
     */
    fun formatCurrency(
        amount: Double,
        includeDecimals: Boolean = true,
        currency: AppCurrency = activeCurrency
    ): String {
        val symbol = currency.symbol
        if (amount.isNaN() || amount.isInfinite() || abs(amount) < 0.0001) {
            return if (includeDecimals) "${symbol}0.00" else "${symbol}0"
        }
        val formatter = getFormatter(includeDecimals, currency)
        return if (amount < 0) "-$symbol${formatter.format(abs(amount))}" else "$symbol${formatter.format(amount)}"
    }

    /**
     * Compact currency abbreviation for large values.
     * Examples (INR):
     * - 15,000,000 -> "₹1.5Cr"
     * - 120,000    -> "₹1.2L"
     * - 4,500      -> "₹4.5k"
     * - 450        -> "₹450"
     *
     * Examples (USD / TRY / Global):
     * - 15,000,000 -> "$15M" / "₺15M"
     * - 120,000    -> "$120k" / "₺120k"
     * - 4,500      -> "$4.5k" / "₺4.5k"
     * - 450        -> "$450" / "₺450"
     */
    fun formatCompactCurrency(
        amount: Double,
        currency: AppCurrency = activeCurrency
    ): String {
        val sign = if (amount < 0) "-" else ""
        val absAmount = abs(amount)
        val symbol = currency.symbol

        val formatted = if (currency == AppCurrency.INR) {
            when {
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
                    getFormatter(false, currency).format(absAmount)
                }
            }
        } else {
            // Standard international notation for USD, TRY, EUR, GBP
            when {
                absAmount >= 1_000_000_000.0 -> {
                    val b = absAmount / 1_000_000_000.0
                    "${String.format(Locale.US, "%.1f", b).removeSuffix(".0")}B"
                }
                absAmount >= 1_000_000.0 -> {
                    val m = absAmount / 1_000_000.0
                    "${String.format(Locale.US, "%.1f", m).removeSuffix(".0")}M"
                }
                absAmount >= 1_000.0 -> {
                    val k = absAmount / 1_000.0
                    "${String.format(Locale.US, "%.1f", k).removeSuffix(".0")}k"
                }
                else -> {
                    getFormatter(false, currency).format(absAmount)
                }
            }
        }
        return "${sign}$symbol$formatted"
    }
}
