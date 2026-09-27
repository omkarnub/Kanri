package com.omkarnub.kanri.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthTest {

    @Test
    fun testScopeStrictlyRestrictedToDriveFile() {
        val requestedScope = GoogleAuthManager.DRIVE_FILE_SCOPE

        // Must strictly be drive.file
        assertEquals(
            "Scope must be restricted to drive.file",
            "https://www.googleapis.com/auth/drive.file",
            requestedScope
        )

        // Must NEVER request broad drive or hidden drive.appdata scope
        assertNotEquals(
            "Must never request broad drive access",
            "https://www.googleapis.com/auth/drive",
            requestedScope
        )
        assertNotEquals(
            "Must never request hidden appdata scope",
            "https://www.googleapis.com/auth/drive.appdata",
            requestedScope
        )
        assertFalse(
            "Scope must not contain full drive access",
            requestedScope.endsWith("/drive")
        )
        assertTrue(
            "Scope must explicitly specify drive.file",
            requestedScope.endsWith("/drive.file")
        )
    }

    @Test
    fun testCloudBackupErrorTypeDiagnosticsMappings() {
        val expired = CloudBackupErrorType.TOKEN_EXPIRED
        assertEquals("Sign In", expired.actionLabel)
        assertTrue(expired.userMessage.contains("expired", ignoreCase = true))

        val revoked = CloudBackupErrorType.PERMISSION_REVOKED
        assertEquals("Grant Access", revoked.actionLabel)
        assertTrue(revoked.userMessage.contains("revoked", ignoreCase = true))

        val quota = CloudBackupErrorType.QUOTA_EXCEEDED
        assertEquals("Check Storage", quota.actionLabel)
        assertTrue(quota.userMessage.contains("full", ignoreCase = true))

        val network = CloudBackupErrorType.NETWORK_ERROR
        assertEquals("Retry", network.actionLabel)
        assertTrue(network.userMessage.contains("Network unreachable", ignoreCase = true))

        val decrypt = CloudBackupErrorType.DECRYPTION_FAILED
        assertEquals("Enter Passphrase", decrypt.actionLabel)
        assertTrue(decrypt.userMessage.contains("passphrase", ignoreCase = true))

        val general = CloudBackupErrorType.GENERAL_ERROR
        assertEquals("Details", general.actionLabel)
    }

    @Test
    fun testGoogleAccountInfoModel() {
        val account = GoogleAccountInfo(
            email = "finance.user@gmail.com",
            displayName = "Finance User",
            id = "10987654321",
            idToken = "sample_jwt_id_token"
        )

        assertEquals("finance.user@gmail.com", account.email)
        assertEquals("Finance User", account.displayName)
        assertEquals("10987654321", account.id)
        assertNotNull(account.idToken)
    }

    @Test
    fun testDriveAuthResultHierarchy() {
        val authorized = DriveAuthResult.Authorized(
            accessToken = "mock_access_token_12345",
            grantedScopes = listOf(GoogleAuthManager.DRIVE_FILE_SCOPE)
        )

        assertEquals("mock_access_token_12345", authorized.accessToken)
        assertEquals(1, authorized.grantedScopes.size)
        assertEquals(GoogleAuthManager.DRIVE_FILE_SCOPE, authorized.grantedScopes[0])
    }
}
