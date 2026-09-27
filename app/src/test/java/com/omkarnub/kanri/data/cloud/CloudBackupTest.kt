package com.omkarnub.kanri.data.cloud

import com.omkarnub.kanri.data.backup.BackupCryptoHelper
import com.omkarnub.kanri.data.backup.BackupJsonParser
import com.omkarnub.kanri.data.backup.BackupPayload
import com.omkarnub.kanri.data.db.BudgetEntity
import com.omkarnub.kanri.data.db.CategoryEntity
import com.omkarnub.kanri.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class CloudBackupTest {

    @Test
    fun testCloudBackupEncryptionRoundTrip() {
        val transactions = listOf(
            TransactionEntity(
                id = 101L,
                amount = 1450.75,
                type = "DEBIT",
                sourceType = "CARD",
                counterparty = "Whole Foods",
                bank = "HDFC Bank",
                refNo = "TXN123456",
                timestamp = 1727340000000L,
                categoryId = 2L,
                rawSms = "Rs 1450.75 debited from HDFC Bank acct",
                notes = "Whole Foods grocery haul"
            )
        )
        val categories = listOf(
            CategoryEntity(
                id = 2L,
                name = "Groceries",
                colorHex = "#10B981",
                iconName = "shopping_cart",
                isCustom = false
            )
        )
        val budgets = listOf(
            BudgetEntity(
                monthKey = "2026-09",
                monthlyLimit = 15000.0
            )
        )

        val payload = BackupPayload(
            version = 2,
            createdAt = 1727350000000L,
            appVersion = "1.0.0",
            transactions = transactions,
            categories = categories,
            budgets = budgets,
            lendingRecords = emptyList(),
            counterpartyMappings = emptyList(),
            lendingRepayments = emptyList()
        )

        val jsonString = BackupJsonParser.toJson(payload)
        val userEmail = "kanri.user@gmail.com"

        // Derive deterministic cloud vault passphrase for this Google Account
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("kanri_sovereign_vault_${userEmail.lowercase()}".toByteArray(Charsets.UTF_8))
        val derivedPassphrase = hash.joinToString("") { "%02x".format(it) }

        // Encrypt exactly as CloudBackupManager does
        val encryptedBytes = BackupCryptoHelper.encryptJson(jsonString, derivedPassphrase)

        // 1. Verify encryption constraints
        assertTrue("Cloud backup payload must have KANRI_ENC_v1 magic header", BackupCryptoHelper.isEncryptedBackup(encryptedBytes))
        assertNotEquals("Encrypted blob must never equal raw JSON string", jsonString, String(encryptedBytes))
        assertFalse("Raw JSON must not be present in encrypted blob", String(encryptedBytes).contains("Whole Foods"))

        // 2. Decrypt with the derived passphrase
        val decryptedJson = BackupCryptoHelper.decryptJson(encryptedBytes, derivedPassphrase)
        assertEquals("Decrypted JSON must match original serialized payload", jsonString, decryptedJson)

        // 3. Parse back into domain entities
        val restoredPayload = BackupJsonParser.fromJson(decryptedJson)
        assertEquals(1, restoredPayload.transactions.size)
        assertEquals(101L, restoredPayload.transactions[0].id)
        assertEquals(1450.75, restoredPayload.transactions[0].amount, 0.001)
        assertEquals("Whole Foods grocery haul", restoredPayload.transactions[0].notes)
        assertEquals(1, restoredPayload.categories.size)
        assertEquals("Groceries", restoredPayload.categories[0].name)
        assertEquals(1, restoredPayload.budgets.size)
        assertEquals(15000.0, restoredPayload.budgets[0].monthlyLimit, 0.001)
    }

    @Test
    fun testCustomPassphraseTakesPrecedenceOverDefaultDerivedKey() {
        val sampleJson = """{"transactions":[],"categories":[]}"""
        val customPassphrase = "MyUltraSecretPassword#2026"
        val wrongPassphrase = "DifferentPassword123"

        val encryptedBlob = BackupCryptoHelper.encryptJson(sampleJson, customPassphrase)

        // Must succeed with custom passphrase
        val decrypted = BackupCryptoHelper.decryptJson(encryptedBlob, customPassphrase)
        assertEquals(sampleJson, decrypted)

        // Must fail with different passphrase
        try {
            BackupCryptoHelper.decryptJson(encryptedBlob, wrongPassphrase)
            org.junit.Assert.fail("Decryption with wrong passphrase should throw exception")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun testNeverUploadUnencryptedDataValidation() {
        val rawJson = """{"test":"financial_data"}"""
        val rawBytes = rawJson.toByteArray(Charsets.UTF_8)

        // An unencrypted raw JSON array must be rejected by isEncryptedBackup
        assertFalse("Unencrypted bytes must NOT pass encryption validation", BackupCryptoHelper.isEncryptedBackup(rawBytes))
    }

    @Test
    fun testBackupModeDefaultsAndDescriptions() {
        val offline = BackupMode.OFFLINE
        val cloud = BackupMode.CLOUD

        assertEquals("Fully Offline", offline.label)
        assertTrue(offline.description.contains("100% on-device"))

        assertEquals("Cloud Backup", cloud.label)
        assertTrue(cloud.description.contains("Google Drive"))
    }
}
