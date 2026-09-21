package com.omkarnub.kanri.data.backup

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoHelper {

    private const val MAGIC_HEADER_STRING = "KANRI_ENC_v1"
    val MAGIC_HEADER = MAGIC_HEADER_STRING.toByteArray(StandardCharsets.US_ASCII)
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128
    private const val ITERATIONS = 65536
    private const val KEY_LENGTH_BITS = 256

    fun isEncryptedBackup(bytes: ByteArray): Boolean {
        if (bytes.size < MAGIC_HEADER.size) return false
        for (i in MAGIC_HEADER.indices) {
            if (bytes[i] != MAGIC_HEADER[i]) return false
        }
        return true
    }

    fun encryptJson(jsonString: String, passphrase: String): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val random = SecureRandom()
        random.nextBytes(salt)
        random.nextBytes(iv)

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val plainBytes = jsonString.toByteArray(StandardCharsets.UTF_8)
        val cipherText = cipher.doFinal(plainBytes)

        val buffer = ByteBuffer.allocate(MAGIC_HEADER.size + SALT_LENGTH + IV_LENGTH + cipherText.size)
        buffer.put(MAGIC_HEADER)
        buffer.put(salt)
        buffer.put(iv)
        buffer.put(cipherText)

        return buffer.array()
    }

    fun decryptJson(encryptedBytes: ByteArray, passphrase: String): String {
        if (!isEncryptedBackup(encryptedBytes)) {
            // Not encrypted with our magic header — return raw UTF-8 string
            return String(encryptedBytes, StandardCharsets.UTF_8)
        }

        val headerSize = MAGIC_HEADER.size
        val saltOffset = headerSize
        val ivOffset = saltOffset + SALT_LENGTH
        val cipherOffset = ivOffset + IV_LENGTH

        if (encryptedBytes.size < cipherOffset) {
            throw IllegalArgumentException("Corrupted backup file: payload is too small.")
        }

        val salt = encryptedBytes.copyOfRange(saltOffset, saltOffset + SALT_LENGTH)
        val iv = encryptedBytes.copyOfRange(ivOffset, ivOffset + IV_LENGTH)
        val cipherText = encryptedBytes.copyOfRange(cipherOffset, encryptedBytes.size)

        val secretKey = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val keySpec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
