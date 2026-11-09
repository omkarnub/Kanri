package com.omkarnub.kanri.widget

import com.omkarnub.kanri.widget.data.WidgetDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoonPhaseTest {

    @Test
    fun testReferenceNewMoonEpoch() {
        val refTime = WidgetDataSource.NEW_MOON_REF_MILLIS
        val daysSinceRef = (refTime - WidgetDataSource.NEW_MOON_REF_MILLIS) / 86400000.0
        val phase = (((daysSinceRef % WidgetDataSource.SYNODIC_MONTH_DAYS) / WidgetDataSource.SYNODIC_MONTH_DAYS + 1.0) % 1.0).toFloat()

        assertEquals(0f, phase, 0.001f)
    }

    @Test
    fun testFullMoonHalfSynodicCycle() {
        // Half cycle after reference new moon should be approx 0.5 (Full Moon)
        val halfCycleMillis = (WidgetDataSource.SYNODIC_MONTH_DAYS / 2.0 * 86400000L).toLong()
        val testTime = WidgetDataSource.NEW_MOON_REF_MILLIS + halfCycleMillis

        val daysSinceRef = (testTime - WidgetDataSource.NEW_MOON_REF_MILLIS) / 86400000.0
        val phase = (((daysSinceRef % WidgetDataSource.SYNODIC_MONTH_DAYS) / WidgetDataSource.SYNODIC_MONTH_DAYS + 1.0) % 1.0).toFloat()

        assertEquals(0.5f, phase, 0.01f)
    }

    @Test
    fun testPhaseBoundedBetweenZeroAndOne() {
        val now = System.currentTimeMillis()
        val daysSinceRef = (now - WidgetDataSource.NEW_MOON_REF_MILLIS) / 86400000.0
        val phase = (((daysSinceRef % WidgetDataSource.SYNODIC_MONTH_DAYS) / WidgetDataSource.SYNODIC_MONTH_DAYS + 1.0) % 1.0).toFloat()

        assertTrue(phase in 0f..1f)
    }
}

