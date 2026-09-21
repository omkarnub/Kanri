package com.omkarnub.kanri.ui.greeting

import com.omkarnub.kanri.util.CurrencyUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

object GreetingResolver {

    /**
     * Determines the TimeBucket according to local time:
     * - 05:00 - 11:59 Morning
     * - 12:00 - 16:59 Afternoon
     * - 17:00 - 20:59 Evening
     * - 21:00 - 23:59 Night
     * - 00:00 - 04:59 Late night
     */
    fun getTimeBucket(time: LocalTime): TimeBucket {
        val hour = time.hour
        return when (hour) {
            in 5..11 -> TimeBucket.Morning
            in 12..16 -> TimeBucket.Afternoon
            in 17..20 -> TimeBucket.Evening
            in 21..23 -> TimeBucket.Night
            else -> TimeBucket.LateNight
        }
    }

    /**
     * Computes a deterministic seed based on epoch day and time bucket.
     */
    fun computeSeed(epochDay: Long, bucket: TimeBucket): Long {
        var result = epochDay
        result = 31L * result + bucket.ordinal.toLong()
        return result
    }

    /**
     * Formats greeting template with userName, cleanly dropping the name if blank.
     */
    fun formatGreeting(template: String, userName: String): String {
        val cleanName = userName.trim()
        return if (cleanName.isNotBlank()) {
            template.replace("{name}", cleanName)
        } else {
            template
                .replace(", {name}", "")
                .replace(" {name}", "")
                .replace("{name}", "")
                .trim()
        }
    }

    /**
     * Resolves the greeting and subtitle deterministically from the given context.
     */
    fun resolve(context: GreetingContext): GreetingResult {
        val timeBucket = getTimeBucket(context.now.toLocalTime())
        val seed = if (context.seed != 0L) context.seed else computeSeed(context.now.toLocalDate().toEpochDay(), timeBucket)
        val rng = Random(seed)

        // 1. Pick Greeting variant
        val greeting = selectGreeting(timeBucket, context.userName, rng)

        // 2. Resolve Subtitle based on priority rules & anti-repetition
        val (subtitle, tone, ruleId, action) = resolveSubtitle(context, timeBucket, rng)

        return GreetingResult(
            greeting = greeting,
            subtitle = subtitle,
            tone = tone,
            ruleId = ruleId,
            action = action
        )
    }

    private fun selectGreeting(bucket: TimeBucket, userName: String, rng: Random): String {
        val (plainVariants, playfulVariants) = when (bucket) {
            TimeBucket.Morning -> listOf(
                "Good morning, {name}",
                "Morning, {name}"
            ) to listOf(
                "Rise and shine, {name}",
                "Fresh day, {name}"
            )
            TimeBucket.Afternoon -> listOf(
                "Good afternoon, {name}",
                "Afternoon, {name}"
            ) to listOf(
                "Hey {name}"
            )
            TimeBucket.Evening -> listOf(
                "Good evening, {name}",
                "Evening, {name}"
            ) to listOf(
                "Winding down, {name}?"
            )
            TimeBucket.Night -> listOf(
                "Good night, {name}",
                "Night, {name}"
            ) to listOf(
                "Late one, {name}?"
            )
            TimeBucket.LateNight -> listOf(
                "Still up, {name}?",
                "Burning the midnight oil, {name}?"
            ) to emptyList()
        }

        val template = if (playfulVariants.isEmpty()) {
            plainVariants[rng.nextInt(plainVariants.size)]
        } else {
            // 60% plain weight, 40% playful weight
            val isPlayful = (rng.nextInt(100) < 40)
            if (isPlayful) {
                playfulVariants[rng.nextInt(playfulVariants.size)]
            } else {
                plainVariants[rng.nextInt(plainVariants.size)]
            }
        }

        return formatGreeting(template, userName)
    }

    private data class SubtitleCandidate(
        val subtitle: String,
        val tone: Tone,
        val ruleId: Int,
        val action: GreetingAction = GreetingAction.None
    )

