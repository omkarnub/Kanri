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

    private fun formatIndianNumber(value: java.math.BigDecimal): String {
        val plain = value.toPlainString()
        val dotIndex = plain.indexOf('.')
        val intPart = if (dotIndex >= 0) plain.substring(0, dotIndex) else plain
        val fracPart = if (dotIndex >= 0) plain.substring(dotIndex) else ""
        val groupedInt = if (intPart.length <= 3) {
            intPart
        } else {
            val lastThree = intPart.takeLast(3)
            val restGrouped = intPart.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$restGrouped,$lastThree"
        }
        return "$groupedInt$fracPart"
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
        val formatter = getFormatter(includeDecimals, currency)
        val fractionDigits = formatter.maximumFractionDigits.coerceAtLeast(0)
        fun formatAbs(value: java.math.BigDecimal): String {
            return if (currency == AppCurrency.INR) {
                formatIndianNumber(value)
            } else {
                formatter.format(value)
            }
        }
        val zeroScaled = java.math.BigDecimal.ZERO.setScale(fractionDigits, java.math.RoundingMode.HALF_UP)
        if (amount.isNaN() || amount.isInfinite()) {
            return "$symbol${formatAbs(zeroScaled)}"
        }
        val roundedAbs = java.math.BigDecimal.valueOf(abs(amount))
            .setScale(fractionDigits, java.math.RoundingMode.HALF_UP)
        if (roundedAbs.compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "$symbol${formatAbs(zeroScaled)}"
        }
        val formattedAbs = formatAbs(roundedAbs)
        return if (amount < 0) "-$symbol$formattedAbs" else "$symbol$formattedAbs"
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
        val symbol = currency.symbol
        if (amount.isNaN() || amount.isInfinite()) return "${symbol}0"
        val absAmount = abs(amount)
        if (absAmount < 1.0 &&
            java.math.BigDecimal.valueOf(absAmount).setScale(0, java.math.RoundingMode.HALF_UP)
                .compareTo(java.math.BigDecimal.ZERO) == 0
        ) {
            return "${symbol}0"
        }
        val sign = if (amount < 0) "-" else ""

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
