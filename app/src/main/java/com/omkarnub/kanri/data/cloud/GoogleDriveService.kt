package com.omkarnub.kanri.data.cloud

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

interface IDriveSyncService {
    suspend fun findBackupFile(accessToken: String, fileName: String = GoogleDriveRestService.DEFAULT_BACKUP_FILENAME): Result<DriveFileMetadata?>
    suspend fun uploadBackupFile(accessToken: String, fileBytes: ByteArray, fileName: String = GoogleDriveRestService.DEFAULT_BACKUP_FILENAME): Result<DriveFileMetadata>
    suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<ByteArray>
    suspend fun deleteBackupFile(accessToken: String, fileId: String): Result<Unit>
}

class GoogleDriveRestService : IDriveSyncService {

    companion object {
        const val TAG = "GoogleDriveRestService"
        const val DEFAULT_BACKUP_FILENAME = "kanri_backup.enc"
        private const val DRIVE_API_BASE = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API_BASE = "https://www.googleapis.com/upload/drive/v3"
        private const val CONNECT_TIMEOUT_MS = 20000
        private const val READ_TIMEOUT_MS = 30000
    }

    override suspend fun findBackupFile(
        accessToken: String,
        fileName: String
    ): Result<DriveFileMetadata?> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val query = "name = '$fileName' and trashed = false"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val fields = URLEncoder.encode("files(id,name,size,modifiedTime)", "UTF-8")
            val urlString = "$DRIVE_API_BASE/files?q=$encodedQuery&fields=$fields&spaces=drive"

            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Accept", "application/json")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = readStream(connection.inputStream)
                val json = JSONObject(responseBody)
                val filesArray = json.optJSONArray("files")