    private fun resolveSubtitle(
        context: GreetingContext,
        bucket: TimeBucket,
        rng: Random
    ): SubtitleCandidate {
        val todayEpochDay = context.now.toLocalDate().toEpochDay()
        val hasBudget = context.monthlyBudget != null && context.monthlyBudget > 0.0 && !context.monthlyBudget.isNaN()
        val allowFinanceRules = (abs(context.seed) % 100L < 40L)

        fun isSuppressed(candidate: SubtitleCandidate): Boolean {
            // Anti-repetition 1: Don't show the same finance rule two days in a row unless it is rule 1 (over budget).
            if (candidate.ruleId in 2..8) {
                if (context.lastSubtitleRuleId == candidate.ruleId &&
                    context.lastShownEpochDay != null &&
                    todayEpochDay == context.lastShownEpochDay + 1
                ) {
                    return true
                }
            }

            // Anti-repetition 2: Warnings appear at most once per time bucket.
            if (candidate.tone == Tone.Warning) {
                if (context.lastWarningEpochDay == todayEpochDay &&
                    context.lastWarningBucket.equals(bucket.name, ignoreCase = true)
                ) {
                    return true
                }
            }

            return false
        }

        // --- RULE 1: Over budget (High severity) ---
        if (hasBudget && context.spentThisMonth > context.monthlyBudget!!) {
            val diff = context.spentThisMonth - context.monthlyBudget
            val candidate = SubtitleCandidate(
                subtitle = "You've crossed your budget by ${CurrencyUtils.formatCompactCurrency(diff)}",
                tone = Tone.Warning,
                ruleId = 1,
                action = GreetingAction.OpenInsights
            )
            if (!isSuppressed(candidate)) return candidate
        }

        // --- RULE 2: Near limit (High severity) ---
        if (hasBudget && context.spentThisMonth >= 0.9 * context.monthlyBudget!! && context.spentThisMonth <= context.monthlyBudget) {
            val daysWord = if (context.daysLeftInMonth == 1) "1 day to go" else "${context.daysLeftInMonth} days to go"
            val candidate = SubtitleCandidate(
                subtitle = "90% of your budget is used, $daysWord",
                tone = Tone.Warning,
                ruleId = 2,
                action = GreetingAction.OpenInsights
            )
            if (!isSuppressed(candidate)) return candidate
        }

        // --- FINANCE-AWARE RULES (Rules 3-8, apply ~40% of the time) ---
        if (allowFinanceRules) {
            // --- RULE 3: Ahead of pace ---
            if (context.safeToSpendPerDay != null &&
                context.safeToSpendPerDay.isFinite() &&
                context.safeToSpendPerDay > 0.0 &&
                context.spentToday > context.safeToSpendPerDay
            ) {
                val diff = context.spentToday - context.safeToSpendPerDay
                val candidate = SubtitleCandidate(
                    subtitle = "${CurrencyUtils.formatCompactCurrency(diff)} above today's safe limit",
                    tone = Tone.Warning,
                    ruleId = 3,
                    action = GreetingAction.OpenInsights
                )
                if (!isSuppressed(candidate)) return candidate
            }

            // --- RULE 4: Milestone streak ---
            if (context.streakDays in listOf(3, 7, 14, 30)) {
                val candidate = SubtitleCandidate(
                    subtitle = "${context.streakDays}-day no-spend streak",
                    tone = Tone.Positive,
                    ruleId = 4,
                    action = GreetingAction.None
                )
                if (!isSuppressed(candidate)) return candidate
            }

            // --- RULE 5: Under pace and strong ---
            if (hasBudget &&
                context.dayOfMonth > 15 &&
                context.spentThisMonth <= 0.6 * context.monthlyBudget!!
            ) {
                val candidate = SubtitleCandidate(
                    subtitle = "Comfortably under budget this month",
                    tone = Tone.Positive,
                    ruleId = 5,
                    action = GreetingAction.None
                )
                if (!isSuppressed(candidate)) return candidate
            }

            // --- RULE 6: Better than last month ---
            if (context.lastMonthSpentSameDay != null &&
                context.lastMonthSpentSameDay > 0.0 &&
                context.spentThisMonth < context.lastMonthSpentSameDay * 0.9
            ) {
                val percentLess = (((context.lastMonthSpentSameDay - context.spentThisMonth) / context.lastMonthSpentSameDay) * 100)
                    .roundToInt()
                val candidate = SubtitleCandidate(
                    subtitle = "$percentLess% less than this time last month",
                    tone = Tone.Positive,
                    ruleId = 6,
                    action = GreetingAction.None
                )
                if (!isSuppressed(candidate)) return candidate
            }

            // --- RULE 7: Nothing spent today (afternoon or later) ---
            if (context.now.hour >= 12 && context.spentToday == 0.0) {
                val candidate = SubtitleCandidate(
                    subtitle = "No spends yet today",
                    tone = Tone.Neutral,
                    ruleId = 7,
                    action = GreetingAction.None
                )
                if (!isSuppressed(candidate)) return candidate
            }

            // --- RULE 8: Review pending ---
            if (context.needsReviewCount > 0) {
                val countText = if (context.needsReviewCount == 1) {
                    "1 transaction needs review"
                } else {
                    "${context.needsReviewCount} transactions need review"
                }
                val candidate = SubtitleCandidate(
                    subtitle = countText,
                    tone = Tone.Neutral,
                    ruleId = 8,
                    action = GreetingAction.OpenReviewSheet
                )
                if (!isSuppressed(candidate)) return candidate
            }
        }

        // --- CALENDAR RULES (Rules 9-10) ---
        // --- RULE 9: Month start (day 1-2) ---
        if (context.dayOfMonth in 1..2) {
            val candidate = SubtitleCandidate(
                subtitle = "New month, fresh budget",
                tone = Tone.Neutral,
                ruleId = 9,
                action = GreetingAction.None
            )
            if (!isSuppressed(candidate)) return candidate
        }

        // --- RULE 10: Month end (daysLeft <= 3) ---
        if (context.daysLeftInMonth <= 3 &&
            context.safeToSpendPerDay != null &&
            context.safeToSpendPerDay.isFinite() &&
            context.safeToSpendPerDay > 0.0
        ) {
            val dayWord = if (context.daysLeftInMonth == 1) "day" else "days"
            val candidate = SubtitleCandidate(
                subtitle = "${context.daysLeftInMonth} $dayWord left, ${CurrencyUtils.formatCompactCurrency(context.safeToSpendPerDay)}/day to spend",
                tone = Tone.Neutral,
                ruleId = 10,
                action = GreetingAction.None
            )
            if (!isSuppressed(candidate)) return candidate
        }

        // --- RULE 11: Fallback: the date ---
        val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
        return SubtitleCandidate(
            subtitle = context.now.format(dateFormatter),
            tone = Tone.Neutral,
            ruleId = 11,
            action = GreetingAction.None
        )
    }
}
