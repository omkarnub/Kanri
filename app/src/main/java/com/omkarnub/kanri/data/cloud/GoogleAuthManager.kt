package com.omkarnub.kanri.data.cloud

import android.accounts.Account
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.omkarnub.kanri.data.profile.UserProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GoogleAuthManager(private val context: Context) {

    companion object {
        const val TAG = "GoogleAuthManager"
        const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

        @Volatile
        private var INSTANCE: GoogleAuthManager? = null

        fun getInstance(context: Context): GoogleAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GoogleAuthManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    /**
     * Step 1: Sign in with Google Identity flow using Credential Manager (androidx.credentials).
     * Returns GoogleAccountInfo containing the user's email, name, and id.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        serverClientId: String? = null
    ): Result<GoogleAccountInfo> = withContext(Dispatchers.IO) {
        try {
            val clientId = serverClientId?.takeIf { it.isNotBlank() } ?: run {
                val resId = context.resources.getIdentifier("google_web_client_id", "string", context.packageName)
                val configId = if (resId != 0) context.getString(resId) else null
                configId?.takeIf { it.isNotBlank() && !it.startsWith("YOUR_") } ?: "kanri-offline-drive.apps.googleusercontent.com"
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(clientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                val photoUrl = googleIdToken.profilePictureUri?.toString()
                val dob = extractDobFromIdToken(googleIdToken.idToken)
                val accountInfo = GoogleAccountInfo(
                    email = googleIdToken.id,
                    displayName = googleIdToken.displayName,
                    id = googleIdToken.id,
                    idToken = googleIdToken.idToken,
                    profilePictureUrl = photoUrl,
                    dob = dob
                )
                // Automatically populate name, profile photo, and DOB into local profile
                syncGoogleProfileToLocalProfile(accountInfo)
                Result.success(accountInfo)
            } else {
                Result.failure(Exception("Unsupported credential type returned from Credential Manager: ${credential.type}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception("Sign-in cancelled by user"))
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Credential Manager sign-in", e)
            Result.failure(e)
        }
    }

    /**
     * Step 2: Request authorization strictly for the 'drive.file' scope using AuthorizationClient.
     * Never requests full drive or drive.appdata scope.
     */
    suspend fun requestDriveAuthorization(
        email: String? = null
    ): Result<DriveAuthResult> = withContext(Dispatchers.IO) {
        try {
            val authClient = Identity.getAuthorizationClient(context)
            val requestedScopes = listOf(Scope(DRIVE_FILE_SCOPE))

            val builder = AuthorizationRequest.builder()
                .setRequestedScopes(requestedScopes)

            if (!email.isNullOrBlank()) {
                builder.setAccount(Account(email, "com.google"))
            }

            val authResult = authClient.authorize(builder.build()).await()

            if (authResult.hasResolution()) {
                val pendingIntent = authResult.pendingIntent
                if (pendingIntent != null) {
                    Result.success(DriveAuthResult.NeedsResolution(pendingIntent))
                } else {
                    Result.failure(DriveApiException.GeneralError(-1, "Authorization required resolution but pendingIntent was null"))
                }
            } else {
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    Result.success(
                        DriveAuthResult.Authorized(
                            accessToken = token,
                            grantedScopes = authResult.grantedScopes ?: emptyList()
                        )
                    )
                } else {
                    Result.failure(DriveApiException.PermissionRevoked("No access token returned for drive.file scope"))
                }
            }
        } catch (e: ApiException) {
            Log.e(TAG, "AuthorizationClient ApiException: status=${e.statusCode} ${e.message}", e)
            when (e.statusCode) {
                CommonStatusCodes.SIGN_IN_REQUIRED ->
                    Result.failure(DriveApiException.TokenExpired("Google account sign-in required: ${e.message}"))
                CommonStatusCodes.NETWORK_ERROR ->
                    Result.failure(DriveApiException.NetworkError(e))
                else ->
                    Result.failure(DriveApiException.GeneralError(e.statusCode, e.message ?: "Authorization failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception requesting drive.file authorization", e)
            Result.failure(e)
        }
    }

    /**
     * Obtains a valid access token for drive.file, refreshing automatically if possible.
     */
    suspend fun getAccessToken(email: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val authResult = requestDriveAuthorization(email)
        authResult.mapCatching { result ->
            when (result) {
                is DriveAuthResult.Authorized -> result.accessToken
                is DriveAuthResult.NeedsResolution ->
                    throw DriveApiException.PermissionRevoked("Drive access requires user consent")
            }
        }
    }

    /**
     * Signs out and clears stored credential state.
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing credential state: ${e.message}")
            Result.success(Unit) // Non-fatal
        }
    }

    /**
     * Automatically populates the local user profile (UserProfilePreferences) with
     * the name, profile picture, and date of birth from the signed-in Google account.
     */
    suspend fun syncGoogleProfileToLocalProfile(account: GoogleAccountInfo) = withContext(Dispatchers.IO) {
        try {
            val userPrefs = UserProfilePreferences.getInstance(context)

            // 1. Sync Display Name
            val displayName = account.displayName
            if (!displayName.isNullOrBlank()) {
                userPrefs.userName = displayName
                Log.d(TAG, "Synced Google display name to user profile: $displayName")
            }

            // 2. Sync DOB if present
            val dob = account.dob
            if (!dob.isNullOrBlank()) {
                userPrefs.userDob = dob
                Log.d(TAG, "Synced Google DOB to user profile: $dob")
            }

            // 3. Download and cache profile picture to internal storage
            val photoUrl = account.profilePictureUrl
            if (!photoUrl.isNullOrBlank()) {
                val success = userPrefs.savePhotoFromUrl(photoUrl)
                Log.d(TAG, "Downloaded and saved Google profile photo: success=$success")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing Google profile to local preferences", e)
        }
    }

    /**
     * Attempts to extract a birthdate / dob from standard Google OpenID Connect ID token claims.
     */
    private fun extractDobFromIdToken(idToken: String?): String? {
        if (idToken.isNullOrBlank()) return null
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                val payloadJson = JSONObject(String(payloadBytes, Charsets.UTF_8))
                val rawDob = when {
                    payloadJson.has("birthdate") -> payloadJson.optString("birthdate")
                    payloadJson.has("birthday") -> payloadJson.optString("birthday")
                    payloadJson.has("dob") -> payloadJson.optString("dob")
                    else -> null
                }
                if (!rawDob.isNullOrBlank() && rawDob != "null") {
                    try {
                        val inFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        val outFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        val parsed = inFormat.parse(rawDob)
                        if (parsed != null) outFormat.format(parsed) else rawDob
                    } catch (e: Exception) {
                        rawDob
                    }
                } else null
            } else null
        } catch (e: Exception) {
            Log.d(TAG, "Could not extract DOB from idToken: ${e.message}")
            null
        }
    }
}

