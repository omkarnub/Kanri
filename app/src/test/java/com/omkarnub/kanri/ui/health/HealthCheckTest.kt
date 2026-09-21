package com.omkarnub.kanri.ui.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCheckTest {

    @Test
    fun testAllHealthyStatus() {
        val items = listOf(
            HealthCheckItem("sms", "SMS", "Healthy", true, "Action", true),
            HealthCheckItem("notification", "Notification", "Healthy", true, "Action", true),
            HealthCheckItem("overlay", "Overlay", "Healthy", true, "Action", false),
            HealthCheckItem("battery", "Battery", "Healthy", true, "Action", false)
        )
        val status = HealthStatus(items, hasCriticalIssue = false, hasAnyIssue = false, warningSummary = null)

        assertFalse(status.hasCriticalIssue)
        assertFalse(status.hasAnyIssue)
        assertNull(status.warningSummary)
    }

    @Test
    fun testCriticalIssueStatus() {
        val items = listOf(
            HealthCheckItem("sms", "SMS", "Missing", false, "Action", true),
            HealthCheckItem("notification", "Notification", "Healthy", true, "Action", true)
        )
        val status = HealthStatus(
            items,
            hasCriticalIssue = true,
            hasAnyIssue = true,
            warningSummary = "SMS permission missing: Bank debit/credit messages cannot be read."
        )

        assertTrue(status.hasCriticalIssue)
        assertTrue(status.hasAnyIssue)
        assertNotNull(status.warningSummary)
        assertEquals("SMS permission missing: Bank debit/credit messages cannot be read.", status.warningSummary)
    }
}
