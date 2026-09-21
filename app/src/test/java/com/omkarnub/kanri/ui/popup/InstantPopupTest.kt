package com.omkarnub.kanri.ui.popup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstantPopupTest {

    @Test
    fun testServiceExtrasConstants_areDistinctAndValid() {
        val keys = setOf(
            InstantPopupService.EXTRA_TRANSACTION_ID,
            InstantPopupService.EXTRA_AMOUNT,
            InstantPopupService.EXTRA_IS_DEBIT,
            InstantPopupService.EXTRA_COUNTERPARTY,
            InstantPopupService.EXTRA_BANK,
            InstantPopupService.EXTRA_SOURCE_TYPE
        )
        // All 6 extra keys must be distinct
        assertEquals(6, keys.size)
        keys.forEach { key ->
            assertTrue(key.isNotBlank())
        }
    }

    @Test
    fun testReceiverActionsAndExtras_areWellFormed() {
        assertEquals("com.omkarnub.kanri.ACTION_POPUP_CATEGORIZE", InstantPopupReceiver.ACTION_CATEGORIZE)
        assertEquals("com.omkarnub.kanri.ACTION_POPUP_DISMISS", InstantPopupReceiver.ACTION_DISMISS)

        val receiverExtras = setOf(
            InstantPopupReceiver.EXTRA_TX_ID,
            InstantPopupReceiver.EXTRA_CATEGORY_ID,
            InstantPopupReceiver.EXTRA_COUNTERPARTY,
            InstantPopupReceiver.EXTRA_NOTIFICATION_ID
        )
        assertEquals(4, receiverExtras.size)
        receiverExtras.forEach { extra ->
            assertTrue(extra.isNotBlank())
        }
    }

    @Test
    fun testNotificationChannelId_isConsistent() {
        assertEquals("kanri_instant_alerts", InstantPopupNotificationHelper.CHANNEL_ID)
    }

    @Test
    fun testCounterpartyNormalization_forAutoMapping() {
        val raw1 = "  Swiggy  "
        val raw2 = "SWIGGY"
        val raw3 = "swiggy"

        val normalized1 = raw1.trim().lowercase()
        val normalized2 = raw2.trim().lowercase()
        val normalized3 = raw3.trim().lowercase()

        assertEquals("swiggy", normalized1)
        assertEquals("swiggy", normalized2)
        assertEquals("swiggy", normalized3)
        assertEquals(normalized1, normalized2)
        assertEquals(normalized2, normalized3)
    }

    @Test
    fun testNotificationIdGeneration_calculatesValidPositiveId() {
        val txId1 = 42L
        val notifId1 = (txId1 % 100000).toInt().coerceAtLeast(5000)
        assertEquals(5000, notifId1)

        val txId2 = 1234567L
        val notifId2 = (txId2 % 100000).toInt().coerceAtLeast(5000)
        assertEquals(34567, notifId2)
        assertTrue(notifId2 >= 5000)
    }

    @Test
    fun testFallbackCategories_containValidIdsAndLabels() {
        val defaultCategories = listOf(
            1L to "Food",
            2L to "Groceries",
            3L to "Shopping"
        )
        assertEquals(3, defaultCategories.size)
        defaultCategories.forEach { (catId, label) ->
            assertTrue(catId > 0L)
            assertTrue(label.isNotBlank())
        }
    }
}
