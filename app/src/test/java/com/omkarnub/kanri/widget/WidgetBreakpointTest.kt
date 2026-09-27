package com.omkarnub.kanri.widget

import com.omkarnub.kanri.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBreakpointTest {

    @Test
    fun testWidgetActionPrefixes() {
        assertTrue(KanriWidgetActions.ACTION_ADD_EXPENSE.startsWith("com.omkarnub.kanri.action."))
        assertTrue(KanriWidgetActions.ACTION_ADD_INCOME.startsWith("com.omkarnub.kanri.action."))
        assertTrue(KanriWidgetActions.ACTION_SPLIT_BILL.startsWith("com.omkarnub.kanri.action."))
        assertTrue(KanriWidgetActions.ACTION_OPEN_GOALS.startsWith("com.omkarnub.kanri.action."))
        assertTrue(KanriWidgetActions.ACTION_OPEN_LENDING.startsWith("com.omkarnub.kanri.action."))
        assertTrue(KanriWidgetActions.ACTION_OPEN_HOME.startsWith("com.omkarnub.kanri.action."))
    }

    @Test
    fun testWidgetActionConsumption() {
        KanriWidgetActions.handleAction(KanriWidgetActions.ACTION_ADD_EXPENSE)
        assertEquals(KanriWidgetActions.ACTION_ADD_EXPENSE, KanriWidgetActions.pendingAction.value)

        val consumed = KanriWidgetActions.consumeAction()
        assertEquals(KanriWidgetActions.ACTION_ADD_EXPENSE, consumed)
        assertNull(KanriWidgetActions.pendingAction.value)
    }

    @Test
    fun testWidgetLayoutResourcesExist() {
        assertTrue(R.layout.widget_quick_add != 0)
        assertTrue(R.layout.widget_budget_ring != 0)
        assertTrue(R.layout.widget_budget_progress != 0)
        assertTrue(R.layout.widget_goals_ring != 0)
        assertTrue(R.layout.widget_lend_borrow != 0)
        assertTrue(R.layout.widget_spent_today != 0)
        assertTrue(R.layout.widget_split_bill != 0)
    }
}

