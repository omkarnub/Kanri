package com.omkarnub.kanri.ui.onboarding

import androidx.annotation.StringRes
import com.omkarnub.kanri.R
import com.omkarnub.kanri.util.CurrencyUtils

/**
 * State machine phases for the Onboarding Hero Demo stage.
 */
enum class DemoStageState {
    Idle,
    SmsEntering,
    SmsHolding,
    Morphing,
    CardLanded,
    GaugeReacting,
    Holding,
    Exiting
}

/**
 * Specification for a hard-coded fictional demo transaction scenario.
 */
data class DemoScenario(
    val id: Int,
    @param:StringRes val smsResId: Int,
    @param:StringRes val payeeResId: Int,
    @param:StringRes val categoryResId: Int,
    @param:StringRes val sourceResId: Int,
    @param:StringRes val semanticsResId: Int,
    val rawAmount: Double,
    val isDebit: Boolean,
    val categoryIconName: String,
    val highlightPayee: String,
    val highlightAmount: String,
    val baselineGauge: Float,
    val targetGauge: Float,
    val gaugeDelta: Float
) {
    /**
     * Formats the amount with Indian digit grouping and appropriate debit/credit prefix.
     */
    fun formattedAmount(includeDecimals: Boolean = false): String {
        val base = CurrencyUtils.formatCurrency(rawAmount, includeDecimals = includeDecimals)
        return if (isDebit) base else "+$base"
    }
}

/**
 * Master list of hard-coded, realistic Indian fictional demo scenarios.
 * Strictly 100% offline sample data with no real bank or merchant trademarks.
 */
object DemoScenarios {
    const val NEUTRAL_GAUGE_LEVEL = 0.75f

    val list: List<DemoScenario> = listOf(
        // 1. Debit: Chai Point, ₹250, Food & Drinks, red accent, gauge dips by 3%
        DemoScenario(
            id = 1,
            smsResId = R.string.onboarding_demo_sms_1,
            payeeResId = R.string.onboarding_demo_payee_1,
            categoryResId = R.string.onboarding_demo_category_1,
            sourceResId = R.string.onboarding_demo_source_1,
            semanticsResId = R.string.onboarding_demo_semantics_1,
            rawAmount = 250.0,
            isDebit = true,
            categoryIconName = "restaurant",
            highlightPayee = "Chai Point",
            highlightAmount = "Rs.250.00",
            baselineGauge = 0.75f,
            targetGauge = 0.72f,
            gaugeDelta = -0.03f
        ),

        // 2. Credit: Aarav K, ₹1,200, Received, green accent, gauge rises by 8%
        DemoScenario(
            id = 2,
            smsResId = R.string.onboarding_demo_sms_2,
            payeeResId = R.string.onboarding_demo_payee_2,
            categoryResId = R.string.onboarding_demo_category_2,
            sourceResId = R.string.onboarding_demo_source_2,
            semanticsResId = R.string.onboarding_demo_semantics_2,
            rawAmount = 1200.0,
            isDebit = false,
            categoryIconName = "payments",
            highlightPayee = "Aarav K",
            highlightAmount = "Rs.1,200.00",
            baselineGauge = 0.72f,
            targetGauge = 0.80f,
            gaugeDelta = 0.08f
        ),

        // 3. Debit: ATM withdrawal, ₹500, Cash, red accent, gauge dips by 5%
        DemoScenario(
            id = 3,
            smsResId = R.string.onboarding_demo_sms_3,
            payeeResId = R.string.onboarding_demo_payee_3,
            categoryResId = R.string.onboarding_demo_category_3,
            sourceResId = R.string.onboarding_demo_source_3,
            semanticsResId = R.string.onboarding_demo_semantics_3,
            rawAmount = 500.0,
            isDebit = true,
            categoryIconName = "local_atm",
            highlightPayee = "ATM",
            highlightAmount = "Rs.500.00",
            baselineGauge = 0.80f,
            targetGauge = 0.75f,
            gaugeDelta = -0.05f
        ),
        // 4. Debit: Swiggy order, ₹199, Food, red, gauge dips 2%
        DemoScenario(
            id = 4,
            smsResId = R.string.onboarding_demo_sms_4,
            payeeResId = R.string.onboarding_demo_payee_4,
            categoryResId = R.string.onboarding_demo_category_4,
            sourceResId = R.string.onboarding_demo_source_4,
            semanticsResId = R.string.onboarding_demo_semantics_4,
            rawAmount = 199.0,
            isDebit = true,
            categoryIconName = "restaurant",
            highlightPayee = "Swiggy",
            highlightAmount = "Rs.199.00",
            baselineGauge = 0.72f,
            targetGauge = 0.70f,
            gaugeDelta = -0.02f
        ),

        // 5. Credit: Salary, ₹45,000, green, gauge rises 12%
        DemoScenario(
            id = 5,
            smsResId = R.string.onboarding_demo_sms_5,
            payeeResId = R.string.onboarding_demo_payee_5,
            categoryResId = R.string.onboarding_demo_category_5,
            sourceResId = R.string.onboarding_demo_source_5,
            semanticsResId = R.string.onboarding_demo_semantics_5,
            rawAmount = 45000.0,
            isDebit = false,
            categoryIconName = "payments",
            highlightPayee = "Infosys Ltd",
            highlightAmount = "Rs.45,000.00",
            baselineGauge = 0.70f,
            targetGauge = 0.82f,
            gaugeDelta = 0.12f
        ),

        // 6. Debit: Jio recharge, ₹349, Utilities, red, gauge dips 4%
        DemoScenario(
            id = 6,
            smsResId = R.string.onboarding_demo_sms_6,
            payeeResId = R.string.onboarding_demo_payee_6,
            categoryResId = R.string.onboarding_demo_category_6,
            sourceResId = R.string.onboarding_demo_source_6,
            semanticsResId = R.string.onboarding_demo_semantics_6,
            rawAmount = 349.0,
            isDebit = true,
            categoryIconName = "bolt",
            highlightPayee = "Jio Recharge",
            highlightAmount = "Rs.349.00",
            baselineGauge = 0.82f,
            targetGauge = 0.78f,
            gaugeDelta = -0.04f
        )
    )
}
