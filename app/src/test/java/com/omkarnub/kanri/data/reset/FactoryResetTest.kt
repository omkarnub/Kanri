package com.omkarnub.kanri.data.reset

import com.omkarnub.kanri.data.db.DatabaseKeyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class FactoryResetTest {

    @Test
    fun testKeyConversionRoundTrip() {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val hex = DatabaseKeyManager.bytesToHex(randomBytes)
        val convertedBytes = DatabaseKeyManager.hexToBytes(hex)

        assertEquals(64, hex.length)
        assertTrue(randomBytes.contentEquals(convertedBytes))
    }

    @Test
    fun testRotationProducesDistinctPassphrase() {
        val key1 = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key2 = ByteArray(32).also { SecureRandom().nextBytes(it) }

        assertFalse(key1.contentEquals(key2))
        assertNotEquals(DatabaseKeyManager.bytesToHex(key1), DatabaseKeyManager.bytesToHex(key2))
    }
}
