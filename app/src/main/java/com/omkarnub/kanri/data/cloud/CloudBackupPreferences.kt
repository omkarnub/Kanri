package com.omkarnub.kanri.data.cloud

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class BackupMode(val label: String, val description: String) {
    OFFLINE(
        label = "Fully Offline",
        description = "100% on-device storage. No account needed, zero cloud sync."
    ),
    CLOUD(
        label = "Cloud Backup",
        description = "Encrypted backup automatically saved to your personal Google Drive."
    )
}

enum class CloudBackupStatus {
    IDLE,
    IN_PROGRESS,
    SUCCESS,
    ERROR
}

enum class CloudBackupErrorType(val userMessage: String, val actionLabel: String) {
    TOKEN_EXPIRED(
        userMessage = "Google session expired. Please sign in again.",
        actionLabel = "Sign In"
    ),
    PERMISSION_REVOKED(
        userMessage = "Google Drive access was revoked. Please re-grant permission.",
        actionLabel = "Grant Access"
    ),
    QUOTA_EXCEEDED(
        userMessage = "Your Google Drive storage is full. Free up space to continue backups.",
        actionLabel = "Check Storage"
    ),
    NETWORK_ERROR(
        userMessage = "Network unreachable. Backup will retry when connection is restored.",
        actionLabel = "Retry"
    ),
    DECRYPTION_FAILED(
        userMessage = "Incorrect backup passphrase or corrupted cloud file.",
        actionLabel = "Enter Passphrase"
    ),
    GENERAL_ERROR(
        userMessage = "Backup failed due to an unexpected error.",
        actionLabel = "Details"
    )
}