                if (filesArray != null && filesArray.length() > 0) {
                    val fileObj = filesArray.getJSONObject(0)
                    val metadata = DriveFileMetadata(
                        id = fileObj.getString("id"),
                        name = fileObj.getString("name"),
                        sizeBytes = fileObj.optLong("size", 0L),
                        modifiedTime = if (fileObj.has("modifiedTime") && !fileObj.isNull("modifiedTime")) fileObj.getString("modifiedTime") else null
                    )
                    Result.success(metadata)
                } else {
                    Result.success(null)
                }
            } else {
                val errorBody = readStream(connection.errorStream)
                Result.failure(mapHttpError(responseCode, errorBody))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error in findBackupFile", e)
            Result.failure(DriveApiException.NetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Exception in findBackupFile", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun uploadBackupFile(
        accessToken: String,
        fileBytes: ByteArray,
        fileName: String
    ): Result<DriveFileMetadata> = withContext(Dispatchers.IO) {
        try {
            // Check if file already exists on Drive
            val existing = findBackupFile(accessToken, fileName).getOrNull()

            if (existing != null) {
                // Update existing file content via PATCH
                updateExistingFile(accessToken, existing.id, fileName, fileBytes)
            } else {
                // Create new file via Multipart upload
                createNewFile(accessToken, fileName, fileBytes)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error during uploadBackupFile", e)
            Result.failure(DriveApiException.NetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Exception during uploadBackupFile", e)
            Result.failure(e)
        }
    }

    private fun updateExistingFile(
        accessToken: String,
        fileId: String,
        fileName: String,
        fileBytes: ByteArray
    ): Result<DriveFileMetadata> {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$UPLOAD_API_BASE/files/$fileId?uploadType=media")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                setFixedLengthStreamingMode(fileBytes.size)
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            connection.outputStream.use { out ->
                out.write(fileBytes)
                out.flush()
            }

            val responseCode = connection.responseCode
            return if (responseCode in 200..299) {
                val responseBody = readStream(connection.inputStream)
                val json = JSONObject(responseBody)
                val updatedMetadata = DriveFileMetadata(
                    id = json.optString("id", fileId),
                    name = json.optString("name", fileName),
                    sizeBytes = fileBytes.size.toLong(),
                    modifiedTime = if (json.has("modifiedTime") && !json.isNull("modifiedTime")) json.getString("modifiedTime") else null
                )
                Result.success(updatedMetadata)
            } else {
                val errorBody = readStream(connection.errorStream)
                Result.failure(mapHttpError(responseCode, errorBody))
            }
        } catch (e: IOException) {
            return Result.failure(DriveApiException.NetworkError(e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun createNewFile(
        accessToken: String,
        fileName: String,
        fileBytes: ByteArray
    ): Result<DriveFileMetadata> {
        var connection: HttpURLConnection? = null
        try {
            val boundary = "kanri_multipart_boundary_${System.currentTimeMillis()}"
            val url = URL("$UPLOAD_API_BASE/files?uploadType=multipart")

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("mimeType", "application/octet-stream")
            }.toString()

            val prefix = "--$boundary\r\n" +
                    "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                    "$metadataJson\r\n" +
                    "--$boundary\r\n" +
                    "Content-Type: application/octet-stream\r\n\r\n"

            val suffix = "\r\n--$boundary--\r\n"

            val prefixBytes = prefix.toByteArray(StandardCharsets.UTF_8)
            val suffixBytes = suffix.toByteArray(StandardCharsets.UTF_8)
            val totalLength = prefixBytes.size + fileBytes.size + suffixBytes.size

            connection.setFixedLengthStreamingMode(totalLength)

            connection.outputStream.use { out ->
                out.write(prefixBytes)
                out.write(fileBytes)
                out.write(suffixBytes)
                out.flush()
            }

            val responseCode = connection.responseCode
            return if (responseCode in 200..299) {
                val responseBody = readStream(connection.inputStream)
                val json = JSONObject(responseBody)
                val metadata = DriveFileMetadata(
                    id = json.getString("id"),
                    name = json.optString("name", fileName),
                    sizeBytes = fileBytes.size.toLong(),
                    modifiedTime = if (json.has("modifiedTime") && !json.isNull("modifiedTime")) json.getString("modifiedTime") else null
                )
                Result.success(metadata)
            } else {
                val errorBody = readStream(connection.errorStream)
                Result.failure(mapHttpError(responseCode, errorBody))
            }
        } catch (e: IOException) {
            return Result.failure(DriveApiException.NetworkError(e))
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun downloadBackupFile(
        accessToken: String,
        fileId: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$DRIVE_API_BASE/files/$fileId?alt=media")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                Result.success(bytes)
            } else {
                val errorBody = readStream(connection.errorStream)
                Result.failure(mapHttpError(responseCode, errorBody))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error downloading backup file", e)
            Result.failure(DriveApiException.NetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading backup file", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun deleteBackupFile(
        accessToken: String,
        fileId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL("$DRIVE_API_BASE/files/$fileId")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Authorization", "Bearer $accessToken")
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299 || responseCode == 404) {
                Result.success(Unit)
            } else {
                val errorBody = readStream(connection.errorStream)
                Result.failure(mapHttpError(responseCode, errorBody))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error deleting backup file", e)
            Result.failure(DriveApiException.NetworkError(e))
        } catch (e: Exception) {
            Log.e(TAG, "Exception deleting backup file", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun mapHttpError(statusCode: Int, errorBody: String?): DriveApiException {
        val bodyText = errorBody ?: ""
        return when (statusCode) {
            401 -> DriveApiException.TokenExpired("Google authorization expired (401)")
            403 -> {
                when {
                    bodyText.contains("storageQuotaExceeded", ignoreCase = true) ||
                            bodyText.contains("quotaExceeded", ignoreCase = true) ->
                        DriveApiException.QuotaExceeded("Google Drive storage quota exceeded")

                    bodyText.contains("insufficientPermissions", ignoreCase = true) ||
                            bodyText.contains("accessNotConfigured", ignoreCase = true) ->
                        DriveApiException.PermissionRevoked("Google Drive access permission revoked or missing")

                    else -> DriveApiException.PermissionRevoked("Google Drive access forbidden (403): $bodyText")
                }
            }
            404 -> DriveApiException.FileNotFound("Backup file not found on Google Drive")
            else -> DriveApiException.GeneralError(statusCode, bodyText)
        }
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        val buffer = ByteArrayOutputStream()
        stream.use { input ->
            val data = ByteArray(4096)
            var n: Int
            while (input.read(data).also { n = it } != -1) {
                buffer.write(data, 0, n)
            }
        }
        return buffer.toString(StandardCharsets.UTF_8.name())
    }
}
