package com.omkarnub.kanri.data.db

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

object DatabaseKeyManager {

    private const val PREFS_FILE = "kanri_secure_db_prefs"
    private const val KEY_DB_PASSPHRASE = "kanri_db_passphrase"

    @Synchronized
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val existingHex = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (!existingHex.isNullOrBlank()) {
            return hexToBytes(existingHex)
        }

        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val newHex = bytesToHex(randomBytes)

        prefs.edit().putString(KEY_DB_PASSPHRASE, newHex).apply()
        return randomBytes
    }

    @Synchronized
    fun rotatePassphrase(context: Context): ByteArray {
        val prefs = getEncryptedPrefs(context)
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val newHex = bytesToHex(randomBytes)
        prefs.edit().putString(KEY_DB_PASSPHRASE, newHex).commit()
        return randomBytes
    }

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}
