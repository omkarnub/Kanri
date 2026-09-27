package com.omkarnub.kanri.data.cloud

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncTest {

    private val restService = GoogleDriveRestService()

    @Test
    fun testHttpErrorMappingTokenExpired() {
        val error = restService.mapHttpError(401, """{"error":{"code":401,"message":"Invalid Credentials"}}""")
        assertTrue("HTTP 401 must map to TokenExpired", error is DriveApiException.TokenExpired)
    }

    @Test
    fun testHttpErrorMappingQuotaExceeded() {
        val quotaErrorBody = """
            {
              "error": {
                "errors": [
                  {
                    "domain": "usageLimits",
                    "reason": "storageQuotaExceeded",
                    "message": "The user's Drive storage quota has been exceeded."
                  }
                ],
                "code": 403,
                "message": "The user's Drive storage quota has been exceeded."
              }
            }
        """.trimIndent()

        val error = restService.mapHttpError(403, quotaErrorBody)
        assertTrue("HTTP 403 with storageQuotaExceeded must map to QuotaExceeded", error is DriveApiException.QuotaExceeded)
        assertTrue(error.message?.contains("quota exceeded", ignoreCase = true) == true)
    }

    @Test
    fun testHttpErrorMappingPermissionRevoked() {
        val permissionErrorBody = """
            {
              "error": {
                "errors": [
                  {
                    "domain": "global",
                    "reason": "insufficientPermissions",
                    "message": "The request requires higher privileges than provided."
                  }
                ],
                "code": 403,
                "message": "Insufficient Permission"
              }
            }
        """.trimIndent()

        val error = restService.mapHttpError(403, permissionErrorBody)
        assertTrue("HTTP 403 with insufficientPermissions must map to PermissionRevoked", error is DriveApiException.PermissionRevoked)
    }

    @Test
    fun testHttpErrorMappingFileNotFound() {
        val error = restService.mapHttpError(404, """{"error":{"code":404,"message":"File not found"}}""")
        assertTrue("HTTP 404 must map to FileNotFound", error is DriveApiException.FileNotFound)
    }

    @Test
    fun testHttpErrorMappingGeneralServerError() {
        val error = restService.mapHttpError(500, """{"error":{"code":500,"message":"Backend Error"}}""")
        assertTrue("HTTP 500 must map to GeneralError", error is DriveApiException.GeneralError)
        assertEquals(500, (error as DriveApiException.GeneralError).code)
    }

    @Test
    fun testDriveServiceLifecycleWithMock() = runBlocking {
        // Mock in-memory Drive Sync Service simulating remote Google Drive behavior
        val mockDrive = object : IDriveSyncService {
            private var storedFile: Pair<DriveFileMetadata, ByteArray>? = null

            override suspend fun findBackupFile(accessToken: String, fileName: String): Result<DriveFileMetadata?> {
                return Result.success(storedFile?.first?.takeIf { it.name == fileName })
            }

            override suspend fun uploadBackupFile(accessToken: String, fileBytes: ByteArray, fileName: String): Result<DriveFileMetadata> {
                val metadata = DriveFileMetadata(
                    id = "drive_file_id_999",
                    name = fileName,
                    sizeBytes = fileBytes.size.toLong(),
                    modifiedTime = "2026-09-26T12:00:00.000Z"
                )
                storedFile = Pair(metadata, fileBytes)
                return Result.success(metadata)
            }

            override suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<ByteArray> {
                val file = storedFile
                return if (file != null && file.first.id == fileId) {
                    Result.success(file.second)
                } else {
                    Result.failure(DriveApiException.FileNotFound())
                }
            }

            override suspend fun deleteBackupFile(accessToken: String, fileId: String): Result<Unit> {
                storedFile = null
                return Result.success(Unit)
            }
        }

        val testToken = "ya29.sample_oauth_token"
        val payloadBytes = "KANRI_ENC_v1_TEST_PAYLOAD".toByteArray(Charsets.UTF_8)

        // 1. Initial query: file should not exist
        val initialFile = mockDrive.findBackupFile(testToken).getOrNull()
        assertNull("File should not exist initially", initialFile)

        // 2. Upload file
        val uploadResult = mockDrive.uploadBackupFile(testToken, payloadBytes)
        assertTrue("Upload should succeed", uploadResult.isSuccess)
        val fileMeta = uploadResult.getOrThrow()
        assertEquals(GoogleDriveRestService.DEFAULT_BACKUP_FILENAME, fileMeta.name)
        assertEquals(payloadBytes.size.toLong(), fileMeta.sizeBytes)

        // 3. Query should now find file
        val foundFile = mockDrive.findBackupFile(testToken).getOrNull()
        assertNotNull("File should be found after upload", foundFile)
        assertEquals(fileMeta.id, foundFile?.id)

        // 4. Download file
        val downloadResult = mockDrive.downloadBackupFile(testToken, fileMeta.id)
        assertTrue("Download should succeed", downloadResult.isSuccess)
        assertArrayEquals("Downloaded bytes must match uploaded bytes", payloadBytes, downloadResult.getOrThrow())

        // 5. Delete file
        val deleteResult = mockDrive.deleteBackupFile(testToken, fileMeta.id)
        assertTrue("Delete should succeed", deleteResult.isSuccess)

        // 6. Query after delete should return null
        val afterDelete = mockDrive.findBackupFile(testToken).getOrNull()
        assertNull("File should be null after deletion", afterDelete)
    }
}
