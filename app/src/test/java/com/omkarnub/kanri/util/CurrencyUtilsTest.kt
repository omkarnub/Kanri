package com.omkarnub.kanri.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CurrencyUtilsTest {

    @Before
    fun setUp() {
        CurrencyUtils.setCurrency(AppCurrency.INR)
    }

    @After
    fun tearDown() {
        CurrencyUtils.setCurrency(AppCurrency.INR)
    }

    @Test
    fun testFormatCurrencyInrAndHalfUpRounding() {
        assertEquals("₹1,23,456.78", CurrencyUtils.formatCurrency(123456.78, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹1,23,457", CurrencyUtils.formatCurrency(123456.78, includeDecimals = false, currency = AppCurrency.INR))
        assertEquals("₹12,34,567.89", CurrencyUtils.formatCurrency(1234567.89, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹1,23,45,678", CurrencyUtils.formatCurrency(12345678.0, includeDecimals = false, currency = AppCurrency.INR))
        assertEquals("₹999.99", CurrencyUtils.formatCurrency(999.99, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹12.35", CurrencyUtils.formatCurrency(12.345, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹12.34", CurrencyUtils.formatCurrency(12.344, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("-₹500.00", CurrencyUtils.formatCurrency(-500.0, includeDecimals = true, currency = AppCurrency.INR))
    }

    @Test
    fun testFormatCurrencyMultipleGlobalCurrencies() {
        assertEquals("$123,456.78", CurrencyUtils.formatCurrency(123456.78, includeDecimals = true, currency = AppCurrency.USD))
        assertEquals("€123.456,78", CurrencyUtils.formatCurrency(123456.78, includeDecimals = true, currency = AppCurrency.EUR))
        assertEquals("£1,250.50", CurrencyUtils.formatCurrency(1250.50, includeDecimals = true, currency = AppCurrency.GBP))
        assertEquals("₺123.456,78", CurrencyUtils.formatCurrency(123456.78, includeDecimals = true, currency = AppCurrency.TRY))
        val jpyFormatted = CurrencyUtils.formatCurrency(5000.0, includeDecimals = false, currency = AppCurrency.JPY)
        assertTrue(jpyFormatted.startsWith("¥"))
        assertTrue(jpyFormatted.contains("5,000") || jpyFormatted.contains("5000"))
        val aedFormatted = CurrencyUtils.formatCurrency(99.5, includeDecimals = true, currency = AppCurrency.AED)
        assertTrue(aedFormatted.startsWith("AED"))
        val chfFormatted = CurrencyUtils.formatCurrency(1250.5, includeDecimals = true, currency = AppCurrency.CHF)
        assertTrue(chfFormatted.startsWith("CHF"))
    }

    @Test
    fun testNegativeZeroAndNonFiniteGuards() {
        assertEquals("₹0.00", CurrencyUtils.formatCurrency(-0.0, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹0.00", CurrencyUtils.formatCurrency(-0.001, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹0", CurrencyUtils.formatCurrency(-0.1, includeDecimals = false, currency = AppCurrency.INR))
        assertEquals("₹0.00", CurrencyUtils.formatCurrency(Double.NaN, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹0.00", CurrencyUtils.formatCurrency(Double.POSITIVE_INFINITY, includeDecimals = true, currency = AppCurrency.INR))
        assertEquals("₹0.00", CurrencyUtils.formatCurrency(Double.NEGATIVE_INFINITY, includeDecimals = true, currency = AppCurrency.INR))
        assertFalse(CurrencyUtils.formatCurrency(-0.004, includeDecimals = true, currency = AppCurrency.USD).startsWith("-"))
    }

    @Test
    fun testCompactCurrencyInrVsGlobal() {
        assertEquals("₹1.5Cr", CurrencyUtils.formatCompactCurrency(15_000_000.0, AppCurrency.INR))
        assertEquals("₹1.2L", CurrencyUtils.formatCompactCurrency(120_000.0, AppCurrency.INR))
        assertEquals("₹4.5k", CurrencyUtils.formatCompactCurrency(4_500.0, AppCurrency.INR))
        assertEquals("-₹500", CurrencyUtils.formatCompactCurrency(-500.0, AppCurrency.INR))
        assertEquals("₹0", CurrencyUtils.formatCompactCurrency(-0.1, AppCurrency.INR))

        assertEquals("$1.5B", CurrencyUtils.formatCompactCurrency(1_500_000_000.0, AppCurrency.USD))
        assertEquals("$15M", CurrencyUtils.formatCompactCurrency(15_000_000.0, AppCurrency.USD))
        assertEquals("$120k", CurrencyUtils.formatCompactCurrency(120_000.0, AppCurrency.USD))
        assertEquals("€4.5k", CurrencyUtils.formatCompactCurrency(4_500.0, AppCurrency.EUR))
        assertEquals("$0", CurrencyUtils.formatCompactCurrency(Double.NaN, AppCurrency.USD))
    }

    @Test
    fun testActiveCurrencyStateSync() {
        CurrencyUtils.setCurrency(AppCurrency.USD)
        assertEquals(AppCurrency.USD, CurrencyUtils.getCurrency())
        assertEquals("$", CurrencyUtils.getCurrencySymbol())
        assertEquals("USD", CurrencyUtils.getCurrencyCode())
        assertEquals("$42.00", CurrencyUtils.formatCurrency(42.0))
    }
}

