package com.omkarnub.kanri.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar

class ExportSchedulerTest {

    @Test
    fun testExportScheduleEnumValues() {
        assertEquals("Off", ExportScheduler.ExportSchedule.OFF.label)
        assertEquals("Weekly", ExportScheduler.ExportSchedule.WEEKLY.label)
        assertNotNull(ExportScheduler.ExportSchedule.MONTHLY.label)
    }

    @Test
    fun testNextMonthFirstDelayCalculation() {
        val now = Calendar.getInstance()
        val targetFirst = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val delay = targetFirst.timeInMillis - now.timeInMillis
        org.junit.Assert.assertTrue("Delay should be positive", delay > 0)
        org.junit.Assert.assertTrue("Delay should be less than 32 days", delay <= 32L * 24 * 60 * 60 * 1000)
    }
}
