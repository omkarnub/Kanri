package com.omkarnub.kanri.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests verifying sample demo scenarios:
 * Checks Indian currency formatting, signs, gauge levels, and strict privacy/non-trademark guarantees.
 */
class DemoScenarioTest {

    private val scenarios = DemoScenarios.list

    @Test
    fun testExactlyThreeScenariosConfigured() {
        assertEquals("Demo must contain 6 scenarios", 6, scenarios.size)
    }

    @Test
    fun testAmountFormattingAndSigns() {
        val s1 = scenarios[0] // Debit 250
        val s2 = scenarios[1] // Credit 1200
        val s3 = scenarios[2] // Debit 500

        // Indian Currency formatting (INR commas, ₹ symbol)
        assertEquals("₹250", s1.formattedAmount(includeDecimals = false))
        assertEquals("+₹1,200", s2.formattedAmount(includeDecimals = false))
        assertEquals("₹500", s3.formattedAmount(includeDecimals = false))

        assertTrue("Scenario 1 must be a debit", s1.isDebit)
        assertFalse("Scenario 2 must be a credit", s2.isDebit)
        assertTrue("Scenario 3 must be a debit", s3.isDebit)
    }

    @Test
    fun testGaugeLevelsAndDeltasWithinRange() {
        assertEquals(0.75f, DemoScenarios.NEUTRAL_GAUGE_LEVEL, 0.001f)

        for (scenario in scenarios) {
            // Target level must be valid fraction between 0.0 and 1.0
            assertTrue(
                "Target gauge level must be between 0 and 1: ${scenario.targetGauge}",
                scenario.targetGauge in 0.0f..1.0f
            )

            // Baseline gauge level must be between 0 and 1
            assertTrue(
                "Baseline gauge level must be between 0 and 1: ${scenario.baselineGauge}",
                scenario.baselineGauge in 0.0f..1.0f
            )

            // Delta must be small realistic percentage (<= 15%)
            assertTrue(
                "Gauge delta must be within -1.0..1.0: ${scenario.gaugeDelta}",
                abs(scenario.gaugeDelta) <= 0.15f
            )

            // Delta polarity matches debit/credit
            if (scenario.isDebit) {
                assertTrue("Debit must decrease gauge", scenario.gaugeDelta < 0)
            } else {
                assertTrue("Credit must increase gauge", scenario.gaugeDelta > 0)
            }
        }
    }

    @Test
    fun testNoRealBankOrBrandTrademarks() {
        val prohibitedTrademarks = listOf(
            "hdfc", "sbi", "icici", "axis", "kotak", "pnb", "bob", "citibank",
            "paytm", "phonepe", "google pay", "amazon pay", "cred"
        )

        for (scenario in scenarios) {
            val combined = "${scenario.highlightPayee} ${scenario.highlightAmount} ${scenario.categoryIconName}".lowercase()

            for (forbidden in prohibitedTrademarks) {
                assertFalse(
                    "Fictional demo data must not include real bank or trademark name '$forbidden' in '$combined'",
                    combined.contains(forbidden)
                )
            }
        }
    }

    @Test
    fun testHighlightsAreNonEmpty() {
        for (scenario in scenarios) {
            assertTrue("Highlight payee must not be blank", scenario.highlightPayee.isNotBlank())
            assertTrue("Highlight amount must not be blank", scenario.highlightAmount.isNotBlank())
            assertTrue("Category icon key must not be blank", scenario.categoryIconName.isNotBlank())
        }
    }
}
