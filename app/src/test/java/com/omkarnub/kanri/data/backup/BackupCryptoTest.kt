package com.omkarnub.kanri.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.AEADBadTagException

class BackupCryptoTest {

    @Test
    fun testEncryptDecryptRoundTrip() {
        val sampleJson = """{"version":1,"exportedAt":"2026-09-18T10:00:00Z","data":{"transactions":[{"id":1,"amount":450.0}]}}"""
        val passphrase = "MySecretStrongPassword#2026"

        val encryptedBytes = BackupCryptoHelper.encryptJson(sampleJson, passphrase)

        // Verify magic header is present
        assertTrue("Magic header should be detected", BackupCryptoHelper.isEncryptedBackup(encryptedBytes))
        assertNotEquals("Encrypted bytes should not equal plain JSON", sampleJson, String(encryptedBytes))

        // Decrypt with correct passphrase
        val decryptedJson = BackupCryptoHelper.decryptJson(encryptedBytes, passphrase)
        assertEquals("Decrypted JSON should match original", sampleJson, decryptedJson)
    }

    @Test(expected = Exception::class)
    fun testWrongPassphraseRejection() {
        val sampleJson = """{"data":{"notes":"confidential"}}"""
        val correctPass = "CorrectPassword123"
        val wrongPass = "WrongPassword999"

        val encryptedBytes = BackupCryptoHelper.encryptJson(sampleJson, correctPass)

        // Decrypting with wrong passphrase must fail with AEADBadTagException / general security exception
        BackupCryptoHelper.decryptJson(encryptedBytes, wrongPass)
    }

    @Test
    fun testLegacyPlainJsonCompatibility() {
        val legacyJson = """{"version":1,"transactions":[]}"""
        val legacyBytes = legacyJson.toByteArray(Charsets.UTF_8)

        // Unencrypted backup should NOT be identified as encrypted
        assertFalse(BackupCryptoHelper.isEncryptedBackup(legacyBytes))

        // Decrypting plain bytes returns original plaintext
        val result = BackupCryptoHelper.decryptJson(legacyBytes, "anyPassphrase")
        assertEquals(legacyJson, result)
    }

    @Test
    fun testCorruptedPayloadThrowsException() {
        val corruptedBytes = "KANRI_ENC_v1short".toByteArray(Charsets.US_ASCII)
        try {
            BackupCryptoHelper.decryptJson(corruptedBytes, "password")
            org.junit.Assert.fail("Expected exception for corrupted payload")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Corrupted") == true)
        }
    }
}
