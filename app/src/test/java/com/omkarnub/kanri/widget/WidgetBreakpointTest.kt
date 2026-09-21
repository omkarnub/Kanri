package com.omkarnub.kanri.widget

import com.omkarnub.kanri.R
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetBreakpointTest {

    @Test
    fun testSmallLayoutResolution() {
        // Height under 100
        assertEquals(R.layout.widget_small, KanriAppWidgetProvider.resolveLayoutForDimensions(120, 70))
        assertEquals(R.layout.widget_small, KanriAppWidgetProvider.resolveLayoutForDimensions(200, 50))
        // Width under 150 and height under 150
        assertEquals(R.layout.widget_small, KanriAppWidgetProvider.resolveLayoutForDimensions(100, 120))
    }

    @Test
    fun testMediumLayoutResolution() {
        // Standard 3x2 cell or default size
        assertEquals(R.layout.widget_medium, KanriAppWidgetProvider.resolveLayoutForDimensions(250, 120))
        assertEquals(R.layout.widget_medium, KanriAppWidgetProvider.resolveLayoutForDimensions(180, 160))
        assertEquals(R.layout.widget_medium, KanriAppWidgetProvider.resolveLayoutForDimensions(0, 0))
    }

    @Test
    fun testLargeLayoutResolution() {
        // Height 180 or more (3+ rows tall)
        assertEquals(R.layout.widget_large, KanriAppWidgetProvider.resolveLayoutForDimensions(250, 180))
        assertEquals(R.layout.widget_large, KanriAppWidgetProvider.resolveLayoutForDimensions(300, 240))
        assertEquals(R.layout.widget_large, KanriAppWidgetProvider.resolveLayoutForDimensions(180, 200))
    }
}
