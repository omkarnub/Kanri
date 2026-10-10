package com.omkarnub.kanri.data.backup

import com.omkarnub.kanri.data.db.TransactionEntity
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun testEncryptDecryptRoundTripWithWalletPayload() {
        val payload = BackupPayload(
            version = 3,
            createdAt = System.currentTimeMillis(),
            appVersion = "1.1.0",
            transactions = listOf(
                TransactionEntity(
                    id = 101L,
                    type = "DEBIT",
                    amount = 750.0,
                    sourceType = "UPI",
                    wallet = "ONLINE",
                    transferToWallet = null,
                    timestamp = 1726500000000L
                )
            ),
            categories = emptyList(),
            budgets = emptyList(),
            lendingRecords = emptyList(),
            counterpartyMappings = emptyList(),
            walletBalances = listOf(
                com.omkarnub.kanri.data.db.WalletBalanceEntity("CASH", 5000.0, 1726400000000L),
                com.omkarnub.kanri.data.db.WalletBalanceEntity("ONLINE", 25000.0, 1726400000000L)
            )
        )
        val json = BackupJsonParser.toJson(payload)
        val password = "StrongWalletBackupPassword123!"

        val encrypted = BackupCryptoHelper.encryptJson(json, password)
        assertTrue(BackupCryptoHelper.isEncryptedBackup(encrypted))

        val decrypted = BackupCryptoHelper.decryptJson(encrypted, password)
        val restored = BackupJsonParser.fromJson(decrypted)

        assertEquals(1, restored.transactions.size)
        assertEquals("ONLINE", restored.transactions[0].wallet)
        assertEquals(2, restored.walletBalances.size)
        assertEquals(5000.0, restored.walletBalances.first { it.walletId == "CASH" }.openingAmount, 0.001)
    }
}
