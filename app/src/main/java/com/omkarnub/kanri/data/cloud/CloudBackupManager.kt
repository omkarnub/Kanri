package com.omkarnub.kanri.data.cloud

import android.content.Context
import android.util.Log
import com.omkarnub.kanri.data.backup.BackupCryptoHelper
import com.omkarnub.kanri.data.backup.BackupJsonParser
import com.omkarnub.kanri.data.backup.BackupRepository
import com.omkarnub.kanri.data.backup.BackupStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class CloudBackupManager(
    private val context: Context,
    private val authManager: GoogleAuthManager = GoogleAuthManager.getInstance(context),
    private val driveService: IDriveSyncService = GoogleDriveRestService(),
    private val backupRepository: BackupRepository = BackupRepository(context),
    private val preferences: CloudBackupPreferences = CloudBackupPreferences.getInstance(context)
) {

    companion object {
        const val TAG = "CloudBackupManager"
        const val DEFAULT_FILE_NAME = "kanri_backup.enc"

        @Volatile
        private var INSTANCE: CloudBackupManager? = null

        fun getInstance(context: Context): CloudBackupManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudBackupManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Resolves the encryption passphrase.
     * Priority:
     * 1. Explicitly provided passphrase (if user entered one)
     * 2. Saved custom passphrase from secure preferences
     * 3. Deterministic account-derived passphrase (ensuring data is ALWAYS AES-GCM encrypted and never plaintext)
     */
    fun resolvePassphrase(explicitPassphrase: String? = null, email: String? = preferences.accountEmail): String {
        if (!explicitPassphrase.isNullOrBlank()) {
            return explicitPassphrase
        }
        val custom = preferences.customPassphrase
        if (!custom.isNullOrBlank()) {
            return custom
        }
        val targetEmail = email?.takeIf { it.isNotBlank() } ?: "kanri_offline_default"
        // Deterministic zero-knowledge salt bound to the user's Google account
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("kanri_sovereign_vault_${targetEmail.lowercase()}".toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Executes cloud backup:
     * 1. Verifies signed in
     * 2. Exports database to JSON payload
     * 3. Encrypts payload with BackupCryptoHelper (AES-GCM + PBKDF2 with KANRI_ENC_v1 magic header)
     * 4. Acquires fresh access token for drive.file
     * 5. Uploads / updates kanri_backup.enc on Google Drive
     * 6. Records timestamp and updates diagnostics state
     */
    suspend fun performCloudBackup(
        explicitPassphrase: String? = null
    ): Result<CloudBackupResult> = withContext(Dispatchers.IO) {
        preferences.lastBackupStatus = CloudBackupStatus.IN_PROGRESS
        try {
            val email = preferences.accountEmail
            if (email.isNullOrBlank()) {
                val error = "Cannot backup to cloud: Not signed in to Google Drive"
                preferences.recordBackupError(CloudBackupErrorType.TOKEN_EXPIRED, error)
                return@withContext Result.failure(DriveApiException.TokenExpired(error))
            }

            // 1. Generate JSON payload
            val payload = backupRepository.createBackupPayload()
            val jsonString = BackupJsonParser.toJson(payload)

            // 2. Encrypt payload (NEVER upload unencrypted data)
            val passphrase = resolvePassphrase(explicitPassphrase, email)
            val encryptedBytes = BackupCryptoHelper.encryptJson(jsonString, passphrase)

            if (!BackupCryptoHelper.isEncryptedBackup(encryptedBytes)) {
                throw IllegalStateException("Encryption check failed: payload was not properly encrypted")
            }

            // 3. Acquire valid access token
            val tokenResult = authManager.getAccessToken(email)
            if (tokenResult.isFailure) {
                val error = tokenResult.exceptionOrNull() ?: Exception("Failed to get Drive access token")
                handleError(error)
                return@withContext Result.failure(error)
            }
            val accessToken = tokenResult.getOrThrow()

            // 4. Upload to Drive
            val uploadResult = driveService.uploadBackupFile(
                accessToken = accessToken,
                fileBytes = encryptedBytes,
                fileName = DEFAULT_FILE_NAME
            )

            if (uploadResult.isFailure) {
                val error = uploadResult.exceptionOrNull() ?: Exception("Upload to Drive failed")
                handleError(error)
                return@withContext Result.failure(error)
            }

            val uploadedFile = uploadResult.getOrThrow()
            val now = System.currentTimeMillis()
            preferences.recordBackupSuccess(now, uploadedFile.id)

            val stats = BackupStats(
                transactionCount = payload.transactions.size,
                categoryCount = payload.categories.size,
                budgetCount = payload.budgets.size,
                lendingCount = payload.lendingRecords.size,
                mappingCount = payload.counterpartyMappings.size,
                repaymentCount = payload.lendingRepayments.size
            )

            Log.i(TAG, "Cloud backup completed successfully: fileId=${uploadedFile.id}, size=${encryptedBytes.size} bytes")
            Result.success(
                CloudBackupResult(
                    fileId = uploadedFile.id,
                    sizeBytes = encryptedBytes.size.toLong(),
                    timestamp = now,
                    stats = stats
                )
            )
        } catch (e: Exception) {
            handleError(e)
            Result.failure(e)
        }
    }

    /**
     * Executes restore from cloud:
     * 1. Queries Drive for kanri_backup.enc (scoped by drive.file)
     * 2. Downloads encrypted blob
     * 3. Decrypts via BackupCryptoHelper
     * 4. Feeds through existing atomic withTransaction Room restore logic
     */
    suspend fun restoreFromCloud(
        explicitPassphrase: String? = null,
        clearExisting: Boolean = true
    ): Result<BackupStats> = withContext(Dispatchers.IO) {
        try {
            val email = preferences.accountEmail
            if (email.isNullOrBlank()) {
                return@withContext Result.failure(DriveApiException.TokenExpired("Not signed in to Google Drive"))
            }

            val tokenResult = authManager.getAccessToken(email)
            if (tokenResult.isFailure) {
                val error = tokenResult.exceptionOrNull() ?: Exception("Failed to obtain Drive access token")
                handleError(error)
                return@withContext Result.failure(error)
            }
            val accessToken = tokenResult.getOrThrow()

            // 1. Locate file on Drive
            val fileResult = driveService.findBackupFile(accessToken, DEFAULT_FILE_NAME)
            if (fileResult.isFailure) {
                val error = fileResult.exceptionOrNull() ?: Exception("Failed to search Drive")
                handleError(error)
                return@withContext Result.failure(error)
            }

            val fileMetadata = fileResult.getOrThrow()
                ?: return@withContext Result.failure(DriveApiException.FileNotFound("No Kanri backup ($DEFAULT_FILE_NAME) found in your Google Drive."))

            // 2. Download file bytes
            val downloadResult = driveService.downloadBackupFile(accessToken, fileMetadata.id)
            if (downloadResult.isFailure) {
                val error = downloadResult.exceptionOrNull() ?: Exception("Failed to download backup file")
                handleError(error)
                return@withContext Result.failure(error)
            }
            val encryptedBytes = downloadResult.getOrThrow()

            // 3. Decrypt file
            val passphrase = resolvePassphrase(explicitPassphrase, email)
            val jsonString = try {
                if (BackupCryptoHelper.isEncryptedBackup(encryptedBytes)) {
                    BackupCryptoHelper.decryptJson(encryptedBytes, passphrase)
                } else {
                    // Fallback plain UTF-8 if legacy
                    String(encryptedBytes, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Decryption failed: ${e.message}", e)
                preferences.recordBackupError(
                    CloudBackupErrorType.DECRYPTION_FAILED,
                    "Failed to decrypt backup. If you set a custom passphrase, please provide it."
                )
                return@withContext Result.failure(
                    Exception("Failed to decrypt cloud backup. Please check your backup passphrase.")
                )
            }

            // 4. Restore via atomic transaction
            val restoreResult = backupRepository.restoreBackupFromJson(jsonString, clearExisting)
            if (restoreResult.isSuccess) {
                preferences.clearError()
                Log.i(TAG, "Cloud restore succeeded: ${restoreResult.getOrNull()}")
            } else {
                handleError(restoreResult.exceptionOrNull() ?: Exception("Database restore failed"))
            }
            restoreResult
        } catch (e: Exception) {
            handleError(e)
            Result.failure(e)
        }
    }

    /**
     * Checks if a backup file currently exists on Google Drive.
     */
    suspend fun getCloudBackupMetadata(): Result<DriveFileMetadata?> = withContext(Dispatchers.IO) {
        try {
            val email = preferences.accountEmail ?: return@withContext Result.success(null)
            val token = authManager.getAccessToken(email).getOrNull() ?: return@withContext Result.success(null)
            driveService.findBackupFile(token, DEFAULT_FILE_NAME)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes the existing backup file from the user's Google Drive.
     */
    suspend fun deleteCloudBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val email = preferences.accountEmail ?: return@withContext Result.success(Unit)
            val token = authManager.getAccessToken(email).getOrNull()
                ?: return@withContext Result.failure(DriveApiException.TokenExpired("Not signed in"))

            val existing = driveService.findBackupFile(token, DEFAULT_FILE_NAME).getOrNull()
            if (existing != null) {
                val deleteResult = driveService.deleteBackupFile(token, existing.id)
                if (deleteResult.isSuccess) {
                    preferences.driveFileId = null
                    preferences.lastBackupTimestamp = 0L
                    preferences.clearError()
                }
                deleteResult
            } else {
                preferences.driveFileId = null
                preferences.lastBackupTimestamp = 0L
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun handleError(e: Throwable) {
        when (e) {
            is DriveApiException.TokenExpired ->
                preferences.recordBackupError(CloudBackupErrorType.TOKEN_EXPIRED, e.message)
            is DriveApiException.PermissionRevoked ->
                preferences.recordBackupError(CloudBackupErrorType.PERMISSION_REVOKED, e.message)
            is DriveApiException.QuotaExceeded ->
                preferences.recordBackupError(CloudBackupErrorType.QUOTA_EXCEEDED, e.message)
            is DriveApiException.NetworkError ->
                preferences.recordBackupError(CloudBackupErrorType.NETWORK_ERROR, e.message)
            else ->
                preferences.recordBackupError(CloudBackupErrorType.GENERAL_ERROR, e.localizedMessage)
        }
    }
}