class CloudBackupPreferences(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for test environments or legacy keystores
            prefs
        }
    }

    private val _backupModeFlow = MutableStateFlow(loadBackupMode())
    val backupModeFlow: StateFlow<BackupMode> = _backupModeFlow.asStateFlow()

    private val _accountEmailFlow = MutableStateFlow(prefs.getString(KEY_ACCOUNT_EMAIL, null))
    val accountEmailFlow: StateFlow<String?> = _accountEmailFlow.asStateFlow()

    private val _accountNameFlow = MutableStateFlow(prefs.getString(KEY_ACCOUNT_NAME, null))
    val accountNameFlow: StateFlow<String?> = _accountNameFlow.asStateFlow()

    private val _lastBackupTimestampFlow = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L))
    val lastBackupTimestampFlow: StateFlow<Long> = _lastBackupTimestampFlow.asStateFlow()

    private val _lastBackupStatusFlow = MutableStateFlow(loadBackupStatus())
    val lastBackupStatusFlow: StateFlow<CloudBackupStatus> = _lastBackupStatusFlow.asStateFlow()

    private val _lastErrorMessageFlow = MutableStateFlow(prefs.getString(KEY_LAST_ERROR_MESSAGE, null))
    val lastErrorMessageFlow: StateFlow<String?> = _lastErrorMessageFlow.asStateFlow()

    private val _lastErrorTypeFlow = MutableStateFlow(loadErrorType())
    val lastErrorTypeFlow: StateFlow<CloudBackupErrorType?> = _lastErrorTypeFlow.asStateFlow()

    private val _driveFileIdFlow = MutableStateFlow(prefs.getString(KEY_DRIVE_FILE_ID, null))
    val driveFileIdFlow: StateFlow<String?> = _driveFileIdFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "kanri_cloud_backup_prefs"
        private const val SECURE_PREFS_NAME = "kanri_cloud_backup_secure_prefs"

        private const val KEY_BACKUP_MODE = "key_backup_mode"
        private const val KEY_ACCOUNT_EMAIL = "key_account_email"
        private const val KEY_ACCOUNT_NAME = "key_account_name"
        private const val KEY_ACCOUNT_ID = "key_account_id"
        private const val KEY_LAST_BACKUP_TIMESTAMP = "key_last_backup_timestamp"
        private const val KEY_LAST_BACKUP_STATUS = "key_last_backup_status"
        private const val KEY_LAST_ERROR_MESSAGE = "key_last_error_message"
        private const val KEY_LAST_ERROR_TYPE = "key_last_error_type"
        private const val KEY_DRIVE_FILE_ID = "key_drive_file_id"
        private const val KEY_CUSTOM_PASSPHRASE = "key_custom_passphrase"
        private const val KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"

        @Volatile
        private var INSTANCE: CloudBackupPreferences? = null

        fun getInstance(context: Context): CloudBackupPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudBackupPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private fun loadBackupMode(): BackupMode {
        val saved = prefs.getString(KEY_BACKUP_MODE, BackupMode.OFFLINE.name) ?: BackupMode.OFFLINE.name
        return try {
            BackupMode.valueOf(saved)
        } catch (e: Exception) {
            BackupMode.OFFLINE
        }
    }

    private fun loadBackupStatus(): CloudBackupStatus {
        val saved = prefs.getString(KEY_LAST_BACKUP_STATUS, CloudBackupStatus.IDLE.name)
            ?: CloudBackupStatus.IDLE.name
        return try {
            CloudBackupStatus.valueOf(saved)
        } catch (e: Exception) {
            CloudBackupStatus.IDLE
        }
    }

    private fun loadErrorType(): CloudBackupErrorType? {
        val saved = prefs.getString(KEY_LAST_ERROR_TYPE, null) ?: return null
        return try {
            CloudBackupErrorType.valueOf(saved)
        } catch (e: Exception) {
            null
        }
    }

    var backupMode: BackupMode
        get() = loadBackupMode()
        set(value) {
            prefs.edit().putString(KEY_BACKUP_MODE, value.name).apply()
            _backupModeFlow.value = value
        }

    var accountEmail: String?
        get() = prefs.getString(KEY_ACCOUNT_EMAIL, null)
        set(value) {
            prefs.edit().putString(KEY_ACCOUNT_EMAIL, value).apply()
            _accountEmailFlow.value = value
        }

    var accountName: String?
        get() = prefs.getString(KEY_ACCOUNT_NAME, null)
        set(value) {
            prefs.edit().putString(KEY_ACCOUNT_NAME, value).apply()
            _accountNameFlow.value = value
        }

    var accountId: String?
        get() = prefs.getString(KEY_ACCOUNT_ID, null)
        set(value) {
            prefs.edit().putString(KEY_ACCOUNT_ID, value).apply()
        }

    var lastBackupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BACKUP_TIMESTAMP, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_BACKUP_TIMESTAMP, value).apply()
            _lastBackupTimestampFlow.value = value
        }

    var lastBackupStatus: CloudBackupStatus
        get() = loadBackupStatus()
        set(value) {
            prefs.edit().putString(KEY_LAST_BACKUP_STATUS, value.name).apply()
            _lastBackupStatusFlow.value = value
        }

    var lastErrorMessage: String?
        get() = prefs.getString(KEY_LAST_ERROR_MESSAGE, null)
        set(value) {
            prefs.edit().putString(KEY_LAST_ERROR_MESSAGE, value).apply()
            _lastErrorMessageFlow.value = value
        }

    var lastErrorType: CloudBackupErrorType?
        get() = loadErrorType()
        set(value) {
            if (value != null) {
                prefs.edit().putString(KEY_LAST_ERROR_TYPE, value.name).apply()
            } else {
                prefs.edit().remove(KEY_LAST_ERROR_TYPE).apply()
            }
            _lastErrorTypeFlow.value = value
        }

    var driveFileId: String?
        get() = prefs.getString(KEY_DRIVE_FILE_ID, null)
        set(value) {
            prefs.edit().putString(KEY_DRIVE_FILE_ID, value).apply()
            _driveFileIdFlow.value = value
        }

    var customPassphrase: String?
        get() = securePrefs.getString(KEY_CUSTOM_PASSPHRASE, null)
        set(value) {
            securePrefs.edit().putString(KEY_CUSTOM_PASSPHRASE, value).apply()
        }

    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, value).apply()
        }

    fun setSignedInAccount(email: String, name: String?, id: String?) {
        prefs.edit()
            .putString(KEY_ACCOUNT_EMAIL, email)
            .putString(KEY_ACCOUNT_NAME, name)
            .putString(KEY_ACCOUNT_ID, id)
            .apply()
        _accountEmailFlow.value = email
        _accountNameFlow.value = name
        clearError()
    }

    fun clearAccount() {
        prefs.edit()
            .remove(KEY_ACCOUNT_EMAIL)
            .remove(KEY_ACCOUNT_NAME)
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_DRIVE_FILE_ID)
            .remove(KEY_LAST_BACKUP_TIMESTAMP)
            .remove(KEY_LAST_BACKUP_STATUS)
            .remove(KEY_LAST_ERROR_MESSAGE)
            .remove(KEY_LAST_ERROR_TYPE)
            .apply()
        securePrefs.edit().remove(KEY_CUSTOM_PASSPHRASE).apply()
        _accountEmailFlow.value = null
        _accountNameFlow.value = null
        _driveFileIdFlow.value = null
        _lastBackupTimestampFlow.value = 0L
        _lastBackupStatusFlow.value = CloudBackupStatus.IDLE
        _lastErrorMessageFlow.value = null
        _lastErrorTypeFlow.value = null
    }

    fun recordBackupSuccess(timestamp: Long, fileId: String?) {
        lastBackupTimestamp = timestamp
        lastBackupStatus = CloudBackupStatus.SUCCESS
        if (fileId != null) {
            driveFileId = fileId
        }
        clearError()
    }

    fun recordBackupError(errorType: CloudBackupErrorType, message: String?) {
        lastBackupStatus = CloudBackupStatus.ERROR
        lastErrorType = errorType
        lastErrorMessage = message ?: errorType.userMessage
    }

    fun clearError() {
        lastErrorType = null
        lastErrorMessage = null
    }

    val isCloudConfiguredAndSignedIn: Boolean
        get() = backupMode == BackupMode.CLOUD && !accountEmail.isNullOrBlank()

    fun reset() {
        backupMode = BackupMode.OFFLINE
        clearAccount()
    }
}
