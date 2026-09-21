package com.omkarnub.kanri.ui.lending

import com.omkarnub.kanri.data.db.LendingEntity
import com.omkarnub.kanri.data.db.LendingRepaymentEntity
import com.omkarnub.kanri.data.db.LendingWithRepayments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class LendingHubViewModelTest {

    @Test
    fun testPersonNormalizationAndDisplayNameSelection() {
        val entry1 = LendingWithRepayments(
            lending = LendingEntity(
                id = 1L,
                personName = "  rahul  sharma ",
                amount = 200.0,
                type = "LENT",
                date = 1000L
            )
        )
        val entry2 = LendingWithRepayments(
            lending = LendingEntity(
                id = 2L,
                personName = "Rahul Sharma",
                amount = 500.0,
                type = "LENT",
                date = 2000L // newer date
            )
        )

        val people = LendingHubViewModel.aggregatePeople(
            records = listOf(entry1, entry2),
            startOfTodayMillis = 5000L
        )

        assertEquals(1, people.size)
        val person = people[0]
        assertEquals("rahul sharma", person.key)
        assertEquals("Rahul Sharma", person.displayName) // selected newer spelling
        assertEquals(700.0, person.toReceive, 0.001)
        assertEquals(0.0, person.toPay, 0.001)
        assertEquals(700.0, person.net, 0.001)
        assertEquals(2, person.openCount)
    }

    @Test
    fun testOverdueAndNeedsAttentionLogic() {
        // Fixed reference: 2026-10-15 12:00:00 UTC
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = cal.timeInMillis
        val startOfToday = LendingHubViewModel.getStartOfTodayMillis(nowMillis)

        // 1. Overdue entry: due 5 days ago (2026-10-10)
        val overdueDueDate = startOfToday - 5L * 86_400_000L
        val overdueRecord = LendingWithRepayments(
            lending = LendingEntity(
                id = 10L,
                personName = "Pooja",
                amount = 1000.0,
                type = "LENT",
                date = overdueDueDate - 1000L,
                dueDate = overdueDueDate,
                isSettled = false
            )
        )

        // 2. Due soon entry: due in 3 days (2026-10-18)
        val dueSoonDate = startOfToday + 3L * 86_400_000L
        val dueSoonRecord = LendingWithRepayments(
            lending = LendingEntity(
                id = 20L,
                personName = "Karan",
                amount = 400.0,
                type = "BORROWED",
                date = startOfToday - 1000L,
                dueDate = dueSoonDate,
                isSettled = false
            )
        )

        // 3. Normal entry: due in 20 days (2026-11-04)
        val farDueDate = startOfToday + 20L * 86_400_000L
        val farRecord = LendingWithRepayments(
            lending = LendingEntity(
                id = 30L,
                personName = "Meera",
                amount = 800.0,
                type = "LENT",
                date = startOfToday - 1000L,
                dueDate = farDueDate,
                isSettled = false
            )
        )

        // 4. Settled entry with past due date (should NOT be overdue or need attention)
        val settledPastRecord = LendingWithRepayments(
            lending = LendingEntity(
                id = 40L,
                personName = "Suresh",
                amount = 500.0,
                type = "LENT",
                date = overdueDueDate - 1000L,
                dueDate = overdueDueDate,
                isSettled = true
            )
        )

        val people = LendingHubViewModel.aggregatePeople(
            records = listOf(overdueRecord, dueSoonRecord, farRecord, settledPastRecord),
            startOfTodayMillis = startOfToday
        )

        val pooja = people.first { it.key == "pooja" }
        assertTrue(pooja.isOverdue)
        assertTrue(pooja.needsAttention)
        assertEquals(5, pooja.overdueDays)

        val karan = people.first { it.key == "karan" }
        assertFalse(karan.isOverdue)
        assertTrue(karan.isDueSoon)
        assertTrue(karan.needsAttention)

        val meera = people.first { it.key == "meera" }
        assertFalse(meera.isOverdue)
        assertFalse(meera.isDueSoon)
        assertFalse(meera.needsAttention)

        val suresh = people.first { it.key == "suresh" }
        assertFalse(suresh.isOverdue)
        assertFalse(suresh.needsAttention)
        assertEquals(0, suresh.openCount)
    }

    @Test
    fun testTimelineFiltering() {
        val startOfToday = 1726500000000L
        val lentOpen = LendingWithRepayments(
            lending = LendingEntity(id = 1L, personName = "A", amount = 100.0, type = "LENT", isSettled = false)
        )
        val borrowedOpen = LendingWithRepayments(
            lending = LendingEntity(id = 2L, personName = "B", amount = 200.0, type = "BORROWED", isSettled = false)
        )
        val settled = LendingWithRepayments(
            lending = LendingEntity(id = 3L, personName = "C", amount = 300.0, type = "LENT", isSettled = true)
        )
        val all = listOf(lentOpen, borrowedOpen, settled)

        assertEquals(3, LendingHubViewModel.filterTimelineRecords(all, TimelineFilter.ALL, startOfToday).size)
        val youLlGet = LendingHubViewModel.filterTimelineRecords(all, TimelineFilter.YOU_LL_GET, startOfToday)
        assertEquals(1, youLlGet.size)
        assertEquals(1L, youLlGet[0].lending.id)

        val youOwe = LendingHubViewModel.filterTimelineRecords(all, TimelineFilter.YOU_OWE, startOfToday)
        assertEquals(1, youOwe.size)
        assertEquals(2L, youOwe[0].lending.id)

        val settledList = LendingHubViewModel.filterTimelineRecords(all, TimelineFilter.SETTLED, startOfToday)
        assertEquals(1, settledList.size)
        assertEquals(3L, settledList[0].lending.id)
    }

    @Test
    fun testReminderMessageContainsRequiredFieldsAndNoEmojis() {
        val msg = LendingReminderUtils.buildReminderMessage(
            personName = "Aditya",
            outstandingAmount = 1500.0,
            originalDateMillis = 1726500000000L,
            dueDateMillis = 1727000000000L,
            currentTimeMillis = 1727500000000L // past due date
        )

        assertTrue(msg.contains("Aditya"))
        assertTrue(msg.contains("1,500"))
        assertTrue(msg.contains("gentle reminder"))
        assertTrue(msg.contains("due on"))
        assertFalse(msg.contains("Kanri")) // no branding
        // No emojis (surrogate pairs or emoji code points)
        for (ch in msg) {
            val type = Character.getType(ch)
            assertFalse(type == Character.SURROGATE.toInt() || type == Character.OTHER_SYMBOL.toInt())
        }
    }
}
