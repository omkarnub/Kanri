package com.omkarnub.kanri.data.cloud

import android.app.PendingIntent
import com.omkarnub.kanri.data.backup.BackupStats

data class GoogleAccountInfo(
    val email: String,
    val displayName: String? = null,
    val id: String? = null,
    val idToken: String? = null,
    val profilePictureUrl: String? = null,
    val dob: String? = null
)

sealed class DriveAuthResult {
    data class Authorized(
        val accessToken: String,
        val grantedScopes: List<String> = emptyList()
    ) : DriveAuthResult()

    data class NeedsResolution(
        val pendingIntent: PendingIntent
    ) : DriveAuthResult()
}

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val modifiedTime: String? = null
)

data class CloudBackupResult(
    val fileId: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val stats: BackupStats
)

sealed class DriveApiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class TokenExpired(message: String = "Google authorization token expired") : DriveApiException(message)
    class PermissionRevoked(message: String = "Google Drive access revoked or scope not granted") : DriveApiException(message)
    class QuotaExceeded(message: String = "Google Drive storage quota exceeded") : DriveApiException(message)
    class NetworkError(cause: Throwable, message: String = "Network error communicating with Google Drive: ${cause.localizedMessage}") : DriveApiException(message, cause)
    class FileNotFound(message: String = "Kanri backup file not found on Google Drive") : DriveApiException(message)
    class GeneralError(val code: Int, message: String) : DriveApiException("Drive API error ($code): $message")
}
